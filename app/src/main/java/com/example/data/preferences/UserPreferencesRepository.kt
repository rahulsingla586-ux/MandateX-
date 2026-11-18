package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mandatex_prefs")

data class CreditWalletState(
    val freeVoiceRemaining: Int = 5,
    val freeTextRemaining: Int = 15,
    val freeUploadRemaining: Int = 1,
    val freeEmailRemaining: Int = 1,
    val creditBalance: Int = 0
)

data class CibilAuditMeta(
    val score: Int? = null,
    val fileName: String? = null,
    val analyzedAt: Long? = null
)

data class MasterVaultProfile(
    val panNumber: String = "",
    val aadhaarMasked: String = "",
    val primaryCardMask: String = "",
    val primaryUpiHandle: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
        val PRIVACY_BLUR = booleanPreferencesKey("privacy_blur_enabled")
        val TTS_VOICE_REPLY = booleanPreferencesKey("tts_voice_reply_enabled")
        val HAS_SEEDED_DEMO = booleanPreferencesKey("has_seeded_demo_v2")
        val HAS_PURGED_LEGACY_DEMO = booleanPreferencesKey("has_purged_legacy_demo_v3")
        val USER_NAME = stringPreferencesKey("user_name")
        val BANK_SUPPORT_EMAIL = stringPreferencesKey("bank_support_email")
        val GMAIL_CONNECTED_EMAIL = stringPreferencesKey("gmail_connected_email")
        val LAST_GMAIL_SYNC_TS = longPreferencesKey("last_gmail_sync_ts")
        val LAST_SMS_SCAN_TS = longPreferencesKey("last_sms_scan_ts")

        // Strict Free Tier + Pay-As-You-Go Credits Wallet
        val FREE_VOICE_REMAINING = intPreferencesKey("free_voice_remaining")
        val FREE_TEXT_REMAINING = intPreferencesKey("free_text_remaining")
        val FREE_UPLOAD_REMAINING = intPreferencesKey("free_upload_remaining")
        val FREE_EMAIL_REMAINING = intPreferencesKey("free_email_remaining")
        val CREDIT_BALANCE = intPreferencesKey("credit_balance")

        // CIBIL PDF Audit Persistence
        val CIBIL_PDF_SCORE = intPreferencesKey("cibil_pdf_score")
        val CIBIL_PDF_NAME = stringPreferencesKey("cibil_pdf_name")
        val CIBIL_PDF_TS = longPreferencesKey("cibil_pdf_ts")

        // Vault Master Autofill & Burner Aliases
        val VAULT_PAN = stringPreferencesKey("vault_pan")
        val VAULT_AADHAAR = stringPreferencesKey("vault_aadhaar")
        val VAULT_CARD_MASK = stringPreferencesKey("vault_card_mask")
        val VAULT_UPI = stringPreferencesKey("vault_upi")
        val BURNER_ALIASES = stringPreferencesKey("burner_aliases_pipe")
    }

    val biometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BIOMETRIC_LOCK] ?: false
    }

    val privacyBlurEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PRIVACY_BLUR] ?: false
    }

    val ttsVoiceReplyEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.TTS_VOICE_REPLY] ?: true
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_NAME] ?: ""
    }

    val bankSupportEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.BANK_SUPPORT_EMAIL] ?: ""
    }

    val gmailConnectedEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.GMAIL_CONNECTED_EMAIL]?.takeIf { it.isNotBlank() }
    }

    val lastGmailSyncTimestamp: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_GMAIL_SYNC_TS]
    }

    val lastSmsScanTimestamp: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SMS_SCAN_TS]
    }

    val creditWallet: Flow<CreditWalletState> = context.dataStore.data.map { prefs ->
        CreditWalletState(
            freeVoiceRemaining = prefs[Keys.FREE_VOICE_REMAINING] ?: 5,
            freeTextRemaining = prefs[Keys.FREE_TEXT_REMAINING] ?: 15,
            freeUploadRemaining = prefs[Keys.FREE_UPLOAD_REMAINING] ?: 1,
            freeEmailRemaining = prefs[Keys.FREE_EMAIL_REMAINING] ?: 1,
            creditBalance = prefs[Keys.CREDIT_BALANCE] ?: 0
        )
    }

    val cibilAuditMeta: Flow<CibilAuditMeta> = context.dataStore.data.map { prefs ->
        val score = prefs[Keys.CIBIL_PDF_SCORE]?.takeIf { it in 300..900 }
        CibilAuditMeta(
            score = score,
            fileName = prefs[Keys.CIBIL_PDF_NAME]?.takeIf { it.isNotBlank() },
            analyzedAt = prefs[Keys.CIBIL_PDF_TS]
        )
    }

    val masterVaultProfile: Flow<MasterVaultProfile> = context.dataStore.data.map { prefs ->
        MasterVaultProfile(
            panNumber = prefs[Keys.VAULT_PAN] ?: "",
            aadhaarMasked = prefs[Keys.VAULT_AADHAAR] ?: "",
            primaryCardMask = prefs[Keys.VAULT_CARD_MASK] ?: "",
            primaryUpiHandle = prefs[Keys.VAULT_UPI] ?: ""
        )
    }

    val burnerAliases: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.BURNER_ALIASES]
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setPrivacyBlurEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PRIVACY_BLUR] = enabled }
    }

    suspend fun setTtsVoiceReplyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TTS_VOICE_REPLY] = enabled }
    }

    /**
     * Checks whether a legacy pre-seeded demo state exists on upgrade and returns true
     * if Room demo rows should be wiped once.
     */
    suspend fun consumeLegacyDemoPurgeFlag(): Boolean {
        val snapshot = context.dataStore.data.first()
        if (snapshot[Keys.HAS_PURGED_LEGACY_DEMO] == true) return false
        val hadLegacyDemo = snapshot[Keys.HAS_SEEDED_DEMO] == true
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.HAS_SEEDED_DEMO)
            if (prefs[Keys.USER_NAME] == "Rahul") prefs.remove(Keys.USER_NAME)
            if (prefs[Keys.BANK_SUPPORT_EMAIL] == "support@hdfcbank.com") prefs.remove(Keys.BANK_SUPPORT_EMAIL)
            if (prefs[Keys.VAULT_PAN] == "ABCDE1234F") prefs.remove(Keys.VAULT_PAN)
            if (prefs[Keys.VAULT_AADHAAR] == "XXXX-XXXX-4821") prefs.remove(Keys.VAULT_AADHAAR)
            if (prefs[Keys.VAULT_CARD_MASK] == "4532 •••• •••• 9012") prefs.remove(Keys.VAULT_CARD_MASK)
            if (prefs[Keys.VAULT_UPI] == "rahul@okhdfcbank") prefs.remove(Keys.VAULT_UPI)
            if (prefs[Keys.BURNER_ALIASES]?.contains("shield.ott.92xk@mandatex-alias.in") == true) {
                prefs.remove(Keys.BURNER_ALIASES)
            }
            prefs[Keys.HAS_PURGED_LEGACY_DEMO] = true
        }
        return hadLegacyDemo
    }

    suspend fun setUserProfile(name: String, bankEmail: String) {
        context.dataStore.edit {
            it[Keys.USER_NAME] = name.trim()
            it[Keys.BANK_SUPPORT_EMAIL] = bankEmail.trim()
        }
    }

    suspend fun setGmailConnected(email: String?, syncTimestamp: Long? = null) {
        context.dataStore.edit { prefs ->
            if (email.isNullOrBlank()) {
                prefs.remove(Keys.GMAIL_CONNECTED_EMAIL)
                prefs.remove(Keys.LAST_GMAIL_SYNC_TS)
            } else {
                prefs[Keys.GMAIL_CONNECTED_EMAIL] = email
                if (syncTimestamp != null) {
                    prefs[Keys.LAST_GMAIL_SYNC_TS] = syncTimestamp
                }
            }
        }
    }

    suspend fun setLastSmsScanTimestamp(ts: Long) {
        context.dataStore.edit { it[Keys.LAST_SMS_SCAN_TS] = ts }
    }

    suspend fun saveCibilAuditMeta(score: Int?, fileName: String, analyzedAt: Long) {
        context.dataStore.edit { prefs ->
            if (score != null && score in 300..900) {
                prefs[Keys.CIBIL_PDF_SCORE] = score
            } else {
                prefs.remove(Keys.CIBIL_PDF_SCORE)
            }
            prefs[Keys.CIBIL_PDF_NAME] = fileName
            prefs[Keys.CIBIL_PDF_TS] = analyzedAt
        }
    }

    suspend fun clearCibilAuditMeta() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.CIBIL_PDF_SCORE)
            prefs.remove(Keys.CIBIL_PDF_NAME)
            prefs.remove(Keys.CIBIL_PDF_TS)
        }
    }

    suspend fun saveMasterVaultProfile(
        panNumber: String,
        aadhaarMasked: String,
        primaryCardMask: String,
        primaryUpiHandle: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VAULT_PAN] = panNumber.trim().uppercase()
            prefs[Keys.VAULT_AADHAAR] = aadhaarMasked.trim()
            prefs[Keys.VAULT_CARD_MASK] = primaryCardMask.trim()
            prefs[Keys.VAULT_UPI] = primaryUpiHandle.trim()
        }
    }

    suspend fun clearVaultAndProfileData() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.VAULT_PAN)
            prefs.remove(Keys.VAULT_AADHAAR)
            prefs.remove(Keys.VAULT_CARD_MASK)
            prefs.remove(Keys.VAULT_UPI)
            prefs.remove(Keys.BURNER_ALIASES)
            prefs.remove(Keys.CIBIL_PDF_SCORE)
            prefs.remove(Keys.CIBIL_PDF_NAME)
            prefs.remove(Keys.CIBIL_PDF_TS)
            prefs.remove(Keys.LAST_SMS_SCAN_TS)
        }
    }

    suspend fun addBurnerAlias(alias: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BURNER_ALIASES]
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val updated = (listOf(alias) + current).distinct().take(6)
            prefs[Keys.BURNER_ALIASES] = updated.joinToString("|")
        }
    }

    suspend fun addCredits(amountInr: Int) {
        if (amountInr < 5) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CREDIT_BALANCE] ?: 0
            prefs[Keys.CREDIT_BALANCE] = current + amountInr
        }
    }

    /**
     * Attempts to consume quota for Voice Mic (1 Free Voice, else 2 Credits).
     * Returns true if allowed, false if insufficient credits.
     */
    suspend fun consumeVoiceQuota(): Boolean {
        var allowed = false
        context.dataStore.edit { prefs ->
            val free = prefs[Keys.FREE_VOICE_REMAINING] ?: 5
            val credits = prefs[Keys.CREDIT_BALANCE] ?: 0
            when {
                free > 0 -> {
                    prefs[Keys.FREE_VOICE_REMAINING] = free - 1
                    allowed = true
                }
                credits >= 2 -> {
                    prefs[Keys.CREDIT_BALANCE] = credits - 2
                    allowed = true
                }
                else -> allowed = false
            }
        }
        return allowed
    }

    /**
     * Attempts to consume quota for Text Chat (1 Free Text, else 1 Credit).
     */
    suspend fun consumeTextQuota(): Boolean {
        var allowed = false
        context.dataStore.edit { prefs ->
            val free = prefs[Keys.FREE_TEXT_REMAINING] ?: 15
            val credits = prefs[Keys.CREDIT_BALANCE] ?: 0
            when {
                free > 0 -> {
                    prefs[Keys.FREE_TEXT_REMAINING] = free - 1
                    allowed = true
                }
                credits >= 1 -> {
                    prefs[Keys.CREDIT_BALANCE] = credits - 1
                    allowed = true
                }
                else -> allowed = false
            }
        }
        return allowed
    }

    /**
     * Attempts to consume quota for + File/Screenshot Upload (1 Free Upload, else 2 Credits).
     */
    suspend fun consumeUploadQuota(): Boolean {
        var allowed = false
        context.dataStore.edit { prefs ->
            val free = prefs[Keys.FREE_UPLOAD_REMAINING] ?: 1
            val credits = prefs[Keys.CREDIT_BALANCE] ?: 0
            when {
                free > 0 -> {
                    prefs[Keys.FREE_UPLOAD_REMAINING] = free - 1
                    allowed = true
                }
                credits >= 2 -> {
                    prefs[Keys.CREDIT_BALANCE] = credits - 2
                    allowed = true
                }
                else -> allowed = false
            }
        }
        return allowed
    }

    /**
     * Attempts to consume quota for Bank/RBI Email Draft (1 Free Email, else 1 Credit).
     */
    suspend fun consumeEmailQuota(): Boolean {
        var allowed = false
        context.dataStore.edit { prefs ->
            val free = prefs[Keys.FREE_EMAIL_REMAINING] ?: 1
            val credits = prefs[Keys.CREDIT_BALANCE] ?: 0
            when {
                free > 0 -> {
                    prefs[Keys.FREE_EMAIL_REMAINING] = free - 1
                    allowed = true
                }
                credits >= 1 -> {
                    prefs[Keys.CREDIT_BALANCE] = credits - 1
                    allowed = true
                }
                else -> allowed = false
            }
        }
        return allowed
    }

    /**
     * PDF Export ALWAYS requires 2 Credits.
     */
    suspend fun consumePdfExportCredits(): Boolean {
        var allowed = false
        context.dataStore.edit { prefs ->
            val credits = prefs[Keys.CREDIT_BALANCE] ?: 0
            if (credits >= 2) {
                prefs[Keys.CREDIT_BALANCE] = credits - 2
                allowed = true
            } else {
                allowed = false
            }
        }
        return allowed
    }
}
