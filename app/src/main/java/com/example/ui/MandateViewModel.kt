package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChanakyaAiService
import com.example.ai.ChanakyaUiState
import com.example.ai.ChatMessage
import com.example.data.local.MandateDatabase
import com.example.data.model.BudgetStatus
import com.example.data.model.CancellationRequestEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.LoanEmiEntity
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.preferences.CibilAuditMeta
import com.example.data.preferences.CreditWalletState
import com.example.data.preferences.MasterVaultProfile
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.MandateRepository
import com.example.engine.CreditHealthCalculator
import com.example.engine.GmailSyncEngine
import com.example.engine.SmsInboxScanner
import com.example.engine.SmsScanResult
import com.example.engine.TrialShieldScheduler
import com.example.util.CurrencyFormatter
import com.example.util.StatementExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OverspendPopupAlert(
    val categoryLabel: String,
    val spent: Double,
    val limit: Double,
    val percent: Int
)

data class DailyVerdictReport(
    val isWarning: Boolean = false,
    val headline: String = "No financial records yet",
    val aisaKiyaLine: String = "0 transactions tracked. Upload a bank PDF/screenshot, sync SMS/Gmail, or add an entry.",
    val zarooriKaamLine: String = "Add your cards, loans, or statements to start live AutoPay & credit audit.",
    val speechText: String = "MandateX taiyar hai. Shuru karne ke liye apna bank PDF, payment screenshot upload karein ya manual entry jodein."
)

data class QuickMetricsSummary(
    val faltuKharcheAmount: Double = 0.0,
    val activeTrialsCount: Int = 0,
    val activeDebtsCount: Int = 0
)

data class FinancialHealthScore(
    val score: Int = 0,
    val maxScore: Int = 900,
    val utilizationPct: Int = 0,
    val onTimeEmisPct: Int = 0,
    val activeMandatesCount: Int = 0,
    val statusLabel: String = "No Data",
    val onTimePaymentsCount: Int = 0,
    val missedDuesCount: Int = 0,
    val activeLoansCount: Int = 0,
    val positiveReasons: List<String> = emptyList(),
    val negativeReasons: List<String> = emptyList(),
    val cibilPdfScore: Int? = null,
    val cibilPdfFileName: String? = null,
    val cibilPdfLastAnalyzedAt: Long? = null
)

data class DashboardAuditSnapshot(
    val verdict: DailyVerdictReport = DailyVerdictReport(),
    val metrics: QuickMetricsSummary = QuickMetricsSummary(),
    val health: FinancialHealthScore = FinancialHealthScore()
)

sealed interface GmailSyncState {
    data object Idle : GmailSyncState
    data object Authorizing : GmailSyncState
    data class FetchingAndParsing(val progressLabel: String) : GmailSyncState
    data class Error(val message: String) : GmailSyncState
}

class MandateViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = MandateDatabase.getInstance(context)
    private val dao = database.mandateDao()
    val prefs = UserPreferencesRepository(context)
    private val repository = MandateRepository(context, dao, prefs)
    private val chanakyaAi = ChanakyaAiService(repository, prefs)

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeServices: StateFlow<List<TransactionEntity>> = transactions
        .map { list ->
            list.filter {
                it.type == TransactionType.AUTOPAY ||
                    it.type == TransactionType.CARD_EMI ||
                    it.isTrial ||
                    !it.mandateRefId.isNullOrBlank()
            }.sortedWith(
                compareBy<TransactionEntity> { it.isCancelled }
                    .thenByDescending { it.isTrial }
                    .thenByDescending { it.timestamp }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTrials: StateFlow<List<TransactionEntity>> = repository.activeTrials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duplicateAlerts: StateFlow<List<DuplicateChargeAlert>> = repository.duplicateAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetStatuses: StateFlow<List<BudgetStatus>> = repository.budgetStatuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cancellations: StateFlow<List<CancellationRequestEntity>> = repository.cancellations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<LoanEmiEntity>> = repository.loans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCardEntity>> = repository.creditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privacyBlurEnabled: StateFlow<Boolean> = prefs.privacyBlurEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val biometricLockEnabled: StateFlow<Boolean> = prefs.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val ttsVoiceReplyEnabled: StateFlow<Boolean> = prefs.ttsVoiceReplyEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val userName: StateFlow<String> = prefs.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val bankSupportEmail: StateFlow<String> = prefs.bankSupportEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val gmailConnectedEmail: StateFlow<String?> = prefs.gmailConnectedEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastGmailSyncTimestamp: StateFlow<Long?> = prefs.lastGmailSyncTimestamp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastSmsScanTimestamp: StateFlow<Long?> = prefs.lastSmsScanTimestamp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val creditWallet: StateFlow<CreditWalletState> = prefs.creditWallet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CreditWalletState())

    val totalSavedMoney: StateFlow<Double> = transactions
        .map { list -> list.sumOf { it.savedAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cibilAuditMeta: StateFlow<CibilAuditMeta> = prefs.cibilAuditMeta
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CibilAuditMeta())

    val masterVaultProfile: StateFlow<MasterVaultProfile> = prefs.masterVaultProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MasterVaultProfile())

    val burnerAliases: StateFlow<List<String>> = prefs.burnerAliases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardAudit: StateFlow<DashboardAuditSnapshot> = combine(
        transactions,
        activeTrials,
        duplicateAlerts,
        loans,
        creditCards
    ) { txs, trials, dups, loanList, cardList ->
        Triple(txs, trials to dups, loanList to cardList)
    }.combine(combine(privacyBlurEnabled, cibilAuditMeta) { blur, cibil -> blur to cibil }) {
        (txs, trialsAndDups, loansAndCards),
        (blur, cibil) ->
        val (trials, dups) = trialsAndDups
        val (loanList, cardList) = loansAndCards
        computeDashboardAudit(txs, trials, dups, loanList, cardList, blur, cibil)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardAuditSnapshot())

    private fun computeDashboardAudit(
        txs: List<TransactionEntity>,
        trials: List<TransactionEntity>,
        dups: List<DuplicateChargeAlert>,
        loanList: List<LoanEmiEntity>,
        cardList: List<CreditCardEntity>,
        privacyBlur: Boolean,
        cibilMeta: CibilAuditMeta
    ): DashboardAuditSnapshot {
        val discretionary = txs.filter {
            !it.isCancelled && (
                it.category == SpendCategory.FOOD_DINING ||
                    it.category == SpendCategory.SHOPPING ||
                    it.category == SpendCategory.ENTERTAINMENT
                )
        }
        val discretionaryTotal = discretionary.sumOf { it.amount }
        val dupTotal = dups.sumOf { it.totalOvercharged }
        val trialExposure = trials.sumOf { (it.postTrialAmount ?: 499.0) - it.amount }
        val faltuKharche = discretionaryTotal + dupTotal + trialExposure

        val activeDebts = loanList.size + cardList.count { !it.isPaid && it.currentBill > 0.0 }

        val health = CreditHealthCalculator.calculate(
            transactions = txs,
            loans = loanList,
            creditCards = cardList,
            trials = trials,
            duplicates = dups,
            cibilPdfScore = cibilMeta.score,
            cibilPdfFileName = cibilMeta.fileName,
            cibilPdfAnalyzedAt = cibilMeta.analyzedAt
        )

        if (txs.isEmpty() && loanList.isEmpty() && cardList.isEmpty() && cibilMeta.score == null) {
            return DashboardAuditSnapshot(
                verdict = DailyVerdictReport(),
                metrics = QuickMetricsSummary(
                    faltuKharcheAmount = 0.0,
                    activeTrialsCount = 0,
                    activeDebtsCount = 0
                ),
                health = health
            )
        }

        val topMerchants = discretionary.map { it.merchantName }.distinct().take(2)
            .joinToString("/")
            .ifBlank { "discretionary spends" }
        val urgentTrial = trials.firstOrNull()
        val firstDup = dups.firstOrNull()

        val isWarning = trials.isNotEmpty() ||
            dups.isNotEmpty() ||
            discretionaryTotal > 1000.0 ||
            health.missedDuesCount > 0 ||
            health.utilizationPct > 30
        val headline = if (isWarning) {
            "⚠️ Chanakya's Audit: Money Leaks & AutoPay Traps Detected!"
        } else {
            "✅ Chanakya's Audit: Zero Leaks — Mandates & Cashflow Clean!"
        }

        val aisaKiyaLine = if (discretionaryTotal > 0.0) {
            "Aaj/Haal hi mein ${CurrencyFormatter.formatInr(discretionaryTotal, privacyBlur)} $topMerchants mein uda diye (${health.activeMandatesCount} active mandates chal rahe hain)."
        } else {
            "Abhi tak koi extra discretionary leak nahi mila — ${health.activeMandatesCount} active mandates monitored hain."
        }

        val zarooriKaamLine = when {
            health.missedDuesCount > 0 ->
                "${health.missedDuesCount} missed/overdue EMI ya card alert mila hai (Score ${health.score}/900) — turant clear karein warna CIBIL girega!"
            urgentTrial != null -> {
                val postPrice = CurrencyFormatter.formatInr(urgentTrial.postTrialAmount ?: 499.0, privacyBlur)
                val countdown = urgentTrial.trialExpiryTimestamp?.let {
                    CurrencyFormatter.formatCountdown(it)
                } ?: "jaldi"
                "$postPrice ka ${urgentTrial.merchantName} trial ($countdown) katne wala hai, abhi band karo!"
            }
            firstDup != null -> {
                val extra = CurrencyFormatter.formatInr(firstDup.totalOvercharged, privacyBlur)
                "${firstDup.merchantName} par $extra double charge hua hai — abhi refund email bhejo!"
            }
            else -> {
                "Bank PDF/screenshot upload karein ya SMS & Gmail sync fresh rakho aur credit utilization ${health.utilizationPct}% ke neeche maintain karo."
            }
        }

        val speechText = buildString {
            append(headline.replace("⚠️", "").replace("✅", "").trim())
            append(". Aisa Kiya: ")
            append(aisaKiyaLine)
            append(" Aur Yeh Zaroori Kaam Nahi Kiya: ")
            append(zarooriKaamLine)
        }

        return DashboardAuditSnapshot(
            verdict = DailyVerdictReport(
                isWarning = isWarning,
                headline = headline,
                aisaKiyaLine = aisaKiyaLine,
                zarooriKaamLine = zarooriKaamLine,
                speechText = speechText
            ),
            metrics = QuickMetricsSummary(
                faltuKharcheAmount = faltuKharche,
                activeTrialsCount = trials.size,
                activeDebtsCount = activeDebts
            ),
            health = health
        )
    }

    private val _isAppUnlocked = MutableStateFlow(true)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _overspendPopup = MutableStateFlow<OverspendPopupAlert?>(null)
    val overspendPopup: StateFlow<OverspendPopupAlert?> = _overspendPopup.asStateFlow()

    private val _showTopUpSheet = MutableStateFlow(false)
    val showTopUpSheet: StateFlow<Boolean> = _showTopUpSheet.asStateFlow()

    private val _topUpTriggerReason = MutableStateFlow<String?>(null)
    val topUpTriggerReason: StateFlow<String?> = _topUpTriggerReason.asStateFlow()

    private val _gmailSyncState = MutableStateFlow<GmailSyncState>(GmailSyncState.Idle)
    val gmailSyncState: StateFlow<GmailSyncState> = _gmailSyncState.asStateFlow()

    private val _lastSmsScanSummary = MutableStateFlow<SmsScanResult?>(null)
    val lastSmsScanSummary: StateFlow<SmsScanResult?> = _lastSmsScanSummary.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "Namaste! Main hoon Chanakya AI — aapka Money Munim. Poochhein Hinglish ya English mein apna spending hisaab, expiring trials, loans/cards, ya '+' dabakar Bank PDF / payment screenshot scan karke ledger mein save karein!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chanakyaState = MutableStateFlow<ChanakyaUiState>(ChanakyaUiState.Idle)
    val chanakyaState: StateFlow<ChanakyaUiState> = _chanakyaState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            TrialShieldScheduler.ensureChannels(context)
            repository.purgeLegacyDemoIfNeeded()
            val lockOn = prefs.biometricLockEnabled.first()
            _isAppUnlocked.value = !lockOn
        }
    }

    fun setUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
    }

    fun setVaultUnlocked(unlocked: Boolean) {
        _isVaultUnlocked.value = unlocked
    }

    fun generateBurnerAlias(purpose: String = "trial"): String {
        val cleanTag = purpose.lowercase().replace(Regex("[^a-z0-9]"), "").take(8).ifBlank { "shield" }
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        val suffix = (1..5).map { chars.random() }.joinToString("")
        val domain = listOf("mandatex-alias.in", "masked-shield.in", "burner-guard.in").random()
        val alias = "$cleanTag.$suffix@$domain"
        viewModelScope.launch {
            prefs.addBurnerAlias(alias)
        }
        _statusBanner.value = "Generated masked burner alias: $alias"
        return alias
    }

    fun saveMasterVaultProfile(
        panNumber: String,
        aadhaarMasked: String,
        primaryCardMask: String,
        primaryUpiHandle: String
    ) {
        viewModelScope.launch {
            prefs.saveMasterVaultProfile(panNumber, aadhaarMasked, primaryCardMask, primaryUpiHandle)
            _statusBanner.value = "Master Autofill Vault updated securely."
        }
    }

    fun togglePrivacyBlur() {
        viewModelScope.launch {
            prefs.setPrivacyBlurEnabled(!privacyBlurEnabled.value)
        }
    }

    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBiometricLockEnabled(enabled)
            if (!enabled) _isAppUnlocked.value = true
        }
    }

    fun toggleTtsVoiceReply(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setTtsVoiceReplyEnabled(enabled)
        }
    }

    fun updateUserProfile(name: String, bankEmail: String) {
        viewModelScope.launch {
            prefs.setUserProfile(name, bankEmail)
            _statusBanner.value = "Profile & bank support email updated."
        }
    }

    fun openTopUpSheet(reason: String? = null) {
        _topUpTriggerReason.value = reason
        _showTopUpSheet.value = true
    }

    fun dismissTopUpSheet() {
        _showTopUpSheet.value = false
        _topUpTriggerReason.value = null
    }

    fun simulatePaymentSuccess(amountInr: Int) {
        val validAmount = maxOf(5, amountInr)
        viewModelScope.launch {
            prefs.addCredits(validAmount)
            dismissTopUpSheet()
            _statusBanner.value = "Payment successful! Added $validAmount Credits (₹$validAmount) to your MandateX Wallet."
        }
    }

    fun ingestBankMessage(sender: String, body: String, source: String = "SMS") {
        viewModelScope.launch {
            val saved = repository.ingestMessage(sender, body, source)
            if (saved == null) {
                _statusBanner.value = "Could not detect a ₹ bank/UPI debit or mandate in that message."
                return@launch
            }
            val msg = buildString {
                append("Captured ${saved.merchantName} (₹${saved.amount.toInt()})")
                if (saved.isTrial) append(" • Trial Exit Shield armed!")
            }
            _statusBanner.value = msg

            val statuses = repository.getBudgetStatusesSnapshot()
            val hit = statuses.find { it.category == saved.category }
            if (hit != null && (hit.isWarning80 || hit.isExceeded100)) {
                _overspendPopup.value = OverspendPopupAlert(
                    categoryLabel = hit.category.label,
                    spent = hit.spent,
                    limit = hit.limit,
                    percent = (hit.percentage * 100).toInt()
                )
            }
        }
    }

    fun scanSmsInbox() {
        viewModelScope.launch {
            val result = SmsInboxScanner.scanRecentSmsInbox(context, dao, repository)
            _lastSmsScanSummary.value = result
            prefs.setLastSmsScanTimestamp(System.currentTimeMillis())
            _statusBanner.value = if (result.importedCount > 0) {
                "Scanned ${result.scannedMessages} SMS (6 mo): imported ${result.importedCount} items (${result.mandatesFound} mandates)."
            } else {
                "Scanned ${result.scannedMessages} SMS (6 mo) — no new bank/UPI mandates found."
            }
        }
    }

    fun setGmailAuthorizing() {
        _gmailSyncState.value = GmailSyncState.Authorizing
    }

    fun onGmailAuthError(message: String) {
        _gmailSyncState.value = GmailSyncState.Error(message)
        _statusBanner.value = message
    }

    fun clearGmailError() {
        if (_gmailSyncState.value is GmailSyncState.Error) {
            _gmailSyncState.value = GmailSyncState.Idle
        }
    }

    fun syncGmailWithAccessToken(accessToken: String) {
        viewModelScope.launch {
            _gmailSyncState.value = GmailSyncState.FetchingAndParsing("Connecting to Gmail inbox...")
            try {
                val userEmail = GmailSyncEngine.fetchUserEmail(accessToken)
                    ?: gmailConnectedEmail.value
                    ?: "Connected Gmail"
                prefs.setGmailConnected(userEmail)

                val existingIds = transactions.value
                    .mapNotNull { it.externalMessageId }
                    .filter { it.startsWith("gmail_") }
                    .toSet()

                _gmailSyncState.value = GmailSyncState.FetchingAndParsing(
                    "Scanning last 6 months of invoices, mandates & trials..."
                )
                val fetchResult = GmailSyncEngine.fetchRecentFinancialEmails(
                    accessToken = accessToken,
                    alreadySyncedExternalIds = existingIds
                )
                val rawEmails = fetchResult.getOrElse { err ->
                    val errMsg = err.localizedMessage ?: "Failed to query Gmail API."
                    _gmailSyncState.value = GmailSyncState.Error(errMsg)
                    _statusBanner.value = errMsg
                    return@launch
                }

                if (rawEmails.isEmpty()) {
                    val now = System.currentTimeMillis()
                    prefs.setGmailConnected(userEmail, now)
                    _gmailSyncState.value = GmailSyncState.Idle
                    _statusBanner.value = "Gmail synced ($userEmail) — no new autopay, trial, or invoice emails in last 6 months."
                    return@launch
                }

                _gmailSyncState.value = GmailSyncState.FetchingAndParsing(
                    "Chanakya AI is auditing ${rawEmails.size} email(s) for hidden subscriptions..."
                )
                val parseResult = chanakyaAi.parseGmailMessages(rawEmails)
                val parsedMandates = parseResult.getOrElse { err ->
                    val errMsg = err.localizedMessage ?: "Chanakya AI failed to parse Gmail messages."
                    _gmailSyncState.value = GmailSyncState.Error(errMsg)
                    _statusBanner.value = errMsg
                    return@launch
                }

                val (added, merged) = repository.ingestGmailMandates(parsedMandates)
                val now = System.currentTimeMillis()
                prefs.setGmailConnected(userEmail, now)
                _gmailSyncState.value = GmailSyncState.Idle
                _statusBanner.value = when {
                    added > 0 && merged > 0 -> "Gmail Auto-Sync complete! Added $added new services & merged $merged with SMS."
                    added > 0 -> "Gmail Auto-Sync complete! Discovered $added new subscription/mandate(s)."
                    merged > 0 -> "Gmail Auto-Sync verified $merged existing SMS mandate(s) with email receipts."
                    else -> "Gmail synced ($userEmail) — checked ${rawEmails.size} emails, no recurring charges added."
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Gmail Auto-Sync encountered an unexpected error."
                _gmailSyncState.value = GmailSyncState.Error(msg)
                _statusBanner.value = msg
            }
        }
    }

    fun disconnectGmail() {
        viewModelScope.launch {
            runCatching { GmailSyncEngine.getGoogleSignInClient(context).signOut() }
            prefs.setGmailConnected(null)
            _gmailSyncState.value = GmailSyncState.Idle
            _statusBanner.value = "Disconnected Gmail Online Auto-Sync."
        }
    }

    fun cancelTrial(trial: TransactionEntity) {
        viewModelScope.launch {
            repository.markTrialCancelled(trial)
            _statusBanner.value = "${trial.merchantName} marked cancelled! Saved ₹${((trial.postTrialAmount ?: 499.0) - trial.amount).toInt()}/mo."
        }
    }

    fun createOrOpenCancellationForTrial(trial: TransactionEntity, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            val existing = cancellations.value.firstOrNull {
                it.merchantName.equals(trial.merchantName, ignoreCase = true)
            }
            val id = existing?.id ?: repository.addCancellationRequest(
                CancellationRequestEntity(
                    merchantName = trial.merchantName,
                    subtitle = "Trial Exit Shield (₹${trial.amount.toInt()} -> ₹${(trial.postTrialAmount ?: 499.0).toInt()}/mo)",
                    upiId = trial.upiId ?: "${trial.merchantName.lowercase().replace(" ", "")}@upi",
                    bankName = trial.bankName,
                    accountLast4 = trial.accountLast4,
                    amount = trial.postTrialAmount ?: 499.0,
                    status = "IN PROGRESS",
                    currentStep = 1,
                    notes = "Trial price-hike alert active. Revoke UPI AutoPay and notify bank."
                )
            )
            onReady(id)
        }
    }

    fun createOrOpenCancellationForService(tx: TransactionEntity, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            val existing = cancellations.value.firstOrNull {
                it.merchantName.equals(tx.merchantName, ignoreCase = true)
            }
            val id = existing?.id ?: repository.addCancellationRequest(
                CancellationRequestEntity(
                    merchantName = tx.merchantName,
                    subtitle = buildString {
                        append(tx.paymentSource.ifBlank { "${tx.bankName} XX${tx.accountLast4}" })
                        if (!tx.mandateRefId.isNullOrBlank()) append(" • Ref ${tx.mandateRefId}")
                    },
                    upiId = tx.upiId ?: "${tx.merchantName.lowercase().replace(" ", "")}@upi",
                    bankName = tx.bankName,
                    accountLast4 = tx.accountLast4,
                    amount = tx.postTrialAmount ?: tx.amount,
                    status = "IN PROGRESS",
                    currentStep = 1,
                    notes = "Initiated from Unified Active Services (${tx.source}). Revoke UPI AutoPay / Bank e-Mandate."
                )
            )
            onReady(id)
        }
    }

    fun advanceCancellationStep(item: CancellationRequestEntity, nextStep: Int, newStatus: String) {
        viewModelScope.launch {
            repository.updateCancellationStep(item, nextStep, newStatus)
            if (nextStep >= 4 || newStatus == "COMPLETED") {
                val matchingTrial = activeTrials.value.firstOrNull {
                    it.merchantName.equals(item.merchantName, ignoreCase = true)
                }
                if (matchingTrial != null) {
                    repository.markTrialCancelled(matchingTrial)
                }
                _statusBanner.value = "${item.merchantName} cancellation completed!"
            } else {
                _statusBanner.value = "${item.merchantName} status updated: $newStatus"
            }
        }
    }

    fun triggerImmediateTrialTestAlert(trial: TransactionEntity) {
        TrialShieldScheduler.showTrialExitNotification(
            context = context,
            txId = trial.id,
            merchant = trial.merchantName,
            trialPrice = trial.amount,
            postPrice = trial.postTrialAmount ?: 499.0
        )
        _statusBanner.value = "Sent Trial Exit Shield notification for ${trial.merchantName}."
    }

    fun payLoanEmi(loan: LoanEmiEntity) {
        viewModelScope.launch {
            repository.payLoanEmi(loan)
            _statusBanner.value = "Recorded ₹${loan.emiAmount.toInt()} EMI payment for ${loan.loanName}."
        }
    }

    fun addLoan(
        loanName: String,
        lenderName: String,
        accountLast4: String,
        emiAmount: Double,
        remainingMonths: Int,
        totalMonths: Int,
        dueDay: Int,
        interestRate: Double
    ) {
        viewModelScope.launch {
            repository.addLoan(
                LoanEmiEntity(
                    loanName = loanName,
                    lenderName = lenderName,
                    accountLast4 = accountLast4.ifBlank { "0000" },
                    emiAmount = emiAmount,
                    remainingMonths = remainingMonths,
                    totalMonths = maxOf(remainingMonths, totalMonths),
                    nextDueDayOfMonth = dueDay.coerceIn(1, 28),
                    interestRate = interestRate,
                    autoDebitEnabled = true
                )
            )
            _statusBanner.value = "Added $loanName to Loans tracker."
        }
    }

    fun payCreditCardBill(card: CreditCardEntity) {
        viewModelScope.launch {
            if (card.currentBill <= 0.0) {
                _statusBanner.value = "${card.cardName} bill is already cleared."
                return@launch
            }
            repository.payCreditCardBill(card)
            _statusBanner.value = "Marked ₹${card.currentBill.toInt()} bill paid for ${card.cardName}."
        }
    }

    fun addCreditCard(
        cardName: String,
        bankName: String,
        last4: String,
        currentBill: Double,
        creditLimit: Double,
        dueDateLabel: String,
        minDue: Double
    ) {
        val resolvedLast4 = last4.ifBlank { "0000" }
        viewModelScope.launch {
            repository.addCreditCard(
                CreditCardEntity(
                    cardName = cardName,
                    bankName = bankName,
                    last4 = resolvedLast4,
                    currentBill = currentBill,
                    creditLimit = maxOf(currentBill, creditLimit),
                    dueDateLabel = dueDateLabel.ifBlank { "Not set" },
                    minDue = minDue
                )
            )
            _statusBanner.value = "Added $cardName XX$resolvedLast4."
        }
    }

    fun updateCategoryBudget(category: SpendCategory, limit: Double) {
        viewModelScope.launch {
            repository.updateCategoryBudget(category, limit)
            _statusBanner.value = "${category.label} monthly cap set to ₹${limit.toInt()}."
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            prefs.clearVaultAndProfileData()
            _statusBanner.value = "All local data cleared to 0."
        }
    }

    fun dismissOverspendPopup() {
        _overspendPopup.value = null
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun askChanakyaText(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val allowed = prefs.consumeTextQuota()
            if (!allowed) {
                openTopUpSheet("Free text chat quota (15/15) finished! Top up Credits (1 Credit / chat) to keep asking Chanakya AI.")
                return@launch
            }
            runChanakyaTextInternal(trimmed)
        }
    }

    fun askChanakyaVoice(query: String, onAiReplyReady: ((String) -> Unit)? = null) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val allowed = prefs.consumeVoiceQuota()
            if (!allowed) {
                openTopUpSheet("Free Voice Mic quota (5/5) finished! Voice queries cost 2 Credits. Top up ₹5+ to continue.")
                return@launch
            }
            val replyText = runChanakyaTextInternal(trimmed)
            if (replyText != null && ttsVoiceReplyEnabled.value) {
                onAiReplyReady?.invoke(replyText)
            }
        }
    }

    private suspend fun runChanakyaTextInternal(trimmed: String): String? {
        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            isUser = true,
            text = trimmed
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _chanakyaState.value = ChanakyaUiState.Thinking

        val reply = chanakyaAi.ask(trimmed)
        _chatMessages.value = _chatMessages.value + reply
        _chanakyaState.value = if (reply.isError) {
            ChanakyaUiState.Error(reply.text)
        } else {
            ChanakyaUiState.Idle
        }
        return if (reply.isError) null else reply.text
    }

    fun analyzeAttachmentUri(uri: Uri, prompt: String = "") {
        viewModelScope.launch {
            val allowed = prefs.consumeUploadQuota()
            if (!allowed) {
                openTopUpSheet("Free document/screenshot upload (1/1) used! File & PDF scans cost 2 Credits. Top up ₹5+ to continue.")
                return@launch
            }

            val filePayload = withContext(Dispatchers.IO) {
                readUriBytesAndMeta(uri)
            }
            if (filePayload == null) {
                _statusBanner.value = "Could not read selected file or image."
                return@launch
            }

            val (fileName, mimeType, bytes) = filePayload
            val userMsg = ChatMessage(
                id = "user_doc_${System.currentTimeMillis()}",
                isUser = true,
                text = prompt.ifBlank { "Uploaded $fileName for MandateX & AutoPay audit" },
                attachmentName = fileName
            )
            _chatMessages.value = _chatMessages.value + userMsg
            _chanakyaState.value = ChanakyaUiState.Thinking

            val audit = chanakyaAi.analyzeAttachment(
                bytes = bytes,
                mimeType = mimeType,
                fileName = fileName,
                userPrompt = prompt
            )
            val baseReply = audit.chatMessage
            if (!baseReply.isError && (
                    audit.extractedTransactions.isNotEmpty() ||
                        audit.extractedCreditCards.isNotEmpty() ||
                        audit.extractedLoans.isNotEmpty()
                    )
            ) {
                val (txCount, cardCount, loanCount) = repository.ingestParsedAttachmentData(
                    transactions = audit.extractedTransactions,
                    creditCards = audit.extractedCreditCards,
                    loans = audit.extractedLoans
                )
                val savedNote = buildString {
                    append("\n\nSaved to local storage: ")
                    val parts = buildList {
                        if (txCount > 0) add("$txCount transaction(s)")
                        if (cardCount > 0) add("$cardCount card(s)")
                        if (loanCount > 0) add("$loanCount loan(s)")
                    }
                    append(if (parts.isNotEmpty()) parts.joinToString(", ") else "already up to date")
                }
                _chatMessages.value = _chatMessages.value + baseReply.copy(text = baseReply.text + savedNote)
                _statusBanner.value = "Parsed $fileName: saved $txCount tx, $cardCount card(s), $loanCount loan(s)."
            } else {
                _chatMessages.value = _chatMessages.value + baseReply
            }

            _chanakyaState.value = if (baseReply.isError) {
                ChanakyaUiState.Error(baseReply.text)
            } else {
                ChanakyaUiState.Idle
            }
        }
    }

    fun analyzeCibilPdfUri(uri: Uri) {
        viewModelScope.launch {
            val allowed = prefs.consumeUploadQuota()
            if (!allowed) {
                openTopUpSheet("Free PDF upload (1/1) used! Analyzing an Official CIBIL Report PDF costs 2 Credits. Top up ₹5+ to continue.")
                return@launch
            }

            val filePayload = withContext(Dispatchers.IO) {
                readUriBytesAndMeta(uri)
            }
            if (filePayload == null) {
                _statusBanner.value = "Could not read selected CIBIL PDF file."
                return@launch
            }

            val (fileName, mimeType, bytes) = filePayload
            val userMsg = ChatMessage(
                id = "user_cibil_${System.currentTimeMillis()}",
                isUser = true,
                text = "Uploaded Official CIBIL PDF ($fileName) — find bureau errors & suggest score-boosting tips",
                attachmentName = fileName
            )
            _chatMessages.value = _chatMessages.value + userMsg
            _chanakyaState.value = ChanakyaUiState.Thinking

            val currentHealth = dashboardAudit.value.health
            val localContextSummary = buildString {
                appendLine("MandateX SMS/Gmail Estimated Score: ${currentHealth.score}/900 (${currentHealth.statusLabel})")
                appendLine("Credit Utilization: ${currentHealth.utilizationPct}%, On-Time EMIs: ${currentHealth.onTimeEmisPct}%")
                appendLine("Tracked Loans: ${currentHealth.activeLoansCount}, Missed Dues Alerts: ${currentHealth.missedDuesCount}, On-Time Events: ${currentHealth.onTimePaymentsCount}")
                if (currentHealth.positiveReasons.isNotEmpty()) {
                    appendLine("Positives: ${currentHealth.positiveReasons.joinToString("; ")}")
                }
                if (currentHealth.negativeReasons.isNotEmpty()) {
                    appendLine("Drags: ${currentHealth.negativeReasons.joinToString("; ")}")
                }
            }

            val auditResult = chanakyaAi.analyzeCibilReportPdf(
                bytes = bytes,
                mimeType = mimeType,
                fileName = fileName,
                localEstimateSummary = localContextSummary,
                userName = userName.value
            )
            val reply = auditResult.chatMessage
            _chatMessages.value = _chatMessages.value + reply
            _chanakyaState.value = if (reply.isError) {
                ChanakyaUiState.Error(reply.text)
            } else {
                ChanakyaUiState.Idle
            }

            if (!reply.isError) {
                prefs.saveCibilAuditMeta(
                    score = auditResult.extractedScore,
                    fileName = fileName,
                    analyzedAt = System.currentTimeMillis()
                )
                _statusBanner.value = auditResult.extractedScore?.let {
                    "CIBIL PDF audited! Official score detected: $it/900."
                } ?: "Official CIBIL PDF audited by Chanakya AI!"
            }
        }
    }

    private fun readUriBytesAndMeta(uri: Uri): Triple<String, String, ByteArray>? {
        return runCatching {
            val cr = context.contentResolver
            val mimeType = cr.getType(uri) ?: "application/octet-stream"
            var fileName = "document"
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) fileName = cursor.getString(idx) ?: fileName
                }
            }
            val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return null
            Triple(fileName, mimeType, bytes)
        }.getOrNull()
    }

    fun tryConsumeEmailDraftQuota(onAllowed: () -> Unit) {
        viewModelScope.launch {
            val allowed = prefs.consumeEmailQuota()
            if (!allowed) {
                openTopUpSheet("Free Bank/RBI email draft (1/1) used! Generating another official email draft costs 1 Credit.")
            } else {
                onAllowed()
            }
        }
    }

    fun resetChanakyaChat() {
        chanakyaAi.resetConversation()
        _chatMessages.value = listOf(
            ChatMessage(
                id = "welcome_${System.currentTimeMillis()}",
                isUser = false,
                text = "Naya session shuru! Poochhein apne kharche, active trials, duplicate charges, loans/cards, ya '+' se PDF/screenshot scan karein."
            )
        )
        _chanakyaState.value = ChanakyaUiState.Idle
    }

    fun exportAndSharePdf() {
        viewModelScope.launch {
            val allowed = prefs.consumePdfExportCredits()
            if (!allowed) {
                openTopUpSheet("CA / Tax PDF Statement Export requires 2 Credits. Top up ₹5+ (₹1 = 1 Credit) to generate & share PDF.")
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val txs = repository.getAllTransactionsSnapshot()
                    val file = StatementExporter.exportPdfStatement(context, txs, userName.value)
                    StatementExporter.shareFile(context, file, "application/pdf")
                    file.name
                }
            }
            result.onSuccess { name ->
                _statusBanner.value = "Generated $name (2 Credits used) — ready to share!"
            }.onFailure { err ->
                Toast.makeText(context, "PDF Export failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportAndShareCsv() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val txs = repository.getAllTransactionsSnapshot()
                    val file = StatementExporter.exportCsvStatement(context, txs)
                    StatementExporter.shareFile(context, file, "text/csv")
                    file.name
                }
            }
            result.onSuccess { name ->
                _statusBanner.value = "Generated $name — ready to share!"
            }.onFailure { err ->
                Toast.makeText(context, "CSV Export failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun addManualEntry(
        merchant: String,
        amount: Double,
        type: TransactionType,
        category: SpendCategory,
        accountLast4: String
    ) {
        viewModelScope.launch {
            repository.addManualTransaction(
                TransactionEntity(
                    amount = amount,
                    merchantName = merchant,
                    type = type,
                    category = category,
                    accountLast4 = accountLast4.ifBlank { "0000" },
                    bankName = "Manual Entry",
                    source = "MANUAL",
                    isTrial = type == TransactionType.AUTOPAY && amount <= 10.0,
                    trialExpiryTimestamp = if (type == TransactionType.AUTOPAY && amount <= 10.0) {
                        System.currentTimeMillis() + 40 * 3_600_000L
                    } else null,
                    postTrialAmount = if (type == TransactionType.AUTOPAY && amount <= 10.0) 499.0 else null
                )
            )
            _statusBanner.value = "Added $merchant (₹${amount.toInt()}) to ledger."
        }
    }
}
