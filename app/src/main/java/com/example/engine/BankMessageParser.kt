package com.example.engine

import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.CurrencyFormatter
import java.util.Locale

object BankMessageParser {

    private val amountRegex = Regex(
        pattern = """(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        option = RegexOption.IGNORE_CASE
    )
    private val upiIdRegex = Regex("""([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)""")
    private val accountLast4Regex = Regex(
        pattern = """(?:A/c|Acct|Account|Card|ending|linked\s*to)\s*(?:No\.?)?\s*[Xx*]*([0-9]{4})""",
        option = RegexOption.IGNORE_CASE
    )
    private val postTrialPriceRegex = Regex(
        pattern = """(?:then|after(?:\s+trial)?|regular|auto-?debit(?:\s+of)?|renew(?:s|al)?(?:\s+at)?)\s*(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        option = RegexOption.IGNORE_CASE
    )
    private val trialDaysRegex = Regex(
        pattern = """(\d{1,2})\s*(?:day|days|d)\b""",
        option = RegexOption.IGNORE_CASE
    )
    private val trialHoursRegex = Regex(
        pattern = """(\d{1,3})\s*(?:hour|hours|hr|hrs)\b""",
        option = RegexOption.IGNORE_CASE
    )
    private val mandateRefRegex = Regex(
        pattern = """(?:UMRN|Mandate\s*(?:Ref(?:erence)?|ID|No\.?)|SI\s*Ref(?:erence)?|Ref(?:erence)?(?:\s*No\.?)?|Txn\s*ID)\s*[:#-]?\s*([A-Za-z0-9_-]{5,24})""",
        option = RegexOption.IGNORE_CASE
    )
    private val nextDueDateRegex = Regex(
        pattern = """(?:next\s*(?:due|debit|billing|renewal|payment)(?:\s*on|\s*date)?|due\s*(?:on|by)|valid\s*till|expires?\s*on|ends?\s*on)\s*[:\-]?\s*(\d{1,2}[-/\s][A-Za-z0-9]{2,9}(?:[-/\s]\d{2,4})?)""",
        option = RegexOption.IGNORE_CASE
    )
    private val merchantHintRegex = Regex(
        pattern = """(?:to|at|towards|for|merchant|VPA)\s+([A-Za-z0-9 &._-]{2,32}?)(?=\s+(?:via|on|Ref|UMRN|Mandate|UPI|A/c|Card|from|for|then|next|due|Rs|INR|₹|\.|,|$))""",
        option = RegexOption.IGNORE_CASE
    )

    fun isFinancialMessage(sender: String, body: String): Boolean {
        val lower = body.lowercase(Locale.ROOT)
        val hasCurrency = amountRegex.containsMatchIn(body)
        val hasKeywords = listOf(
            "debited", "paid", "spent", "upi", "autopay", "mandate",
            "standing instruction", "e-mandate", "recurring", "umrn",
            "emi", "credit card", "subscription", "trial", "auto-debit",
            "a/c", "acct", "vpa", "transferred", "bill",
            "overdue", "bounced", "bounce", "late fee", "penalty",
            "dishonoured", "failed", "missed", "insufficient", "past due"
        ).any { lower.contains(it) }
        val isKnownWallet = sender.contains("Pay", ignoreCase = true) ||
            sender.contains("PhonePe", ignoreCase = true) ||
            sender.contains("Paytm", ignoreCase = true)
        return hasCurrency && (hasKeywords || isKnownWallet)
    }

    fun isMissedOrOverdueMessage(rawMessage: String): Boolean {
        val lower = rawMessage.lowercase(Locale.ROOT)
        return listOf(
            "overdue", "missed", "bounced", "bounce", "late fee",
            "penalty", "dishonour", "insufficient balance", "ecs return",
            "nach fail", "mandate failed", "past due", "defaulted",
            "payment failed"
        ).any { lower.contains(it) }
    }

    fun isOnTimeCreditOrEmiPayment(tx: TransactionEntity): Boolean {
        if (tx.isCancelled) return false
        if (isMissedOrOverdueMessage(tx.rawMessage)) return false
        val lower = tx.rawMessage.lowercase(Locale.ROOT)
        val isCreditOrLoan = tx.type == TransactionType.CARD_EMI ||
            tx.category == SpendCategory.CARD_EMI ||
            tx.source == "EMI_PAYMENT" ||
            tx.source == "CARD_PAYMENT" ||
            lower.contains("emi") ||
            lower.contains("credit card") ||
            lower.contains("card bill") ||
            lower.contains("loan")
        if (!isCreditOrLoan) return false
        return tx.source == "EMI_PAYMENT" ||
            tx.source == "CARD_PAYMENT" ||
            lower.contains("debited") ||
            lower.contains("paid") ||
            lower.contains("cleared") ||
            lower.contains("executed") ||
            lower.contains("payment received") ||
            lower.contains("thank you for paying")
    }

    fun parse(
        sender: String,
        body: String,
        source: String = "SMS",
        nowMillis: Long = System.currentTimeMillis()
    ): TransactionEntity? {
        if (!isFinancialMessage(sender, body)) return null

        val amountMatch = amountRegex.find(body) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val upiId = upiIdRegex.find(body)?.groupValues?.get(1)
        val accountLast4 = accountLast4Regex.find(body)?.groupValues?.get(1) ?: "0000"
        val bankName = detectBankName(sender, body)
        val merchantName = detectMerchantName(body, upiId)
        val lower = body.lowercase(Locale.ROOT)
        val mandateRefId = mandateRefRegex.find(body)?.groupValues?.get(1)?.trim()

        val isCardOrEmi = lower.contains("credit card") ||
            lower.contains("emi") ||
            lower.contains("loan") ||
            lower.contains("card bill")

        val isTrial = (amount <= 10.0 && (
            lower.contains("trial") ||
                lower.contains("intro") ||
                lower.contains("mandate") ||
                lower.contains("autopay") ||
                lower.contains("first month") ||
                lower.contains("then rs") ||
                lower.contains("then ₹") ||
                lower.contains("then inr")
            ))

        val isAutoPay = isTrial ||
            lower.contains("autopay") ||
            lower.contains("mandate") ||
            lower.contains("standing instruction") ||
            lower.contains("e-mandate") ||
            lower.contains("subscription") ||
            lower.contains("recurring") ||
            lower.contains("auto-debit") ||
            lower.contains("umrn") ||
            lower.contains("si ")

        val type = when {
            isAutoPay -> TransactionType.AUTOPAY
            isCardOrEmi -> TransactionType.CARD_EMI
            else -> TransactionType.EXPENSE
        }

        val category = detectCategory(merchantName, lower, isCardOrEmi)

        var postTrialAmount: Double? = null
        var trialExpiryTimestamp: Long? = null
        if (isTrial) {
            postTrialAmount = postTrialPriceRegex.find(body)
                ?.groupValues?.get(1)
                ?.replace(",", "")
                ?.toDoubleOrNull()
                ?: 499.0

            val hoursFromMsg = trialHoursRegex.find(body)?.groupValues?.get(1)?.toLongOrNull()
            val daysFromMsg = trialDaysRegex.find(body)?.groupValues?.get(1)?.toLongOrNull()
            val durationMillis = when {
                hoursFromMsg != null -> hoursFromMsg * 3600_000L
                daysFromMsg != null -> daysFromMsg * 86_400_000L
                else -> 48 * 3600_000L
            }
            trialExpiryTimestamp = nowMillis + durationMillis
        }

        val extractedNextDue = nextDueDateRegex.find(body)?.groupValues?.get(1)?.trim()
            ?: trialExpiryTimestamp?.let { CurrencyFormatter.formatDateOnly(it) }

        val paymentSource = detectPaymentSource(sender, body, upiId, bankName, accountLast4)

        return TransactionEntity(
            amount = amount,
            merchantName = merchantName,
            upiId = upiId,
            type = type,
            category = category,
            accountLast4 = accountLast4,
            bankName = bankName,
            timestamp = nowMillis,
            rawMessage = body.trim(),
            source = source,
            isTrial = isTrial,
            trialExpiryTimestamp = trialExpiryTimestamp,
            postTrialAmount = postTrialAmount,
            mandateRefId = mandateRefId,
            nextDueDateLabel = extractedNextDue,
            paymentSource = paymentSource,
            externalMessageId = "${source}_${body.trim().hashCode()}"
        )
    }

    private fun detectPaymentSource(
        sender: String,
        body: String,
        upiId: String?,
        bankName: String,
        last4: String
    ): String {
        val combined = "$sender $body ${upiId.orEmpty()}".lowercase(Locale.ROOT)
        val wallet = when {
            combined.contains("phonepe") || combined.contains("@ybl") || combined.contains("@ibl") -> "PhonePe"
            combined.contains("gpay") || combined.contains("google pay") || combined.contains("@ok") -> "Google Pay"
            combined.contains("paytm") || combined.contains("@paytm") -> "Paytm"
            combined.contains("credit card") || combined.contains("card xx") -> "Credit Card"
            combined.contains("upi") -> "UPI"
            else -> bankName
        }
        return if (wallet == bankName) "$bankName • XX$last4" else "$wallet • XX$last4"
    }

    private fun detectBankName(sender: String, body: String): String {
        val combined = "$sender $body".uppercase(Locale.ROOT)
        return when {
            combined.contains("HDFC") -> "HDFC Bank"
            combined.contains("ICICI") -> "ICICI Bank"
            combined.contains("SBI") -> "SBI"
            combined.contains("AXIS") -> "Axis Bank"
            combined.contains("KOTAK") -> "Kotak Bank"
            combined.contains("GPAY") || combined.contains("GOOGLE PAY") -> "Google Pay"
            combined.contains("PHONEPE") -> "PhonePe"
            combined.contains("PAYTM") -> "Paytm"
            else -> sender.take(16).ifBlank { "Bank" }
        }
    }

    private fun detectMerchantName(body: String, upiId: String?): String {
        val knownMerchants = listOf(
            "Zomato", "Swiggy", "Blinkit", "Zepto", "Netflix", "Spotify",
            "Disney+ Hotstar", "Hotstar", "Amazon Prime", "Amazon India", "Amazon",
            "Flipkart", "Myntra", "Canva Pro", "Canva", "YouTube Premium", "YouTube",
            "Adobe", "ChatGPT", "Apple", "PVR", "BookMyShow", "Airtel", "Jio",
            "LIC India", "HDFC Credit Card", "Axis Magnus Credit Card", "ICICI Credit Card",
            "Home Loan", "Car Loan", "Bajaj Finserv", "Uber", "Ola"
        )
        for (merchant in knownMerchants) {
            if (body.contains(merchant, ignoreCase = true)) {
                return merchant
            }
        }
        val hint = merchantHintRegex.find(body)?.groupValues?.get(1)?.trim()
        if (!hint.isNullOrBlank()) {
            return hint.split(" ")
                .filter { it.isNotBlank() }
                .take(3)
                .joinToString(" ") { part ->
                    part.replaceFirstChar { c -> c.titlecase(Locale.ROOT) }
                }
        }
        if (!upiId.isNullOrBlank()) {
            return upiId.substringBefore("@").replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
        return "Merchant Payment"
    }

    private fun detectCategory(
        merchantName: String,
        lowerBody: String,
        isCardOrEmi: Boolean
    ): SpendCategory {
        val m = merchantName.lowercase(Locale.ROOT)
        return when {
            isCardOrEmi -> SpendCategory.CARD_EMI
            m.contains("zomato") || m.contains("swiggy") || m.contains("blinkit") ||
                m.contains("zepto") || lowerBody.contains("food") || lowerBody.contains("restaurant") ->
                SpendCategory.FOOD_DINING
            m.contains("netflix") || m.contains("spotify") || m.contains("hotstar") ||
                m.contains("prime") || m.contains("youtube") || m.contains("canva") ||
                m.contains("adobe") || lowerBody.contains("subscription") || lowerBody.contains("trial") ->
                SpendCategory.OTT_APPS
            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
                lowerBody.contains("shopping") ->
                SpendCategory.SHOPPING
            m.contains("pvr") || m.contains("bookmyshow") || lowerBody.contains("movie") ->
                SpendCategory.ENTERTAINMENT
            m.contains("airtel") || m.contains("jio") || m.contains("lic") || lowerBody.contains("electricity") ||
                lowerBody.contains("recharge") || lowerBody.contains("broadband") ->
                SpendCategory.UTILITIES
            lowerBody.contains("upi") -> SpendCategory.UPI_TRANSFER
            else -> SpendCategory.OTHER
        }
    }
}
