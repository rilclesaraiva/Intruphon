package com.intruphon.app.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TabSwipeTest {
    @Test fun fingerToTheLeftGoesToTheNextTab() = assertEquals(1, tabAfterSwipe(0, 3, -200f, 100f))
    @Test fun fingerToTheRightGoesToThePreviousTab() = assertEquals(1, tabAfterSwipe(2, 3, 200f, 100f))
    @Test fun aShortDragStays() = assertEquals(1, tabAfterSwipe(1, 3, -60f, 100f))
    @Test fun neverPastTheEnds() {
        assertEquals(2, tabAfterSwipe(2, 3, -500f, 100f))
        assertEquals(0, tabAfterSwipe(0, 3, 500f, 100f))
    }
}
