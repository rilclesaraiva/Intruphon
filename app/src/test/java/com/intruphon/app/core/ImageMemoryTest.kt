package com.intruphon.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageMemoryTest {
    @Test fun budgetIsAShareOfTheMemoryInKb() {
        assertEquals(16 * 1024, ImageMemory.budgetKb(16, maxMemoryBytes = 256L * 1024 * 1024))
        assertEquals(32 * 1024, ImageMemory.budgetKb(8, maxMemoryBytes = 256L * 1024 * 1024))
    }

    @Test fun budgetNeverReachesZero() {
        assertTrue(ImageMemory.budgetKb(16, maxMemoryBytes = 100L) >= 1)
    }

    @Test fun aMissingPictureCountsAsOneKb() {
        assertEquals(1, ImageMemory.kb(null))
    }
}
