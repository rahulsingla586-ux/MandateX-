package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.TransactionEntity
import com.example.data.preferences.CreditWalletState
import com.example.data.preferences.MasterVaultProfile
import com.example.engine.PaymentNotificationListenerService
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldTextDark
import com.example.ui.theme.GoldWalletBg
import com.example.ui.theme.GoldWalletBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

@Composable
fun VaultPrivacyScreen(
    activeTrials: List<TransactionEntity>,
    duplicateAlerts: List<DuplicateChargeAlert>,
    totalSaved: Double,
    transactionsCount: Int,
    wallet: CreditWalletState,
    privacyBlur: Boolean,
    biometricLockEnabled: Boolean,
    isVaultUnlocked: Boolean,
    vaultProfile: MasterVaultProfile,
    burnerAliases: List<String>,
    userName: String,
    bankSupportEmail: String,
    onUnlockVaultWithBiometric: () -> Unit,
    onLockVault: () -> Unit,
    onSaveVaultProfile: (String, String, String, String) -> Unit,
    onGenerateBurnerAlias: (String) -> String,
    onExitTrialNow: (TransactionEntity) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenTopUp: () -> Unit,
    onTogglePrivacyBlur: () -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit,
    onTestBiometricLock: () -> Unit,
    onScanSmsInbox: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onClearAllData: () -> Unit
) {
    val context = LocalContext.current
    var selectedAliasPurpose by remember { mutableStateOf("OTT Trial") }
    var showEditVaultDialog by remember { mutableStateOf(false) }
    var nameField by remember(userName) { mutableStateOf(userName) }
    var emailField by remember(bankSupportEmail) { mutableStateOf(bankSupportEmail) }

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val isNotifListenerActive = remember {
        PaymentNotificationListenerService.isNotificationAccessEnabled(context)
    }

    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasSmsPermission = grants[Manifest.permission.READ_SMS] == true
        if (hasSmsPermission) onScanSmsInbox()
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    val hasActiveRisk = activeTrials.isNotEmpty() || duplicateAlerts.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Savings & Credit Wallet Banner
        item {
            Card(
                onClick = onOpenTopUp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldSoft),
                border = BorderStroke(1.dp, EmeraldContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PRIVACY SHIELD & CREDITS WALLET", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        Text(
                            text = CurrencyFormatter.formatInr(totalSaved, privacyBlur),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text("Total AutoPay & trial traps blocked", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Surface(
                        color = GoldWalletBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GoldWalletBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(16.dp))
                                Text("${wallet.creditBalance}", style = MaterialTheme.typography.titleMedium, color = GoldTextDark, fontWeight = FontWeight.Bold)
                            }
                            Text("Top Up +", style = MaterialTheme.typography.labelSmall, color = GoldTextDark)
                        }
                    }
                }
            }
        }

        // 1. Screen Risk Interceptor (Red Alert Trigger for Suspicious Apps / Traps)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("screen_risk_interceptor_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.5.dp, if (hasActiveRisk) AlertRed.copy(alpha = 0.65f) else EmeraldGreen.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (hasActiveRisk) AlertRedContainer else EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasActiveRisk) Icons.Filled.Warning else Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = if (hasActiveRisk) AlertRed else EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Screen Risk Interceptor", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (hasActiveRisk) "HIGH RISK TRAP INTERCEPTED" else "Monitoring dark-pattern mandates",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasActiveRisk) AlertRed else EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            color = if (hasActiveRisk) AlertRedContainer else EmeraldContainer,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = if (hasActiveRisk) "ALERT" else "SAFE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasActiveRisk) AlertRed else EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (hasActiveRisk) {
                        val riskDesc = if (activeTrials.isNotEmpty()) {
                            val first = activeTrials.first()
                            "RED INTERCEPT: ${first.merchantName} (₹${first.amount.toInt()} trial -> ₹${(first.postTrialAmount ?: 499.0).toInt()}/mo) dark-pattern renewal detected!"
                        } else {
                            "RED INTERCEPT: ${duplicateAlerts.first().merchantName} duplicate charge pattern intercepted across payment handles."
                        }
                        Surface(
                            color = AlertRedContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = riskDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRed,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Real-time guard inspects UPI payment notifications & SMS mandates for hidden auto-debit traps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    if (activeTrials.isNotEmpty()) {
                        Button(
                            onClick = { onExitTrialNow(activeTrials.first()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("vault_quarantine_trial_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Quarantine & Exit Now", maxLines = 1)
                        }
                    }
                }
            }
        }

        // 2. 1-Click Burner Masked Alias Generator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("burner_alias_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AlternateEmail, contentDescription = null, tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("1-Click Burner Masked Alias Generator", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Protect your real email from spam & trial traps", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("OTT Trial", "Shopping", "Loan Check", "App Signup")) { purpose ->
                            FilterChip(
                                selected = selectedAliasPurpose == purpose,
                                onClick = { selectedAliasPurpose = purpose },
                                label = { Text(purpose) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val newAlias = onGenerateBurnerAlias(selectedAliasPurpose)
                            copyToClipboard(context, newAlias, "Burner Alias")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_burner_alias_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.AlternateEmail, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Copy Burner Alias", fontWeight = FontWeight.Bold)
                    }

                    if (burnerAliases.isEmpty()) {
                        Text(
                            text = "No burner aliases generated yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        burnerAliases.take(3).forEachIndexed { idx, alias ->
                            Surface(
                                color = SurfaceMuted.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = alias,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { copyToClipboard(context, alias, "Burner Alias") },
                                        modifier = Modifier.testTag("copy_burner_alias_$idx")
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy alias", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Biometric Master Profile Vault (Aadhaar / PAN / Card Details)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biometric_master_vault_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, if (isVaultUnlocked) EmeraldContainer else RoyalBlueContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isVaultUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (isVaultUnlocked) EmeraldGreen else RoyalBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Biometric Master Profile Vault", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Aadhaar • PAN • Card Autofill (Encrypted)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        if (isVaultUnlocked) {
                            TextButton(onClick = onLockVault) {
                                Text("Lock", color = AlertRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!isVaultUnlocked) {
                        Surface(
                            color = RoyalBlueSoft,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "PAN, Aadhaar & Card autofill details are locked behind hardware BiometricPrompt.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Button(
                                    onClick = onUnlockVaultWithBiometric,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("unlock_master_vault_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlock Vault with Biometric", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        VaultFieldCopyRow("PAN Number", vaultProfile.panNumber) {
                            copyToClipboard(context, vaultProfile.panNumber, "PAN")
                        }
                        VaultFieldCopyRow("Aadhaar Reference", vaultProfile.aadhaarMasked) {
                            copyToClipboard(context, vaultProfile.aadhaarMasked, "Aadhaar")
                        }
                        VaultFieldCopyRow("Primary Card", vaultProfile.primaryCardMask) {
                            copyToClipboard(context, vaultProfile.primaryCardMask, "Card Mask")
                        }
                        VaultFieldCopyRow("Primary UPI ID", vaultProfile.primaryUpiHandle) {
                            copyToClipboard(context, vaultProfile.primaryUpiHandle, "UPI Handle")
                        }
                        OutlinedButton(
                            onClick = { showEditVaultDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("edit_master_vault_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Master Autofill Details")
                        }
                    }
                }
            }
        }

        // 4. 1-Click PDF & CSV CA Report Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1-Click PDF / CSV Statement Export", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$transactionsCount ledger entries ready. PDF Export costs 2 Credits (₹2); Excel CSV is free.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onExportPdf,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF (2 Cr)")
                        }
                        OutlinedButton(
                            onClick = onExportCsv,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_csv_button"),
                            border = BorderStroke(1.dp, EmeraldGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV")
                        }
                    }
                }
            }
        }

        // 5. Security, Listeners & Profile Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security, Listeners & Profile", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = RoyalBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Biometric App Lock", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Protect app on launch", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Switch(checked = biometricLockEnabled, onCheckedChange = onToggleBiometricLock, colors = SwitchDefaults.colors(checkedTrackColor = RoyalBlue))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Privacy Blur (₹****)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Mask balances in public", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Switch(checked = privacyBlur, onCheckedChange = { onTogglePrivacyBlur() }, colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen))
                    }

                    OutlinedButton(
                        onClick = onTestBiometricLock,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Biometric App Lock Screen")
                    }

                    HorizontalDivider(color = BorderLight)

                    VaultPermissionRow(
                        icon = Icons.Filled.Sms,
                        title = "Bank & UPI SMS Access",
                        enabled = hasSmsPermission,
                        actionLabel = if (hasSmsPermission) "Scan" else "Grant",
                        onClick = {
                            if (hasSmsPermission) onScanSmsInbox()
                            else smsPermLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                        }
                    )

                    VaultPermissionRow(
                        icon = Icons.Filled.Notifications,
                        title = "GPay / PhonePe / Paytm Listener",
                        enabled = isNotifListenerActive,
                        actionLabel = if (isNotifListenerActive) "Active" else "Enable",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        VaultPermissionRow(
                            icon = Icons.Filled.Notifications,
                            title = "Trial Shield Alerts",
                            enabled = hasNotifPermission,
                            actionLabel = if (hasNotifPermission) "Allowed" else "Allow",
                            onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                    }

                    OutlinedTextField(value = nameField, onValueChange = { nameField = it }, label = { Text("Account Holder Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = emailField, onValueChange = { emailField = it }, label = { Text("Bank Nodal Support Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { onUpdateProfile(nameField, emailField) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Save Profile") }

                    OutlinedButton(
                        onClick = onClearAllData,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Data")
                    }
                }
            }
        }
    }

    if (showEditVaultDialog) {
        var pan by remember(vaultProfile) { mutableStateOf(vaultProfile.panNumber) }
        var aadhaar by remember(vaultProfile) { mutableStateOf(vaultProfile.aadhaarMasked) }
        var cardMask by remember(vaultProfile) { mutableStateOf(vaultProfile.primaryCardMask) }
        var upi by remember(vaultProfile) { mutableStateOf(vaultProfile.primaryUpiHandle) }

        AlertDialog(
            onDismissRequest = { showEditVaultDialog = false },
            containerColor = CardWhite,
            title = { Text("Edit Master Vault Details", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pan, onValueChange = { pan = it }, label = { Text("PAN Number") }, singleLine = true)
                    OutlinedTextField(value = aadhaar, onValueChange = { aadhaar = it }, label = { Text("Aadhaar Masked / Ref") }, singleLine = true)
                    OutlinedTextField(value = cardMask, onValueChange = { cardMask = it }, label = { Text("Primary Card Mask") }, singleLine = true)
                    OutlinedTextField(value = upi, onValueChange = { upi = it }, label = { Text("Primary UPI ID") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveVaultProfile(pan, aadhaar, cardMask, upi)
                        showEditVaultDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
                ) { Text("Save Vault") }
            },
            dismissButton = {
                TextButton(onClick = { showEditVaultDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun VaultFieldCopyRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Surface(
        color = SurfaceMuted.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                    text = value.ifBlank { "Not set" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Bold
                    ),
                    color = if (value.isBlank()) TextMuted else TextPrimary
                )
            }
            if (value.isNotBlank()) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $label", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun VaultPermissionRow(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (enabled) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                tint = if (enabled) EmeraldGreen else GoldAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
        OutlinedButton(onClick = onClick, modifier = Modifier.height(38.dp), shape = RoundedCornerShape(10.dp)) {
            Text(actionLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}
