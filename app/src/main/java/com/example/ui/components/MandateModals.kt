package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.preferences.CreditWalletState
import com.example.engine.PaymentNotificationListenerService
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
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
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.RazorpayCheckoutHelper
import com.example.util.UpiApp
import com.example.util.UpiAutoPayIntents
import java.util.Locale

class VoiceTtsController(
    val isListening: Boolean,
    val startMicWithPermissionCheck: () -> Unit,
    val stopListening: () -> Unit,
    val speakReply: (String) -> Unit,
    val stopSpeaking: () -> Unit
)

@Composable
fun rememberVoiceTtsController(
    onSpokenResult: (String) -> Unit
): VoiceTtsController {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val indianLocale = Locale("en", "IN")
                val res = instance?.setLanguage(indianLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    instance?.setLanguage(Locale.ENGLISH)
                }
            }
        }
        instance
    }

    val recognizerIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) onSpokenResult(spoken)
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer, tts) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(context, "Voice capture paused ($error). Tap Mic again or type.", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) onSpokenResult(spoken)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            speechRecognizer?.destroy()
            tts.stop()
            tts.shutdown()
        }
    }

    fun startVoiceCapture() {
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer.startListening(intent)
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Chanakya AI in Hinglish or English")
            }
            runCatching { recognizerIntentLauncher.launch(intent) }.onFailure {
                Toast.makeText(context, "Speech service unavailable on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceCapture()
        else Toast.makeText(context, "Microphone permission required for voice AI.", Toast.LENGTH_SHORT).show()
    }

    return VoiceTtsController(
        isListening = isListening,
        startMicWithPermissionCheck = {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasMic) startVoiceCapture()
            else audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        stopListening = {
            speechRecognizer?.stopListening()
            isListening = false
        },
        speakReply = { text ->
            val clean = text.replace("✦", "").replace("₹", "Rupees ").take(420)
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "chanakya_reply")
        },
        stopSpeaking = {
            tts.stop()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpCreditsBottomSheet(
    wallet: CreditWalletState,
    userName: String,
    triggerReason: String?,
    onLaunchCheckout: (Int) -> Unit,
    onSimulateSuccess: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("20") }
    val presets = listOf(5, 10, 20, 50)
    val parsedAmount = amountText.toIntOrNull() ?: 0
    val isValidAmount = parsedAmount >= 5

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MandateX Credits Wallet",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pay-As-You-Go • ₹1 = 1 Credit • Min ₹5",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    color = GoldWalletBg,
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.2.dp, GoldWalletBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${wallet.creditBalance} Credits",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = GoldTextDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!triggerReason.isNullOrBlank()) {
                Surface(
                    color = AlertRedContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = triggerReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Strict Free Tier Remaining Strip
            Surface(
                color = SurfaceMuted,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "FREE TIER ALLOWANCE REMAINING",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Voice Mic: ${wallet.freeVoiceRemaining}/5  •  Text Chat: ${wallet.freeTextRemaining}/15",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "File/Image Upload: ${wallet.freeUploadRemaining}/1  •  Bank/RBI Email: ${wallet.freeEmailRemaining}/1",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Post-Free Rates: Voice Mic = 2 Credits | + File Scan = 2 Credits | CA PDF Export = 2 Credits | Text Chat = 1 Credit | Email Draft = 1 Credit",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Quick Top-Up Chips: ₹5, ₹10, ₹20, ₹50
            Text(
                text = "Quick Top-Up Chips",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = parsedAmount == preset,
                        onClick = { amountText = preset.toString() },
                        label = { Text("₹$preset", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldWalletBg,
                            selectedLabelColor = GoldTextDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("topup_chip_$preset")
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { text -> amountText = text.filter { it.isDigit() }.take(4) },
                label = { Text("Custom Top-Up Amount (Min ₹5)") },
                prefix = { Text("₹ ") },
                supportingText = {
                    if (!isValidAmount) {
                        Text("Minimum top-up is ₹5 (5 Credits)", color = AlertRed)
                    } else {
                        Text("You get $parsedAmount Credits instantly", color = EmeraldGreen)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("topup_custom_amount_input")
            )

            Button(
                onClick = { onLaunchCheckout(maxOf(5, parsedAmount)) },
                enabled = isValidAmount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("razorpay_checkout_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Pay ₹${maxOf(5, parsedAmount)} via Razorpay / UPI")
            }

            OutlinedButton(
                onClick = { onSimulateSuccess(maxOf(5, parsedAmount)) },
                enabled = isValidAmount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("simulate_topup_success_button"),
                border = BorderStroke(1.2.dp, EmeraldGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate Payment Success (+${maxOf(5, parsedAmount)} Credits)")
            }
        }
    }
}

@Composable
fun AddManualTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, TransactionType, SpendCategory, String) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(SpendCategory.FOOD_DINING) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = merchant.isNotBlank() && parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Add Financial Entry", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Biller") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { last4 = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Last 4") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TransactionType.entries) { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SpendCategory.entries) { c ->
                        FilterChip(
                            selected = selectedCategory == c,
                            onClick = { selectedCategory = c },
                            label = { Text(c.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(merchant.trim(), parsedAmount, selectedType, selectedCategory, last4.trim())
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White)
            ) { Text("Save Entry") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsSheet(
    userName: String,
    bankEmail: String,
    privacyBlur: Boolean,
    onSaveProfile: (String, String) -> Unit,
    onTogglePrivacyBlur: () -> Unit,
    onOpenSaveSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nameInput by remember(userName) { mutableStateOf(userName) }
    var emailInput by remember(bankEmail) { mutableStateOf(bankEmail) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Profile & Bank Nodal Settings", style = MaterialTheme.typography.titleLarge, color = RoyalBlue, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Bank Nodal Support Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Privacy Blur (₹****)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Switch(
                    checked = privacyBlur,
                    onCheckedChange = { onTogglePrivacyBlur() },
                    colors = SwitchDefaults.colors(checkedTrackColor = EmeraldGreen)
                )
            }
            Button(
                onClick = {
                    onSaveProfile(nameInput, emailInput)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Profile")
            }
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenSaveSettings()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Open Security, PDF Export & Budgets →", color = RoyalBlue)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveShieldStatusSheet(
    activeTrialsCount: Int,
    hasSmsPermission: Boolean,
    onRequestSmsOrScan: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notifListenerActive = remember {
        PaymentNotificationListenerService.isNotificationAccessEnabled(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MandateX Active Shield", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Text(
                text = if (activeTrialsCount == 0) {
                    "All clear! 0 unwanted intro trials detected. WorkManager & SMS/UPI monitors are guarding your account."
                } else {
                    "$activeTrialsCount trial trap(s) detected with 24–48h pre-renewal alarms armed."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Surface(
                color = EmeraldSoft,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, EmeraldContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Sms, contentDescription = null, tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (hasSmsPermission) "Bank SMS Inbox Access Active" else "Bank SMS Permission Needed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    TextButton(onClick = {
                        onRequestSmsOrScan()
                        onDismiss()
                    }) {
                        Text(if (hasSmsPermission) "Scan" else "Grant")
                    }
                }
            }

            Surface(
                color = RoyalBlueSoft,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = RoyalBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (notifListenerActive) "GPay / PhonePe / Paytm Listener ON" else "Enable UPI Notification Listener",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                        Text(if (notifListenerActive) "Active" else "Enable")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    tx: TransactionEntity,
    privacyBlur: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onStartCancellation: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tx.merchantName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SourceBadgePill(tx.source)
            }

            Text(
                text = CurrencyFormatter.formatInr(tx.amount, privacyBlur),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = RoyalBlue
            )

            Surface(
                color = SurfaceMuted,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val paymentSourceText = tx.paymentSource.ifBlank { "${tx.bankName} XX${tx.accountLast4}" }
                    Text(
                        text = "Payment Source: $paymentSourceText",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Type & Category: ${tx.type.label} • ${tx.category.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (!tx.upiId.isNullOrBlank()) {
                        Text(
                            text = "Merchant UPI ID: ${tx.upiId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoyalBlue
                        )
                    }
                    if (!tx.mandateRefId.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mandate Ref / UMRN: ${tx.mandateRefId}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { copyToClipboard(context, tx.mandateRefId, "Mandate Ref ID") },
                                modifier = Modifier.testTag("copy_mandate_ref_button")
                            ) {
                                Text("Copy ID", color = RoyalBlue)
                            }
                        }
                    }
                    if (!tx.nextDueDateLabel.isNullOrBlank()) {
                        Text(
                            text = "Next Due / Renewal: ${tx.nextDueDateLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (tx.isTrial && !tx.isCancelled) AlertRed else EmeraldGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Captured: ${CurrencyFormatter.formatTimestamp(tx.timestamp)} (${tx.source})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            if (tx.rawMessage.isNotBlank()) {
                Surface(
                    color = RoyalBlueSoft,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tx.rawMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (onStartCancellation != null && !tx.isCancelled) {
                Text(
                    text = "1-Tap Cancellation & AutoPay Revocation",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UpiApp.entries.forEach { app ->
                        OutlinedButton(
                            onClick = { UpiAutoPayIntents.openUpiAppAutoPay(context, app) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = app.displayName.replace("Google Pay", "GPay"),
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBlue,
                                maxLines = 1
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        onDismiss()
                        onStartCancellation()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("service_detail_cancel_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop AutoPay & Open Bank/RBI Escalation")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        copyToClipboard(
                            context,
                            "${tx.merchantName} — ₹${tx.amount.toInt()} (${tx.paymentSource.ifBlank { "${tx.bankName} XX${tx.accountLast4}" }}) ${tx.mandateRefId?.let { "| Ref: $it" }.orEmpty()}\n${tx.rawMessage}",
                            "Transaction Receipt"
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
                OutlinedButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleCloudSetupGuideSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val projectId = "gen-lang-client-0652726471"
    val packageName = "com.aistudio.mandatex.xkpqzr"
    val scopeUri = "https://www.googleapis.com/auth/gmail.readonly"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Cloud, contentDescription = null, tint = RoyalBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enable Gmail API in Google Cloud Console",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To run live read-only Gmail Auto-Sync on your device, enable the Gmail API & Android OAuth Client in Google Cloud Console:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            item {
                Surface(
                    color = RoyalBlueSoft,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Copyable Project Identifiers", style = MaterialTheme.typography.labelSmall, color = RoyalBlue, fontWeight = FontWeight.Bold)
                        Text("Firebase / GCP Project: $projectId", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("Android Package: $packageName", style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily), color = TextPrimary)
                        Text("Gmail Scope: $scopeUri", style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily), color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { copyToClipboard(context, packageName, "Package Name") },
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) { Text("Copy Package") }
                            OutlinedButton(
                                onClick = { copyToClipboard(context, scopeUri, "Gmail Scope") },
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) { Text("Copy Scope") }
                        }
                    }
                }
            }

            val steps = listOf(
                "1. Open Google Cloud Console -> APIs & Services -> Library -> select project '$projectId'.",
                "2. Search for 'Gmail API' and click Enable.",
                "3. Go to Google Auth Platform / OAuth Consent Screen -> choose External -> add scope '$scopeUri' and 'userinfo.email'.",
                "4. Under Audience / Test Users, add your Gmail address (required while app publishing status is Testing).",
                "5. Go to Clients / Credentials -> Create OAuth Client ID -> Application type: Android -> Package name '$packageName' -> paste your debug/release SHA-1 fingerprint.",
                "6. Return to MandateX and tap 'Connect Gmail for Online Auto-Sync' to grant read-only permission and audit the last 6 months of invoices & mandates via Chanakya AI."
            )

            items(steps) { step ->
                Surface(
                    color = SurfaceMuted,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Got It")
                }
            }
        }
    }
}
