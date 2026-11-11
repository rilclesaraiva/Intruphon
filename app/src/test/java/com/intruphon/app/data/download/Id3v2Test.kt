package com.intruphon.app.data.download

import com.intruphon.app.data.media.TrackMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Id3v2Test {
    private fun contains(hay: ByteArray, needle: ByteArray): Boolean =
        (0..hay.size - needle.size).any { i -> needle.indices.all { hay[i + it] == needle[it] } }

    @Test fun headerSizeMatchesFrames() {
        val tag = Id3v2.build(TrackMetadata("Título Ç", "Artista", "Álbum", 2019, "https://example.com", null), byteArrayOf(1, 2, 3))
        assertEquals("ID3", String(tag, 0, 3, Charsets.ISO_8859_1))
        assertEquals(3, tag[3].toInt())
        val size = (tag[6].toInt() shl 21) or (tag[7].toInt() shl 14) or (tag[8].toInt() shl 7) or tag[9].toInt()
        assertEquals(tag.size - 10, size)
        for (id in listOf("TIT2", "TPE1", "TPE2", "TALB", "TYER", "COMM", "APIC")) {
            assertTrue(id, contains(tag, id.toByteArray(Charsets.ISO_8859_1)))
        }
        val title = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "Título Ç".toByteArray(Charsets.UTF_16LE)
        assertTrue(contains(tag, title))
    }

    @Test fun onlyTitleWhenNothingElseIsKnown() {
        val tag = Id3v2.build(TrackMetadata("Just a title", null, null, null, null, null), null)
        assertTrue(contains(tag, "TIT2".toByteArray()))
        assertTrue(!contains(tag, "TPE1".toByteArray()))
        assertTrue(!contains(tag, "APIC".toByteArray()))
    }
}
