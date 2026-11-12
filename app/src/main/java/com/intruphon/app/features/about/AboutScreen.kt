package com.intruphon.app.features.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.BuildConfig
import com.intruphon.app.R
import com.intruphon.app.core.Config
import com.intruphon.app.core.ui.PageHeader

@Composable
fun AboutScreen(versionName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(stringResource(R.string.settings_about_entry))
        Text(stringResource(R.string.about_version, versionName + " (build " + BuildConfig.BUILD + ")"), style = MaterialTheme.typography.titleMedium)
        // Only an invitation that leads to the repository: no donation or payment wording in the app.
        Text(stringResource(R.string.about_support))
        Button(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.REPO_URL)))
        }) { Text(stringResource(R.string.about_open_repo)) }
    }
}
