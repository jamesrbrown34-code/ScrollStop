package com.scrollstop.app.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scrollstop.app.R
import com.scrollstop.app.data.ReminderStyleGraph
import com.scrollstop.app.premium.AppTheme
import com.scrollstop.app.premium.PremiumGraph
import com.scrollstop.app.ui.MainActivity

object InterventionManager {
    private const val CHANNEL_ID = "doomscroll_alerts"
    private const val CHANNEL_NAME = "ScrollBeat Reminders"
    private const val NOTIFICATION_ID = 10101

    fun trigger(
        context: Context,
        packageName: String,
        level: AwarenessLevel,
        title: String,
        summary: String,
        message: String
    ) {
        createChannel(context)

        Log.w("DoomscrollDetector", "[$packageName] $level: $title — $message")

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_scrollstop)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(accentColor(context))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val style = ReminderStyleGraph.repository(context).style.value
        val fullScreen = style == ReminderStyle.FULL_SCREEN && FullScreenIntent.canUse(context)
        val category = if (fullScreen) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER
        builder
            .setCategory(category)
            .setPriority(toNotificationPriority(level))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_pause, "Pause 30 min", pauseIntent(context, 30, PAUSE_30_REQUEST))
            .addAction(android.R.drawable.ic_media_pause, "Pause 1 hr", pauseIntent(context, 60, PAUSE_60_REQUEST))

        if (fullScreen) {
            builder.setFullScreenIntent(contentIntent, true)
        }

        if (notificationsAllowed(context)) {
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call — skip the reminder.
            }
        }
        vibrate(context)
    }

    /** On Android 13+ reminders need the notification permission; below that it's granted by default. */
    private fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** The applied theme accent, mirroring the in-app colour for premium themes and white for free. */
    private fun accentColor(context: Context): Int = when (PremiumGraph.manager(context).state.value.theme) {
        AppTheme.MONOCHROME -> Color.WHITE
        AppTheme.FOREST -> Color.rgb(0x9E, 0xD9, 0xBA)
        AppTheme.OCEAN -> Color.rgb(0x9E, 0xC9, 0xE0)
        AppTheme.EMBER -> Color.rgb(0xE8, 0xC8, 0x9A)
    }

    private fun pauseIntent(context: Context, minutes: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, PauseReceiver::class.java).apply {
            action = PauseReceiver.ACTION
            putExtra(PauseReceiver.EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val PAUSE_30_REQUEST = 1
    private const val PAUSE_60_REQUEST = 2

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
            channel.description = "Scroll reminders"
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
