package com.intruphon.app

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
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
import com.intruphon.app.features.search.SearchScreen
import com.intruphon.app.features.downloads.ActiveDownloadsScreen
import com.intruphon.app.features.downloads.DownloadsScreen
import com.intruphon.app.features.home.HomeScreen
import com.intruphon.app.features.settings.AppearanceSettings
import com.intruphon.app.features.settings.DownloadSettings
import com.intruphon.app.features.settings.SettingsScreen
import com.intruphon.app.features.settings.SystemSettings
import com.intruphon.app.features.update.UpdatePrompt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HOME = 0
private const val DOWNLOADS = 1
private const val SETTINGS = 2
private const val ABOUT = 3
private const val ACTIVE = 4
private const val SETTINGS_APPEARANCE = 5
private const val SETTINGS_DOWNLOADS = 6
private const val SETTINGS_SYSTEM = 7
private const val SEARCH = 8

/** A name shared to the app as plain text, waiting for the app to open its search page. */
internal object ShareSearch {
    val pending = MutableStateFlow<String?>(null)
}

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
        AppSettings.loadTheme(applicationContext)
        // Opening the app from scratch always looks for an update; coming back does it at most every 10 minutes.
        UpdateRepository.start(applicationContext, versionName, fresh = savedInstanceState == null)
        lifecycle.addObserver(LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) UpdateRepository.onResume() })
        if (savedInstanceState == null) intent?.getStringExtra(EXTRA_SEARCH)?.let { ShareSearch.pending.value = it }
        setContent { IntruphonTheme { App(versionName) } }
    }

    // The app was already running when a name was shared to it (launchMode singleTask).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_SEARCH)?.let { ShareSearch.pending.value = it }
    }

    companion object {
        /** Extra with the name to search for, set by ShareActivity. */
        const val EXTRA_SEARCH = "intruphon.search"
    }
}

@Composable
private fun App(versionName: String) {
    // Pages opened from Home, in order. Home is always first; the last one is on screen.
    val stack = rememberSaveable(
        saver = listSaver<SnapshotStateList<Int>, Int>(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf(HOME) }
    var sheetLink by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf<String?>(null) }
    // True while the Downloads page shows its picking bar, which takes the place of the Back button.
    var picking by rememberSaveable { mutableStateOf(false) }
    val predictive by AppSettings.predictiveBackFlow.collectAsState()
    val pages = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // How far the top page is out of the way: 0 in place, 1 fully slid to the right. The same value drives
    // opening a page, the back gesture, the back button and the system back.
    val exit = remember { Animatable(0f) }
    // A tap never waits for the animation: it ends the one in flight on the spot and starts its own.
    var navJob by remember { mutableStateOf<Job?>(null) }
    var commit by remember { mutableStateOf<(() -> Unit)?>(null) }   // what a closing slide still has to do to the stack
    var gesturing by remember { mutableStateOf(false) }               // the finger is dragging the back gesture

    suspend fun slide(to: Float) {
        exit.animateTo(to, tween(AppMotion.SHORT, easing = FastOutSlowInEasing))
    }

    // Ends the running transition now: an opening one is simply cut (the page is already in the stack), a closing one
    // takes its page off at once. The next transition snaps the offset to where it starts.
    fun finishNow() {
        navJob?.cancel()
        navJob = null
        commit?.invoke()
        commit = null
    }

    fun startClose(fromCurrent: Boolean) {
        commit = {
            pages.removeState(stack.last())   // a page that is left for good forgets where it was
            stack.removeAt(stack.lastIndex)
        }
        navJob = scope.launch {
            if (!fromCurrent) exit.snapTo(0f)
            slide(1f)
            commit?.invoke()   // the page is swapped after the slide ends, without animating a second time
            commit = null
            exit.snapTo(0f)
        }
    }

    fun open(page: Int) {
        if (gesturing) return
        if (page in stack && commit == null) return   // already open (or opening): a double tap is ignored
        finishNow()
        if (page in stack) return
        focusManager.clearFocus()   // the link field must not keep blinking its cursor under the new page
        navJob = scope.launch {
            stack.add(page)
            exit.snapTo(1f)
            slide(0f)
        }
    }

    fun close() {
        if (gesturing) return
        finishNow()
        if (stack.size < 2) return
        startClose(fromCurrent = false)
    }

    // Only for pages opened from Home; on Home the system closes the app as usual.
    PredictiveBackHandler(enabled = stack.size > 1 && predictive) { progress ->
        finishNow()
        if (stack.size < 2) {
            progress.collect { }
            return@PredictiveBackHandler
        }
        gesturing = true
        exit.snapTo(0f)
        try {
            progress.collect { exit.snapTo(it.progress) }
            gesturing = false
            startClose(fromCurrent = true)   // let go: the page finishes leaving, and a tap meanwhile is not lost
        } catch (e: CancellationException) {
            gesturing = false
            withContext(NonCancellable) { slide(0f) }   // gesture cancelled: the page returns to its place
        }
    }
    BackHandler(enabled = stack.size > 1 && !predictive) { close() }

    // A name shared to the app opens the search page (or, if it is already open, searches the new name there).
    val shared by ShareSearch.pending.collectAsState()
    LaunchedEffect(shared) {
        shared?.let {
            ShareSearch.pending.value = null
            searchQuery = it
            open(SEARCH)
        }
    }

    @Composable
    fun Page(page: Int) = pages.SaveableStateProvider(page) {
        when (page) {
            HOME -> HomeScreen(onDownload = { sheetLink = it }, onSearch = { searchQuery = it; open(SEARCH) }, onOpenDownloads = { open(DOWNLOADS) }, onOpenActive = { open(ACTIVE) })
            DOWNLOADS -> DownloadsScreen(onRetry = { sheetLink = it }, onOpenActive = { open(ACTIVE) }, onSelecting = { picking = it })
            ACTIVE -> ActiveDownloadsScreen()
            SETTINGS -> SettingsScreen(
                onOpenAppearance = { open(SETTINGS_APPEARANCE) },
                onOpenDownloads = { open(SETTINGS_DOWNLOADS) },
                onOpenSystem = { open(SETTINGS_SYSTEM) },
                onOpenAbout = { open(ABOUT) },
            )
            SETTINGS_APPEARANCE -> AppearanceSettings()
            SETTINGS_DOWNLOADS -> DownloadSettings()
            SETTINGS_SYSTEM -> SystemSettings()
            SEARCH -> SearchScreen(initialQuery = searchQuery.orEmpty(), onPick = { sheetLink = it })
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
