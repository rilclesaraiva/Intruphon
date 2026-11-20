package com.intruphon.app.features.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import com.intruphon.app.core.ui.ChoiceRow
import com.intruphon.app.core.ui.ExpandableOptionRow
import com.intruphon.app.core.ui.IconRowGroup
import com.intruphon.app.core.ui.IconRowItem
import com.intruphon.app.core.ui.RowTone
import com.intruphon.app.core.ui.SectionCard
import com.intruphon.app.data.settings.AppSettings

/** Settings → Appearance: theme, colors, the back gesture and the app language. */
@Composable
fun AppearanceSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val theme by AppSettings.themeFlow.collectAsState()

    SettingsPage(stringResource(R.string.settings_cat_appearance), modifier) {
        ExpandableOptionRow(
            icon = ImageVector.vectorResource(R.drawable.ic_palette),
            tone = RowTone.PRIMARY,
            title = stringResource(R.string.settings_theme),
            summary = themeName(theme),
        ) {
            AppSettings.ThemeMode.entries.forEach { mode ->
                ChoiceRow(themeName(mode), selected = mode == theme, onSelect = { AppSettings.setTheme(context, mode) })
            }
        }

        SectionCard {
            val predictive by AppSettings.predictiveBackFlow.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_predictive_back), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_predictive_back_hint), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = predictive, onCheckedChange = { AppSettings.setPredictiveBack(context, it) })
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {   // wallpaper colors exist from Android 12
                val systemColors by AppSettings.dynamicColorFlow.collectAsState()
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_dynamic_color), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.settings_dynamic_color_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = systemColors, onCheckedChange = { AppSettings.setDynamicColor(context, it) })
                }
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
    }
}

@Composable
private fun themeName(mode: AppSettings.ThemeMode): String = stringResource(
    when (mode) {
        AppSettings.ThemeMode.SYSTEM -> R.string.settings_theme_system
        AppSettings.ThemeMode.LIGHT -> R.string.settings_theme_light
        AppSettings.ThemeMode.DARK -> R.string.settings_theme_dark
        AppSettings.ThemeMode.AMOLED -> R.string.settings_theme_amoled
    },
)
