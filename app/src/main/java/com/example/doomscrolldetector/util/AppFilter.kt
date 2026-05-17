package com.example.doomscrolldetector.util

object AppFilter {
    private val targetPackages = setOf(
        "com.zhiliaoapp.musically", // TikTok
        "com.instagram.android",    // Instagram
        "com.google.android.youtube", // YouTube
        "com.reddit.frontpage",     // Reddit
        "com.twitter.android",      // X
        "com.xcorp.android"         // X alternative package (future-safe)
    )

    fun shouldTrack(packageName: String?): Boolean {
        return packageName != null && targetPackages.contains(packageName)
    }
}
