package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.engine.BankMessageParser
import com.example.engine.PaymentNotificationListenerService
import com.example.ui.components.AddManualTransactionDialog
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldWalletBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

enum class TimelineFilter(val label: String) {
    ALL("All Feed"),
    FINANCES("Finances"),
    AUTOPAY_TRAPS("AutoPay Traps"),
    ALERTS("Alerts")
}

private sealed class TimelineItem(open val timestamp: Long) {
    data class TransactionFeedItem(
        val tx: TransactionEntity,
        val isTrapOrOverdue: Boolean,
        val commentary: String
    ) : TimelineItem(tx.timestamp)

    data class AlertFeedItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val commentary: String,
        val ctaLabel: String,
        val isSevere: Boolean,
        override val timestamp: Long,
        val duplicateAlert: DuplicateChargeAlert? = null
    ) : TimelineItem(timestamp)
}

@Composable
fun TimelineFeedScreen(
    transactions: List<TransactionEntity>,
    duplicateAlerts: List<DuplicateChargeAlert>,
    privacyBlur: Boolean,
    onScanSmsInbox: () -> Unit,
    onSyncGmail: () -> Unit,
    onAddManualEntry: (String, Double, TransactionType, SpendCategory, String) -> Unit,
    onStartServiceCancellation: (TransactionEntity) -> Unit,
    onDraftDuplicateRefund: (DuplicateChargeAlert) -> Unit,
    onOpenVaultTab: () -> Unit,
    onAskChanakya: (String) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(TimelineFilter.ALL) }
    var selectedTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddEntryDialog by remember { mutableStateOf(false) }

    val hasSmsPerm = remember(transactions.size) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val hasListener = remember(transactions.size) {
        PaymentNotificationListenerService.isNotificationAccessEnabled(context)
    }

    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.READ_SMS] == true) {
            onScanSmsInbox()
        }
    }

    val feedItems = remember(transactions, duplicateAlerts, hasSmsPerm, hasListener, privacyBlur) {
        val list = mutableListOf<TimelineItem>()
        val now = System.currentTimeMillis()

        // Security / Permission Shield Warnings
        if (!hasSmsPerm || !hasListener) {
            list.add(
                TimelineItem.AlertFeedItem(
                    id = "perm_shield_alert",
                    title = "Live Interceptor Guardrail Needs Permission",
                    subtitle = buildString {
                        if (!hasSmsPerm) append("Bank SMS Reader offline")
                        if (!hasSmsPerm && !hasListener) append(" • ")
                        if (!hasListener) append("UPI Notification Listener inactive")
                    },
                    commentary = "Chanakya Bole: Bina SMS/Notification permission ke hidden ₹1 trials aur duplicate debits real-time mein nahi pakde ja sakte — Vault mein jaakar Guard ON karo!",
                    ctaLabel = "Open Vault Shield",
                    isSevere = true,
                    timestamp = now
                )
            )
        }

        // Duplicate charge alerts
        duplicateAlerts.forEach { dup ->
            list.add(
                TimelineItem.AlertFeedItem(
                    id = "dup_${dup.merchantName}",
                    title = "Duplicate Charge Trap: ${dup.merchantName}",
                    subtitle = "${dup.charges.size} charges • ${CurrencyFormatter.formatInr(dup.totalOvercharged, privacyBlur)} extra vasooli",
                    commentary = "Chanakya Bole: Ek hi service ke liye 2 jagah se paisa kata hai (${dup.reason}). Turant RBI/Merchant refund email bhejo!",
                    ctaLabel = "Draft Refund Email",
                    isSevere = true,
                    timestamp = dup.charges.maxOfOrNull { it.timestamp } ?: now,
                    duplicateAlert = dup
                )
            )
        }

        // Transaction items with Chanakya's blunt Hinglish commentary
        transactions.forEach { tx ->
            val isMissed = BankMessageParser.isMissedOrOverdueMessage(tx.rawMessage)
            val isTrap = (tx.isTrial && !tx.isCancelled) || isMissed
            val commentary = buildChanakyaCommentary(tx, isMissed, privacyBlur)
            list.add(
                TimelineItem.TransactionFeedItem(
                    tx = tx,
                    isTrapOrOverdue = isTrap,
                    commentary = commentary
                )
            )
        }

        list.sortedByDescending { it.timestamp }
    }

    val filteredFeed = remember(feedItems, selectedFilter) {
        feedItems.filter { item ->
            when (selectedFilter) {
                TimelineFilter.ALL -> true
                TimelineFilter.FINANCES -> item is TimelineItem.TransactionFeedItem && !item.isTrapOrOverdue
                TimelineFilter.AUTOPAY_TRAPS -> item is TimelineItem.TransactionFeedItem &&
                    (item.tx.type == TransactionType.AUTOPAY || item.tx.isTrial)
                TimelineFilter.ALERTS -> item is TimelineItem.AlertFeedItem ||
                    (item is TimelineItem.TransactionFeedItem && item.isTrapOrOverdue)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Digital Life & Activity Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-time SMS/Gmail debits, AutoPay traps & Chanakya's blunt Hinglish audit",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Quick Live Sync Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hasSmsPerm) onScanSmsInbox()
                            else smsPermLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("timeline_scan_sms_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.Sms, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Scan SMS", style = MaterialTheme.typography.labelMedium, color = RoyalBlue, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onSyncGmail,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("timeline_sync_gmail_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Gmail Sync", style = MaterialTheme.typography.labelMedium, color = EmeraldGreen, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showAddEntryDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("timeline_add_entry_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Add Entry", style = MaterialTheme.typography.labelMedium, color = RoyalBlue, maxLines = 1)
                    }
                }

                // Filter Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TimelineFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("timeline_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filteredFeed.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No timeline activity yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap + Add Entry, Scan SMS, or Sync Gmail to populate your feed.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        } else {
            items(filteredFeed, key = { item ->
                when (item) {
                    is TimelineItem.AlertFeedItem -> "alert_${item.id}"
                    is TimelineItem.TransactionFeedItem -> "tx_${item.tx.id}"
                }
            }) { item ->
                when (item) {
                    is TimelineItem.AlertFeedItem -> {
                        TimelineAlertCard(
                            item = item,
                            onActionClick = {
                                if (item.duplicateAlert != null) {
                                    onDraftDuplicateRefund(item.duplicateAlert)
                                } else {
                                    onOpenVaultTab()
                                }
                            }
                        )
                    }
                    is TimelineItem.TransactionFeedItem -> {
                        TimelineTransactionCard(
                            item = item,
                            privacyBlur = privacyBlur,
                            onCardClick = { selectedTx = item.tx },
                            onExitOrCancel = { onStartServiceCancellation(item.tx) },
                            onAskChanakya = {
                                onAskChanakya("${item.tx.merchantName} (₹${item.tx.amount.toInt()}) transaction par audit aur advice do")
                            }
                        )
                    }
                }
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

@Composable
private fun TimelineAlertCard(
    item: TimelineItem.AlertFeedItem,
    onActionClick: () -> Unit
) {
    Card(
        onClick = onActionClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.2.dp, if (item.isSevere) AlertRed.copy(alpha = 0.55f) else GoldAmber)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AlertRedContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.duplicateAlert != null) Icons.Filled.Warning else Icons.Filled.Security,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = AlertRed, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Text(CurrencyFormatter.formatDateOnly(item.timestamp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }

            Surface(
                color = AlertRedContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = item.commentary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Button(
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(item.ctaLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimelineTransactionCard(
    item: TimelineItem.TransactionFeedItem,
    privacyBlur: Boolean,
    onCardClick: () -> Unit,
    onExitOrCancel: () -> Unit,
    onAskChanakya: () -> Unit
) {
    val tx = item.tx
    val borderColor = when {
        item.isTrapOrOverdue -> AlertRed.copy(alpha = 0.55f)
        tx.type == TransactionType.AUTOPAY -> GoldAmber.copy(alpha = 0.45f)
        else -> BorderLight
    }
    val badgeBg = when {
        item.isTrapOrOverdue -> AlertRedContainer
        tx.type == TransactionType.CARD_EMI -> EmeraldContainer
        tx.type == TransactionType.AUTOPAY -> GoldWalletBg
        else -> RoyalBlueSoft
    }
    val badgeColor = when {
        item.isTrapOrOverdue -> AlertRed
        tx.type == TransactionType.CARD_EMI -> EmeraldGreen
        tx.type == TransactionType.AUTOPAY -> GoldAmber
        else -> RoyalBlue
    }

    Card(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_tx_${tx.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tx.merchantName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = tx.source,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = buildString {
                            append(tx.paymentSource.ifBlank { "${tx.bankName} • XX${tx.accountLast4}" })
                            if (!tx.upiId.isNullOrBlank()) append(" • ${tx.upiId}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatInr(tx.amount, privacyBlur),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (item.isTrapOrOverdue) AlertRed else TextPrimary
                    )
                    Text(
                        text = CurrencyFormatter.formatDateOnly(tx.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Chanakya's blunt Hinglish commentary
            Surface(
                color = if (item.isTrapOrOverdue) AlertRedContainer.copy(alpha = 0.45f) else RoyalBlueSoft.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (tx.type == TransactionType.CARD_EMI) Icons.Filled.AccountBalance else Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (item.isTrapOrOverdue) AlertRed else RoyalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.commentary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tx.type == TransactionType.AUTOPAY || tx.isTrial) {
                    OutlinedButton(
                        onClick = onExitOrCancel,
                        modifier = Modifier.weight(1f).height(40.dp),
                        border = BorderStroke(1.dp, if (item.isTrapOrOverdue) AlertRed else RoyalBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (tx.isCancelled) "Cancelled" else "Revoke Mandate",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.isTrapOrOverdue) AlertRed else RoyalBlue,
                            maxLines = 1
                        )
                    }
                }
                OutlinedButton(
                    onClick = onAskChanakya,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Ask Chanakya",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalBlue,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun buildChanakyaCommentary(
    tx: TransactionEntity,
    isMissed: Boolean,
    privacyBlur: Boolean
): String {
    if (isMissed) {
        return "Chanakya Warning: EMI/Card bounce ya overdue alert aaya hai! Late fee aur CIBIL drop se bachne ke liye abhi clear karo."
    }
    if (tx.isTrial && !tx.isCancelled) {
        val full = CurrencyFormatter.formatInr(tx.postTrialAmount ?: 499.0, privacyBlur)
        return "Chanakya Bole: Yeh ₹${tx.amount.toInt()} ka trial jaal hai — expiry ke baad $full/mo kat jayega. Time rehte band maaro!"
    }
    if (tx.isCancelled) {
        val saved = CurrencyFormatter.formatInr(tx.savedAmount, privacyBlur)
        return "Chanakya Salute: Badhiya! Yeh mandate band karke aapne $saved bachaye."
    }
    return when (tx.category) {
        SpendCategory.FOOD_DINING ->
            "Chanakya Bole: Phir ${tx.merchantName} par order? Mahine ka Food & Dining budget dhyan mein rakho, chhote orders milkar bada bill banate hain!"
        SpendCategory.OTT_APPS ->
            "Chanakya Bole: Active subscription (${tx.mandateRefId ?: "UPI AutoPay"}). Agar roz use nahi ho raha toh turant cancel karo!"
        SpendCategory.CARD_EMI ->
            "Chanakya Audit: Time par EMI/Card payment CIBIL health score ko +16 points tak boost karta hai."
        SpendCategory.SHOPPING ->
            "Chanakya Bole: Impulse shopping se pehle 24-hour rule lagao — kya yeh zaroori kharcha tha?"
        else ->
            "Chanakya Audit: ${tx.source} se auto-captured. Sab clean aur tracked hai."
    }
}
