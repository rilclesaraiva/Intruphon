package com.intruphon.app.data.net

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import okhttp3.Request

/** Small pictures from the web (playlist items), kept in memory only. */
object Thumbnails {
    private const val TARGET = 320
    private val cache = LruCache<String, Bitmap>(150)

    /** The picture if it was already loaded, so a row can show it on its first frame. */
    fun peek(url: String): Bitmap? = cache.get(url)

    /** Downloads and shrinks the picture. Null when it cannot be read. Call off the main thread. */
    fun load(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        return runCatching {
            val request = Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build()
            val bytes = Http.client.newCall(request).execute().use { if (it.isSuccessful) it.body?.bytes() else null } ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply { inSampleSize = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / TARGET) }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }.getOrNull()?.also { cache.put(url, it) }
    }
}
