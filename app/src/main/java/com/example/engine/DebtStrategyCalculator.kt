package com.example.engine

import com.example.data.model.CreditCardEntity
import com.example.data.model.LoanEmiEntity
import kotlin.math.roundToInt

enum class DebtStrategyMode(val title: String, val subtitle: String) {
    AVALANCHE("Avalanche", "Save Max Interest"),
    SNOWBALL("Snowball", "Quick Wins First")
}

data class DebtAccountItem(
    val id: String,
    val name: String,
    val subLabel: String,
    val balance: Double,
    val aprPercent: Double,
    val monthlyCommitment: Double,
    val isCreditCard: Boolean
)

data class DebtStrategyPlan(
    val mode: DebtStrategyMode,
    val orderedDebts: List<DebtAccountItem>,
    val totalOutstanding: Double,
    val totalMonthlyCommitment: Double,
    val extraMonthlyPayment: Double,
    val baselineInterestTotal: Double,
    val strategyInterestTotal: Double,
    val interestSaved: Double,
    val baselineMonths: Int,
    val strategyMonths: Int,
    val monthsSaved: Int,
    val targetFirstDebt: DebtAccountItem?
)

object DebtStrategyCalculator {

    fun buildStrategy(
        loans: List<LoanEmiEntity>,
        creditCards: List<CreditCardEntity>,
        extraMonthlyPayment: Double,
        mode: DebtStrategyMode
    ): DebtStrategyPlan {
        val items = mutableListOf<DebtAccountItem>()

        creditCards.filter { !it.isPaid && it.currentBill > 0.0 }.forEach { card ->
            items.add(
                DebtAccountItem(
                    id = "card_${card.id}",
                    name = "${card.cardName} (XX${card.last4})",
                    subLabel = "${card.bankName} • Revolving Card APR ~36%",
                    balance = card.currentBill,
                    aprPercent = 36.0,
                    monthlyCommitment = maxOf(card.minDue, card.currentBill * 0.08).coerceAtMost(card.currentBill),
                    isCreditCard = true
                )
            )
        }

        loans.filter { it.remainingMonths > 0 && it.emiAmount > 0.0 }.forEach { loan ->
            val balance = loan.emiAmount * loan.remainingMonths
            items.add(
                DebtAccountItem(
                    id = "loan_${loan.id}",
                    name = loan.loanName,
                    subLabel = "${loan.lenderName} • ${loan.remainingMonths} EMIs left",
                    balance = balance,
                    aprPercent = loan.interestRate,
                    monthlyCommitment = loan.emiAmount,
                    isCreditCard = false
                )
            )
        }

        val ordered = when (mode) {
            DebtStrategyMode.AVALANCHE -> items.sortedWith(
                compareByDescending<DebtAccountItem> { it.aprPercent }
                    .thenBy { it.balance }
            )
            DebtStrategyMode.SNOWBALL -> items.sortedWith(
                compareBy<DebtAccountItem> { it.balance }
                    .thenByDescending { it.aprPercent }
            )
        }

        val totalOutstanding = ordered.sumOf { it.balance }
        val totalMonthly = ordered.sumOf { it.monthlyCommitment }

        if (ordered.isEmpty()) {
            return DebtStrategyPlan(
                mode = mode,
                orderedDebts = emptyList(),
                totalOutstanding = 0.0,
                totalMonthlyCommitment = 0.0,
                extraMonthlyPayment = extraMonthlyPayment,
                baselineInterestTotal = 0.0,
                strategyInterestTotal = 0.0,
                interestSaved = 0.0,
                baselineMonths = 0,
                strategyMonths = 0,
                monthsSaved = 0,
                targetFirstDebt = null
            )
        }

        val baseline = simulatePayoff(ordered, extraMonthly = 0.0)
        val withStrategy = simulatePayoff(ordered, extraMonthly = extraMonthlyPayment.coerceAtLeast(0.0))

        val interestSaved = (baseline.first - withStrategy.first).coerceAtLeast(0.0)
        val monthsSaved = (baseline.second - withStrategy.second).coerceAtLeast(0)

        return DebtStrategyPlan(
            mode = mode,
            orderedDebts = ordered,
            totalOutstanding = totalOutstanding,
            totalMonthlyCommitment = totalMonthly,
            extraMonthlyPayment = extraMonthlyPayment,
            baselineInterestTotal = baseline.first,
            strategyInterestTotal = withStrategy.first,
            interestSaved = interestSaved,
            baselineMonths = baseline.second,
            strategyMonths = withStrategy.second,
            monthsSaved = monthsSaved,
            targetFirstDebt = ordered.firstOrNull()
        )
    }

    /**
     * Simulates month-by-month debt payoff with snowball/avalanche rollover + optional extra monthly payment.
     * Returns Pair(totalInterestPaid, totalMonthsToDebtFree).
     */
    private fun simulatePayoff(
        ordered: List<DebtAccountItem>,
        extraMonthly: Double
    ): Pair<Double, Int> {
        val balances = ordered.map { it.balance }.toDoubleArray()
        val monthlyRates = ordered.map { (it.aprPercent / 100.0) / 12.0 }.toDoubleArray()
        val mins = ordered.map { it.monthlyCommitment }.toDoubleArray()

        var totalInterest = 0.0
        var months = 0
        val maxMonths = 360

        while (months < maxMonths && balances.any { it > 0.5 }) {
            months++
            var freedBudget = extraMonthly

            for (i in balances.indices) {
                if (balances[i] <= 0.5) {
                    // Closed debt's monthly commitment rolls over into freed snowball/avalanche pool
                    freedBudget += mins[i]
                    continue
                }
                val interest = balances[i] * monthlyRates[i]
                totalInterest += interest
                balances[i] += interest

                val pay = minOf(balances[i], mins[i])
                balances[i] -= pay
                if (pay < mins[i]) {
                    freedBudget += (mins[i] - pay)
                }
            }

            // Direct freed + extra budget to the highest-priority remaining debt in order
            for (i in balances.indices) {
                if (freedBudget <= 0.0) break
                if (balances[i] > 0.5) {
                    val extraPay = minOf(balances[i], freedBudget)
                    balances[i] -= extraPay
                    freedBudget -= extraPay
                }
            }
        }

        return totalInterest.roundToInt().toDouble() to months
    }
}
