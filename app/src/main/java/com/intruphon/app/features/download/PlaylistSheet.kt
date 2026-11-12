package com.intruphon.app.features.download

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.intruphon.app.R
import com.intruphon.app.data.download.DownloadEngine
import com.intruphon.app.data.media.FailReason
import com.intruphon.app.data.media.MediaRepository
import com.intruphon.app.data.media.PlaylistData
import com.intruphon.app.data.media.PlaylistEntry
import com.intruphon.app.data.media.PlaylistResult
import com.intruphon.app.data.net.Thumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Bottom sheet for a playlist link: pick the items and one format for all of them, then queue them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSheet(link: String, onDismiss: () -> Unit) {
    val result by produceState<PlaylistResult?>(initialValue = null, link) { value = MediaRepository.loadPlaylist(link) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when (val r = result) {
            null -> Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.playlist_loading), style = MaterialTheme.typography.titleMedium)
                CircularProgressIndicator()
            }
            is PlaylistResult.Failed -> Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (r.reason == FailReason.NETWORK) R.string.download_error_network else R.string.download_error_unreadable))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.download_close)) }
            }
            is PlaylistResult.Ready -> PlaylistChooser(r.playlist, onDismiss)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.PlaylistChooser(playlist: PlaylistData, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entries = playlist.entries
    // Indexes the person unchecked; everything starts selected.
    var unchecked by remember { mutableStateOf(emptySet<Int>()) }
    var format by rememberSaveable { mutableStateOf(PlaylistFormat.AUDIO_HIGHEST) }
    val count = entries.size - unchecked.size

    fun start() {
        val chosen = entries.filterIndexed { index, _ -> index !in unchecked }
        val selectedFormat = format
        DownloadEngine.enqueueAll(chosen, selectedFormat.kind) { selectedFormat.pick(it) }
        Toast.makeText(context, context.getString(R.string.playlist_started, chosen.size), Toast.LENGTH_LONG).show()
        onDismiss()
    }
    // Android 13+: ask once for permission to show the progress notification. Download starts either way.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { start() }

    Column(Modifier.fillMaxWidth().weight(1f, fill = false).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(playlist.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaylistFormat.entries.forEach { option ->
                FilterChip(selected = format == option, onClick = { format = option }, label = { Text(stringResource(option.label)) })
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable {
                unchecked = if (unchecked.isEmpty()) entries.indices.toSet() else emptySet()
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = unchecked.isEmpty(), onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Text(stringResource(R.string.playlist_select_all), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.playlist_count, entries.size), style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(entries, key = { index, _ -> index }) { index, entry ->
                PlaylistRow(index + 1, entry, checked = index !in unchecked) {
                    unchecked = if (index in unchecked) unchecked - index else unchecked + index
                }
            }
        }
        Button(
            enabled = count > 0,
            onClick = {
                val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                if (needsAsk) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) else start()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.playlist_download, count), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun PlaylistRow(number: Int, entry: PlaylistEntry, checked: Boolean, onToggle: () -> Unit) {
    val picture by produceState(entry.thumbnailUrl?.let { Thumbnails.peek(it) }, entry.thumbnailUrl) {
        val url = entry.thumbnailUrl
        if (url != null) value = withContext(Dispatchers.IO) { Thumbnails.load(url) }
    }
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 12.dp))
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            picture?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text("$number. ${entry.title}", style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(entry.artist, entry.durationSeconds.takeIf { it > 0 }?.let(::formatClock)).joinToString(" · ")
            if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatClock(seconds: Long): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}
