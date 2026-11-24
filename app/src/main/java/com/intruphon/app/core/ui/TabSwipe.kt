package com.intruphon.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The tabs of a page as a carousel: the next page follows the finger and is seen coming in; letting go finishes the
 * move (or goes back) with a spring. Tapping a pill moves the same way, because [selected] is only where it ends up.
 * Each page draws its own title and pills (they scroll with its list) and keeps its own scroll position.
 */
@Composable
fun TabPager(selected: Int, count: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, page: @Composable (Int) -> Unit) {
    // Where the pages are, in pages: 1.0 is the second one; between two numbers is a move in progress.
    val position = remember { Animatable(selected.toFloat()) }
    var width by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)
    val holder = rememberSaveableStateHolder()

    LaunchedEffect(selected) { position.animateTo(selected.toFloat(), AppMotion.spatial()) }

    val drag = rememberDraggableState { delta ->
        val next = position.value - delta / width
        scope.launch { position.snapTo(next.coerceIn(0f, (count - 1).toFloat())) }
    }
    val visible by remember(count) {
        derivedStateOf { floor(position.value).toInt().coerceIn(0, count - 1)..ceil(position.value).toInt().coerceIn(0, count - 1) }
    }

    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { width = maxOf(it.width, 1) }
            .draggable(drag, Orientation.Horizontal, onDragStopped = { velocity ->
                val target = tabAfterRelease(current, count, position.value - current, velocity / width)
                if (target != current) select(target) else scope.launch { position.animateTo(current.toFloat(), AppMotion.spatial()) }
            }),
    ) {
        for (index in visible) key(index) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { translationX = (index - position.value) * size.width },
            ) {
                holder.SaveableStateProvider(index) { page(index) }
            }
        }
    }
}

/**
 * The tab a drag ends on: the next or previous one when the pages moved more than a third of the way or were flicked
 * ([flick] in pages per second), otherwise the same one. [moved] is the distance from [selected], in pages.
 */
internal fun tabAfterRelease(selected: Int, count: Int, moved: Float, flick: Float): Int = when {
    moved > 0.33f || (flick < -FLICK && moved > 0f) -> minOf(selected + 1, count - 1)
    moved < -0.33f || (flick > FLICK && moved < 0f) -> maxOf(selected - 1, 0)
    else -> selected
}

private const val FLICK = 1.5f
