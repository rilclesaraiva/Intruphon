package com.intruphon.app.core

/** Finds the first web link inside text shared by other apps (which often add a title or a caption). */
object LinkExtractor {
    private val url = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    fun firstUrl(text: String?): String? =
        text?.let { url.find(it)?.value?.trimEnd('.', ',', ';', ')', '"', '\'') }

    private val playlist = Regex("""^https?://(www\.|m\.|music\.)?youtube\.com/playlist\?(\S*&)?list=""", RegexOption.IGNORE_CASE)

    /** True for a link that points at a whole playlist (not a video that merely carries a list id). */
    fun isPlaylist(link: String): Boolean = playlist.containsMatchIn(link)

    private const val MAX_QUERY = 200

    /**
     * The text to search for when [text] has no link in it (a song name typed or shared): spaces tidied and cut to a
     * sane length; null if there is nothing to search or the text holds a link (that goes to the downloader).
     */
    fun searchQuery(text: String?): String? {
        if (text == null || firstUrl(text) != null) return null
        val tidy = text.trim().replace(Regex("""\s+"""), " ").take(MAX_QUERY).trim()
        return tidy.ifEmpty { null }
    }
}
