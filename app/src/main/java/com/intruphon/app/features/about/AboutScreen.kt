package com.intruphon.app.features.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.runtime.produceState
import com.intruphon.app.data.net.Avatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.intruphon.app.BuildConfig
import com.intruphon.app.R
import com.intruphon.app.core.Config
import com.intruphon.app.core.ui.IconRowGroup
import com.intruphon.app.core.ui.IconRowItem
import com.intruphon.app.core.ui.PageHeader
import com.intruphon.app.core.ui.RowTone
import com.intruphon.app.core.ui.SectionLabel

/** About Intruphon: header, developer, project, legal and the invitation to the repository (rule 9.1). */
@Composable
fun AboutScreen(versionName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var licenses by rememberSaveable { mutableStateOf(false) }
    // The profile picture comes from GitHub and is cached; until it arrives (or when offline) an icon shows.
    val avatar by produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) { Avatar.load(context, Config.MAINTAINER)?.asImageBitmap() }
    }
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageHeader(stringResource(R.string.about_title), afterBack = true)

        // Header: icon, name, version and build type (read from the build), one sentence.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(120.dp).clip(CircleShape).background(colorResource(R.color.ic_launcher_background)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize().scale(1.35f))
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(versionName)
                Chip(stringResource(R.string.about_build, BuildConfig.BUILD))
                Chip(BuildConfig.BUILD_TYPE.uppercase())
            }
            Text(stringResource(R.string.about_tagline), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }

        SectionLabel(stringResource(R.string.about_developer))
        IconRowGroup(listOf(IconRowItem(Icons.Default.Person, RowTone.PRIMARY, Config.MAINTAINER, Config.PROFILE_URL.removePrefix("https://"), avatar) { open(Config.PROFILE_URL) }))

        SectionLabel(stringResource(R.string.about_project))
        IconRowGroup(
            listOf(
                IconRowItem(ImageVector.vectorResource(R.drawable.ic_code), RowTone.SECONDARY, stringResource(R.string.about_source), Config.REPO_URL.removePrefix("https://")) { open(Config.REPO_URL) },
                IconRowItem(ImageVector.vectorResource(R.drawable.ic_verified), RowTone.TERTIARY, stringResource(R.string.about_news), stringResource(R.string.about_news_hint)) { open(Config.CHANGELOG_URL) },
                IconRowItem(ImageVector.vectorResource(R.drawable.ic_bug_report), RowTone.PRIMARY, stringResource(R.string.about_report), stringResource(R.string.about_report_hint)) { open(Config.ISSUES_URL) },
            ),
        )

        SectionLabel(stringResource(R.string.about_legal))
        IconRowGroup(
            listOf(
                IconRowItem(Icons.Default.Lock, RowTone.NEUTRAL, stringResource(R.string.about_privacy), stringResource(R.string.about_privacy_hint)) { open(Config.PRIVACY_URL) },
                IconRowItem(ImageVector.vectorResource(R.drawable.ic_article), RowTone.NEUTRAL, stringResource(R.string.about_terms), stringResource(R.string.about_terms_hint)) { open(Config.TERMS_URL) },
                IconRowItem(ImageVector.vectorResource(R.drawable.ic_article), RowTone.NEUTRAL, stringResource(R.string.about_licenses), stringResource(R.string.about_licenses_hint)) { licenses = true },
            ),
        )

        // Only an invitation that leads to the repository: no donation or payment wording in the app.
        IconRowGroup(
            listOf(IconRowItem(Icons.Default.Favorite, RowTone.TERTIARY, stringResource(R.string.about_support_title), stringResource(R.string.about_support_hint)) { open(Config.REPO_URL) }),
        )
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    if (licenses) {
        AlertDialog(
            onDismissRequest = { licenses = false },
            confirmButton = { TextButton(onClick = { licenses = false }) { Text(stringResource(R.string.download_close)) } },
            title = { Text(stringResource(R.string.about_licenses)) },
            text = { Text(stringResource(R.string.about_licenses_body), Modifier.verticalScroll(rememberScrollState())) },
        )
    }
}

@Composable
private fun Chip(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}
