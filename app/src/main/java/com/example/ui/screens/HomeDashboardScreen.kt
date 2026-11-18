package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai.ChanakyaUiState
import com.example.ai.ChatMessage
import com.example.data.model.CancellationRequestEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.LoanEmiEntity
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.preferences.CreditWalletState
import com.example.ui.DashboardAuditSnapshot
import com.example.ui.GmailSyncState
import com.example.ui.components.AddManualTransactionDialog
import com.example.ui.components.AskChanakyaInlineCard
import com.example.ui.components.CancellationTrackerCard
import com.example.ui.components.ChanakyaDailyVerdictCard
import com.example.ui.components.ChanakyaLiveShieldStatusBar
import com.example.ui.components.DuplicateChargeAlertCard
import com.example.ui.components.FinancialHealthGaugeCard
import com.example.ui.components.GmailAutoSyncCard
import com.example.ui.components.QuickMetrics3ColumnRow
import com.example.ui.components.RoyalBlueHeroCard
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.TrialExitShieldCard
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

@Composable
fun HomeDashboardScreen(
    transactions: List<TransactionEntity>,
    activeServices: List<TransactionEntity>,
    activeTrials: List<TransactionEntity>,
    cancellations: List<CancellationRequestEntity>,
    duplicateAlerts: List<DuplicateChargeAlert>,
    loans: List<LoanEmiEntity>,
    creditCards: List<CreditCardEntity>,
    moneySaved: Double,
    privacyBlur: Boolean,
    wallet: CreditWalletState,
    latestAiReply: ChatMessage?,
    chanakyaState: ChanakyaUiState,
    isMicListening: Boolean,
    gmailConnectedEmail: String?,
    lastGmailSyncTimestamp: Long?,
    lastSmsScanTimestamp: Long?,
    gmailSyncState: GmailSyncState,
    dashboardAudit: DashboardAuditSnapshot,
    onOpenActiveShieldSheet: () -> Unit,
    onSpeakVerdict: (String) -> Unit,
    onSyncBureauAndAccounts: () -> Unit,
    onUploadCibilPdf: (Uri) -> Unit,
    onScanSmsInbox: () -> Unit,
    onConnectOrSyncGmail: () -> Unit,
    onDisconnectGmail: () -> Unit,
    onOpenCloudSetupGuide: () -> Unit,
    onAddManualEntry: (String, Double, TransactionType, SpendCategory, String) -> Unit,
    onInlineQuickQuery: (String) -> Unit,
    onInlineSendText: (String) -> Unit,
    onInlineMicClick: () -> Unit,
    onAttachDocumentUri: (Uri) -> Unit,
    onOpenFullChanakya: () -> Unit,
    onOpenActiveServices: () -> Unit,
    onStartServiceCancellation: (TransactionEntity) -> Unit,
    onOpenCancellationDetail: (Long) -> Unit,
    onExitTrialNow: (TransactionEntity) -> Unit,
    onTestTrialNotification: (TransactionEntity) -> Unit,
    onDraftDuplicateRefund: (DuplicateChargeAlert) -> Unit,
    onOpenLoansTab: () -> Unit,
    onOpenCardsTab: () -> Unit,
    onOpenSaveTab: () -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    val context = LocalContext.current
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var selectedTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingSyncAfterSmsPerm by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.READ_SMS] == true) {
            onScanSmsInbox()
        }
        if (pendingSyncAfterSmsPerm) {
            pendingSyncAfterSmsPerm = false
            onConnectOrSyncGmail()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAttachDocumentUri(uri)
    }

    val cibilPdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onUploadCibilPdf(uri)
    }

    val totalMonthlyEmi = remember(loans) { loans.sumOf { it.emiAmount } }
    val totalCardBill = remember(creditCards) { creditCards.sumOf { it.currentBill } }
    val duplicateMerchants = remember(duplicateAlerts) {
        duplicateAlerts.map { it.merchantName.lowercase() }.toSet()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0A. Live Status Bar (● CHANAKYA 24/7 ACTIVE SHIELD + RUNNING NON-STOP)
        item {
            ChanakyaLiveShieldStatusBar(onClick = onOpenActiveShieldSheet)
        }

        // 0B. Chanakya's Daily Verdict (Hinglish Audit + Suno Chanakya TTS + Discuss with Chanakya)
        item {
            ChanakyaDailyVerdictCard(
                verdict = dashboardAudit.verdict,
                onSpeakVerdict = onSpeakVerdict,
                onDiscussWithChanakya = onOpenFullChanakya
            )
        }

        // 0C. 3-Column Quick Metrics Row (Faltu Kharche, Active Trials, Active Debts)
        item {
            QuickMetrics3ColumnRow(
                metrics = dashboardAudit.metrics,
                privacyBlur = privacyBlur,
                onFaltuKharcheClick = onOpenActiveServices,
                onActiveTrialsClick = {
                    val urgentTrial = activeTrials.firstOrNull()
                    if (urgentTrial != null) onExitTrialNow(urgentTrial) else onOpenSaveTab()
                },
                onActiveDebtsClick = onOpenLoansTab
            )
        }

        // 0D. Semi-Circle Credit & AutoPay Health Gauge + Upload Official CIBIL PDF
        item {
            FinancialHealthGaugeCard(
                health = dashboardAudit.health,
                onSyncClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        onSyncBureauAndAccounts()
                    } else {
                        pendingSyncAfterSmsPerm = true
                        smsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                        )
                    }
                },
                onUploadCibilPdfClick = {
                    cibilPdfPickerLauncher.launch(arrayOf("application/pdf"))
                },
                onViewCibilAuditClick = onOpenFullChanakya
            )
        }

        // 1. Royal Blue Hero Card (with 6-month SMS Scanner + Simulator)
        item {
            RoyalBlueHeroCard(
                moneySaved = moneySaved,
                activeTrialsCount = activeTrials.size,
                privacyBlur = privacyBlur,
                onCardClick = onOpenSaveTab,
                onScanSmsClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) onScanSmsInbox()
                    else smsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                    )
                },
                onAddManualEntryClick = { showAddEntryDialog = true }
            )
        }

        // 2. Online Gmail Auto-Sync Card (Google Sign-In + Gmail REST API + Chanakya AI Parser)
        item {
            GmailAutoSyncCard(
                connectedEmail = gmailConnectedEmail,
                lastGmailSyncTs = lastGmailSyncTimestamp,
                lastSmsScanTs = lastSmsScanTimestamp,
                gmailSyncState = gmailSyncState,
                onConnectOrSyncClick = onConnectOrSyncGmail,
                onDisconnectClick = onDisconnectGmail,
                onSetupGuideClick = onOpenCloudSetupGuide
            )
        }

        // 3. Ask Chanakya AI Inline Card
        item {
            AskChanakyaInlineCard(
                wallet = wallet,
                latestReply = latestAiReply,
                uiState = chanakyaState,
                isListening = isMicListening,
                onSuggestionClick = onInlineQuickQuery,
                onSendText = onInlineSendText,
                onMicClick = onInlineMicClick,
                onAttachClick = {
                    documentPickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                },
                onOpenFullAssistant = onOpenFullChanakya
            )
        }

        // 4. Unified Live Services Directory Preview (SMS + Gmail Deduplicated)
        if (activeServices.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Services & Auto-Pay List",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Merged SMS (6 mo) + Gmail mandates • Tap for details & cancel",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    TextButton(
                        onClick = onOpenActiveServices,
                        modifier = Modifier.testTag("view_all_active_services_button")
                    ) {
                        Text("View All (${activeServices.size})", color = RoyalBlue)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Active Services Directory",
                            tint = RoyalBlue
                        )
                    }
                }
            }
            items(activeServices.take(3), key = { "active_svc_${it.id}" }) { service ->
                TransactionRowCard(
                    tx = service,
                    privacyBlur = privacyBlur,
                    isDuplicateHighlighted = duplicateMerchants.contains(service.merchantName.lowercase()),
                    onClick = { selectedTx = service }
                )
            }
        }

        // 5. Active Cancellation & Mandate Tracker Cards
        if (cancellations.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Mandate & Cancellation Trackers",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(cancellations.take(3), key = { "cancel_${it.id}" }) { item ->
                CancellationTrackerCard(
                    item = item,
                    privacyBlur = privacyBlur,
                    onStatusDetailsClick = { onOpenCancellationDetail(item.id) }
                )
            }
        }

        // 6. Smart Trial Exit Shield Alert Card
        if (activeTrials.isNotEmpty()) {
            items(activeTrials, key = { "trial_${it.id}" }) { trial ->
                TrialExitShieldCard(
                    trial = trial,
                    privacyBlur = privacyBlur,
                    onExitAndCancel = { onExitTrialNow(trial) },
                    onTestNotification = { onTestTrialNotification(trial) }
                )
            }
        }

        // 7. Duplicate Charge Detector Card
        if (duplicateAlerts.isNotEmpty()) {
            items(duplicateAlerts.take(2), key = { "dup_${it.merchantName}" }) { alert ->
                DuplicateChargeAlertCard(
                    alert = alert,
                    privacyBlur = privacyBlur,
                    onDraftRefund = { onDraftDuplicateRefund(alert) }
                )
            }
        }

        // 8. Loans & Cards Quick Snapshot Row (Zero Dead Clicks)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    onClick = onOpenLoansTab,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_loans_snapshot_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = RoyalBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Monthly EMIs", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatInr(totalMonthlyEmi, privacyBlur),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "${loans.size} active loans →",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue
                        )
                    }
                }

                Card(
                    onClick = onOpenCardsTab,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_cards_snapshot_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CreditCard, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Card Bills Due", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatInr(totalCardBill, privacyBlur),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "${creditCards.size} cards tracked →",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }

        // 9. Recent Auto-Tracked Activity
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Auto-Tracked Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onOpenCardsTab,
                    modifier = Modifier.testTag("view_full_passbook_button")
                ) {
                    Text("Full Passbook", color = RoyalBlue)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RoyalBlue
                    )
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Surface(
                    onClick = { documentPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                    shape = RoundedCornerShape(18.dp),
                    color = RoyalBlueSoft,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No transactions recorded yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap to Upload Bank PDF / Screenshot, or use Scan SMS / + Add Entry.", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                    }
                }
            }
        } else {
            items(transactions.take(5), key = { "home_tx_${it.id}" }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    privacyBlur = privacyBlur,
                    isDuplicateHighlighted = duplicateMerchants.contains(tx.merchantName.lowercase()),
                    onClick = { selectedTx = tx }
                )
            }
        }
    }

    if (showAddEntryDialog) {
        AddManualTransactionDialog(
            onDismiss = { showAddEntryDialog = false },
            onConfirm = { merchant, amount, type, category, last4 ->
                onAddManualEntry(merchant, amount, type, category, last4)
                showAddEntryDialog = false
            }
        )
    }

    selectedTx?.let { tx ->
        TransactionDetailSheet(
            tx = tx,
            privacyBlur = privacyBlur,
            onDelete = { onDeleteTransaction(tx.id) },
            onDismiss = { selectedTx = null },
            onStartCancellation = if (
                tx.type == TransactionType.AUTOPAY ||
                tx.type == TransactionType.CARD_EMI ||
                tx.isTrial
            ) {
                { onStartServiceCancellation(tx) }
            } else null
        )
    }
}
