package com.intruphon.app.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchMergeTest {
    private fun entry(id: String, host: String = "www.youtube.com") =
        PlaylistEntry(url = "https://$host/watch?v=$id", title = id, artist = null, durationSeconds = 0, thumbnailUrl = null)

    @Test fun videoIdIsTheSameForBothHosts() {
        assertEquals("abcDEF12345", SearchMerge.videoId("https://music.youtube.com/watch?v=abcDEF12345"))
        assertEquals("abcDEF12345", SearchMerge.videoId("https://www.youtube.com/watch?v=abcDEF12345&t=3"))
        assertEquals(null, SearchMerge.videoId("https://example.com/other"))
    }

    @Test fun videosAndSongsTakeTurnsAVideoFirst() {
        val hits = SearchMerge.merge(listOf(entry("v1aaaaaa"), entry("v2aaaaaa")), listOf(entry("s1aaaaaa"), entry("s2aaaaaa")))
        assertEquals(listOf("v1aaaaaa", "s1aaaaaa", "v2aaaaaa", "s2aaaaaa"), hits.map { it.entry.title })
        assertEquals(listOf(false, true, false, true), hits.map { it.music })
    }

    @Test fun aTrackInBothListsShowsOnceAsASong() {
        val hits = SearchMerge.merge(listOf(entry("same1111"), entry("v2aaaaaa")), listOf(entry("same1111", "music.youtube.com")))
        assertEquals(listOf("same1111", "v2aaaaaa"), hits.map { it.entry.title })
        assertEquals(listOf(true, false), hits.map { it.music })
    }

    @Test fun oneSideOnlyKeepsItsKind() {
        assertEquals(listOf(false, false), SearchMerge.merge(listOf(entry("v1aaaaaa"), entry("v2aaaaaa")), emptyList()).map { it.music })
        assertEquals(listOf(true), SearchMerge.merge(emptyList(), listOf(entry("s1aaaaaa"))).map { it.music })
        assertEquals(emptyList<SearchHit>(), SearchMerge.merge(emptyList(), emptyList()))
    }
}
