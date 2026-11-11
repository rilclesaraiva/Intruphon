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
}
