package com.intruphon.app.features.downloads

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import java.util.Locale
import kotlinx.coroutines.withContext
import com.intruphon.app.data.download.MediaPreview
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.produceState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

    // Look for files deleted outside the app when the tab opens and whenever the app comes back to the front.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) DownloadEngine.checkFiles() }
        lifecycleOwner.lifecycle.addObserver(observer)
        DownloadEngine.checkFiles()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        list.isEmpty() -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.downloads_empty))
        }
        else -> LazyColumn(modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            items(list, key = { it.record.id }) { item ->
                DownloadRow(
                    item = item,
                    onOpen = {
                        when (DownloadEngine.open(context, item.record.id)) {
                            DownloadEngine.OpenResult.OPENED -> Unit
                            DownloadEngine.OpenResult.NO_APP ->
                                Toast.makeText(context, R.string.downloads_cannot_open, Toast.LENGTH_LONG).show()
                            DownloadEngine.OpenResult.MISSING ->
                                Toast.makeText(context, R.string.downloads_status_missing, Toast.LENGTH_LONG).show()
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
    val context = LocalContext.current
    val record = item.record
    val status = item.status
    val inProgress = status.state.active
    val percent = if (status.state == DownloadState.RUNNING && status.totalBytes > 0) (status.downloadedBytes * 100 / status.totalBytes).toInt() else null

    // Length and picture come from the saved file itself, read once off the main thread.
    val preview by produceState<MediaPreview?>(null, record.uri, status.state) {
        value = if (status.state == DownloadState.DONE && record.uri != null) {
            withContext(Dispatchers.IO) { MediaPreview.load(context, record.uri, record.kind) }
        } else null
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val thumbnail = preview?.thumbnail
            if (thumbnail != null) {
                Image(thumbnail.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(record.extension.uppercase(), style = MaterialTheme.typography.labelMedium)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            val kind = stringResource(if (record.kind == Kind.AUDIO) R.string.download_section_audio else R.string.download_section_video)
            val details = listOf(kind, record.extension.uppercase(), record.quality).filter { it.isNotEmpty() }.joinToString(" · ")
            val facts = if (status.state == DownloadState.DONE) {
                listOfNotNull(preview?.durationMs?.takeIf { it > 0 }?.let(::formatDuration), formatSize(status.totalBytes).takeIf { status.totalBytes > 0 })
            } else emptyList()
            Text((listOf(details) + facts).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
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
                    DownloadState.PROCESSING -> stringResource(R.string.downloads_status_processing)
                    DownloadState.DONE -> stringResource(R.string.downloads_status_done)
                    DownloadState.MISSING -> stringResource(R.string.downloads_status_missing)
                    DownloadState.FAILED -> when {
                        status.reason in 100..599 -> stringResource(R.string.downloads_status_failed_http, status.reason)
                        status.reason == REASON_INTERRUPTED -> stringResource(R.string.downloads_status_interrupted)
                        status.reason == REASON_IO -> stringResource(R.string.downloads_status_failed_io)
                        else -> stringResource(R.string.downloads_status_failed, status.reason)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (status.state == DownloadState.DONE && record.tags != null) {
                Text(
                    stringResource(if (record.tags) R.string.downloads_tags_ok else R.string.downloads_tags_failed),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row {
                when (status.state) {
                    DownloadState.DONE -> {
                        TextButton(onClick = onOpen) { Text(stringResource(R.string.downloads_open)) }
                        TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_delete)) }
                    }
                    DownloadState.FAILED, DownloadState.MISSING -> {
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.downloads_retry)) }
                        TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_remove)) }
                    }
                    else -> TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_cancel)) }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

private fun formatSize(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
