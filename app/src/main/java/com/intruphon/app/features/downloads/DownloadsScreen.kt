package com.intruphon.app.features.downloads

import android.app.Activity
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.intruphon.app.R
import com.intruphon.app.core.ui.BottomPanelFlow
import com.intruphon.app.core.ui.PanelBody
import com.intruphon.app.core.ui.PanelAction
import com.intruphon.app.core.ui.PageHeader
import com.intruphon.app.data.download.DownloadEngine
import com.intruphon.app.data.download.DownloadItem
import com.intruphon.app.data.download.DownloadState
import com.intruphon.app.data.download.MediaPreview
import com.intruphon.app.data.download.REASON_INTERRUPTED
import com.intruphon.app.data.download.REASON_IO
import com.intruphon.app.data.download.REASON_NEEDS_WIFI
import com.intruphon.app.data.download.REASON_NO_FORMAT
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.net.Thumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Library of started downloads. Tap a row to open it; hold it (or tap its dots) for more; tap a cover to start picking
 * several. [onRetry] receives the original link, [onSelecting] tells the entry when the picking bar is up.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(onRetry: (String) -> Unit, onOpenActive: () -> Unit, onSelecting: (Boolean) -> Unit, modifier: Modifier = Modifier) {
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
    val active = list.filter { it.status.state.active }
    val failed = list.filter { it.status.state == DownloadState.FAILED }
    val rest = list.filter { !it.status.state.active && it.status.state != DownloadState.FAILED }
    val pickable = (failed + rest).map { it.record.id }

    // Picking several: starts from a cover; the bar at the top shows how many and what to do.
    var selected by rememberSaveable { mutableStateOf<Set<Long>>(emptySet()) }
    val selecting = selected.isNotEmpty()
    LaunchedEffect(selecting) { onSelecting(selecting) }
    DisposableEffect(Unit) { onDispose { onSelecting(false) } }
    LaunchedEffect(all) {
        val ids = all.map { it.record.id }.toSet()
        if (selected.any { it !in ids }) selected = selected.filter { it in ids }.toSet()
    }
    BackHandler(enabled = selecting) { selected = emptySet() }
    fun toggle(id: Long) { selected = if (id in selected) selected - id else selected + id }

    // The panel with the other things to do with one download.
    // The one panel: which downloads it is about, and its step (0 actions, 1 delete question, 2 confirmation).
    var panelIds by rememberSaveable { mutableStateOf<List<Long>?>(null) }
    var panelStep by rememberSaveable { mutableStateOf(0) }
    // Deleting asks twice: first remove from the list or delete from the device, then the confirmation window.
    var deletingIds by rememberSaveable { mutableStateOf<List<Long>?>(null) }
    val deleteWindow = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val ids = deletingIds
        deletingIds = null
        if (ids != null) scope.launch {
            val gone = withContext(Dispatchers.IO) { DownloadEngine.finishDelete(context, ids) }
            if (!gone && result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(context, R.string.downloads_delete_cancelled, Toast.LENGTH_SHORT).show()
            } else if (!gone) {
                Toast.makeText(context, R.string.downloads_delete_failed, Toast.LENGTH_LONG).show()
            }
            selected = emptySet()
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

    fun open(item: DownloadItem) {
        when (DownloadEngine.open(context, item.record.id)) {
            DownloadEngine.OpenResult.OPENED -> Unit
            DownloadEngine.OpenResult.NO_APP -> Toast.makeText(context, R.string.downloads_cannot_open, Toast.LENGTH_LONG).show()
            DownloadEngine.OpenResult.MISSING -> Toast.makeText(context, R.string.downloads_status_missing, Toast.LENGTH_LONG).show()
        }
    }

    // What a tap does: pick (while picking), open a saved file, retry a failed one.
    fun tap(item: DownloadItem) {
        val id = item.record.id
        when {
            selecting -> toggle(id)
            item.status.state == DownloadState.DONE -> open(item)
            item.status.state == DownloadState.FAILED -> onRetry(item.record.sourceLink)
            item.status.state == DownloadState.MISSING -> Toast.makeText(context, R.string.downloads_status_missing, Toast.LENGTH_LONG).show()
        }
    }

    // Deleting from the device only makes sense for files that are still there: the ones already gone are just dropped from the list.
    fun askDelete(ids: List<Long>) {
        scope.launch {
            val present = DownloadEngine.stillOnDevice(ids)
            val gone = ids.filterNot { it in present }
            if (gone.isNotEmpty()) withContext(Dispatchers.IO) { gone.forEach { DownloadEngine.remove(it, deleteFile = false) } }
            if (present.isEmpty()) { selected = emptySet(); panelIds = null } else { panelIds = present; panelStep = 1 }
        }
    }

    @Composable
    fun ItemRow(item: DownloadItem) = DownloadRow(
        item = item,
        picked = item.record.id in selected,
        onTap = { tap(item) },
        onHold = { panelIds = listOf(item.record.id); panelStep = 0 },
        onCover = { toggle(item.record.id) },
        onMore = { panelIds = listOf(item.record.id); panelStep = 0 },
    )

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
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
            // Everything that is running sits in one card; it opens the page where each one can be cancelled.
            if (active.isNotEmpty()) item(key = "active") { ActiveCard(active, onOpenActive) }
            // Failures pile up here, at the top, until they are retried or removed.
            if (failed.isNotEmpty()) {
                item(key = "failed-head") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.downloads_failed_title, failed.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { scope.launch(Dispatchers.IO) { failed.forEach { DownloadEngine.remove(it.record.id) } } }) {
                            Text(stringResource(R.string.downloads_failed_remove_all))
                        }
                    }
                }
                items(failed, key = { it.record.id }) { ItemRow(it) }
            }
            if (rest.isNotEmpty()) {
                if (active.isNotEmpty() || failed.isNotEmpty()) {
                    item(key = "done-head") { Text(stringResource(R.string.downloads_done_title), style = MaterialTheme.typography.titleMedium) }
                }
                items(rest, key = { it.record.id }) { ItemRow(it) }
            }
        }

        if (selecting) {
            SelectionBar(
                count = selected.size,
                onClose = { selected = emptySet() },
                onSelectAll = { selected = if (selected.containsAll(pickable)) emptySet() else pickable.toSet() },
                onMore = { panelIds = selected.toList(); panelStep = 0 },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    // One panel for everything about a download: the actions, then the delete question, then the confirmation.
    // The steps slide inside the same panel, with a back button from the second step on.
    panelIds?.let { ids ->
        LaunchedEffect(ids) { DownloadEngine.checkFiles() }   // a file removed elsewhere turns the delete option into "remove from list"
        val chosen = ids.mapNotNull { id -> all.firstOrNull { it.record.id == id } }
        if (chosen.isEmpty()) panelIds = null else BottomPanelFlow(
            onDismiss = { panelIds = null },
            step = panelStep,
            onBack = { if (panelStep > 0) panelStep -= 1 },
        ) { shown ->
            when (shown) {
                0 -> {
                    if (chosen.size == 1) {
                        val item = chosen.first()
                        val id = item.record.id
                        val state = item.status.state
                        Text(item.record.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                        if (state == DownloadState.DONE) PanelRow(Icons.Default.PlayArrow, R.string.downloads_open) { panelIds = null; open(item) }
                        if (state == DownloadState.FAILED) PanelRow(Icons.Default.Refresh, R.string.downloads_retry) { panelIds = null; onRetry(item.record.sourceLink) }
                        PanelRow(Icons.Default.Check, R.string.downloads_select) { panelIds = null; toggle(id) }
                        if (state == DownloadState.DONE) {
                            PanelRow(Icons.Default.Delete, R.string.downloads_delete, danger = true) { askDelete(listOf(id)) }
                        } else {
                            PanelRow(Icons.Default.Close, R.string.downloads_remove) { panelIds = null; scope.launch(Dispatchers.IO) { DownloadEngine.remove(id) } }
                        }
                    } else {
                        PanelRow(Icons.Default.Delete, R.string.downloads_delete, danger = true) { askDelete(chosen.map { it.record.id }) }
                    }
                }
                1 -> PanelBody(
                    title = if (ids.size == 1) stringResource(R.string.downloads_delete_title) else stringResource(R.string.downloads_delete_title_many, ids.size),
                    message = stringResource(R.string.downloads_delete_message),
                ) {
                    PanelAction(stringResource(R.string.downloads_delete_device), main = true, danger = true, onClick = { panelStep = 2 })
                    PanelAction(stringResource(R.string.downloads_remove_list), onClick = {
                        panelIds = null
                        selected = emptySet()
                        scope.launch(Dispatchers.IO) { ids.forEach { DownloadEngine.remove(it, deleteFile = false) } }
                    })
                    PanelAction(stringResource(R.string.downloads_cancel), onClick = { panelIds = null })
                }
                else -> {
                    // For deleting from the device: a panel like the system window, with the cover.
                    val first = chosen.first()
                    val picture = first.record.uri?.let { MediaPreview.peek(it)?.thumbnail } ?: first.record.coverUrl?.let { Thumbnails.peek(it) }
                    PanelBody(
                        title = when {
                            ids.size > 1 -> stringResource(R.string.downloads_allow_many, ids.size)
                            first.record.kind == Kind.VIDEO -> stringResource(R.string.downloads_allow_video)
                            else -> stringResource(R.string.downloads_allow_audio)
                        },
                    ) {
                        if (picture != null) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Image(picture.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(180.dp).clip(RoundedCornerShape(16.dp)))
                            }
                        }
                        PanelAction(stringResource(R.string.downloads_allow), main = true, danger = true, onClick = {
                            panelIds = null
                            scope.launch {
                                when (val step = withContext(Dispatchers.IO) { DownloadEngine.startDelete(context, ids) }) {
                                    is DownloadEngine.DeleteStep.Ask -> {
                                        deletingIds = step.ids
                                        deleteWindow.launch(IntentSenderRequest.Builder(step.sender).build())
                                    }
                                    DownloadEngine.DeleteStep.Gone -> selected = emptySet()
                                    is DownloadEngine.DeleteStep.Failed ->
                                        Toast.makeText(context, context.getString(R.string.downloads_delete_failed) + (step.detail?.let { "\n$it" } ?: ""), Toast.LENGTH_LONG).show()
                                }
                            }
                        })
                        PanelAction(stringResource(R.string.downloads_deny), onClick = { panelIds = null })
                    }
                }
            }
        }
    }
}

/** The two floating pills while picking: count and close on the left, select all and more on the right. */
@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit, onMore: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)) {
            Row(Modifier.height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.downloads_clear_selection)) }
                Text("$count", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 16.dp))
            }
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)) {
            Row(Modifier.height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSelectAll) { Icon(ImageVector.vectorResource(R.drawable.ic_select_all), contentDescription = stringResource(R.string.downloads_select_all)) }
                IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.downloads_more)) }
            }
        }
    }
}

@Composable
private fun PanelRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRow(item: DownloadItem, picked: Boolean, onTap: () -> Unit, onHold: () -> Unit, onCover: () -> Unit, onMore: () -> Unit) {
    val context = LocalContext.current
    val record = item.record
    val status = item.status
    val percent = if (status.state == DownloadState.RUNNING && status.totalBytes > 0) (status.downloadedBytes * 100 / status.totalBytes).toInt() else null

    // Length and picture come from the saved file itself, read once off the main thread.
    val preview by produceState(record.uri?.let { MediaPreview.peek(it) }, record.uri, status.state) {
        value = if (status.state == DownloadState.DONE && record.uri != null) {
            withContext(Dispatchers.IO) { MediaPreview.load(context, record.uri, record.kind) }
        } else null
    }
    val cover by rememberCover(record.coverUrl)

    Card(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).combinedClickable(onClick = onTap, onLongClick = onHold),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (picked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Only the cover starts picking; while picking, the whole row toggles.
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onCover),
                contentAlignment = Alignment.Center,
            ) {
                val thumbnail = preview?.thumbnail ?: cover
                if (picked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                } else if (thumbnail != null) {
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
                            status.reason == REASON_NO_FORMAT -> stringResource(R.string.downloads_status_failed_no_format)
                            else -> stringResource(R.string.downloads_status_failed)
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
            }
            IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.downloads_more)) }
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

/** The picture of a download that is not saved yet (from its web address), shown from the first frame when already loaded. */
@Composable
internal fun rememberCover(url: String?): State<Bitmap?> = produceState(url?.let { Thumbnails.peek(it) }, url) {
    if (url != null && value == null) value = withContext(Dispatchers.IO) { Thumbnails.load(url) }
}

/** One card for everything that is downloading: count, the first item and its progress. Tap to manage them. */
@Composable
private fun ActiveCard(active: List<DownloadItem>, onClick: () -> Unit) {
    val first = active.first()
    val status = first.status
    val percent = if (status.state == DownloadState.RUNNING && status.totalBytes > 0) (status.downloadedBytes * 100 / status.totalBytes).toInt() else null
    val cover by rememberCover(first.record.coverUrl)
    Card(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                val picture = cover
                if (picture != null) Image(picture.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text(first.record.extension.uppercase(), style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.active_count, active.size), style = MaterialTheme.typography.titleMedium)
                Text(first.record.title, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                if (percent != null) LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
