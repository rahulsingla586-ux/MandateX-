package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiActionButton
import com.example.ai.AiActionType
import com.example.ai.ChanakyaUiState
import com.example.ai.ChatMessage
import com.example.data.preferences.CreditWalletState
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardWhite
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldTextDark
import com.example.ui.theme.GoldWalletBg
import com.example.ui.theme.GoldWalletBorder
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.UpiApp
import com.example.util.UpiAutoPayIntents

@Composable
fun ChanakyaAiScreen(
    messages: List<ChatMessage>,
    uiState: ChanakyaUiState,
    wallet: CreditWalletState,
    ttsEnabled: Boolean,
    isListening: Boolean,
    onToggleTts: (Boolean) -> Unit,
    onSendText: (String) -> Unit,
    onMicClick: () -> Unit,
    onAttachUri: (Uri) -> Unit,
    onResetChat: () -> Unit,
    onActionWithQuota: (AiActionButton) -> Unit,
    onOpenTopUp: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    val quickPrompts = remember {
        listOf(
            "Roast my expenses",
            "780+ CIBIL Roadmap",
            "Debt Avalanche Plan",
            "Expiring Trials",
            "Draft mandate cancellation email"
        )
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAttachUri(uri)
    }

    LaunchedEffect(messages.size, uiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Header + TTS Toggle + Reset + Credits Quota Strip
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = CardWhite,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_chanakya_avatar),
                            contentDescription = "Chanakya AI",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(2.dp, RoyalBlue, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Chanakya AI", style = MaterialTheme.typography.titleMedium, color = RoyalBlue, fontWeight = FontWeight.Bold)
                            Text("Voice + Multimodal PDF/Image Munim", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onToggleTts(!ttsEnabled) },
                            modifier = Modifier.testTag("chanakya_tts_toggle")
                        ) {
                            Icon(
                                imageVector = if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (ttsEnabled) "Mute Hinglish voice reply" else "Enable Hinglish voice reply",
                                tint = if (ttsEnabled) RoyalBlue else TextSecondary
                            )
                        }
                        IconButton(
                            onClick = onResetChat,
                            modifier = Modifier.testTag("chanakya_reset_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "New Chat Session", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    onClick = onOpenTopUp,
                    shape = RoundedCornerShape(10.dp),
                    color = GoldWalletBg,
                    border = BorderStroke(1.dp, GoldWalletBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Free: ${wallet.freeVoiceRemaining} Mic • ${wallet.freeTextRemaining} Chat • ${wallet.freeUploadRemaining} Upload",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldTextDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${wallet.creditBalance} Credits +",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPrompts) { prompt ->
                Surface(
                    onClick = { onSendText(prompt) },
                    shape = RoundedCornerShape(50.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "✦ $prompt",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                FullChatBubble(
                    message = msg,
                    onCopy = { copyToClipboard(context, msg.text, "Chanakya AI Reply") },
                    onActionClick = onActionWithQuota
                )
            }

            if (uiState is ChanakyaUiState.Thinking) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RoyalBlue, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Chanakya AI is reading your ledger & documents...", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Input Bar: + Upload + Ask Anything + Mic + Send
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { documentPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceMuted)
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                    .testTag("chanakya_attach_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Attach Bank PDF or Payment Screenshot", tint = RoyalBlue)
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask Anything in Hinglish...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalBlue,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chanakya_input_field")
            )

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListening) AlertRed else RoyalBlue)
                    .testTag("chanakya_mic_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isListening) "Stop voice input" else "Speak to Chanakya AI",
                    tint = Color.White
                )
            }

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendText(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && uiState !is ChanakyaUiState.Thinking,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("chanakya_send_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun FullChatBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onActionClick: (AiActionButton) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            color = when {
                message.isError -> AlertRedContainer
                message.isUser -> RoyalBlueSoft
                else -> CardWhite
            },
            border = BorderStroke(
                1.dp,
                when {
                    message.isError -> AlertRed
                    message.isUser -> RoyalBlueContainer
                    else -> BorderLight
                }
            ),
            shadowElevation = if (message.isUser) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!message.attachmentName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(message.attachmentName, style = MaterialTheme.typography.labelSmall, color = RoyalBlue, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isError) AlertRed else TextPrimary
                )

                if (!message.isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            onClick = onCopy,
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceMuted,
                            modifier = Modifier.testTag("copy_bubble_${message.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy response", tint = RoyalBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall, color = RoyalBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (message.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        message.actions.forEach { action ->
                            OutlinedButton(
                                onClick = { onActionClick(action) },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, EmeraldGreen),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(action.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
