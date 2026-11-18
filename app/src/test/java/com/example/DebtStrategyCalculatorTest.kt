package com.example

import com.example.data.model.CreditCardEntity
import com.example.data.model.LoanEmiEntity
import com.example.engine.DebtStrategyCalculator
import com.example.engine.DebtStrategyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtStrategyCalculatorTest {

    private fun sampleCards() = listOf(
        CreditCardEntity(
            id = 1L,
            cardName = "HDFC Millennia",
            bankName = "HDFC",
            last4 = "9012",
            currentBill = 45000.0,
            creditLimit = 100000.0,
            dueDateLabel = "Due 24 Nov",
            minDue = 2250.0,
            isPaid = false
        )
    )

    private fun sampleLoans() = listOf(
        LoanEmiEntity(
            id = 10L,
            loanName = "Home Loan",
            lenderName = "HDFC",
            accountLast4 = "1190",
            emiAmount = 20000.0,
            remainingMonths = 24,
            totalMonths = 60,
            nextDueDayOfMonth = 5,
            interestRate = 8.5,
            autoDebitEnabled = true,
            lastPaidTimestamp = 0L
        ),
        LoanEmiEntity(
            id = 11L,
            loanName = "Personal Gadget Loan",
            lenderName = "Bajaj Finserv",
            accountLast4 = "4421",
            emiAmount = 3500.0,
            remainingMonths = 6,
            totalMonths = 12,
            nextDueDayOfMonth = 10,
            interestRate = 16.0,
            autoDebitEnabled = true,
            lastPaidTimestamp = 0L
        )
    )

    @Test
    fun avalancheOrdersHighestAprFirst() {
        val plan = DebtStrategyCalculator.buildStrategy(
            loans = sampleLoans(),
            creditCards = sampleCards(),
            extraMonthlyPayment = 5000.0,
            mode = DebtStrategyMode.AVALANCHE
        )

        assertEquals(3, plan.orderedDebts.size)
        assertNotNull(plan.targetFirstDebt)
        // Credit card at 36% APR comes before 16% personal loan and 8.5% home loan
        assertTrue(plan.targetFirstDebt!!.isCreditCard)
        assertEquals(36.0, plan.targetFirstDebt!!.aprPercent, 0.01)
        assertEquals(16.0, plan.orderedDebts[1].aprPercent, 0.01)
        assertEquals(8.5, plan.orderedDebts[2].aprPercent, 0.01)
    }

    @Test
    fun snowballOrdersSmallestBalanceFirst() {
        val plan = DebtStrategyCalculator.buildStrategy(
            loans = sampleLoans(),
            creditCards = sampleCards(),
            extraMonthlyPayment = 5000.0,
            mode = DebtStrategyMode.SNOWBALL
        )

        assertEquals(3, plan.orderedDebts.size)
        // Personal Gadget Loan balance = 3500 * 6 = 21,000 < Card 45,000 < Home Loan 480,000
        assertEquals("Personal Gadget Loan", plan.orderedDebts[0].name)
        assertTrue(plan.orderedDebts[0].balance < plan.orderedDebts[1].balance)
        assertTrue(plan.orderedDebts[1].balance < plan.orderedDebts[2].balance)
    }

    @Test
    fun extraPaymentSavesInterestAndMonthsAndAvalancheSavesAtLeastAsMuchAsSnowball() {
        val avalanchePlan = DebtStrategyCalculator.buildStrategy(
            loans = sampleLoans(),
            creditCards = sampleCards(),
            extraMonthlyPayment = 10000.0,
            mode = DebtStrategyMode.AVALANCHE
        )
        val snowballPlan = DebtStrategyCalculator.buildStrategy(
            loans = sampleLoans(),
            creditCards = sampleCards(),
            extraMonthlyPayment = 10000.0,
            mode = DebtStrategyMode.SNOWBALL
        )

        assertTrue(avalanchePlan.interestSaved > 0.0)
        assertTrue(avalanchePlan.monthsSaved > 0)
        assertTrue(avalanchePlan.strategyMonths < avalanchePlan.baselineMonths)
        assertTrue(avalanchePlan.strategyInterestTotal <= snowballPlan.strategyInterestTotal)
    }

    @Test
    fun emptyDebtsReturnsZeroPlan() {
        val plan = DebtStrategyCalculator.buildStrategy(
            loans = emptyList(),
            creditCards = emptyList(),
            extraMonthlyPayment = 5000.0,
            mode = DebtStrategyMode.AVALANCHE
        )

        assertTrue(plan.orderedDebts.isEmpty())
        assertEquals(0.0, plan.totalOutstanding, 0.01)
        assertEquals(0.0, plan.interestSaved, 0.01)
        assertEquals(0, plan.monthsSaved)
    }
}
