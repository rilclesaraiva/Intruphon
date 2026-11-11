package com.intruphon.app.features.download

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
    val result by produceState<LoadResult?>(initialValue = null, link) { value = MediaRepository.load(link) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
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
private fun FormatList(info: MediaInfo, link: String, onDismiss: () -> Unit, onStarted: () -> Unit) {
    val context = LocalContext.current
    val options = info.options
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = options.firstOrNull { it.id == selectedId } ?: options.first()

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
    Section(R.string.download_section_audio, options.filter { it.kind == Kind.AUDIO }, selected) { selectedId = it.id }
    Section(R.string.download_section_video, options.filter { it.kind == Kind.VIDEO }, selected) { selectedId = it.id }
    Button(
        onClick = {
            val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsAsk) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) else start()
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.download_button)) }
}

@Composable
private fun Section(title: Int, options: List<DownloadOption>, selected: DownloadOption, onSelect: (DownloadOption) -> Unit) {
    if (options.isEmpty()) return
    Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
    options.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = option.id == selected.id, role = Role.RadioButton, onClick = { onSelect(option) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = option.id == selected.id, onClick = null)
            Text(
                text = listOf(option.format, option.quality).filter { it.isNotEmpty() }.joinToString(" · "),
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            option.sizeBytes?.let { Text(formatSize(it), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun formatSize(bytes: Long): String =
    String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
