package com.intruphon.app.features.about

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.intruphon.app.R
import kotlin.math.abs
import kotlin.math.pow

// The app icon, drawn in Compose so it can move: the intruder looks left, right, then at you, and settles behind
// the funnel exactly as the launcher icon (see ic_launcher_foreground.xml). It plays when About opens and on every tap.
// Shapes live in the "art" space of the icon; ART_* places that space inside the 108-unit launcher canvas.
private const val ART_K = 0.78f
private const val ART_TX = 15.03f
private const val ART_TY = 19.01f
private const val END = 2.3f   // seconds

private val IconLight = Color(0xFFC7DDE8)
private fun shape(d: String): Path = PathParser().parsePathString(d).toPath()
private val Blob = shape("M0 28C-2 6 16 -6 36 -2C56 2 64 22 56 40C48 56 26 58 12 50C4 46 0 40 0 28Z")
private val Band = shape("M2 25L60 31L58 41L1 35Z")
private val EyeL = shape("M12 28L26 30L25 35L12 33Z")
private val EyeR = shape("M40 31L52 32.5L51 36L40 35Z")
private val Bars = listOf(
    shape("M13 52H87L80 61H20Z"),
    shape("M24 64H76L68 73H32Z"),
    shape("M34 76H66L58 85H42Z"),
)

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun smooth(x: Float) = x * x * (3 - 2 * x)
private fun clamp01(x: Float) = x.coerceIn(0f, 1f)
private fun back(x: Float): Float { val c1 = 1.2f; val c3 = c1 + 1; return 1 + c3 * (x - 1).pow(3) + c1 * (x - 1).pow(2) }

/** Smooth interpolation through (time, value) points. */
private fun keys(t: Float, k: List<Pair<Float, Float>>): Float {
    if (t <= k.first().first) return k.first().second
    for (i in 0 until k.size - 1) {
        if (t < k[i + 1].first) return lerp(k[i].second, k[i + 1].second, smooth((t - k[i].first) / (k[i + 1].first - k[i].first)))
    }
    return k.last().second
}

/** The looks: centre, left, right, centre again. */
private fun look(amount: Float) = listOf(0f to 0f, 0.3f to 0f, 0.55f to -amount, 0.72f to -amount, 1.0f to amount, 1.15f to amount, 1.4f to 0f)
private val EyeWide = listOf(0f to 1f, 1.35f to 1f, 1.45f to 1.3f, 1.75f to 1.3f, 2.0f to 1f)

@Composable
fun AboutIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val background = colorResource(R.color.ic_launcher_background)
    val time = remember { Animatable(0f) }
    var plays by remember { mutableIntStateOf(0) }
    LaunchedEffect(plays) {
        // With animations turned off in the system the icon just shows its final look.
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) time.snapTo(END) else {
            time.snapTo(0f)
            time.animateTo(END, tween((END * 1000).toInt(), easing = LinearEasing))
        }
    }
    Canvas(
        modifier
            .size(132.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { plays++ },
    ) {
        drawIntruder(time.value, background)
    }
}

private fun DrawScope.drawIntruder(t: Float, background: Color) {
    val u = clamp01((t - 1.65f) / 0.6f)          // the face settles into the icon
    val ease = back(u)
    val settle = smooth(u)
    val faceScale = when {
        u > 0f -> lerp(1.5f, 1f, ease)
        t < 0.28f -> lerp(0.6f, 1.5f, back(clamp01(t / 0.28f)))
        else -> 1.5f
    }
    val faceX = lerp(50f, 42f, settle) + keys(t, look(6f)) * (1 - settle)
    val faceY = lerp(42f, 30f, settle)
    val turn = keys(t, look(13f)) * (1 - settle)
    val faceAlpha = keys(t, listOf(0f to 0f, 0.18f to 1f))
    val eyeShift = keys(t, look(8f))
    val eyeScale = keys(t, EyeWide)
    val squash = 1 - 0.12f * abs(eyeShift) / 8f
    val funnelStep = clamp01((t - 1.7f) / 0.55f)
    val funnelAlpha = keys(t, listOf(1.7f to 0f, 1.85f to 1f))

    withTransform({
        scale(size.width / 108f, size.width / 108f, Offset.Zero)
        translate(ART_TX, ART_TY)
        scale(ART_K, ART_K, Offset.Zero)
    }) {
        withTransform({
            translate(faceX, faceY)
            rotate(turn, Offset.Zero)
            scale(faceScale, faceScale, Offset.Zero)
            translate(-36f, -26f)
        }) {
            drawPath(Blob, IconLight, alpha = faceAlpha)
            drawPath(Band, background, alpha = faceAlpha)
            for ((eye, cx, cy) in listOf(Triple(EyeL, 18.5f, 31.5f), Triple(EyeR, 46f, 33.5f))) {
                withTransform({
                    translate(eyeShift, 0f)
                    translate(cx, cy)
                    scale(eyeScale * squash, eyeScale, Offset.Zero)
                    translate(-cx, -cy)
                }) { drawPath(eye, IconLight, alpha = faceAlpha) }
            }
        }
        withTransform({
            translate(0f, lerp(130f, 0f, back(funnelStep)))
            rotate(lerp(35f, 0f, smooth(funnelStep)), Offset(60f, 68f))
            rotate(14f, Offset(60f, 68f))
            translate(8f, 4f)
        }) {
            for (bar in Bars) {
                drawPath(bar, IconLight, alpha = funnelAlpha)
                drawPath(bar, background, alpha = funnelAlpha, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
            }
        }
    }
}
