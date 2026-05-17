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
    val awarenessLevel: AwarenessLevel = AwarenessLevel.NORMAL
)

object ScrollTracker {
    private var sessionApp: String? = null
    private var sessionStartTime: Long = 0L
    private var scrollCount: Int = 0
    private var lastEmittedAwarenessLevel: AwarenessLevel = AwarenessLevel.NORMAL

    private val _uiState = MutableStateFlow(ScrollUiState())
    val uiState: StateFlow<ScrollUiState> = _uiState.asStateFlow()

    @Synchronized
    fun onScrollEvent(context: Context, packageName: String, timestamp: Long) {
        if (sessionApp == null || sessionApp != packageName) {
            startNewSession(packageName, timestamp)
        }

        scrollCount += 1
        val sessionDuration = timestamp - sessionStartTime
        val awarenessState = DoomscrollAwareness.evaluate(sessionDuration, scrollCount)
        updateUiState(packageName, sessionDuration, "Active", awarenessState.level)

        val shouldTrigger = awarenessState.message != null &&
            awarenessState.level.ordinal > lastEmittedAwarenessLevel.ordinal

        if (shouldTrigger) {
            InterventionManager.trigger(context, packageName, awarenessState)
            lastEmittedAwarenessLevel = awarenessState.level
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
        updateUiState("None", 0L, "Idle", AwarenessLevel.NORMAL)
    }

    private fun startNewSession(packageName: String, timestamp: Long) {
        sessionApp = packageName
        sessionStartTime = timestamp
        scrollCount = 0
        lastEmittedAwarenessLevel = AwarenessLevel.NORMAL
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
