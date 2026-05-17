package com.example.doomscrolldetector.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.doomscrolldetector.R

object InterventionManager {
    private const val CHANNEL_ID = "doomscroll_alerts"
    private const val CHANNEL_NAME = "Doomscroll Alerts"
    private const val NOTIFICATION_ID = 10101

    fun trigger(context: Context, packageName: String, awarenessState: AwarenessState) {
        val message = awarenessState.message ?: return
        createChannel(context)

        Log.w("DoomscrollDetector", "[$packageName] ${awarenessState.level}: $message")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(toNotificationPriority(awarenessState.level))
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
