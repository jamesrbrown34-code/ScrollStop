package com.example.doomscrolldetector.core

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val PREFS_NAME = "daily_scroll_stats"
private const val KEY_DATE = "date"
private const val KEY_SCROLLS = "scrolls"
private const val KEY_SESSIONS = "sessions"
private const val KEY_LONGEST_SESSION_MS = "longest_session_ms"
private const val KEY_HIGHEST_AWARENESS_LEVEL = "highest_awareness_level"

data class DailyStats(
    val date: String = LocalDate.now().toString(),
    val scrolls: Int = 0,
    val sessions: Int = 0,
    val longestSessionMs: Long = 0L,
    val highestAwarenessLevel: AwarenessLevel = AwarenessLevel.NORMAL
)

object DailyStatsStore {
    fun read(context: Context, timestamp: Long = System.currentTimeMillis()): DailyStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = dateKey(timestamp)
        val storedDate = prefs.getString(KEY_DATE, null)

        if (storedDate != today) {
            val freshStats = DailyStats(date = today)
            prefs.edit()
                .putString(KEY_DATE, freshStats.date)
                .putInt(KEY_SCROLLS, freshStats.scrolls)
                .putInt(KEY_SESSIONS, freshStats.sessions)
                .putLong(KEY_LONGEST_SESSION_MS, freshStats.longestSessionMs)
                .putString(KEY_HIGHEST_AWARENESS_LEVEL, freshStats.highestAwarenessLevel.name)
                .apply()
            return freshStats
        }

        return DailyStats(
            date = today,
            scrolls = prefs.getInt(KEY_SCROLLS, 0),
            sessions = prefs.getInt(KEY_SESSIONS, 0),
            longestSessionMs = prefs.getLong(KEY_LONGEST_SESSION_MS, 0L),
            highestAwarenessLevel = runCatching {
                AwarenessLevel.valueOf(
                    prefs.getString(KEY_HIGHEST_AWARENESS_LEVEL, AwarenessLevel.NORMAL.name)
                        ?: AwarenessLevel.NORMAL.name
                )
            }.getOrDefault(AwarenessLevel.NORMAL)
        )
    }

    fun recordSessionStart(context: Context, timestamp: Long): DailyStats {
        val current = read(context, timestamp)
        val updated = current.copy(sessions = current.sessions + 1)
        save(context, updated)
        return updated
    }

    fun recordScroll(
        context: Context,
        timestamp: Long,
        sessionDurationMs: Long,
        awarenessLevel: AwarenessLevel
    ): DailyStats {
        val current = read(context, timestamp)
        val updated = current.copy(
            scrolls = current.scrolls + 1,
            longestSessionMs = maxOf(current.longestSessionMs, sessionDurationMs),
            highestAwarenessLevel = maxAwareness(current.highestAwarenessLevel, awarenessLevel)
        )
        save(context, updated)
        return updated
    }

    private fun save(context: Context, stats: DailyStats) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_DATE, stats.date)
            .putInt(KEY_SCROLLS, stats.scrolls)
            .putInt(KEY_SESSIONS, stats.sessions)
            .putLong(KEY_LONGEST_SESSION_MS, stats.longestSessionMs)
            .putString(KEY_HIGHEST_AWARENESS_LEVEL, stats.highestAwarenessLevel.name)
            .apply()
    }

    private fun maxAwareness(first: AwarenessLevel, second: AwarenessLevel): AwarenessLevel {
        return if (first.ordinal >= second.ordinal) first else second
    }

    private fun dateKey(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }
}
