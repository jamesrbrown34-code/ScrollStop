package com.scrollstop.app.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scrollstop.app.R
import com.scrollstop.app.data.AnalyticsGraph
import com.scrollstop.app.data.CuratedLibrary
import com.scrollstop.app.premium.PremiumGraph
import com.scrollstop.app.ui.MainActivity
import java.text.NumberFormat
import java.time.LocalDate

class SummaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        if (!PremiumGraph.manager(context).state.value.isPremium) return

        val analytics = AnalyticsGraph.scrolls(context)
        val today = LocalDate.now()
        val event = analytics.events.value.firstOrNull { it.date == today }
        val scrolls = event?.scrolls ?: 0
        val reminders = event?.interruptionsShown ?: 0
        val topApp = analytics.appEvents.value
            .filter { it.date == today }
            .maxByOrNull { it.scrolls }

        createChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (scrolls == 0) {
            "No scrolling today — a clean day."
        } else {
            buildString {
                append("You scrolled ${NumberFormat.getIntegerInstance().format(scrolls)} times")
                if (reminders > 0) append(". Reminders: $reminders")
                topApp?.let { append(". Top app: ${it.packageName.toDisplayName()}") }
                append(".")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_scrollstop)
            .setContentTitle("Your day at a glance")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        if (notificationsAllowed(context)) {
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Permission revoked between check and post.
            }
        }
    }

    private fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Daily Summary",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "A daily summary of your scrolling"
                }
            )
        }
    }

    private fun String.toDisplayName(): String =
        CuratedLibrary.apps.firstOrNull { it.packageName == this }?.displayName ?: this

    companion object {
        const val ACTION = "com.scrollstop.app.DAILY_SUMMARY"
        private const val CHANNEL_ID = "daily_summary"
        private const val NOTIFICATION_ID = 20202
    }
}
