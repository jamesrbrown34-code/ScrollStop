package com.scrollstop.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

const val DEFAULT_TODAY_GOAL = 500

data class TodayGoalSettings(
    val default: Int = DEFAULT_TODAY_GOAL,
    val perDay: Map<DayOfWeek, Int> = emptyMap()
) {
    /** The goal that applies on a given date: per-day override or the default. */
    fun effectiveGoal(date: LocalDate): Int = perDay[date.dayOfWeek] ?: default
}

private val Context.todayGoalDataStore by preferencesDataStore("today_goal")

class TodayGoalRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val defaultKey = intPreferencesKey("goal_limit")
    private val perDayKey = stringPreferencesKey("per_day_goals")
    val settings: StateFlow<TodayGoalSettings> = appContext.todayGoalDataStore.data
        .map { prefs ->
            TodayGoalSettings(
                default = (prefs[defaultKey] ?: DEFAULT_TODAY_GOAL).coerceAtLeast(1),
                perDay = parsePerDay(prefs[perDayKey] ?: "")
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, TodayGoalSettings())

    /** The base goal, kept for the onboarding step which only sets the default. */
    val goal: StateFlow<Int> = appContext.todayGoalDataStore.data
        .map { (it[defaultKey] ?: DEFAULT_TODAY_GOAL).coerceAtLeast(1) }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_TODAY_GOAL)

    fun setGoal(limit: Int) {
        scope.launch { appContext.todayGoalDataStore.edit { it[defaultKey] = limit.coerceAtLeast(1) } }
    }

    /** Sets a per-day goal, or clears it (back to the default) when [limit] is null. */
    fun setPerDayGoal(day: DayOfWeek, limit: Int?) {
        scope.launch {
            appContext.todayGoalDataStore.edit { prefs ->
                val current = parsePerDay(prefs[perDayKey] ?: "")
                val updated = if (limit == null) current - day else current + (day to limit.coerceAtLeast(1))
                val json = JSONObject()
                updated.forEach { (d, v) -> json.put(d.name, v) }
                prefs[perDayKey] = json.toString()
            }
        }
    }

    private fun parsePerDay(json: String): Map<DayOfWeek, Int> {
        if (json.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            obj.keys().asSequence().mapNotNull { key ->
                runCatching { DayOfWeek.valueOf(key) }.getOrNull()?.let { it to obj.getInt(key) }
            }.toMap()
        }.getOrDefault(emptyMap())
    }
}

object TodayGoalGraph {
    @Volatile private var repository: TodayGoalRepository? = null
    fun repository(context: Context): TodayGoalRepository = synchronized(this) {
        repository ?: TodayGoalRepository(context.applicationContext).also { repository = it }
    }
}
