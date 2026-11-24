package com.intruphon.app.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TabSwipeTest {
    @Test fun pagesMovedPastAThirdGoToTheNextTab() = assertEquals(1, tabAfterRelease(0, 3, 0.5f, 0f))
    @Test fun pagesMovedBackPastAThirdGoToThePreviousTab() = assertEquals(1, tabAfterRelease(2, 3, -0.5f, 0f))
    @Test fun aShortDragStays() = assertEquals(1, tabAfterRelease(1, 3, 0.2f, 0f))
    @Test fun aFlickGoesEvenWhenShort() {
        assertEquals(2, tabAfterRelease(1, 3, 0.1f, -3f))
        assertEquals(0, tabAfterRelease(1, 3, -0.1f, 3f))
    }
    @Test fun aFlickTheOtherWayDoesNotMoveForward() = assertEquals(1, tabAfterRelease(1, 3, 0.1f, 3f))
    @Test fun neverPastTheEnds() {
        assertEquals(2, tabAfterRelease(2, 3, 0.9f, -5f))
        assertEquals(0, tabAfterRelease(0, 3, -0.9f, 5f))
    }
}
