package com.example

import com.example.data.model.SpendCategory
import com.example.data.model.TransactionType
import com.example.engine.BankMessageParser
import com.example.engine.GmailSyncEngine
import com.example.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankMessageParserTest {

    @Test
    fun parsesUpiFoodExpenseAccurately() {
        val tx = BankMessageParser.parse(
            sender = "AX-HDFCBK",
            body = "INR 420.00 debited from A/c XX4821 via UPI to Zomato (zomato@okaxis) on 16-Nov-26.",
            source = "TEST",
            nowMillis = 1_700_000_000_000L
        )
        assertNotNull(tx)
        assertEquals(420.0, tx!!.amount, 0.01)
        assertEquals("Zomato", tx.merchantName)
        assertEquals("zomato@okaxis", tx.upiId)
        assertEquals("4821", tx.accountLast4)
        assertEquals("HDFC Bank", tx.bankName)
        assertEquals(SpendCategory.FOOD_DINING, tx.category)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertTrue(tx.paymentSource.contains("Google Pay"))
    }

    @Test
    fun parsesLowCostTrialAndPostTrialPriceHike() {
        val now = 1_700_000_000_000L
        val tx = BankMessageParser.parse(
            sender = "VM-ICICIB",
            body = "UPI AutoPay Mandate created for ₹1.00 introductory 3-day trial at Canva Pro (canva@ybl) on Card XX9012, then Rs 499.00/month auto-debit. Mandate ID: CNV883102.",
            source = "TEST",
            nowMillis = now
        )
        assertNotNull(tx)
        assertTrue(tx!!.isTrial)
        assertEquals(1.0, tx.amount, 0.01)
        assertEquals(499.0, tx.postTrialAmount ?: 0.0, 0.01)
        assertEquals(TransactionType.AUTOPAY, tx.type)
        assertEquals("9012", tx.accountLast4)
        assertEquals("CNV883102", tx.mandateRefId)
        assertTrue(tx.paymentSource.contains("PhonePe"))
        assertTrue((tx.trialExpiryTimestamp ?: 0L) > now)
    }

    @Test
    fun parsesStandingInstructionUmrnAndNextDueDate() {
        val tx = BankMessageParser.parse(
            sender = "HDFCBK",
            body = "Standing Instruction e-Mandate executed for Rs 2,500.00 towards LIC India from A/c XX4821 via PhonePe. UMRN: HDFC702918374. Next debit on 15 Dec 2026.",
            source = "SMS_6M"
        )
        assertNotNull(tx)
        assertEquals(TransactionType.AUTOPAY, tx!!.type)
        assertEquals(2500.0, tx.amount, 0.01)
        assertEquals("LIC India", tx.merchantName)
        assertEquals("HDFC702918374", tx.mandateRefId)
        assertEquals("15 Dec 2026", tx.nextDueDateLabel)
        assertEquals("PhonePe • XX4821", tx.paymentSource)
    }

    @Test
    fun parsesCreditCardAndLoanEmi() {
        val cardTx = BankMessageParser.parse(
            sender = "AD-AXISBK",
            body = "Axis Magnus Credit Card XX3309 bill of Rs 8,950.00 debited via AutoDebit from A/c XX4821."
        )
        assertNotNull(cardTx)
        assertEquals(TransactionType.CARD_EMI, cardTx!!.type)
        assertEquals(SpendCategory.CARD_EMI, cardTx.category)
        assertEquals(8950.0, cardTx.amount, 0.01)

        val emiTx = BankMessageParser.parse(
            sender = "VK-HDFCBK",
            body = "Alert: EMI of INR 16,400.00 for Car Loan A/c XX1190 debited from A/c XX4821."
        )
        assertNotNull(emiTx)
        assertEquals(TransactionType.CARD_EMI, emiTx!!.type)
        assertEquals(16400.0, emiTx.amount, 0.01)
    }

    @Test
    fun verifiesGmailReadOnlyScopeAndSixMonthQuery() {
        assertEquals("https://www.googleapis.com/auth/gmail.readonly", GmailSyncEngine.GMAIL_READONLY_SCOPE)
        assertTrue(GmailSyncEngine.GMAIL_SEARCH_QUERY.contains("newer_than:180d"))
        assertTrue(GmailSyncEngine.GMAIL_SEARCH_QUERY.contains("autopay"))
        assertTrue(GmailSyncEngine.GMAIL_SEARCH_QUERY.contains("standing instruction"))
    }

    @Test
    fun formatsCurrencyAndMasksWhenPrivacyBlurEnabled() {
        assertEquals("₹14,250", CurrencyFormatter.formatInr(14250.0, privacyBlur = false))
        assertEquals("₹****", CurrencyFormatter.formatInr(14250.0, privacyBlur = true))
    }
}
