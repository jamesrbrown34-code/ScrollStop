package com.example.doomscrolldetector.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.doomscrolldetector.R
import com.example.doomscrolldetector.ui.MainActivity

object InterventionManager {
    private const val CHANNEL_ID = "doomscroll_alerts"
    private const val CHANNEL_NAME = "Doomscroll Alerts"
    private const val NOTIFICATION_ID = 10101

    fun trigger(context: Context, packageName: String, awarenessState: AwarenessState) {
        val message = awarenessState.message ?: return
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
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        vibrate(context)
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
