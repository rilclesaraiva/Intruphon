package com.intruphon.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test fun underAnHour() = assertEquals("1:15", formatClock(75))
    @Test fun fromAnHour() = assertEquals("1:02:05", formatClock(3725))
    @Test fun zero() = assertEquals("0:00", formatClock(0))
}
