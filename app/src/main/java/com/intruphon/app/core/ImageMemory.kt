package com.intruphon.app.core

import android.graphics.Bitmap

/**
 * Sizes for the picture caches. They are measured in kilobytes of picture, not in number of entries, so a few
 * big pictures cannot crowd out many small ones, and the total follows the memory Android gives the app.
 */
object ImageMemory {
    /** A cache budget in KB: one [divisor]th of the app's memory limit. */
    fun budgetKb(divisor: Int, maxMemoryBytes: Long = Runtime.getRuntime().maxMemory()): Int =
        (maxMemoryBytes / 1024 / divisor).toInt().coerceAtLeast(1)

    /** Size of a picture in KB (at least 1), for the cache's `sizeOf`. */
    fun kb(bitmap: Bitmap?): Int = if (bitmap == null) 1 else (bitmap.allocationByteCount / 1024).coerceAtLeast(1)
}
