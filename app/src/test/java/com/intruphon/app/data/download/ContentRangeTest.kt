package com.intruphon.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentRangeTest {
    @Test fun readsTotal() {
        assertEquals(1234L, totalFromContentRange("bytes 0-99/1234"))
    }

    @Test fun unknownTotalOrMissingHeader() {
        assertNull(totalFromContentRange("bytes 0-99/*"))
        assertNull(totalFromContentRange(null))
    }
}
