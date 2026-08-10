package com.scrollstop.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.scrollstop.app.core.SummaryReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class DailySummarySettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0
)

private val Context.dailySummaryDataStore by preferencesDataStore("daily_summary")

/** A once-a-day local notification summarising the day's scrolling (Premium). */
class DailySummaryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val enabledKey = booleanPreferencesKey("enabled")
    private val hourKey = intPreferencesKey("hour")
    private val minuteKey = intPreferencesKey("minute")
    val settings: StateFlow<DailySummarySettings> = appContext.dailySummaryDataStore.data
        .map {
            DailySummarySettings(
                enabled = it[enabledKey] ?: false,
                hour = it[hourKey] ?: 20,
                minute = it[minuteKey] ?: 0
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, DailySummarySettings())

    fun setEnabled(enabled: Boolean) {
        val current = settings.value
        scope.launch {
            appContext.dailySummaryDataStore.edit { it[enabledKey] = enabled }
            scheduleSummary(appContext, current.copy(enabled = enabled))
        }
    }

    fun setTime(hour: Int, minute: Int) {
        val current = settings.value
        scope.launch {
            appContext.dailySummaryDataStore.edit {
                it[hourKey] = hour
                it[minuteKey] = minute
            }
            scheduleSummary(appContext, current.copy(hour = hour, minute = minute))
        }
    }
}

object DailySummaryGraph {
    @Volatile private var repository: DailySummaryRepository? = null

    fun repository(context: Context): DailySummaryRepository = synchronized(this) {
        repository ?: DailySummaryRepository(context.applicationContext).also { repository = it }
    }
}

fun scheduleSummary(context: Context, settings: DailySummarySettings) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val pendingIntent = summaryPendingIntent(context)
    if (!settings.enabled) {
        alarmManager.cancel(pendingIntent)
        return
    }
    alarmManager.setInexactRepeating(
        AlarmManager.RTC,
        nextTriggerAtMillis(settings.hour, settings.minute),
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

/** Re-applies whatever the stored schedule is — used on boot and after install. */
fun rescheduleSummary(context: Context) {
    scheduleSummary(context, DailySummaryGraph.repository(context).settings.value)
}

private fun summaryPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SummaryReceiver::class.java).apply {
        action = SummaryReceiver.ACTION
    }
    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun nextTriggerAtMillis(hour: Int, minute: Int): Long {
    val now = ZonedDateTime.now()
    var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return next.toInstant().toEpochMilli()
}
