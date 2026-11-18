package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MandateDatabase
import com.example.data.model.CreditCardEntity
import com.example.data.model.LoanEmiEntity
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.MandateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ZeroStatePersistenceTest {

    private lateinit var context: Context
    private lateinit var db: MandateDatabase
    private lateinit var prefs: UserPreferencesRepository
    private lateinit var repository: MandateRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MandateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = UserPreferencesRepository(context)
        repository = MandateRepository(context, db.mandateDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun freshDatabaseAndPreferencesStartEmptyWithZeroBalances() = runBlocking {
        val dao = db.mandateDao()

        assertEquals(0, dao.countTransactions())
        assertEquals(0, dao.countCancellations())
        assertEquals(0, dao.countLoans())
        assertEquals(0, dao.countCreditCards())
        assertTrue(dao.getAllBudgetsSnapshot().isEmpty())

        assertEquals("", prefs.userName.first())
        assertEquals("", prefs.bankSupportEmail.first())
        assertTrue(prefs.burnerAliases.first().isEmpty())

        val vault = prefs.masterVaultProfile.first()
        assertEquals("", vault.panNumber)
        assertEquals("", vault.aadhaarMasked)
        assertEquals("", vault.primaryCardMask)
        assertEquals("", vault.primaryUpiHandle)

        val wallet = prefs.creditWallet.first()
        assertEquals(0, wallet.creditBalance)
    }

    @Test
    fun parsedAttachmentsPersistDeduplicateAndClearCleanly() = runBlocking {
        val dao = db.mandateDao()

        val extractedTx = TransactionEntity(
            amount = 749.0,
            merchantName = "Cloud Storage",
            type = TransactionType.AUTOPAY,
            category = SpendCategory.OTT_APPS,
            accountLast4 = "4321",
            bankName = "Statement PDF",
            source = "PDF_AUDIT",
            rawMessage = "PDF_ENTRY_CLOUD_749",
            externalMessageId = "pdf_tx_1"
        )
        val extractedCard = CreditCardEntity(
            cardName = "Platinum Rewards",
            bankName = "ICICI",
            last4 = "4321",
            currentBill = 12500.0,
            creditLimit = 100000.0,
            dueDateLabel = "Due 12th",
            minDue = 625.0
        )
        val extractedLoan = LoanEmiEntity(
            loanName = "Personal Loan",
            lenderName = "SBI",
            accountLast4 = "9876",
            emiAmount = 8500.0,
            remainingMonths = 18,
            totalMonths = 24,
            nextDueDayOfMonth = 5,
            interestRate = 11.2,
            autoDebitEnabled = true
        )

        val firstIngest = repository.ingestParsedAttachmentData(
            transactions = listOf(extractedTx),
            creditCards = listOf(extractedCard),
            loans = listOf(extractedLoan)
        )
        assertEquals(Triple(1, 1, 1), firstIngest)
        assertEquals(1, dao.countTransactions())
        assertEquals(1, dao.countCreditCards())
        assertEquals(1, dao.countLoans())

        // Re-ingesting same transaction externalMessageId/rawMessage deduplicates tx cleanly
        val secondIngest = repository.ingestParsedAttachmentData(
            transactions = listOf(extractedTx),
            creditCards = emptyList(),
            loans = emptyList()
        )
        assertEquals(Triple(0, 0, 0), secondIngest)
        assertEquals(1, dao.countTransactions())

        repository.clearAllData()
        assertEquals(0, dao.countTransactions())
        assertEquals(0, dao.countCancellations())
        assertEquals(0, dao.countLoans())
        assertEquals(0, dao.countCreditCards())
        assertTrue(dao.getAllBudgetsSnapshot().isEmpty())
    }
}
