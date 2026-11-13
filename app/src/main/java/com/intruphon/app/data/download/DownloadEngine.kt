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
import com.intruphon.app.data.media.FailReason
import com.intruphon.app.data.media.LoadResult
import com.intruphon.app.data.media.MediaInfo
import com.intruphon.app.data.media.MediaRepository
import com.intruphon.app.data.media.PlaylistEntry
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
        val record = DownloadRecord(id, if (option.kind == Kind.AUDIO) info.metadata.title else info.title, option.kind, option.extension, option.quality, sourceLink, coverUrl = info.metadata.coverUrl)
        val item = DownloadItem(record, DownloadStatus(DownloadState.WAITING, 0, option.sizeBytes ?: 0, 0))
        state.update { listOf(item) + it }
        DownloadStore.save(appContext, state.value)
        DownloadService.start(appContext)
        jobs[id] = scope.launch { run(id, info, option) }
    }

    private val readingCount = MutableStateFlow(0)
    private val readers = java.util.concurrent.CopyOnWriteArraySet<Job>()

    /** True while playlist items are still being read, so the service stays up between two of them. */
    val reading: StateFlow<Boolean> = MutableStateFlow(false).also { flag ->
        scope.launch { readingCount.collect { flag.value = it > 0 } }
    }.asStateFlow()

    /**
     * Reads the playlist [entries] one by one and queues each in the format [choose] picks from its options.
     * An item that cannot be read shows up in the list as a failed entry (the retry button opens it as a single link).
     */
    fun enqueueAll(entries: List<PlaylistEntry>, kind: Kind, choose: (List<DownloadOption>) -> DownloadOption?) {
        readingCount.update { it + 1 }
        readers.add(scope.launch {
            try {
                for (entry in entries) {
                    // One more try when the first read fails: a single bad moment should not lose the item.
                    var result = MediaRepository.load(entry.url)
                    if (result is LoadResult.Failed) {
                        delay(1500)
                        result = MediaRepository.load(entry.url)
                    }
                    val option = (result as? LoadResult.Ready)?.let { choose(it.info.options) }
                    if (result is LoadResult.Ready && option != null) {
                        enqueue(result.info, option, entry.url)
                    } else if (result is LoadResult.Failed && result.reason == FailReason.NETWORK) {
                        addFailed(entry, kind, REASON_IO)   // a connection problem can pass: it stays on the list to retry
                    } else if (result is LoadResult.Ready) {
                        addFailed(entry, kind, REASON_NO_FORMAT)
                    }
                    // An item the source cannot give (removed, private, blocked) is skipped quietly, like the sheet shows it.
                }
            } finally {
                readingCount.update { it - 1 }
            }
        }.also { job -> job.invokeOnCompletion { readers.remove(job) } })
    }

    private fun addFailed(entry: PlaylistEntry, kind: Kind, reason: Int) {
        val record = DownloadRecord(nextId(), entry.title, kind, "", "", entry.url, note = withBuild("playlist item"), coverUrl = entry.thumbnailUrl)
        state.update { listOf(DownloadItem(record, DownloadStatus(DownloadState.FAILED, 0, 0, reason))) + it }
        DownloadStore.save(appContext, state.value)
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
        if (deleteFile) item?.record?.uri?.let { runCatching { SaveLocation.delete(appContext.contentResolver, Uri.parse(it)) } }
    }

    /** What to do next to delete a saved file from the device. */
    sealed interface DeleteStep {
        /** Every file is gone and the entries are forgotten. */
        data object Gone : DeleteStep

        /** The system must ask the person for the files in [ids]; launch [sender] and call [finishDelete] with the result. */
        data class Ask(val sender: IntentSender, val ids: List<Long>) : DeleteStep

        /** Could not delete. [detail] says what the system answered, to find the cause. */
        data class Failed(val detail: String?) : DeleteStep
    }

    /**
     * Deletes saved files from the device. The app first deletes what it can itself (files this install created); for
     * the rest the system's own window asks the person (no storage permission). Entries of deleted files are forgotten.
     * Call off the main thread.
     */
    fun startDelete(context: Context, ids: List<Long>): DeleteStep {
        val resolver = context.contentResolver
        val left = ArrayList<Pair<Long, Uri>>()
        var problem: Exception? = null
        var recoverable: IntentSender? = null
        for (id in ids) {
            val uri = state.value.firstOrNull { it.record.id == id }?.record?.uri?.let(Uri::parse)
            if (uri == null || !exists(uri)) {
                remove(id, deleteFile = false)
                continue
            }
            try {
                SaveLocation.delete(resolver, uri)
            } catch (e: RecoverableSecurityException) {
                recoverable = recoverable ?: e.userAction.actionIntent.intentSender
            } catch (e: Exception) {
                problem = problem ?: e
            }
            if (exists(uri)) left.add(id to uri) else remove(id, deleteFile = false)
        }
        if (left.isEmpty()) return DeleteStep.Gone
        // Files in a folder the person picked are deleted through the folder's own permission: the system window
        // (MediaStore) cannot handle them.
        if (left.any { it.second.authority != MediaStore.AUTHORITY }) {
            return DeleteStep.Failed(problem?.let { "${it.javaClass.simpleName}: ${it.message}" })
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                DeleteStep.Ask(MediaStore.createDeleteRequest(resolver, left.map { it.second }).intentSender, left.map { it.first })
            } catch (e: Exception) {
                DeleteStep.Failed("${e.javaClass.simpleName}: ${e.message}")
            }
        }
        // Android 10: the system asks about one file at a time.
        recoverable?.let { return DeleteStep.Ask(it, listOf(left.first().first)) }
        return DeleteStep.Failed(problem?.let { "${it.javaClass.simpleName}: ${it.message}" })
    }

    /**
     * After the system window closed: forgets the entries whose file is gone (trying once more to delete what is left,
     * now that the person agreed) and returns true when none of [ids] is left. Call off the main thread.
     */
    fun finishDelete(context: Context, ids: List<Long>): Boolean {
        var allGone = true
        for (id in ids) {
            val uri = state.value.firstOrNull { it.record.id == id }?.record?.uri?.let(Uri::parse)
            if (uri != null && exists(uri)) runCatching { SaveLocation.delete(context.contentResolver, uri) }
            if (uri != null && exists(uri)) allGone = false else remove(id, deleteFile = false)
        }
        return allGone
    }

    fun cancelAll() {
        readers.forEach { it.cancel() }   // playlist items not yet read are dropped too
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

    /** Looks at the files right now: marks the vanished ones as missing and returns the ids that still have a file. */
    suspend fun stillOnDevice(ids: List<Long>): List<Long> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        ids.filter { id ->
            val item = state.value.firstOrNull { it.record.id == id } ?: return@filter false
            val uri = item.record.uri
            when {
                item.status.state != DownloadState.DONE || uri == null -> false
                exists(Uri.parse(uri)) -> true
                else -> { markMissing(id); false }
            }
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
            while (state.value.count { it.status.state == DownloadState.RUNNING || it.status.state == DownloadState.PROCESSING } >= AppSettings.maxParallelNow(appContext)) {
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
