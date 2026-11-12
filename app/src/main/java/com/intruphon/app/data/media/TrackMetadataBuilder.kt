package com.intruphon.app.data.media

private val RELEASED_ON = Regex("""Released on:\s*(\d{4})""")

/**
 * Builds clean tags from what a site tells us: the channel name usually is the artist (YouTube adds
 * " - Topic" to auto-generated music channels), and those channels carry a "Provided to YouTube by"
 * description that names the album and release year. When the site gives no album, the album is the title.
 */
fun buildTrackMetadata(
    rawTitle: String,
    uploader: String?,
    description: String?,
    uploadYear: Int?,
    sourceUrl: String?,
    coverUrl: String?,
): TrackMetadata {
    val artist = uploader?.trim()?.removeSuffix("- Topic")?.trim()?.ifEmpty { null }
    val title = cleanTitle(rawTitle.trim(), artist)
    var album: String? = null
    var year: Int? = null
    if (description != null && description.trimStart().startsWith("Provided to YouTube by")) {
        val lines = description.lines().map { it.trim() }
        val songLine = lines.indexOfFirst { " · " in it }
        if (songLine >= 0) album = lines.drop(songLine + 1).firstOrNull { it.isNotEmpty() }
        year = RELEASED_ON.find(description)?.groupValues?.get(1)?.toIntOrNull()
    }
    val finalTitle = title.ifEmpty { rawTitle }
    // Without an album tag, players group the file under its folder name ("Audio"); the title is the honest fallback.
    return TrackMetadata(finalTitle, artist, album ?: finalTitle, year ?: uploadYear, sourceUrl, coverUrl)
}

/** Drops an artist that the title repeats, as in "Title — Artist" or "Artist - Title". */
private fun cleanTitle(title: String, artist: String?): String {
    if (artist == null) return title
    for (separator in listOf(" — ", " – ", " - ")) {
        if (title.endsWith("$separator$artist", ignoreCase = true)) return title.dropLast(separator.length + artist.length).trim()
        if (title.startsWith("$artist$separator", ignoreCase = true)) return title.drop(artist.length + separator.length).trim()
    }
    return title
}
