package com.intruphon.app.data.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.net.Http
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.Request
import java.io.IOException
import java.io.OutputStream

/** The server answered with an HTTP error status. */
class HttpStatusException(val code: Int) : IOException("HTTP $code")

/**
 * Downloads a file in ranged chunks (what video sites expect) straight into the public Downloads
 * folder through MediaStore: Download/Intruphon/Audio or Download/Intruphon/Video.
 * An app can add files to its own folder this way without any storage permission on Android 10+.
 */
internal object HttpFileDownloader {
    private const val CHUNK = 10L * 1024 * 1024

    /**
     * Creates the file in Download/Intruphon/Audio or Video, lets [write] fill it and returns its content Uri.
     * A half-written file is deleted on failure or cancel.
     */
    suspend fun save(context: Context, fileName: String, mimeType: String, kind: Kind, write: suspend (OutputStream) -> Unit): Uri {
        val resolver = context.contentResolver
        val subfolder = if (kind == Kind.AUDIO) "Audio" else "Video"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Intruphon/$subfolder")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create the file")
        var finished = false
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("Could not open the file")
            out.use { write(it) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            finished = true
            return uri
        } finally {
            if (!finished) runCatching { resolver.delete(uri, null, null) }   // also on cancel: no partial files
        }
    }

    suspend fun fetch(url: String, out: OutputStream, onProgress: (Long, Long) -> Unit) {
        val buffer = ByteArray(64 * 1024)
        var position = 0L
        var total = -1L
        var done = false
        while (!done) {
            currentCoroutineContext().ensureActive()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", Http.USER_AGENT)
                .header("Range", "bytes=$position-${position + CHUNK - 1}")
                .build()
            Http.client.newCall(request).execute().use { response ->
                if (response.code == 416 && position > 0) {   // asked past the end: nothing left
                    done = true
                    return@use
                }
                if (!response.isSuccessful) throw HttpStatusException(response.code)
                val body = response.body ?: throw IOException("Empty response")
                val whole = response.code == 200             // server ignored the range: whole file at once
                if (whole && position > 0) throw IOException("Server ignored the range request")
                total = if (whole) body.contentLength() else totalFromContentRange(response.header("Content-Range")) ?: total
                var received = 0L
                val input = body.byteStream()
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    received += n
                    position += n
                    onProgress(position, total)
                    currentCoroutineContext().ensureActive()
                }
                done = whole || received == 0L || (total > 0 && position >= total) || (total <= 0 && received < CHUNK)
            }
        }
    }
}

// Reads the total size from a Content-Range header: a range ending in /1234 gives 1234; an unknown total or no header gives null.
internal fun totalFromContentRange(header: String?): Long? = header?.substringAfter('/', "")?.toLongOrNull()

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
