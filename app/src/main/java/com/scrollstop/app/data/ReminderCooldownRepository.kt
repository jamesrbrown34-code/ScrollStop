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

const val DEFAULT_REMINDER_COOLDOWN_MINUTES = 10

private val Context.reminderCooldownDataStore by preferencesDataStore("reminder_cooldown")

/** How long ScrollBeat waits between reminders, in minutes. Free setting, global. */
class ReminderCooldownRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cooldownKey = intPreferencesKey("cooldown_minutes")
    val cooldownMinutes: StateFlow<Int> = appContext.reminderCooldownDataStore.data
        .map { it[cooldownKey] ?: DEFAULT_REMINDER_COOLDOWN_MINUTES }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_REMINDER_COOLDOWN_MINUTES)

    fun setCooldown(minutes: Int) {
        scope.launch { appContext.reminderCooldownDataStore.edit { it[cooldownKey] = minutes.coerceAtLeast(0) } }
    }
}

object ReminderCooldownGraph {
    @Volatile private var repository: ReminderCooldownRepository? = null

    fun repository(context: Context): ReminderCooldownRepository = synchronized(this) {
        repository ?: ReminderCooldownRepository(context.applicationContext).also { repository = it }
    }
}
