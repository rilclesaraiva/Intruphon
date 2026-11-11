package com.intruphon.app.data.download

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.intruphon.app.data.net.Http
import okhttp3.Request
import java.io.ByteArrayOutputStream

/** Downloads a thumbnail and returns it as a square JPEG (cropped from the centre), or null on any problem. */
internal object CoverArt {
    private const val MAX_SIDE = 600

    fun fetch(url: String?): ByteArray? {
        if (url.isNullOrEmpty()) return null
        return try {
            val bytes = Http.client.newCall(Request.Builder().url(url).header("User-Agent", Http.USER_AGENT).build())
                .execute().use { if (it.isSuccessful) it.body?.bytes() else null } ?: return null
            val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val side = minOf(source.width, source.height)
            val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
            val scaled = if (side > MAX_SIDE) Bitmap.createScaledBitmap(square, MAX_SIDE, MAX_SIDE, true) else square
            ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
