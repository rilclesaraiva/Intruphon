package com.intruphon.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * A setting with a list of choices: it starts closed, showing only its title and what is chosen ([summary]);
 * tapping opens the choices ([content], usually [ChoiceRow]s) right under it, with a spring. Looks like the
 * rows of [IconRowGroup] (round colored icon, big rounded corners).
 */
@Composable
fun ExpandableOptionRow(
    icon: ImageVector,
    tone: RowTone,
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 180f else 0f, AppMotion.spatial(), label = "optionArrow")
    val scheme = MaterialTheme.colorScheme
    val (container, onContainer) = tone.colors()
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainerLow) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = onContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(rotation))
            }
            AnimatedVisibility(
                visible = open,
                enter = expandVertically(AppMotion.spatial<IntSize>()) + fadeIn(AppMotion.effects()),
                exit = shrinkVertically(AppMotion.spatial<IntSize>()) + fadeOut(AppMotion.effects()),
            ) {
                Column(Modifier.padding(bottom = 8.dp), content = content)
            }
        }
    }
}

/** One choice inside an [ExpandableOptionRow]: a radio button and its label; the whole row is the target. */
@Composable
fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 10.dp))
    }
}
