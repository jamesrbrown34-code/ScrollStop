package com.scrollstop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.scrollstop.app.BuildConfig
import com.scrollstop.app.core.ReminderStyle
import com.scrollstop.app.core.ScrollStopStatus
import com.scrollstop.app.data.CuratedLibrary
import com.scrollstop.app.data.QuietHoursSettings
import com.scrollstop.app.data.getInstalledApps
import com.scrollstop.app.premium.FREE_REMINDER_LIMIT
import com.scrollstop.app.premium.AppTheme
import com.scrollstop.app.premium.PremiumUiState
import com.scrollstop.app.premium.ReminderTone
import kotlinx.coroutines.delay

@Composable
internal fun SettingsTab(
    premium: PremiumUiState,
    trackedApps: Set<String>,
    pausedUntil: Long,
    quietHours: QuietHoursSettings,
    debugPremiumEnabled: Boolean,
    reminderStyle: ReminderStyle,
    fullScreenAllowed: Boolean,
    cooldownMinutes: Int,
    status: ScrollStopStatus,
    serviceActive: Boolean,
    notificationGranted: Boolean,
    onOpenPremium: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAccessibilityList: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSetReminderStyle: (ReminderStyle) -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    onSetCooldown: (Int) -> Unit,
    onSetLimit: (Int) -> Unit,
    onSetTone: (ReminderTone) -> Unit,
    onSetTheme: (AppTheme) -> Unit,
    onUpdateQuietHours: (QuietHoursSettings) -> Unit,
    onSetQuietExcluded: (String) -> Unit,
    dailySummaryEnabled: Boolean,
    dailySummaryHour: Int,
    dailySummaryMinute: Int,
    onSetDailySummaryEnabled: (Boolean) -> Unit,
    onSetDailySummaryTime: (Int, Int) -> Unit,
    onToggleApp: (String) -> Unit,
    onAddApp: (String) -> Unit,
    onSetPerAppLimit: (String, Int?) -> Unit,
    onSetPerAppTone: (String, ReminderTone?) -> Unit,
    onSetDebugPremium: (Boolean) -> Unit
) {
    var showTrackedApps by remember { mutableStateOf(false) }
    var showQuietHours by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusCard(
            status = status,
            serviceActive = serviceActive,
            notificationGranted = notificationGranted,
            onOpenSettings = onOpenAccessibilitySettings,
            onOpenList = onOpenAccessibilityList,
            onRefresh = onRefreshStatus,
            onRequestNotification = onRequestNotificationPermission
        )
        ReminderLimitCard(premium, onOpenPremium = onOpenPremium, onSetLimit = onSetLimit)
        ReminderStyleCard(
            style = reminderStyle,
            fullScreenAllowed = fullScreenAllowed,
            onOpenFullScreenSettings = onOpenFullScreenSettings,
            onSetStyle = onSetReminderStyle
        )
        ReminderCooldownCard(
            cooldownMinutes = cooldownMinutes,
            onSetCooldown = onSetCooldown
        )
        if (premium.isPremium) DailySummaryCard(
            enabled = dailySummaryEnabled,
            hour = dailySummaryHour,
            minute = dailySummaryMinute,
            onSetEnabled = onSetDailySummaryEnabled,
            onSetTime = onSetDailySummaryTime
        )
        if (premium.isPremium) ReminderToneCard(premium.reminderTone, onSetTone = onSetTone)
        if (premium.isPremium) ThemeCard(premium.theme, onSetTheme = onSetTheme)
        QuietHoursCard(quietHours, onEdit = { showQuietHours = true })
        TrackedAppsCard(trackedApps, pausedUntil = pausedUntil, onEditApps = { showTrackedApps = true })

        if (BuildConfig.DEBUG) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Debug premium override", color = TextSecondary, fontSize = 12.sp)
                Switch(checked = debugPremiumEnabled, onCheckedChange = onSetDebugPremium)
            }
        }
    }

    if (showTrackedApps) TrackedAppsDialog(
        selected = trackedApps,
        premium = premium,
        quietHours = quietHours,
        onToggle = onToggleApp,
        onAddApp = onAddApp,
        onToggleQuietExcluded = onSetQuietExcluded,
        onSetPerAppLimit = onSetPerAppLimit,
        onSetPerAppTone = onSetPerAppTone,
        onDismiss = { showTrackedApps = false }
    )

    if (showQuietHours) QuietHoursDialog(
        settings = quietHours,
        onUpdate = onUpdateQuietHours,
        onDismiss = { showQuietHours = false }
    )
}

@Composable
private fun StatusCard(
    status: ScrollStopStatus,
    serviceActive: Boolean,
    notificationGranted: Boolean,
    onOpenSettings: () -> Unit,
    onOpenList: () -> Unit,
    onRefresh: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Status", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "ScrollBeat needs two things to work.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Accessibility service", color = TextPrimary, fontSize = 14.sp)
                Text(
                    if (status == ScrollStopStatus.ENABLED) "On ✓" else "Off",
                    color = if (status == ScrollStopStatus.ENABLED) Accent else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (status == ScrollStopStatus.DISABLED) {
                EnableAccessibilityActions(
                    onOpenSettings = onOpenSettings,
                    onOpenList = onOpenList,
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (status == ScrollStopStatus.ENABLED && !serviceActive) {
                ServiceNotRunningHint(modifier = Modifier.padding(top = 10.dp))
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notifications", color = TextPrimary, fontSize = 14.sp)
                Text(
                    if (notificationGranted) "Allowed ✓" else "Off",
                    color = if (notificationGranted) Accent else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (!notificationGranted) {
                Text(
                    "Reminders need notifications to reach you.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Button(
                    onClick = onRequestNotification,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Allow notifications") }
            }
        }
    }
}

@Composable
private fun ReminderLimitCard(premium: PremiumUiState, onOpenPremium: () -> Unit, onSetLimit: (Int) -> Unit) {
    var customInput by remember { mutableStateOf("") }
    val currentLimit = premium.reminderSettings.reminderLimit
    val parsed = customInput.toIntOrNull()
    val showError = customInput.isNotEmpty() && (parsed == null || parsed <= 0)
    val commitCustom = {
        parsed?.takeIf { it > 0 }?.let(onSetLimit)
        if (customInput.isNotEmpty()) customInput = ""
    }
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Reminder Limit", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (premium.isPremium) {
                Text("$currentLimit scrolls", color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Pick a preset or set your own.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    listOf(50, 100, 250, 500, 1000).forEach { limit ->
                        FilterChip(
                            selected = currentLimit == limit,
                            onClick = { onSetLimit(limit) },
                            label = { Text(limit.toString()) },
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
                    label = { Text("Custom reminder limit") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Enter a positive number.", color = TextSecondary) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitCustom() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            } else {
                Text("$FREE_REMINDER_LIMIT scrolls", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("🔒 Custom reminder limits are available with ScrollBeat Premium.", color = TextSecondary, fontSize = 12.sp)
                Button(
                    onClick = onOpenPremium,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text("Upgrade") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderStyleCard(
    style: ReminderStyle,
    fullScreenAllowed: Boolean,
    onOpenFullScreenSettings: () -> Unit,
    onSetStyle: (ReminderStyle) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Reminder Style", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("How reminders appear on your screen.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                ReminderStyle.entries.forEach { option ->
                    FilterChip(
                        selected = style == option,
                        onClick = { onSetStyle(option) },
                        label = { Text(option.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AccentContainer,
                            labelColor = TextPrimary,
                            selectedContainerColor = Accent,
                            selectedLabelColor = Background
                        )
                    )
                }
            }
            if (style == ReminderStyle.FULL_SCREEN) {
                if (!fullScreenAllowed) {
                    Text(
                        "Full-screen needs permission to cover your screen.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = onOpenFullScreenSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Allow full-screen") }
                }
                Text(
                    "On unlocked screens this appears as a large heads-up.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderCooldownCard(cooldownMinutes: Int, onSetCooldown: (Int) -> Unit) {
    var customInput by remember { mutableStateOf("") }
    val parsed = customInput.toIntOrNull()
    val showError = customInput.isNotEmpty() && (parsed == null || parsed < 0)
    val commitCustom = {
        parsed?.takeIf { it >= 0 }?.let(onSetCooldown)
        if (customInput.isNotEmpty()) customInput = ""
    }
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Reminder Cooldown", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (cooldownMinutes == 0) {
                    "Off — reminders fire on every limit"
                } else {
                    "$cooldownMinutes min between reminders"
                },
                color = Accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "How long ScrollBeat waits between reminders.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                listOf(0, 5, 10, 15, 30, 45, 50).forEach { minutes ->
                    FilterChip(
                        selected = cooldownMinutes == minutes,
                        onClick = { onSetCooldown(minutes) },
                        label = { Text(if (minutes == 0) "Off" else "$minutes min") },
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
                label = { Text("Custom cooldown (minutes, 0 = off)") },
                isError = showError,
                supportingText = if (showError) {
                    { Text("Enter 0 or a positive number.", color = TextSecondary) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustom() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            Text(
                "Tip: the lower the cooldown, the better the app works at breaking the habit.",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun DailySummaryCard(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onSetEnabled: (Boolean) -> Unit,
    onSetTime: (Int, Int) -> Unit
) {
    var pickingTime by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Daily Summary", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "A once-a-day recap of your scrolling.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enabled", color = TextPrimary, fontSize = 14.sp)
                Switch(checked = enabled, onCheckedChange = onSetEnabled)
            }
            TimeRow(
                label = "Time",
                minutes = hour * 60 + minute,
                onClick = { pickingTime = true },
                enabled = enabled
            )
        }
    }

    if (pickingTime) TimePickerDialog(
        initialMinutes = hour * 60 + minute,
        onConfirm = { m -> onSetTime(m / 60, m % 60); pickingTime = false },
        onDismiss = { pickingTime = false }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderToneCard(selected: ReminderTone, onSetTone: (ReminderTone) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Reminder Tone", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Pick a voice for your reminders.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                ReminderTone.entries.forEach { tone ->
                    FilterChip(
                        selected = selected == tone,
                        onClick = { onSetTone(tone) },
                        label = { Text(tone.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AccentContainer,
                            labelColor = TextPrimary,
                            selectedContainerColor = Accent,
                            selectedLabelColor = Background
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeCard(selected: AppTheme, onSetTheme: (AppTheme) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Theme", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Pick an accent colour.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = selected == theme,
                        onClick = { onSetTheme(theme) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(theme.accent.highlight)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(theme.displayName)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AccentContainer,
                            labelColor = TextPrimary,
                            selectedContainerColor = Accent,
                            selectedLabelColor = Background
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackedAppsCard(selected: Set<String>, pausedUntil: Long, onEditApps: () -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pausedUntil) {
        while (pausedUntil > System.currentTimeMillis()) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
        now = System.currentTimeMillis()
    }
    val paused = pausedUntil > now
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Tracked Apps", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("ScrollBeat watches these apps.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            if (paused) {
                Text(
                    "⏸ Tracking paused until ${formatTime(pausedUntil)}",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (selected.isEmpty()) {
                Text("No apps tracked.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    selected.forEach { pkg ->
                        val name = CuratedLibrary.apps.firstOrNull { it.packageName == pkg }?.displayName ?: pkg
                        Surface(
                            color = AccentContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onEditApps,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) { Text("Edit apps") }
        }
    }
}

@Composable
private fun QuietHoursCard(settings: QuietHoursSettings, onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = PanelColor), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Quiet Hours", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (settings.enabled) {
                    "Reminders silenced ${formatMinutes(settings.startMinutes)}–${formatMinutes(settings.endMinutes)}"
                } else {
                    "Reminders run at all times."
                },
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) { Text(if (settings.enabled) "Edit" else "Set up") }
        }
    }
}

@Composable
private fun QuietHoursDialog(
    settings: QuietHoursSettings,
    onUpdate: (QuietHoursSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(settings.enabled) }
    var start by remember { mutableStateOf(settings.startMinutes) }
    var end by remember { mutableStateOf(settings.endMinutes) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quiet Hours") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Silence reminders", color = TextPrimary, fontSize = 14.sp)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                TimeRow(label = "Start", minutes = start, onClick = { pickingStart = true }, enabled = enabled)
                TimeRow(label = "End", minutes = end, onClick = { pickingEnd = true }, enabled = enabled)
                Text(
                    "Scrolling is still counted during quiet hours — only reminders are silenced.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdate(settings.copy(enabled = enabled, startMinutes = start, endMinutes = end))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (pickingStart) TimePickerDialog(
        initialMinutes = start,
        onConfirm = { start = it; pickingStart = false },
        onDismiss = { pickingStart = false }
    )
    if (pickingEnd) TimePickerDialog(
        initialMinutes = end,
        onConfirm = { end = it; pickingEnd = false },
        onDismiss = { pickingEnd = false }
    )
}

@Composable
private fun TimeRow(label: String, minutes: Int, onClick: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .background(AccentContainer.copy(alpha = if (enabled) 1f else 0.4f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = if (enabled) TextPrimary else TextSecondary, fontSize = 14.sp)
        Text(formatMinutes(minutes), color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}

private fun formatTime(epochMillis: Long): String {
    val time = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

@Composable
private fun AppSettingChip(
    label: String,
    value: String,
    overridden: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (overridden) AccentContainer else Color(0xFF1E1E1E))
            .border(
                width = 1.dp,
                color = if (overridden) Accent else TextSecondary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (overridden) "$label $value" else "$label $value · global",
            color = if (overridden) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuietHoursChip(quietFollowed: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (quietFollowed) AccentContainer else Color(0xFF1E1E1E))
            .border(
                width = 1.dp,
                color = if (quietFollowed) Accent else TextSecondary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (quietFollowed) "Quiet hours: on" else "Quiet hours: off",
            color = if (quietFollowed) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackedAppsDialog(
    selected: Set<String>,
    premium: PremiumUiState,
    quietHours: QuietHoursSettings,
    onToggle: (String) -> Unit,
    onAddApp: (String) -> Unit,
    onToggleQuietExcluded: (String) -> Unit,
    onSetPerAppLimit: (String, Int?) -> Unit,
    onSetPerAppTone: (String, ReminderTone?) -> Unit,
    onDismiss: () -> Unit
) {
    var editingLimitFor by remember { mutableStateOf<String?>(null) }
    var editingToneFor by remember { mutableStateOf<String?>(null) }
    var showAddApp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tracked Apps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (premium.isPremium) {
                    Text(
                        "Tap a value to change it for that app.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                CuratedLibrary.apps.forEach { app ->
                    val tracked = app.packageName in selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggle(app.packageName) }
                            .padding(vertical = 3.dp)
                    ) {
                        Checkbox(
                            checked = tracked,
                            onCheckedChange = { onToggle(app.packageName) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.displayName, color = TextPrimary, fontSize = 14.sp)
                            if (premium.isPremium && tracked) {
                                val perAppLimits = premium.reminderSettings.perAppLimits
                                val perAppTones = premium.reminderSettings.perAppTones
                                val effLimit = perAppLimits[app.packageName]
                                    ?: premium.reminderSettings.reminderLimit
                                val hasLimitOverride = perAppLimits.containsKey(app.packageName)
                                val effTone = perAppTones[app.packageName]
                                    ?: premium.reminderTone
                                val hasToneOverride = perAppTones.containsKey(app.packageName)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    AppSettingChip(
                                        label = "Limit",
                                        value = effLimit.toString(),
                                        overridden = hasLimitOverride,
                                        onClick = { editingLimitFor = app.packageName }
                                    )
                                    AppSettingChip(
                                        label = "Tone",
                                        value = effTone.displayName,
                                        overridden = hasToneOverride,
                                        onClick = { editingToneFor = app.packageName }
                                    )
                                    QuietHoursChip(
                                        quietFollowed = app.packageName !in quietHours.quietExcludedApps,
                                        onClick = { onToggleQuietExcluded(app.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (premium.isPremium) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showAddApp = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("+ Add any app") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )

    if (editingLimitFor != null) LimitPickerDialog(
        appName = CuratedLibrary.apps.firstOrNull { it.packageName == editingLimitFor }?.displayName ?: editingLimitFor!!,
        currentLimit = premium.reminderSettings.perAppLimits[editingLimitFor],
        globalLimit = premium.reminderSettings.reminderLimit,
        onSet = { onSetPerAppLimit(editingLimitFor!!, it); editingLimitFor = null },
        onDismiss = { editingLimitFor = null }
    )

    if (editingToneFor != null) TonePickerDialog(
        appName = CuratedLibrary.apps.firstOrNull { it.packageName == editingToneFor }?.displayName ?: editingToneFor!!,
        currentTone = premium.reminderSettings.perAppTones[editingToneFor],
        globalTone = premium.reminderTone,
        onSet = { onSetPerAppTone(editingToneFor!!, it); editingToneFor = null },
        onDismiss = { editingToneFor = null }
    )

    if (showAddApp) InstalledAppsDialog(
        selected = selected,
        onAdd = { onAddApp(it); showAddApp = false },
        onDismiss = { showAddApp = false }
    )
}

@Composable
private fun LimitPickerDialog(
    appName: String,
    currentLimit: Int?,
    globalLimit: Int,
    onSet: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val parsed = input.toIntOrNull()
    val showError = input.isNotEmpty() && (parsed == null || parsed <= 0)
    val commit = {
        parsed?.takeIf { it > 0 }?.let(onSet)
        if (input.isNotEmpty()) input = ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Limit for $appName") },
        text = {
            Column {
                Text("Current: ${currentLimit ?: "global ($globalLimit)"} scrolls", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(50, 100, 250, 500).forEach { limit ->
                        FilterChip(
                            selected = currentLimit == limit,
                            onClick = { onSet(limit) },
                            label = { Text(limit.toString()) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AccentContainer, labelColor = TextPrimary,
                                selectedContainerColor = Accent, selectedLabelColor = Background
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = input, onValueChange = { input = it.filter(Char::isDigit) },
                    label = { Text("Custom") }, isError = showError,
                    supportingText = if (showError) { { Text("Enter a positive number.", color = TextSecondary) } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                if (currentLimit != null) {
                    Button(
                        onClick = { onSet(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentContainer, contentColor = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Use global") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun TonePickerDialog(
    appName: String,
    currentTone: ReminderTone?,
    globalTone: ReminderTone,
    onSet: (ReminderTone?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tone for $appName") },
        text = {
            Column {
                ReminderTone.entries.forEach { tone ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { onSet(tone); onDismiss() }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = currentTone == tone,
                            onClick = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(tone.displayName, color = TextPrimary, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { onSet(null); onDismiss() }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = currentTone == null,
                        onClick = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Use global (${globalTone.displayName})", color = TextSecondary, fontSize = 14.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun InstalledAppsDialog(selected: Set<String>, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val apps = remember { getInstalledApps(context) }.filter { it.packageName !in selected }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add any app") },
        text = {
            if (apps.isEmpty()) {
                Text("No new apps available.", color = TextSecondary, fontSize = 14.sp)
            } else {
                Column {
                    apps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable { onAdd(app.packageName) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(app.displayName, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
