package com.intruphon.app.data.download

import com.intruphon.app.data.media.TrackMetadata
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * Writes iTunes-style tags (title, artist, album, year, comment, cover) into an MP4/M4A file by
 * replacing the `udta` box at the end of `moov`. Only files whose `moov` is the last box are handled,
 * which is how Android's MediaMuxer and YouTube's M4A files are laid out; anything else throws.
 */
object Mp4Tags {
    private const val COVER_JPEG = 13
    private const val COVER_PNG = 14

    fun write(file: File, meta: TrackMetadata, cover: ByteArray?) {
        RandomAccessFile(file, "rw").use { raf ->
            val length = raf.length()
            // Walk the top-level boxes to find the last one.
            var pos = 0L
            var lastStart = -1L
            var lastType = ""
            while (pos < length) {
                raf.seek(pos)
                val size = raf.readInt().toLong() and 0xFFFFFFFFL
                val typeBytes = ByteArray(4)
                raf.readFully(typeBytes)
                if (size < 8) throw IllegalStateException("Unsupported MP4 box size")
                lastStart = pos
                lastType = String(typeBytes, Charsets.ISO_8859_1)
                pos += size
            }
            if (pos != length || lastType != "moov") throw IllegalStateException("moov is not the last box")

            raf.seek(lastStart + 8)
            val body = ByteArray((length - lastStart - 8).toInt())
            raf.readFully(body)

            // Drop an existing udta when it is the last child of moov.
            var keep = body.size
            var childPos = 0
            while (childPos + 8 <= body.size) {
                val size = readInt(body, childPos)
                if (size < 8 || childPos + size > body.size) throw IllegalStateException("Broken moov")
                val type = String(body, childPos + 4, 4, Charsets.ISO_8859_1)
                if (type == "udta" && childPos + size == body.size) keep = childPos
                childPos += size
            }

            val udta = buildUdta(meta, cover)
            val newSize = 8 + keep + udta.size
            val out = ByteArrayOutputStream()
            DataOutputStream(out).apply {
                writeInt(newSize)
                write("moov".toByteArray(Charsets.ISO_8859_1))
                write(body, 0, keep)
                write(udta)
            }
            raf.setLength(lastStart)
            raf.seek(lastStart)
            raf.write(out.toByteArray())
        }
    }

    private fun readInt(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xFF) shl 24) or ((b[at + 1].toInt() and 0xFF) shl 16) or
            ((b[at + 2].toInt() and 0xFF) shl 8) or (b[at + 3].toInt() and 0xFF)

    private fun box(type: String, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(8 + payload.size)
            write(type.toByteArray(Charsets.ISO_8859_1))
            write(payload)
        }
        return out.toByteArray()
    }

    private fun dataBox(flags: Int, value: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(flags)
            writeInt(0)
            write(value)
        }
        return box("data", out.toByteArray())
    }

    // Item names start with the byte 0xA9, so they are built from ISO-8859-1 text.
    private fun textItem(name: String, text: String): ByteArray =
        box(name, dataBox(1, text.toByteArray(Charsets.UTF_8)))

    private fun buildUdta(meta: TrackMetadata, cover: ByteArray?): ByteArray {
        val items = ByteArrayOutputStream()
        items.write(textItem("©nam", meta.title))
        meta.artist?.let {
            items.write(textItem("©ART", it))
            items.write(textItem("aART", it))
        }
        meta.album?.let { items.write(textItem("©alb", it)) }
        meta.year?.let { items.write(textItem("©day", it.toString())) }
        meta.sourceUrl?.let { items.write(textItem("©cmt", it)) }
        if (cover != null && cover.isNotEmpty()) {
            val flags = if (cover.size > 3 && cover[0] == 0x89.toByte() && cover[1] == 'P'.code.toByte()) COVER_PNG else COVER_JPEG
            items.write(box("covr", dataBox(flags, cover)))
        }
        val ilst = box("ilst", items.toByteArray())

        val hdlrPayload = ByteArrayOutputStream()
        DataOutputStream(hdlrPayload).apply {
            writeInt(0); writeInt(0)
            write("mdir".toByteArray(Charsets.ISO_8859_1))
            write("appl".toByteArray(Charsets.ISO_8859_1))
            writeInt(0); writeInt(0)
            write(0)
        }
        val metaPayload = ByteArrayOutputStream()
        DataOutputStream(metaPayload).apply {
            writeInt(0)
            write(box("hdlr", hdlrPayload.toByteArray()))
            write(ilst)
        }
        return box("udta", box("meta", metaPayload.toByteArray()))
    }
}
