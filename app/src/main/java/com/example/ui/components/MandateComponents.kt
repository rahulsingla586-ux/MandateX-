package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.ChanakyaUiState
import com.example.ai.ChatMessage
import com.example.data.model.CancellationRequestEntity
import com.example.data.model.DuplicateChargeAlert
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.preferences.CreditWalletState
import com.example.ui.GmailSyncState
import com.example.ui.OverspendPopupAlert
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.AlertRedDeep
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldTextDark
import com.example.ui.theme.GoldWalletBg
import com.example.ui.theme.GoldWalletBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter

@Composable
fun MandateTopBar(
    userName: String,
    activeTrialsCount: Int,
    privacyBlur: Boolean,
    wallet: CreditWalletState,
    onProfileClick: () -> Unit,
    onActiveShieldClick: () -> Unit,
    onTogglePrivacyBlur: () -> Unit,
    onCreditsClick: () -> Unit
) {
    Surface(
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: User Greeting + Profile Click
            Surface(
                onClick = onProfileClick,
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                modifier = Modifier.testTag("profile_greeting_button")
            ) {
                Row(
                    modifier = Modifier.padding(end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mandatex_icon),
                        contentDescription = "MandateX Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .border(1.5.dp, RoyalBlue.copy(alpha = 0.2f), RoundedCornerShape(11.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (userName.isNotBlank()) "Hi, $userName" else "Welcome",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = "Profile & Settings",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "MandateX",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right: ACTIVE SHIELD pill + Eye Blur Toggle + Golden Credits Wallet Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onActiveShieldClick,
                    shape = RoundedCornerShape(50.dp),
                    color = if (activeTrialsCount > 0) AlertRedContainer else EmeraldSoft,
                    border = BorderStroke(
                        1.dp,
                        if (activeTrialsCount > 0) AlertRed.copy(alpha = 0.45f) else EmeraldGreen.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("active_shield_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (activeTrialsCount > 0) AlertRed else EmeraldGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "ACTIVE SHIELD",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeTrialsCount > 0) AlertRed else EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePrivacyBlur,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (privacyBlur) RoyalBlueSoft else SurfaceMuted)
                        .testTag("privacy_blur_toggle")
                ) {
                    Icon(
                        imageVector = if (privacyBlur) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (privacyBlur) "Show rupee amounts" else "Hide rupee amounts",
                        tint = if (privacyBlur) RoyalBlue else TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Golden Credits Wallet Pill
                Surface(
                    onClick = onCreditsClick,
                    shape = RoundedCornerShape(50.dp),
                    color = GoldWalletBg,
                    border = BorderStroke(1.2.dp, GoldWalletBorder),
                    modifier = Modifier.testTag("credits_wallet_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Credits Wallet",
                            tint = GoldAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${wallet.creditBalance}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GoldTextDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoyalBlueHeroCard(
    moneySaved: Double,
    activeTrialsCount: Int,
    privacyBlur: Boolean,
    onCardClick: () -> Unit,
    onScanSmsClick: () -> Unit,
    onAddManualEntryClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = RoyalBlue.copy(alpha = 0.28f),
                spotColor = RoyalBlueDeep.copy(alpha = 0.32f)
            )
            .testTag("royal_blue_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalBlue)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(RoyalBlueBright, RoyalBlue, RoyalBlueDeep)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AUTOPAY SAFETY & SAVINGS METER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDBEAFE),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = if (activeTrialsCount == 0) EmeraldGreen else AlertRed,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = if (activeTrialsCount == 0) "0 Trials Active" else "$activeTrialsCount Trial Alert",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${CurrencyFormatter.formatInr(moneySaved, privacyBlur)} Saved This Year",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (activeTrialsCount == 0) {
                        "0 Unwanted Trials Active • Real-time SMS & UPI Guard"
                    } else {
                        "$activeTrialsCount Active Trial Trap detected — tap Exit Now before price hike!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE0E7FF)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onScanSmsClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_scan_sms_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = RoyalBlue
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scan SMS & Refresh",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onAddManualEntryClick,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("hero_add_entry_button"),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Entry",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GmailAutoSyncCard(
    connectedEmail: String?,
    lastGmailSyncTs: Long?,
    lastSmsScanTs: Long?,
    gmailSyncState: GmailSyncState,
    onConnectOrSyncClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSetupGuideClick: () -> Unit
) {
    val isBusy = gmailSyncState is GmailSyncState.Authorizing ||
        gmailSyncState is GmailSyncState.FetchingAndParsing
    val isConnected = !connectedEmail.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = RoyalBlue.copy(alpha = 0.08f)
            )
            .testTag("gmail_auto_sync_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(
            1.dp,
            if (isConnected) EmeraldContainer else BorderLight
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isConnected) EmeraldSoft else RoyalBlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Filled.CloudDone else Icons.Filled.MailOutline,
                            contentDescription = "Online Gmail Auto-Sync",
                            tint = if (isConnected) EmeraldGreen else RoyalBlue,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Online Gmail Auto-Sync",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isConnected) EmeraldContainer else RoyalBlueContainer,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = if (isConnected) "CONNECTED" else "6-MO SCAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isConnected) EmeraldGreen else RoyalBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isConnected) {
                                val syncLabel = lastGmailSyncTs?.let { CurrencyFormatter.formatTimestamp(it) } ?: "Ready"
                                "$connectedEmail • Synced $syncLabel"
                            } else {
                                "Detect hidden trials, OTT invoices & e-Mandates via Gmail + Chanakya AI"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onSetupGuideClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("gmail_setup_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.HelpOutline,
                        contentDescription = "Google Cloud Gmail API Setup Guide",
                        tint = RoyalBlue,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            if (lastSmsScanTs != null) {
                Text(
                    text = "6-Month SMS Inbox last scanned: ${CurrencyFormatter.formatTimestamp(lastSmsScanTs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            when (gmailSyncState) {
                is GmailSyncState.Authorizing -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoyalBlueSoft)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RoyalBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Authorizing read-only Gmail access...",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoyalBlue
                        )
                    }
                }
                is GmailSyncState.FetchingAndParsing -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoyalBlueSoft)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RoyalBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = gmailSyncState.progressLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = RoyalBlue
                        )
                    }
                }
                is GmailSyncState.Error -> {
                    Surface(
                        color = AlertRedContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = gmailSyncState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRed
                            )
                            TextButton(
                                onClick = onSetupGuideClick,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Open Google Cloud Console Setup Guide →", color = RoyalBlue)
                            }
                        }
                    }
                }
                GmailSyncState.Idle -> Unit
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConnectOrSyncClick,
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("connect_gmail_sync_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) EmeraldGreen else RoyalBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Sync else Icons.Filled.MailOutline,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = if (isConnected) "Sync Gmail Now (6 Mo)" else "Connect Gmail for Online Auto-Sync",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        enabled = !isBusy,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("gmail_disconnect_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LinkOff,
                            contentDescription = "Disconnect Gmail",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SourceBadgePill(source: String) {
    val upper = source.uppercase()
    val (label, bg, fg) = when {
        upper.contains("SMS") && upper.contains("GMAIL") ->
            Triple("SMS + GMAIL", EmeraldContainer, EmeraldGreen)
        upper.contains("GMAIL") ->
            Triple("GMAIL", RoyalBlueContainer, RoyalBlue)
        upper == "SMS_6M" ->
            Triple("SMS 6M", SurfaceMuted, TextSecondary)
        else ->
            Triple(upper.take(9), SurfaceMuted, TextSecondary)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskChanakyaInlineCard(
    wallet: CreditWalletState,
    latestReply: ChatMessage?,
    uiState: ChanakyaUiState,
    isListening: Boolean,
    onSuggestionClick: (String) -> Unit,
    onSendText: (String) -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit,
    onOpenFullAssistant: () -> Unit
) {
    val context = LocalContext.current
    var inputQuery by remember { mutableStateOf("") }

    val chips = remember {
        listOf(
            "✦ Mera total kharcha kitna hai?",
            "✦ Kaunsa trial khatam hone wala hai?",
            "✦ Meri Loan EMI aur Card bill dikhao",
            "✦ Double charge check karo"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = RoyalBlue.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFullAssistant),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_chanakya_avatar),
                        contentDescription = "Chanakya AI",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, RoyalBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ask Chanakya AI",
                                style = MaterialTheme.typography.titleMedium,
                                color = RoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Free: ${wallet.freeTextRemaining} Chat • ${wallet.freeVoiceRemaining} Mic • ${wallet.freeUploadRemaining} Doc",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                TextButton(
                    onClick = onOpenFullAssistant,
                    modifier = Modifier.testTag("open_full_chanakya_link")
                ) {
                    Text("Full Chat", color = RoyalBlue)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Chanakya AI Tab",
                        tint = RoyalBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // 4 Clickable Hinglish Suggestion Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEachIndexed { index, chip ->
                    Surface(
                        onClick = { onSuggestionClick(chip.removePrefix("✦ ").trim()) },
                        shape = RoundedCornerShape(12.dp),
                        color = RoyalBlueSoft,
                        border = BorderStroke(1.dp, RoyalBlueContainer),
                        modifier = Modifier.testTag("inline_chip_$index")
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalBlueDeep,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Input Bar: + Attachment Icon + Ask Anything + Send / Royal Blue Mic
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onAttachClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceMuted)
                        .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                        .testTag("inline_attach_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Upload Bank PDF or Payment Screenshot",
                        tint = RoyalBlue
                    )
                }

                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { Text("Ask Anything in Hinglish...", maxLines = 1) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = SurfaceMuted.copy(alpha = 0.45f),
                        unfocusedContainerColor = SurfaceMuted.copy(alpha = 0.45f)
                    ),
                    trailingIcon = {
                        if (inputQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendText(inputQuery)
                                    inputQuery = ""
                                },
                                modifier = Modifier.testTag("inline_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Question",
                                    tint = RoyalBlue
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inline_ask_input")
                )

                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isListening) AlertRed else RoyalBlue)
                        .testTag("inline_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Speak to Chanakya AI",
                        tint = Color.White
                    )
                }
            }

            if (uiState is ChanakyaUiState.Thinking) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoyalBlueSoft)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = RoyalBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Chanakya AI is analyzing your ledger...",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoyalBlue
                    )
                }
            } else if (latestReply != null && !latestReply.isUser && latestReply.id != "welcome") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (latestReply.isError) AlertRedContainer else EmeraldSoft,
                    border = BorderStroke(
                        1.dp,
                        if (latestReply.isError) AlertRed.copy(alpha = 0.4f) else EmeraldContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chanakya AI Reply",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (latestReply.isError) AlertRed else EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    copyToClipboard(context, latestReply.text, "Chanakya AI Reply")
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("inline_copy_reply_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy AI Reply",
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = latestReply.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CancellationTrackerCard(
    item: CancellationRequestEntity,
    privacyBlur: Boolean,
    onStatusDetailsClick: () -> Unit
) {
    val isCompleted = item.status == "COMPLETED"
    val badgeBg = if (isCompleted) EmeraldContainer else RoyalBlueContainer
    val badgeFg = if (isCompleted) EmeraldGreen else RoyalBlue

    Card(
        onClick = onStatusDetailsClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cancellation_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${item.merchantName} Cancellation",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.subtitle} • XX${item.accountLast4}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Protected: ${CurrencyFormatter.formatInr(item.amount, privacyBlur)}/mo • Step ${item.currentStep}/4",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    color = if (isCompleted) EmeraldGreen else TextSecondary
                )

                OutlinedButton(
                    onClick = onStatusDetailsClick,
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("cancellation_details_btn_${item.id}"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = "✦ Status & Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StickyTrialWarningBanner(
    trial: TransactionEntity,
    privacyBlur: Boolean,
    onExitNowClick: () -> Unit
) {
    val countdown = trial.trialExpiryTimestamp?.let {
        CurrencyFormatter.formatCountdown(it)
    } ?: "2 days left"
    val hikePrice = CurrencyFormatter.formatInr(trial.postTrialAmount ?: 499.0, privacyBlur)

    Surface(
        onClick = onExitNowClick,
        color = AlertRed,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sticky_trial_exit_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(AlertRedDeep, AlertRed)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Trial Price Hike Warning",
                    tint = Color(0xFFFEF08A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${trial.merchantName} ($countdown) — Price jumps to $hikePrice!",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Exit Now",
                    style = MaterialTheme.typography.labelMedium,
                    color = AlertRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun TrialExitShieldCard(
    trial: TransactionEntity,
    privacyBlur: Boolean,
    onExitAndCancel: () -> Unit,
    onTestNotification: () -> Unit
) {
    val postPrice = trial.postTrialAmount ?: 499.0
    val countdown = trial.trialExpiryTimestamp?.let {
        CurrencyFormatter.formatCountdown(it)
    } ?: "Expires soon"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
        border = BorderStroke(1.5.dp, AlertRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AlertRed.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Trial Price Hike Warning",
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SMART TRIAL EXIT SHIELD",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertRed,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = trial.merchantName,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onTestNotification,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Send Test Trial Alert Notification",
                        tint = RoyalBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Paid ${CurrencyFormatter.formatInr(trial.amount, privacyBlur)} trial on XX${trial.accountLast4}. Price hikes to ${CurrencyFormatter.formatInr(postPrice, privacyBlur)}/mo ($countdown).",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onExitAndCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("trial_exit_cancel_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Exit & Cancel Now (Save ${CurrencyFormatter.formatInr(postPrice - trial.amount, privacyBlur)})",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun DuplicateChargeAlertCard(
    alert: DuplicateChargeAlert,
    privacyBlur: Boolean,
    onDraftRefund: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.5.dp, GoldAmber.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AlertRedContainer,
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "Duplicate Charge Detected!",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlertRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "+${CurrencyFormatter.formatInr(alert.totalOvercharged, privacyBlur)} extra",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GoldAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${alert.merchantName} (${alert.charges.size}x in 30 days)",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.reason,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDraftRefund,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("duplicate_refund_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Draft 1-Tap Refund Email",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: TransactionEntity,
    privacyBlur: Boolean,
    isDuplicateHighlighted: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val accentColor by animateColorAsState(
        targetValue = when {
            tx.isTrial && !tx.isCancelled -> AlertRed
            isDuplicateHighlighted -> GoldAmber
            tx.type == TransactionType.CARD_EMI -> RoyalBlue
            tx.type == TransactionType.AUTOPAY -> EmeraldGreen
            else -> RoyalBlue
        },
        label = "txAccent"
    )

    val iconVector = when {
        tx.type == TransactionType.CARD_EMI -> Icons.Filled.CreditCard
        tx.type == TransactionType.AUTOPAY -> Icons.Filled.Autorenew
        tx.category == SpendCategory.FOOD_DINING -> Icons.Filled.Restaurant
        tx.category == SpendCategory.SHOPPING -> Icons.Filled.ShoppingBag
        tx.category == SpendCategory.OTT_APPS -> Icons.Filled.Subscriptions
        else -> Icons.Filled.AccountBalance
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDuplicateHighlighted) GoldAmber.copy(alpha = 0.7f) else BorderLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = tx.category.label,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SourceBadgePill(tx.source)
                    if (isDuplicateHighlighted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = GoldWalletBg, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "DUP",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldTextDark,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (tx.isTrial && !tx.isCancelled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(color = AlertRedContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "TRIAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertRed,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                val metaLine = buildString {
                    if (tx.paymentSource.isNotBlank()) {
                        append(tx.paymentSource)
                    } else {
                        if (!tx.upiId.isNullOrBlank()) append("${tx.upiId} • ")
                        append("${tx.bankName} XX${tx.accountLast4}")
                    }
                    if (!tx.mandateRefId.isNullOrBlank()) {
                        append(" • ${tx.mandateRefId}")
                    }
                }
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val duePart = tx.nextDueDateLabel?.let { " • Due: $it" }.orEmpty()
                Text(
                    text = "${tx.category.label}$duePart • ${CurrencyFormatter.formatTimestamp(tx.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatInr(tx.amount, privacyBlur),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (tx.isCancelled) TextMuted else TextPrimary
                )
                if (tx.isCancelled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "SAVED",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete transaction",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverspendAlertDialog(
    alert: OverspendPopupAlert,
    privacyBlur: Boolean,
    onDismiss: () -> Unit,
    onAskChanakya: () -> Unit
) {
    val isExceeded = alert.percent >= 100
    val accent = if (isExceeded) AlertRed else GoldAmber

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExceeded) "Auto-Freeze Budget Alert!" else "Chanakya Spend Warning",
                    color = accent,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Text(
                text = "Ruk jaao! Is mahine ${alert.categoryLabel} ka ${CurrencyFormatter.formatInr(alert.spent, privacyBlur)} / ${CurrencyFormatter.formatInr(alert.limit, privacyBlur)} budget (${alert.percent}%) cross ho chuka hai.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        },
        confirmButton = {
            Button(
                onClick = onAskChanakya,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
            ) {
                Text("Ask Chanakya AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", color = TextSecondary)
            }
        }
    )
}

fun copyToClipboard(context: Context, text: String, label: String = "MandateX") {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
}
