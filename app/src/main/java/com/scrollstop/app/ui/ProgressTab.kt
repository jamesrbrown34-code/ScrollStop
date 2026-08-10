package com.scrollstop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrollstop.app.data.DashboardSnapshot
import com.scrollstop.app.data.DayTopApp
import com.scrollstop.app.data.MonthlyScrollSummary
import com.scrollstop.app.data.ScrollComparison
import java.text.NumberFormat

@Composable
internal fun ProgressTab(dashboard: DashboardSnapshot, isPremium: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WeeklyReportCard(dashboard)
        MonthlyOverviewCard(dashboard.monthly, dashboard.thisMonthScrolls, dashboard.monthComparison)
        if (isPremium) YearlyOverviewCard(dashboard)
        if (isPremium) LongTermTrendCard(dashboard.longTrend, dashboard.longTrendComparison)
        if (isPremium) AppDominanceCard(dashboard.topAppsOverTime)
        if (isPremium) HourlyHeatmapCard(dashboard.todayByHour)
        if (isPremium) MilestonesCard(dashboard.currentStreakDays)
        InterruptionsCard(dashboard.interruptionsToday)
    }
}

private val milestoneDays = listOf(7, 14, 30, 60, 100, 200, 365)

@Composable
private fun WeeklyReportCard(dashboard: DashboardSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("This Week", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${NumberFormat.getIntegerInstance().format(dashboard.week.sumOf { it.scrolls })} scrolls this week",
                color = Accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                WeekStatLine("🔥", "Current streak", "${dashboard.currentStreakDays} days")
                WeekStatLine(
                    "🏆",
                    "Best day",
                    dashboard.bestDay?.let { "${it.label} · ${NumberFormat.getIntegerInstance().format(it.scrolls)} scrolls" }
                        ?: "Your best day will appear here"
                )
                WeekStatLine(
                    "📉",
                    "Improvement",
                    "${NumberFormat.getIntegerInstance().format(dashboard.fewerScrollsThisWeek)} fewer scrolls than last week"
                )
                WeekStatLine("⏸️", "Reminders given", "${dashboard.interruptionsThisWeek} this week")
            }
            if (dashboard.currentStreakDays in milestoneDays) {
                Text(
                    "🏅 ${dashboard.currentStreakDays}-day streak — milestone reached!",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun WeekStatLine(icon: String, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun MonthlyOverviewCard(
    monthly: List<MonthlyScrollSummary>,
    thisMonth: Int,
    monthComparison: ScrollComparison
) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Monthly Breakdown", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (thisMonth == 0 && monthly.all { it.scrolls == 0 }) {
                Text("No data yet.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            } else {
                Text(
                    "${NumberFormat.getIntegerInstance().format(thisMonth)} scrolls this month",
                    color = Accent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    monthComparison.trendLabel("last month"),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                MonthBarChart(monthly)
                Text(
                    "Your last ${monthly.size} months, by scrolls.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun YearlyOverviewCard(dashboard: DashboardSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("This Year", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (dashboard.yearToDateScrolls == 0 && dashboard.yearOverYear == null) {
                Text("No data yet.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            } else {
                Text(
                    "${NumberFormat.getIntegerInstance().format(dashboard.yearToDateScrolls)} scrolls this year",
                    color = Accent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    WeekStatLine(
                        "🏆",
                        "Best month",
                        dashboard.bestMonthYear?.let {
                            "${it.label} · ${NumberFormat.getIntegerInstance().format(it.scrolls)} scrolls"
                        } ?: "Your best month will appear here"
                    )
                    WeekStatLine(
                        "📅",
                        "Average per day",
                        "${NumberFormat.getIntegerInstance().format(dashboard.averagePerDayYear)} scrolls"
                    )
                    WeekStatLine(
                        "📈",
                        "This year vs last",
                        dashboard.yearOverYear?.trendLabel("last year") ?: "Tracking for less than a year"
                    )
                    WeekStatLine(
                        "↔️",
                        "Month over month",
                        dashboard.monthlyMomentum?.trendLabel("the month before") ?: "Not enough history yet"
                    )
                }
            }
        }
    }
}

@Composable
private fun LongTermTrendCard(longTrend: List<MonthlyScrollSummary>, trendComparison: ScrollComparison?) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Long-term Trend", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (longTrend.all { it.scrolls == 0 }) {
                Text("No data yet.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            } else {
                MonthBarChart(longTrend)
                val average = longTrend.sumOf { it.scrolls } / longTrend.size
                Text(
                    "~${NumberFormat.getIntegerInstance().format(average)} scrolls per month, last ${longTrend.size} months.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    trendComparison?.trendLabel("the previous three months")
                        ?: "Not enough history for a trend yet.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthBarChart(months: List<MonthlyScrollSummary>) {
    val max = (months.maxOfOrNull { it.scrolls } ?: 0).coerceAtLeast(1)
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().height(88.dp).padding(top = 12.dp)
    ) {
        months.forEach { month ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp)
                    .height((14 + 64 * month.scrolls / max).dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(if (month.scrolls > 0) Accent else AccentContainer)
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        months.forEach { month ->
            Text(
                month.label,
                color = TextSecondary,
                fontSize = 9.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 1.dp)
            )
        }
    }
}

private fun ScrollComparison.trendLabel(period: String): String = when (this) {
    is ScrollComparison.Lower -> "↓ $percent% fewer than $period"
    is ScrollComparison.Higher -> "↑ $percent% more than $period"
    ScrollComparison.Same -> "About the same as $period"
}

@Composable
private fun AppDominanceCard(topAppsOverTime: List<DayTopApp>) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Daily Top App", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                topAppsOverTime.forEach { day ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(day.label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(36.dp))
                        Text(
                            day.displayName,
                            color = if (day.scrolls > 0) TextPrimary else TextSecondary.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (day.scrolls > 0) {
                            Text(
                                "${NumberFormat.getIntegerInstance().format(day.scrolls)}",
                                color = Accent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyHeatmapCard(todayByHour: List<Int>) {
    val max = (todayByHour.maxOrNull() ?: 0).coerceAtLeast(1)
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Today by the hour", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (todayByHour.sum() == 0) {
                Text("No scrolling tracked today yet.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth().height(96.dp).padding(top = 12.dp)
                ) {
                    todayByHour.forEachIndexed { hour, count ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.dp)
                                .height((16 + 72 * count / max).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(if (count > 0) Accent else AccentContainer)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("12a", "6a", "12p", "6p", "12a").forEach {
                        Text(it, color = TextSecondary, fontSize = 10.sp)
                    }
                }
                Text(
                    "Your busiest hours of the day.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MilestonesCard(currentStreakDays: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Milestones", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp)) {
                milestoneDays.forEach { days ->
                    val unlocked = currentStreakDays >= days
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (unlocked) "✓" else "•",
                            color = if (unlocked) Accent else TextSecondary.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            "$days-day streak",
                            color = if (unlocked) TextPrimary else TextSecondary.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = if (unlocked) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterruptionsCard(interruptions: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = AccentContainer), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Recent Interruptions", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("You paused:", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp))
            Text("$interruptions times today", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Each pause is a moment to choose what comes next.", color = TextPrimary.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
