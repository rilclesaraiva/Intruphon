package com.intruphon.app.data.download

import com.intruphon.app.data.media.TrackMetadata
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * Writes iTunes-style tags (title, artist, album, year, comment, cover) into an MP4/M4A file by
 * replacing the `udta` box inside `moov`. `moov` may come before the media data (Android's MediaMuxer
 * writes it there) or after it; when it comes first, the chunk offsets are shifted by the size change.
 * Fragmented files are not supported and throw.
 */
object Mp4Tags {
    private const val COVER_JPEG = 13
    private const val COVER_PNG = 14
    private val CONTAINERS = setOf("trak", "mdia", "minf", "stbl")

    fun write(file: File, meta: TrackMetadata, cover: ByteArray?) {
        val moovStart: Long
        val moovEnd: Long
        var mdatAfterMoov = false
        val oldBody: ByteArray
        RandomAccessFile(file, "r").use { raf ->
            val length = raf.length()
            var pos = 0L
            var foundStart = -1L
            var foundEnd = -1L
            var mdatStart = -1L
            while (pos < length) {
                raf.seek(pos)
                val size = raf.readInt().toLong() and 0xFFFFFFFFL
                val typeBytes = ByteArray(4)
                raf.readFully(typeBytes)
                if (size < 8) throw IllegalStateException("Unsupported MP4 box size")
                when (String(typeBytes, Charsets.ISO_8859_1)) {
                    "moov" -> { foundStart = pos; foundEnd = pos + size }
                    "mdat" -> if (mdatStart < 0) mdatStart = pos
                    "moof" -> throw IllegalStateException("Fragmented MP4 is not supported")
                }
                pos += size
            }
            if (pos != length || foundStart < 0) throw IllegalStateException("moov not found")
            moovStart = foundStart
            moovEnd = foundEnd
            mdatAfterMoov = mdatStart > moovStart
            raf.seek(moovStart + 8)
            oldBody = ByteArray((moovEnd - moovStart - 8).toInt())
            raf.readFully(oldBody)
        }

        // Keep every child of moov except an existing udta.
        val kept = ByteArrayOutputStream()
        var childPos = 0
        while (childPos + 8 <= oldBody.size) {
            val size = readInt(oldBody, childPos)
            if (size < 8 || childPos + size > oldBody.size) throw IllegalStateException("Broken moov")
            val type = String(oldBody, childPos + 4, 4, Charsets.ISO_8859_1)
            if (type != "udta") kept.write(oldBody, childPos, size)
            childPos += size
        }
        val keptBytes = kept.toByteArray()
        val udta = buildUdta(meta, cover)
        val newSize = 8 + keptBytes.size + udta.size
        val delta = newSize - (moovEnd - moovStart).toInt()
        if (mdatAfterMoov && delta != 0) shiftChunkOffsets(keptBytes, 0, keptBytes.size, delta)

        val moov = ByteArrayOutputStream()
        DataOutputStream(moov).apply {
            writeInt(newSize)
            write("moov".toByteArray(Charsets.ISO_8859_1))
            write(keptBytes)
            write(udta)
        }

        val temp = File(file.parentFile, file.name + ".tmp")
        try {
            RandomAccessFile(file, "r").use { src ->
                temp.outputStream().buffered().use { out ->
                    copyRange(src, 0, moovStart, out)
                    out.write(moov.toByteArray())
                    copyRange(src, moovEnd, src.length(), out)
                }
            }
            if (!file.delete() || !temp.renameTo(file)) throw IllegalStateException("Could not replace the file")
        } finally {
            temp.delete()
        }
    }

    private fun copyRange(src: RandomAccessFile, from: Long, to: Long, out: java.io.OutputStream) {
        val buffer = ByteArray(64 * 1024)
        var pos = from
        src.seek(from)
        while (pos < to) {
            val n = src.read(buffer, 0, minOf(buffer.size.toLong(), to - pos).toInt())
            if (n < 0) throw IllegalStateException("Unexpected end of file")
            out.write(buffer, 0, n)
            pos += n
        }
    }

    /** Adds [delta] to every entry of the stco and co64 tables found in [b] between [start] and [end]. */
    private fun shiftChunkOffsets(b: ByteArray, start: Int, end: Int, delta: Int) {
        var pos = start
        while (pos + 8 <= end) {
            val size = readInt(b, pos)
            if (size < 8 || pos + size > end) throw IllegalStateException("Broken moov")
            when (String(b, pos + 4, 4, Charsets.ISO_8859_1)) {
                in CONTAINERS -> shiftChunkOffsets(b, pos + 8, pos + size, delta)
                "stco" -> {
                    val count = readInt(b, pos + 12)
                    for (i in 0 until count) {
                        val at = pos + 16 + i * 4
                        writeInt(b, at, readInt(b, at) + delta)
                    }
                }
                "co64" -> {
                    val count = readInt(b, pos + 12)
                    for (i in 0 until count) {
                        val at = pos + 16 + i * 8
                        val value = (readInt(b, at).toLong() shl 32) or (readInt(b, at + 4).toLong() and 0xFFFFFFFFL)
                        val shifted = value + delta
                        writeInt(b, at, (shifted ushr 32).toInt())
                        writeInt(b, at + 4, shifted.toInt())
                    }
                }
            }
            pos += size
        }
    }

    private fun writeInt(b: ByteArray, at: Int, value: Int) {
        b[at] = (value ushr 24).toByte()
        b[at + 1] = (value ushr 16).toByte()
        b[at + 2] = (value ushr 8).toByte()
        b[at + 3] = value.toByte()
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
