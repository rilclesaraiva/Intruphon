package com.intruphon.app.core.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Swiping sideways on a list moves to the next tab (finger to the left) or the previous one (finger to the right),
 * one tab per swipe. The tabs are the pills above the list; tapping a pill still works.
 */
@Composable
fun Modifier.swipeBetweenTabs(selected: Int, count: Int, onSelect: (Int) -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    val current by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)
    return pointerInput(count) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragCancel = { total = 0f },
            onDragEnd = {
                val next = tabAfterSwipe(current, count, total, threshold)
                if (next != current) select(next)
            },
        ) { _, delta -> total += delta }
    }
}

/** The tab a horizontal drag of [dragged] pixels lands on: the same one when the drag is short or already at an end. */
internal fun tabAfterSwipe(selected: Int, count: Int, dragged: Float, threshold: Float): Int = when {
    dragged <= -threshold -> minOf(selected + 1, count - 1)
    dragged >= threshold -> maxOf(selected - 1, 0)
    else -> selected
}
