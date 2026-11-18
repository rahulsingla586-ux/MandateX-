package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.BudgetStatus
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.preferences.CreditWalletState
import com.example.engine.PaymentNotificationListenerService
import com.example.ui.components.DuplicateChargeAlertCard
import com.example.ui.components.TrialExitShieldCard
import com.example.ui.theme.AlertRed
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
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

@Composable
fun SaveHubScreen(
    activeTrials: List<TransactionEntity>,
    duplicateAlerts: List<DuplicateChargeAlert>,
    budgetStatuses: List<BudgetStatus>,
    totalSaved: Double,
    transactionsCount: Int,
    wallet: CreditWalletState,
    privacyBlur: Boolean,
    biometricLockEnabled: Boolean,
    userName: String,
    bankSupportEmail: String,
    onExitTrialNow: (TransactionEntity) -> Unit,
    onTestTrialNotification: (TransactionEntity) -> Unit,
    onDraftDuplicateRefund: (DuplicateChargeAlert) -> Unit,
    onUpdateBudget: (SpendCategory, Double) -> Unit,
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
    var editingBudget by remember { mutableStateOf<BudgetStatus?>(null) }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Savings & Credits Wallet Summary Card
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
                        Text("AUTOPAY SAVINGS & WALLET", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        Text(
                            text = CurrencyFormatter.formatInr(totalSaved, privacyBlur),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text("Protected from unwanted trials & duplicates", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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

        // 2. Trial Exit Shield
        if (activeTrials.isNotEmpty()) {
            item {
                Text("Trial Exit Shield Alerts", style = MaterialTheme.typography.titleMedium, color = AlertRed, fontWeight = FontWeight.Bold)
            }
            items(activeTrials, key = { "save_trial_${it.id}" }) { trial ->
                TrialExitShieldCard(
                    trial = trial,
                    privacyBlur = privacyBlur,
                    onExitAndCancel = { onExitTrialNow(trial) },
                    onTestNotification = { onTestTrialNotification(trial) }
                )
            }
        }

        // 3. Duplicate Charge Radar
        if (duplicateAlerts.isNotEmpty()) {
            item {
                Text("Scam & Duplicate Charge Radar", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            items(duplicateAlerts, key = { "save_dup_${it.merchantName}" }) { alert ->
                DuplicateChargeAlertCard(
                    alert = alert,
                    privacyBlur = privacyBlur,
                    onDraftRefund = { onDraftDuplicateRefund(alert) }
                )
            }
        }

        // 4. Smart Spend Limits (80% / 100%)
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Category Spend Limits (80% / 100%)", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)
            }
        }

        items(budgetStatuses, key = { "budget_${it.category.name}" }) { status ->
            val progressColor = when {
                status.isExceeded100 -> AlertRed
                status.isWarning80 -> GoldAmber
                else -> EmeraldGreen
            }
            Card(
                onClick = { editingBudget = status },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(
                    1.dp,
                    if (status.isExceeded100 || status.isWarning80) progressColor.copy(alpha = 0.6f) else BorderLight
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(status.category.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${CurrencyFormatter.formatInr(status.spent, privacyBlur)} / ${CurrencyFormatter.formatInr(status.limit, privacyBlur)} (${(status.percentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = progressColor
                            )
                        }
                        IconButton(
                            onClick = { editingBudget = status },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_budget_${status.category.name}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit ${status.category.label} cap", tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { status.percentage.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50.dp)),
                        color = progressColor,
                        trackColor = SurfaceMuted
                    )
                    if (status.isExceeded100 || status.isWarning80) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (status.isExceeded100) "Auto-Freeze Triggered: Limit crossed!" else "Chanakya Warning: Crossed 80% threshold",
                            style = MaterialTheme.typography.labelSmall,
                            color = progressColor
                        )
                    }
                }
            }
        }

        // 5. 1-Click PDF & CSV CA Report Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Safe PDF / Excel CA Report Export", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)
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

        // 6. Biometric Lock, Privacy & Live Permissions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Security, Listeners & Profile", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = RoyalBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Biometric App Lock", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Protect vault on app launch", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                        Text("Test Biometric Lock Screen")
                    }

                    PermissionRow(
                        icon = Icons.Filled.Sms,
                        title = "Bank & UPI SMS Access",
                        enabled = hasSmsPermission,
                        actionLabel = if (hasSmsPermission) "Scan" else "Grant",
                        onClick = {
                            if (hasSmsPermission) onScanSmsInbox()
                            else smsPermLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                        }
                    )

                    PermissionRow(
                        icon = Icons.Filled.Notifications,
                        title = "GPay / PhonePe / Paytm Listener",
                        enabled = isNotifListenerActive,
                        actionLabel = if (isNotifListenerActive) "Active" else "Enable",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
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

    editingBudget?.let { target ->
        var limitText by remember(target) { mutableStateOf(target.limit.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { editingBudget = null },
            containerColor = CardWhite,
            title = { Text("Set ${target.category.label} Cap", color = TextPrimary) },
            text = {
                OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text("Monthly Limit (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = limitText.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            onUpdateBudget(target.category, parsed)
                            editingBudget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { editingBudget = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun PermissionRow(
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
