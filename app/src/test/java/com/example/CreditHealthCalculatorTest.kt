package com.example

import com.example.data.model.CreditCardEntity
import com.example.data.model.LoanEmiEntity
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.engine.BankMessageParser
import com.example.engine.CreditHealthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditHealthCalculatorTest {

    @Test
    fun rewardsOnTimeEmiPaymentsAndLowUtilization() {
        val lowUtilCards = listOf(
            CreditCardEntity(
                cardName = "HDFC Millennia",
                bankName = "HDFC",
                last4 = "9012",
                currentBill = 4000.0,
                creditLimit = 200000.0,
                dueDateLabel = "Due 24 Nov",
                minDue = 200.0
            )
        )
        val loans = listOf(
            LoanEmiEntity(
                loanName = "Home Loan",
                lenderName = "HDFC",
                accountLast4 = "1190",
                emiAmount = 24800.0,
                remainingMonths = 120,
                totalMonths = 180,
                nextDueDayOfMonth = 5,
                interestRate = 8.5,
                autoDebitEnabled = true,
                lastPaidTimestamp = System.currentTimeMillis()
            )
        )
        val onTimeTxs = listOf(
            TransactionEntity(
                amount = 24800.0,
                merchantName = "Home Loan EMI",
                type = TransactionType.CARD_EMI,
                category = SpendCategory.CARD_EMI,
                source = "SMS",
                rawMessage = "HDFC Home Loan EMI A/c XX1190 of INR 24,800.00 debited."
            ),
            TransactionEntity(
                amount = 4000.0,
                merchantName = "HDFC Millennia Card",
                type = TransactionType.CARD_EMI,
                category = SpendCategory.CARD_EMI,
                source = "CARD_PAYMENT",
                rawMessage = "Paid Credit Card bill ₹4,000 for HDFC Millennia XX9012"
            )
        )

        val baseline = CreditHealthCalculator.calculate(
            transactions = emptyList(),
            loans = loans,
            creditCards = lowUtilCards,
            trials = emptyList(),
            duplicates = emptyList()
        )
        val rewarded = CreditHealthCalculator.calculate(
            transactions = onTimeTxs,
            loans = loans,
            creditCards = lowUtilCards,
            trials = emptyList(),
            duplicates = emptyList()
        )

        assertTrue(rewarded.score > baseline.score)
        assertTrue(rewarded.score in 750..900)
        assertEquals(2, rewarded.utilizationPct)
        assertEquals(100, rewarded.onTimeEmisPct)
        assertEquals(2, rewarded.onTimePaymentsCount)
    }

    @Test
    fun deductsPointsForHighUtilizationMissedDuesAndExcessiveLoans() {
        val missedTx = BankMessageParser.parse(
            sender = "VK-HDFCBK",
            body = "URGENT: Loan 1 EMI of Rs 5,000.00 A/c XX1101 BOUNCED / OVERDUE due to insufficient balance. Late fee applicable."
        )
        assertNotNull(missedTx)
        assertTrue(BankMessageParser.isMissedOrOverdueMessage(missedTx!!.rawMessage))
        assertFalse(BankMessageParser.isOnTimeCreditOrEmiPayment(missedTx))

        val highUtilCards = listOf(
            CreditCardEntity(
                cardName = "Axis Magnus",
                bankName = "Axis",
                last4 = "3309",
                currentBill = 85000.0,
                creditLimit = 100000.0,
                dueDateLabel = "Due 28 Nov",
                minDue = 4250.0
            )
        )
        val excessiveLoans = (1..5).map { idx ->
            LoanEmiEntity(
                loanName = "Loan $idx",
                lenderName = "Bank $idx",
                accountLast4 = "110$idx",
                emiAmount = 5000.0,
                remainingMonths = 12,
                totalMonths = 24,
                nextDueDayOfMonth = 5,
                interestRate = 12.0,
                autoDebitEnabled = true,
                lastPaidTimestamp = System.currentTimeMillis()
            )
        }

        val healthy = CreditHealthCalculator.calculate(
            transactions = emptyList(),
            loans = excessiveLoans.take(1),
            creditCards = listOf(highUtilCards.first().copy(currentBill = 5000.0)),
            trials = emptyList(),
            duplicates = emptyList()
        )
        val stressed = CreditHealthCalculator.calculate(
            transactions = listOf(missedTx),
            loans = excessiveLoans,
            creditCards = highUtilCards,
            trials = emptyList(),
            duplicates = emptyList()
        )

        assertTrue(stressed.score in 300..900)
        assertTrue(stressed.score < healthy.score - 150)
        assertEquals(85, stressed.utilizationPct)
        assertEquals(1, stressed.missedDuesCount)
        assertEquals(5, stressed.activeLoansCount)
        assertTrue(stressed.onTimeEmisPct < 100)
        assertTrue(stressed.negativeReasons.isNotEmpty())
    }

    @Test
    fun returnsZeroScoreAndNoDataWhenDatabaseIsEmpty() {
        val emptyReport = CreditHealthCalculator.calculate(
            transactions = emptyList(),
            loans = emptyList(),
            creditCards = emptyList(),
            trials = emptyList(),
            duplicates = emptyList()
        )

        assertEquals(0, emptyReport.score)
        assertEquals("No Data", emptyReport.statusLabel)
        assertEquals(0, emptyReport.utilizationPct)
        assertEquals(0, emptyReport.onTimeEmisPct)
        assertEquals(0, emptyReport.activeMandatesCount)
        assertEquals(0, emptyReport.activeLoansCount)
        assertEquals(0, emptyReport.missedDuesCount)

        val cibilOnlyReport = CreditHealthCalculator.calculate(
            transactions = emptyList(),
            loans = emptyList(),
            creditCards = emptyList(),
            trials = emptyList(),
            duplicates = emptyList(),
            cibilPdfScore = 765
        )
        assertEquals(765, cibilOnlyReport.score)
        assertEquals("Good Standing", cibilOnlyReport.statusLabel)
    }
}
