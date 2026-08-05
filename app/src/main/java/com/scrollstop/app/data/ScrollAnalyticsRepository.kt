package com.scrollstop.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

data class ScrollEvent(
    val timestamp: Long,
    val date: LocalDate,
    val scrolls: Int,
    val interruptionsShown: Int
)

data class AppScrollEvent(
    val timestamp: Long,
    val date: LocalDate,
    val packageName: String,
    val scrolls: Int,
    val interruptionsShown: Int
)

interface ScrollAnalyticsRepository {
    val events: StateFlow<List<ScrollEvent>>
    val appEvents: StateFlow<List<AppScrollEvent>>

    fun recordScroll(packageName: String, timestamp: Long = System.currentTimeMillis())
    fun recordInterruption(packageName: String, timestamp: Long = System.currentTimeMillis())
}

/** Local, dependency-free storage for the rolling 30-day analytics history. */
class LocalScrollAnalyticsRepository(context: Context) : ScrollAnalyticsRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val _events = MutableStateFlow(readEvents())
    override val events: StateFlow<List<ScrollEvent>> = _events.asStateFlow()
    private val _appEvents = MutableStateFlow(readAppEvents())
    override val appEvents: StateFlow<List<AppScrollEvent>> = _appEvents.asStateFlow()

    override fun recordScroll(packageName: String, timestamp: Long) {
        updateDay(timestamp) { event ->
            event.copy(scrolls = event.scrolls + 1)
        }
        updateAppDay(timestamp, packageName) { event ->
            event.copy(scrolls = event.scrolls + 1)
        }
    }

    override fun recordInterruption(packageName: String, timestamp: Long) {
        updateDay(timestamp) { event ->
            event.copy(interruptionsShown = event.interruptionsShown + 1)
        }
        updateAppDay(timestamp, packageName) { event ->
            event.copy(interruptionsShown = event.interruptionsShown + 1)
        }
    }

    private fun updateDay(timestamp: Long, transform: (ScrollEvent) -> ScrollEvent) {
        val date = timestamp.toLocalDate()
        _events.value = updateEvents(_events.value, date, timestamp, transform)

        scope.launch {
            synchronized(lock) {
                val persisted = readEvents()
                val updated = updateEvents(persisted, date, timestamp, transform)
                preferences.edit().putString(EVENTS_KEY, encode(updated)).apply()
                _events.value = updated
            }
        }
    }

    private fun updateEvents(
        events: List<ScrollEvent>,
        date: LocalDate,
        timestamp: Long,
        transform: (ScrollEvent) -> ScrollEvent
    ): List<ScrollEvent> {
        val existing = events.firstOrNull { it.date == date }
            ?: ScrollEvent(timestamp, date, scrolls = 0, interruptionsShown = 0)
        val updated = transform(existing).copy(timestamp = timestamp)
        return (events.filterNot { it.date == date } + updated)
            .sortedBy { it.date }
            .takeLast(RETAINED_DAYS)
    }

    private fun updateAppDay(timestamp: Long, packageName: String, transform: (AppScrollEvent) -> AppScrollEvent) {
        val date = timestamp.toLocalDate()
        _appEvents.value = updateAppEvents(_appEvents.value, date, packageName, timestamp, transform)

        scope.launch {
            synchronized(lock) {
                val persisted = readAppEvents()
                val updated = updateAppEvents(persisted, date, packageName, timestamp, transform)
                preferences.edit().putString(APP_EVENTS_KEY, encodeApp(updated)).apply()
                _appEvents.value = updated
            }
        }
    }

    private fun updateAppEvents(
        events: List<AppScrollEvent>,
        date: LocalDate,
        packageName: String,
        timestamp: Long,
        transform: (AppScrollEvent) -> AppScrollEvent
    ): List<AppScrollEvent> {
        val existing = events.firstOrNull { it.date == date && it.packageName == packageName }
            ?: AppScrollEvent(timestamp, date, packageName, scrolls = 0, interruptionsShown = 0)
        val updated = transform(existing).copy(timestamp = timestamp)
        return (events.filterNot { it.date == date && it.packageName == packageName } + updated)
            .sortedBy { it.date }
            .takeLast(RETAINED_DAYS)
    }

    private fun readEvents(): List<ScrollEvent> = decode(preferences.getString(EVENTS_KEY, null))

    private fun readAppEvents(): List<AppScrollEvent> = decodeApp(preferences.getString(APP_EVENTS_KEY, null))
    private fun decode(serialized: String?): List<ScrollEvent> {
        if (serialized.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(serialized)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        ScrollEvent(
                            timestamp = item.getLong("timestamp"),
                            date = LocalDate.parse(item.getString("date")),
                            scrolls = item.getInt("scrolls"),
                            interruptionsShown = item.getInt("interruptionsShown")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(events: List<ScrollEvent>): String = JSONArray().apply {
        events.forEach { event ->
            put(JSONObject().apply {
                put("timestamp", event.timestamp)
                put("date", event.date.toString())
                put("scrolls", event.scrolls)
                put("interruptionsShown", event.interruptionsShown)
            })
        }
    }.toString()

    private fun decodeApp(serialized: String?): List<AppScrollEvent> {
        if (serialized.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(serialized)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        AppScrollEvent(
                            timestamp = item.getLong("timestamp"),
                            date = LocalDate.parse(item.getString("date")),
                            packageName = item.getString("package"),
                            scrolls = item.getInt("scrolls"),
                            interruptionsShown = item.getInt("interruptionsShown")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeApp(events: List<AppScrollEvent>): String = JSONArray().apply {
        events.forEach { event ->
            put(JSONObject().apply {
                put("timestamp", event.timestamp)
                put("date", event.date.toString())
                put("package", event.packageName)
                put("scrolls", event.scrolls)
                put("interruptionsShown", event.interruptionsShown)
            })
        }
    }.toString()

    private fun Long.toLocalDate(): LocalDate =
        java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    private companion object {
        const val PREFERENCES_NAME = "scroll_analytics"
        const val EVENTS_KEY = "daily_scroll_events"
        const val APP_EVENTS_KEY = "app_scroll_events"
        const val RETAINED_DAYS = 60
    }
}

/** Application-scoped repository holder; UI only consumes DashboardRepository. */
object AnalyticsGraph {
    @Volatile private var dashboardRepository: DashboardRepository? = null
    @Volatile private var scrollRepository: ScrollAnalyticsRepository? = null

    fun dashboard(context: Context): DashboardRepository = synchronized(this) {
        val existing = dashboardRepository
        if (existing != null) {
            existing
        } else {
            DashboardRepository(scrolls(context)).also { dashboardRepository = it }
        }
    }

    fun scrolls(context: Context): ScrollAnalyticsRepository = synchronized(this) {
        val existing = scrollRepository
        if (existing != null) {
            existing
        } else {
            LocalScrollAnalyticsRepository(context).also { scrollRepository = it }
        }
    }
}
