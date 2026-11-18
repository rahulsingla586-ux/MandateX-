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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.LoanEmiEntity
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

@Composable
fun LoansScreen(
    loans: List<LoanEmiEntity>,
    privacyBlur: Boolean,
    onLoanClick: (Long) -> Unit,
    onAddLoan: (String, String, String, Double, Int, Int, Int, Double) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val totalMonthlyEmi = remember(loans) { loans.sumOf { it.emiAmount } }
    val nextDueLoan = remember(loans) { loans.minByOrNull { it.nextDueDayOfMonth } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = RoyalBlue)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlueDeep)))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MONTHLY LOAN EMI TRACKER",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDBEAFE),
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "${loans.size} Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = CurrencyFormatter.formatInr(totalMonthlyEmi, privacyBlur),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nextDueLoan?.let {
                                "Next Due: ${it.loanName} on ${it.nextDueDayOfMonth}th (${CurrencyFormatter.formatInr(it.emiAmount, privacyBlur)})"
                            } ?: "No active EMIs scheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE0E7FF)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("add_loan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = RoyalBlue
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add New Loan / EMI")
                        }
                    }
                }
            }
        }

        items(loans, key = { it.id }) { loan ->
            val paidMonths = (loan.totalMonths - loan.remainingMonths).coerceAtLeast(0)
            val progress = if (loan.totalMonths > 0) paidMonths.toFloat() / loan.totalMonths.toFloat() else 0f

            Card(
                onClick = { onLoanClick(loan.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loan_item_${loan.id}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RoyalBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = RoyalBlue)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(loan.loanName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${loan.lenderName} • XX${loan.accountLast4} • ${loan.interestRate}% p.a.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.formatInr(loan.emiAmount, privacyBlur),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = RoyalBlue
                            )
                            Text("Due ${loan.nextDueDayOfMonth}th", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        color = EmeraldGreen,
                        trackColor = SurfaceMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$paidMonths / ${loan.totalMonths} EMIs paid (${loan.remainingMonths} left)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "View Schedule →",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLoanDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, lender, last4, emi, rem, total, due, rate ->
                onAddLoan(name, lender, last4, emi, rem, total, due, rate)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LoanDetailScreen(
    loan: LoanEmiEntity?,
    privacyBlur: Boolean,
    onBack: () -> Unit,
    onMarkEmiPaid: (LoanEmiEntity) -> Unit,
    onAskChanakya: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    if (loan == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Loan not found", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Back to Loans") }
        }
        return
    }

    val paidMonths = (loan.totalMonths - loan.remainingMonths).coerceAtLeast(0)
    val progress = if (loan.totalMonths > 0) paidMonths.toFloat() / loan.totalMonths.toFloat() else 0f
    val remainingPrincipalEst = loan.emiAmount * loan.remainingMonths

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("loan_detail_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Loans", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(loan.loanName, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("${loan.lenderName} • Account XX${loan.accountLast4}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("MONTHLY EMI", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = CurrencyFormatter.formatInr(loan.emiAmount, privacyBlur),
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                                color = RoyalBlue
                            )
                        }
                        Surface(
                            color = EmeraldSoft,
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.dp, EmeraldContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Due ${loan.nextDueDayOfMonth}th / mo", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        color = EmeraldGreen,
                        trackColor = SurfaceMuted
                    )

                    Text(
                        text = "$paidMonths of ${loan.totalMonths} installments completed • ${loan.remainingMonths} months remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Interest Rate: ${loan.interestRate}% p.a. • Est. Remaining Outflow: ${CurrencyFormatter.formatInr(remainingPrincipalEst, privacyBlur)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = TextPrimary
                    )
                    if (loan.lastPaidTimestamp != null) {
                        Text(
                            text = "Last Paid: ${CurrencyFormatter.formatDateOnly(loan.lastPaidTimestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onMarkEmiPaid(loan) },
                enabled = loan.remainingMonths > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("mark_emi_paid_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Record This Month EMI Paid (${CurrencyFormatter.formatInr(loan.emiAmount, privacyBlur)})")
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    onAskChanakya("${loan.loanName} (${loan.interestRate}% rate, ${loan.remainingMonths} months left) ke liye smart prepayment tip do")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ask_chanakya_loan_button"),
                border = BorderStroke(1.dp, RoyalBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RoyalBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ask Chanakya AI for Prepayment Tips", color = RoyalBlue)
            }
        }
    }
}

@Composable
private fun AddLoanDialog(
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
                    OutlinedTextField(value = lender, onValueChange = { lender = it }, label = { Text("Bank / NBFC") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = last4, onValueChange = { last4 = it.filter { ch -> ch.isDigit() }.take(4) }, label = { Text("Last 4") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(92.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emiText,
                        onValueChange = { emiText = it },
                        label = { Text("EMI (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthsLeft,
                        onValueChange = { monthsLeft = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Months") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = { dueDay = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Due Day (1-28)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        label = { Text("Rate %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
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
            ) {
                Text("Save Loan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
