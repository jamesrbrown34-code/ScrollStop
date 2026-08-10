package com.scrollstop.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import java.time.LocalTime

data class QuietHoursSettings(
    val enabled: Boolean = false,
    val startMinutes: Int = 22 * 60,
    val endMinutes: Int = 8 * 60,
    /** Apps opted out of the global quiet-hours window (Premium per-app quiet hours). */
    val quietExcludedApps: Set<String> = emptySet()
)

private val Context.quietHoursDataStore by preferencesDataStore("quiet_hours")

class QuietHoursRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val enabledKey = booleanPreferencesKey("enabled")
    private val startKey = intPreferencesKey("start_minutes")
    private val endKey = intPreferencesKey("end_minutes")
    private val quietExcludedKey = stringPreferencesKey("quiet_excluded_apps")
    val settings: StateFlow<QuietHoursSettings> = appContext.quietHoursDataStore.data
        .map {
            QuietHoursSettings(
                enabled = it[enabledKey] ?: false,
                startMinutes = it[startKey] ?: 22 * 60,
                endMinutes = it[endKey] ?: 8 * 60,
                quietExcludedApps = parseSet(it[quietExcludedKey] ?: "")
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, QuietHoursSettings())

    /** True when the current time falls inside the window AND [packageName] isn't opted out. */
    fun isInWindow(packageName: String? = null, now: LocalTime = LocalTime.now()): Boolean {
        val settings = settings.value
        if (!settings.enabled) return false
        if (packageName != null && packageName in settings.quietExcludedApps) return false
        val nowMinutes = now.hour * 60 + now.minute
        return if (settings.startMinutes <= settings.endMinutes) {
            nowMinutes in settings.startMinutes until settings.endMinutes
        } else {
            nowMinutes >= settings.startMinutes || nowMinutes < settings.endMinutes
        }
    }

    /** Opts a single app in/out of the global quiet-hours window. */
    fun setQuietExcludedApp(packageName: String, excluded: Boolean) {
        scope.launch {
            appContext.quietHoursDataStore.edit { prefs ->
                val current = parseSet(prefs[quietExcludedKey] ?: "")
                val updated = if (excluded) current + packageName else current - packageName
                prefs[quietExcludedKey] = updated.joinToString(",")
            }
        }
    }

    /** Flips an app's quiet-hours opt-out based on its current state. */
    fun toggleQuietExcluded(packageName: String) {
        setQuietExcludedApp(packageName, packageName !in settings.value.quietExcludedApps)
    }

    fun update(settings: QuietHoursSettings) {
        scope.launch {
            appContext.quietHoursDataStore.edit {
                it[enabledKey] = settings.enabled
                it[startKey] = settings.startMinutes
                it[endKey] = settings.endMinutes
                it[quietExcludedKey] = settings.quietExcludedApps.joinToString(",")
            }
        }
    }

    private fun parseSet(serialized: String): Set<String> =
        serialized.split(',').filter { it.isNotBlank() }.toSet()
}

object QuietHoursGraph {
    @Volatile private var repository: QuietHoursRepository? = null
    fun repository(context: Context): QuietHoursRepository = synchronized(this) {
        repository ?: QuietHoursRepository(context.applicationContext).also { repository = it }
    }
}
