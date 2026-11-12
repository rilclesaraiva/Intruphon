package com.intruphon.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The one way to ask or tell something: a panel that rises from the bottom, like every other pop-up
 * in the app. There are no windows in the middle of the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomPanel(
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (title != null) Text(title, style = MaterialTheme.typography.headlineSmall)
            if (message != null) Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

/** A full-width action in a [BottomPanel]. [main] is the filled one; [danger] paints it as destructive. */
@Composable
fun PanelAction(label: String, onClick: () -> Unit, main: Boolean = false, danger: Boolean = false) {
    val modifier = Modifier.fillMaxWidth()
    when {
        main && danger -> Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
        ) { Text(label) }
        main -> Button(onClick = onClick, modifier = modifier) { Text(label) }
        danger -> FilledTonalButton(onClick = onClick, modifier = modifier) { Text(label, color = MaterialTheme.colorScheme.error) }
        else -> TextButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}
