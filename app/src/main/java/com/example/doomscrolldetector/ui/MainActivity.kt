package com.example.doomscrolldetector.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) {
        while (true) {
            ScrollTracker.onIdleCheck(System.currentTimeMillis())
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
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
}
