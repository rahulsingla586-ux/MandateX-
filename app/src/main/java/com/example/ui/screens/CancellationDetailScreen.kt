package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CancellationRequestEntity
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.UpiApp
import com.example.util.UpiAutoPayIntents

@Composable
fun CancellationDetailScreen(
    cancellation: CancellationRequestEntity?,
    userName: String,
    bankSupportEmail: String,
    privacyBlur: Boolean,
    onBack: () -> Unit,
    onAdvanceStep: (CancellationRequestEntity, Int, String) -> Unit,
    onConsumeEmailQuota: (onAllowed: () -> Unit) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    if (cancellation == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Mandate tracker not found", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Back") }
        }
        return
    }

    var generatorStep by remember { mutableIntStateOf(1) }
    var isRbiMode by remember { mutableStateOf(false) }
    var merchantInput by remember(cancellation.id) { mutableStateOf(cancellation.merchantName) }
    var bankInput by remember(cancellation.id) { mutableStateOf(cancellation.bankName) }
    var last4Input by remember(cancellation.id) { mutableStateOf(cancellation.accountLast4) }
    var amountInput by remember(cancellation.id) { mutableStateOf(cancellation.amount.toInt().toString()) }
    var reasonInput by remember(cancellation.id) {
        mutableStateOf("Immediate revocation of AutoPay / trial mandate before recurring charge.")
    }

    val (draftSubject, draftBody) = remember(
        isRbiMode, userName, merchantInput, bankInput, last4Input, amountInput, reasonInput
    ) {
        val amt = amountInput.toDoubleOrNull() ?: cancellation.amount
        if (isRbiMode) {
            UpiAutoPayIntents.buildRbiOmbudsmanEmail(
                userName = userName,
                bankName = bankInput,
                merchant = merchantInput,
                upiOrCardLast4 = amt,
                accountLast4 = last4Input,
                reason = reasonInput
            )
        } else {
            UpiAutoPayIntents.buildBankCancellationEmail(
                userName = userName,
                merchant = merchantInput,
                upiOrCardLast4 = last4Input,
                amount = amt,
                reason = reasonInput
            )
        }
    }

    val steps = listOf(
        "1. Mandate Detected & Protected" to "Auto-flagged ${cancellation.upiId} on XX${cancellation.accountLast4}",
        "2. UPI App AutoPay Revocation" to "Pause/Revoke inside GPay, PhonePe or Paytm AutoPay",
        "3. Formal Bank / RBI Escalation" to "Send RBI-compliant e-Mandate cancellation email",
        "4. Cancellation Confirmed" to "Zero future debits verified • Savings locked in"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("cancellation_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${cancellation.merchantName} Mandate", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("${cancellation.subtitle} • ${CurrencyFormatter.formatInr(cancellation.amount, privacyBlur)}/mo", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Surface(
                    color = if (cancellation.status == "COMPLETED") EmeraldContainer else RoyalBlueContainer,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = cancellation.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cancellation.status == "COMPLETED") EmeraldGreen else RoyalBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 1. Step-by-Step Tracking Timeline Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Live Cancellation Timeline", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)

                    steps.forEachIndexed { idx, (title, subtitle) ->
                        val stepNum = idx + 1
                        val done = cancellation.currentStep >= stepNum
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (done) EmeraldSoft else SurfaceMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (done) EmeraldGreen else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(title, style = MaterialTheme.typography.titleSmall, color = if (done) TextPrimary else TextSecondary, fontWeight = FontWeight.SemiBold)
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (cancellation.currentStep < 4) {
                            OutlinedButton(
                                onClick = {
                                    val next = (cancellation.currentStep + 1).coerceAtMost(4)
                                    val nextStatus = when (next) {
                                        2 -> "IN PROGRESS"
                                        3 -> "EMAIL SENT"
                                        else -> "COMPLETED"
                                    }
                                    onAdvanceStep(cancellation, next, nextStatus)
                                },
                                modifier = Modifier.weight(1f).height(46.dp).testTag("advance_timeline_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Advance Step")
                            }
                        }
                        Button(
                            onClick = { onAdvanceStep(cancellation, 4, "COMPLETED") },
                            enabled = cancellation.status != "COMPLETED",
                            modifier = Modifier.weight(1f).height(46.dp).testTag("complete_cancellation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (cancellation.status == "COMPLETED") "Completed ✓" else "Mark Cancelled")
                        }
                    }
                }
            }
        }

        // 2. 1-Tap UPI AutoPay App Redirects
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1-Tap UPI AutoPay Redirects", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UpiApp.entries.forEach { app ->
                        Card(
                            onClick = { UpiAutoPayIntents.openUpiAppAutoPay(context, app) },
                            modifier = Modifier.weight(1f).testTag("detail_upi_${app.name.lowercase()}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open ${app.displayName}", tint = RoyalBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(app.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("AutoPay", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. 2-Step Bank Cancellation & RBI Ombudsman Generator (Quota Enforced + Copy 📋)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Gavel, contentDescription = null, tint = RoyalBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bank & RBI Email Generator", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)
                        }
                        Surface(color = RoyalBlueContainer, shape = RoundedCornerShape(50.dp)) {
                            Text("Step $generatorStep/2", style = MaterialTheme.typography.labelSmall, color = RoyalBlue, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isRbiMode,
                            onClick = { isRbiMode = false },
                            label = { Text("Bank e-Mandate Cancel") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlueContainer, selectedLabelColor = RoyalBlue)
                        )
                        FilterChip(
                            selected = isRbiMode,
                            onClick = { isRbiMode = true },
                            label = { Text("RBI Ombudsman CMS") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AlertRedContainer, selectedLabelColor = AlertRed)
                        )
                    }

                    if (generatorStep == 1) {
                        OutlinedTextField(value = merchantInput, onValueChange = { merchantInput = it }, label = { Text("Merchant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = bankInput, onValueChange = { bankInput = it }, label = { Text("Bank") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = last4Input, onValueChange = { last4Input = it.take(4) }, label = { Text("Last 4") }, singleLine = true, modifier = Modifier.width(92.dp))
                        }
                        OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text("Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = reasonInput, onValueChange = { reasonInput = it }, label = { Text("Reason") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                onConsumeEmailQuota {
                                    generatorStep = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("detail_generate_email_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Generate Draft (1 Free / 1 Credit)")
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceMuted,
                            border = BorderStroke(1.dp, BorderLight)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Subject: $draftSubject", style = MaterialTheme.typography.titleSmall, color = RoyalBlue, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(draftBody, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { generatorStep = 1 }, modifier = Modifier.weight(1f).height(48.dp)) {
                                Text("Edit")
                            }
                            OutlinedButton(
                                onClick = { copyToClipboard(context, "$draftSubject\n\n$draftBody", "Official Email Draft") },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("detail_copy_email_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy 📋")
                            }
                        }

                        Button(
                            onClick = {
                                val target = if (isRbiMode) "crpc@rbi.org.in" else bankSupportEmail
                                UpiAutoPayIntents.launchEmailIntent(context, target, draftSubject, draftBody)
                                onAdvanceStep(cancellation, 3, if (isRbiMode) "ESCALATED TO RBI" else "EMAIL SENT")
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("detail_send_email_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isRbiMode) AlertRed else RoyalBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRbiMode) "Send to RBI Ombudsman" else "Send to Bank Nodal Team")
                        }
                    }
                }
            }
        }
    }
}
