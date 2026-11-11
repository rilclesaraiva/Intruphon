package com.intruphon.app.features.downloads

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import com.intruphon.app.data.download.DownloadItem
import com.intruphon.app.data.download.DownloadState
import com.intruphon.app.data.download.DownloadEngine
import com.intruphon.app.data.download.REASON_INTERRUPTED
import com.intruphon.app.data.download.REASON_IO
import com.intruphon.app.data.media.Kind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Library of started downloads, refreshed every second while visible. [onRetry] receives the original link. */
@Composable
fun DownloadsScreen(onRetry: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val list by DownloadEngine.items.collectAsState()

    when {
        list.isEmpty() -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.downloads_empty))
        }
        else -> LazyColumn(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            items(list, key = { it.record.id }) { item ->
                DownloadRow(
                    item = item,
                    onOpen = {
                        if (!DownloadEngine.open(context, item.record.id)) {
                            Toast.makeText(context, R.string.downloads_cannot_open, Toast.LENGTH_LONG).show()
                        }
                    },
                    onRemove = { scope.launch(Dispatchers.IO) { DownloadEngine.remove(item.record.id) } },
                    onRetry = { onRetry(item.record.sourceLink) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onOpen: () -> Unit, onRemove: () -> Unit, onRetry: () -> Unit) {
    val record = item.record
    val status = item.status
    val inProgress = status.state == DownloadState.RUNNING || status.state == DownloadState.WAITING
    val percent = if (status.totalBytes > 0) (status.downloadedBytes * 100 / status.totalBytes).toInt() else null

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(record.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
        val kind = stringResource(if (record.kind == Kind.AUDIO) R.string.download_section_audio else R.string.download_section_video)
        Text(
            listOf(kind, record.extension.uppercase(), record.quality).filter { it.isNotEmpty() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
        )
        if (inProgress) {
            if (percent != null) LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = when (status.state) {
                DownloadState.WAITING -> stringResource(R.string.downloads_status_waiting)
                DownloadState.RUNNING ->
                    if (percent != null) stringResource(R.string.downloads_status_running, percent)
                    else stringResource(R.string.downloads_status_running_unknown)
                DownloadState.DONE -> stringResource(R.string.downloads_status_done)
                DownloadState.FAILED -> when {
                    status.reason in 100..599 -> stringResource(R.string.downloads_status_failed_http, status.reason)
                    status.reason == REASON_INTERRUPTED -> stringResource(R.string.downloads_status_interrupted)
                    status.reason == REASON_IO -> stringResource(R.string.downloads_status_failed_io)
                    else -> stringResource(R.string.downloads_status_failed, status.reason)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Row {
            when (status.state) {
                DownloadState.DONE -> {
                    TextButton(onClick = onOpen) { Text(stringResource(R.string.downloads_open)) }
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_delete)) }
                }
                DownloadState.FAILED -> {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.downloads_retry)) }
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_remove)) }
                }
                else -> TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_cancel)) }
            }
        }
    }
}
