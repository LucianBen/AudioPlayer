package com.kid.audioplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class ClickThrottleTest {
    @Test
    fun firstClickRunsImmediatelyAndIgnoredClicksDoNotExtendWindow() {
        var now = 0L
        val throttle = ClickThrottle { now }
        var clicks = 0

        throttle.run { clicks++ }
        assertEquals(1, clicks)

        now = 100L
        throttle.run { clicks++ }
        now = 299L
        throttle.run { clicks++ }
        assertEquals(1, clicks)

        now = 300L
        throttle.run { clicks++ }
        assertEquals(2, clicks)
    }

    @Test
    fun differentActionsShareTheWindowAndEachAcceptedClickStartsANewWindow() {
        var now = 1_000L
        val throttle = ClickThrottle { now }
        val actions = mutableListOf<String>()

        throttle.run { actions.add("open") }
        now = 1_200L
        throttle.run { actions.add("back") }
        now = 1_301L
        throttle.run { actions.add("play") }
        now = 1_600L
        throttle.run { actions.add("pause") }

        assertEquals(listOf("open", "play"), actions)
    }
}
