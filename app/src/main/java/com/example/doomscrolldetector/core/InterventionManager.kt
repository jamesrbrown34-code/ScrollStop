package com.example.doomscrolldetector.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.doomscrolldetector.R
import com.example.doomscrolldetector.service.InterventionActionReceiver
import com.example.doomscrolldetector.ui.MainActivity

object InterventionManager {
    const val ACTION_SNOOZE_INTERVENTIONS = "com.example.doomscrolldetector.action.SNOOZE_INTERVENTIONS"

    private const val CHANNEL_ID = "doomscroll_alerts"
    private const val CHANNEL_NAME = "Doomscroll Alerts"
    private const val NOTIFICATION_ID = 10101
    private const val PREFS_NAME = "intervention_preferences"
    private const val KEY_SNOOZE_UNTIL_MS = "snooze_until_ms"
    private const val SNOOZE_DURATION_MS = 15 * 60 * 1000L

    fun trigger(context: Context, packageName: String, awarenessState: AwarenessState) {
        val message = awarenessState.message ?: return
        if (isSnoozed(context)) return

        createChannel(context)

        Log.w("DoomscrollDetector", "[$packageName] ${awarenessState.level}: $message")

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, InterventionActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_INTERVENTIONS
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(toNotificationPriority(awarenessState.level))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                context.getString(R.string.snooze_interventions_15_min),
                snoozePendingIntent
            )
            .setAutoCancel(true)
            .build()

        if (canPostNotifications(context)) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
        vibrate(context)
    }

    fun snooze(context: Context, currentTimeMs: Long = System.currentTimeMillis()) {
        val snoozeUntilMs = currentTimeMs + SNOOZE_DURATION_MS
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_SNOOZE_UNTIL_MS, snoozeUntilMs)
            .apply()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun isSnoozed(context: Context, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        val snoozeUntilMs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_SNOOZE_UNTIL_MS, 0L)
        return currentTimeMs < snoozeUntilMs
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun toNotificationPriority(level: AwarenessLevel): Int {
        return when (level) {
            AwarenessLevel.NORMAL -> NotificationCompat.PRIORITY_LOW
            AwarenessLevel.NOTICE -> NotificationCompat.PRIORITY_DEFAULT
            AwarenessLevel.WARNING -> NotificationCompat.PRIORITY_HIGH
            AwarenessLevel.CRITICAL -> NotificationCompat.PRIORITY_MAX
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Interventions for mindless scrolling"
            channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 200, 120, 200)
            manager.createNotificationChannel(channel)
        }
    }

    private fun vibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(350)
        }
    }
}
