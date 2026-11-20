package com.intruphon.app.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager

/**
 * When the keyboard closes (back gesture, hide button) the field loses focus, instead of staying with its cursor
 * blinking as if the keyboard were still open. Put it on any text field.
 */
@Composable
fun Modifier.clearFocusWhenKeyboardCloses(): Modifier {
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        var wasVisible = false
        snapshotFlow { ime.getBottom(density) > 0 }.collect { visible ->
            if (wasVisible && !visible) focus.clearFocus()
            wasVisible = visible
        }
    }
    return onFocusChanged { focused = it.isFocused }
}
