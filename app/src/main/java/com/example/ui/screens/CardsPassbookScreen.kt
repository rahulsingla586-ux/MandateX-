package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CreditCardEntity
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

@Composable
fun CardsPassbookScreen(
    creditCards: List<CreditCardEntity>,
    transactions: List<TransactionEntity>,
    duplicateAlerts: List<DuplicateChargeAlert>,
    privacyBlur: Boolean,
    onPayCardBill: (CreditCardEntity) -> Unit,
    onAddCreditCard: (String, String, String, Double, Double, String, Double) -> Unit,
    onAskChanakya: (String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onAddManualEntry: (String, Double, TransactionType, SpendCategory, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SpendCategory?>(null) }
    var selectedCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var selectedTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }

    val tabTitles = listOf("All Expenses", "Cards & EMIs", "AutoPay")
    val duplicateMerchants = remember(duplicateAlerts) {
        duplicateAlerts.map { it.merchantName.lowercase() }.toSet()
    }

    val filteredList = remember(transactions, selectedTab, searchQuery, selectedCategory) {
        transactions.filter { tx ->
            val matchesTab = when (selectedTab) {
                0 -> true
                1 -> tx.type == TransactionType.CARD_EMI
                2 -> tx.type == TransactionType.AUTOPAY
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                tx.merchantName.contains(searchQuery, ignoreCase = true) ||
                (tx.upiId?.contains(searchQuery, ignoreCase = true) == true) ||
                tx.accountLast4.contains(searchQuery)
            val matchesCat = selectedCategory == null || tx.category == selectedCategory
            matchesTab && matchesSearch && matchesCat
        }
    }

    val tabSum = remember(filteredList) {
        filteredList.filter { !it.isCancelled }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Credit Cards Carousel Header
        item {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Credit Cards & Utilization",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { showAddCardDialog = true },
                        modifier = Modifier.testTag("add_credit_card_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Card", color = RoyalBlue)
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(creditCards, key = { it.id }) { card ->
                        val utilization = if (card.creditLimit > 0) (card.currentBill / card.creditLimit).toFloat().coerceIn(0f, 1f) else 0f
                        val utilPct = (utilization * 100).toInt()
                        val utilColor = when {
                            utilPct >= 50 -> AlertRed
                            utilPct >= 30 -> GoldAmber
                            else -> EmeraldGreen
                        }

                        Card(
                            onClick = { selectedCard = card },
                            modifier = Modifier
                                .width(256.dp)
                                .testTag("credit_card_item_${card.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            border = BorderStroke(1.dp, BorderLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CreditCard, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "XX${card.last4}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = RoyalBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = utilColor.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(50.dp)
                                    ) {
                                        Text(
                                            text = if (card.isPaid || card.currentBill <= 0.0) "PAID" else card.dueDateLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (card.isPaid || card.currentBill <= 0.0) EmeraldGreen else utilColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(card.cardName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = CurrencyFormatter.formatInr(card.currentBill, privacyBlur),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Limit ${CurrencyFormatter.formatInr(card.creditLimit, privacyBlur)} • $utilPct% used",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { utilization },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50.dp)),
                                    color = utilColor,
                                    trackColor = SurfaceMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3-Tab A-to-Z Passbook Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "A-to-Z SMS & UPI Passbook",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = AppBackground,
                    contentColor = RoyalBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = RoyalBlue
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            modifier = Modifier.testTag("passbook_tab_$index"),
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedTab == index) RoyalBlue else TextSecondary
                                )
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search merchant, UPI ID, or last 4") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { showAddTxDialog = true },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("add_manual_tx_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add manual entry", tint = RoyalBlue)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All Categories") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBlueContainer,
                                    selectedLabelColor = RoyalBlue
                                )
                            )
                        }
                        items(SpendCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                },
                                label = { Text(cat.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBlueContainer,
                                    selectedLabelColor = RoyalBlue
                                )
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredList.size} entries in ${tabTitles[selectedTab]}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatInr(tabSum, privacyBlur),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                                color = if (selectedTab == 2) EmeraldGreen else RoyalBlue
                            )
                        }
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No matching entries", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Try clearing filters or tap + to add an entry.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        } else {
            items(filteredList, key = { "passbook_tx_${it.id}" }) { tx ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TransactionRowCard(
                        tx = tx,
                        privacyBlur = privacyBlur,
                        isDuplicateHighlighted = duplicateMerchants.contains(tx.merchantName.lowercase()),
                        onDelete = { onDeleteTransaction(tx.id) },
                        onClick = { selectedTx = tx }
                    )
                }
            }
        }
    }

    selectedCard?.let { card ->
        CreditCardDetailSheet(
            card = card,
            privacyBlur = privacyBlur,
            onPayBill = {
                onPayCardBill(card)
                selectedCard = null
            },
            onAskChanakya = {
                selectedCard = null
                onAskChanakya("${card.cardName} (Bill ₹${card.currentBill.toInt()}, Limit ₹${card.creditLimit.toInt()}) ka utilization aur due date advice do")
            },
            onDismiss = { selectedCard = null }
        )
    }

    selectedTx?.let { tx ->
        TransactionDetailSheet(
            tx = tx,
            privacyBlur = privacyBlur,
            onDelete = { onDeleteTransaction(tx.id) },
            onDismiss = { selectedTx = null }
        )
    }

    if (showAddCardDialog) {
        AddCreditCardDialog(
            onDismiss = { showAddCardDialog = false },
            onConfirm = { name, bank, last4, bill, limit, due, minDue ->
                onAddCreditCard(name, bank, last4, bill, limit, due, minDue)
                showAddCardDialog = false
            }
        )
    }

    if (showAddTxDialog) {
        AddManualEntryDialog(
            onDismiss = { showAddTxDialog = false },
            onConfirm = { m, a, t, c, l4 ->
                onAddManualEntry(m, a, t, c, l4)
                showAddTxDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditCardDetailSheet(
    card: CreditCardEntity,
    privacyBlur: Boolean,
    onPayBill: () -> Unit,
    onAskChanakya: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val utilization = if (card.creditLimit > 0) ((card.currentBill / card.creditLimit) * 100).toInt() else 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlueDeep)))
                    .padding(18.dp)
            ) {
                Column {
                    Text("${card.bankName} •••• ${card.last4}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFDBEAFE))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(card.cardName, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Statement Bill: ${CurrencyFormatter.formatInr(card.currentBill, privacyBlur)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${card.dueDateLabel} • Min Due: ${CurrencyFormatter.formatInr(card.minDue, privacyBlur)} • Limit Util: $utilization%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE0E7FF)
                    )
                }
            }

            Button(
                onClick = onPayBill,
                enabled = card.currentBill > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pay_card_bill_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (card.currentBill > 0.0) "Mark Bill Paid (${CurrencyFormatter.formatInr(card.currentBill, privacyBlur)})"
                    else "Bill Already Paid"
                )
            }

            OutlinedButton(
                onClick = onAskChanakya,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RoyalBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ask Chanakya about Utilization & Rewards", color = RoyalBlue)
            }
        }
    }
}

@Composable
private fun AddCreditCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Double, String, Double) -> Unit
) {
    var cardName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var bill by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var dueLabel by remember { mutableStateOf("") }
    val parsedLimit = limit.toDoubleOrNull() ?: 0.0
    val isValid = cardName.isNotBlank() && bankName.isNotBlank() && parsedLimit > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Add Credit Card", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cardName, onValueChange = { cardName = it }, label = { Text("Card Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = last4, onValueChange = { last4 = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("Last 4") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(92.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bill,
                        onValueChange = { bill = it },
                        label = { Text("Bill (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = dueLabel, onValueChange = { dueLabel = it }, label = { Text("Due Date Label") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = (bill.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    if (isValid) {
                        onConfirm(
                            cardName.trim(),
                            bankName.trim(),
                            last4.ifBlank { "0000" },
                            b,
                            parsedLimit,
                            dueLabel.trim().ifBlank { "Due not set" },
                            if (b > 0.0) (b * 0.05) else 0.0
                        )
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
            ) {
                Text("Save Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun AddManualEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, TransactionType, SpendCategory, String) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(SpendCategory.FOOD_DINING) }
    val amt = amountText.toDoubleOrNull() ?: 0.0
    val isValid = merchant.isNotBlank() && amt > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Add Ledger Entry", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("Merchant / Biller") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(value = last4, onValueChange = { last4 = it.filter { ch -> ch.isDigit() }.take(4) }, label = { Text("Last 4") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(96.dp))
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TransactionType.entries) { t ->
                        FilterChip(selected = selectedType == t, onClick = { selectedType = t }, label = { Text(t.label) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SpendCategory.entries) { c ->
                        FilterChip(selected = selectedCategory == c, onClick = { selectedCategory = c }, label = { Text(c.label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(merchant.trim(), amt, selectedType, selectedCategory, last4.ifBlank { "0000" })
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
            ) { Text("Save Entry") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
