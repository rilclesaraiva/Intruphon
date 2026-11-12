package com.intruphon.app.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Page title that scrolls away with the content (the app has no fixed top bar). It always sits beside the
 * floating button, never under it: pages with a Back button (top left) pass [afterBack] = true.
 */
@Composable
fun PageHeader(title: String, modifier: Modifier = Modifier, afterBack: Boolean = false) {
    val padding = if (afterBack) Modifier.padding(start = 72.dp) else Modifier.padding(end = 72.dp)
    Box(modifier.fillMaxWidth().statusBarsPadding().height(80.dp).then(padding), contentAlignment = Alignment.CenterStart) {
        Text(title, style = MaterialTheme.typography.headlineLarge, maxLines = 1)
    }
}

/** Round floating button, used for Settings (top right) and Back (top left). */
@Composable
fun FloatingCircleButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),   // slightly see-through
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { Icon(icon, contentDescription = contentDescription) }
}

/** Small colored label that opens a group of settings. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, top = 8.dp),
    )
}

/** Rounded container for one group of rows. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

enum class RowTone { PRIMARY, SECONDARY, TERTIARY, NEUTRAL }

/** One tappable row: round colored icon, title and an optional line under it. */
data class IconRowItem(
    val icon: ImageVector,
    val tone: RowTone,
    val title: String,
    val subtitle: String? = null,
    /** A picture shown in the circle instead of the icon, when there is one. */
    val image: ImageBitmap? = null,
    val onClick: () -> Unit,
)

/** A group of [IconRowItem]s: big rounded corners on the outside, thin gaps between rows. */
@Composable
fun IconRowGroup(items: List<IconRowItem>, modifier: Modifier = Modifier) {
    val big = 32.dp
    val small = 6.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEachIndexed { index, item ->
            val first = index == 0
            val last = index == items.lastIndex
            val shape = RoundedCornerShape(
                topStart = if (first) big else small,
                topEnd = if (first) big else small,
                bottomStart = if (last) big else small,
                bottomEnd = if (last) big else small,
            )
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = item.onClick),
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val scheme = MaterialTheme.colorScheme
                    val (container, content) = when (item.tone) {
                        RowTone.PRIMARY -> scheme.primaryContainer to scheme.onPrimaryContainer
                        RowTone.SECONDARY -> scheme.secondaryContainer to scheme.onSecondaryContainer
                        RowTone.TERTIARY -> scheme.tertiaryContainer to scheme.onTertiaryContainer
                        RowTone.NEUTRAL -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
                    }
                    Box(Modifier.size(52.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
                        if (item.image != null) {
                            Image(item.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(item.icon, contentDescription = null, tint = content)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}
