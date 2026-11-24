package com.intruphon.app.features.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledIconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalWindowInfo
import com.intruphon.app.core.ui.FunnelProgress
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.intruphon.app.R
import com.intruphon.app.core.ui.clearFocusWhenKeyboardCloses
import com.intruphon.app.core.LinkExtractor
import com.intruphon.app.core.ui.PageHeader
import com.intruphon.app.data.download.DownloadEngine
import com.intruphon.app.data.download.DownloadItem
import com.intruphon.app.data.download.DownloadState
import com.intruphon.app.data.download.MediaPreview
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.net.Thumbnails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MIN_RECENT = 3

/** Home: paste a link, press Download, and see the latest downloads below. The wiring comes from the entry point. */
@Composable
fun HomeScreen(onDownload: (String) -> Unit, onSearch: (String) -> Unit, onOpenDownloads: () -> Unit, onOpenActive: () -> Unit, modifier: Modifier = Modifier) {
    var link by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val url = LinkExtractor.firstUrl(link)
    // Text with no link in it is a name to search for: the same button then searches instead of downloading.
    val query = LinkExtractor.searchQuery(link)
    val focusManager = LocalFocusManager.current
    val act: () -> Unit = {
        focusManager.clearFocus()
        if (url != null) onDownload(url)
        else if (query != null) onSearch(query)
    }
    val density = LocalDensity.current
    // Is there text on the clipboard? Only its type is looked at, never its content, until the person taps the chip.
    val systemClipboard = remember { context.getSystemService(android.content.ClipboardManager::class.java) }
    var hasCopied by remember { mutableStateOf(false) }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(windowFocused) {
        if (windowFocused) hasCopied = systemClipboard?.primaryClipDescription?.hasMimeType("text/*") == true
    }
    DisposableEffect(systemClipboard) {
        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            hasCopied = systemClipboard?.primaryClipDescription?.hasMimeType("text/*") == true
        }
        systemClipboard?.addPrimaryClipChangedListener(listener)
        onDispose { systemClipboard?.removePrimaryClipChangedListener(listener) }
    }
    fun useCopied() {
        val pasted = clipboard.getText()?.text.orEmpty()
        if (pasted.isBlank()) { hasCopied = false; Toast.makeText(context, R.string.home_clipboard_empty, Toast.LENGTH_SHORT).show(); return }
        link = pasted
        val copiedUrl = LinkExtractor.firstUrl(pasted)
        if (copiedUrl != null) onDownload(copiedUrl)
    }
    // The recent list fills the screen to the bottom: how many rows fit depends on the device, so it is measured.
    var viewport by remember { mutableIntStateOf(0) }
    var headerBottom by remember { mutableIntStateOf(0) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val gap = with(density) { 16.dp.roundToPx() }
    val rowEstimate = if (rowHeight > 0) rowHeight else with(density) { 80.dp.roundToPx() }
    val navigationBar = WindowInsets.navigationBars.getBottom(density)
    val room = viewport - headerBottom - gap - with(density) { 24.dp.roundToPx() } - navigationBar
    val fit = if (viewport > 0 && headerBottom > 0) maxOf(MIN_RECENT, (room + gap) / (rowEstimate + gap)) else MIN_RECENT
    val items = DownloadEngine.items.collectAsState().value
    val recent = items.take(fit)
    val running = items.filter { it.status.state.active }

    // Files deleted outside the app are marked when Home shows, like in the Downloads page.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) DownloadEngine.checkFiles() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .onSizeChanged { viewport = it.height }
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }   // tapping outside leaves the field
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(stringResource(R.string.home_title))
        // One capsule: the field with its round action button inside, the same height with or without focus.
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            // One small line that never wraps.
            label = { Text(stringResource(R.string.home_link_label), style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            // The Enter key of the keyboard does what the round button does (search or download).
            keyboardOptions = KeyboardOptions(imeAction = if (url == null) ImeAction.Search else ImeAction.Go),
            keyboardActions = KeyboardActions(onAny = { act() }),
            shape = CircleShape,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (link.isEmpty()) {
                        IconButton(onClick = {
                            val pasted = clipboard.getText()?.text.orEmpty()
                            if (pasted.isBlank()) Toast.makeText(context, R.string.home_clipboard_empty, Toast.LENGTH_SHORT).show()
                            else link = pasted
                        }) { Icon(ImageVector.vectorResource(R.drawable.ic_content_paste), contentDescription = stringResource(R.string.home_paste)) }
                    } else {
                        IconButton(onClick = { link = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.home_clear))
                        }
                    }
                    val searching = url == null && query != null
                    FilledIconButton(onClick = act, enabled = url != null || query != null, modifier = Modifier.padding(end = 6.dp).size(44.dp)) {
                        Icon(
                            if (searching) Icons.Default.Search else Icons.Default.ArrowDownward,
                            contentDescription = stringResource(if (searching) R.string.home_search else R.string.home_download),
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().clearFocusWhenKeyboardCloses(),
        )
        // Something was copied: one tap brings it in (a link goes straight to the format sheet).
        if (hasCopied && link.isEmpty()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable { useCopied() },
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_content_paste), contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.home_use_copied), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        // Something is downloading: the funnel shows how far along, and the strip opens the page to manage them.
        if (running.isNotEmpty()) {
            val known = running.mapNotNull { r -> r.status.takeIf { it.state == DownloadState.RUNNING && it.totalBytes > 0 }?.let { it.downloadedBytes.toFloat() / it.totalBytes } }
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onOpenActive),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FunnelProgress(known.takeIf { it.isNotEmpty() }?.average()?.toFloat())
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.active_count, running.size), style = MaterialTheme.typography.titleMedium)
                        Text(running.first().record.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().onGloballyPositioned { headerBottom = (it.positionInParent().y + it.size.height).toInt() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.home_recent), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenDownloads) { Text(stringResource(R.string.home_see_all)) }
        }
        if (recent.isEmpty()) {
            Text(stringResource(R.string.downloads_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            recent.forEach { RecentRow(it, onOpenDownloads, onHeight = { h -> if (rowHeight == 0 || h < rowHeight) rowHeight = h }) }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun RecentRow(item: DownloadItem, onClick: () -> Unit, onHeight: (Int) -> Unit) {
    val context = LocalContext.current
    val record = item.record
    val status = item.status
    val percent = if (status.state == DownloadState.RUNNING && status.totalBytes > 0) (status.downloadedBytes * 100 / status.totalBytes).toInt() else null
    val cover by produceState<android.graphics.Bitmap?>(record.coverUrl?.let { Thumbnails.peek(it) }, record.coverUrl) {
        val url = record.coverUrl
        if (url != null && value == null) value = withContext(Dispatchers.IO) { Thumbnails.load(url) }
    }
    val preview by produceState(record.uri?.let { MediaPreview.peek(it) }, record.uri, status.state) {
        value = if (status.state == DownloadState.DONE && record.uri != null) {
            withContext(Dispatchers.IO) { MediaPreview.load(context, record.uri, record.kind) }
        } else null
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().onSizeChanged { onHeight(it.height) }.clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val thumbnail = preview?.thumbnail ?: cover
                if (thumbnail != null) {
                    Image(thumbnail.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(record.extension.uppercase(), style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(record.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                val kind = stringResource(if (record.kind == Kind.AUDIO) R.string.download_section_audio else R.string.download_section_video)
                val details = listOf(kind, record.extension.uppercase(), record.quality).filter { it.isNotEmpty() }.joinToString(" · ")
                Text(details, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = when (status.state) {
                        DownloadState.WAITING -> stringResource(R.string.downloads_status_waiting)
                        DownloadState.RUNNING ->
                            if (percent != null) stringResource(R.string.downloads_status_running, percent)
                            else stringResource(R.string.downloads_status_running_unknown)
                        DownloadState.PROCESSING -> stringResource(R.string.downloads_status_processing)
                        DownloadState.DONE -> stringResource(R.string.home_recent_done)
                        DownloadState.MISSING -> stringResource(R.string.downloads_status_missing)
                        DownloadState.FAILED -> stringResource(R.string.home_recent_failed)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (status.state.active) {
                    if (percent != null) LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                    else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
