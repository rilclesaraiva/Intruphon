package com.intruphon.app.features.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconFrameTest {
    private val eps = 0.001f

    @Test fun endsExactlyAsTheLauncherIcon() {
        val f = iconFrame(ICON_END)
        assertEquals(1f, f.faceScale, eps)
        assertEquals(42f, f.faceX, eps)
        assertEquals(30f, f.faceY, eps)
        assertEquals(0f, f.turn, eps)
        assertEquals(1f, f.faceAlpha, eps)
        assertEquals(0f, f.eyeShift, eps)
        assertEquals(1f, f.eyeScale, eps)
        assertEquals(1f, f.squash, eps)
        assertEquals(1f, f.funnelStep, eps)
        assertEquals(1f, f.funnelAlpha, eps)
    }

    @Test fun startsHiddenAndSmall() {
        val f = iconFrame(0f)
        assertEquals(0f, f.faceAlpha, eps)
        assertEquals(0f, f.funnelAlpha, eps)
        assertEquals(0f, f.funnelStep, eps)
        assertEquals(0.45f, f.faceScale, eps)
        assertEquals(50f, f.faceX, eps)
        assertEquals(42f, f.faceY, eps)
    }

    @Test fun staysSettledAfterTheEnd() {
        assertEquals(iconFrame(ICON_END), iconFrame(ICON_END + 5f))
    }

    @Test fun looksLeftThenRight() {
        assertTrue(iconFrame(0.65f).eyeShift < -7f)   // left
        assertTrue(iconFrame(1.1f).eyeShift > 7f)     // right
    }

    @Test fun neverLeavesTheValidRange() {
        var t = 0f
        while (t <= ICON_END + 1f) {
            val f = iconFrame(t)
            assertTrue(f.faceAlpha in 0f..1f)
            assertTrue(f.funnelAlpha in 0f..1f)
            assertTrue(f.funnelStep in 0f..1f)
            assertTrue(f.squash in 0.8f..1f)
            t += 0.01f
        }
    }
}
