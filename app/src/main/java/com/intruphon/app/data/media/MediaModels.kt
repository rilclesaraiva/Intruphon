package com.intruphon.app.data.media

enum class Kind { AUDIO, VIDEO }

/** One downloadable file offered for a link. [quality] is a short label such as "128 kbps" or "360p". */
data class DownloadOption(
    val id: String,
    val kind: Kind,
    val quality: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val url: String,
)

data class MediaInfo(val title: String, val options: List<DownloadOption>)

enum class FailReason { NETWORK, UNREADABLE, NO_FORMATS }

sealed interface LoadResult {
    data class Ready(val info: MediaInfo) : LoadResult
    data class Failed(val reason: FailReason) : LoadResult
}
