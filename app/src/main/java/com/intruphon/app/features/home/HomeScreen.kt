package com.intruphon.app.features.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.intruphon.app.R
import com.intruphon.app.core.LinkExtractor
import com.intruphon.app.core.ui.PageHeader
import com.intruphon.app.data.download.DownloadEngine
import com.intruphon.app.data.download.DownloadItem
import com.intruphon.app.data.download.DownloadState
import com.intruphon.app.data.download.MediaPreview
import com.intruphon.app.data.media.Kind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MIN_RECENT = 3

/** Home: paste a link, press Download, and see the latest downloads below. The wiring comes from the entry point. */
@Composable
fun HomeScreen(onDownload: (String) -> Unit, onOpenDownloads: () -> Unit, modifier: Modifier = Modifier) {
    var link by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val url = LinkExtractor.firstUrl(link)
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    // The recent list fills the screen to the bottom: how many rows fit depends on the device, so it is measured.
    var viewport by remember { mutableIntStateOf(0) }
    var headerBottom by remember { mutableIntStateOf(0) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val gap = with(density) { 16.dp.roundToPx() }
    val rowEstimate = if (rowHeight > 0) rowHeight else with(density) { 80.dp.roundToPx() }
    val room = viewport - headerBottom - gap - with(density) { 24.dp.roundToPx() }
    val fit = if (viewport > 0 && headerBottom > 0) maxOf(MIN_RECENT, (room + gap) / (rowEstimate + gap)) else MIN_RECENT
    val recent = DownloadEngine.items.collectAsState().value.take(fit)

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
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text(stringResource(R.string.home_link_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            trailingIcon = {
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
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { url?.let(onDownload) },
            enabled = url != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.home_download), style = MaterialTheme.typography.titleMedium) }

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
    }
}

@Composable
private fun RecentRow(item: DownloadItem, onClick: () -> Unit, onHeight: (Int) -> Unit) {
    val context = LocalContext.current
    val record = item.record
    val status = item.status
    val percent = if (status.state == DownloadState.RUNNING && status.totalBytes > 0) (status.downloadedBytes * 100 / status.totalBytes).toInt() else null
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
                val thumbnail = preview?.thumbnail
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
