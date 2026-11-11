package com.intruphon.app.data.download

import com.intruphon.app.data.media.TrackMetadata
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/** Builds an ID3v2.3 tag (title, artist, album, year, comment, cover) to put in front of an MP3 stream. */
object Id3v2 {
    private val BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())

    fun build(meta: TrackMetadata, cover: ByteArray?): ByteArray {
        val frames = ByteArrayOutputStream()
        frames.write(textFrame("TIT2", meta.title))
        meta.artist?.let {
            frames.write(textFrame("TPE1", it))
            frames.write(textFrame("TPE2", it))
        }
        meta.album?.let { frames.write(textFrame("TALB", it)) }
        meta.year?.let { frames.write(textFrame("TYER", it.toString())) }
        meta.sourceUrl?.let { frames.write(commentFrame(it)) }
        if (cover != null && cover.isNotEmpty()) frames.write(coverFrame(cover))
        val body = frames.toByteArray()

        val out = ByteArrayOutputStream()
        out.write("ID3".toByteArray(Charsets.ISO_8859_1))
        out.write(byteArrayOf(3, 0, 0))                      // version 2.3.0, no flags
        out.write(syncSafe(body.size))
        out.write(body)
        return out.toByteArray()
    }

    private fun syncSafe(n: Int) = byteArrayOf(
        ((n shr 21) and 0x7F).toByte(), ((n shr 14) and 0x7F).toByte(), ((n shr 7) and 0x7F).toByte(), (n and 0x7F).toByte(),
    )

    private fun utf16(text: String): ByteArray = BOM + text.toByteArray(Charsets.UTF_16LE)

    private fun frame(id: String, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            write(id.toByteArray(Charsets.ISO_8859_1))
            writeInt(payload.size)
            writeShort(0)                                      // frame flags
            write(payload)
        }
        return out.toByteArray()
    }

    private fun textFrame(id: String, text: String) = frame(id, byteArrayOf(1) + utf16(text))   // 1 = UTF-16 with BOM

    private fun commentFrame(text: String) =
        frame("COMM", byteArrayOf(1) + "eng".toByteArray(Charsets.ISO_8859_1) + BOM + byteArrayOf(0, 0) + utf16(text))

    private fun coverFrame(image: ByteArray): ByteArray {
        val png = image.size > 3 && image[0] == 0x89.toByte() && image[1] == 'P'.code.toByte()
        val mime = if (png) "image/png" else "image/jpeg"
        // encoding 0 (Latin-1), mime type, picture type 3 (front cover), empty description
        return frame("APIC", byteArrayOf(0) + mime.toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0, 3, 0) + image)
    }
}
