package com.intruphon.app.features.home

import org.junit.Assert.assertEquals
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
}
