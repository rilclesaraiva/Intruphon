package com.intruphon.app.features.settings

import android.content.Intent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.intruphon.app.data.update.CheckResult
import com.intruphon.app.data.update.UpdateRepository
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import com.intruphon.app.core.ui.IconRowGroup
import com.intruphon.app.core.ui.IconRowItem
import com.intruphon.app.core.ui.RowTone
import com.intruphon.app.core.ui.PageHeader
import com.intruphon.app.core.ui.SectionCard
import com.intruphon.app.core.ui.SectionLabel
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
import com.intruphon.app.BuildConfig
import com.intruphon.app.R
import com.intruphon.app.data.update.MONTH_MS
import com.intruphon.app.data.update.WEEK_MS
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.saveable.rememberSaveable
import com.intruphon.app.data.settings.AppSettings
import com.intruphon.app.data.stats.SpeedLog
import com.intruphon.app.data.stats.formatSpeed
import com.intruphon.app.data.stats.summarize
import com.intruphon.app.data.stats.toCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onOpenAbout: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var wifiOnly by remember { mutableStateOf(AppSettings.wifiOnly(context)) }
    var parallel by remember { mutableIntStateOf(AppSettings.maxParallel(context)) }
    var reload by remember { mutableIntStateOf(0) }
    val entries by produceState(emptyList(), reload) { value = withContext(Dispatchers.IO) { SpeedLog.all(context) } }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageHeader(stringResource(R.string.tab_settings), afterBack = true)

        SectionLabel(stringResource(R.string.settings_cat_appearance))
        SectionCard {
            val predictive by AppSettings.predictiveBackFlow.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_predictive_back), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_predictive_back_hint), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = predictive, onCheckedChange = { AppSettings.setPredictiveBack(context, it) })
            }
        }

        // Language is chosen on Android's own screen (per-app language on Android 13+).
        IconRowGroup(
            listOf(
                IconRowItem(
                    ImageVector.vectorResource(R.drawable.ic_language),
                    RowTone.SECONDARY,
                    stringResource(R.string.settings_language),
                    stringResource(R.string.settings_language_hint),
                    onClick = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Intent(android.provider.Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        } else Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS)
                        runCatching { context.startActivity(intent) }
                    },
                ),
            ),
        )

        SectionLabel(stringResource(R.string.settings_cat_downloads))
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_wifi_only), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_wifi_only_hint), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it; AppSettings.setWifiOnly(context, it) })
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_parallel, parallel), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = parallel.toFloat(),
                onValueChange = { parallel = it.toInt() },
                onValueChangeFinished = { AppSettings.setMaxParallel(context, parallel) },
                valueRange = AppSettings.MIN_PARALLEL.toFloat()..AppSettings.MAX_PARALLEL.toFloat(),
                steps = AppSettings.MAX_PARALLEL - AppSettings.MIN_PARALLEL - 1,
            )
        }

        SectionLabel(stringResource(R.string.settings_speed_title))
        SectionCard {
            Text(stringResource(R.string.settings_speed_hint), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
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

        SectionLabel(stringResource(R.string.settings_cat_updates))
        val choices by UpdateRepository.choices.collectAsState()
        val now = System.currentTimeMillis()
        val notices = when {
            choices.skippedVersion != null -> stringResource(R.string.settings_updates_skipping, choices.skippedVersion.orEmpty())
            choices.snoozed(now) -> stringResource(R.string.settings_updates_snoozed, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(choices.snoozeUntilMs)))
            else -> stringResource(R.string.settings_updates_on)
        }
        var checking by remember { mutableStateOf(false) }
        var result by remember { mutableStateOf<CheckResult?>(null) }
        var choosing by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val checkHint = when {
            checking -> stringResource(R.string.settings_updates_checking)
            result == CheckResult.UP_TO_DATE -> stringResource(R.string.settings_updates_up_to_date)
            result == CheckResult.AVAILABLE -> stringResource(R.string.settings_updates_available)
            result == CheckResult.FAILED -> stringResource(R.string.settings_updates_failed)
            else -> stringResource(R.string.settings_updates_check_hint, BuildConfig.VERSION_NAME)
        }
        IconRowGroup(
            listOf(
                IconRowItem(
                    ImageVector.vectorResource(R.drawable.ic_download),
                    RowTone.PRIMARY,
                    stringResource(R.string.settings_updates_check),
                    checkHint,
                    onClick = {
                        if (!checking) {
                            checking = true
                            scope.launch { result = UpdateRepository.checkNow(); checking = false }
                        }
                    },
                ),
                IconRowItem(
                    ImageVector.vectorResource(R.drawable.ic_download),
                    RowTone.PRIMARY,
                    stringResource(R.string.settings_updates_title),
                    notices,
                    onClick = { choosing = true },
                ),
            ),
        )
        if (choosing) {
            AlertDialog(
                onDismissRequest = { choosing = false },
                title = { Text(stringResource(R.string.settings_updates_title)) },
                text = {
                    Column {
                        TextButton(onClick = { choosing = false; UpdateRepository.undoChoices() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_updates_normal), modifier = Modifier.fillMaxWidth())
                        }
                        TextButton(onClick = { choosing = false; UpdateRepository.snooze(WEEK_MS) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_updates_week), modifier = Modifier.fillMaxWidth())
                        }
                        TextButton(onClick = { choosing = false; UpdateRepository.snooze(MONTH_MS) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_updates_month), modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { choosing = false }) { Text(stringResource(R.string.downloads_cancel)) } },
            )
        }

        SectionLabel(stringResource(R.string.settings_cat_about))
        IconRowGroup(
            listOf(
                IconRowItem(
                    Icons.Default.Info,
                    RowTone.NEUTRAL,
                    stringResource(R.string.settings_about_entry),
                    stringResource(R.string.settings_about_hint),
                    onClick = onOpenAbout,
                ),
            ),
        )
    }
}
