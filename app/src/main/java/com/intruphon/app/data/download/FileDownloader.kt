package com.intruphon.app.data.download

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.intruphon.app.data.media.DownloadOption
import com.intruphon.app.data.media.Kind

/**
 * Hands files to Android's download manager, which writes into the public Downloads folder
 * (no storage permission needed on Android 10+), and reads their progress back.
 * Layout on the device: Download/Intruphon/Audio and Download/Intruphon/Video.
 */
object FileDownloader {
    fun enqueue(context: Context, title: String, option: DownloadOption, sourceLink: String) {
        val fileName = "${sanitizeFileName(title)}.${option.extension}"
        val subfolder = if (option.kind == Kind.AUDIO) "Audio" else "Video"
        val request = DownloadManager.Request(Uri.parse(option.url))
            .setTitle(fileName)
            .setMimeType(option.mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Intruphon/$subfolder/$fileName")
        val id = manager(context).enqueue(request)
        DownloadStore.add(context, DownloadRecord(id, title, option.kind, option.extension, option.quality, sourceLink))
    }

    /** Started downloads, newest first, with their current status. Call off the main thread. */
    fun snapshot(context: Context): List<DownloadItem> {
        val records = DownloadStore.load(context)
        if (records.isEmpty()) return emptyList()
        val statuses = query(manager(context), records.map { it.id })
        val missing = DownloadStatus(DownloadState.MISSING, 0, 0, 0)
        return records.reversed().map { DownloadItem(it, statuses[it.id] ?: missing) }
    }

    /** Cancels a running download or deletes the finished file, and forgets the entry. */
    fun remove(context: Context, id: Long) {
        manager(context).remove(id)
        DownloadStore.remove(context, id)
    }

    /** Opens the finished file in another app. Returns false if nothing can open it. */
    fun open(context: Context, id: Long): Boolean {
        val manager = manager(context)
        val uri = manager.getUriForDownloadedFile(id) ?: return false
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, manager.getMimeTypeForDownloadedFile(id))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun manager(context: Context) = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun query(manager: DownloadManager, ids: List<Long>): Map<Long, DownloadStatus> {
        val result = HashMap<Long, DownloadStatus>()
        manager.query(DownloadManager.Query().setFilterById(*ids.toLongArray()))?.use { c ->
            val idCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val statusCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val doneCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val reasonCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
            while (c.moveToNext()) {
                val state = when (c.getInt(statusCol)) {
                    DownloadManager.STATUS_RUNNING -> DownloadState.RUNNING
                    DownloadManager.STATUS_PAUSED -> DownloadState.PAUSED
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadState.DONE
                    DownloadManager.STATUS_FAILED -> DownloadState.FAILED
                    else -> DownloadState.WAITING
                }
                result[c.getLong(idCol)] = DownloadStatus(state, c.getLong(doneCol), c.getLong(totalCol), c.getInt(reasonCol))
            }
        }
        return result
    }
}

/** Removes characters that are not allowed in file names and limits the length. */
internal fun sanitizeFileName(name: String): String {
    val cleaned = name
        .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_")
        .trim()
        .trim('.')
        .take(100)
        .trim()
    return cleaned.ifEmpty { "media" }
}
