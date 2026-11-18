package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CreditWalletTest {

    @Test
    fun enforcesStrictFreeTierAndPayAsYouGoCredits() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesRepository(context)

        val initial = prefs.creditWallet.first()
        assertEquals(5, initial.freeVoiceRemaining)
        assertEquals(15, initial.freeTextRemaining)
        assertEquals(1, initial.freeUploadRemaining)
        assertEquals(1, initial.freeEmailRemaining)
        assertEquals(0, initial.creditBalance)

        // PDF Export always requires 2 Credits
        assertFalse(prefs.consumePdfExportCredits())

        // Minimum top-up is ₹5
        prefs.addCredits(3)
        assertEquals(0, prefs.creditWallet.first().creditBalance)

        // Valid ₹5 top-up grants 5 credits
        prefs.addCredits(5)
        assertEquals(5, prefs.creditWallet.first().creditBalance)

        // Now PDF Export consumes 2 Credits -> 3 left
        assertTrue(prefs.consumePdfExportCredits())
        assertEquals(3, prefs.creditWallet.first().creditBalance)

        // Upload uses 1 Free Upload first without touching Credits
        assertTrue(prefs.consumeUploadQuota())
        assertEquals(0, prefs.creditWallet.first().freeUploadRemaining)
        assertEquals(3, prefs.creditWallet.first().creditBalance)

        // Second Upload consumes 2 Credits -> 1 left
        assertTrue(prefs.consumeUploadQuota())
        assertEquals(1, prefs.creditWallet.first().creditBalance)

        // Email uses 1 Free Email first -> 0 free left, 1 credit left
        assertTrue(prefs.consumeEmailQuota())
        assertEquals(0, prefs.creditWallet.first().freeEmailRemaining)
        assertEquals(1, prefs.creditWallet.first().creditBalance)

        // Second Email consumes 1 Credit -> 0 left
        assertTrue(prefs.consumeEmailQuota())
        assertEquals(0, prefs.creditWallet.first().creditBalance)

        // Next Email should fail (triggers Top-Up Sheet)
        assertFalse(prefs.consumeEmailQuota())
    }
}
