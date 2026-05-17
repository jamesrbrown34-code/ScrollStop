package com.example.doomscrolldetector.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.doomscrolldetector.core.ScrollTracker
import com.example.doomscrolldetector.util.AppFilter

class ScrollMonitorService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (!AppFilter.shouldTrack(packageName)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                ScrollTracker.onAppChanged(packageName, event.eventTime)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                ScrollTracker.onScrollEvent(applicationContext, packageName, event.eventTime)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        ScrollTracker.resetToIdle()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        ScrollTracker.resetToIdle()
        return super.onUnbind(intent)
    }
}
