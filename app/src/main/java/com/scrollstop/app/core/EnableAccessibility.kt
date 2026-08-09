package com.scrollstop.app.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.scrollstop.app.service.ScrollMonitorService

object EnableAccessibility {
    /**
     * Opens ScrollBeat's own accessibility toggle (one flip) on Android 10+, falling back to
     * the full accessibility list where the deep link isn't available or the OEM breaks it.
     */
    fun openServiceSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.startActivity(
                    Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
                        putExtra(EXTRA_COMPONENT_NAME, ComponentName(context, ScrollMonitorService::class.java))
                    }
                )
                return
            } catch (_: Exception) {
                // OEM without a details screen for this service — fall through to the list.
            }
        }
        openAccessibilityList(context)
    }

    /** Opens the full accessibility settings list (the "still can't find it" fallback). */
    fun openAccessibilityList(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
    private const val EXTRA_COMPONENT_NAME = "android.intent.extra.COMPONENT_NAME"
}
