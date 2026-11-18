package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val indianFormat = DecimalFormat("##,##,##0.##")

    fun formatInr(amount: Double, privacyBlur: Boolean = false): String {
        if (privacyBlur) return "₹****"
        return "₹${indianFormat.format(amount)}"
    }

    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateOnly(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatCountdown(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = targetMillis - nowMillis
        if (diff <= 0L) return "Expiring any moment!"
        val hours = diff / 3_600_000L
        val mins = (diff % 3_600_000L) / 60_000L
        return if (hours >= 24) {
            val days = hours / 24
            val remHours = hours % 24
            "${days}d ${remHours}h left"
        } else {
            "${hours}h ${mins}m left"
        }
    }
}
