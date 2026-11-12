package com.intruphon.app.features.downloads

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import com.intruphon.app.core.ui.PageHeader
import com.intruphon.app.data.download.DownloadEngine
import com.intruphon.app.data.download.DownloadItem
import com.intruphon.app.data.download.DownloadState
import com.intruphon.app.data.media.Kind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Everything that is downloading right now, with a cancel button on each one and "Cancel all" on top. */
@Composable
fun ActiveDownloadsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val items by DownloadEngine.items.collectAsState()
    val reading by DownloadEngine.reading.collectAsState()
    val active = items.filter { it.status.state.active }
    var confirmAll by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { PageHeader(stringResource(R.string.active_title), afterBack = true) }
        item(key = "bar") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.active_count, active.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(enabled = active.isNotEmpty() || reading, onClick = { confirmAll = true }) { Text(stringResource(R.string.active_cancel_all)) }
            }
        }
        if (reading) item(key = "reading") { Text(stringResource(R.string.active_reading), style = MaterialTheme.typography.bodyMedium) }
        if (active.isEmpty() && !reading) item(key = "empty") { Text(stringResource(R.string.active_empty), style = MaterialTheme.typography.bodyLarge) }
        items(active, key = { it.record.id }) { item ->
            ActiveRow(item) { scope.launch(Dispatchers.IO) { DownloadEngine.remove(item.record.id) } }
        }
    }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text(stringResource(R.string.active_cancel_all_title)) },
            text = { Text(stringResource(R.string.active_cancel_all_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmAll = false
                    scope.launch(Dispatchers.IO) { DownloadEngine.cancelAll() }
                }) { Text(stringResource(R.string.active_cancel_all), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text(stringResource(R.string.download_close)) } },
        )
    }
}

@Composable
private fun ActiveRow(item: DownloadItem, onCancel: () -> Unit) {
    val record = item.record
    val status = item.status
    val percent = if (status.state == DownloadState.RUNNING && status.totalBytes > 0) (status.downloadedBytes * 100 / status.totalBytes).toInt() else null
    val cover by rememberCover(record.coverUrl)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                val picture = cover
                if (picture != null) Image(picture.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text(record.extension.uppercase(), style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(record.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                val kind = stringResource(if (record.kind == Kind.AUDIO) R.string.download_section_audio else R.string.download_section_video)
                Text(listOf(kind, record.extension.uppercase(), record.quality).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                if (percent != null) LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = when (status.state) {
                        DownloadState.WAITING -> stringResource(R.string.downloads_status_waiting)
                        DownloadState.PROCESSING -> stringResource(R.string.downloads_status_processing)
                        else -> if (percent != null) stringResource(R.string.downloads_status_running, percent) else stringResource(R.string.downloads_status_running_unknown)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.downloads_cancel)) }
        }
    }
}
