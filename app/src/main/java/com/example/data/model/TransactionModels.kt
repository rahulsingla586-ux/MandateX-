package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    CARD_EMI("Card & EMI"),
    AUTOPAY("AutoPay")
}

enum class SpendCategory(val label: String) {
    FOOD_DINING("Food & Dining"),
    SHOPPING("Shopping"),
    ENTERTAINMENT("Entertainment"),
    OTT_APPS("OTT & Apps"),
    CARD_EMI("Cards & EMIs"),
    UTILITIES("Utilities & Bills"),
    UPI_TRANSFER("UPI Transfer"),
    OTHER("Other")
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchantName: String,
    val upiId: String? = null,
    val type: TransactionType,
    val category: SpendCategory,
    val accountLast4: String = "0000",
    val bankName: String = "Bank",
    val timestamp: Long = System.currentTimeMillis(),
    val rawMessage: String = "",
    val source: String = "SMS",
    val isTrial: Boolean = false,
    val trialExpiryTimestamp: Long? = null,
    val postTrialAmount: Double? = null,
    val isCancelled: Boolean = false,
    val savedAmount: Double = 0.0,
    val mandateRefId: String? = null,
    val nextDueDateLabel: String? = null,
    val paymentSource: String = "",
    val externalMessageId: String? = null
)

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey val category: SpendCategory,
    val monthlyLimit: Double
)

@Entity(tableName = "cancellation_requests")
data class CancellationRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val subtitle: String,
    val upiId: String,
    val bankName: String,
    val accountLast4: String,
    val amount: Double,
    val status: String, // "IN PROGRESS", "EMAIL SENT", "ESCALATED TO RBI", "COMPLETED"
    val currentStep: Int, // 1..4
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "loan_emi_accounts")
data class LoanEmiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanName: String,
    val lenderName: String,
    val accountLast4: String,
    val emiAmount: Double,
    val remainingMonths: Int,
    val totalMonths: Int,
    val nextDueDayOfMonth: Int,
    val interestRate: Double,
    val autoDebitEnabled: Boolean = true,
    val lastPaidTimestamp: Long? = null
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String,
    val bankName: String,
    val last4: String,
    val currentBill: Double,
    val creditLimit: Double,
    val dueDateLabel: String,
    val minDue: Double,
    val isPaid: Boolean = false
)

data class DuplicateChargeAlert(
    val merchantName: String,
    val charges: List<TransactionEntity>,
    val totalOvercharged: Double,
    val reason: String
)

data class BudgetStatus(
    val category: SpendCategory,
    val spent: Double,
    val limit: Double,
    val percentage: Float,
    val isWarning80: Boolean,
    val isExceeded100: Boolean
)
