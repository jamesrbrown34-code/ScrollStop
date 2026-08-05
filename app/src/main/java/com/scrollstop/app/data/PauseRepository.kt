package com.scrollstop.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PauseRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _pausedUntil = MutableStateFlow(prefs.getLong(PAUSED_UNTIL_KEY, 0L))
    val pausedUntil: StateFlow<Long> = _pausedUntil.asStateFlow()

    fun isPaused(now: Long = System.currentTimeMillis()): Boolean = _pausedUntil.value > now

    fun pauseFor(minutes: Int) {
        _pausedUntil.value = System.currentTimeMillis() + minutes * 60_000L
        prefs.edit().putLong(PAUSED_UNTIL_KEY, _pausedUntil.value).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "pause_tracking"
        const val PAUSED_UNTIL_KEY = "paused_until"
    }
}

object PauseGraph {
    @Volatile private var repository: PauseRepository? = null
    fun repository(context: Context): PauseRepository = synchronized(this) {
        repository ?: PauseRepository(context.applicationContext).also { repository = it }
    }
}
