package com.example.doomscrolldetector.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScrollUiState(
    val currentApp: String = "None",
    val scrollCount: Int = 0,
    val sessionDurationMs: Long = 0L,
    val status: String = "Idle"
)

object ScrollTracker {
    private const val DOOMSCROLL_MINUTES_THRESHOLD = 15L
    private const val DOOMSCROLL_SCROLL_COUNT_THRESHOLD = 100

    private var sessionApp: String? = null
    private var sessionStartTime: Long = 0L
    private var scrollCount: Int = 0

    private val _uiState = MutableStateFlow(ScrollUiState())
    val uiState: StateFlow<ScrollUiState> = _uiState.asStateFlow()

    @Synchronized
    fun onScrollEvent(context: Context, packageName: String, timestamp: Long) {
        if (sessionApp == null || sessionApp != packageName) {
            startNewSession(packageName, timestamp)
        }

        scrollCount += 1
        val sessionDuration = timestamp - sessionStartTime
        updateUiState(packageName, sessionDuration, "Active")

        val durationMinutes = sessionDuration / 60000
        val doomscrollDetected = durationMinutes > DOOMSCROLL_MINUTES_THRESHOLD &&
            scrollCount > DOOMSCROLL_SCROLL_COUNT_THRESHOLD

        if (doomscrollDetected) {
            InterventionManager.trigger(context, packageName, durationMinutes, scrollCount)
            resetToIdle()
        }
    }

    @Synchronized
    fun onAppChanged(packageName: String, timestamp: Long) {
        if (sessionApp != packageName) {
            startNewSession(packageName, timestamp)
            updateUiState(packageName, 0L, "Active")
        }
    }

    @Synchronized
    fun onIdleCheck(currentTime: Long) {
        if (sessionApp == null) {
            updateUiState("None", 0L, "Idle")
            return
        }
        val duration = currentTime - sessionStartTime
        updateUiState(sessionApp ?: "None", duration, "Active")
    }

    @Synchronized
    fun resetToIdle() {
        sessionApp = null
        sessionStartTime = 0L
        scrollCount = 0
        updateUiState("None", 0L, "Idle")
    }

    private fun startNewSession(packageName: String, timestamp: Long) {
        sessionApp = packageName
        sessionStartTime = timestamp
        scrollCount = 0
    }

    private fun updateUiState(app: String, durationMs: Long, status: String) {
        _uiState.value = ScrollUiState(
            currentApp = app,
            scrollCount = scrollCount,
            sessionDurationMs = durationMs,
            status = status
        )
    }
}
