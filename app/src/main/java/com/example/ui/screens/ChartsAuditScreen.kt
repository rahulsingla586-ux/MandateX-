package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.data.model.BudgetStatus
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import java.util.Calendar

enum class ChartTimeRange(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month")
}

enum class ChartBucket(
    val label: String,
    val color: Color,
    val isEssential: Boolean
) {
    FOOD("Food", Color(0xFFF59E0B), false),
    SHOPPING("Shopping", Color(0xFF6366F1), false),
    LOANS_EMI("Loans & EMI", Color(0xFF1D4ED8), true),
    BILLS("Bills", Color(0xFF059669), true),
    AUTOPAY("AutoPay", Color(0xFFDC2626), false)
}

private fun TransactionEntity.toChartBucket(): ChartBucket {
    return when {
        type == TransactionType.CARD_EMI || category == SpendCategory.CARD_EMI -> ChartBucket.LOANS_EMI
        type == TransactionType.AUTOPAY || category == SpendCategory.OTT_APPS || isTrial -> ChartBucket.AUTOPAY
        category == SpendCategory.FOOD_DINING -> ChartBucket.FOOD
        category == SpendCategory.UTILITIES -> ChartBucket.BILLS
        else -> ChartBucket.SHOPPING
    }
}

@Composable
fun ChartsAuditScreen(
    transactions: List<TransactionEntity>,
    budgetStatuses: List<BudgetStatus>,
    privacyBlur: Boolean,
    onUpdateBudget: (SpendCategory, Double) -> Unit,
    onAskChanakya: (String) -> Unit,
    onStartServiceCancellation: (TransactionEntity) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    var selectedRange by remember { mutableStateOf(ChartTimeRange.MONTH) }
    var selectedBucket by remember { mutableStateOf<ChartBucket?>(null) }
    var editingBudget by remember { mutableStateOf<BudgetStatus?>(null) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var selectedTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val cutoffMs = remember(selectedRange) {
        val cal = Calendar.getInstance()
        when (selectedRange) {
            ChartTimeRange.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            ChartTimeRange.WEEK -> System.currentTimeMillis() - 7L * 86_400_000L
            ChartTimeRange.MONTH -> System.currentTimeMillis() - 30L * 86_400_000L
        }
    }

    val filteredTx = remember(transactions, cutoffMs) {
        transactions.filter { !it.isCancelled && it.timestamp >= cutoffMs }
    }

    val bucketTotals = remember(filteredTx) {
        ChartBucket.entries.map { bucket ->
            val matching = filteredTx.filter { it.toChartBucket() == bucket }
            Triple(bucket, matching.sumOf { it.amount }, matching.size)
        }
    }
    val nonZeroBuckets = remember(bucketTotals) { bucketTotals.filter { it.second > 0.0 } }
    val totalSpend = remember(bucketTotals) { bucketTotals.sumOf { it.second } }

    val essentialTotal = remember(bucketTotals) {
        bucketTotals.filter { it.first.isEssential }.sumOf { it.second }
    }
    val faltuTotal = remember(bucketTotals) {
        bucketTotals.filter { !it.first.isEssential }.sumOf { it.second }
    }
    val essentialRatio = if (totalSpend > 0.0) (essentialTotal / totalSpend).toFloat().coerceIn(0f, 1f) else 0f
    val faltuPct = if (totalSpend > 0.0) ((faltuTotal / totalSpend) * 100.0).toInt() else 0

    val displayedTransactions = remember(filteredTx, selectedBucket) {
        if (selectedBucket == null) filteredTx
        else filteredTx.filter { it.toChartBucket() == selectedBucket }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Time Range Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Visual Intelligence & Audits",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap any category slice to inspect transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Icon(Icons.Filled.DonutLarge, contentDescription = null, tint = RoyalBlue)
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ChartTimeRange.entries) { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chart_filter_${range.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 1. Interactive Donut Chart Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("charts_donut_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expense Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedBucket != null) {
                            TextButton(onClick = { selectedBucket = null }) {
                                Text("Reset Slice", color = RoyalBlue)
                            }
                        }
                    }

                    val activeSliceTotal = selectedBucket?.let { sel ->
                        bucketTotals.find { it.first == sel }?.second ?: 0.0
                    } ?: totalSpend

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(186.dp)) {
                            val baseStroke = 26.dp.toPx()
                            if (totalSpend <= 0.0) {
                                drawArc(
                                    color = SurfaceMuted,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = baseStroke)
                                )
                            } else {
                                var startAngle = -90f
                                nonZeroBuckets.forEach { (bucket, amount, _) ->
                                    val sweep = ((amount / totalSpend) * 360.0).toFloat()
                                    val isSelected = selectedBucket == null || selectedBucket == bucket
                                    val strokeWidth = if (selectedBucket == bucket) baseStroke + 8.dp.toPx() else baseStroke
                                    val inset = strokeWidth / 2f
                                    drawArc(
                                        color = if (isSelected) bucket.color else bucket.color.copy(alpha = 0.25f),
                                        startAngle = startAngle + 1.5f,
                                        sweepAngle = (sweep - 3f).coerceAtLeast(2f),
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = selectedBucket?.label ?: selectedRange.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = CurrencyFormatter.formatInr(activeSliceTotal, privacyBlur),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = selectedBucket?.color ?: TextPrimary
                            )
                            val pctText = if (totalSpend > 0.0) {
                                "${((activeSliceTotal / totalSpend) * 100.0).toInt()}% of total"
                            } else {
                                "0 entries"
                            }
                            Text(
                                text = pctText,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    // Interactive Category Legend Rows
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bucketTotals.forEach { (bucket, amount, count) ->
                            val pct = if (totalSpend > 0.0) ((amount / totalSpend) * 100.0).toInt() else 0
                            val isSelected = selectedBucket == bucket
                            Surface(
                                onClick = {
                                    selectedBucket = if (selectedBucket == bucket) null else bucket
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) bucket.color.copy(alpha = 0.10f) else SurfaceMuted.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, if (isSelected) bucket.color else BorderLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chart_legend_${bucket.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clip(CircleShape)
                                                .background(bucket.color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = bucket.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "($count)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = CurrencyFormatter.formatInr(amount, privacyBlur),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = bucket.color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Essential vs Faltu Spend Ratio Progress Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("essential_vs_faltu_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Essential vs Faltu Spend Ratio",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (faltuPct > 25) AlertRedContainer else EmeraldContainer,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "$faltuPct% Faltu",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (faltuPct > 25) AlertRed else EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Dual split progress bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (totalSpend > 0.0) AlertRed.copy(alpha = 0.25f) else SurfaceMuted)
                    ) {
                        if (totalSpend > 0.0) {
                            if (essentialRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(essentialRatio.coerceAtLeast(0.01f))
                                        .height(14.dp)
                                        .background(EmeraldGreen)
                                )
                            }
                            val faltuWeight = (1f - essentialRatio)
                            if (faltuWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(faltuWeight.coerceAtLeast(0.01f))
                                        .height(14.dp)
                                        .background(AlertRed)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Essential (EMIs + Bills)", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            Text(
                                text = CurrencyFormatter.formatInr(essentialTotal, privacyBlur),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Faltu / Discretionary", style = MaterialTheme.typography.labelSmall, color = AlertRed, fontWeight = FontWeight.Bold)
                            Text(
                                text = CurrencyFormatter.formatInr(faltuTotal, privacyBlur),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                                color = AlertRed
                            )
                        }
                    }

                    Surface(
                        color = if (totalSpend <= 0.0) SurfaceMuted else if (faltuPct > 25) AlertRedContainer.copy(alpha = 0.45f) else EmeraldSoft,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when {
                                totalSpend <= 0.0 ->
                                    "No expenses recorded in this period yet. Add entries or upload a statement PDF/screenshot to inspect your Essential vs Faltu ratio."
                                faltuPct > 25 ->
                                    "Chanakya Audit: ${faltuPct}% kharcha Food, Shopping aur OTT AutoPay par ja raha hai — non-essential spends 20% ke neeche laao!"
                                else ->
                                    "Chanakya Audit: Badhiya! Aapka Essential vs Faltu ratio disciplined hai. Extra cash EMI prepayment mein lagao."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onAskChanakya("Mera ${selectedRange.label} expense audit karo: Essential ₹${essentialTotal.toInt()} vs Faltu ₹${faltuTotal.toInt()} — kaha cut karu?")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ask_chanakya_charts_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RoyalBlue)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Roast & Audit My Spend with Chanakya AI", color = RoyalBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Monthly Spending Cap Tracker
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monthly Spending Cap Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = { showAddBudgetDialog = true },
                    modifier = Modifier.testTag("charts_add_cap_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Cap", color = RoyalBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (budgetStatuses.isEmpty()) {
            item {
                Surface(
                    onClick = { showAddBudgetDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No monthly spending caps set", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap + Add Cap to monitor 80% / 100% category alerts.", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                    }
                }
            }
        }

        items(budgetStatuses, key = { "chart_budget_${it.category.name}" }) { status ->
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
                    if (status.isExceeded100 || status.isWarning80) progressColor.copy(alpha = 0.55f) else BorderLight
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
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chart_edit_budget_${status.category.name}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit ${status.category.label} cap", tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { status.percentage.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        color = progressColor,
                        trackColor = SurfaceMuted
                    )
                    if (status.isExceeded100 || status.isWarning80) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (status.isExceeded100) "Limit Crossed! Cap breached." else "Warning: Crossed 80% monthly budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = progressColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 4. Filtered Slice Transactions
        if (displayedTransactions.isNotEmpty()) {
            item {
                Text(
                    text = selectedBucket?.let { "${it.label} Transactions (${displayedTransactions.size})" }
                        ?: "Recent ${selectedRange.label} Entries (${displayedTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            items(displayedTransactions.take(8), key = { "chart_tx_${it.id}" }) { tx ->
                TransactionRowCard(
                    tx = tx,
                    privacyBlur = privacyBlur,
                    onClick = { selectedTx = tx }
                )
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
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Limit (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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

    if (showAddBudgetDialog) {
        var selectedCat by remember { mutableStateOf(SpendCategory.FOOD_DINING) }
        var limitText by remember { mutableStateOf("") }
        val parsed = limitText.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            containerColor = CardWhite,
            title = { Text("Add Monthly Spending Cap", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SpendCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCat == cat,
                                onClick = { selectedCat = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monthly Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parsed > 0.0) {
                            onUpdateBudget(selectedCat, parsed)
                            showAddBudgetDialog = false
                        }
                    },
                    enabled = parsed > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
                ) { Text("Save Cap") }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) { Text("Cancel", color = TextMuted) }
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
