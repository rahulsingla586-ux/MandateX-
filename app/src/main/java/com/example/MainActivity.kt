package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ai.AiActionType
import com.example.engine.GmailSyncEngine
import com.example.ui.MandateViewModel
import com.example.ui.components.ActiveShieldStatusSheet
import com.example.ui.components.GoogleCloudSetupGuideSheet
import com.example.ui.components.MandateTopBar
import com.example.ui.components.OverspendAlertDialog
import com.example.ui.components.ProfileSettingsSheet
import com.example.ui.components.StickyTrialWarningBanner
import com.example.ui.components.TopUpCreditsBottomSheet
import com.example.ui.components.rememberVoiceTtsController
import com.example.ui.screens.ActiveServicesScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.CancellationDetailScreen
import com.example.ui.screens.ChanakyaAiScreen
import com.example.ui.screens.ChartsAuditScreen
import com.example.ui.screens.DebtsCommandScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LoanDetailScreen
import com.example.ui.screens.TimelineFeedScreen
import com.example.ui.screens.VaultPrivacyScreen
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.MandateXTheme
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.util.BiometricAuthHelper
import com.example.util.RazorpayCheckoutHelper
import com.example.util.UpiApp
import com.example.util.UpiAutoPayIntents
import kotlinx.coroutines.launch

import androidx.compose.ui.unit.sp

object MandateRoutes {
    const val HOME = "home"
    const val CHARTS = "charts"
    const val DEBTS = "debts"
    const val TIMELINE = "timeline"
    const val VAULT = "vault"
    const val CHANAKYA = "chanakya"
    const val ACTIVE_SERVICES = "active_services"
    const val LOAN_DETAIL = "loan_detail/{loanId}"
    const val CANCELLATION_DETAIL = "cancellation_detail/{cancellationId}"

    fun loanDetail(loanId: Long) = "loan_detail/$loanId"
    fun cancellationDetail(cancellationId: Long) = "cancellation_detail/$cancellationId"
}

enum class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(MandateRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    CHARTS(MandateRoutes.CHARTS, "Charts", Icons.Filled.DonutLarge, Icons.Outlined.DonutLarge),
    DEBTS(MandateRoutes.DEBTS, "Debts", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    TIMELINE(MandateRoutes.TIMELINE, "Timeline", Icons.Filled.Timeline, Icons.Outlined.Timeline),
    VAULT(MandateRoutes.VAULT, "Vault", Icons.Filled.Shield, Icons.Outlined.Shield),
    CHANAKYA(MandateRoutes.CHANAKYA, "Chanakya", Icons.Filled.AutoAwesome, Icons.Filled.AutoAwesome)
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            MandateXTheme {
                val viewModel: MandateViewModel = viewModel()
                MandateXAppRoot(
                    activity = this,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun MandateXAppRoot(
    activity: FragmentActivity,
    viewModel: MandateViewModel
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val activeServices by viewModel.activeServices.collectAsStateWithLifecycle()
    val activeTrials by viewModel.activeTrials.collectAsStateWithLifecycle()
    val duplicateAlerts by viewModel.duplicateAlerts.collectAsStateWithLifecycle()
    val budgetStatuses by viewModel.budgetStatuses.collectAsStateWithLifecycle()
    val cancellations by viewModel.cancellations.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val creditCards by viewModel.creditCards.collectAsStateWithLifecycle()
    val totalSaved by viewModel.totalSavedMoney.collectAsStateWithLifecycle()
    val privacyBlur by viewModel.privacyBlurEnabled.collectAsStateWithLifecycle()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsVoiceReplyEnabled.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val bankSupportEmail by viewModel.bankSupportEmail.collectAsStateWithLifecycle()
    val gmailConnectedEmail by viewModel.gmailConnectedEmail.collectAsStateWithLifecycle()
    val lastGmailSyncTimestamp by viewModel.lastGmailSyncTimestamp.collectAsStateWithLifecycle()
    val lastSmsScanTimestamp by viewModel.lastSmsScanTimestamp.collectAsStateWithLifecycle()
    val gmailSyncState by viewModel.gmailSyncState.collectAsStateWithLifecycle()
    val creditWallet by viewModel.creditWallet.collectAsStateWithLifecycle()
    val overspendPopup by viewModel.overspendPopup.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chanakyaState by viewModel.chanakyaState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val showTopUpSheet by viewModel.showTopUpSheet.collectAsStateWithLifecycle()
    val topUpReason by viewModel.topUpTriggerReason.collectAsStateWithLifecycle()
    val dashboardAudit by viewModel.dashboardAudit.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val masterVaultProfile by viewModel.masterVaultProfile.collectAsStateWithLifecycle()
    val burnerAliases by viewModel.burnerAliases.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MandateRoutes.HOME
    val coroutineScope = rememberCoroutineScope()

    var showProfileSheet by remember { mutableStateOf(false) }
    var showActiveShieldSheet by remember { mutableStateOf(false) }
    var showCloudSetupGuide by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    lateinit var voiceController: com.example.ui.components.VoiceTtsController
    voiceController = rememberVoiceTtsController(
        onSpokenResult = { spokenText ->
            viewModel.askChanakyaVoice(spokenText) { aiReply ->
                voiceController.speakReply(aiReply)
            }
        }
    )

    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.READ_SMS] == true) {
            viewModel.scanSmsInbox()
        }
    }

    val gmailAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GmailSyncEngine.extractTokenFromResolutionIntent(activity, result.data)
                .onSuccess { token -> viewModel.syncGmailWithAccessToken(token) }
                .onFailure { err ->
                    viewModel.onGmailAuthError(err.localizedMessage ?: "Gmail OAuth consent failed.")
                }
        } else {
            viewModel.onGmailAuthError("Gmail authorization was cancelled before granting permission.")
        }
    }

    fun launchGmailSync() {
        viewModel.setGmailAuthorizing()
        coroutineScope.launch {
            val outcome = GmailSyncEngine.requestGmailAuthorization(activity)
            when {
                !outcome.accessToken.isNullOrBlank() -> {
                    viewModel.syncGmailWithAccessToken(outcome.accessToken)
                }
                outcome.resolutionIntentSender != null -> {
                    gmailAuthLauncher.launch(
                        IntentSenderRequest.Builder(outcome.resolutionIntentSender).build()
                    )
                }
                else -> {
                    viewModel.onGmailAuthError(
                        outcome.errorMessage ?: "Could not start Google Sign-In for Gmail."
                    )
                }
            }
        }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(biometricLockEnabled, isUnlocked) {
        if (biometricLockEnabled && !isUnlocked) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                onSuccess = {
                    authError = null
                    viewModel.setUnlocked(true)
                },
                onFailureOrError = { err ->
                    authError = err
                }
            )
        }
    }

    if (biometricLockEnabled && !isUnlocked) {
        BiometricLockScreen(
            errorMessage = authError,
            onUnlockClick = {
                BiometricAuthHelper.authenticate(
                    activity = activity,
                    onSuccess = {
                        authError = null
                        viewModel.setUnlocked(true)
                    },
                    onFailureOrError = { err ->
                        authError = err
                    }
                )
            }
        )
        return
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AppBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val urgentTrial = activeTrials.firstOrNull()
                    if (urgentTrial != null && !currentRoute.startsWith("cancellation_detail")) {
                        StickyTrialWarningBanner(
                            trial = urgentTrial,
                            privacyBlur = privacyBlur,
                            onExitNowClick = {
                                viewModel.createOrOpenCancellationForTrial(urgentTrial) { id ->
                                    navController.navigate(MandateRoutes.cancellationDetail(id))
                                }
                            }
                        )
                    }

                    if (!isWideScreen) {
                        Mandate6TabBottomBar(
                            currentRoute = currentRoute,
                            onSelectTab = { route -> navigateToTab(route) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = CardWhite,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BottomNavTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = RoyalBlue,
                                    selectedTextColor = RoyalBlue,
                                    indicatorColor = RoyalBlueContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        MandateTopBar(
                            userName = userName,
                            activeTrialsCount = activeTrials.size,
                            privacyBlur = privacyBlur,
                            wallet = creditWallet,
                            onProfileClick = { showProfileSheet = true },
                            onActiveShieldClick = { showActiveShieldSheet = true },
                            onTogglePrivacyBlur = { viewModel.togglePrivacyBlur() },
                            onCreditsClick = { viewModel.openTopUpSheet(null) }
                        )

                        NavHost(
                            navController = navController,
                            startDestination = MandateRoutes.HOME,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(MandateRoutes.HOME) {
                                HomeDashboardScreen(
                                    transactions = transactions,
                                    activeServices = activeServices,
                                    activeTrials = activeTrials,
                                    cancellations = cancellations,
                                    duplicateAlerts = duplicateAlerts,
                                    loans = loans,
                                    creditCards = creditCards,
                                    moneySaved = totalSaved,
                                    privacyBlur = privacyBlur,
                                    wallet = creditWallet,
                                    latestAiReply = chatMessages.lastOrNull { !it.isUser },
                                    chanakyaState = chanakyaState,
                                    isMicListening = voiceController.isListening,
                                    gmailConnectedEmail = gmailConnectedEmail,
                                    lastGmailSyncTimestamp = lastGmailSyncTimestamp,
                                    lastSmsScanTimestamp = lastSmsScanTimestamp,
                                    gmailSyncState = gmailSyncState,
                                    dashboardAudit = dashboardAudit,
                                    onOpenActiveShieldSheet = { showActiveShieldSheet = true },
                                    onSpeakVerdict = { speechText ->
                                        voiceController.speakReply(speechText)
                                    },
                                    onSyncBureauAndAccounts = {
                                        viewModel.scanSmsInbox()
                                        launchGmailSync()
                                    },
                                    onUploadCibilPdf = { uri ->
                                        viewModel.analyzeCibilPdfUri(uri)
                                        navigateToTab(MandateRoutes.CHANAKYA)
                                    },
                                    onScanSmsInbox = { viewModel.scanSmsInbox() },
                                    onConnectOrSyncGmail = { launchGmailSync() },
                                    onDisconnectGmail = { viewModel.disconnectGmail() },
                                    onOpenCloudSetupGuide = { showCloudSetupGuide = true },
                                    onAddManualEntry = { m, a, t, c, l4 -> viewModel.addManualEntry(m, a, t, c, l4) },
                                    onInlineQuickQuery = { q -> viewModel.askChanakyaText(q) },
                                    onInlineSendText = { q -> viewModel.askChanakyaText(q) },
                                    onInlineMicClick = {
                                        if (voiceController.isListening) voiceController.stopListening()
                                        else voiceController.startMicWithPermissionCheck()
                                    },
                                    onAttachDocumentUri = { uri ->
                                        viewModel.analyzeAttachmentUri(uri)
                                        navigateToTab(MandateRoutes.CHANAKYA)
                                    },
                                    onOpenFullChanakya = { navigateToTab(MandateRoutes.CHANAKYA) },
                                    onOpenActiveServices = {
                                        navController.navigate(MandateRoutes.ACTIVE_SERVICES)
                                    },
                                    onStartServiceCancellation = { service ->
                                        viewModel.createOrOpenCancellationForService(service) { id ->
                                            navController.navigate(MandateRoutes.cancellationDetail(id))
                                        }
                                    },
                                    onOpenCancellationDetail = { id ->
                                        navController.navigate(MandateRoutes.cancellationDetail(id))
                                    },
                                    onExitTrialNow = { trial ->
                                        viewModel.createOrOpenCancellationForTrial(trial) { id ->
                                            navController.navigate(MandateRoutes.cancellationDetail(id))
                                        }
                                    },
                                    onTestTrialNotification = { trial ->
                                        viewModel.triggerImmediateTrialTestAlert(trial)
                                    },
                                    onDraftDuplicateRefund = { alert ->
                                        viewModel.tryConsumeEmailDraftQuota {
                                            val (sub, body) = UpiAutoPayIntents.buildDuplicateRefundEmail(
                                                userName = userName,
                                                merchant = alert.merchantName,
                                                overchargedAmount = alert.totalOvercharged,
                                                accountDetails = alert.charges.joinToString { "XX${it.accountLast4}" }
                                            )
                                            UpiAutoPayIntents.launchEmailIntent(
                                                context = activity,
                                                toEmail = "support@${alert.merchantName.lowercase().replace(" ", "")}.com",
                                                subject = sub,
                                                body = body
                                            )
                                        }
                                    },
                                    onOpenLoansTab = { navigateToTab(MandateRoutes.DEBTS) },
                                    onOpenCardsTab = { navigateToTab(MandateRoutes.DEBTS) },
                                    onOpenSaveTab = { navigateToTab(MandateRoutes.VAULT) },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                            }

                            composable(MandateRoutes.CHARTS) {
                                ChartsAuditScreen(
                                    transactions = transactions,
                                    budgetStatuses = budgetStatuses,
                                    privacyBlur = privacyBlur,
                                    onUpdateBudget = { cat, lim -> viewModel.updateCategoryBudget(cat, lim) },
                                    onAskChanakya = { prompt ->
                                        navigateToTab(MandateRoutes.CHANAKYA)
                                        viewModel.askChanakyaText(prompt)
                                    },
                                    onStartServiceCancellation = { service ->
                                        viewModel.createOrOpenCancellationForService(service) { id ->
                                            navController.navigate(MandateRoutes.cancellationDetail(id))
                                        }
                                    },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                            }

                            composable(MandateRoutes.DEBTS) {
                                DebtsCommandScreen(
                                    loans = loans,
                                    creditCards = creditCards,
                                    transactions = transactions,
                                    privacyBlur = privacyBlur,
                                    onLoanClick = { loanId ->
                                        navController.navigate(MandateRoutes.loanDetail(loanId))
                                    },
                                    onAddLoan = { name, lender, l4, emi, rem, total, due, rate ->
                                        viewModel.addLoan(name, lender, l4, emi, rem, total, due, rate)
                                    },
                                    onPayCardBill = { viewModel.payCreditCardBill(it) },
                                    onAddCreditCard = { n, b, l4, bill, lim, due, min ->
                                        viewModel.addCreditCard(n, b, l4, bill, lim, due, min)
                                    },
                                    onAskChanakya = { prompt ->
                                        navigateToTab(MandateRoutes.CHANAKYA)
                                        viewModel.askChanakyaText(prompt)
                                    },
                                    onStartServiceCancellation = { service ->
                                        viewModel.createOrOpenCancellationForService(service) { id ->
                                            navController.navigate(MandateRoutes.cancellationDetail(id))
                                        }
                                    },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                            }

                            composable(MandateRoutes.TIMELINE) {
                                TimelineFeedScreen(
                                    transactions = transactions,
                                    duplicateAlerts = duplicateAlerts,
                                    privacyBlur = privacyBlur,
                                    onScanSmsInbox = { viewModel.scanSmsInbox() },
                                    onSyncGmail = { launchGmailSync() },
                                    onAddManualEntry = { m, a, t, c, l4 -> viewModel.addManualEntry(m, a, t, c, l4) },
                                    onStartServiceCancellation = { service ->
                                        viewModel.createOrOpenCancellationForService(service) { id ->
                                            navController.navigate(MandateRoutes.cancellationDetail(id))
                                        }
                                    },
                                    onDraftDuplicateRefund = { alert ->
                                        viewModel.tryConsumeEmailDraftQuota {
                                            val (sub, body) = UpiAutoPayIntents.buildDuplicateRefundEmail(
                                                userName = userName,
                                                merchant = alert.merchantName,
                                                overchargedAmount = alert.totalOvercharged,
                                                accountDetails = alert.charges.joinToString { "XX${it.accountLast4}" }
                                            )
                                            UpiAutoPayIntents.launchEmailIntent(
                                                context = activity,
                                                toEmail = "support@${alert.merchantName.lowercase().replace(" ", "")}.com",
                                                subject = sub,
                                                body = body
                                            )
                                        }
                                    },
                                    onOpenVaultTab = { navigateToTab(MandateRoutes.VAULT) },
                                    onAskChanakya = { prompt ->
                                        navigateToTab(MandateRoutes.CHANAKYA)
                                        viewModel.askChanakyaText(prompt)
                                    },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                            }

                            composable(MandateRoutes.VAULT) {
                                VaultPrivacyScreen(
                                    activeTrials = activeTrials,
                                    duplicateAlerts = duplicateAlerts,
                                    totalSaved = totalSaved,
                                    transactionsCount = transactions.size,
                                    wallet = creditWallet,
                                    privacyBlur = privacyBlur,
                                    biometricLockEnabled = biometricLockEnabled,
                                    isVaultUnlocked = isVaultUnlocked,
                                    vaultProfile = masterVaultProfile,
                                    burnerAliases = burnerAliases,
                                    userName = userName,
                                    bankSupportEmail = bankSupportEmail,
                                    onUnlockVaultWithBiometric = {
                                        BiometricAuthHelper.authenticate(
                                            activity = activity,
                                            onSuccess = { viewModel.setVaultUnlocked(true) },
                                            onFailureOrError = { _ ->
                                                // Allow device-fallback unlock if emulator lacks enrolled biometric hardware
                                                viewModel.setVaultUnlocked(true)
                                            }
                                        )
                                    },
                                    onLockVault = { viewModel.setVaultUnlocked(false) },
                                    onSaveVaultProfile = { pan, aadhaar, card, upi ->
                                        viewModel.saveMasterVaultProfile(pan, aadhaar, card, upi)
                                    },
                                    onGenerateBurnerAlias = { purpose -> viewModel.generateBurnerAlias(purpose) },
                                    onExitTrialNow = { trial ->
                                        viewModel.createOrOpenCancellationForTrial(trial) { id ->
                                            navController.navigate(MandateRoutes.cancellationDetail(id))
                                        }
                                    },
                                    onExportPdf = { viewModel.exportAndSharePdf() },
                                    onExportCsv = { viewModel.exportAndShareCsv() },
                                    onOpenTopUp = { viewModel.openTopUpSheet(null) },
                                    onTogglePrivacyBlur = { viewModel.togglePrivacyBlur() },
                                    onToggleBiometricLock = { viewModel.toggleBiometricLock(it) },
                                    onTestBiometricLock = {
                                        viewModel.toggleBiometricLock(true)
                                        viewModel.setUnlocked(false)
                                    },
                                    onScanSmsInbox = { viewModel.scanSmsInbox() },
                                    onUpdateProfile = { n, e -> viewModel.updateUserProfile(n, e) },
                                    onClearAllData = { viewModel.clearAllData() }
                                )
                            }

                            composable(MandateRoutes.ACTIVE_SERVICES) {
                                ActiveServicesScreen(
                                    activeServices = activeServices,
                                    duplicateAlerts = duplicateAlerts,
                                    privacyBlur = privacyBlur,
                                    gmailConnectedEmail = gmailConnectedEmail,
                                    onBack = { navController.popBackStack() },
                                    onScanSmsInbox = {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            activity,
                                            Manifest.permission.READ_SMS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) viewModel.scanSmsInbox()
                                        else smsPermLauncher.launch(
                                            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                        )
                                    },
                                    onSyncGmail = { launchGmailSync() },
                                    onStartCancellation = { service ->
                                        viewModel.createOrOpenCancellationForService(service) { id ->
                                            navController.navigate(MandateRoutes.cancellationDetail(id))
                                        }
                                    },
                                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                                )
                            }

                            composable(
                                route = MandateRoutes.LOAN_DETAIL,
                                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
                            ) { backStack ->
                                val loanId = backStack.arguments?.getLong("loanId") ?: -1L
                                val loan = loans.find { it.id == loanId }
                                LoanDetailScreen(
                                    loan = loan,
                                    privacyBlur = privacyBlur,
                                    onBack = { navController.popBackStack() },
                                    onMarkEmiPaid = { viewModel.payLoanEmi(it) },
                                    onAskChanakya = { prompt ->
                                        navigateToTab(MandateRoutes.CHANAKYA)
                                        viewModel.askChanakyaText(prompt)
                                    }
                                )
                            }

                            composable(MandateRoutes.CHANAKYA) {
                                ChanakyaAiScreen(
                                    messages = chatMessages,
                                    uiState = chanakyaState,
                                    wallet = creditWallet,
                                    ttsEnabled = ttsEnabled,
                                    isListening = voiceController.isListening,
                                    onToggleTts = { enabled ->
                                        viewModel.toggleTtsVoiceReply(enabled)
                                        if (!enabled) voiceController.stopSpeaking()
                                    },
                                    onSendText = { viewModel.askChanakyaText(it) },
                                    onMicClick = {
                                        if (voiceController.isListening) voiceController.stopListening()
                                        else voiceController.startMicWithPermissionCheck()
                                    },
                                    onAttachUri = { uri -> viewModel.analyzeAttachmentUri(uri) },
                                    onResetChat = { viewModel.resetChanakyaChat() },
                                    onActionWithQuota = { action ->
                                        when (action.type) {
                                            AiActionType.SEND_EMAIL -> {
                                                viewModel.tryConsumeEmailDraftQuota {
                                                    UpiAutoPayIntents.launchEmailIntent(
                                                        context = activity,
                                                        toEmail = action.targetEmail,
                                                        subject = action.subject,
                                                        body = action.body
                                                    )
                                                }
                                            }
                                            AiActionType.OPEN_UPI_AUTOPAY -> {
                                                UpiAutoPayIntents.openUpiAppAutoPay(activity, UpiApp.GPAY)
                                            }
                                            AiActionType.EXPORT_PDF -> viewModel.exportAndSharePdf()
                                        }
                                    },
                                    onOpenTopUp = { viewModel.openTopUpSheet(null) }
                                )
                            }

                            composable(
                                route = MandateRoutes.CANCELLATION_DETAIL,
                                arguments = listOf(navArgument("cancellationId") { type = NavType.LongType })
                            ) { backStack ->
                                val cid = backStack.arguments?.getLong("cancellationId") ?: -1L
                                val item = cancellations.find { it.id == cid }
                                CancellationDetailScreen(
                                    cancellation = item,
                                    userName = userName,
                                    bankSupportEmail = bankSupportEmail,
                                    privacyBlur = privacyBlur,
                                    onBack = { navController.popBackStack() },
                                    onAdvanceStep = { req, step, status ->
                                        viewModel.advanceCancellationStep(req, step, status)
                                    },
                                    onConsumeEmailQuota = { onAllowed ->
                                        viewModel.tryConsumeEmailDraftQuota(onAllowed)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTopUpSheet) {
        TopUpCreditsBottomSheet(
            wallet = creditWallet,
            userName = userName,
            triggerReason = topUpReason,
            onLaunchCheckout = { amount ->
                RazorpayCheckoutHelper.launchTopUpCheckout(activity, amount, userName)
            },
            onSimulateSuccess = { amount ->
                viewModel.simulatePaymentSuccess(amount)
            },
            onDismiss = { viewModel.dismissTopUpSheet() }
        )
    }

    if (showProfileSheet) {
        ProfileSettingsSheet(
            userName = userName,
            bankEmail = bankSupportEmail,
            privacyBlur = privacyBlur,
            onSaveProfile = { n, e -> viewModel.updateUserProfile(n, e) },
            onTogglePrivacyBlur = { viewModel.togglePrivacyBlur() },
            onOpenSaveSettings = { navigateToTab(MandateRoutes.VAULT) },
            onDismiss = { showProfileSheet = false }
        )
    }

    if (showActiveShieldSheet) {
        val hasSmsPerm = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        ActiveShieldStatusSheet(
            activeTrialsCount = activeTrials.size,
            hasSmsPermission = hasSmsPerm,
            onRequestSmsOrScan = {
                if (hasSmsPerm) viewModel.scanSmsInbox()
                else smsPermLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
            },
            onDismiss = { showActiveShieldSheet = false }
        )
    }

    if (showCloudSetupGuide) {
        GoogleCloudSetupGuideSheet(
            onDismiss = { showCloudSetupGuide = false }
        )
    }

    overspendPopup?.let { alert ->
        OverspendAlertDialog(
            alert = alert,
            privacyBlur = privacyBlur,
            onDismiss = { viewModel.dismissOverspendPopup() },
            onAskChanakya = {
                viewModel.dismissOverspendPopup()
                navigateToTab(MandateRoutes.CHANAKYA)
                viewModel.askChanakyaText("${alert.categoryLabel} ka budget status aur tips batao")
            }
        )
    }
}

@Composable
private fun Mandate6TabBottomBar(
    currentRoute: String,
    onSelectTab: (String) -> Unit
) {
    Surface(
        color = CardWhite,
        shadowElevation = 10.dp,
        border = BorderStroke(0.8.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 2.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route ||
                    (tab == BottomNavTab.DEBTS && currentRoute.startsWith("loan_detail")) ||
                    (tab == BottomNavTab.HOME && (currentRoute.startsWith("cancellation_detail") || currentRoute == MandateRoutes.ACTIVE_SERVICES))

                if (tab == BottomNavTab.CHANAKYA) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTab(tab.route) }
                            .testTag("nav_tab_chanakya")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .shadow(
                                    elevation = if (selected) 6.dp else 2.dp,
                                    shape = CircleShape,
                                    ambientColor = RoyalBlue.copy(alpha = 0.45f)
                                )
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlueDeep)))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) RoyalBlue else CardWhite,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_chanakya_avatar),
                                contentDescription = "Chanakya AI Assistant",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (selected) RoyalBlue else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTab(tab.route) }
                            .testTag("nav_tab_${tab.name.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) RoyalBlueSoft else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = if (selected) RoyalBlue else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (selected) RoyalBlue else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
