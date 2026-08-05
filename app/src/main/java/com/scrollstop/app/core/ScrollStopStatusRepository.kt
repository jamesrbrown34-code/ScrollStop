package com.scrollstop.app.core

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.scrollstop.app.service.ScrollMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScrollStopStatus {
    ENABLED,
    DISABLED,
    PERMISSION_REQUIRED
}

/** Single source of truth for whether the app can monitor scrolling. */
class ScrollStopStatusRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var settleJob: Job? = null

    private val _status = MutableStateFlow(currentStatus())
    val status: StateFlow<ScrollStopStatus> = _status.asStateFlow()

    /** True when the service is actually bound/running, not just enabled in settings. */
    private val _serviceActive = MutableStateFlow(isServiceBound())
    val serviceActive: StateFlow<Boolean> = _serviceActive.asStateFlow()

    fun refresh() {
        _status.value = currentStatus()
        _serviceActive.value = isServiceBound()
    }

    /**
     * Re-checks repeatedly for a short window after returning from system settings, so slow
     * OEMs (MIUI, older One UI) have time to persist the toggle before we trust a negative read.
     */
    fun beginSettlePoll() {
        if (settleJob?.isActive == true) return
        settleJob = scope.launch {
            repeat(POLLS_PER_SETTLE) {
                refresh()
                if (_status.value == ScrollStopStatus.ENABLED && _serviceActive.value) return@launch
                delay(POLL_INTERVAL_MS)
            }
            refresh()
        }
    }

    private fun currentStatus(): ScrollStopStatus {
        if (!isAccessibilityEnabled()) return ScrollStopStatus.DISABLED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return ScrollStopStatus.PERMISSION_REQUIRED
        }
        return ScrollStopStatus.ENABLED
    }

    /** The settings flag is the gate; the bound-services signal confirms it's actually running. */
    private fun isAccessibilityEnabled(): Boolean = isSettingsFlagSet() || isServiceBound()

    private fun isSettingsFlagSet(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val serviceName = ComponentName(context, ScrollMonitorService::class.java).flattenToString()
        return enabledServices.split(':').any { it.equals(serviceName, ignoreCase = true) }
    }

    private fun isServiceBound(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val component = ComponentName(context, ScrollMonitorService::class.java).flattenToString()
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val serviceInfo = info.resolveInfo?.serviceInfo ?: return@any false
                ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToString() == component
            }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 500L
        const val POLLS_PER_SETTLE = 16
    }
}

object ScrollStopStatusGraph {
    @Volatile private var repository: ScrollStopStatusRepository? = null

    fun repository(context: Context): ScrollStopStatusRepository = synchronized(this) {
        val existing = repository
        if (existing != null) {
            existing
        } else {
            ScrollStopStatusRepository(context.applicationContext).also { repository = it }
        }
    }
}
