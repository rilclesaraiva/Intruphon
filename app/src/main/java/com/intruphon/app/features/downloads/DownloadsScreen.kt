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
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import com.intruphon.app.core.ui.PageHeader
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
import com.intruphon.app.data.download.REASON_NEEDS_WIFI
import com.intruphon.app.data.media.Kind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Library of started downloads, refreshed every second while visible. [onRetry] receives the original link. */
@Composable
fun DownloadsScreen(onRetry: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val all by DownloadEngine.items.collectAsState()
    // Tabs: 0 = all (default), 1 = audio, 2 = video.
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val list = when (filter) {
        1 -> all.filter { it.record.kind == Kind.AUDIO }
        2 -> all.filter { it.record.kind == Kind.VIDEO }
        else -> all
    }
    // "Delete" asks first: forget the entry only, or also delete the file from the device (through the system).
    var confirmId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val deleteWindow = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val id = deletingId
        deletingId = null
        if (id != null) scope.launch {
            val gone = withContext(Dispatchers.IO) { DownloadEngine.finishDelete(context, id) }
            if (!gone && result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(context, R.string.downloads_delete_cancelled, Toast.LENGTH_SHORT).show()
            } else if (!gone) {
                Toast.makeText(context, R.string.downloads_delete_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Look for files deleted outside the app when the tab opens and whenever the app comes back to the front.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) DownloadEngine.checkFiles() }
        lifecycleOwner.lifecycle.addObserver(observer)
        DownloadEngine.checkFiles()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { PageHeader(stringResource(R.string.tab_downloads), afterBack = true) }
        item(key = "filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.downloads_filter_all, R.string.download_section_audio, R.string.download_section_video).forEachIndexed { index, label ->
                    FilterChip(
                        selected = filter == index,
                        onClick = { filter = index },
                        label = { Text(stringResource(label)) },
                        shape = CircleShape,
                    )
                }
            }
        }
        if (list.isEmpty()) {
            item(key = "empty") { Text(stringResource(R.string.downloads_empty), style = MaterialTheme.typography.bodyLarge) }
        }
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
                onRemove = {
                    if (item.status.state == DownloadState.DONE) confirmId = item.record.id
                    else scope.launch(Dispatchers.IO) { DownloadEngine.remove(item.record.id) }
                },
                onRetry = { onRetry(item.record.sourceLink) },
            )
        }
    }

    confirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            title = { Text(stringResource(R.string.downloads_delete_title)) },
            text = { Text(stringResource(R.string.downloads_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmId = null
                    scope.launch {
                        when (val step = withContext(Dispatchers.IO) { DownloadEngine.startDelete(context, id) }) {
                            is DownloadEngine.DeleteStep.Ask -> {
                                deletingId = id
                                deleteWindow.launch(IntentSenderRequest.Builder(step.sender).build())
                            }
                            DownloadEngine.DeleteStep.Gone -> withContext(Dispatchers.IO) { DownloadEngine.remove(id, deleteFile = false) }
                            DownloadEngine.DeleteStep.Failed ->
                                Toast.makeText(context, R.string.downloads_delete_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text(stringResource(R.string.downloads_delete_device), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        confirmId = null
                        scope.launch(Dispatchers.IO) { DownloadEngine.remove(id, deleteFile = false) }
                    }) { Text(stringResource(R.string.downloads_remove_list)) }
                    TextButton(onClick = { confirmId = null }) { Text(stringResource(R.string.downloads_cancel)) }
                }
            },
        )
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
    val preview by produceState(record.uri?.let { MediaPreview.peek(it) }, record.uri, status.state) {
        value = if (status.state == DownloadState.DONE && record.uri != null) {
            withContext(Dispatchers.IO) { MediaPreview.load(context, record.uri, record.kind) }
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
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
                        status.reason == REASON_NEEDS_WIFI -> stringResource(R.string.downloads_status_needs_wifi)
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
            if (record.note.isNotEmpty() && (status.state == DownloadState.DONE || status.state == DownloadState.FAILED)) {
                Text(record.note, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                when (status.state) {
                    DownloadState.DONE -> {
                        FilledTonalButton(onClick = onOpen) { Text(stringResource(R.string.downloads_open)) }
                        TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_delete)) }
                    }
                    DownloadState.FAILED, DownloadState.MISSING -> {
                        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.downloads_retry)) }
                        TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_remove)) }
                    }
                    else -> TextButton(onClick = onRemove) { Text(stringResource(R.string.downloads_cancel)) }
                }
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
