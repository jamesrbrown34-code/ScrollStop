package com.scrollstop.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrollstop.app.premium.BillingState

private val premiumFeatures = listOf(
    "Custom reminder limits",
    "Reminder tones",
    "Per-app limits & tones",
    "Colour themes",
    "Streak freeze",
    "Per-app quiet hours",
    "Per-day goals",
    "Daily summary",
    "Hourly insights",
    "Full milestone gallery"
)

private enum class PaywallPlan(val productId: String, val label: String) {
    ANNUAL("scrollstop_yearly", "Annual"),
    MONTHLY("scrollstop_monthly", "Monthly")
}

@Composable
internal fun PremiumPaywallPage(
    billing: BillingState,
    onClose: () -> Unit,
    onPurchase: (productId: String) -> Unit,
    onRestore: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(PaywallPlan.ANNUAL) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "✕",
                    color = TextSecondary,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClose)
                        .padding(4.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("ScrollBeat Premium", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(
                "Doomscrolling habits, met with calm control.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(24.dp))
            premiumFeatures.forEach { feature ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("✓", color = PremiumGold, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 10.dp))
                    Text(feature, color = TextPrimary, fontSize = 14.sp)
                }
            }
            Text(
                "…and more to come",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )

            Spacer(Modifier.height(24.dp))
            PaywallPlan.entries.forEach { plan ->
                PaywallPlanCard(
                    plan = plan,
                    billing = billing,
                    selected = selectedPlan == plan,
                    onClick = { selectedPlan = plan },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onPurchase(selectedPlan.productId) },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Background),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Go Premium",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Text(
                "1 week free, then ${if (selectedPlan == PaywallPlan.ANNUAL) (billing.yearlyPrice ?: "$14.99") else (billing.monthlyPrice ?: "$1.99")}/${if (selectedPlan == PaywallPlan.ANNUAL) "year" else "month"} · cancel anytime",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            billing.message?.let {
                Text(it, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            }

            TextButton(onClick = onRestore, modifier = Modifier.padding(top = 16.dp)) {
                Text("Restore purchases", color = TextSecondary)
            }
            Text(
                "Subscriptions auto-renew until cancelled. Payment is taken at the end of your free week.",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PaywallPlanCard(
    plan: PaywallPlan,
    billing: BillingState,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) BorderStroke(2.dp, PremiumGold) else BorderStroke(1.dp, PanelColor)
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelColor),
        shape = RoundedCornerShape(16.dp),
        border = border,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (plan == PaywallPlan.ANNUAL && billing.annualSavingsPercent != null) {
                        Text(
                            "Save ${billing.annualSavingsPercent}%",
                            color = Background,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(PremiumGold, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text("1 week free", color = TextSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                when (plan) {
                    PaywallPlan.ANNUAL -> {
                        Text(
                            billing.monthlyYearlyEquivalent ?: "$23.88",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Text("${billing.yearlyPrice ?: "$14.99"}/year", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    PaywallPlan.MONTHLY -> {
                        Text("${billing.monthlyPrice ?: "$1.99"}/month", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
