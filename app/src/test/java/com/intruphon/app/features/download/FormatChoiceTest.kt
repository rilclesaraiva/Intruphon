package com.intruphon.app.features.download

import com.intruphon.app.data.media.DownloadOption
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.media.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatChoiceTest {
    private fun opt(id: String, kind: Kind, format: String, quality: String, size: Long?) =
        DownloadOption(id, kind, format, quality, "x", "x/x", size, Plan.Direct("u"))

    @Test
    fun audioTakesHighestAndSmallest() {
        val list = listOf(
            opt("m128", Kind.AUDIO, "M4A", "128 kbps", 1500),
            opt("m48", Kind.AUDIO, "M4A", "48 kbps", 600),
            opt("p128", Kind.AUDIO, "MP3", "128 kbps", 1400),
            opt("p64", Kind.AUDIO, "MP3", "64 kbps", 700),
        )
        val p = picks(list)
        assertEquals(listOf("m128", "m48"), p.map { it.option.id })
        assertEquals(listOf(End.HIGHEST, End.SMALLEST), p.map { it.end })
    }

    @Test
    fun videoTakesHighestAndLowestResolution() {
        val list = listOf(
            opt("v1080", Kind.VIDEO, "MP4", "1080p", 4800),
            opt("v360", Kind.VIDEO, "MP4", "360p", 1300),
            opt("v720", Kind.VIDEO, "MP4", "720p", 3200),
        )
        assertEquals(listOf("v1080", "v360"), picks(list).map { it.option.id })
    }

    @Test
    fun oneChoiceHasNoLabel() {
        val p = picks(listOf(opt("a", Kind.AUDIO, "M4A", "128 kbps", 1)))
        assertEquals(1, p.size)
        assertNull(p[0].end)
    }

    @Test
    fun emptyListGivesNothing() = assertEquals(0, picks(emptyList()).size)
}
