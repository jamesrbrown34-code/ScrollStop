package com.scrollstop.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.scrollstop.app.R
import org.json.JSONArray
import java.time.LocalDate

class ScrollStopWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_scrollstop)
            views.setTextViewText(R.id.scrolls_text, formatScrolls(readTodayScrolls(context)))
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.scrolls_text, pending)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun readTodayScrolls(context: Context): Int {
        val prefs = context.getSharedPreferences("scroll_analytics", Context.MODE_PRIVATE)
        val json = prefs.getString("daily_scroll_events", null) ?: return 0
        val today = LocalDate.now().toString()
        return runCatching {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                if (item.getString("date") == today) return item.getInt("scrolls")
            }
            0
        }.getOrDefault(0)
    }

    private fun formatScrolls(count: Int): String {
        val nf = java.text.NumberFormat.getIntegerInstance()
        return "${nf.format(count)} scrolls today"
    }
}
