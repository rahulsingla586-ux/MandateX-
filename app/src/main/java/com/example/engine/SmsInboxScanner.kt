package com.example.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.MandateDao
import com.example.data.model.TransactionType
import com.example.data.repository.MandateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsScanResult(
    val scannedMessages: Int,
    val importedCount: Int,
    val mandatesFound: Int
)

object SmsInboxScanner {

    private const val SIX_MONTHS_MS = 180L * 86_400_000L

    suspend fun scanRecentSmsInbox(
        context: Context,
        dao: MandateDao,
        repository: MandateRepository,
        limit: Int = 1200
    ): SmsScanResult = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext SmsScanResult(0, 0, 0)

        val sixMonthsAgo = System.currentTimeMillis() - SIX_MONTHS_MS
        var scanned = 0
        var importedCount = 0
        var mandatesFound = 0

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(sixMonthsAgo.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use { c ->
                val addrIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

                while (c.moveToNext() && scanned < limit) {
                    scanned++
                    val sender = if (addrIdx >= 0) c.getString(addrIdx).orEmpty() else "BankSMS"
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx).orEmpty() else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                    if (body.isNotBlank() && dao.countByRawMessage(body.trim()) == 0) {
                        val inserted = repository.ingestMessage(
                            sender = sender,
                            body = body,
                            source = "SMS_6M",
                            timestamp = date
                        )
                        if (inserted != null) {
                            importedCount++
                            if (inserted.type == TransactionType.AUTOPAY || inserted.isTrial) {
                                mandatesFound++
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsInboxScanner", "Failed to scan 6-month SMS inbox", e)
        }

        SmsScanResult(
            scannedMessages = scanned,
            importedCount = importedCount,
            mandatesFound = mandatesFound
        )
    }
}
