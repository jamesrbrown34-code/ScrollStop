package com.scrollstop.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrollstop.app.core.ScrollStopStatus
import com.scrollstop.app.core.ScrollStopStatusGraph
import com.scrollstop.app.data.CuratedLibrary
import com.scrollstop.app.data.TodayGoalGraph
import com.scrollstop.app.data.TrackedAppsGraph

private enum class OnboardingStep(val title: String) {
    WELCOME("Welcome"),
    PERMISSIONS("Enable ScrollBeat"),
    APPS("Tracked apps"),
    GOAL("Today's goal")
}

@Composable
internal fun OnboardingScreen(
    onOpenServiceSettings: () -> Unit,
    onOpenAccessibilityList: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val trackedAppsRepository = remember(context) { TrackedAppsGraph.repository(context) }
    val trackedApps by trackedAppsRepository.selected.collectAsState()
    val goalRepository = remember(context) { TodayGoalGraph.repository(context) }
    val goal by goalRepository.goal.collectAsState()
    val statusRepository = remember(context) { ScrollStopStatusGraph.repository(context) }
    val status by statusRepository.status.collectAsState()
    val serviceActive by statusRepository.serviceActive.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { statusRepository.refresh() }

    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val steps = OnboardingStep.entries
    val stepIndex = steps.indexOf(step)

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LinearProgressIndicator(
                    progress = { (stepIndex + 1).toFloat() / steps.size },
                    color = Accent,
                    trackColor = AccentContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onFinish) { Text("Skip", color = TextSecondary, fontSize = 13.sp) }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp)
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.PERMISSIONS -> PermissionsStep(
                        status = status,
                        serviceActive = serviceActive,
                        onOpenServiceSettings = onOpenServiceSettings,
                        onOpenAccessibilityList = onOpenAccessibilityList,
                        onRefreshStatus = statusRepository::refresh,
                        onRequestNotificationPermission = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                    OnboardingStep.APPS -> AppsStep(
                        selected = trackedApps,
                        onToggle = trackedAppsRepository::toggle
                    )
                    OnboardingStep.GOAL -> GoalStep(goal = goal, onSetGoal = goalRepository::setGoal)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step != OnboardingStep.WELCOME) {
                    TextButton(onClick = { step = steps[stepIndex - 1] }) { Text("Back", color = TextSecondary) }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (step == OnboardingStep.GOAL) onFinish() else step = steps[stepIndex + 1]
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (step == OnboardingStep.GOAL) "Get started" else "Next", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column {
        Text("ScrollBeat", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(
            "Beat the doomscroll.",
            color = TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "ScrollBeat watches the apps you scroll through, nudges you when you've had enough, and shows you how your time is really spent.",
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            "It takes about a minute to set up.",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun PermissionsStep(
    status: ScrollStopStatus,
    serviceActive: Boolean,
    onOpenServiceSettings: () -> Unit,
    onOpenAccessibilityList: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Column {
        Text(
            "ScrollBeat runs in the background",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "To detect scrolling, ScrollBeat needs the accessibility permission and notification access.",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(Modifier.height(24.dp))
        when (status) {
            ScrollStopStatus.ENABLED -> {
                Text("ScrollBeat is active ✓", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (!serviceActive) {
                    ServiceNotRunningHint(modifier = Modifier.padding(top = 8.dp))
                }
            }
            ScrollStopStatus.DISABLED -> {
                EnableAccessibilityActions(
                    onOpenSettings = onOpenServiceSettings,
                    onOpenList = onOpenAccessibilityList,
                    onRefresh = onRefreshStatus
                )
            }
            ScrollStopStatus.PERMISSION_REQUIRED -> {
                Button(
                    onClick = onRequestNotificationPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Allow notifications", fontWeight = FontWeight.Bold) }
                Text(
                    "ScrollBeat needs notifications to deliver its reminders.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AppsStep(selected: Set<String>, onToggle: (String) -> Unit) {
    Column {
        Text(
            "Which apps do you scroll?",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Pick the ones you want ScrollBeat to watch. You can change this any time.",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Spacer(Modifier.height(12.dp))
        CuratedLibrary.apps.forEach { app ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle(app.packageName) }
                    .padding(vertical = 3.dp)
            ) {
                Checkbox(
                    checked = app.packageName in selected,
                    onCheckedChange = { onToggle(app.packageName) }
                )
                Text(app.displayName, color = TextPrimary, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalStep(goal: Int, onSetGoal: (Int) -> Unit) {
    var customInput by remember { mutableStateOf("") }
    val parsed = customInput.toIntOrNull()
    val showError = customInput.isNotEmpty() && (parsed == null || parsed <= 0)
    val commitCustom = {
        parsed?.takeIf { it > 0 }?.let(onSetGoal)
        if (customInput.isNotEmpty()) customInput = ""
    }
    Column {
        Text(
            "Set a daily goal (optional)",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Try to stay under this many scrolls each day. You can change it later.",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}
