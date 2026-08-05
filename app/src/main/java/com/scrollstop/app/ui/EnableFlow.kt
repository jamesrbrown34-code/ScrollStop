package com.scrollstop.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared guidance for the "turn on accessibility" step: primary deep-link button, OEM-aware
 * copy, a manual re-check, and a fallback to the full settings list. Used by onboarding, the
 * Today banner and the Settings status card.
 */
@Composable
internal fun EnableAccessibilityActions(
    onOpenSettings: () -> Unit,
    onOpenList: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onOpenSettings,
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) { Text("Enable ScrollStop", fontWeight = FontWeight.Bold) }
    Text(
        "You'll see a ScrollStop toggle — flip it ON, then come back. If you see a security warning, tap Allow.",
        color = TextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onRefresh) { Text("I've turned it on", color = TextSecondary, fontSize = 12.sp) }
        TextButton(onClick = onOpenList) { Text("Can't find it", color = TextSecondary, fontSize = 12.sp) }
    }
}

/** Soft, non-blocking hint shown when the service is enabled in settings but not actually running. */
@Composable
internal fun ServiceNotRunningHint(modifier: Modifier = Modifier) {
    Text(
        "Service isn't running — open ScrollStop to restart it.",
        color = Accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}
