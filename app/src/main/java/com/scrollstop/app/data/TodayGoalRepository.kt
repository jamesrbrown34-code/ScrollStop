package com.scrollstop.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val DEFAULT_TODAY_GOAL = 500

private val Context.todayGoalDataStore by preferencesDataStore("today_goal")

class TodayGoalRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = intPreferencesKey("goal_limit")
    val goal: StateFlow<Int> = appContext.todayGoalDataStore.data
        .map { (it[key] ?: DEFAULT_TODAY_GOAL).coerceAtLeast(1) }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_TODAY_GOAL)

    fun setGoal(limit: Int) {
        scope.launch {
            appContext.todayGoalDataStore.edit { it[key] = limit.coerceAtLeast(1) }
        }
    }
}

object TodayGoalGraph {
    @Volatile private var repository: TodayGoalRepository? = null
    fun repository(context: Context): TodayGoalRepository = synchronized(this) {
        repository ?: TodayGoalRepository(context.applicationContext).also { repository = it }
    }
}
