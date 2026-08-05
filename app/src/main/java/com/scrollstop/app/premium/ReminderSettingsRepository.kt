package com.scrollstop.app.premium

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
import org.json.JSONObject

const val FREE_REMINDER_LIMIT = 100

data class ReminderSettings(
    val reminderLimit: Int = FREE_REMINDER_LIMIT,
    val reminderTone: ReminderTone = ReminderTone.GENERIC,
    val theme: AppTheme = AppTheme.MONOCHROME,
    val perAppLimits: Map<String, Int> = emptyMap(),
    val perAppTones: Map<String, ReminderTone> = emptyMap()
)

private val Context.reminderSettingsDataStore by preferencesDataStore("reminder_settings")

class ReminderSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val limitKey = intPreferencesKey("reminder_limit")
    private val toneKey = stringPreferencesKey("reminder_tone")
    private val themeKey = stringPreferencesKey("theme")
    private val perAppLimitsKey = stringPreferencesKey("per_app_limits")
    private val perAppTonesKey = stringPreferencesKey("per_app_tones")
    val settings: StateFlow<ReminderSettings> = appContext.reminderSettingsDataStore.data
        .map {
            ReminderSettings(
                reminderLimit = (it[limitKey] ?: FREE_REMINDER_LIMIT).coerceAtLeast(1),
                reminderTone = it[toneKey]?.let { name ->
                    ReminderTone.entries.firstOrNull { tone -> tone.name == name }
                } ?: ReminderTone.GENERIC,
                theme = it[themeKey]?.let { name ->
                    AppTheme.entries.firstOrNull { theme -> theme.name == name }
                } ?: AppTheme.MONOCHROME,
                perAppLimits = parseLimits(it[perAppLimitsKey] ?: ""),
                perAppTones = parseTones(it[perAppTonesKey] ?: "")
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, ReminderSettings())

    suspend fun setReminderLimit(limit: Int) {
        require(limit > 0) { "Reminder limit must be positive." }
        appContext.reminderSettingsDataStore.edit { it[limitKey] = limit }
    }

    suspend fun setReminderTone(tone: ReminderTone) {
        appContext.reminderSettingsDataStore.edit { it[toneKey] = tone.name }
    }

    suspend fun setTheme(theme: AppTheme) {
        appContext.reminderSettingsDataStore.edit { it[themeKey] = theme.name }
    }

    suspend fun setPerAppLimit(packageName: String, limit: Int?) {
        appContext.reminderSettingsDataStore.edit { prefs ->
            val current = parseLimits(prefs[perAppLimitsKey] ?: "")
            val updated = if (limit == null) current - packageName else current + (packageName to limit)
            val json = JSONObject()
            updated.forEach { (k, v) -> json.put(k, v) }
            prefs[perAppLimitsKey] = json.toString()
        }
    }

    suspend fun setPerAppTone(packageName: String, tone: ReminderTone?) {
        appContext.reminderSettingsDataStore.edit { prefs ->
            val current = parseTones(prefs[perAppTonesKey] ?: "")
            val updated = if (tone == null) current - packageName else current + (packageName to tone)
            val json = JSONObject()
            updated.forEach { (k, v) -> json.put(k, v.name) }
            prefs[perAppTonesKey] = json.toString()
        }
    }

    private fun parseLimits(json: String): Map<String, Int> {
        if (json.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getInt(it) }
        }.getOrDefault(emptyMap())
    }

    private fun parseTones(json: String): Map<String, ReminderTone> {
        if (json.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            obj.keys().asSequence().mapNotNull { key ->
                ReminderTone.entries.firstOrNull { it.name == obj.getString(key) }?.let { key to it }
            }.toMap()
        }.getOrDefault(emptyMap())
    }
}
