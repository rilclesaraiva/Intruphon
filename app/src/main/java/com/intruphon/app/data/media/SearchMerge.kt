package com.intruphon.app.data.media

/** Joins the video results and the song results of one search into the single list of the "All" tab. */
object SearchMerge {
    private val ID = Regex("""[?&]v=([\w-]{6,})""")

    /** The video id of a watch link (the same for youtube.com and music.youtube.com), or null. */
    fun videoId(url: String): String? = ID.find(url)?.groupValues?.get(1)

    /**
     * Videos and songs take turns, a video first. A track that is in both lists shows once, as a song (audio).
     * Without songs every hit is a video, and the other way round.
     */
    fun merge(videos: List<PlaylistEntry>, songs: List<PlaylistEntry>): List<SearchHit> {
        val songIds = songs.mapNotNull { videoId(it.url) }.toSet()
        val seen = HashSet<String>()
        val out = ArrayList<SearchHit>(videos.size + songs.size)
        fun add(entry: PlaylistEntry, music: Boolean) {
            if (seen.add(videoId(entry.url) ?: entry.url)) out.add(SearchHit(entry, music))
        }
        for (i in 0 until maxOf(videos.size, songs.size)) {
            videos.getOrNull(i)?.let { add(it, music = videoId(it.url) in songIds) }
            songs.getOrNull(i)?.let { add(it, music = true) }
        }
        return out
    }
}
