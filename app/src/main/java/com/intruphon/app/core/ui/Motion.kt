package com.intruphon.app.core.ui

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * The app's motion in one place. Everything that moves uses these named specs instead of a number written on the spot.
 *
 * The springs carry the values of Material 3 Expressive's motion scheme (spatial: things that move or change size,
 * with a little bounce; effects: color and transparency, no bounce). They are written out here because the
 * pinned Material 3 has no `MotionScheme` yet; when it does, only this file changes.
 *
 * The one documented exception is the page slide ([SHORT]): it is a fixed-time tween so that letting go of the back
 * gesture, the Back button and the system back all end in exactly the same time. Like every animation here it follows
 * the system animation scale (and jumps to the end when the person turned animations off).
 */
object AppMotion {
    /** Duration of a page slide, in milliseconds. */
    const val SHORT = 280

    /** Things that move or change size (panels growing, content arriving). */
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Quick, short moves (settling back after a drag). */
    fun <T> fastSpatial(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Color and transparency: appearing, disappearing, switching state. Never bounces. */
    fun <T> effects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}
