package com.intruphon.app.data.download

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore

/**
 * What the "Details" panel shows about a saved file, read from the file itself. Every field is null when it could not
 * be read (a folder that went offline, a format that does not say). Never throws.
 */
class FileDetails(
    val path: String?,
    val sizeBytes: Long?,
    val modifiedMs: Long?,
    val durationMs: Long?,
    /** Codecs of the tracks, for example "H.264 + AAC". */
    val codecs: String?,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val width: Int?,
    val height: Int?,
    val frameRate: Int?,
) {
    companion object {
        /** Reads the file. Call off the main thread. */
        fun load(context: Context, uriString: String): FileDetails {
            val uri = Uri.parse(uriString)
            var path: String? = null
            var size: Long? = null
            var modified: Long? = null
            try {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    // A folder the person picked (Storage Access Framework).
                    path = pathFromDocumentId(DocumentsContract.getDocumentId(uri))
                    val columns = arrayOf(DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    context.contentResolver.query(uri, columns, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            size = c.getLong(0).takeIf { !c.isNull(0) }
                            modified = c.getLong(1).takeIf { !c.isNull(1) && it > 0 }
                        }
                    }
                } else {
                    // The default public folder, through MediaStore.
                    val columns = arrayOf(
                        MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED,
                    )
                    context.contentResolver.query(uri, columns, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            path = (c.getString(0).orEmpty()) + (c.getString(1).orEmpty())
                            size = c.getLong(2)
                            modified = c.getLong(3).takeIf { it > 0 }?.times(1000)
                        }
                    }
                }
            } catch (_: Exception) {
            }

            var duration: Long? = null
            var bitrate: Int? = null
            var width: Int? = null
            var height: Int? = null
            var frames: Int? = null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                fun meta(key: Int) = retriever.extractMetadata(key)
                duration = meta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                bitrate = meta(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.div(1000)
                width = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                height = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                frames = meta(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()?.let { Math.round(it) }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }

            val codecs = ArrayList<String>()
            var sampleRate: Int? = null
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    codecs.add(codecName(mime))
                    if (mime.startsWith("audio/") && format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { extractor.release() }
            }

            return FileDetails(
                path = path?.takeIf { it.isNotBlank() },
                sizeBytes = size?.takeIf { it > 0 },
                modifiedMs = modified,
                durationMs = duration?.takeIf { it > 0 },
                codecs = codecs.distinct().joinToString(" + ").ifEmpty { null },
                bitrateKbps = bitrate?.takeIf { it > 0 },
                sampleRateHz = sampleRate,
                width = width?.takeIf { it > 0 },
                height = height?.takeIf { it > 0 },
                frameRate = frames?.takeIf { it > 0 },
            )
        }

        /** The readable place of a file in a picked folder: its document id without the volume ("primary:Music/a.m4a" gives "Music/a.m4a"). */
        internal fun pathFromDocumentId(id: String): String = id.substringAfter(':')

        /** A short name for a codec's MIME type; the MIME type itself when it is not a known one. */
        internal fun codecName(mime: String): String = when (mime.lowercase()) {
            "audio/mp4a-latm" -> "AAC"
            "audio/mpeg" -> "MP3"
            "audio/opus" -> "Opus"
            "audio/vorbis" -> "Vorbis"
            "audio/flac" -> "FLAC"
            "video/avc" -> "H.264"
            "video/hevc" -> "H.265"
            "video/x-vnd.on2.vp9" -> "VP9"
            "video/x-vnd.on2.vp8" -> "VP8"
            "video/av01" -> "AV1"
            else -> mime
        }
    }
}
