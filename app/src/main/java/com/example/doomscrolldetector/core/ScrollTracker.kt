package com.example.doomscrolldetector.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScrollUiState(
    val currentApp: String = "None",
    val scrollCount: Int = 0,
    val sessionDurationMs: Long = 0L,
    val status: String = "Idle",
    val awarenessLevel: AwarenessLevel = AwarenessLevel.NORMAL,
    val intensity: ScrollIntensity = ScrollIntensity(),
    val dailyStats: DailyStats = DailyStats()
)

object ScrollTracker {
    private const val IDLE_TIMEOUT_MS = 60_000L

    private var sessionApp: String? = null
    private var sessionStartTime: Long = 0L
    private var lastActivityTime: Long = 0L
    private var scrollCount: Int = 0
    private var hasRecordedCurrentSession: Boolean = false
    private var lastEmittedAwarenessLevel: AwarenessLevel = AwarenessLevel.NORMAL

    private val _uiState = MutableStateFlow(ScrollUiState())
    val uiState: StateFlow<ScrollUiState> = _uiState.asStateFlow()

    @Synchronized
    fun loadDailyStats(context: Context, timestamp: Long = System.currentTimeMillis()) {
        val dailyStats = DailyStatsStore.read(context, timestamp)
        val current = _uiState.value
        _uiState.value = current.copy(dailyStats = dailyStats)
    }

    @Synchronized
    fun onScrollEvent(context: Context, packageName: String, timestamp: Long) {
        if (sessionApp == null || sessionApp != packageName || isSessionIdle(timestamp)) {
            startNewSession(packageName, timestamp)
        }

        if (!hasRecordedCurrentSession) {
            sessionStartTime = timestamp
            hasRecordedCurrentSession = true
            DailyStatsStore.recordSessionStart(context, timestamp)
        }

        lastActivityTime = timestamp
        scrollCount += 1
        val sessionDuration = timestamp - sessionStartTime
        val awarenessState = DoomscrollAwareness.evaluate(sessionDuration, scrollCount)
        val intensity = ScrollIntensityAnalyzer.evaluate(sessionDuration, scrollCount)
        val dailyStats = DailyStatsStore.recordScroll(
            context = context,
            timestamp = timestamp,
            sessionDurationMs = sessionDuration,
            awarenessLevel = awarenessState.level
        )
        updateUiState(packageName, sessionDuration, "Active", awarenessState.level, intensity, dailyStats)

        val shouldTrigger = awarenessState.message != null &&
            awarenessState.level.ordinal > lastEmittedAwarenessLevel.ordinal

        if (shouldTrigger) {
            InterventionManager.trigger(context, packageName, awarenessState)
            lastEmittedAwarenessLevel = awarenessState.level
        }
    }

    @Synchronized
    fun onAppChanged(packageName: String, timestamp: Long) {
        if (sessionApp != packageName || isSessionIdle(timestamp)) {
            startNewSession(packageName, timestamp)
            updateUiState(packageName, 0L, "Ready", AwarenessLevel.NORMAL, ScrollIntensity(), _uiState.value.dailyStats)
        }
    }

    @Synchronized
    fun onIdleCheck(currentTime: Long) {
        if (sessionApp == null || isSessionIdle(currentTime)) {
            resetToIdle()
            return
        }
        val duration = if (hasRecordedCurrentSession) currentTime - sessionStartTime else 0L
        val awarenessState = DoomscrollAwareness.evaluate(duration, scrollCount)
        val status = if (hasRecordedCurrentSession) "Active" else "Ready"
        updateUiState(
            sessionApp ?: "None",
            duration,
            status,
            awarenessState.level,
            ScrollIntensityAnalyzer.evaluate(duration, scrollCount),
            _uiState.value.dailyStats
        )
    }

    @Synchronized
    fun resetToIdle() {
        sessionApp = null
        sessionStartTime = 0L
        lastActivityTime = 0L
        scrollCount = 0
        hasRecordedCurrentSession = false
        lastEmittedAwarenessLevel = AwarenessLevel.NORMAL
        updateUiState("None", 0L, "Idle", AwarenessLevel.NORMAL, ScrollIntensity(), _uiState.value.dailyStats)
    }

    private fun startNewSession(packageName: String, timestamp: Long) {
        sessionApp = packageName
        sessionStartTime = timestamp
        lastActivityTime = timestamp
        scrollCount = 0
        hasRecordedCurrentSession = false
        lastEmittedAwarenessLevel = AwarenessLevel.NORMAL
    }

    private fun isSessionIdle(timestamp: Long): Boolean {
        return lastActivityTime > 0L && timestamp - lastActivityTime >= IDLE_TIMEOUT_MS
    }

    private fun updateUiState(
        app: String,
        durationMs: Long,
        status: String,
        awarenessLevel: AwarenessLevel,
        intensity: ScrollIntensity,
        dailyStats: DailyStats
    ) {
        _uiState.value = ScrollUiState(
            currentApp = app,
            scrollCount = scrollCount,
            sessionDurationMs = durationMs,
            status = status,
            awarenessLevel = awarenessLevel,
            intensity = intensity,
            dailyStats = dailyStats
        )
    }
}
