package com.intruphon.app.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.material3.SmallFloatingActionButton
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

/** Round floating button, used for Settings (top right) and Back (top left). [mini] is the small one used inside panels. */
@Composable
fun FloatingCircleButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, mini: Boolean = false) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)   // slightly see-through
    val content = MaterialTheme.colorScheme.onSurface
    if (mini) {
        SmallFloatingActionButton(onClick = onClick, modifier = modifier, shape = CircleShape, containerColor = container, contentColor = content) {
            Icon(icon, contentDescription = contentDescription)
        }
    } else {
        FloatingActionButton(onClick = onClick, modifier = modifier, shape = CircleShape, containerColor = container, contentColor = content) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
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

/** Each icon has its own color, like the settings of the maintainer's other app: a dark tint behind a light icon (the other way round in the light theme). */
enum class RowTone(private val darkContainer: Long, private val darkContent: Long, private val lightContainer: Long, private val lightContent: Long) {
    PURPLE(0xFF3D2A6B, 0xFFD0BCFF, 0xFFE8DDFF, 0xFF4A2F8A),
    BLUE(0xFF1E3A5F, 0xFFAECBFA, 0xFFD3E3FD, 0xFF1F4B8E),
    GREEN(0xFF1E4D2B, 0xFFA8DAB5, 0xFFC7EDD1, 0xFF1B5E2F),
    AMBER(0xFF5A3F00, 0xFFFFC857, 0xFFFFE2A8, 0xFF6B4A00),
    TEAL(0xFF0F4F4A, 0xFF84D8CF, 0xFFBFEFEA, 0xFF0D5A54),
    PINK(0xFF5B1A44, 0xFFFFADD9, 0xFFFFD8EC, 0xFF7A1E55),
    RED(0xFF6B1A1A, 0xFFF8B4B4, 0xFFFFD9D6, 0xFF8C1D18);

    /** The circle's color and the icon's color for the current theme. */
    @Composable
    fun colors(): Pair<Color, Color> =
        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(darkContainer) to Color(darkContent)
        else Color(lightContainer) to Color(lightContent)
}

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
                    val (container, content) = item.tone.colors()
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

/**
 * The three bars of the app icon as a progress indicator: each one fills to [fraction] (0 to 1), the shortest at the
 * bottom. With no known progress ([fraction] null) the bars stay empty.
 */
@Composable
fun FunnelProgress(fraction: Float?, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(36.dp, 23.dp)) {
        val bar = 5.dp.toPx()
        val gap = 4.dp.toPx()
        val done = (fraction ?: 0f).coerceIn(0f, 1f)
        listOf(1f, 0.8f, 0.6f).forEachIndexed { row, share ->
            val top = Offset(0f, row * (bar + gap))
            val length = size.width * share
            val corner = CornerRadius(bar / 2)
            drawRoundRect(track, top, Size(length, bar), corner)
            if (done > 0f) drawRoundRect(fill, top, Size(maxOf(length * done, bar), bar), corner)
        }
    }
}
