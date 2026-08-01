package com.example.doomscrolldetector.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollIntensityAnalyzerTest {
    @Test
    fun evaluateIgnoresVeryShortSessionsToAvoidFalsePositives() {
        val intensity = ScrollIntensityAnalyzer.evaluate(sessionDurationMs = 4_000L, scrollCount = 20)

        assertEquals(AwarenessLevel.NORMAL, intensity.level)
        assertEquals(0.0, intensity.scrollsPerMinute, 0.0)
    }

    @Test
    fun evaluateClassifiesHighVelocityScrolling() {
        val intensity = ScrollIntensityAnalyzer.evaluate(sessionDurationMs = 60_000L, scrollCount = 65)

        assertEquals(AwarenessLevel.WARNING, intensity.level)
        assertTrue(intensity.scrollsPerMinute >= 60.0)
    }

    @Test
    fun doomscrollAwarenessUsesVelocityWhenItIsHighestRiskSignal() {
        val awareness = DoomscrollAwareness.evaluate(sessionDurationMs = 60_000L, scrollCount = 95)

        assertEquals(AwarenessLevel.CRITICAL, awareness.level)
        assertEquals(
            "Your scrolling pace is very intense. Pause and take one deep breath.",
            awareness.message
        )
    }
}
