package com.example.doomscrolldetector.core

private const val MILLIS_PER_MINUTE = 60_000.0
private const val MIN_DURATION_FOR_RATE_MS = 5_000L
private const val NOTICE_SCROLLS_PER_MINUTE = 35.0
private const val WARNING_SCROLLS_PER_MINUTE = 60.0
private const val CRITICAL_SCROLLS_PER_MINUTE = 90.0

data class ScrollIntensity(
    val scrollsPerMinute: Double = 0.0,
    val level: AwarenessLevel = AwarenessLevel.NORMAL
)

object ScrollIntensityAnalyzer {
    fun evaluate(sessionDurationMs: Long, scrollCount: Int): ScrollIntensity {
        if (sessionDurationMs < MIN_DURATION_FOR_RATE_MS || scrollCount <= 0) {
            return ScrollIntensity()
        }

        val scrollsPerMinute = scrollCount / (sessionDurationMs / MILLIS_PER_MINUTE)
        val level = when {
            scrollsPerMinute >= CRITICAL_SCROLLS_PER_MINUTE -> AwarenessLevel.CRITICAL
            scrollsPerMinute >= WARNING_SCROLLS_PER_MINUTE -> AwarenessLevel.WARNING
            scrollsPerMinute >= NOTICE_SCROLLS_PER_MINUTE -> AwarenessLevel.NOTICE
            else -> AwarenessLevel.NORMAL
        }

        return ScrollIntensity(scrollsPerMinute = scrollsPerMinute, level = level)
    }
}
