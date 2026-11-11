package com.intruphon.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeFileNameTest {
    @Test fun forbiddenCharactersBecomeUnderscores() {
        assertEquals("a_b_c_d", sanitizeFileName("a/b:c*d"))
    }

    @Test fun emptyOrDotsFallBack() {
        assertEquals("media", sanitizeFileName("   "))
        assertEquals("media", sanitizeFileName("..."))
    }

    @Test fun longNamesAreCut() {
        assertEquals(100, sanitizeFileName("x".repeat(300)).length)
    }
}
