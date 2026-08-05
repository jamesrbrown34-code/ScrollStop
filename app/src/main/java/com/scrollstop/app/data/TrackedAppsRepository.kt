package com.scrollstop.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrackedApp(val packageName: String, val displayName: String)

/** The curated library of known scrolling apps free users can opt into. */
object CuratedLibrary {
    val apps: List<TrackedApp> = listOf(
        TrackedApp("com.zhiliaoapp.musically", "TikTok"),
        TrackedApp("com.instagram.android", "Instagram"),
        TrackedApp("com.google.android.youtube", "YouTube"),
        TrackedApp("com.reddit.frontpage", "Reddit"),
        TrackedApp("com.twitter.android", "X"),
        TrackedApp("com.xcorp.android", "X (alt)"),
        TrackedApp("com.facebook.katana", "Facebook"),
        TrackedApp("com.threads.android", "Threads"),
        TrackedApp("com.snapchat.android", "Snapchat"),
        TrackedApp("com.pinterest", "Pinterest"),
        TrackedApp("com.linkedin.android", "LinkedIn"),
        TrackedApp("com.quora.android", "Quora"),
        TrackedApp("com.ninegag.android.app", "9GAG"),
        TrackedApp("com.imgur.mobile", "Imgur")
    )

    /** The set selected for a fresh install; the app works immediately with these. */
    val defaultPackages: Set<String> = setOf(
        "com.zhiliaoapp.musically",
        "com.instagram.android",
        "com.google.android.youtube",
        "com.reddit.frontpage",
        "com.twitter.android",
        "com.xcorp.android"
    )
}

private val Context.trackedAppsDataStore by preferencesDataStore("tracked_apps")

class TrackedAppsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = stringSetPreferencesKey("tracked_packages")
    val selected: StateFlow<Set<String>> = appContext.trackedAppsDataStore.data
        .map { it[key] ?: CuratedLibrary.defaultPackages }
        .stateIn(scope, SharingStarted.Eagerly, CuratedLibrary.defaultPackages)

    fun isTracked(packageName: String): Boolean = packageName in selected.value

    fun toggle(packageName: String) {
        scope.launch {
            appContext.trackedAppsDataStore.edit { prefs ->
                val current = prefs[key] ?: CuratedLibrary.defaultPackages
                prefs[key] = if (packageName in current) current - packageName else current + packageName
            }
        }
    }

    fun addApp(packageName: String) {
        scope.launch {
            appContext.trackedAppsDataStore.edit { prefs ->
                val current = prefs[key] ?: CuratedLibrary.defaultPackages
                prefs[key] = current + packageName
            }
        }
    }
}

object TrackedAppsGraph {
    @Volatile private var repository: TrackedAppsRepository? = null
    fun repository(context: Context): TrackedAppsRepository = synchronized(this) {
        repository ?: TrackedAppsRepository(context.applicationContext).also { repository = it }
    }
}

fun getInstalledApps(context: Context): List<TrackedApp> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(mainIntent, 0)
        .map { it.activityInfo }
        .distinctBy { it.packageName }
        .map { TrackedApp(it.packageName, it.loadLabel(pm).toString()) }
        .sortedBy { it.displayName.lowercase() }
}
