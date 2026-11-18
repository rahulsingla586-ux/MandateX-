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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CreditCardEntity
import com.example.data.model.LoanEmiEntity
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.engine.DebtStrategyCalculator
import com.example.engine.DebtStrategyMode
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.components.TransactionRowCard
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
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.UpiApp
import com.example.util.UpiAutoPayIntents

@Composable
fun DebtsCommandScreen(
    loans: List<LoanEmiEntity>,
    creditCards: List<CreditCardEntity>,
    transactions: List<TransactionEntity>,
    privacyBlur: Boolean,
    onLoanClick: (Long) -> Unit,
    onAddLoan: (String, String, String, Double, Int, Int, Int, Double) -> Unit,
    onPayCardBill: (CreditCardEntity) -> Unit,
    onAddCreditCard: (String, String, String, Double, Double, String, Double) -> Unit,
    onAskChanakya: (String) -> Unit,
    onStartServiceCancellation: (TransactionEntity) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    var strategyMode by remember { mutableStateOf(DebtStrategyMode.AVALANCHE) }
    var extraMonthly by remember { mutableDoubleStateOf(5000.0) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var selectedTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val totalCardDue = remember(creditCards) { creditCards.sumOf { it.currentBill } }
    val totalLoanOutstanding = remember(loans) { loans.sumOf { it.emiAmount * it.remainingMonths } }
    val totalMonthlyEmi = remember(loans) { loans.sumOf { it.emiAmount } }
    val totalLimit = remember(creditCards) { creditCards.sumOf { it.creditLimit } }
    val overallCurPct = if (totalLimit > 0.0) ((totalCardDue / totalLimit) * 100.0).toInt() else 0

    val strategyPlan = remember(loans, creditCards, extraMonthly, strategyMode) {
        DebtStrategyCalculator.buildStrategy(
            loans = loans,
            creditCards = creditCards,
            extraMonthlyPayment = extraMonthly,
            mode = strategyMode
        )
    }

    val cardAndEmiTxs = remember(transactions) {
        transactions.filter { it.type == TransactionType.CARD_EMI }.take(6)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Debt & Credit Hero Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debts_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBlue)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlueDeep)))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DEBT & CREDIT COMMAND CENTER",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDBEAFE),
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "CUR $overallCurPct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Credit Cards Due", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDBEAFE))
                                Text(
                                    text = CurrencyFormatter.formatInr(totalCardDue, privacyBlur),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Loans Outstanding", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDBEAFE))
                                Text(
                                    text = CurrencyFormatter.formatInr(totalLoanOutstanding, privacyBlur),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = "Monthly EMI Outflow: ${CurrencyFormatter.formatInr(totalMonthlyEmi, privacyBlur)} across ${loans.size} loans & ${creditCards.size} cards",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE0E7FF)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showAddCardDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("debts_add_card_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = RoyalBlue
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Card", maxLines = 1)
                            }
                            Button(
                                onClick = { showAddLoanDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("debts_add_loan_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Loan", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // 2. Chanakya's Debt Elimination Strategy (Avalanche vs Snowball)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_strategy_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chanakya's Debt Elimination Strategy",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Compare interest savings vs quick psychological wins",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RoyalBlue)
                    }

                    // Interactive Avalanche vs Snowball Segmented Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceMuted)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DebtStrategyMode.entries.forEach { mode ->
                            val selected = strategyMode == mode
                            Surface(
                                onClick = { strategyMode = mode },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) RoyalBlue else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("strategy_toggle_${mode.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color(0xFFDBEAFE) else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Extra Monthly Prepay Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Extra Monthly Prepayment Boost",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(0.0, 2000.0, 5000.0, 10000.0)) { extra ->
                                FilterChip(
                                    selected = extraMonthly == extra,
                                    onClick = { extraMonthly = extra },
                                    label = {
                                        Text(
                                            if (extra == 0.0) "Minimum Only"
                                            else "+${CurrencyFormatter.formatInr(extra, privacyBlur)}/mo"
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Exact Interest Saved & Time Shaved Banner
                    Surface(
                        color = if (strategyPlan.orderedDebts.isEmpty()) SurfaceMuted else EmeraldSoft,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (strategyPlan.orderedDebts.isEmpty()) BorderLight else EmeraldContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimated Interest Saved",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (strategyPlan.orderedDebts.isEmpty()) TextSecondary else EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(strategyPlan.interestSaved, privacyBlur),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (strategyPlan.orderedDebts.isEmpty()) TextPrimary else EmeraldGreen
                                )
                            }
                            if (strategyPlan.orderedDebts.isEmpty()) {
                                Text(
                                    text = "Add a credit card balance or loan to calculate Avalanche vs Snowball interest savings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            } else {
                                Text(
                                    text = "Debt-free in ${strategyPlan.strategyMonths} months (${strategyPlan.monthsSaved} months faster than baseline ${strategyPlan.baselineMonths} mo).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                strategyPlan.targetFirstDebt?.let { first ->
                                    Text(
                                        text = if (strategyMode == DebtStrategyMode.AVALANCHE) {
                                            "Target #1 (Highest APR ${first.aprPercent}%): ${first.name}"
                                        } else {
                                            "Target #1 (Smallest Balance): ${first.name} — quick win!"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Priority Payoff Order
                    if (strategyPlan.orderedDebts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Recommended Payoff Order",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            strategyPlan.orderedDebts.take(4).forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceMuted.copy(alpha = 0.6f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (idx == 0) RoyalBlue else RoyalBlueSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (idx == 0) Color.White else RoyalBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.aprPercent}% APR • ${CurrencyFormatter.formatInr(item.monthlyCommitment, privacyBlur)}/mo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatInr(item.balance, privacyBlur),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onAskChanakya(
                                "Meri ${strategyMode.title} debt plan +₹${extraMonthly.toInt()}/mo extra prepayment par step-by-step roadmap do"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("ask_chanakya_debt_strategy_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RoyalBlue)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Chanakya AI for Custom Debt Plan", color = RoyalBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Individual Credit Cards with CUR Watchdog Warnings
        item {
            Text(
                text = "Credit Cards & CUR Watchdog",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        if (creditCards.isEmpty()) {
            item {
                Surface(
                    onClick = { showAddCardDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No credit cards added yet", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap + Add Card or upload a statement PDF to track CUR.", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                    }
                }
            }
        }

        items(creditCards, key = { "debt_card_${it.id}" }) { card ->
            val util = if (card.creditLimit > 0.0) (card.currentBill / card.creditLimit).toFloat().coerceIn(0f, 1f) else 0f
            val utilPct = (util * 100).toInt()
            val curColor = when {
                card.isPaid || card.currentBill <= 0.0 -> EmeraldGreen
                utilPct >= 50 -> AlertRed
                utilPct >= 30 -> GoldAmber
                else -> EmeraldGreen
            }
            val safeTargetBill = card.creditLimit * 0.28
            val paydownNeeded = (card.currentBill - safeTargetBill).coerceAtLeast(0.0)

            Card(
                onClick = { selectedCard = card },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_credit_card_${card.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(
                    1.dp,
                    if (utilPct >= 30 && card.currentBill > 0.0) curColor.copy(alpha = 0.55f) else BorderLight
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.CreditCard, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(card.cardName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${card.bankName} • XX${card.last4}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.formatInr(card.currentBill, privacyBlur),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = if (card.isPaid || card.currentBill <= 0.0) "PAID" else card.dueDateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = curColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { util },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        color = curColor,
                        trackColor = SurfaceMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Limit: ${CurrencyFormatter.formatInr(card.creditLimit, privacyBlur)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "CUR: $utilPct%",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = curColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // CUR Watchdog Warning Strip
                    Surface(
                        color = when {
                            utilPct >= 50 && card.currentBill > 0.0 -> AlertRedContainer.copy(alpha = 0.5f)
                            utilPct >= 30 && card.currentBill > 0.0 -> GoldWalletBg
                            else -> EmeraldSoft
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (utilPct >= 30 && card.currentBill > 0.0) Icons.Filled.WarningAmber else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = curColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    card.currentBill <= 0.0 || card.isPaid ->
                                        "CUR Watchdog: 0% utilization — maximum CIBIL boost active."
                                    utilPct >= 50 ->
                                        "CUR Watchdog CRITICAL ($utilPct%): Pay ${CurrencyFormatter.formatInr(paydownNeeded, privacyBlur)} before statement date to drop <30%!"
                                    utilPct >= 30 ->
                                        "CUR Watchdog Warning ($utilPct%): Crossed 30% safe zone. Pay ${CurrencyFormatter.formatInr(paydownNeeded, privacyBlur)} to protect CIBIL."
                                    else ->
                                        "CUR Watchdog: Healthy $utilPct% (<30% safe CIBIL zone)."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 4. Active Loan EMIs
        item {
            Text(
                text = "Active Loans & EMIs",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        if (loans.isEmpty()) {
            item {
                Surface(
                    onClick = { showAddLoanDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No loans or EMIs added yet", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap + Add Loan or upload a loan/bank statement PDF.", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                    }
                }
            }
        }

        items(loans, key = { "debt_loan_${it.id}" }) { loan ->
            val paidMonths = (loan.totalMonths - loan.remainingMonths).coerceAtLeast(0)
            val progress = if (loan.totalMonths > 0) paidMonths.toFloat() / loan.totalMonths.toFloat() else 0f
            Card(
                onClick = { onLoanClick(loan.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_loan_${loan.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight)
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RoyalBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(loan.loanName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${loan.lenderName} • XX${loan.accountLast4} • ${loan.interestRate}% p.a.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.formatInr(loan.emiAmount, privacyBlur),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                                color = RoyalBlue
                            )
                            Text("Due ${loan.nextDueDayOfMonth}th", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        color = EmeraldGreen,
                        trackColor = SurfaceMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$paidMonths/${loan.totalMonths} EMIs paid (${loan.remainingMonths} left)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("Schedule & Pay →", style = MaterialTheme.typography.labelSmall, color = RoyalBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Recent Card & EMI Passbook Transactions
        if (cardAndEmiTxs.isNotEmpty()) {
            item {
                HorizontalDivider(color = BorderLight)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Recent Card & EMI Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(cardAndEmiTxs, key = { "debt_tx_${it.id}" }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    privacyBlur = privacyBlur,
                    onClick = { selectedTx = tx }
                )
            }
        }
    }

    if (showAddLoanDialog) {
        DebtAddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { n, l, l4, e, rem, tot, d, r ->
                onAddLoan(n, l, l4, e, rem, tot, d, r)
                showAddLoanDialog = false
            }
        )
    }

    if (showAddCardDialog) {
        DebtAddCardDialog(
            onDismiss = { showAddCardDialog = false },
            onConfirm = { n, b, l4, bill, lim, due, min ->
                onAddCreditCard(n, b, l4, bill, lim, due, min)
                showAddCardDialog = false
            }
        )
    }

    selectedCard?.let { card ->
        DebtCardDetailSheet(
            card = card,
            privacyBlur = privacyBlur,
            onPayFull = {
                onPayCardBill(card)
                selectedCard = null
            },
            onAskChanakya = {
                selectedCard = null
                onAskChanakya("${card.cardName} (XX${card.last4}) bill ₹${card.currentBill.toInt()} aur credit utilization par CIBIL tip do")
            },
            onDismiss = { selectedCard = null }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtCardDetailSheet(
    card: CreditCardEntity,
    privacyBlur: Boolean,
    onPayFull: () -> Unit,
    onAskChanakya: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${card.cardName} (XX${card.last4})", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("${card.bankName} • ${card.dueDateLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = "Current Bill: ${CurrencyFormatter.formatInr(card.currentBill, privacyBlur)}",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                color = RoyalBlue
            )
            Text(
                text = "Min Due: ${CurrencyFormatter.formatInr(card.minDue, privacyBlur)} • Limit: ${CurrencyFormatter.formatInr(card.creditLimit, privacyBlur)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Button(
                onClick = onPayFull,
                enabled = !card.isPaid && card.currentBill > 0.0,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (card.isPaid || card.currentBill <= 0.0) "Bill Paid" else "Mark Full Bill Paid")
            }
            OutlinedButton(
                onClick = { UpiAutoPayIntents.openUpiAppAutoPay(context, UpiApp.GPAY) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Pay via Google Pay / Bank App", color = RoyalBlue)
            }
            OutlinedButton(
                onClick = onAskChanakya,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                border = BorderStroke(1.dp, RoyalBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Ask Chanakya AI about CUR", color = RoyalBlue)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DebtAddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Int, Int, Int, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lender by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var emiText by remember { mutableStateOf("") }
    var monthsLeft by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }

    val parsedEmi = emiText.toDoubleOrNull() ?: 0.0
    val isValid = name.isNotBlank() && lender.isNotBlank() && parsedEmi > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Add Loan / EMI", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Loan Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lender, onValueChange = { lender = it }, label = { Text("Lender") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = last4, onValueChange = { last4 = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("Last 4") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(92.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = emiText, onValueChange = { emiText = it }, label = { Text("EMI (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = monthsLeft, onValueChange = { monthsLeft = it.filter { c -> c.isDigit() } }, label = { Text("Months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(92.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dueDay, onValueChange = { dueDay = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Due Day") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("APR %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.width(92.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rem = (monthsLeft.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val day = (dueDay.toIntOrNull() ?: 1).coerceIn(1, 28)
                    val r = (rate.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    if (isValid) {
                        onConfirm(name.trim(), lender.trim(), last4.ifBlank { "0000" }, parsedEmi, rem, rem, day, r)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun DebtAddCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Double, String, Double) -> Unit
) {
    var cardName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var billText by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var dueLabel by remember { mutableStateOf("") }

    val parsedLimit = limitText.toDoubleOrNull() ?: 0.0
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
                    OutlinedTextField(value = billText, onValueChange = { billText = it }, label = { Text("Bill (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text("Limit (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = dueLabel, onValueChange = { dueLabel = it }, label = { Text("Due Date") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bill = (billText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    if (isValid) {
                        onConfirm(
                            cardName.trim(),
                            bankName.trim(),
                            last4.ifBlank { "0000" },
                            bill,
                            parsedLimit,
                            dueLabel.trim().ifBlank { "Due not set" },
                            if (bill > 0.0) (bill * 0.05) else 0.0
                        )
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
            ) { Text("Save Card") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
