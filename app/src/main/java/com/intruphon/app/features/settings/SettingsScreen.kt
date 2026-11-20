package com.intruphon.app.features.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.intruphon.app.R
import com.intruphon.app.core.ui.IconRowGroup
import com.intruphon.app.core.ui.IconRowItem
import com.intruphon.app.core.ui.RowTone
import com.intruphon.app.core.ui.SectionLabel

/**
 * Settings home: only the categories (each one a page of its own) and, below, the About entry. Nothing is loose
 * here: every option lives in a category. The entry point opens each page.
 */
@Composable
fun SettingsScreen(
    onOpenAppearance: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSystem: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(stringResource(R.string.tab_settings), modifier) {
        IconRowGroup(
            listOf(
                IconRowItem(
                    ImageVector.vectorResource(R.drawable.ic_palette),
                    RowTone.PRIMARY,
                    stringResource(R.string.settings_cat_appearance),
                    stringResource(R.string.settings_cat_appearance_hint),
                    onClick = onOpenAppearance,
                ),
                IconRowItem(
                    ImageVector.vectorResource(R.drawable.ic_download),
                    RowTone.SECONDARY,
                    stringResource(R.string.settings_cat_downloads),
                    stringResource(R.string.settings_cat_downloads_hint),
                    onClick = onOpenDownloads,
                ),
                IconRowItem(
                    ImageVector.vectorResource(R.drawable.ic_tune),
                    RowTone.TERTIARY,
                    stringResource(R.string.settings_cat_system),
                    stringResource(R.string.settings_cat_system_hint),
                    onClick = onOpenSystem,
                ),
            ),
        )
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
