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

// The app icon, drawn in Compose so it can move: the intruder looks left, right, then at you, and settles behind
// the funnel exactly as the launcher icon (see ic_launcher_foreground.xml). It plays when About opens and on every tap.
// Shapes live in the "art" space of the icon; ART_* places that space inside the 108-unit launcher canvas.
private const val ART_K = 0.78f
private const val ART_TX = 15.03f
private const val ART_TY = 19.01f
private const val VISIBLE = 72f   // the part of the 108-unit canvas a launcher shows

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

@Composable
fun AboutIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val background = colorResource(R.color.ic_launcher_background)
    val time = remember { Animatable(0f) }
    var plays by remember { mutableIntStateOf(0) }
    LaunchedEffect(plays) {
        // With animations turned off in the system the icon just shows its final look.
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) time.snapTo(ICON_END) else {
            time.snapTo(0f)
            time.animateTo(ICON_END, tween((ICON_END * 1000).toInt(), easing = LinearEasing))
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
    val f = iconFrame(t)
    val faceScale = f.faceScale
    val faceX = f.faceX
    val faceY = f.faceY
    val turn = f.turn
    val faceAlpha = f.faceAlpha
    val eyeShift = f.eyeShift
    val eyeScale = f.eyeScale
    val squash = f.squash
    val funnelStep = f.funnelStep
    val funnelAlpha = f.funnelAlpha

    withTransform({
        // Like a launcher mask: only the central 72 of the 108 units show, so the drawing fills the tile.
        scale(size.width / VISIBLE, size.width / VISIBLE, Offset.Zero)
        translate(-(108f - VISIBLE) / 2, -(108f - VISIBLE) / 2)
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
