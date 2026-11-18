package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object RazorpayCheckoutHelper {

    /**
     * Launches a Razorpay / UPI Checkout payment intent for Pay-As-You-Go Credits (₹1 = 1 Credit).
     * Falls back cleanly with guidance if no UPI payment handler is installed (e.g., emulator),
     * where the user can test via 'Simulate Payment Success'.
     */
    fun launchTopUpCheckout(
        context: Context,
        amountInr: Int,
        userName: String
    ): Boolean {
        val safeAmount = maxOf(5, amountInr)
        val upiUri = Uri.parse(
            "upi://pay?pa=mandatex@razorpay&pn=${Uri.encode("MandateX Credits")}&tn=${Uri.encode("Top-up $safeAmount Credits for $userName")}&am=$safeAmount.00&cu=INR"
        )
        val intent = Intent(Intent.ACTION_VIEW, upiUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(intent, "Pay ₹$safeAmount via Razorpay / UPI"))
            true
        } else {
            Toast.makeText(
                context,
                "No UPI/Razorpay app on device. Tap 'Simulate Payment Success' to test ₹$safeAmount ($safeAmount Credits).",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
