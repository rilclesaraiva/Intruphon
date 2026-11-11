package com.intruphon.app.data.media

enum class Kind { AUDIO, VIDEO }

enum class Container { MP4, WEBM }

/** How a chosen option is turned into a file on the device. */
sealed interface Plan {
    /** One ready-made file, saved as it is. */
    data class Direct(val url: String) : Plan

    /** A video-only file and an audio-only file, joined into one file. */
    data class Mux(val videoUrl: String, val audioUrl: String, val container: Container) : Plan

    /** Audio saved as a clean M4A file with tags and cover. */
    data class AudioM4a(val url: String) : Plan
}

/** One thing the user can pick. [format] and [quality] are short labels such as "M4A" and "128 kbps". */
data class DownloadOption(
    val id: String,
    val kind: Kind,
    val format: String,
    val quality: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val plan: Plan,
)

/** Tags written into audio files. [coverUrl] is fetched only when the file is tagged. */
data class TrackMetadata(
    val title: String,
    val artist: String?,
    val album: String?,
    val year: Int?,
    val sourceUrl: String?,
    val coverUrl: String?,
)

data class MediaInfo(val title: String, val metadata: TrackMetadata, val options: List<DownloadOption>)

enum class FailReason { NETWORK, UNREADABLE, NO_FORMATS }

sealed interface LoadResult {
    data class Ready(val info: MediaInfo) : LoadResult
    data class Failed(val reason: FailReason) : LoadResult
}
