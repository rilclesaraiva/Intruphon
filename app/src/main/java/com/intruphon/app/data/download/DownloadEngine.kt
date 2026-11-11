package com.intruphon.app.data.download

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.flow.update
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

    val items: StateFlow<List<DownloadItem>> = state.asStateFlow()

    /** Call once from Application.onCreate. Downloads that were running when the app died are marked interrupted. */
    fun init(context: Context) {
        appContext = context.applicationContext
        val loaded = DownloadStore.load(appContext).map {
            if (it.status.state == DownloadState.WAITING || it.status.state == DownloadState.RUNNING) {
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

    /** Cancels a running download, or deletes the saved file, and forgets the entry. Call off the main thread. */
    fun remove(id: Long) {
        val item = state.value.firstOrNull { it.record.id == id }
        jobs.remove(id)?.cancel()
        state.update { list -> list.filterNot { it.record.id == id } }
        DownloadStore.save(appContext, state.value)
        item?.record?.uri?.let { runCatching { appContext.contentResolver.delete(Uri.parse(it), null, null) } }
    }

    fun cancelAll() {
        state.value
            .filter { it.status.state == DownloadState.WAITING || it.status.state == DownloadState.RUNNING }
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

    private fun markMissing(id: Long) =
        edit(id) { it.copy(status = it.status.copy(state = DownloadState.MISSING)) }

    // Opening a descriptor fails when the file is gone, even if the media index still lists it.
    private fun exists(uri: Uri): Boolean = try {
        appContext.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
    }

    private suspend fun run(id: Long, info: MediaInfo, option: DownloadOption) {
        edit(id) { it.copy(status = it.status.copy(state = DownloadState.RUNNING)) }
        var lastEmit = 0L
        try {
            val saved = MediaDownloader.download(appContext, info, option) { done, total ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastEmit >= 750) {
                    lastEmit = now
                    edit(id, save = false) {
                        it.copy(status = it.status.copy(downloadedBytes = done, totalBytes = if (total > 0) total else it.status.totalBytes))
                    }
                }
            }
            finish(id) { item ->
                val size = item.status.totalBytes.takeIf { it > 0 } ?: item.status.downloadedBytes
                item.copy(
                    record = item.record.copy(uri = saved.uri.toString(), tags = saved.tagged),
                    status = DownloadStatus(DownloadState.DONE, size, size, 0),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpStatusException) {
            fail(id, e.code)
        } catch (e: java.io.IOException) {
            fail(id, REASON_IO)
        } catch (e: Exception) {
            fail(id, 0)
        } finally {
            jobs.remove(id)
        }
    }

    private fun fail(id: Long, reason: Int) =
        finish(id) { it.copy(status = it.status.copy(state = DownloadState.FAILED, reason = reason)) }

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
