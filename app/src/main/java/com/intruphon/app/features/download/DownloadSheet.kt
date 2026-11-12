package com.intruphon.app.features.download

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.intruphon.app.R
import com.intruphon.app.data.download.DownloadEngine
import com.intruphon.app.data.media.DownloadOption
import com.intruphon.app.data.media.FailReason
import com.intruphon.app.data.media.Kind
import com.intruphon.app.data.media.LoadResult
import com.intruphon.app.data.media.MediaInfo
import com.intruphon.app.data.media.MediaRepository
import java.util.Locale

/** Bottom sheet that reads [link], lists the formats and starts the download. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(link: String, onDismiss: () -> Unit, onStarted: () -> Unit = {}) {
    // A playlist link opens the list with checkboxes instead of the format list of one item.
    if (com.intruphon.app.core.LinkExtractor.isPlaylist(link)) {
        PlaylistSheet(link, onDismiss)
        return
    }
    val result by produceState<LoadResult?>(initialValue = null, link) { value = MediaRepository.load(link) }
    // Fully open from the start, so the Download button is never hidden below the fold.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (val r = result) {
                null -> {
                    Text(stringResource(R.string.download_loading), style = MaterialTheme.typography.titleMedium)
                    CircularProgressIndicator()
                }
                is LoadResult.Failed -> {
                    val message = when (r.reason) {
                        FailReason.NETWORK -> R.string.download_error_network
                        FailReason.UNREADABLE -> R.string.download_error_unreadable
                        FailReason.NO_FORMATS -> R.string.download_error_no_formats
                    }
                    Text(stringResource(message))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.download_close)) }
                }
                is LoadResult.Ready -> FormatList(r.info, link, onDismiss, onStarted)
            }
        }
    }
}

@Composable
private fun ColumnScope.FormatList(info: MediaInfo, link: String, onDismiss: () -> Unit, onStarted: () -> Unit) {
    val context = LocalContext.current
    val options = info.options
    val audio = options.filter { it.kind == Kind.AUDIO }
    val video = options.filter { it.kind == Kind.VIDEO }
    val audioPicks = picks(audio)
    val videoPicks = picks(video)
    val shownCount = audioPicks.size + videoPicks.size
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val default = (audioPicks.firstOrNull() ?: videoPicks.first()).option
    val selected = options.firstOrNull { it.id == selectedId } ?: default

    fun start() {
        DownloadEngine.enqueue(info, selected, link)
        Toast.makeText(context, R.string.download_started, Toast.LENGTH_LONG).show()
        onStarted()
        onDismiss()
    }

    // Android 13+: ask once for permission to show the progress notification. Download starts either way.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { start() }

    Text(stringResource(R.string.download_title), style = MaterialTheme.typography.titleLarge)
    Text(info.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
    // Only the list scrolls; the Download button below stays in view.
    Column(
        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (expanded) {
            Section(R.string.download_section_audio, audio.map { Pick(it, null) }, selected) { selectedId = it.id }
            Section(R.string.download_section_video, video.map { Pick(it, null) }, selected) { selectedId = it.id }
        } else {
            Section(R.string.download_section_audio, audioPicks, selected) { selectedId = it.id }
            Section(R.string.download_section_video, videoPicks, selected) { selectedId = it.id }
        }
        if (options.size > shownCount) {
            TextButton(
                onClick = {
                    expanded = !expanded
                    // Back to the short list: keep the choice only if that list still shows it.
                    if (!expanded && (audioPicks + videoPicks).none { it.option.id == selected.id }) selectedId = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (expanded) R.string.download_fewer else R.string.download_more)) }
        }
    }
    Button(
        onClick = {
            val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsAsk) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) else start()
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) { Text(stringResource(R.string.download_button), style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Section(title: Int, picks: List<Pick>, selected: DownloadOption, onSelect: (DownloadOption) -> Unit) {
    if (picks.isEmpty()) return
    Text(stringResource(title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    picks.forEach { pick ->
        val option = pick.option
        val isSelected = option.id == selected.id
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) }),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isSelected, onClick = null)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = listOf(option.format, option.quality).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val label = when (pick.end) {
                        End.HIGHEST -> R.string.download_highest
                        End.SMALLEST -> R.string.download_smallest
                        null -> null
                    }
                    if (label != null) Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
                }
                option.sizeBytes?.let { Text(formatSize(it), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun formatSize(bytes: Long): String =
    String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
