package com.intruphon.app.features.download

import com.intruphon.app.data.media.DownloadOption
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.media.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistFormatTest {
    private fun opt(id: String, kind: Kind, format: String, quality: String, size: Long?) =
        DownloadOption(id, kind, format, quality, "x", "x/x", size, Plan.Direct("u"))

    private val options = listOf(
        opt("m128", Kind.AUDIO, "M4A", "128 kbps", 1500),
        opt("m48", Kind.AUDIO, "M4A", "48 kbps", 600),
        opt("p128", Kind.AUDIO, "MP3", "128 kbps", 1400),
        opt("p64", Kind.AUDIO, "MP3", "64 kbps", 700),
        opt("v1080", Kind.VIDEO, "MP4", "1080p", 4800),
        opt("v360", Kind.VIDEO, "MP4", "360p", 900),
    )

    @Test
    fun audioRules() {
        assertEquals("m128", PlaylistFormat.AUDIO_HIGHEST.pick(options)?.id)
        assertEquals("m48", PlaylistFormat.AUDIO_SMALLEST.pick(options)?.id)
        assertEquals("p128", PlaylistFormat.MP3.pick(options)?.id)
    }

    @Test
    fun videoRules() {
        assertEquals("v1080", PlaylistFormat.VIDEO_HIGHEST.pick(options)?.id)
        assertEquals("v360", PlaylistFormat.VIDEO_SMALLEST.pick(options)?.id)
    }

    @Test
    fun noOptionOfThatKind() {
        assertNull(PlaylistFormat.VIDEO_HIGHEST.pick(options.filter { it.kind == Kind.AUDIO }))
        assertNull(PlaylistFormat.MP3.pick(options.filter { it.format != "MP3" }))
    }
}
