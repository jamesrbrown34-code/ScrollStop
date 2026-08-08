package com.scrollstop.app.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scrollstop.app.data.rescheduleSummary

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleSummary(context)
        }
    }
}
