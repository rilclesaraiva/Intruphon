package com.intruphon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.core.ui.FloatingCircleButton
import com.intruphon.app.core.ui.IntruphonTheme
import com.intruphon.app.features.about.AboutScreen
import com.intruphon.app.features.download.DownloadSheet
import com.intruphon.app.features.downloads.DownloadsScreen
import com.intruphon.app.features.home.HomeScreen
import com.intruphon.app.features.settings.SettingsScreen

private const val HOME = 0
private const val DOWNLOADS = 1
private const val SETTINGS = 2
private const val ABOUT = 3

// Entry point: registers the pages. Features never import each other.
//
// Rules for the screen (CLAUDE.md, "Interface"): no fixed top bar, a floating Settings button
// (a floating Back button inside sub-pages), and every page remembers where it was left.
// The last rule is the SaveableStateProvider around each page: whatever a page keeps with
// rememberSaveable, rememberScrollState or rememberLazyListState survives leaving the page.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            IntruphonTheme {
                var page by rememberSaveable { mutableIntStateOf(HOME) }
                var returnTo by rememberSaveable { mutableIntStateOf(HOME) }   // main page Settings goes back to
                var sheetLink by rememberSaveable { mutableStateOf<String?>(null) }
                val pages = rememberSaveableStateHolder()
                val onMainPage = page == HOME || page == DOWNLOADS

                BackHandler(enabled = page != HOME) {
                    page = when (page) {
                        ABOUT -> SETTINGS
                        SETTINGS -> returnTo
                        else -> HOME
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (onMainPage) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = page == HOME,
                                    onClick = { page = HOME },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(stringResource(R.string.tab_home)) },
                                )
                                NavigationBarItem(
                                    selected = page == DOWNLOADS,
                                    onClick = { page = DOWNLOADS },
                                    icon = { Icon(painterResource(R.drawable.ic_nav_downloads), contentDescription = null) },
                                    label = { Text(stringResource(R.string.tab_downloads)) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        AnimatedContent(
                            targetState = page,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                    scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))) togetherWith
                                    fadeOut(spring(stiffness = Spring.StiffnessMedium))
                            },
                            label = "page",
                        ) { target ->
                            pages.SaveableStateProvider(target) {
                                when (target) {
                                    HOME -> HomeScreen(onDownload = { sheetLink = it })
                                    DOWNLOADS -> DownloadsScreen(onRetry = { sheetLink = it })
                                    SETTINGS -> SettingsScreen(onOpenAbout = { page = ABOUT })
                                    else -> AboutScreen(versionName)
                                }
                            }
                        }
                        if (onMainPage) {
                            FloatingCircleButton(
                                icon = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_open),
                                onClick = { returnTo = page; page = SETTINGS },
                                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                            )
                        } else {
                            FloatingCircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                onClick = {
                                    page = if (page == ABOUT) SETTINGS else returnTo
                                },
                                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                            )
                        }
                    }
                }
                sheetLink?.let {
                    DownloadSheet(link = it, onDismiss = { sheetLink = null }, onStarted = { page = DOWNLOADS })
                }
            }
        }
    }
}
