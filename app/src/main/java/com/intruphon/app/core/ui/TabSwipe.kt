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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The tabs of a page as a carousel of lists: only the lists move with the finger (the next one is seen coming in);
 * the [header] (title and pills) stays where it is while swiping and scrolls away with the list like any other
 * content. Letting go finishes the move (or goes back) with a spring; tapping a pill moves the same way, because
 * [selected] is only where it ends up. [header] receives the position in pages (1.0 is the second tab, in between
 * is a move in progress) so the pills can follow it. Each page gets its own list state and the height to leave free
 * at the top of its list for the header ([page] puts it as the list's first item).
 */
@Composable
fun TabPager(
    selected: Int,
    count: Int,
    onSelect: (Int) -> Unit,
    header: @Composable (position: () -> Float) -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (index: Int, state: LazyListState, headerSpace: Dp) -> Unit,
) {
    val position = remember { Animatable(selected.toFloat()) }
    var width by remember { mutableIntStateOf(1) }
    var headerHeight by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)
    val states = List(count) { rememberLazyListState() }

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
        val headerSpace = with(LocalDensity.current) { headerHeight.toDp() }
        for (index in visible) key(index) {
            Box(Modifier.fillMaxSize().graphicsLayer { translationX = (index - position.value) * size.width }) {
                page(index, states[index], headerSpace)
            }
        }
        // Above the lists, going up with the one that is selected until it is out of sight.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { headerHeight = it.height }
                .graphicsLayer {
                    val state = states[current.coerceIn(0, count - 1)]
                    val scrolled = if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
                    translationY = -minOf(scrolled, headerHeight.toFloat())
                },
        ) { header { position.value } }
    }
}

/**
 * The pills of a [TabPager]: a filled pill slides behind the labels following [position] (a tap, a flick or the finger
 * itself), and the label colors fade with it.
 */
@Composable
fun TabPills(labels: List<String>, position: () -> Float, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val bounds = remember(labels.size) { mutableStateMapOf<Int, IntRange>() }
    val density = LocalDensity.current
    val now = position()
    Box(modifier) {
        val lower = floor(now).toInt().coerceIn(0, labels.size - 1)
        val upper = minOf(lower + 1, labels.size - 1)
        val a = bounds[lower]
        val b = bounds[upper]
        if (a != null && b != null) {
            val f = (now - lower).coerceIn(0f, 1f)
            val x = a.first + (b.first - a.first) * f
            val w = (a.last - a.first) + ((b.last - b.first) - (a.last - a.first)) * f
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), 0) }
                    .size(with(density) { w.toDp() }, PILL_HEIGHT)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { index, label ->
                val near = (1f - abs(now - index)).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .onGloballyPositioned { bounds[index] = it.positionInParent().x.roundToInt()..(it.positionInParent().x.roundToInt() + it.size.width) }
                        .height(PILL_HEIGHT)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 1f - near), CircleShape)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = lerp(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.onSecondaryContainer, near),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val PILL_HEIGHT = 32.dp

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
