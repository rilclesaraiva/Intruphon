package com.intruphon.app.features.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.BuildConfig
import com.intruphon.app.R
import com.intruphon.app.core.Config
import com.intruphon.app.core.ui.PageHeader
import com.intruphon.app.core.ui.SectionCard
import com.intruphon.app.core.ui.SectionLabel

/** About Intruphon: header, developer, project, legal and the invitation to the repository (rule 9.1). */
@Composable
fun AboutScreen(versionName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var licenses by rememberSaveable { mutableStateOf(false) }
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageHeader(stringResource(R.string.settings_about_entry))

        // Header: icon, name, version and build type (read from the build), one sentence.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(colorResource(R.color.ic_launcher_background)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize().scale(1.35f))
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.about_version, "$versionName · build ${BuildConfig.BUILD} · ${BuildConfig.BUILD_TYPE}"),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.about_tagline), style = MaterialTheme.typography.bodyMedium)
        }

        SectionLabel(stringResource(R.string.about_developer))
        SectionCard(Modifier.clickable { open(Config.PROFILE_URL) }) { Row1(Config.MAINTAINER) }

        SectionLabel(stringResource(R.string.about_project))
        SectionCard {
            Link(stringResource(R.string.about_source)) { open(Config.REPO_URL) }
            Link(stringResource(R.string.about_news)) { open(Config.CHANGELOG_URL) }
            Link(stringResource(R.string.about_report)) { open(Config.ISSUES_URL) }
        }

        SectionLabel(stringResource(R.string.about_legal))
        SectionCard {
            Link(stringResource(R.string.about_privacy)) { open(Config.PRIVACY_URL) }
            Link(stringResource(R.string.about_terms)) { open(Config.TERMS_URL) }
            Link(stringResource(R.string.about_licenses)) { licenses = true }
        }

        SectionLabel(stringResource(R.string.about_support_title))
        // Only an invitation that leads to the repository: no donation or payment wording in the app.
        SectionCard(Modifier.clickable { open(Config.REPO_URL) }) {
            Text(stringResource(R.string.about_support))
        }
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
private fun Row1(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))

@Composable
private fun Link(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}
