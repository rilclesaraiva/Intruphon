package com.intruphon.app.features.about

import kotlin.math.abs
import kotlin.math.pow

/** Length of the About icon animation, in seconds. */
const val ICON_END = 2.3f

/** Everything the animated icon needs to draw one moment of it, in "art" units. Pure, so it can be tested. */
data class IconFrame(
    val faceScale: Float,
    val faceX: Float,
    val faceY: Float,
    val turn: Float,
    val faceAlpha: Float,
    val eyeShift: Float,
    val eyeScale: Float,
    val squash: Float,
    val funnelStep: Float,
    val funnelAlpha: Float,
)

/** The frame at [t] seconds. At [ICON_END] it is exactly the launcher icon (face settled, funnel in place). */
fun iconFrame(t: Float): IconFrame {
    val u = clamp01((t - 1.65f) / 0.6f)          // the face settles into the icon
    val ease = back(u)
    val settle = smooth(u)
    val faceScale = when {
        u > 0f -> lerp(1.15f, 1f, ease)
        t < 0.28f -> lerp(0.45f, 1.15f, back(clamp01(t / 0.28f)))
        else -> 1.15f
    }
    val eyeShift = keys(t, look(8f))
    return IconFrame(
        faceScale = faceScale,
        faceX = lerp(50f, 42f, settle) + keys(t, look(6f)) * (1 - settle),
        faceY = lerp(42f, 30f, settle),
        turn = keys(t, look(13f)) * (1 - settle),
        faceAlpha = keys(t, listOf(0f to 0f, 0.18f to 1f)),
        eyeShift = eyeShift,
        eyeScale = keys(t, EyeWide),
        squash = 1 - 0.12f * abs(eyeShift) / 8f,
        funnelStep = clamp01((t - 1.7f) / 0.55f),
        funnelAlpha = keys(t, listOf(1.7f to 0f, 1.85f to 1f)),
    )
}

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
internal fun smooth(x: Float) = x * x * (3 - 2 * x)
internal fun clamp01(x: Float) = x.coerceIn(0f, 1f)
internal fun back(x: Float): Float { val c1 = 1.2f; val c3 = c1 + 1; return 1 + c3 * (x - 1).pow(3) + c1 * (x - 1).pow(2) }

/** Smooth interpolation through (time, value) points. */
internal fun keys(t: Float, k: List<Pair<Float, Float>>): Float {
    if (t <= k.first().first) return k.first().second
    for (i in 0 until k.size - 1) {
        if (t < k[i + 1].first) return lerp(k[i].second, k[i + 1].second, smooth((t - k[i].first) / (k[i + 1].first - k[i].first)))
    }
    return k.last().second
}

/** The looks: centre, left, right, centre again. */
internal fun look(amount: Float) = listOf(0f to 0f, 0.3f to 0f, 0.55f to -amount, 0.72f to -amount, 1.0f to amount, 1.15f to amount, 1.4f to 0f)
internal val EyeWide = listOf(0f to 1f, 1.35f to 1f, 1.45f to 1.3f, 1.75f to 1.3f, 2.0f to 1f)

