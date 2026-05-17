package com.example.doomscrolldetector.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.doomscrolldetector.core.ScrollTracker
import kotlinx.coroutines.delay

private val AppBackground = Color(0xFF050505)
private val PanelColor = Color(0xFF111111)
private val AccentColor = Color(0xFFEAEAEA)
private val SecondaryText = Color(0xFF8D8D8D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
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
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SCROLLSTOP",
            color = AccentColor,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = "minimal anti-doomscroll monitor",
            color = SecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PanelColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRow(label = "Current app", value = state.currentApp)
                MetricRow(label = "Scroll count", value = state.scrollCount.toString())
                MetricRow(label = "Session", value = "${state.sessionDurationMs / 1000}s")
                MetricRow(label = "Status", value = state.status)
            }
        }

        Text(
            text = "Enable accessibility + notifications for 100-scroll alerts.",
            color = SecondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 14.dp)
        )

        Button(
            onClick = onOpenAccessibilitySettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentColor,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
        ) {
            Text("Open Accessibility Settings", fontWeight = FontWeight.SemiBold)
        }
    }

    if (showHundredScrollPopup) {
        AlertDialog(
            onDismissRequest = { showHundredScrollPopup = false },
            containerColor = PanelColor,
            titleContentColor = AccentColor,
            textContentColor = SecondaryText,
            title = { Text("100 Scrolls Reached") },
            text = { Text("You've hit 100 scrolls in this session. Time for a short break.") },
            confirmButton = {
                TextButton(onClick = { showHundredScrollPopup = false }) {
                    Text("OK", color = AccentColor)
                }
            }
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 10.dp)
                .background(SecondaryText.copy(alpha = 0.35f), CircleShape)
                .height(8.dp)
                .width(8.dp)
        )
        Text(text = label.uppercase(), color = SecondaryText, fontSize = 11.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = AccentColor, fontWeight = FontWeight.SemiBold)
    }
}
