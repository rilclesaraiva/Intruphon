package com.intruphon.app.features.settings

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.BuildConfig
import com.intruphon.app.R
import com.intruphon.app.core.ui.ChoiceRow
import com.intruphon.app.core.ui.ExpandableOptionRow
import com.intruphon.app.core.ui.IconRowGroup
import com.intruphon.app.core.ui.IconRowItem
import com.intruphon.app.core.ui.RowTone
import com.intruphon.app.core.ui.SectionCard
import com.intruphon.app.core.ui.SectionLabel
import com.intruphon.app.data.stats.SpeedLog
import com.intruphon.app.data.stats.formatSpeed
import com.intruphon.app.data.stats.summarize
import com.intruphon.app.data.stats.toCsv
import com.intruphon.app.data.update.CheckResult
import com.intruphon.app.data.update.MONTH_MS
import com.intruphon.app.data.update.UpdateRepository
import com.intruphon.app.data.update.WEEK_MS
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Settings → System: updates (check now, how the notices behave) and the download speed log. */
@Composable
fun SystemSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var reload by remember { mutableIntStateOf(0) }
    val entries by produceState(emptyList(), reload) { value = withContext(Dispatchers.IO) { SpeedLog.all(context) } }

    SettingsPage(stringResource(R.string.settings_cat_system), modifier) {
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
            ),
        )
        ExpandableOptionRow(
            icon = ImageVector.vectorResource(R.drawable.ic_download),
            tone = RowTone.PRIMARY,
            title = stringResource(R.string.settings_updates_title),
            summary = notices,
        ) {
            val normal = choices.skippedVersion == null && !choices.snoozed(now)
            ChoiceRow(stringResource(R.string.settings_updates_normal), selected = normal, onSelect = { UpdateRepository.undoChoices() })
            ChoiceRow(stringResource(R.string.settings_updates_week), selected = false, onSelect = { UpdateRepository.snooze(WEEK_MS) })
            ChoiceRow(stringResource(R.string.settings_updates_month), selected = false, onSelect = { UpdateRepository.snooze(MONTH_MS) })
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
    }
}
