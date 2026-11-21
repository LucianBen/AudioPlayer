package com.kid.audioplayer.player

import com.kid.audioplayer.data.BookPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackPositionsTest {
    @Test
    fun pictureChangesAtStartAndKeepsLastImageAtEnd() {
        val pages = listOf(BookPage("1.jpg", 0, 34_940), BookPage("2.jpg", 34_940, 86_540))
        assertEquals(0, pictureIndex(pages, 34_939, false))
        assertEquals(1, pictureIndex(pages, 34_940, false))
        assertEquals(0, pictureIndex(pages, 10_000, false))
        assertEquals(1, pictureIndex(pages, 34_000, true))
    }

    @Test
    fun sentenceJumpUsesAdjacentStartsAndStopsAtEnds() {
        val starts = listOf(0L, 1_000L, 2_000L)
        assertEquals(0L, sentenceTarget(starts, 1_400L, -1))
        assertEquals(2_000L, sentenceTarget(starts, 1_000L, 1))
        assertNull(sentenceTarget(starts, 500L, -1))
        assertNull(sentenceTarget(starts, 2_500L, 1))
    }

    @Test
    fun sentenceDetectionChecksBothDurationsAndAppliesPreroll() {
        val voice = -15.0
        val quiet = -65.0
        val frames = List(10) { voice } + List(19) { quiet } + List(5) { voice } +
            List(20) { quiet } + List(4) { voice } + List(20) { quiet } + List(5) { voice }
        assertEquals(listOf(0L, 1_480L), sentenceStarts(frames))
        assertEquals(listOf(0L), sentenceStarts(List(50) { quiet }))
        assertEquals(emptyList<Long>(), sentenceStarts(emptyList()))
    }
}
