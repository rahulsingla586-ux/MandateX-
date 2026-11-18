package com.example.engine

import com.example.data.model.CreditCardEntity
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.LoanEmiEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.FinancialHealthScore
import java.util.Locale

object CreditHealthCalculator {

    fun calculate(
        transactions: List<TransactionEntity>,
        loans: List<LoanEmiEntity>,
        creditCards: List<CreditCardEntity>,
        trials: List<TransactionEntity>,
        duplicates: List<DuplicateChargeAlert>,
        cibilPdfScore: Int? = null,
        cibilPdfFileName: String? = null,
        cibilPdfAnalyzedAt: Long? = null
    ): FinancialHealthScore {
        if (transactions.isEmpty() &&
            loans.isEmpty() &&
            creditCards.isEmpty() &&
            trials.isEmpty() &&
            duplicates.isEmpty()
        ) {
            if (cibilPdfScore == null) {
                return FinancialHealthScore(
                    score = 0,
                    maxScore = 900,
                    utilizationPct = 0,
                    onTimeEmisPct = 0,
                    activeMandatesCount = 0,
                    statusLabel = "No Data",
                    cibilPdfScore = null,
                    cibilPdfFileName = cibilPdfFileName,
                    cibilPdfLastAnalyzedAt = cibilPdfAnalyzedAt
                )
            }
            val bureauScore = cibilPdfScore.coerceIn(300, 900)
            val label = when {
                bureauScore >= 780 -> "Excellent"
                bureauScore >= 700 -> "Good Standing"
                bureauScore >= 600 -> "Fair"
                else -> "Needs Care"
            }
            return FinancialHealthScore(
                score = bureauScore,
                maxScore = 900,
                utilizationPct = 0,
                onTimeEmisPct = 0,
                activeMandatesCount = 0,
                statusLabel = label,
                positiveReasons = listOf("Official CIBIL PDF score: $bureauScore/900"),
                cibilPdfScore = bureauScore,
                cibilPdfFileName = cibilPdfFileName,
                cibilPdfLastAnalyzedAt = cibilPdfAnalyzedAt
            )
        }

        val totalLimit = creditCards.sumOf { it.creditLimit }
        val totalBill = creditCards.sumOf { it.currentBill }
        val utilizationPct = if (totalLimit > 0.0) {
            ((totalBill / totalLimit) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val onTimeLedgerPayments = transactions.count { BankMessageParser.isOnTimeCreditOrEmiPayment(it) }
        val paidCardsCount = creditCards.count { it.isPaid || it.currentBill == 0.0 }
        val totalOnTimeEvents = onTimeLedgerPayments + paidCardsCount
        val missedDuesCount = transactions.count {
            !it.isCancelled && BankMessageParser.isMissedOrOverdueMessage(it.rawMessage)
        }

        val autoDebitCoveredLoans = loans.count { it.autoDebitEnabled && it.lastPaidTimestamp != null }
        val totalObligations = totalOnTimeEvents + autoDebitCoveredLoans + missedDuesCount
        val onTimeEmisPct = if (totalObligations == 0) {
            0
        } else {
            (((totalOnTimeEvents + autoDebitCoveredLoans).toDouble() / (totalOnTimeEvents + autoDebitCoveredLoans + missedDuesCount * 2.0)) * 100.0)
                .toInt()
                .coerceIn(0, 100)
        }

        // Count active loans without double-counting loans already in LoanEmiEntity
        val trackedLoanTokens = loans.map { it.loanName.lowercase(Locale.ROOT) }
        val extraParsedLoans = transactions.filter { tx ->
            !tx.isCancelled &&
                (tx.rawMessage.contains("loan", ignoreCase = true) || tx.merchantName.contains("loan", ignoreCase = true)) &&
                trackedLoanTokens.none { loanName ->
                    loanName.contains(tx.merchantName.lowercase(Locale.ROOT)) ||
                        tx.merchantName.lowercase(Locale.ROOT).contains(loanName) ||
                        (tx.accountLast4 != "0000" && loans.any { it.accountLast4 == tx.accountLast4 })
                }
        }.map { it.merchantName.lowercase(Locale.ROOT) }.distinct().size
        val activeLoansCount = loans.size + extraParsedLoans

        val activeMandatesCount = transactions.count {
            !it.isCancelled && (
                it.type == TransactionType.AUTOPAY ||
                    it.isTrial ||
                    !it.mandateRefId.isNullOrBlank()
                )
        }
        val savedTotal = transactions.sumOf { it.savedAmount }

        // 300-900 Score Formula (without paid bureau APIs)
        var score = 690
        val positives = mutableListOf<String>()
        val negatives = mutableListOf<String>()

        // 1. Reward on-time EMI & card bill payments
        val paymentBonus = (totalOnTimeEvents * 16).coerceAtMost(80) +
            if (loans.isNotEmpty() && autoDebitCoveredLoans == loans.size && missedDuesCount == 0) 15 else 0
        score += paymentBonus
        if (totalOnTimeEvents > 0) {
            positives.add("+$paymentBonus pts: $totalOnTimeEvents on-time EMI/card payments")
        }

        // 2. Credit utilization impact
        val utilizationDelta = when {
            creditCards.isEmpty() -> 15
            utilizationPct <= 10 -> 45
            utilizationPct <= 20 -> 30
            utilizationPct <= 30 -> 15
            utilizationPct <= 50 -> -20
            utilizationPct <= 75 -> -55
            else -> -95
        }
        score += utilizationDelta
        if (utilizationDelta > 0) {
            positives.add("+$utilizationDelta pts: Low utilization ($utilizationPct%)")
        } else if (utilizationDelta < 0) {
            negatives.add("$utilizationDelta pts: High utilization ($utilizationPct%)")
        }

        // 3. Missed dues / bounced ECS / late fee deductions
        if (missedDuesCount > 0) {
            val missedPenalty = (missedDuesCount * 60).coerceAtMost(220)
            score -= missedPenalty
            negatives.add("-$missedPenalty pts: $missedDuesCount missed/overdue EMI alert")
        }

        // 4. Excessive loans burden
        val loanDelta = when {
            activeLoansCount == 0 -> 0
            activeLoansCount <= 2 -> 10
            activeLoansCount == 3 -> -10
            else -> -10 - ((activeLoansCount - 3) * 32).coerceAtMost(110)
        }
        score += loanDelta
        if (loanDelta > 0) {
            positives.add("+$loanDelta pts: Balanced debt load ($activeLoansCount loans)")
        } else if (activeLoansCount >= 4) {
            negatives.add("$loanDelta pts: Excessive loans ($activeLoansCount active)")
        } else if (activeLoansCount == 3) {
            negatives.add("-10 pts: $activeLoansCount concurrent loans")
        }

        // 5. AutoPay traps, duplicate charges & money saved
        val trapPenalty = (trials.size * 10) + (duplicates.size * 10)
        if (trapPenalty > 0) {
            score -= trapPenalty
            negatives.add("-$trapPenalty pts: ${trials.size} trial & ${duplicates.size} duplicate leak")
        }
        val savedBonus = (savedTotal / 500.0).toInt().coerceAtMost(18)
        if (savedBonus > 0) {
            score += savedBonus
        }

        val clampedScore = score.coerceIn(300, 900)
        val statusLabel = when {
            clampedScore >= 780 -> "Excellent"
            clampedScore >= 700 -> "Good Standing"
            clampedScore >= 600 -> "Fair"
            else -> "Needs Care"
        }

        return FinancialHealthScore(
            score = clampedScore,
            maxScore = 900,
            utilizationPct = utilizationPct,
            onTimeEmisPct = onTimeEmisPct,
            activeMandatesCount = activeMandatesCount,
            statusLabel = statusLabel,
            onTimePaymentsCount = totalOnTimeEvents,
            missedDuesCount = missedDuesCount,
            activeLoansCount = activeLoansCount,
            positiveReasons = positives.take(2),
            negativeReasons = negatives.take(2),
            cibilPdfScore = cibilPdfScore,
            cibilPdfFileName = cibilPdfFileName,
            cibilPdfLastAnalyzedAt = cibilPdfAnalyzedAt
        )
    }
}
