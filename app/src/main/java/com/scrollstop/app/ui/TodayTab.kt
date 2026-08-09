package com.scrollstop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrollstop.app.core.ReminderMessaging
import com.scrollstop.app.core.ScrollStopStatus
import com.scrollstop.app.data.AnalyticsGraph
import com.scrollstop.app.data.DailyScrollSummary
import com.scrollstop.app.data.DashboardSnapshot
import com.scrollstop.app.data.ReductionPlanDifficulty
import com.scrollstop.app.data.ReductionPlanGraph
import com.scrollstop.app.data.ScrollComparison
import com.scrollstop.app.data.TopApp
import com.scrollstop.app.data.buildWeeklyBaseline
import com.scrollstop.app.premium.ReminderTone
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun TodayTab(
    dashboard: DashboardSnapshot,
    todayGoal: Int,
    perDayGoals: Map<DayOfWeek, Int>,
    latestNudge: String?,
    reminderTone: ReminderTone,
    lastFallback: String?,
    onSetGoal: (Int) -> Unit,
    onSetPerDayGoal: (DayOfWeek, Int?) -> Unit,
    onPersistFallback: (String) -> Unit,
    status: ScrollStopStatus,
    isPremium: Boolean,
    onOpenPremium: () -> Unit,
    onEnable: () -> Unit,
    onOpenAccessibilityList: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (status == ScrollStopStatus.ENABLED) {
            if (!isPremium) PremiumBanner(onOpenPremium = onOpenPremium)
            TodayScrollsCard(
                scrolls = dashboard.todayScrolls,
                comparison = dashboard.comparison,
                latestNudge = latestNudge,
                reminderTone = reminderTone,
                lastFallback = lastFallback,
                onPersistFallback = onPersistFallback
            )
            TodayGoalCard(
                todayScrolls = dashboard.todayScrolls,
                goal = todayGoal,
                perDayGoals = perDayGoals,
                isPremium = isPremium,
                onSetGoal = onSetGoal,
                onSetPerDayGoal = onSetPerDayGoal
            )
            ReductionPlanCard()
            WeekOverview(dashboard.week)
            TopAppsTodayCard(dashboard.topAppsToday)
        } else {
            TrackingPausedCard(
                status = status,
                onEnable = onEnable,
                onOpenAccessibilityList = onOpenAccessibilityList,
                onRefreshStatus = onRefreshStatus,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }
    }
}

@Composable
private fun PremiumBanner(onOpenPremium: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("ScrollBeat Premium", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Custom limits, tones, per-app controls, themes and more.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = onOpenPremium,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Background),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) { Text("Go Premium", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun TrackingPausedCard(
    status: ScrollStopStatus,
    onEnable: () -> Unit,
    onOpenAccessibilityList: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("⚠️", fontSize = 30.sp)
            Text(
                text = if (status == ScrollStopStatus.PERMISSION_REQUIRED) {
                    "Notifications need permission"
                } else {
                    "Your scrolling insights are paused"
                },
                color = TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = if (status == ScrollStopStatus.PERMISSION_REQUIRED) {
                    "Allow notifications so ScrollBeat can deliver its reminders."
                } else {
                    "Turn on ScrollBeat to start tracking your scrolling habits. Tracking will not work until the service is enabled."
                },
                color = TextPrimary.copy(alpha = 0.82f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (status == ScrollStopStatus.PERMISSION_REQUIRED) {
                Button(
                    onClick = onRequestNotificationPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                ) {
                    Text("Allow Notifications", fontWeight = FontWeight.SemiBold)
                }
            } else {
                EnableAccessibilityActions(
                    onOpenSettings = onEnable,
                    onOpenList = onOpenAccessibilityList,
                    onRefresh = onRefreshStatus,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayScrollsCard(
    scrolls: Int,
    comparison: ScrollComparison,
    latestNudge: String?,
    reminderTone: ReminderTone,
    lastFallback: String?,
    onPersistFallback: (String) -> Unit
) {
    var fallback by remember(reminderTone) { mutableStateOf<String?>(null) }
    val caption = if (latestNudge != null) {
        "Last nudge: $latestNudge"
    } else {
        fallback ?: run {
            val picked = ReminderMessaging.pickQuote(reminderTone, lastFallback)
            fallback = picked
            onPersistFallback(picked)
            picked
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Today's Scrolls", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = NumberFormat.getIntegerInstance().format(scrolls),
                color = TextPrimary,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = comparison.label(),
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (scrolls > 0 || latestNudge != null) {
                Text(
                    text = caption,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayGoalCard(
    todayScrolls: Int,
    goal: Int,
    perDayGoals: Map<DayOfWeek, Int>,
    isPremium: Boolean,
    onSetGoal: (Int) -> Unit,
    onSetPerDayGoal: (DayOfWeek, Int?) -> Unit
) {
    var customInput by remember { mutableStateOf("") }
    var showPerDay by remember { mutableStateOf(false) }
    val parsed = customInput.toIntOrNull()
    val showError = customInput.isNotEmpty() && (parsed == null || parsed <= 0)
    val commitCustom = {
        parsed?.takeIf { it > 0 }?.let(onSetGoal)
        if (customInput.isNotEmpty()) customInput = ""
    }
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Today's Goal", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${NumberFormat.getIntegerInstance().format(todayScrolls)} / ${NumberFormat.getIntegerInstance().format(goal)} scrolls",
                color = Accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
            LinearProgressIndicator(
                progress = { (todayScrolls.toFloat() / goal).coerceIn(0f, 1f) },
                color = Accent,
                trackColor = AccentContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                listOf(250, 500, 1000).forEach { preset ->
                    FilterChip(
                        selected = goal == preset,
                        onClick = { onSetGoal(preset) },
                        label = { Text(preset.toString()) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AccentContainer,
                            labelColor = TextPrimary,
                            selectedContainerColor = Accent,
                            selectedLabelColor = Background
                        )
                    )
                }
            }
            OutlinedTextField(
                value = customInput,
                onValueChange = { customInput = it.filter(Char::isDigit) },
                label = { Text("Custom goal") },
                isError = showError,
                supportingText = if (showError) {
                    { Text("Enter a positive number.", color = TextSecondary) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustom() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            if (isPremium) {
                TextButton(
                    onClick = { showPerDay = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) { Text("Per-day goals", color = Accent, fontSize = 13.sp) }
            }
        }
    }

    if (showPerDay) PerDayGoalsDialog(
        perDayGoals = perDayGoals,
        globalGoal = goal,
        onSetPerDayGoal = onSetPerDayGoal,
        onDismiss = { showPerDay = false }
    )
}

@Composable
private fun PerDayGoalsDialog(
    perDayGoals: Map<DayOfWeek, Int>,
    globalGoal: Int,
    onSetPerDayGoal: (DayOfWeek, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val inputs = remember(perDayGoals) {
        mutableStateMapOf<DayOfWeek, String>().apply {
            DayOfWeek.entries.forEach { day ->
                perDayGoals[day]?.let { put(day, it.toString()) }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Per-day goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "A different goal for each day. Leave blank to use the default.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                DayOfWeek.entries.forEach { day ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(day.displayName, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = inputs[day] ?: "",
                            onValueChange = { inputs[day] = it.filter(Char::isDigit) },
                            placeholder = { Text(globalGoal.toString()) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                DayOfWeek.entries.forEach { day ->
                    val text = inputs[day]
                    if (text.isNullOrBlank()) {
                        onSetPerDayGoal(day, null)
                    } else {
                        text.toIntOrNull()?.takeIf { it > 0 }?.let { onSetPerDayGoal(day, it) }
                    }
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private val DayOfWeek.displayName: String
    get() = when (this) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.TUESDAY -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY -> "Thursday"
        DayOfWeek.FRIDAY -> "Friday"
        DayOfWeek.SATURDAY -> "Saturday"
        DayOfWeek.SUNDAY -> "Sunday"
    }

@Composable
private fun WeekOverview(days: List<DailyScrollSummary>) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("This Week", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            val maximum = (days.maxOfOrNull { it.scrolls } ?: 1).coerceAtLeast(1)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.height(96.dp), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((18 + (72 * day.scrolls / maximum)).dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(if (day.isToday) Accent else AccentContainer)
                            )
                        }
                        Text(day.label, color = if (day.isToday) Accent else TextSecondary, fontSize = 11.sp)
                    }
                }
            }
            Text(
                "Today is highlighted",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun TopAppsTodayCard(topApps: List<TopApp>) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Top Apps Today", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (topApps.isEmpty()) {
                Text("No scrolling tracked today yet.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    topApps.forEachIndexed { index, app ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${index + 1}",
                                color = Accent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(app.displayName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(
                                "${NumberFormat.getIntegerInstance().format(app.scrolls)} scrolls",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ScrollComparison.label(): String = when (this) {
    is ScrollComparison.Lower -> "↓ $percent% less than yesterday"
    is ScrollComparison.Higher -> "↑ $percent% more than yesterday"
    ScrollComparison.Same -> "Same as yesterday"
}

@Composable
private fun ReductionPlanCard() {
    val context = LocalContext.current
    val repository = remember(context) { ReductionPlanGraph.repository(context) }
    val settings by repository.settings.collectAsState()
    val analytics = remember(context) { AnalyticsGraph.scrolls(context) }
    val events by analytics.events.collectAsState()
    val today = LocalDate.now()
    var showPicker by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Reduction Plan", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val s = settings
            if (s.isActive && s.difficulty != null) {
                val difficulty = s.difficulty
                val startDate = s.startDate
                if (startDate != null) {
                    val weekIndex = s.weekIndex(today)
                    val target = s.targetForWeek(today)
                    val weekStart = startDate.plusWeeks(weekIndex.toLong())
                    val weekEnd = startDate.plusWeeks((weekIndex + 1).toLong()).minusDays(1)
                    val actual = events.filter { it.date in weekStart..weekEnd }.sumOf { it.scrolls }
                    val complete = s.isComplete(today)
                    Text(
                        "${difficulty.displayName} · ${if (complete) "Complete" else "Week ${weekIndex + 1} of ${difficulty.durationWeeks}"}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        difficulty.scheduleLabel,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        "${NumberFormat.getIntegerInstance().format(actual)} / ${NumberFormat.getIntegerInstance().format(target)} scrolls this week",
                        color = Accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LinearProgressIndicator(
                        progress = { (actual.toFloat() / target).coerceIn(0f, 1f) },
                        color = Accent,
                        trackColor = AccentContainer,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    if (complete) {
                        val over = (actual - target).coerceAtLeast(0)
                        Text(
                            if (over > 0) {
                                "Plan complete — ${NumberFormat.getIntegerInstance().format(over)} scrolls over this week's target."
                            } else {
                                "Plan complete — you've hit this week's target."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        val remaining = (target - actual).coerceAtLeast(0)
                        if (remaining > 0) {
                            val daysLeft = ChronoUnit.DAYS.between(today, weekEnd) + 1
                            if (daysLeft > 0) {
                                val perDay = ceil(remaining.toDouble() / daysLeft).roundToInt()
                                Text(
                                    "${NumberFormat.getIntegerInstance().format(perDay)} scrolls/day for the rest of this week to stay on target",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        TextButton(onClick = { showPicker = true }) { Text("Change plan", color = Accent, fontSize = 13.sp) }
                        TextButton(onClick = { repository.clearPlan() }) { Text("Cancel plan", color = TextSecondary, fontSize = 13.sp) }
                    }
                }
            } else {
                val baseline = buildWeeklyBaseline(events, today)
                if (baseline == null) {
                    Text(
                        "Not enough data yet to build a baseline. Keep using ScrollBeat for a few more days.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Text(
                        "Cut your scrolling week by week, starting from your recent baseline.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        "Your weekly baseline: ${NumberFormat.getIntegerInstance().format(baseline)} scrolls",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = { showPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Start a plan", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    if (showPicker) {
        val planBaseline = if (settings.isActive) {
            settings.baselineWeeklyScrolls
        } else {
            buildWeeklyBaseline(events, today) ?: 0
        }
        ReductionPlanPickerDialog(
            current = settings.difficulty,
            baseline = planBaseline,
            hasActivePlan = settings.isActive,
            onPick = { difficulty ->
                repository.setPlan(difficulty, today, planBaseline)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun ReductionPlanPickerDialog(
    current: ReductionPlanDifficulty?,
    baseline: Int,
    hasActivePlan: Boolean,
    onPick: (ReductionPlanDifficulty) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reduction plan") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "A multi-week programme that sets a declining weekly target from your baseline.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    "Your weekly baseline: ${NumberFormat.getIntegerInstance().format(baseline)} scrolls",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                ReductionPlanDifficulty.entries.forEach { difficulty ->
                    val isSelected = selected == difficulty
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Accent else AccentContainer)
                            .clickable { selected = difficulty }
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${difficulty.displayName} · ${difficulty.durationWeeks} weeks",
                                color = if (isSelected) Background else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                difficulty.pitch,
                                color = if (isSelected) Background.copy(alpha = 0.8f) else TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                difficulty.scheduleLabel,
                                color = if (isSelected) Background.copy(alpha = 0.8f) else TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let(onPick) },
                enabled = selected != null
            ) { Text(if (hasActivePlan) "Use plan" else "Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
