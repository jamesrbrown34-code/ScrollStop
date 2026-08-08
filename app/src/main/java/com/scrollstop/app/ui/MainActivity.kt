package com.scrollstop.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scrollstop.app.core.EnableAccessibility
import com.scrollstop.app.core.FullScreenIntent
import com.scrollstop.app.core.ScrollTracker
import com.scrollstop.app.core.ScrollStopStatusGraph
import com.scrollstop.app.data.AnalyticsGraph
import com.scrollstop.app.data.DailySummaryGraph
import com.scrollstop.app.data.NudgeGraph
import com.scrollstop.app.data.PauseGraph
import com.scrollstop.app.data.QuietHoursGraph
import com.scrollstop.app.data.ReminderCooldownGraph
import com.scrollstop.app.data.ReminderStyleGraph
import com.scrollstop.app.data.TodayGoalGraph
import com.scrollstop.app.data.TrackedAppsGraph
import com.scrollstop.app.premium.AppTheme
import com.scrollstop.app.premium.PremiumGraph
import kotlinx.coroutines.delay

private enum class MainTab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.Home),
    PROGRESS("Progress", Icons.Filled.DateRange),
    SETTINGS("Settings", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = AppColorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    ScrollStopApp(
                        onOpenServiceSettings = { EnableAccessibility.openServiceSettings(this) },
                        onOpenAccessibilityList = { EnableAccessibility.openAccessibilityList(this) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollStopApp(
    onOpenServiceSettings: () -> Unit,
    onOpenAccessibilityList: () -> Unit
) {
    val context = LocalContext.current
    val statusRepository = remember(context) { ScrollStopStatusGraph.repository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("onboarding", Context.MODE_PRIVATE) }
    var showOnboarding by rememberSaveable { mutableStateOf(!prefs.getBoolean(ONBOARDING_COMPLETE_KEY, false)) }

    DisposableEffect(lifecycleOwner, statusRepository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                statusRepository.refresh()
                statusRepository.beginSettlePoll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showOnboarding) {
        OnboardingScreen(
            onOpenServiceSettings = onOpenServiceSettings,
            onOpenAccessibilityList = onOpenAccessibilityList,
            onFinish = {
                prefs.edit().putBoolean(ONBOARDING_COMPLETE_KEY, true).apply()
                showOnboarding = false
            }
        )
    } else {
        MainScreen(
            onOpenAccessibilitySettings = onOpenServiceSettings,
            onOpenAccessibilityList = onOpenAccessibilityList
        )
    }
}

private const val ONBOARDING_COMPLETE_KEY = "complete"

@Composable
private fun MainScreen(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAccessibilityList: () -> Unit
) {
    val context = LocalContext.current
    val dashboardRepository = remember(context) { AnalyticsGraph.dashboard(context) }
    val dashboard by dashboardRepository.snapshot.collectAsState()
    val statusRepository = remember(context) { ScrollStopStatusGraph.repository(context) }
    val status by statusRepository.status.collectAsState()
    val serviceActive by statusRepository.serviceActive.collectAsState()
    val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val premiumManager = remember(context) { PremiumGraph.manager(context) }
    val premium by premiumManager.state.collectAsState()
    val debugPremiumEnabled by premiumManager.debugPremiumEnabled.collectAsState()

    dashboardRepository.isPremium = premium.isPremium
    val trackedAppsRepository = remember(context) { TrackedAppsGraph.repository(context) }
    val trackedApps by trackedAppsRepository.selected.collectAsState()
    val quietHoursRepository = remember(context) { QuietHoursGraph.repository(context) }
    val quietHours by quietHoursRepository.settings.collectAsState()
    val pauseRepository = remember(context) { PauseGraph.repository(context) }
    val pausedUntil by pauseRepository.pausedUntil.collectAsState()
    val todayGoalRepository = remember(context) { TodayGoalGraph.repository(context) }
    val todayGoalSettings by todayGoalRepository.settings.collectAsState()
    val todayGoal = if (premium.isPremium) {
        todayGoalSettings.effectiveGoal(java.time.LocalDate.now())
    } else {
        todayGoalSettings.default
    }
    val reminderStyleRepository = remember(context) { ReminderStyleGraph.repository(context) }
    val reminderStyle by reminderStyleRepository.style.collectAsState()
    val cooldownRepository = remember(context) { ReminderCooldownGraph.repository(context) }
    val cooldownMinutes by cooldownRepository.cooldownMinutes.collectAsState()
    val dailySummaryRepository = remember(context) { DailySummaryGraph.repository(context) }
    val dailySummary by dailySummaryRepository.settings.collectAsState()
    val nudgeRepository = remember(context) { NudgeGraph.repository(context) }
    val latestNudge by nudgeRepository.lastMessage.collectAsState()
    val lastFallback by nudgeRepository.lastFallback.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { statusRepository.refresh() }

    var fullScreenAllowed by remember { mutableStateOf(FullScreenIntent.canUse(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            ScrollTracker.onIdleCheck(System.currentTimeMillis())
            dashboardRepository.refresh()
            fullScreenAllowed = FullScreenIntent.canUse(context)
            delay(1_000)
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.TODAY) }
    var showPremium by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(premium.isPremium, premium.theme) {
        val activeTheme = if (premium.isPremium) premium.theme else AppTheme.MONOCHROME
        applyAccent(activeTheme.accent)
    }

    if (showPremium) {
        BackHandler { showPremium = false }
        PremiumPaywallPage(
            billing = premium.billing,
            onClose = { showPremium = false },
            onPurchase = { productId ->
                (context as? Activity)?.let { premiumManager.buy(it, productId, preferTrial = true) }
            },
            onRestore = premiumManager::refreshPremiumStatus
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)) {
                Text("SCROLLSTOP", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("A calmer view of your scrolling", color = TextSecondary, fontSize = 14.sp)
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    MainTab.TODAY -> TodayTab(
                        dashboard = dashboard,
                        todayGoal = todayGoal,
                        perDayGoals = todayGoalSettings.perDay,
                        latestNudge = latestNudge,
                        reminderTone = premium.reminderTone,
                        lastFallback = lastFallback,
                        onSetGoal = todayGoalRepository::setGoal,
                        onSetPerDayGoal = todayGoalRepository::setPerDayGoal,
                        onPersistFallback = nudgeRepository::setLastFallback,
                        status = status,
                        isPremium = premium.isPremium,
                        onOpenPremium = { showPremium = true },
                        onEnable = onOpenAccessibilitySettings,
                        onOpenAccessibilityList = onOpenAccessibilityList,
                        onRefreshStatus = statusRepository::refresh,
                        onRequestNotificationPermission = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                    MainTab.PROGRESS -> ProgressTab(
                        dashboard = dashboard,
                        isPremium = premium.isPremium
                    )
                    MainTab.SETTINGS -> SettingsTab(
                        premium = premium,
                        trackedApps = trackedApps,
                        pausedUntil = pausedUntil,
                        quietHours = quietHours,
                        debugPremiumEnabled = debugPremiumEnabled,
                        reminderStyle = reminderStyle,
                        fullScreenAllowed = fullScreenAllowed,
                        cooldownMinutes = cooldownMinutes,
                        status = status,
                        serviceActive = serviceActive,
                        notificationGranted = notificationGranted,
                        onOpenPremium = { showPremium = true },
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onOpenAccessibilityList = onOpenAccessibilityList,
                        onRefreshStatus = statusRepository::refresh,
                        onRequestNotificationPermission = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onSetReminderStyle = reminderStyleRepository::setStyle,
                        onOpenFullScreenSettings = { FullScreenIntent.openPermissionSettings(context) },
                        onSetCooldown = cooldownRepository::setCooldown,
                        onSetLimit = premiumManager::setReminderLimit,
                        onSetTone = premiumManager::setReminderTone,
                        onSetTheme = premiumManager::setTheme,
                        onUpdateQuietHours = quietHoursRepository::update,
                        onSetQuietExcluded = quietHoursRepository::toggleQuietExcluded,
                        dailySummaryEnabled = dailySummary.enabled,
                        dailySummaryHour = dailySummary.hour,
                        dailySummaryMinute = dailySummary.minute,
                        onSetDailySummaryEnabled = dailySummaryRepository::setEnabled,
                        onSetDailySummaryTime = dailySummaryRepository::setTime,
                        onToggleApp = trackedAppsRepository::toggle,
                        onAddApp = trackedAppsRepository::addApp,
                        onSetPerAppLimit = premiumManager::setPerAppLimit,
                        onSetPerAppTone = premiumManager::setPerAppTone,
                        onSetDebugPremium = premiumManager::setDebugPremiumEnabled
                    )
                }
            }

            NavigationBar(containerColor = PanelColor) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    }
}
