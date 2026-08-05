package com.scrollstop.app.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scrollstop.app.data.PauseGraph

class PauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 30)
        PauseGraph.repository(context).pauseFor(minutes)
    }

    companion object {
        const val ACTION = "com.scrollstop.app.PAUSE_TRACKING"
        const val EXTRA_MINUTES = "pause_minutes"
    }
}
