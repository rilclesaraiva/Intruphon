package com.intruphon.app.data.download

import com.intruphon.app.data.media.TrackMetadata
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp4TagsTest {
    private val meta = TrackMetadata("Título Ç", "Artista", "Álbum", 2019, "https://example.com/v", null)

    private fun fixture(name: String): File {
        val bytes = javaClass.classLoader!!.getResourceAsStream(name)!!.readBytes()
        return File.createTempFile("tags", ".m4a").also { it.writeBytes(bytes); it.deleteOnExit() }
    }

    private fun contains(hay: ByteArray, needle: ByteArray): Boolean =
        (0..hay.size - needle.size).any { i -> needle.indices.all { hay[i + it] == needle[it] } }

    private fun topLevel(b: ByteArray): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        var pos = 0
        while (pos < b.size) {
            val size = ((b[pos].toInt() and 0xFF) shl 24) or ((b[pos + 1].toInt() and 0xFF) shl 16) or
                ((b[pos + 2].toInt() and 0xFF) shl 8) or (b[pos + 3].toInt() and 0xFF)
            result += String(b, pos + 4, 4, Charsets.ISO_8859_1) to size
            pos += size
        }
        assertEquals(b.size, pos)
        return result
    }

    private fun check(name: String, cover: ByteArray?) {
        val f = fixture(name)
        Mp4Tags.write(f, meta, cover)
        val out = f.readBytes()
        assertEquals("moov", topLevel(out).last().first)
        assertTrue(contains(out, "Título Ç".toByteArray(Charsets.UTF_8)))
        assertTrue(contains(out, "Álbum".toByteArray(Charsets.UTF_8)))
        assertTrue(contains(out, "2019".toByteArray()))
        assertTrue(contains(out, "ilst".toByteArray()))
        if (cover != null) assertTrue(contains(out, "covr".toByteArray()))
        // Writing twice must replace the tags, not stack them.
        Mp4Tags.write(f, meta, cover)
        assertEquals(out.size, f.readBytes().size)
    }

    @Test fun fileWithoutExistingTags() = check("plain.m4a", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3))

    @Test fun fileWithExistingTags() = check("with_udta.m4a", null)

    private fun indexOf(hay: ByteArray, text: String): Int {
        val needle = text.toByteArray(Charsets.ISO_8859_1)
        return (0..hay.size - needle.size).first { i -> needle.indices.all { hay[i + it] == needle[it] } }
    }

    private fun firstChunkOffset(b: ByteArray): Int {
        val at = indexOf(b, "stco") + 12   // type, version/flags, count, then the first offset
        return ((b[at].toInt() and 0xFF) shl 24) or ((b[at + 1].toInt() and 0xFF) shl 16) or
            ((b[at + 2].toInt() and 0xFF) shl 8) or (b[at + 3].toInt() and 0xFF)
    }

    // moov before mdat (what Android's MediaMuxer writes): the chunk offsets must still point at the same audio bytes.
    @Test fun moovBeforeMediaDataKeepsChunkOffsets() {
        val f = fixture("faststart.m4a")
        val before = f.readBytes()
        assertEquals("moov", topLevel(before)[1].first)
        val sample = before.copyOfRange(firstChunkOffset(before), firstChunkOffset(before) + 16)
        repeat(2) {
            Mp4Tags.write(f, meta, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9))
            val after = f.readBytes()
            topLevel(after)
            assertTrue(contains(after, "Título Ç".toByteArray(Charsets.UTF_8)))
            val offset = firstChunkOffset(after)
            assertTrue(after.copyOfRange(offset, offset + 16).contentEquals(sample))
        }
    }

    // Fragmented MP4 (what the web serves as DASH audio): the fragments must come through byte for byte.
    @Test fun fragmentedFileKeepsItsFragments() {
        val f = fixture("fragmented.m4a")
        val before = f.readBytes()
        val boxesBefore = topLevel(before)
        val tailBefore = before.copyOfRange(boxesBefore[0].second + boxesBefore[1].second, before.size)
        Mp4Tags.write(f, meta, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9))
        val after = f.readBytes()
        val boxesAfter = topLevel(after)
        assertEquals(boxesBefore.map { it.first }, boxesAfter.map { it.first })
        val moovEnd = boxesAfter[0].second + boxesAfter[1].second
        assertTrue(after.copyOfRange(moovEnd, after.size).contentEquals(tailBefore))
        assertTrue(contains(after, "Título Ç".toByteArray(Charsets.UTF_8)))
    }
}
