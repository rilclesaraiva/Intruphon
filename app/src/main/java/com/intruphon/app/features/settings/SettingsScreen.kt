package com.intruphon.app.features.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import com.intruphon.app.data.settings.AppSettings
import com.intruphon.app.data.stats.SpeedLog
import com.intruphon.app.data.stats.formatSpeed
import com.intruphon.app.data.stats.summarize
import com.intruphon.app.data.stats.toCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var wifiOnly by remember { mutableStateOf(AppSettings.wifiOnly(context)) }
    var parallel by remember { mutableIntStateOf(AppSettings.maxParallel(context)) }
    var reload by remember { mutableIntStateOf(0) }
    val entries by produceState(emptyList(), reload) { value = withContext(Dispatchers.IO) { SpeedLog.all(context) } }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_wifi_only), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_wifi_only_hint), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it; AppSettings.setWifiOnly(context, it) })
        }

        Column {
            Text(stringResource(R.string.settings_parallel, parallel), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = parallel.toFloat(),
                onValueChange = { parallel = it.toInt() },
                onValueChangeFinished = { AppSettings.setMaxParallel(context, parallel) },
                valueRange = AppSettings.MIN_PARALLEL.toFloat()..AppSettings.MAX_PARALLEL.toFloat(),
                steps = AppSettings.MAX_PARALLEL - AppSettings.MIN_PARALLEL - 1,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.settings_speed_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.settings_speed_hint), style = MaterialTheme.typography.bodySmall)
            val summary = summarize(entries)
            if (summary.isEmpty()) {
                Text(stringResource(R.string.settings_speed_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                summary.forEach {
                    val quality = it.quality.ifEmpty { "-" }
                    Text(stringResource(R.string.settings_speed_row, it.format, quality, formatSpeed(it.bytesPerSecond), it.count), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row {
                TextButton(
                    enabled = entries.isNotEmpty(),
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, toCsv(entries))
                        context.startActivity(Intent.createChooser(send, null))
                    },
                ) { Text(stringResource(R.string.settings_speed_share)) }
                TextButton(
                    enabled = entries.isNotEmpty(),
                    onClick = { SpeedLog.clear(context); reload++ },
                ) { Text(stringResource(R.string.settings_speed_clear)) }
            }
        }
    }
}
