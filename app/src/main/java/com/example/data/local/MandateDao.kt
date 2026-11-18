package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CancellationRequestEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.LoanEmiEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MandateDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isTrial = 1 AND isCancelled = 0 ORDER BY trialExpiryTimestamp ASC")
    fun observeActiveTrials(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    fun observeAllBudgets(): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM cancellation_requests ORDER BY updatedAt DESC")
    fun observeCancellations(): Flow<List<CancellationRequestEntity>>

    @Query("SELECT * FROM loan_emi_accounts ORDER BY nextDueDayOfMonth ASC")
    fun observeLoans(): Flow<List<LoanEmiEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    fun observeCreditCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE LOWER(merchantName) LIKE '%' || LOWER(:query) || '%' OR LOWER(COALESCE(upiId, '')) LIKE '%' || LOWER(:query) || '%' ORDER BY timestamp DESC")
    suspend fun getExpensesByMerchant(query: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type = 'CARD_EMI' ORDER BY timestamp DESC")
    suspend fun getCardsAndLoans(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isTrial = 1 AND isCancelled = 0 ORDER BY trialExpiryTimestamp ASC")
    suspend fun getExpiringTrials(): List<TransactionEntity>

    @Query("SELECT * FROM loan_emi_accounts ORDER BY nextDueDayOfMonth ASC")
    suspend fun getLoansSnapshot(): List<LoanEmiEntity>

    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    suspend fun getCreditCardsSnapshot(): List<CreditCardEntity>

    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    suspend fun getAllBudgetsSnapshot(): List<CategoryBudgetEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    @Query("SELECT COUNT(*) FROM cancellation_requests")
    suspend fun countCancellations(): Int

    @Query("SELECT COUNT(*) FROM loan_emi_accounts")
    suspend fun countLoans(): Int

    @Query("SELECT COUNT(*) FROM credit_cards")
    suspend fun countCreditCards(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE rawMessage = :rawMessage")
    suspend fun countByRawMessage(rawMessage: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE externalMessageId = :externalId")
    suspend fun countByExternalMessageId(externalId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(tx: TransactionEntity)

    @Query("UPDATE transactions SET isCancelled = 1, savedAmount = :savedAmount WHERE id = :id")
    suspend fun markTrialCancelled(id: Long, savedAmount: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: CategoryBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<CategoryBudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCancellation(item: CancellationRequestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoan(loan: LoanEmiEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCreditCard(card: CreditCardEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM cancellation_requests")
    suspend fun deleteAllCancellations()

    @Query("DELETE FROM loan_emi_accounts")
    suspend fun deleteAllLoans()

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAllCreditCards()

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAllBudgets()
}
