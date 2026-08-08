package com.scrollstop.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class DashboardSnapshot(
    val todayScrolls: Int = 0,
    val comparison: ScrollComparison = ScrollComparison.Same,
    val week: List<DailyScrollSummary> = emptyList(),
    val currentStreakDays: Int = 0,
    val bestDay: DailyScrollSummary? = null,
    val fewerScrollsThisWeek: Int = 0,
    val interruptionsToday: Int = 0,
    val interruptionsThisWeek: Int = 0,
    val topAppsToday: List<TopApp> = emptyList(),
    val topAppsOverTime: List<DayTopApp> = emptyList(),
    val todayByHour: List<Int> = List(24) { 0 },
    val thisMonthScrolls: Int = 0,
    val monthly: List<MonthlyScrollSummary> = emptyList(),
    val monthComparison: ScrollComparison = ScrollComparison.Same,
    val yearToDateScrolls: Int = 0,
    val bestMonthYear: MonthlyScrollSummary? = null,
    val averagePerDayYear: Int = 0,
    val yearOverYear: ScrollComparison? = null,
    val monthlyMomentum: ScrollComparison? = null,
    val longTrend: List<MonthlyScrollSummary> = emptyList(),
    val longTrendComparison: ScrollComparison? = null
)

data class DailyScrollSummary(val label: String, val scrolls: Int, val isToday: Boolean = false)

data class MonthlyScrollSummary(val label: String, val scrolls: Int, val isCurrentMonth: Boolean = false)

data class TopApp(val displayName: String, val scrolls: Int)

data class DayTopApp(val label: String, val displayName: String, val scrolls: Int)

sealed interface ScrollComparison {
    data class Lower(val percent: Int) : ScrollComparison
    data class Higher(val percent: Int) : ScrollComparison
    data object Same : ScrollComparison
}

class DashboardRepository(private val analytics: ScrollAnalyticsRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _snapshot = MutableStateFlow(createSnapshot(emptyList(), emptyList()))
    val snapshot: StateFlow<DashboardSnapshot> = _snapshot.asStateFlow()

    /** Mutable: set by the UI when premium entitlement changes — enables the streak freeze. */
    var isPremium: Boolean = false

    init {
        scope.launch {
            combine(analytics.events, analytics.appEvents) { events, appEvents ->
                createSnapshot(events, appEvents)
            }.collect { _snapshot.value = it }
        }
    }

    /** Recalculates date-sensitive values, including the midnight rollover. */
    fun refresh() {
        _snapshot.value = createSnapshot(analytics.events.value, analytics.appEvents.value)
    }

    private fun createSnapshot(events: List<ScrollEvent>, appEvents: List<AppScrollEvent>): DashboardSnapshot {
        val today = LocalDate.now()
        val byDate = events.associateBy { it.date }
        fun count(date: LocalDate) = byDate[date]?.scrolls ?: 0
        val todayScrolls = count(today)
        val yesterdayScrolls = count(today.minusDays(1))
        val weekDates = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val week = weekDates.map { date ->
            DailyScrollSummary(
                label = if (date == today) "Today" else date.dayOfWeek.shortLabel(),
                scrolls = count(date),
                isToday = date == today
            )
        }
        val currentWeek = week.sumOf { it.scrolls }
        val previousWeek = (7..13).sumOf { count(today.minusDays(it.toLong())) }
        val activeDays = events.filter { it.scrolls > 0 }
        val best = activeDays.minByOrNull { it.scrolls }?.let {
            DailyScrollSummary(it.date.dayOfWeek.shortLabel(), it.scrolls)
        }
        val topAppsToday = appEvents
            .filter { it.date == today }
            .map { TopApp(it.packageName.toDisplayName(), it.scrolls) }
            .sortedByDescending { it.scrolls }
            .take(3)

        val topAppsOverTime = weekDates.map { date ->
            val top = appEvents.filter { it.date == date }.maxByOrNull { it.scrolls }
            DayTopApp(
                label = if (date == today) "Today" else date.dayOfWeek.shortLabel(),
                displayName = top?.packageName?.toDisplayName() ?: "—",
                scrolls = top?.scrolls ?: 0
            )
        }

        val todayByHour = List(24) { hour -> byDate[today]?.hourlyScrolls?.get(hour) ?: 0 }

        val byYearMonth = events.groupBy { it.date.year to it.date.month }
        fun monthTotal(month: LocalDate): Int =
            byYearMonth[month.year to month.month]?.sumOf { it.scrolls } ?: 0

        val currentMonthStart = today.withDayOfMonth(1)
        val monthList = (11 downTo 0).map { currentMonthStart.minusMonths(it.toLong()) }
        val monthly = monthList.map { month ->
            MonthlyScrollSummary(
                label = month.month.shortLabel(),
                scrolls = monthTotal(month),
                isCurrentMonth = month == currentMonthStart
            )
        }
        val thisMonthScrolls = monthTotal(currentMonthStart)
        val monthComparison = comparison(thisMonthScrolls, monthTotal(currentMonthStart.minusMonths(1)))

        val yearStart = today.withDayOfYear(1)
        val yearScrolls = events.filter { !it.date.isBefore(yearStart) }.sumOf { it.scrolls }
        val yearMonths = (0 until today.monthValue - 1).map { yearStart.plusMonths(it.toLong()) }
        val yearMonthly = yearMonths.map { month ->
            MonthlyScrollSummary(label = month.month.shortLabel(), scrolls = monthTotal(month))
        }
        val bestMonthYear = yearMonthly.maxByOrNull { it.scrolls }?.takeIf { it.scrolls > 0 }
        val averagePerDayYear = if (today.dayOfYear > 0) yearScrolls / today.dayOfYear else 0
        val lastYearScrolls = events
            .filter { !it.date.isBefore(yearStart.minusYears(1)) && !it.date.isAfter(today.minusYears(1)) }
            .sumOf { it.scrolls }
        val yearOverYear = if (yearScrolls > 0 || lastYearScrolls > 0) {
            comparison(yearScrolls, lastYearScrolls)
        } else {
            null
        }
        val monthlyMomentum = if (yearMonths.size >= 2) {
            val last = monthTotal(yearMonths[yearMonths.size - 1])
            val previous = monthTotal(yearMonths[yearMonths.size - 2])
            if (last > 0 || previous > 0) comparison(last, previous) else null
        } else {
            null
        }
        val longTrend = monthList.takeLast(6).map { month ->
            MonthlyScrollSummary(label = month.month.shortLabel(), scrolls = monthTotal(month))
        }
        val recentQuarter = longTrend.takeLast(3).sumOf { it.scrolls }
        val priorQuarter = longTrend.take(3).sumOf { it.scrolls }
        val longTrendComparison = if (recentQuarter > 0 || priorQuarter > 0) {
            comparison(recentQuarter, priorQuarter)
        } else {
            null
        }

        return DashboardSnapshot(
            todayScrolls = todayScrolls,
            comparison = comparison(todayScrolls, yesterdayScrolls),
            week = week,
            currentStreakDays = streak(today, byDate, isPremium),
            bestDay = best,
            fewerScrollsThisWeek = (previousWeek - currentWeek).coerceAtLeast(0),
            interruptionsToday = byDate[today]?.interruptionsShown ?: 0,
            interruptionsThisWeek = weekDates.sumOf { byDate[it]?.interruptionsShown ?: 0 },
            topAppsToday = topAppsToday,
            topAppsOverTime = topAppsOverTime,
            todayByHour = todayByHour,
            thisMonthScrolls = thisMonthScrolls,
            monthly = monthly,
            monthComparison = monthComparison,
            yearToDateScrolls = yearScrolls,
            bestMonthYear = bestMonthYear,
            averagePerDayYear = averagePerDayYear,
            yearOverYear = yearOverYear,
            monthlyMomentum = monthlyMomentum,
            longTrend = longTrend,
            longTrendComparison = longTrendComparison
        )
    }

    private fun comparison(today: Int, yesterday: Int): ScrollComparison {
        if (today == yesterday) return ScrollComparison.Same
        if (yesterday == 0) return ScrollComparison.Higher(100)
        val percent = ((kotlin.math.abs(today - yesterday) * 100.0) / yesterday).roundToInt()
        return if (today < yesterday) ScrollComparison.Lower(percent) else ScrollComparison.Higher(percent)
    }

    private fun streak(today: LocalDate, events: Map<LocalDate, ScrollEvent>, isPremium: Boolean): Int {
        var days = 0
        var date = today
        var freezeAvailable = isPremium
        while (true) {
            val scrolls = events[date]?.scrolls ?: 0
            if (scrolls > 0) { days++; date = date.minusDays(1); continue }
            if (freezeAvailable) { freezeAvailable = false; days++; date = date.minusDays(1); continue }
            break
        }
        return days
    }

    private fun java.time.DayOfWeek.shortLabel(): String =
        name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    private fun java.time.Month.shortLabel(): String =
        name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    private fun String.toDisplayName(): String =
        CuratedLibrary.apps.firstOrNull { it.packageName == this }?.displayName ?: this
}
