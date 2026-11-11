package com.intruphon.app.data.download

import com.intruphon.app.data.media.Kind

/** What the app remembers about a download it started. [uri] points to the saved file once it is done. */
data class DownloadRecord(
    val id: Long,
    val title: String,
    val kind: Kind,
    val extension: String,
    val quality: String,
    val sourceLink: String,
    val uri: String? = null,
)

enum class DownloadState { WAITING, RUNNING, DONE, FAILED }

/**
 * [reason] is meaningful when [state] is FAILED: an HTTP status code (for example 403),
 * [REASON_INTERRUPTED], [REASON_IO] or 0 for an unknown error.
 */
data class DownloadStatus(val state: DownloadState, val downloadedBytes: Long, val totalBytes: Long, val reason: Int)

data class DownloadItem(val record: DownloadRecord, val status: DownloadStatus)

const val REASON_INTERRUPTED = -1
const val REASON_IO = -2
