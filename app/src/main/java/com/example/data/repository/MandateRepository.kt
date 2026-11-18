package com.example.data.repository

import android.content.Context
import com.example.ai.ParsedGmailMandate
import com.example.data.local.MandateDao
import com.example.data.model.BudgetStatus
import com.example.data.model.CancellationRequestEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.LoanEmiEntity
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.preferences.UserPreferencesRepository
import com.example.engine.BankMessageParser
import com.example.engine.TrialShieldScheduler
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.Locale

class MandateRepository(
    private val context: Context,
    private val dao: MandateDao,
    private val prefs: UserPreferencesRepository
) {
    val allTransactions: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val activeTrials: Flow<List<TransactionEntity>> = dao.observeActiveTrials()
    val allBudgets: Flow<List<CategoryBudgetEntity>> = dao.observeAllBudgets()
    val cancellations: Flow<List<CancellationRequestEntity>> = dao.observeCancellations()
    val loans: Flow<List<LoanEmiEntity>> = dao.observeLoans()
    val creditCards: Flow<List<CreditCardEntity>> = dao.observeCreditCards()

    val duplicateAlerts: Flow<List<DuplicateChargeAlert>> = allTransactions.combine(allBudgets) { txs, _ ->
        detectDuplicates(txs)
    }

    val budgetStatuses: Flow<List<BudgetStatus>> = allTransactions.combine(allBudgets) { txs, budgets ->
        computeBudgetStatuses(txs, budgets)
    }

    suspend fun purgeLegacyDemoIfNeeded() {
        val hadLegacyDemo = prefs.consumeLegacyDemoPurgeFlag()
        if (hadLegacyDemo) {
            clearAllData()
        }
    }

    suspend fun ingestMessage(
        sender: String,
        body: String,
        source: String,
        timestamp: Long = System.currentTimeMillis()
    ): TransactionEntity? {
        val parsed = BankMessageParser.parse(sender, body, source, timestamp) ?: return null
        val id = dao.insertTransaction(parsed)
        val saved = parsed.copy(id = id)

        if (saved.isTrial) {
            TrialShieldScheduler.scheduleTrialExitShield(context, saved)
            dao.upsertCancellation(
                CancellationRequestEntity(
                    merchantName = saved.merchantName,
                    subtitle = "Trial Exit Shield (₹${saved.amount.toInt()} -> ₹${(saved.postTrialAmount ?: 499.0).toInt()}/mo)",
                    upiId = saved.upiId ?: "${saved.merchantName.lowercase().replace(" ", "")}@upi",
                    bankName = saved.bankName,
                    accountLast4 = saved.accountLast4,
                    amount = saved.postTrialAmount ?: 499.0,
                    status = "IN PROGRESS",
                    currentStep = 1,
                    notes = "Auto-detected trial mandate. Revoke before full-price auto-debit."
                )
            )
        }

        val statuses = computeBudgetStatuses(
            dao.getAllTransactionsSnapshot(),
            dao.getAllBudgetsSnapshot()
        )
        statuses.find { it.category == saved.category }?.let { status ->
            if (status.isWarning80 || status.isExceeded100) {
                TrialShieldScheduler.showBudgetWarningNotification(
                    context = context,
                    categoryLabel = status.category.label,
                    spent = status.spent,
                    limit = status.limit,
                    percent = (status.percentage * 100).toInt()
                )
            }
        }
        return saved
    }

    /**
     * Merges Gemini-parsed Gmail mandates/subscriptions into Room while deduplicating against existing SMS/Gmail records.
     * Returns Pair(newInsertedCount, mergedWithExistingSmsCount).
     */
    suspend fun ingestGmailMandates(mandates: List<ParsedGmailMandate>): Pair<Int, Int> {
        var insertedCount = 0
        var mergedCount = 0
        val existing = dao.getAllTransactionsSnapshot().toMutableList()
        val threeDaysMs = 3L * 86_400_000L

        for (item in mandates) {
            val extId = "gmail_${item.messageId}"
            if (dao.countByExternalMessageId(extId) > 0) continue

            val normName = normalizeMerchant(item.serviceName)
            val match = existing.firstOrNull { tx ->
                val txNorm = normalizeMerchant(tx.merchantName)
                val sameMerchant = txNorm.contains(normName) || normName.contains(txNorm)
                val sameAmount = kotlin.math.abs(tx.amount - item.amountInr) <= 2.0
                val closeTime = kotlin.math.abs(tx.timestamp - item.emailTimestampMillis) <= threeDaysMs
                sameMerchant && sameAmount && closeTime
            }

            if (match != null) {
                val updated = match.copy(
                    source = if (match.source.contains("GMAIL")) match.source else "${match.source} + GMAIL",
                    mandateRefId = match.mandateRefId ?: item.mandateRefId,
                    nextDueDateLabel = match.nextDueDateLabel ?: item.nextDueDate,
                    paymentSource = match.paymentSource.ifBlank { item.paymentSource },
                    externalMessageId = extId
                )
                dao.updateTransaction(updated)
                mergedCount++
            } else {
                val last4 = Regex("""(\d{4})""").find(item.paymentSource)?.groupValues?.get(1) ?: "GMAIL"
                val trialExpiry = if (item.isTrial) {
                    System.currentTimeMillis() + 44L * 3_600_000L
                } else null

                val entity = TransactionEntity(
                    amount = item.amountInr,
                    merchantName = item.serviceName,
                    type = item.transactionType,
                    category = item.spendCategory,
                    accountLast4 = last4,
                    bankName = "Gmail Sync",
                    timestamp = item.emailTimestampMillis,
                    rawMessage = "${item.summary}\n${item.rawEmailSnippet}".trim(),
                    source = "GMAIL",
                    isTrial = item.isTrial,
                    trialExpiryTimestamp = trialExpiry,
                    postTrialAmount = item.postTrialAmountInr,
                    mandateRefId = item.mandateRefId,
                    nextDueDateLabel = item.nextDueDate,
                    paymentSource = item.paymentSource,
                    externalMessageId = extId
                )
                val newId = dao.insertTransaction(entity)
                val saved = entity.copy(id = newId)
                existing.add(saved)
                insertedCount++

                if (saved.isTrial) {
                    TrialShieldScheduler.scheduleTrialExitShield(context, saved)
                    dao.upsertCancellation(
                        CancellationRequestEntity(
                            merchantName = saved.merchantName,
                            subtitle = "Gmail Trial Alert (₹${saved.amount.toInt()} -> ₹${(saved.postTrialAmount ?: 499.0).toInt()}/mo)",
                            upiId = "${saved.merchantName.lowercase().replace(" ", "")}@upi",
                            bankName = saved.paymentSource,
                            accountLast4 = saved.accountLast4,
                            amount = saved.postTrialAmount ?: 499.0,
                            status = "IN PROGRESS",
                            currentStep = 1,
                            notes = "Hidden trial discovered by Chanakya AI in Gmail."
                        )
                    )
                }
            }
        }
        return insertedCount to mergedCount
    }

    /**
     * Persists structured transactions, credit cards, and loan EMIs parsed by Chanakya AI
     * from an uploaded bank PDF or payment screenshot into local Room storage.
     * Returns Triple(insertedTxCount, upsertedCardsCount, upsertedLoansCount).
     */
    suspend fun ingestParsedAttachmentData(
        transactions: List<TransactionEntity>,
        creditCards: List<CreditCardEntity>,
        loans: List<LoanEmiEntity>
    ): Triple<Int, Int, Int> {
        var txCount = 0
        var cardCount = 0
        var loanCount = 0

        for (tx in transactions) {
            val extId = tx.externalMessageId
            val dupByExt = !extId.isNullOrBlank() && dao.countByExternalMessageId(extId) > 0
            val dupByRaw = tx.rawMessage.isNotBlank() && dao.countByRawMessage(tx.rawMessage) > 0
            if (dupByExt || dupByRaw) continue

            val newId = dao.insertTransaction(tx)
            val saved = tx.copy(id = newId)
            txCount++

            if (saved.isTrial && !saved.isCancelled) {
                TrialShieldScheduler.scheduleTrialExitShield(context, saved)
                dao.upsertCancellation(
                    CancellationRequestEntity(
                        merchantName = saved.merchantName,
                        subtitle = "Document Trial Alert (₹${saved.amount.toInt()} -> ₹${(saved.postTrialAmount ?: 499.0).toInt()}/mo)",
                        upiId = saved.upiId ?: "${saved.merchantName.lowercase().replace(" ", "")}@upi",
                        bankName = saved.bankName,
                        accountLast4 = saved.accountLast4,
                        amount = saved.postTrialAmount ?: 499.0,
                        status = "IN PROGRESS",
                        currentStep = 1,
                        notes = "Trial detected in uploaded statement/screenshot. Revoke before full-price renewal."
                    )
                )
            }
        }

        val existingCards = dao.getCreditCardsSnapshot()
        for (card in creditCards) {
            val existingMatch = existingCards.firstOrNull {
                it.cardName.equals(card.cardName, ignoreCase = true) ||
                    (card.last4 != "0000" && it.last4 == card.last4)
            }
            val toSave = if (existingMatch != null) {
                card.copy(id = existingMatch.id)
            } else {
                card
            }
            dao.upsertCreditCard(toSave)
            cardCount++
        }

        val existingLoans = dao.getLoansSnapshot()
        for (loan in loans) {
            val existingMatch = existingLoans.firstOrNull {
                it.loanName.equals(loan.loanName, ignoreCase = true) ||
                    (loan.accountLast4 != "0000" && it.accountLast4 == loan.accountLast4)
            }
            val toSave = if (existingMatch != null) {
                loan.copy(id = existingMatch.id)
            } else {
                loan
            }
            dao.upsertLoan(toSave)
            loanCount++
        }

        return Triple(txCount, cardCount, loanCount)
    }

    suspend fun addManualTransaction(tx: TransactionEntity): TransactionEntity {
        val id = dao.insertTransaction(tx)
        val saved = tx.copy(id = id)
        if (saved.isTrial) {
            TrialShieldScheduler.scheduleTrialExitShield(context, saved)
        }
        return saved
    }

    suspend fun markTrialCancelled(trial: TransactionEntity) {
        val saved = (trial.postTrialAmount ?: 499.0) - trial.amount
        dao.markTrialCancelled(trial.id, savedAmount = maxOf(0.0, saved))
        // Also update matching cancellation tracker if present
        dao.upsertCancellation(
            CancellationRequestEntity(
                merchantName = trial.merchantName,
                subtitle = "Trial Exit Shield Cancelled",
                upiId = trial.upiId ?: "autopay@upi",
                bankName = trial.bankName,
                accountLast4 = trial.accountLast4,
                amount = trial.postTrialAmount ?: 499.0,
                status = "COMPLETED",
                currentStep = 4,
                notes = "Cancelled before auto-renewal. Saved ₹${saved.toInt()}."
            )
        )
    }

    suspend fun updateCancellationStep(item: CancellationRequestEntity, nextStep: Int, newStatus: String) {
        dao.upsertCancellation(
            item.copy(
                currentStep = nextStep.coerceIn(1, 4),
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addCancellationRequest(item: CancellationRequestEntity): Long =
        dao.upsertCancellation(item)

    suspend fun payLoanEmi(loan: LoanEmiEntity) {
        val updatedMonths = (loan.remainingMonths - 1).coerceAtLeast(0)
        dao.upsertLoan(
            loan.copy(
                remainingMonths = updatedMonths,
                lastPaidTimestamp = System.currentTimeMillis()
            )
        )
        dao.insertTransaction(
            TransactionEntity(
                amount = loan.emiAmount,
                merchantName = loan.loanName,
                type = TransactionType.CARD_EMI,
                category = SpendCategory.CARD_EMI,
                accountLast4 = loan.accountLast4,
                bankName = loan.lenderName,
                source = "EMI_PAYMENT",
                rawMessage = "Recorded EMI payment of ₹${loan.emiAmount.toInt()} for ${loan.loanName} XX${loan.accountLast4}"
            )
        )
    }

    suspend fun addLoan(loan: LoanEmiEntity) {
        dao.upsertLoan(loan)
    }

    suspend fun payCreditCardBill(card: CreditCardEntity) {
        dao.upsertCreditCard(
            card.copy(
                currentBill = 0.0,
                isPaid = true
            )
        )
        dao.insertTransaction(
            TransactionEntity(
                amount = card.currentBill,
                merchantName = "${card.cardName} Bill",
                type = TransactionType.CARD_EMI,
                category = SpendCategory.CARD_EMI,
                accountLast4 = card.last4,
                bankName = card.bankName,
                source = "CARD_PAYMENT",
                rawMessage = "Paid Credit Card bill ₹${card.currentBill.toInt()} for ${card.cardName} XX${card.last4}"
            )
        )
    }

    suspend fun addCreditCard(card: CreditCardEntity) {
        dao.upsertCreditCard(card)
    }

    suspend fun updateCategoryBudget(category: SpendCategory, limit: Double) {
        dao.upsertBudget(CategoryBudgetEntity(category = category, monthlyLimit = limit))
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransaction(id)
    }

    suspend fun clearAllTransactions() {
        dao.deleteAllTransactions()
    }

    suspend fun clearAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllCancellations()
        dao.deleteAllLoans()
        dao.deleteAllCreditCards()
        dao.deleteAllBudgets()
    }

    // Direct DAO helpers for Chanakya AI Function Calling & Export
    suspend fun getExpensesByMerchant(query: String): List<TransactionEntity> =
        dao.getExpensesByMerchant(query)

    suspend fun getCardsAndLoans(): List<TransactionEntity> =
        dao.getCardsAndLoans()

    suspend fun getLoansSnapshot(): List<LoanEmiEntity> =
        dao.getLoansSnapshot()

    suspend fun getCreditCardsSnapshot(): List<CreditCardEntity> =
        dao.getCreditCardsSnapshot()

    suspend fun getExpiringTrials(): List<TransactionEntity> =
        dao.getExpiringTrials()

    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity> =
        dao.getAllTransactionsSnapshot()

    suspend fun getDuplicateAlertsSnapshot(): List<DuplicateChargeAlert> =
        detectDuplicates(dao.getAllTransactionsSnapshot())

    suspend fun getBudgetStatusesSnapshot(): List<BudgetStatus> =
        computeBudgetStatuses(dao.getAllTransactionsSnapshot(), dao.getAllBudgetsSnapshot())

    fun detectDuplicates(transactions: List<TransactionEntity>): List<DuplicateChargeAlert> {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 86_400_000L
        val recent = transactions.filter { it.timestamp >= thirtyDaysAgo && !it.isCancelled && !it.isTrial }

        return recent.groupBy { normalizeMerchant(it.merchantName) }
            .values
            .mapNotNull { group ->
                if (group.size < 2) return@mapNotNull null

                val distinctAccounts = group.map { it.accountLast4 }.distinct()
                val distinctUpi = group.mapNotNull { it.upiId }.distinct()
                val sameAmountPairs = group.groupBy { it.amount.toInt() }.any { it.value.size >= 2 }
                val isSubscriptionOrOtt = group.any {
                    it.type == TransactionType.AUTOPAY || it.category == SpendCategory.OTT_APPS
                }

                val isDuplicate = (distinctAccounts.size >= 2 || distinctUpi.size >= 2) ||
                    (isSubscriptionOrOtt && sameAmountPairs)

                if (!isDuplicate) return@mapNotNull null

                val reason = when {
                    distinctAccounts.size >= 2 ->
                        "Charged across ${distinctAccounts.size} payment methods (${distinctAccounts.joinToString { "XX$it" }}) within 30 days"
                    distinctUpi.size >= 2 ->
                        "Charged via ${distinctUpi.size} different UPI IDs within 30 days"
                    else ->
                        "Charged ${group.size} times within 30 days for the same subscription"
                }

                val sorted = group.sortedByDescending { it.timestamp }
                val overcharged = sorted.drop(1).sumOf { it.amount }
                DuplicateChargeAlert(
                    merchantName = sorted.first().merchantName,
                    charges = sorted,
                    totalOvercharged = overcharged,
                    reason = reason
                )
            }
    }

    fun computeBudgetStatuses(
        transactions: List<TransactionEntity>,
        budgets: List<CategoryBudgetEntity>
    ): List<BudgetStatus> {
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthTx = transactions.filter { it.timestamp >= startOfMonth && !it.isCancelled }
        val spentByCategory = monthTx.groupBy { it.category }.mapValues { (_, list) ->
            list.sumOf { it.amount }
        }

        return budgets.map { budget ->
            val spent = spentByCategory[budget.category] ?: 0.0
            val pct = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat() else 0f
            BudgetStatus(
                category = budget.category,
                spent = spent,
                limit = budget.monthlyLimit,
                percentage = pct,
                isWarning80 = pct >= 0.8f && pct < 1.0f,
                isExceeded100 = pct >= 1.0f
            )
        }.sortedByDescending { it.percentage }
    }

    private fun normalizeMerchant(name: String): String {
        return name.lowercase(Locale.ROOT)
            .replace("autopay", "")
            .replace("india", "")
            .replace("subscription", "")
            .trim()
    }
}
