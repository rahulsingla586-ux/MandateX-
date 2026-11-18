package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

@Composable
fun ActiveServicesScreen(
    activeServices: List<TransactionEntity>,
    duplicateAlerts: List<DuplicateChargeAlert>,
    privacyBlur: Boolean,
    gmailConnectedEmail: String?,
    onBack: () -> Unit,
    onScanSmsInbox: () -> Unit,
    onSyncGmail: () -> Unit,
    onStartCancellation: (TransactionEntity) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedFilter by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf<TransactionEntity?>(null) }

    val filters = listOf(
        "All Services",
        "SMS (6 Mo)",
        "Gmail Sync",
        "AutoPay & Trials",
        "Cards & EMIs"
    )

    val duplicateMerchants = remember(duplicateAlerts) {
        duplicateAlerts.map { it.merchantName.lowercase() }.toSet()
    }

    val filteredServices = remember(activeServices, selectedFilter, searchQuery) {
        activeServices.filter { tx ->
            val srcUpper = tx.source.uppercase()
            val matchesFilter = when (selectedFilter) {
                0 -> true
                1 -> srcUpper.contains("SMS")
                2 -> srcUpper.contains("GMAIL")
                3 -> tx.type == TransactionType.AUTOPAY || tx.isTrial
                4 -> tx.type == TransactionType.CARD_EMI
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                tx.merchantName.contains(searchQuery, ignoreCase = true) ||
                (tx.mandateRefId?.contains(searchQuery, ignoreCase = true) == true) ||
                tx.paymentSource.contains(searchQuery, ignoreCase = true) ||
                (tx.upiId?.contains(searchQuery, ignoreCase = true) == true)
            matchesFilter && matchesSearch
        }
    }

    val activeMonthlyTotal = remember(filteredServices) {
        filteredServices.filter { !it.isCancelled }.sumOf { it.postTrialAmount ?: it.amount }
    }
    val smsCount = remember(activeServices) {
        activeServices.count { it.source.uppercase().contains("SMS") }
    }
    val gmailCount = remember(activeServices) {
        activeServices.count { it.source.uppercase().contains("GMAIL") }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("active_services_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Services & Auto-Pay List",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Unified 6-Month SMS + Gmail Directory (Deduplicated)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Summary & Dual-Engine Refresh Strip
        item {
            Surface(
                color = CardWhite,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RECURRING & EMI EXPOSURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyFormatter.formatInr(activeMonthlyTotal, privacyBlur),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = RoyalBlue
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = RoyalBlueSoft, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = "SMS: $smsCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(color = EmeraldSoft, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = "Gmail: $gmailCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onScanSmsInbox,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("directory_scan_sms_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan 6M SMS", maxLines = 1)
                        }
                        Button(
                            onClick = onSyncGmail,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("directory_sync_gmail_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (gmailConnectedEmail != null) EmeraldGreen else RoyalBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (gmailConnectedEmail != null) "Sync Gmail" else "Connect Gmail", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Search & Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search service, UMRN, UPI ID, or last 4") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_services_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters.indices.toList()) { idx ->
                        FilterChip(
                            selected = selectedFilter == idx,
                            onClick = { selectedFilter = idx },
                            label = { Text(filters[idx]) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlueContainer,
                                selectedLabelColor = RoyalBlue
                            ),
                            modifier = Modifier.testTag("services_filter_$idx")
                        )
                    }
                }
            }
        }

        if (filteredServices.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No matching active services", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan your 6-month SMS inbox or connect Gmail Auto-Sync to populate all recurring mandates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredServices, key = { "service_${it.id}" }) { service ->
                TransactionRowCard(
                    tx = service,
                    privacyBlur = privacyBlur,
                    isDuplicateHighlighted = duplicateMerchants.contains(service.merchantName.lowercase()),
                    onDelete = { onDeleteTransaction(service.id) },
                    onClick = { selectedService = service }
                )
            }
        }
    }

    selectedService?.let { tx ->
        TransactionDetailSheet(
            tx = tx,
            privacyBlur = privacyBlur,
            onDelete = { onDeleteTransaction(tx.id) },
            onDismiss = { selectedService = null },
            onStartCancellation = { onStartCancellation(tx) }
        )
    }
}
