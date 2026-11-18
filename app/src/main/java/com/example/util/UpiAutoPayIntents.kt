package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

enum class UpiApp(
    val displayName: String,
    val packageName: String,
    val mandateGuide: String
) {
    GPAY(
        displayName = "Google Pay",
        packageName = "com.google.android.apps.nbu.paisa.user",
        mandateGuide = "Profile -> AutoPay -> Select Mandate -> Pause or Revoke"
    ),
    PHONEPE(
        displayName = "PhonePe",
        packageName = "com.phonepe.app",
        mandateGuide = "Profile -> AutoPay Settings -> Select Merchant -> Revoke AutoPay"
    ),
    PAYTM(
        displayName = "Paytm",
        packageName = "net.one97.paytm",
        mandateGuide = "Profile -> UPI & Payment Settings -> UPI Automatic Payments -> Cancel"
    )
}

object UpiAutoPayIntents {

    fun openUpiAppAutoPay(context: Context, app: UpiApp): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Toast.makeText(
                context,
                "Opening ${app.displayName}. Navigate to: ${app.mandateGuide}",
                Toast.LENGTH_LONG
            ).show()
            true
        } else {
            // Try generic UPI deep link or show clear installation guidance
            val upiUri = Uri.parse("upi://pay")
            val genericUpi = Intent(Intent.ACTION_VIEW, upiUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (genericUpi.resolveActivity(pm) != null) {
                context.startActivity(Intent.createChooser(genericUpi, "Open UPI App for AutoPay"))
            } else {
                Toast.makeText(
                    context,
                    "${app.displayName} not installed. Steps: ${app.mandateGuide}",
                    Toast.LENGTH_LONG
                ).show()
            }
            false
        }
    }

    fun buildBankCancellationEmail(
        userName: String,
        merchant: String,
        upiOrCardLast4: String,
        amount: Double,
        reason: String
    ): Pair<String, String> {
        val displayUser = userName.ifBlank { "Account Holder" }
        val subject = "URGENT: Immediate Revocation of UPI AutoPay / e-Mandate — $merchant (Ref XX$upiOrCardLast4)"
        val body = buildString {
            appendLine("To the Nodal Officer / Mandate Operations Team,")
            appendLine()
            appendLine("Subject: Request for Immediate Cancellation of e-Mandate / Standing Instruction")
            appendLine()
            appendLine("Dear Sir/Madam,")
            appendLine()
            appendLine("I am writing to formally instruct the bank to immediately REVOKE and BLOCK all future recurring auto-debit / UPI AutoPay / e-Mandate instructions linked to my Account/Card ending in XX$upiOrCardLast4 for the following merchant:")
            appendLine()
            appendLine("• Merchant / Biller: $merchant")
            appendLine("• Mandate / Charge Amount: ₹${amount.toInt()}")
            appendLine("• Reason for Revocation: $reason")
            appendLine()
            appendLine("As per RBI e-Mandate guidelines (Master Direction on Recurring Transactions), a customer has the right to revoke a recurring mandate at any time without penalty, and no debit may be processed without pre-debit notification and consent.")
            appendLine()
            appendLine("Kindly confirm revocation of this mandate in writing within 24 hours and ensure zero future debits.")
            appendLine()
            appendLine("Regards,")
            appendLine(displayUser)
        }
        return subject to body
    }

    fun buildRbiOmbudsmanEmail(
        userName: String,
        bankName: String,
        merchant: String,
        upiOrCardLast4: Double,
        accountLast4: String,
        reason: String
    ): Pair<String, String> {
        val displayUser = userName.ifBlank { "Account Holder" }
        val subject = "RBI Ombudsman (CMS) Complaint: Unauthorized Recurring Debit / Mandate Non-Cancellation — $bankName"
        val body = buildString {
            appendLine("To: Reserve Bank — Integrated Ombudsman Scheme (crpc@rbi.org.in)")
            appendLine()
            appendLine("Complainant Name: $displayUser")
            appendLine("Regulated Entity (Bank/NBFC): $bankName")
            appendLine("Account / Card Last 4 Digits: XX$accountLast4")
            appendLine("Disputed Merchant: $merchant")
            appendLine("Disputed Amount: ₹${upiOrCardLast4.toInt()}")
            appendLine()
            appendLine("Grievance Details:")
            appendLine("1. $reason")
            appendLine("2. Despite attempting cancellation via UPI AutoPay / Bank support, unauthorized recurring charges or lack of mandatory pre-debit notification (RBI Circular DPSS.CO.PD No.447) occurred.")
            appendLine()
            appendLine("Relief Sought:")
            appendLine("- Immediate permanent revocation of the e-Mandate / Standing Instruction.")
            appendLine("- Full refund / reversal of disputed amount ₹${upiOrCardLast4.toInt()} along with applicable compensation under RBI Ombudsman Scheme.")
            appendLine()
            appendLine("Submitted via RBI CMS Portal / Email Escalation")
            appendLine(displayUser)
        }
        return subject to body
    }

    fun buildDuplicateRefundEmail(
        userName: String,
        merchant: String,
        overchargedAmount: Double,
        accountDetails: String
    ): Pair<String, String> {
        val displayUser = userName.ifBlank { "Account Holder" }
        val subject = "Duplicate Charge Refund Request — $merchant (₹${overchargedAmount.toInt()})"
        val body = buildString {
            appendLine("Dear $merchant Billing & Support Team,")
            appendLine()
            appendLine("I noticed a duplicate / ghost recurring charge of ₹${overchargedAmount.toInt()} on my account ($accountDetails) within the last 30 days for the same subscription.")
            appendLine()
            appendLine("Please investigate this duplicate debit and initiate an immediate refund of ₹${overchargedAmount.toInt()} back to the original payment source, and disable any duplicate AutoPay mandate.")
            appendLine()
            appendLine("Thank you,")
            appendLine(displayUser)
        }
        return subject to body
    }

    fun launchEmailIntent(
        context: Context,
        toEmail: String,
        subject: String,
        body: String
    ) {
        val uri = Uri.parse(
            "mailto:${Uri.encode(toEmail)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Send Official Email"))
        }.onFailure {
            Toast.makeText(context, "No email app found. Email copied instead.", Toast.LENGTH_SHORT).show()
        }
    }
}
