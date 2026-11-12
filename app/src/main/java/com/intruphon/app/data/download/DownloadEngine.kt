package com.intruphon.app.data.download

import android.content.ActivityNotFoundException
import android.content.Context
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import android.net.Uri
import android.os.SystemClock
import com.intruphon.app.data.media.DownloadOption
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.media.MediaInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.intruphon.app.data.media.Plan
import com.intruphon.app.data.net.NetworkKind
import com.intruphon.app.data.settings.AppSettings
import com.intruphon.app.data.stats.SpeedEntry
import com.intruphon.app.data.stats.SpeedLog
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/** Runs the downloads and exposes the list (newest first) for the Downloads tab and the notification. */
object DownloadEngine {
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val state = MutableStateFlow<List<DownloadItem>>(emptyList())
    private var lastId = 0L
    private val gate = Mutex()

    val items: StateFlow<List<DownloadItem>> = state.asStateFlow()

    /** Call once from Application.onCreate. Downloads that were running when the app died are marked interrupted. */
    fun init(context: Context) {
        appContext = context.applicationContext
        val loaded = DownloadStore.load(appContext).map {
            if (it.status.state.active) {
                it.copy(status = it.status.copy(state = DownloadState.FAILED, reason = REASON_INTERRUPTED))
            } else it
        }
        state.value = loaded
        DownloadStore.save(appContext, loaded)
    }

    fun enqueue(info: MediaInfo, option: DownloadOption, sourceLink: String) {
        val id = nextId()
        val record = DownloadRecord(id, if (option.kind == Kind.AUDIO) info.metadata.title else info.title, option.kind, option.extension, option.quality, sourceLink)
        val item = DownloadItem(record, DownloadStatus(DownloadState.WAITING, 0, option.sizeBytes ?: 0, 0))
        state.update { listOf(item) + it }
        DownloadStore.save(appContext, state.value)
        DownloadService.start(appContext)
        jobs[id] = scope.launch { run(id, info, option) }
    }

    /**
     * Cancels a running download and forgets the entry. A file that is still being written is removed with it;
     * a finished file stays on the device when [deleteFile] is false. Call off the main thread.
     */
    fun remove(id: Long, deleteFile: Boolean = true) {
        val item = state.value.firstOrNull { it.record.id == id }
        jobs.remove(id)?.cancel()
        state.update { list -> list.filterNot { it.record.id == id } }
        DownloadStore.save(appContext, state.value)
        if (deleteFile) item?.record?.uri?.let { runCatching { appContext.contentResolver.delete(Uri.parse(it), null, null) } }
    }

    /** What to do next to delete a saved file from the device. */
    sealed interface DeleteStep {
        /** Nothing left to delete (the file is already gone). */
        data object Gone : DeleteStep
        /** The system must ask the person: launch [sender] and call [finishDelete] with the answer. */
        data class Ask(val sender: IntentSender) : DeleteStep
        data object Failed : DeleteStep
    }

    /**
     * Starts deleting the saved file of [id] through the system, without any storage permission.
     * Android 11+: the system shows its own "allow to delete" window. Android 10: the file is deleted
     * directly, and the system only asks if it does not let the app do that. Call off the main thread.
     */
    fun startDelete(context: Context, id: Long): DeleteStep {
        val uri = state.value.firstOrNull { it.record.id == id }?.record?.uri?.let(Uri::parse) ?: return DeleteStep.Gone
        if (!exists(uri)) return DeleteStep.Gone
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                DeleteStep.Ask(MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender)
            } catch (e: Exception) {
                DeleteStep.Failed
            }
        }
        return try {
            resolver.delete(uri, null, null)
            if (exists(uri)) DeleteStep.Failed else DeleteStep.Gone
        } catch (e: RecoverableSecurityException) {
            DeleteStep.Ask(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            DeleteStep.Failed
        }
    }

    /**
     * After the system window closed: if the file is gone, forget the entry and return true.
     * On Android 10 the delete is tried once more, now that the person agreed. Call off the main thread.
     */
    fun finishDelete(context: Context, id: Long): Boolean {
        val uri = state.value.firstOrNull { it.record.id == id }?.record?.uri?.let(Uri::parse)
        if (uri != null && exists(uri) && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
        if (uri != null && exists(uri)) return false
        remove(id, deleteFile = false)
        return true
    }

    fun cancelAll() {
        state.value
            .filter { it.status.state.active }
            .forEach { remove(it.record.id) }
    }

    enum class OpenResult { OPENED, NO_APP, MISSING }

    /** Opens the finished file in another app. A file that was deleted outside the app is marked as missing. */
    fun open(context: Context, id: Long): OpenResult {
        val uri = state.value.firstOrNull { it.record.id == id }?.record?.uri?.let(Uri::parse) ?: return OpenResult.MISSING
        if (!exists(uri)) {
            markMissing(id)
            return OpenResult.MISSING
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, runCatching { context.contentResolver.getType(uri) }.getOrNull())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
            OpenResult.OPENED
        } catch (e: ActivityNotFoundException) {
            OpenResult.NO_APP
        } catch (e: Exception) {
            if (exists(uri)) OpenResult.NO_APP else { markMissing(id); OpenResult.MISSING }
        }
    }

    /** Looks for finished files that were deleted outside the app and marks them as missing. */
    fun checkFiles() {
        scope.launch {
            state.value
                .filter { it.status.state == DownloadState.DONE && it.record.uri != null }
                .filterNot { exists(Uri.parse(it.record.uri)) }
                .forEach { markMissing(it.record.id) }
        }
    }

    // Every note says which build wrote it, so a screenshot tells which version was tested.
    private fun withBuild(note: String) = if (note.isEmpty()) "" else "$note [build ${com.intruphon.app.BuildConfig.BUILD}]"

    private fun markMissing(id: Long) =
        edit(id) { it.copy(status = it.status.copy(state = DownloadState.MISSING)) }

    // Opening a descriptor fails when the file is gone, even if the media index still lists it.
    private fun exists(uri: Uri): Boolean = try {
        appContext.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
    }

    private suspend fun run(id: Long, info: MediaInfo, option: DownloadOption) {
        // Downloads start one by one, in order, while fewer than the chosen number are running.
        gate.withLock {
            while (state.value.count { it.status.state == DownloadState.RUNNING || it.status.state == DownloadState.PROCESSING } >= AppSettings.maxParallel(appContext)) {
                delay(500)
            }
            edit(id) { it.copy(status = it.status.copy(state = DownloadState.RUNNING)) }
        }
        if (AppSettings.wifiOnly(appContext) && !NetworkKind.isWifiOrEthernet(appContext)) {
            fail(id, REASON_NEEDS_WIFI)
            jobs.remove(id)
            return
        }
        val network = NetworkKind.current(appContext)
        val startedAt = SystemClock.elapsedRealtime()
        var processingAt = 0L
        var fetched = 0L
        fun log(ok: Boolean, reason: Int) {
            val end = SystemClock.elapsedRealtime()
            val downloadMs = (if (processingAt > 0) processingAt else end) - startedAt
            val processMs = if (processingAt > 0) end - processingAt else 0L
            val plan = when (option.plan) {
                is Plan.Direct -> "direct"
                is Plan.AudioM4a -> "m4a"
                is Plan.AudioMp3 -> "mp3"
                is Plan.Mux -> "mux"
            }
            SpeedLog.add(appContext, SpeedEntry(System.currentTimeMillis(), option.kind.name, option.format, option.quality, plan, network, fetched, downloadMs, processMs, ok, reason))
        }
        var lastEmit = 0L
        try {
            val saved = MediaDownloader.download(appContext, info, option, onProgress = { done, total ->
                fetched = done
                val now = SystemClock.elapsedRealtime()
                if (now - lastEmit >= 750) {
                    lastEmit = now
                    edit(id, save = false) {
                        it.copy(status = it.status.copy(downloadedBytes = done, totalBytes = if (total > 0) total else it.status.totalBytes))
                    }
                }
            }, onProcessing = {
                if (processingAt == 0L) processingAt = SystemClock.elapsedRealtime()
                edit(id, save = false) { it.copy(status = it.status.copy(state = DownloadState.PROCESSING)) }
            })
            val fileSize = runCatching { appContext.contentResolver.openFileDescriptor(saved.uri, "r")?.use { it.statSize } }.getOrNull()
            log(true, 0)
            finish(id) { item ->
                val size = fileSize?.takeIf { it > 0 } ?: item.status.totalBytes.takeIf { it > 0 } ?: item.status.downloadedBytes
                item.copy(
                    record = item.record.copy(uri = saved.uri.toString(), tags = saved.tagged, note = withBuild(saved.note)),
                    status = DownloadStatus(DownloadState.DONE, size, size, 0),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpStatusException) {
            log(false, e.code)
            fail(id, e.code)
        } catch (e: java.io.IOException) {
            log(false, REASON_IO)
            fail(id, REASON_IO, "${e.javaClass.simpleName}: ${e.message}")
        } catch (e: Exception) {
            log(false, 0)
            fail(id, 0, "${e.javaClass.simpleName}: ${e.message}")
        } finally {
            jobs.remove(id)
        }
    }

    private fun fail(id: Long, reason: Int, note: String = "") =
        finish(id) { it.copy(record = it.record.copy(note = withBuild(note)), status = it.status.copy(state = DownloadState.FAILED, reason = reason)) }

    private fun finish(id: Long, change: (DownloadItem) -> DownloadItem) {
        edit(id, transform = change)
        state.value.firstOrNull { it.record.id == id }?.let { DownloadNotifications.finished(appContext, it) }
    }

    private fun edit(id: Long, save: Boolean = true, transform: (DownloadItem) -> DownloadItem) {
        state.update { list -> list.map { if (it.record.id == id) transform(it) else it } }
        if (save) DownloadStore.save(appContext, state.value)
    }

    @Synchronized
    private fun nextId(): Long {
        lastId = maxOf(System.currentTimeMillis(), lastId + 1)
        return lastId
    }
}
