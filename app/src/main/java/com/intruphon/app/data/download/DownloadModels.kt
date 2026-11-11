package com.intruphon.app.data.download

import com.intruphon.app.data.media.Kind

/** What the app remembers about a download it started (the system download manager holds the progress). */
data class DownloadRecord(
    val id: Long,
    val title: String,
    val kind: Kind,
    val extension: String,
    val quality: String,
    val sourceLink: String,
)

enum class DownloadState { WAITING, RUNNING, PAUSED, DONE, FAILED, MISSING }

/** [reason] is the HTTP status code (for example 403) or a download manager error code when [state] is FAILED. */
data class DownloadStatus(val state: DownloadState, val downloadedBytes: Long, val totalBytes: Long, val reason: Int)

data class DownloadItem(val record: DownloadRecord, val status: DownloadStatus)
