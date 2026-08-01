package com.example.doomscrolldetector.core

enum class AwarenessLevel {
    NORMAL,
    NOTICE,
    WARNING,
    CRITICAL
}

data class AwarenessState(
    val level: AwarenessLevel,
    val message: String? = null
)

object DoomscrollAwareness {
    private const val TWO_MINUTES_MS = 2 * 60 * 1000L
    private const val FIVE_MINUTES_MS = 5 * 60 * 1000L
    private const val TEN_MINUTES_MS = 10 * 60 * 1000L

    private const val SCROLL_NOTICE_THRESHOLD = 100
    private const val SCROLL_WARNING_THRESHOLD = 300
    private const val SCROLL_CRITICAL_THRESHOLD = 500

    fun evaluate(sessionDurationMs: Long, scrollCount: Int): AwarenessState {
        val states = listOf(
            evaluateTime(sessionDurationMs),
            evaluateScrollCount(scrollCount),
            evaluateIntensity(sessionDurationMs, scrollCount)
        )
        return states.maxBy { it.level.ordinal }
    }

    private fun evaluateTime(sessionDurationMs: Long): AwarenessState {
        return when {
            sessionDurationMs >= TEN_MINUTES_MS -> AwarenessState(
                level = AwarenessLevel.CRITICAL,
                message = "You've been scrolling for 10 minutes. You may want to take a break."
            )

            sessionDurationMs >= FIVE_MINUTES_MS -> AwarenessState(
                level = AwarenessLevel.WARNING,
                message = "You've been scrolling for 5 minutes straight."
            )

            sessionDurationMs >= TWO_MINUTES_MS -> AwarenessState(
                level = AwarenessLevel.NOTICE,
                message = "You've been scrolling for a couple of minutes."
            )

            else -> AwarenessState(AwarenessLevel.NORMAL)
        }
    }

    private fun evaluateIntensity(sessionDurationMs: Long, scrollCount: Int): AwarenessState {
        val intensity = ScrollIntensityAnalyzer.evaluate(sessionDurationMs, scrollCount)
        return when (intensity.level) {
            AwarenessLevel.CRITICAL -> AwarenessState(
                level = AwarenessLevel.CRITICAL,
                message = "Your scrolling pace is very intense. Pause and take one deep breath."
            )

            AwarenessLevel.WARNING -> AwarenessState(
                level = AwarenessLevel.WARNING,
                message = "Your scrolling pace is picking up. Consider slowing down."
            )

            AwarenessLevel.NOTICE -> AwarenessState(
                level = AwarenessLevel.NOTICE,
                message = "You're scrolling faster than usual. Notice how this feels."
            )

            AwarenessLevel.NORMAL -> AwarenessState(AwarenessLevel.NORMAL)
        }
    }

    private fun evaluateScrollCount(scrollCount: Int): AwarenessState {
        return when {
            scrollCount >= SCROLL_CRITICAL_THRESHOLD -> AwarenessState(
                level = AwarenessLevel.CRITICAL,
                message = "This is a high amount of scrolling for one session."
            )

            scrollCount >= SCROLL_WARNING_THRESHOLD -> AwarenessState(
                level = AwarenessLevel.WARNING,
                message = "You've scrolled quite a lot in this session."
            )

            scrollCount >= SCROLL_NOTICE_THRESHOLD -> AwarenessState(
                level = AwarenessLevel.NOTICE,
                message = "You've scrolled quite a lot in this session."
            )

            else -> AwarenessState(AwarenessLevel.NORMAL)
        }
    }
}
