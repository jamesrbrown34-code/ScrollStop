package com.scrollstop.app.core

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build

object FullScreenIntent {
    /** True when the OS will actually honour a full-screen intent from this app. */
    fun canUse(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
        }
        return true
    }

    /** Deep-links to the full-screen-intent grant toggle (Android 14+). No-op where it doesn't exist. */
    fun openPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        try {
            context.startActivity(
                Intent(MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    putExtra(EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        } catch (_: Exception) {
            // OEM without this settings screen — nothing to do.
        }
    }

    private const val MANAGE_APP_USE_FULL_SCREEN_INTENT = "android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT"
    private const val EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE"
}
