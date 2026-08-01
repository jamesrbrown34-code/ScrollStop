package com.example.doomscrolldetector.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.doomscrolldetector.core.InterventionManager

class InterventionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == InterventionManager.ACTION_SNOOZE_INTERVENTIONS) {
            InterventionManager.snooze(context)
        }
    }
}
