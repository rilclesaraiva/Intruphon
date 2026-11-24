package com.intruphon.app.core

import java.util.Locale

/** A length in seconds as a clock: 1:15, or 1:02:05 from one hour up. */
fun formatClock(seconds: Long): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}
