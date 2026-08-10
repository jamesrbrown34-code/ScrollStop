package com.scrollstop.app.core

/** How a scroll reminder is delivered on screen. Free setting, global. */
enum class ReminderStyle(val displayName: String) {
    HEADS_UP("Heads-up"),
    FULL_SCREEN("Full-screen")
}
