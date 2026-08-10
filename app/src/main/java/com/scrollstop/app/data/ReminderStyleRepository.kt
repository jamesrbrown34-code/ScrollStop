package com.scrollstop.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.scrollstop.app.core.ReminderStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.reminderStyleDataStore by preferencesDataStore("reminder_style")

class ReminderStyleRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val styleKey = stringPreferencesKey("style")
    val style: StateFlow<ReminderStyle> = appContext.reminderStyleDataStore.data
        .map { prefs ->
            prefs[styleKey]?.let { name ->
                ReminderStyle.entries.firstOrNull { it.name == name }
            } ?: ReminderStyle.HEADS_UP
        }
        .stateIn(scope, SharingStarted.Eagerly, ReminderStyle.HEADS_UP)

    fun setStyle(style: ReminderStyle) {
        scope.launch { appContext.reminderStyleDataStore.edit { it[styleKey] = style.name } }
    }
}

object ReminderStyleGraph {
    @Volatile private var repository: ReminderStyleRepository? = null

    fun repository(context: Context): ReminderStyleRepository = synchronized(this) {
        repository ?: ReminderStyleRepository(context.applicationContext).also { repository = it }
    }
}
