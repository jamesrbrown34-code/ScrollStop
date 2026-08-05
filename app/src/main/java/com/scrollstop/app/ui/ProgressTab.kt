package com.scrollstop.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrollstop.app.data.DashboardSnapshot
import com.scrollstop.app.data.DayTopApp
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
        if (isPremium) AppDominanceCard(dashboard.topAppsOverTime)
        InterruptionsCard(dashboard.interruptionsToday)
    }
}

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
