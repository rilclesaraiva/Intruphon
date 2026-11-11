package com.intruphon.app.data.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import com.intruphon.app.data.media.Kind

/** What the Downloads tab shows for a saved file: its length and a small square picture. */
class MediaPreview(val durationMs: Long, val thumbnail: Bitmap?) {
    companion object {
        private const val SIZE = 160
        private val cache = LruCache<String, MediaPreview>(64)

        /** Reads the saved file itself (cover art for audio, a frame for video). Null if the file cannot be read. Call off the main thread. */
        fun load(context: Context, uri: String, kind: Kind): MediaPreview? {
            cache.get(uri)?.let { return it }
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, Uri.parse(uri))
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val picture = if (kind == Kind.AUDIO) {
                    retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                } else {
                    retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                MediaPreview(duration, picture?.let(::squareThumbnail)).also { cache.put(uri, it) }
            } catch (e: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

        private fun squareThumbnail(source: Bitmap): Bitmap {
            val side = minOf(source.width, source.height)
            val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            return if (side > SIZE) Bitmap.createScaledBitmap(square, SIZE, SIZE, true) else square
        }
    }
}
