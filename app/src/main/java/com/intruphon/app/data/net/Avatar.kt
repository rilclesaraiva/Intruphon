package com.intruphon.app.data.net

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.Request
import java.io.File

/** The maintainer's public GitHub profile picture, kept in the app's cache for a week so the network is rarely used. */
object Avatar {
    private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

    /** Returns the picture, or null when there is none yet and the network is not available. Call off the main thread. */
    fun load(context: Context, user: String): Bitmap? {
        val file = File(context.cacheDir, "avatar_$user.img")
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < MAX_AGE_MS
        if (!fresh) {
            runCatching {
                val request = Request.Builder().url("https://github.com/$user.png?size=200").header("User-Agent", "Intruphon").build()
                Http.client.newCall(request).execute().use { response ->
                    val bytes = if (response.isSuccessful) response.body?.bytes() else null
                    // Keep it only if it really decodes as a picture.
                    if (bytes != null && BitmapFactory.decodeByteArray(bytes, 0, bytes.size) != null) file.writeBytes(bytes)
                }
            }
        }
        return if (file.exists()) runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() else null
    }
}
