package com.intruphon.app.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    private val r = ReleaseInfo("0.3.0", "notes", "page", null)
    private val none = UpdateChoices()

    @Test fun semverCompares() {
        assertTrue(isNewer("v0.10.0", "0.9.9"))
        assertTrue(isNewer("1.0.0", "0.99.99"))
        assertFalse(isNewer("0.2.0", "0.2.0"))
        assertFalse(isNewer("0.1.9", "0.2.0"))
        assertFalse(isNewer("junk", "0.2.0"))
        assertFalse(isNewer("1.0.0-beta", "1.0.0"))
    }

    @Test fun promptsOnlyForNewerVersion() {
        assertTrue(shouldPrompt(r, "0.2.0", none, 1000, false))
        assertFalse(shouldPrompt(r, "0.3.0", none, 1000, false))
        assertFalse(shouldPrompt(null, "0.2.0", none, 1000, false))
    }

    @Test fun answersSilenceThePrompt() {
        assertFalse(shouldPrompt(r, "0.2.0", none, 1000, true))
        assertFalse(shouldPrompt(r, "0.2.0", UpdateChoices(snoozeUntilMs = 5000), 1000, false))
        assertTrue(shouldPrompt(r, "0.2.0", UpdateChoices(snoozeUntilMs = 5000), 6000, false))
        assertFalse(shouldPrompt(r, "0.2.0", UpdateChoices(skippedVersion = "0.3.0"), 1000, false))
        assertTrue(shouldPrompt(r.copy(version = "0.4.0"), "0.2.0", UpdateChoices(skippedVersion = "0.3.0"), 1000, false))
    }

    @Test fun checkInterval() {
        assertTrue(dueForCheck(0, 1000, false))
        assertFalse(dueForCheck(1000, 1000 + CHECK_INTERVAL_MS - 1, false))
        assertTrue(dueForCheck(1000, 1000 + CHECK_INTERVAL_MS, false))
        assertTrue(dueForCheck(1000, 1001, true))
        assertEquals(7 * 24 * 3600 * 1000L, WEEK_MS)
    }
}
