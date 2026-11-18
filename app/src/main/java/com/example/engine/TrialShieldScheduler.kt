package com.example.engine

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.model.TransactionEntity
import java.util.concurrent.TimeUnit

object TrialShieldScheduler {

    const val CHANNEL_TRIAL_SHIELD = "trial_shield_alerts"
    const val CHANNEL_BUDGET_ALERT = "budget_overspend_alerts"
    private const val EXTRA_MERCHANT = "extra_merchant"
    private const val EXTRA_TRIAL_PRICE = "extra_trial_price"
    private const val EXTRA_POST_PRICE = "extra_post_price"
    private const val EXTRA_TX_ID = "extra_tx_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val trialChannel = NotificationChannel(
                CHANNEL_TRIAL_SHIELD,
                "Trial Exit Shield Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts 24-48h before low-cost trials auto-renew at full price"
                enableVibration(true)
            }
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ALERT,
                "Budget Overspend Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when category spend crosses 80% or 100% limit"
                enableVibration(true)
            }
            nm.createNotificationChannel(trialChannel)
            nm.createNotificationChannel(budgetChannel)
        }
    }

    fun scheduleTrialExitShield(context: Context, tx: TransactionEntity) {
        if (!tx.isTrial || tx.isCancelled) return
        ensureChannels(context)

        val now = System.currentTimeMillis()
        val expiry = tx.trialExpiryTimestamp ?: (now + 48 * 3600_000L)
        // Schedule alert 24 hours before expiry (minimum 15 seconds delay if trial expires sooner)
        val leadTimeMillis = 24 * 3600_000L
        val triggerAtMillis = maxOf(now + 15_000L, expiry - leadTimeMillis)
        val delayMillis = maxOf(15_000L, triggerAtMillis - now)

        val postPrice = tx.postTrialAmount ?: 499.0

        // 1. WorkManager reliable background alert
        val workRequest = OneTimeWorkRequestBuilder<TrialReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    EXTRA_TX_ID to tx.id,
                    EXTRA_MERCHANT to tx.merchantName,
                    EXTRA_TRIAL_PRICE to tx.amount,
                    EXTRA_POST_PRICE to postPrice
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "trial_shield_${tx.id}_${tx.merchantName}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // 2. AlarmManager secondary alert
        val alarmIntent = Intent(context, TrialAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TX_ID, tx.id)
            putExtra(EXTRA_MERCHANT, tx.merchantName)
            putExtra(EXTRA_TRIAL_PRICE, tx.amount)
            putExtra(EXTRA_POST_PRICE, postPrice)
        }
        val requestCode = (tx.id.toInt().takeIf { it != 0 } ?: tx.merchantName.hashCode())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun showTrialExitNotification(
        context: Context,
        txId: Long,
        merchant: String,
        trialPrice: Double,
        postPrice: Double
    ) {
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            txId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TRIAL_SHIELD)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Trial Exit Shield: $merchant")
            .setContentText(
                "₹${trialPrice.toInt()} trial ends soon! AutoPay will charge ₹${postPrice.toInt()}. Tap to Exit & Cancel Now."
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Ruk jaao! $merchant ka ₹${trialPrice.toInt()} trial khatam hone wala hai. 24-48h mein ₹${postPrice.toInt()} ka full charge katega. MandateX khol kar turant Exit & Cancel karein."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(1000 + (txId.toInt().takeIf { it > 0 } ?: merchant.hashCode()), notification)
        }
    }

    fun showBudgetWarningNotification(
        context: Context,
        categoryLabel: String,
        spent: Double,
        limit: Double,
        percent: Int
    ) {
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            categoryLabel.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val statusText = if (percent >= 100) {
            "Budget Exceeded ($percent%)!"
        } else {
            "Budget Warning ($percent% used)"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERT)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Chanakya AI: $categoryLabel $statusText")
            .setContentText("Spent ₹${spent.toInt()} / ₹${limit.toInt()} this month.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(2000 + categoryLabel.hashCode(), notification)
        }
    }
}

class TrialReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val txId = inputData.getLong("extra_tx_id", 0L)
        val merchant = inputData.getString("extra_merchant") ?: "Subscription Trial"
        val trialPrice = inputData.getDouble("extra_trial_price", 1.0)
        val postPrice = inputData.getDouble("extra_post_price", 499.0)
        TrialShieldScheduler.showTrialExitNotification(
            context = applicationContext,
            txId = txId,
            merchant = merchant,
            trialPrice = trialPrice,
            postPrice = postPrice
        )
        return Result.success()
    }
}

class TrialAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val txId = intent.getLongExtra("extra_tx_id", 0L)
        val merchant = intent.getStringExtra("extra_merchant") ?: "Subscription Trial"
        val trialPrice = intent.getDoubleExtra("extra_trial_price", 1.0)
        val postPrice = intent.getDoubleExtra("extra_post_price", 499.0)
        TrialShieldScheduler.showTrialExitNotification(
            context = context,
            txId = txId,
            merchant = merchant,
            trialPrice = trialPrice,
            postPrice = postPrice
        )
    }
}
