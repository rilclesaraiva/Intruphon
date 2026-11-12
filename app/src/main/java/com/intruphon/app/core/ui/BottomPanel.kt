package com.intruphon.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.intruphon.app.R
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration
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

/** Title, message and actions of one step, with the side margins the steps of a [BottomPanelFlow] share. */
@Composable
fun PanelBody(title: String? = null, message: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) Text(title, style = MaterialTheme.typography.headlineSmall)
        if (message != null) Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/**
 * One panel with several steps (0 is the first). Changing [step] makes what is showing slide out to the
 * right and the next step arrive from the same side, with a light spring; from step 1 on a small back
 * button sits at the top left and goes back one step ([onBack]), also on the system back. The panel is as
 * tall as the step it shows and grows or shrinks with a spring when the step changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomPanelFlow(
    onDismiss: () -> Unit,
    step: Int,
    onBack: () -> Unit,
    content: @Composable ColumnScope.(Int) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        BackHandler(enabled = step > 0, onBack = onBack)
        var shown by remember { mutableIntStateOf(step) }
        val away = remember { Animatable(0f) }   // 0 in place, 1 off to the right
        LaunchedEffect(step) {
            withContext(AppMotion.FixedScale) {
                if (step != shown) {
                    away.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                    shown = step
                }
                away.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
            }
        }
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.7f
        Column(Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .graphicsLayer {
                        translationX = away.value * size.width
                        alpha = (1f - away.value).coerceIn(0f, 1f)
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shown > 0) {
                    FloatingCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onBack,
                        modifier = Modifier.padding(start = 16.dp),
                        mini = true,
                    )
                }
                content(shown)
            }
        }
    }
}
