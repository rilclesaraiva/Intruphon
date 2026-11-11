package com.intruphon.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class QualityTagTest {
    @Test fun audio() = assertEquals("M4A_128K", qualityTag("M4A", "128 kbps"))
    @Test fun video() = assertEquals("MP4_720p60", qualityTag("MP4", "720p60"))
    @Test fun noQuality() = assertEquals("WEBM", qualityTag("WEBM", ""))
}
