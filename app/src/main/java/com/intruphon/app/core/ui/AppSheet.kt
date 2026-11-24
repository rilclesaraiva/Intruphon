package com.intruphon.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Tallest a sheet gets, as a share of the screen: it never reaches the top, so a strip above shows it can be pulled down. */
private const val MAX_SHEET_FRACTION = 0.88f

/**
 * The only way the app shows a bottom sheet (used by [BottomPanel], [BottomPanelFlow] and the sheets of the
 * download features) instead of `ModalBottomSheet` directly.
 *
 * The difference is the back: Material's own back shrinks the sheet with the system animation while the gesture
 * happens. Here back is ours: the sheet goes down with the normal closing animation and only then [onDismiss] runs.
 * The sheet state (still experimental) stays in here, so callers need no `@OptIn`. To close from a button inside,
 * use [AppSheetScope.dismiss].
 *
 * [dragOnlyFromTop]: only the top strip (the handle) drags the sheet; scrolling the content to its top and
 * pulling on does not close it. For sheets with something to lose (a selection, a half-made choice).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dragOnlyFromTop: Boolean = false,
    content: @Composable AppSheetScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismiss)
    // Goes down first, then closes.
    val hideThenDismiss: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { dismiss() } }
    ModalBottomSheet(
        onDismissRequest = { dismiss() },
        modifier = modifier,
        sheetState = sheetState,
        // Without Material's back (and the system's shrinking animation): see the back handler below.
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        BackHandler(onBack = hideThenDismiss)
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * MAX_SHEET_FRACTION
        Column(Modifier.animateContentSize(AppMotion.spatial()).heightIn(max = maxHeight).then(if (dragOnlyFromTop) Modifier.nestedScroll(KeepScrollInside) else Modifier)) {
            val sheetScope = remember(this) { AppSheetScopeImpl(this, hideThenDismiss) }
            sheetScope.content()
        }
    }
}

/** The content of an [AppSheet]: a column that also knows how to close the sheet going down. */
interface AppSheetScope : ColumnScope {
    /** Closes the sheet with the going-down animation (then calls `onDismiss`). */
    fun dismiss()
}

private class AppSheetScopeImpl(column: ColumnScope, private val close: () -> Unit) : AppSheetScope, ColumnScope by column {
    override fun dismiss() = close()
}

/** Takes the scroll the content did not use (pulling past the top) so it never reaches the sheet and drags it. */
private object KeepScrollInside : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}
