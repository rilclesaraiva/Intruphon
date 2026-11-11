package com.intruphon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.intruphon.app.core.ui.IntruphonTheme
import com.intruphon.app.features.about.AboutScreen
import com.intruphon.app.features.download.DownloadSheet
import com.intruphon.app.features.downloads.DownloadsScreen
import com.intruphon.app.features.home.HomeScreen
import com.intruphon.app.features.settings.SettingsScreen

// Entry point: registers the routes. Features never import each other.
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        val tabs = listOf(R.string.tab_home, R.string.tab_downloads, R.string.tab_settings, R.string.menu_about)
        setContent {
            IntruphonTheme {
                var tab by rememberSaveable { mutableStateOf(0) }
                var sheetLink by rememberSaveable { mutableStateOf<String?>(null) }
                BackHandler(enabled = tab != 0) { tab = 0 }
                Scaffold(
                    topBar = {
                        Column {
                            TopAppBar(title = { Text(stringResource(R.string.home_title)) })
                            TabRow(selectedTabIndex = tab) {
                                tabs.forEachIndexed { index, label ->
                                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(label)) })
                                }
                            }
                        }
                    },
                ) { padding ->
                    val m = Modifier.padding(padding)
                    when (tab) {
                        0 -> HomeScreen(onDownload = { sheetLink = it }, modifier = m)
                        1 -> DownloadsScreen(onRetry = { sheetLink = it }, modifier = m)
                        2 -> SettingsScreen(modifier = m)
                        else -> AboutScreen(versionName, m)
                    }
                }
                sheetLink?.let {
                    DownloadSheet(link = it, onDismiss = { sheetLink = null }, onStarted = { tab = 1 })
                }
            }
        }
    }
}
