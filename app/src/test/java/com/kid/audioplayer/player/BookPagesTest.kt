package com.kid.audioplayer.player

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookPagesTest {
    @Test
    fun requiresHorizontalDisplacementGreaterThanThirtyFivePercentOfTheWindow() {
        assertNull(swipePageTarget(Offset(-349f, 0f), 1000, 1, 2))
        assertNull(swipePageTarget(Offset(-350f, 0f), 1000, 1, 2))
        assertNull(swipePageTarget(Offset(-351f, 400f), 1000, 1, 2))
        assertNull(swipePageTarget(Offset(-351f, 0f), 0, 1, 2))
        assertEquals(2, swipePageTarget(Offset(-351f, 0f), 1000, 1, 2))
        assertEquals(0, swipePageTarget(Offset(351f, 0f), 1000, 1, 2))
    }

    @Test
    fun doesNotPagePastEitherEnd() {
        assertNull(swipePageTarget(Offset(501f, 0f), 1000, 0, 2))
        assertNull(swipePageTarget(Offset(-501f, 0f), 1000, 2, 2))
    }
}
