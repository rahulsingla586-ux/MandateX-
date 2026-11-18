package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.MandateDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.MandateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val grouped = messages.groupBy { it.displayOriginatingAddress ?: "BankSMS" }
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = MandateDatabase.getInstance(context)
                val prefs = UserPreferencesRepository(context)
                val repo = MandateRepository(context, db.mandateDao(), prefs)

                grouped.forEach { (sender, parts) ->
                    val fullBody = parts.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
                    val ts = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    repo.ingestMessage(sender = sender, body = fullBody, source = "SMS", timestamp = ts)
                }
            } catch (e: Exception) {
                Log.e("SmsBroadcastReceiver", "Failed to process incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
