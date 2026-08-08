package com.scrollstop.app.premium

import android.app.Activity
import android.content.Context
import com.scrollstop.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PremiumUiState(
    val isPremium: Boolean = false,
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val reminderTone: ReminderTone = ReminderTone.GENERIC,
    val theme: AppTheme = AppTheme.MONOCHROME,
    val billing: BillingState = BillingState()
)

class PremiumManager(context: Context) {
    private val appContext = context.applicationContext
    private val billing = BillingRepository(appContext)
    private val settings = ReminderSettingsRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val debugPrefs = appContext.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
    private val _debugPremiumEnabled = MutableStateFlow(debugPrefs.getBoolean(DEBUG_PREMIUM_KEY, false))
    val debugPremiumEnabled: StateFlow<Boolean> = _debugPremiumEnabled.asStateFlow()
    val state: StateFlow<PremiumUiState> = combine(
        billing.state,
        settings.settings,
        _debugPremiumEnabled
    ) { billingState, reminder, debugEnabled ->
        val isPremium = if (BuildConfig.DEBUG) debugEnabled else billingState.isPremium
        PremiumUiState(
            isPremium,
            if (isPremium) reminder else ReminderSettings(),
            if (isPremium) reminder.reminderTone else ReminderTone.GENERIC,
            if (isPremium) reminder.theme else AppTheme.MONOCHROME,
            billingState
        )
    }.stateIn(scope, SharingStarted.Eagerly, PremiumUiState())

    init { billing.connect() }
    fun refreshPremiumStatus() = billing.refreshPurchases()
    fun buy(activity: Activity, productId: String, preferTrial: Boolean = false) = billing.launchPurchase(activity, productId, preferTrial)
    fun setReminderLimit(limit: Int) {
        if (!state.value.isPremium) return
        scope.launch { settings.setReminderLimit(limit) }
    }
    fun setReminderTone(tone: ReminderTone) {
        if (!state.value.isPremium) return
        scope.launch { settings.setReminderTone(tone) }
    }
    fun setPerAppLimit(packageName: String, limit: Int?) {
        if (!state.value.isPremium) return
        scope.launch { settings.setPerAppLimit(packageName, limit) }
    }
    fun setPerAppTone(packageName: String, tone: ReminderTone?) {
        if (!state.value.isPremium) return
        scope.launch { settings.setPerAppTone(packageName, tone) }
    }
    fun setTheme(theme: AppTheme) {
        if (!state.value.isPremium) return
        scope.launch { settings.setTheme(theme) }
    }
    fun setDebugPremiumEnabled(enabled: Boolean) {
        _debugPremiumEnabled.value = enabled
        debugPrefs.edit().putBoolean(DEBUG_PREMIUM_KEY, enabled).apply()
    }

    private companion object {
        const val DEBUG_PREMIUM_KEY = "debug_premium_enabled"
    }
}

object PremiumGraph {
    @Volatile private var manager: PremiumManager? = null
    fun manager(context: Context): PremiumManager = synchronized(this) {
        manager ?: PremiumManager(context.applicationContext).also { manager = it }
    }
}
