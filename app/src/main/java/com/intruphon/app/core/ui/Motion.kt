package com.intruphon.app.core.ui

import androidx.compose.ui.MotionDurationScale

object AppMotion {
    /** Duration of a page slide, in milliseconds. */
    const val SHORT = 280

    /** Ignores the system animation scale: the app's own durations always apply (use with withContext). */
    val FixedScale = object : MotionDurationScale {
        override val scaleFactor: Float = 1f
    }
}
