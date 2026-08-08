package com.scrollstop.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
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

private val Context.nudgeDataStore by preferencesDataStore("nudge_store")

/** The last reminder message shown, and the last rotated tone quote, for the Today dashboard. */
class NudgeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastMessageKey = stringPreferencesKey("last_message")
    private val lastFallbackKey = stringPreferencesKey("last_fallback")

    val lastMessage: StateFlow<String?> = appContext.nudgeDataStore.data
        .map { it[lastMessageKey] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val lastFallback: StateFlow<String?> = appContext.nudgeDataStore.data
        .map { it[lastFallbackKey] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun setLastMessage(message: String) {
        scope.launch { appContext.nudgeDataStore.edit { it[lastMessageKey] = message } }
    }

    fun setLastFallback(message: String) {
        scope.launch { appContext.nudgeDataStore.edit { it[lastFallbackKey] = message } }
    }
}

object NudgeGraph {
    @Volatile private var repository: NudgeRepository? = null

    fun repository(context: Context): NudgeRepository = synchronized(this) {
        repository ?: NudgeRepository(context.applicationContext).also { repository = it }
    }
}
