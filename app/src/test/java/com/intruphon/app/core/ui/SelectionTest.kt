package com.intruphon.app.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionTest {
    @Test fun startsEmptyAndInactive() {
        val s = Selection()
        assertFalse(s.isActive)
        assertEquals(0, s.count)
    }

    @Test fun toggleAddsThenRemoves() {
        val s = Selection()
        s.toggle(5)
        assertTrue(s.isSelected(5))
        assertTrue(s.isActive)
        s.toggle(5)
        assertFalse(s.isSelected(5))
        assertFalse(s.isActive)
    }

    @Test fun selectAllAddsToWhatIsPicked() {
        val s = Selection(setOf(1L))
        s.selectAll(listOf(2, 3))
        assertEquals(setOf(1L, 2L, 3L), s.selected)
        assertTrue(s.allSelected(listOf(1, 2, 3)))
        assertFalse(s.allSelected(listOf(1, 2, 3, 4)))
    }

    @Test fun allSelectedIsFalseForAnEmptyList() {
        assertFalse(Selection(setOf(1L)).allSelected(emptyList()))
    }

    @Test fun retainDropsWhatLeftTheList() {
        val s = Selection(setOf(1L, 2L, 3L))
        s.retain(setOf(2L, 3L, 9L))
        assertEquals(setOf(2L, 3L), s.selected)
        s.retain(setOf(2L, 3L))
        assertEquals(setOf(2L, 3L), s.selected)
    }

    @Test fun clearEmpties() {
        val s = Selection(setOf(1L, 2L))
        s.clear()
        assertFalse(s.isActive)
    }
}
