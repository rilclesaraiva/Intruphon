package com.intruphon.app.data.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.intruphon.app.data.media.DownloadOption
import com.intruphon.app.data.media.Kind

/** Hands the file to Android's download manager: it shows progress and completion notifications. */
object FileDownloader {
    fun enqueue(context: Context, title: String, option: DownloadOption) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "${sanitizeFileName(title)}.${option.extension}"
        val folder = if (option.kind == Kind.AUDIO) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val request = DownloadManager.Request(Uri.parse(option.url))
            .setTitle(fileName)
            .setMimeType(option.mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(folder, "Intruphon/$fileName")
        manager.enqueue(request)
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
