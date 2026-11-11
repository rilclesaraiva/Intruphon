package com.intruphon.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.intruphon.app.core.ui.IntruphonTheme
import com.intruphon.app.features.about.AboutScreen
import com.intruphon.app.features.home.HomeScreen

// Entry point: registers the routes. Features never import each other.
class MainActivity : ComponentActivity() {
    private var sharedText by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after rotation the saved field must not be overwritten.
        if (savedInstanceState == null) sharedText = sharedTextFrom(intent)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            IntruphonTheme {
                var showAbout by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = showAbout) { showAbout = false }
                LaunchedEffect(sharedText) { if (sharedText != null) showAbout = false }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(stringResource(if (showAbout) R.string.about_title else R.string.home_title))
                            },
                            actions = {
                                if (showAbout) {
                                    TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.about_back)) }
                                } else {
                                    TextButton(onClick = { showAbout = true }) { Text(stringResource(R.string.menu_about)) }
                                }
                            },
                        )
                    },
                ) { padding ->
                    val m = Modifier.padding(padding)
                    if (showAbout) AboutScreen(versionName, m) else HomeScreen(sharedText, m)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = sharedTextFrom(intent)
    }

    private fun sharedTextFrom(intent: Intent?): String? =
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain")
            intent.getStringExtra(Intent.EXTRA_TEXT)
        else null
}
