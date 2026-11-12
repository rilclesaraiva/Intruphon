package com.intruphon.app.core

/** Finds the first web link inside text shared by other apps (which often add a title or a caption). */
object LinkExtractor {
    private val url = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    fun firstUrl(text: String?): String? =
        text?.let { url.find(it)?.value?.trimEnd('.', ',', ';', ')', '"', '\'') }

    private val playlist = Regex("""^https?://(www\.|m\.|music\.)?youtube\.com/playlist\?(\S*&)?list=""", RegexOption.IGNORE_CASE)

    /** True for a link that points at a whole playlist (not a video that merely carries a list id). */
    fun isPlaylist(link: String): Boolean = playlist.containsMatchIn(link)
}
