package com.intruphon.app.features.downloads

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import com.intruphon.app.core.formatClock
import com.intruphon.app.data.download.DownloadItem
import com.intruphon.app.data.download.FileDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** The last step of the download panel: what is known about the saved file, each line with a small colored label. */
@Composable
internal fun DetailsStep(item: DownloadItem) {
    val context = LocalContext.current
    val record = item.record
    val details by produceState<FileDetails?>(null, record.uri) {
        value = record.uri?.let { withContext(Dispatchers.IO) { FileDetails.load(context, it) } }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(record.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val d = details
        if (d == null && record.uri != null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            return@Column
        }
        val rows = listOf(
            R.string.details_path to d?.path,
            R.string.details_format to listOf(record.extension.uppercase(), d?.codecs.orEmpty()).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { null },
            R.string.details_size to d?.sizeBytes?.let { Formatter.formatFileSize(context, it) },
            R.string.details_duration to d?.durationMs?.let { formatClock(it / 1000) },
            R.string.details_bitrate to d?.bitrateKbps?.let { "$it kbps" },
            R.string.details_samplerate to d?.sampleRateHz?.let { "$it Hz" },
            R.string.details_resolution to if (d?.width != null && d.height != null) "${d.width} × ${d.height}" else null,
            R.string.details_framerate to d?.frameRate?.let { "$it fps" },
            R.string.details_modified to d?.modifiedMs?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) },
            R.string.details_link to record.sourceLink.ifEmpty { null },
        )
        rows.forEach { (label, value) ->
            if (value != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(value, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
