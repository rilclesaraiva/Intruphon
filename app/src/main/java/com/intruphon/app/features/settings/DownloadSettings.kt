package com.intruphon.app.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import com.intruphon.app.core.ui.BottomPanel
import com.intruphon.app.core.ui.IconRowGroup
import com.intruphon.app.core.ui.IconRowItem
import com.intruphon.app.core.ui.PanelAction
import com.intruphon.app.core.ui.RowTone
import com.intruphon.app.core.ui.SectionCard
import com.intruphon.app.data.download.SaveLocation
import com.intruphon.app.data.settings.AppSettings

/** Settings → Downloads: Wi-Fi only, how many downloads run at once, and where the files go. */
@Composable
fun DownloadSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var wifiOnly by remember { mutableStateOf(AppSettings.wifiOnly(context)) }
    var parallelWifi by remember { mutableIntStateOf(AppSettings.maxParallelWifi(context)) }
    var parallelMobile by remember { mutableIntStateOf(AppSettings.maxParallelMobile(context)) }
    var folder by remember { mutableStateOf(SaveLocation.label(context)) }
    var folderLost by remember { mutableStateOf(SaveLocation.lost(context)) }
    var folderPanel by rememberSaveable { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            runCatching { SaveLocation.choose(context, tree) }
                .onFailure { Toast.makeText(context, R.string.settings_folder_failed, Toast.LENGTH_LONG).show() }
            folder = SaveLocation.label(context)
            folderLost = SaveLocation.lost(context)
        }
    }

    SettingsPage(stringResource(R.string.settings_cat_downloads), modifier) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_wifi_only), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_wifi_only_hint), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it; AppSettings.setWifiOnly(context, it) })
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_parallel_wifi, parallelWifi), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = parallelWifi.toFloat(),
                onValueChange = { parallelWifi = it.toInt() },
                onValueChangeFinished = { AppSettings.setMaxParallelWifi(context, parallelWifi) },
                valueRange = AppSettings.MIN_PARALLEL.toFloat()..AppSettings.MAX_PARALLEL.toFloat(),
                steps = AppSettings.MAX_PARALLEL - AppSettings.MIN_PARALLEL - 1,
            )
            Text(stringResource(R.string.settings_parallel_mobile, parallelMobile), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = parallelMobile.toFloat(),
                onValueChange = { parallelMobile = it.toInt() },
                onValueChangeFinished = { AppSettings.setMaxParallelMobile(context, parallelMobile) },
                valueRange = AppSettings.MIN_PARALLEL.toFloat()..AppSettings.MAX_PARALLEL.toFloat(),
                steps = AppSettings.MAX_PARALLEL - AppSettings.MIN_PARALLEL - 1,
            )
            Text(stringResource(R.string.settings_parallel_hint), style = MaterialTheme.typography.bodySmall)
        }

        IconRowGroup(
            listOf(
                IconRowItem(
                    ImageVector.vectorResource(R.drawable.ic_folder),
                    RowTone.SECONDARY,
                    stringResource(R.string.settings_folder),
                    if (folderLost) stringResource(R.string.settings_folder_lost) else folder ?: stringResource(R.string.settings_folder_default),
                    onClick = { folderPanel = true },
                ),
            ),
        )
        if (folderPanel) {
            BottomPanel(
                onDismiss = { folderPanel = false },
                title = stringResource(R.string.settings_folder),
                message = if (folderLost) stringResource(R.string.settings_folder_lost) else stringResource(R.string.settings_folder_message, folder ?: stringResource(R.string.settings_folder_default)),
            ) {
                PanelAction(stringResource(R.string.settings_folder_choose), main = true, onClick = { folderPanel = false; folderPicker.launch(null) })
                if (folder != null || folderLost) {
                    PanelAction(stringResource(R.string.settings_folder_reset), onClick = {
                        folderPanel = false
                        SaveLocation.useDefault(context)
                        folder = null
                        folderLost = false
                    })
                }
            }
        }
    }
}
