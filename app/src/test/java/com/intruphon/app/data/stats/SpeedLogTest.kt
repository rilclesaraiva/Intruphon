package com.intruphon.app.data.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedLogTest {
    private fun entry(format: String, quality: String, bytes: Long, ms: Long, ok: Boolean = true) =
        SpeedEntry(0, "AUDIO", format, quality, "direct", "wifi", bytes, ms, 0, ok, 0)

    @Test fun summaryWeightsByBytesAndSkipsNoise() {
        val list = listOf(
            entry("M4A", "128 kbps", 2_000_000, 1000),
            entry("M4A", "128 kbps", 4_000_000, 1000),
            entry("M4A", "128 kbps", 10, 10),                    // too small: ignored
            entry("M4A", "128 kbps", 5_000_000, 1000, ok = false), // failed: ignored
            entry("MP4", "720p", 8_000_000, 2000),
        )
        val summary = summarize(list)
        assertEquals(2, summary.size)
        assertEquals("M4A", summary[0].format)
        assertEquals(2, summary[0].count)
        assertEquals(3_000_000L, summary[0].bytesPerSecond)
        assertEquals(4_000_000L, summary[1].bytesPerSecond)
    }

    @Test fun csvHasHeaderAndEscapes() {
        val csv = toCsv(listOf(entry("M4A", "a,b", 1024, 1000)))
        val lines = csv.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("time_utc,kind"))
        assertTrue(lines[1].contains("\"a,b\""))
        assertTrue(lines[1].contains(",1,true,0"))   // 1 KB/s, ok, reason
    }

    @Test fun speedText() {
        assertEquals("2.0 MB/s", formatSpeed(2 * 1_048_576L))
        assertEquals("512 KB/s", formatSpeed(512 * 1024L))
    }
}
