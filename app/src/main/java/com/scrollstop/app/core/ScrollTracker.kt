package com.scrollstop.app.core

import android.content.Context
import com.scrollstop.app.data.AnalyticsGraph
import com.scrollstop.app.data.NudgeGraph
import com.scrollstop.app.data.PauseGraph
import com.scrollstop.app.data.QuietHoursGraph
import com.scrollstop.app.data.ReductionPlanGraph
import com.scrollstop.app.data.ReminderCooldownGraph
import com.scrollstop.app.premium.PremiumGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class ScrollUiState(
    val currentApp: String = "None",
    val scrollCount: Int = 0,
    val sessionDurationMs: Long = 0L,
    val status: String = "Idle",
    val awarenessLevel: AwarenessLevel = AwarenessLevel.NORMAL
)

object ScrollTracker {
    /**
     * A single user gesture emits many TYPE_VIEW_SCROLLED events (the system throttles them
     * to ~100ms apart). Scroll events arriving within this window are treated as one gesture,
     * so "one scroll" counts once instead of once per accessibility event.
     */
    private const val SCROLL_GESTURE_WINDOW_MS = 300L

    private var sessionApp: String? = null
    private var sessionStartTime: Long = 0L
    private var scrollCount: Int = 0
    private var lastEmittedAwarenessLevel: AwarenessLevel = AwarenessLevel.NORMAL
    private var lastReminderBody: String? = null
    private var lastReminderTime: Long = 0L
    private var lastScrollEventTime: Long = 0L

    private val _uiState = MutableStateFlow(ScrollUiState())
    val uiState: StateFlow<ScrollUiState> = _uiState.asStateFlow()

    @Synchronized
    fun onScrollEvent(context: Context, packageName: String, timestamp: Long) {
        if (PauseGraph.repository(context).isPaused()) return

        if (sessionApp == null || sessionApp != packageName) {
            startNewSession(packageName, timestamp)
        }

        // Coalesce the event burst of one gesture into a single count.
        if (lastScrollEventTime > 0 && timestamp - lastScrollEventTime < SCROLL_GESTURE_WINDOW_MS) {
            lastScrollEventTime = timestamp
            return
        }
        lastScrollEventTime = timestamp

        scrollCount += 1
        val nowWall = System.currentTimeMillis()
        val analytics = AnalyticsGraph.scrolls(context)
        analytics.recordScroll(packageName, nowWall)
        val sessionDuration = timestamp - sessionStartTime
        val awarenessState = DoomscrollAwareness.evaluate(sessionDuration, scrollCount)
        updateUiState(packageName, sessionDuration, "Active", awarenessState.level)

        val premiumState = PremiumGraph.manager(context).state.value
        val today = LocalDate.now()
        val inQuietHours = if (premiumState.isPremium) {
            QuietHoursGraph.repository(context).isInWindow(packageName)
        } else {
            QuietHoursGraph.repository(context).isInWindow()
        }

        if (premiumState.isPremium) {
            val perAppCount = analytics.appEvents.value.firstOrNull { it.date == today && it.packageName == packageName }?.scrolls ?: 0
            val planLimit = ReductionPlanGraph.repository(context)
                .currentReminderLimit(premiumState.reminderSettings.reminderLimit)
            val effectiveLimit = premiumState.reminderSettings.perAppLimits[packageName] ?: planLimit
            val shouldTrigger = perAppCount > 0 && perAppCount % effectiveLimit == 0 && isCooldownElapsed(context)
            if (shouldTrigger && !inQuietHours) {
                val effectiveTone = premiumState.reminderSettings.perAppTones[packageName]
                    ?: premiumState.reminderTone
                val level = DoomscrollAwareness.dailyLevel(perAppCount)
                val copy = ReminderMessaging.pick(effectiveTone, level, perAppCount, lastReminderBody)
                lastReminderBody = copy.message
                lastReminderTime = System.currentTimeMillis()
                NudgeGraph.repository(context).setLastMessage(copy.message)
                InterventionManager.trigger(context, packageName, level, copy.title, copy.summary, copy.message)
                analytics.recordInterruption(packageName, nowWall)
                lastEmittedAwarenessLevel = level
            }
        } else {
            val dailyCount = analytics.events.value.firstOrNull { it.date == today }?.scrolls ?: 0
            val reminderLimit = ReductionPlanGraph.repository(context)
                .currentReminderLimit(premiumState.reminderSettings.reminderLimit)
            val shouldTrigger = dailyCount > 0 && dailyCount % reminderLimit == 0 && isCooldownElapsed(context)
            if (shouldTrigger && !inQuietHours) {
                val level = DoomscrollAwareness.dailyLevel(dailyCount)
                val copy = ReminderMessaging.pick(premiumState.reminderTone, level, dailyCount, lastReminderBody)
                lastReminderBody = copy.message
                lastReminderTime = System.currentTimeMillis()
                NudgeGraph.repository(context).setLastMessage(copy.message)
                InterventionManager.trigger(context, packageName, level, copy.title, copy.summary, copy.message)
                analytics.recordInterruption(packageName, nowWall)
                lastEmittedAwarenessLevel = level
            }
        }
    }

    @Synchronized
    fun onAppChanged(packageName: String, timestamp: Long) {
        if (sessionApp != packageName) {
            startNewSession(packageName, timestamp)
            updateUiState(packageName, 0L, "Active", AwarenessLevel.NORMAL)
        }
    }

    @Synchronized
    fun onIdleCheck(currentTime: Long) {
        if (sessionApp == null) {
            updateUiState("None", 0L, "Idle", AwarenessLevel.NORMAL)
            return
        }
        val duration = currentTime - sessionStartTime
        val awarenessState = DoomscrollAwareness.evaluate(duration, scrollCount)
        updateUiState(sessionApp ?: "None", duration, "Active", awarenessState.level)
    }

    @Synchronized
    fun resetToIdle() {
        sessionApp = null
        sessionStartTime = 0L
        scrollCount = 0
        lastEmittedAwarenessLevel = AwarenessLevel.NORMAL
        lastScrollEventTime = 0L
        updateUiState("None", 0L, "Idle", AwarenessLevel.NORMAL)
    }

    /** True when enough time has passed since the last reminder to send another. */
    private fun isCooldownElapsed(context: Context): Boolean {
        val cooldownMinutes = ReminderCooldownGraph.repository(context).cooldownMinutes.value
        if (cooldownMinutes <= 0) return true
        val cooldownMs = cooldownMinutes * 60_000L
        return System.currentTimeMillis() - lastReminderTime >= cooldownMs
    }

    private fun startNewSession(packageName: String, timestamp: Long) {
        sessionApp = packageName
        sessionStartTime = timestamp
        scrollCount = 0
        lastEmittedAwarenessLevel = AwarenessLevel.NORMAL
        lastScrollEventTime = 0L
    }

    private fun updateUiState(app: String, durationMs: Long, status: String, awarenessLevel: AwarenessLevel) {
        _uiState.value = ScrollUiState(
            currentApp = app,
            scrollCount = scrollCount,
            sessionDurationMs = durationMs,
            status = status,
            awarenessLevel = awarenessLevel
        )
    }
}
