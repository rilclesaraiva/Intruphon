package com.intruphon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.intruphon.app.core.ui.AppMotion
import com.intruphon.app.core.ui.FloatingCircleButton
import com.intruphon.app.core.ui.IntruphonTheme
import com.intruphon.app.data.settings.AppSettings
import com.intruphon.app.data.update.UpdateRepository
import com.intruphon.app.features.about.AboutScreen
import com.intruphon.app.features.download.DownloadSheet
import com.intruphon.app.features.downloads.ActiveDownloadsScreen
import com.intruphon.app.features.downloads.DownloadsScreen
import com.intruphon.app.features.home.HomeScreen
import com.intruphon.app.features.settings.SettingsScreen
import com.intruphon.app.features.update.UpdatePrompt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOME = 0
private const val DOWNLOADS = 1
private const val SETTINGS = 2
private const val ABOUT = 3
private const val ACTIVE = 4

// Entry point: registers the pages. Features never import each other.
//
// Rules for the screen (CLAUDE.md, "Interface"): no fixed top bar and no bottom bar, a floating Settings
// button on Home (a floating Back button inside the other pages), predictive back between pages, and every
// page remembers where it was left (the SaveableStateProvider around each page keeps whatever it holds with
// rememberSaveable, rememberScrollState or rememberLazyListState).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        AppSettings.loadPredictiveBack(applicationContext)
        // Opening the app from scratch always looks for an update; coming back does it at most every 10 minutes.
        UpdateRepository.start(applicationContext, versionName, fresh = savedInstanceState == null)
        lifecycle.addObserver(LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) UpdateRepository.onResume() })
        setContent { IntruphonTheme { App(versionName) } }
    }
}

@Composable
private fun App(versionName: String) {
    // Pages opened from Home, in order. Home is always first; the last one is on screen.
    val stack = rememberSaveable(
        saver = listSaver<SnapshotStateList<Int>, Int>(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf(HOME) }
    var sheetLink by rememberSaveable { mutableStateOf<String?>(null) }
    // True while the Downloads page shows its picking bar, which takes the place of the Back button.
    var picking by rememberSaveable { mutableStateOf(false) }
    val predictive by AppSettings.predictiveBackFlow.collectAsState()
    val pages = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // How far the top page is out of the way: 0 in place, 1 fully slid to the right. The same value drives
    // opening a page, the back gesture, the back button and the system back.
    val exit = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }

    suspend fun slide(to: Float) = withContext(AppMotion.FixedScale) {
        exit.animateTo(to, tween(AppMotion.SHORT, easing = FastOutSlowInEasing))
    }

    fun open(page: Int) {
        if (busy || page in stack) return
        busy = true
        focusManager.clearFocus()   // the link field must not keep blinking its cursor under the new page
        scope.launch {
            stack.add(page)
            exit.snapTo(1f)
            slide(0f)
            busy = false
        }
    }

    // The page is swapped after the slide ends, without animating a second time.
    fun close() {
        if (busy || stack.size < 2) return
        busy = true
        scope.launch {
            slide(1f)
            pages.removeState(stack.last())   // a page that is left for good forgets where it was
            stack.removeAt(stack.lastIndex)
            exit.snapTo(0f)
            busy = false
        }
    }

    // Only for pages opened from Home; on Home the system closes the app as usual.
    PredictiveBackHandler(enabled = stack.size > 1 && predictive) { progress ->
        if (busy) {
            progress.collect { }
            return@PredictiveBackHandler
        }
        busy = true
        try {
            progress.collect { exit.snapTo(it.progress) }
            slide(1f)
            pages.removeState(stack.last())
            stack.removeAt(stack.lastIndex)
            exit.snapTo(0f)
        } catch (e: CancellationException) {
            withContext(NonCancellable) { slide(0f) }   // gesture cancelled: the page returns to its place
        } finally {
            busy = false
        }
    }
    BackHandler(enabled = stack.size > 1 && !predictive) { close() }

    @Composable
    fun Page(page: Int) = pages.SaveableStateProvider(page) {
        when (page) {
            HOME -> HomeScreen(onDownload = { sheetLink = it }, onOpenDownloads = { open(DOWNLOADS) })
            DOWNLOADS -> DownloadsScreen(onRetry = { sheetLink = it }, onOpenActive = { open(ACTIVE) }, onSelecting = { picking = it })
            ACTIVE -> ActiveDownloadsScreen()
            SETTINGS -> SettingsScreen(onOpenAbout = { open(ABOUT) })
            else -> AboutScreen(versionName)
        }
    }

    // The pages draw behind the system bars (edge to edge); each page keeps its content clear of them.
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
        Box(Modifier.fillMaxSize()) {
            // Every page of the stack stays composed in its own layer, keyed by the page, so nothing is rebuilt when a
            // page goes from top to below: scroll, fields and thumbnails stay. Each layer carries its own floating button.
            val last = stack.lastIndex
            stack.forEachIndexed { index, page ->
                key(page) {
                    val isTop = index == last
                    val isBelow = index == last - 1
                    val layer = when {
                        isTop -> Modifier
                            .pointerInput(Unit) {}   // the layers underneath never receive touches
                            .graphicsLayer {
                                translationX = exit.value * size.width
                                // Shadow and rounded corners appear right at the start of the movement.
                                val grow = minOf(1f, exit.value * 12f)
                                shape = RoundedCornerShape(28.dp * grow)
                                clip = true
                                shadowElevation = 16.dp.toPx() * grow
                            }
                        // The page underneath shows while the top one slides: it moves in from 20% to the left, under a dark veil.
                        isBelow -> Modifier
                            .clearAndSetSemantics {}
                            .graphicsLayer {
                                alpha = if (exit.value > 0f) 1f else 0f
                                translationX = -0.2f * size.width * (1f - exit.value)
                            }
                        else -> Modifier.clearAndSetSemantics {}.graphicsLayer { alpha = 0f }
                    }
                    Box(layer.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        Page(page)
                        if (page == HOME) {
                            FloatingCircleButton(
                                icon = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_open),
                                onClick = { open(SETTINGS) },
                                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
                            )
                        } else if (!(page == DOWNLOADS && picking)) {
                            FloatingCircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                onClick = { close() },
                                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp),
                            )
                        }
                        if (isBelow) {
                            Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black.copy(alpha = 0.32f * (1f - exit.value))) })
                        }
                    }
                }
            }
        }
    }
    UpdatePrompt()
    sheetLink?.let {
        DownloadSheet(link = it, onDismiss = { sheetLink = null })
    }
}
