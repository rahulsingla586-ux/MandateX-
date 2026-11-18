package com.example.engine

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.MandateDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.MandateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val watchedPackages = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val appLabel = watchedPackages[sbn.packageName] ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val combined = listOf(title, bigText.ifBlank { text })
            .filter { it.isNotBlank() }
            .joinToString(" — ")

        if (combined.isBlank()) return

        serviceScope.launch {
            try {
                val db = MandateDatabase.getInstance(applicationContext)
                val dao = db.mandateDao()
                if (dao.countByRawMessage(combined.trim()) == 0) {
                    val repo = MandateRepository(
                        context = applicationContext,
                        dao = dao,
                        prefs = UserPreferencesRepository(applicationContext)
                    )
                    repo.ingestMessage(
                        sender = appLabel,
                        body = combined,
                        source = "NOTIFICATION",
                        timestamp = sbn.postTime
                    )
                }
            } catch (e: Exception) {
                Log.e("PaymentNotifListener", "Error parsing notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        fun isNotificationAccessEnabled(context: Context): Boolean {
            val cn = ComponentName(context, PaymentNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            return flat.split(":").any {
                ComponentName.unflattenFromString(it) == cn
            }
        }
    }
}
