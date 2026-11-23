package com.intruphon.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class LinkExtractorTest {
    @Test fun plainLink() {
        val link = "https://music.youtube.com/watch?v=hCUBILjVcPs&drli=1"
        assertEquals(link, LinkExtractor.firstUrl(link))
    }

    @Test fun linkInsideText() {
        assertEquals("https://example.com/a?b=1", LinkExtractor.firstUrl("Look at this: https://example.com/a?b=1 so good"))
    }

    @Test fun trailingPunctuationIsDropped() {
        assertEquals("https://example.com/x", LinkExtractor.firstUrl("See https://example.com/x."))
    }

    @Test fun noLink() {
        assertNull(LinkExtractor.firstUrl("just words"))
        assertNull(LinkExtractor.firstUrl(null))
    }

    @Test
    fun playlistLinks() {
        assertTrue(LinkExtractor.isPlaylist("https://music.youtube.com/playlist?list=PLdpRY1jkvxrktNlut1aOcwYxwbQdK1fdn&drli=1"))
        assertTrue(LinkExtractor.isPlaylist("https://www.youtube.com/playlist?list=PL123"))
        assertFalse(LinkExtractor.isPlaylist("https://www.youtube.com/watch?v=abc&list=PL123"))
        assertFalse(LinkExtractor.isPlaylist("https://example.com/playlist?list=1"))
    }

    @Test fun searchQueryIsTidiedText() {
        assertEquals("daft punk one more time", LinkExtractor.searchQuery("  daft   punk\none more time "))
    }

    @Test fun searchQueryIsNullWhenThereIsNothingToSearch() {
        assertNull(LinkExtractor.searchQuery(null))
        assertNull(LinkExtractor.searchQuery("   \n "))
    }

    @Test fun searchQueryIsNullWhenTheTextHasALink() {
        assertNull(LinkExtractor.searchQuery("listen https://example.com/x now"))
    }

    @Test fun searchQueryIsCutToALimit() {
        assertEquals(200, LinkExtractor.searchQuery("a".repeat(500))!!.length)
    }
}
