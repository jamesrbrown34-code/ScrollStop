package com.example.doomscrolldetector.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.doomscrolldetector.core.ScrollTracker
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreen(onOpenAccessibilitySettings: () -> Unit) {
    val state by ScrollTracker.uiState.collectAsState()
    val context = LocalContext.current
    var hasShownHundredScrollPopup by remember { mutableStateOf(false) }
    var showHundredScrollPopup by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        while (true) {
            ScrollTracker.onIdleCheck(System.currentTimeMillis())
            delay(1000)
        }
    }

    LaunchedEffect(state.scrollCount) {
        if (state.scrollCount >= 100 && !hasShownHundredScrollPopup) {
            showHundredScrollPopup = true
            hasShownHundredScrollPopup = true
        } else if (state.scrollCount < 100) {
            hasShownHundredScrollPopup = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Turn notifications on to get alerts at 100 scrolls and beyond.")
        Text(text = "Current App: ${state.currentApp}")
        Text(text = "Scroll Count: ${state.scrollCount}")
        Text(text = "Session Time: ${state.sessionDurationMs / 1000}s")
        Text(text = "Status: ${state.status}")

        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("Open Accessibility Settings")
        }
    }

    if (showHundredScrollPopup) {
        AlertDialog(
            onDismissRequest = { showHundredScrollPopup = false },
            title = { Text("100 Scrolls Reached") },
            text = { Text("You've hit 100 scrolls in this session. Time for a short break.") },
            confirmButton = {
                TextButton(onClick = { showHundredScrollPopup = false }) {
                    Text("OK")
                }
            }
        )
    }
}
