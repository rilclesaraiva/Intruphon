package com.intruphon.app.features.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {
    @Test fun parsesEachKindOfLine() {
        val lines = parseNotes("> Big news\n\n### Added\n- One\n* Two\nSome text")
        assertEquals(
            listOf(
                NoteLine.Highlight("Big news"),
                NoteLine.Heading("Added"),
                NoteLine.Bullet("One"),
                NoteLine.Bullet("Two"),
                NoteLine.Plain("Some text"),
            ),
            lines,
        )
    }
}
