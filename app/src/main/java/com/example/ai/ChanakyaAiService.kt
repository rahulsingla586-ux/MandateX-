package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CreditCardEntity
import com.example.data.model.LoanEmiEntity
import com.example.data.model.SpendCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.MandateRepository
import com.example.engine.GmailRawEmail
import com.example.util.CurrencyFormatter
import com.example.util.UpiAutoPayIntents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AiActionButton(
    val label: String,
    val type: AiActionType,
    val subject: String = "",
    val body: String = "",
    val targetEmail: String = ""
)

enum class AiActionType {
    SEND_EMAIL,
    OPEN_UPI_AUTOPAY,
    EXPORT_PDF
}

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentName: String? = null,
    val actions: List<AiActionButton> = emptyList(),
    val isError: Boolean = false
)

sealed interface ChanakyaUiState {
    data object Idle : ChanakyaUiState
    data object Thinking : ChanakyaUiState
    data class Error(val message: String) : ChanakyaUiState
}

data class ParsedGmailMandate(
    val messageId: String,
    val serviceName: String,
    val amountInr: Double,
    val paymentSource: String,
    val mandateRefId: String?,
    val nextDueDate: String?,
    val isTrial: Boolean,
    val postTrialAmountInr: Double?,
    val transactionType: TransactionType,
    val spendCategory: SpendCategory,
    val summary: String,
    val emailTimestampMillis: Long,
    val rawEmailSnippet: String
)

data class CibilPdfAuditResult(
    val chatMessage: ChatMessage,
    val extractedScore: Int?
)

data class AttachmentAuditResult(
    val chatMessage: ChatMessage,
    val extractedTransactions: List<TransactionEntity> = emptyList(),
    val extractedCreditCards: List<CreditCardEntity> = emptyList(),
    val extractedLoans: List<LoanEmiEntity> = emptyList()
)

class ChanakyaAiService(
    private val repository: MandateRepository,
    private val prefs: UserPreferencesRepository
) {
    private val TAG = "ChanakyaAiService"
    private val modelName = "gemini-3.8-flash"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val chatHistory = mutableListOf<JSONObject>()

    private val chatSystemInstruction = """
        You are Chanakya AI — the sharp, protective Indian fintech 'Money Munim' inside the MandateX app.
        - Match the user's language (natural Hinglish or English).
        - Always call the appropriate Room Database tool functions before answering questions about user spending, merchants, cards/EMIs, loans, trials, duplicate charges, or budgets.
        - Quote exact ₹ amounts, card utilization %, due dates, and item counts returned by the tools. Never invent fake numbers.
        - Keep answers concise, structured with clean bullet points, and actionable.
        - If trials are expiring or duplicate charges exist, strongly advise immediate action.
    """.trimIndent()

    private val cibilAuditSystemInstruction = """
        You are Chanakya AI, an expert Indian CIBIL & Credit Bureau Report Auditor inside MandateX.
        When the user uploads an official TransUnion CIBIL (or Experian/CRIF/Equifax) PDF report:
        1. Always start with `CIBIL Score: <300-900 number or Not Found>` on the very first line if a score appears in the PDF.
        2. Inspect every credit card, loan account, DPD (Days Past Due) grid, settlement/written-off tag, and hard enquiry in the PDF.
        3. Flag potential bureau ERRORS & discrepancies (e.g., closed loan still marked Active, wrong DPD late markers, duplicate enquiries, unfamiliar accounts, wrong sanctioned/limit amounts).
        4. Cross-check against the user's MandateX SMS/Gmail context provided in the prompt.
        5. Provide 3-5 concrete, numbered Hinglish/English score-boosting steps with exact ₹ paydown targets (e.g., bringing card utilization below 30% / 10%) and step-by-step instructions for raising a dispute on cibil.com/dispute.
    """.trimIndent()

    private val attachmentExtractorSystemInstruction = """
        You are Chanakya AI, an Indian fintech document and payment screenshot parser.
        Inspect the uploaded bank PDF statement, credit card bill, loan schedule, or UPI payment screenshot.
        Extract ONLY real transactions, credit card bills, and loans visibly present in the uploaded document/image. Never invent data.
        Set accountLast4/last4 to the actual visible 4 digits or '0000' if omitted.
        Also provide a concise Hinglish/English breakdown in summaryMarkdown.
    """.trimIndent()

    private val emailParserSystemInstruction = """
        You are Chanakya AI's financial email extraction engine for Indian users.
        Inspect each Gmail message (subject, sender, date, body) and extract real recurring subscriptions, UPI AutoPay / e-Mandates, hidden low-cost trials, credit card bills, EMIs, and paid invoices.
        Ignore marketing newsletters or promotional ads that do not represent an active mandate, bill, receipt, or trial.
        Always preserve the exact input messageId.
    """.trimIndent()

    private val chatToolsJson: JSONArray by lazy {
        JSONArray().put(
            JSONObject().put(
                "functionDeclarations",
                JSONArray().apply {
                    put(
                        JSONObject()
                            .put("name", "getExpensesByMerchant")
                            .put(
                                "description",
                                "Query local Room database for all expenses, UPI payments, or subscriptions matching a merchant name or UPI ID."
                            )
                            .put(
                                "parameters",
                                JSONObject()
                                    .put("type", "OBJECT")
                                    .put(
                                        "properties",
                                        JSONObject().put(
                                            "merchant",
                                            JSONObject()
                                                .put("type", "STRING")
                                                .put(
                                                    "description",
                                                    "Merchant name or keyword to search in local ledger."
                                                )
                                        )
                                    )
                                    .put("required", JSONArray().put("merchant"))
                            )
                    )
                    put(
                        JSONObject()
                            .put("name", "getCardsAndLoans")
                            .put(
                                "description",
                                "Fetch all Loan EMIs, Credit Card bills, utilization %, due dates, and Card/EMI transactions tracked in the local Room database."
                            )
                    )
                    put(
                        JSONObject()
                            .put("name", "getExpiringTrials")
                            .put(
                                "description",
                                "Fetch active low-cost introductory trials (₹1, ₹2, ₹5) that are about to auto-renew at full price."
                            )
                    )
                    put(
                        JSONObject()
                            .put("name", "getDuplicateCharges")
                            .put(
                                "description",
                                "Scan last 30 days of transactions to detect duplicate or ghost subscriptions charged multiple times or across multiple cards/UPI IDs."
                            )
                    )
                    put(
                        JSONObject()
                            .put("name", "getCategoryBudgetStatus")
                            .put(
                                "description",
                                "Check monthly spending caps, current spend, and 80%/100% overspend status across categories."
                            )
                            .put(
                                "parameters",
                                JSONObject()
                                    .put("type", "OBJECT")
                                    .put(
                                        "properties",
                                        JSONObject().put(
                                            "category",
                                            JSONObject()
                                                .put("type", "STRING")
                                                .put(
                                                    "description",
                                                    "Category name like 'Food & Dining' or 'ALL' to return all category budgets."
                                                )
                                        )
                                    )
                                    .put("required", JSONArray().put("category"))
                            )
                    )
                    put(
                        JSONObject()
                            .put("name", "draftCancellationEmail")
                            .put(
                                "description",
                                "Generate a formal Bank e-Mandate cancellation email or refund request draft for a merchant."
                            )
                            .put(
                                "parameters",
                                JSONObject()
                                    .put("type", "OBJECT")
                                    .put(
                                        "properties",
                                        JSONObject()
                                            .put(
                                                "merchant",
                                                JSONObject()
                                                    .put("type", "STRING")
                                                    .put("description", "Name of the merchant or OTT app to cancel")
                                            )
                                            .put(
                                                "reason",
                                                JSONObject()
                                                    .put("type", "STRING")
                                                    .put(
                                                        "description",
                                                        "Reason such as 'Trial cancellation before price hike' or 'Duplicate charge refund'"
                                                    )
                                            )
                                    )
                                    .put("required", JSONArray().put("merchant").put("reason"))
                            )
                    )
                }
            )
        )
    }

    private val attachmentExtractionSchemaJson: JSONObject by lazy {
        val txProperties = JSONObject()
            .put("merchantName", stringSchema("Clean merchant or biller name"))
            .put("amountInr", numberSchema("Numeric INR amount (> 0)"))
            .put(
                "transactionType",
                enumSchema(
                    listOf("AUTOPAY", "CARD_EMI", "EXPENSE"),
                    "AUTOPAY for mandates/subscriptions/trials, CARD_EMI for card bills/EMIs, EXPENSE for normal spends"
                )
            )
            .put(
                "spendCategory",
                enumSchema(
                    listOf(
                        "OTT_APPS",
                        "FOOD_DINING",
                        "SHOPPING",
                        "CARD_EMI",
                        "UTILITIES",
                        "ENTERTAINMENT",
                        "UPI_TRANSFER",
                        "OTHER"
                    ),
                    "Matching SpendCategory"
                )
            )
            .put("accountLast4", stringSchema("Last 4 digits of account/card if visible, else 0000"))
            .put("bankName", stringSchema("Bank or payment app name"))
            .put("paymentSource", stringSchema("Source label like UPI app or Card"))
            .put("mandateRefId", stringSchema("UMRN or mandate ID if visible, else empty string"))
            .put("nextDueDate", stringSchema("Next due date label if visible, else empty string"))
            .put("isTrial", booleanSchema("True if low-cost introductory trial converting to higher recurring charge"))
            .put("postTrialAmountInr", numberSchema("Post-trial recurring amount in INR if trial, else 0"))
            .put("upiId", stringSchema("UPI VPA handle if visible, else empty string"))

        val cardProperties = JSONObject()
            .put("cardName", stringSchema("Credit card name"))
            .put("bankName", stringSchema("Issuing bank name"))
            .put("last4", stringSchema("Card last 4 digits, else 0000"))
            .put("currentBillInr", numberSchema("Total bill due in INR"))
            .put("creditLimitInr", numberSchema("Credit limit in INR if present, else equal to bill or 0"))
            .put("minDueInr", numberSchema("Minimum amount due in INR if present, else 0"))
            .put("dueDateLabel", stringSchema("Payment due date text if visible"))

        val loanProperties = JSONObject()
            .put("loanName", stringSchema("Loan or EMI account name"))
            .put("lenderName", stringSchema("Lender bank or NBFC"))
            .put("accountLast4", stringSchema("Loan account last 4 digits, else 0000"))
            .put("emiAmountInr", numberSchema("Monthly EMI amount in INR"))
            .put("remainingMonths", integerSchema("Remaining EMIs if visible, else 12"))
            .put("totalMonths", integerSchema("Total tenure months if visible, else 12"))
            .put("nextDueDayOfMonth", integerSchema("Day of month EMI is due (1-28), else 5"))
            .put("interestRate", numberSchema("Annual interest rate % if visible, else 0"))

        JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put(
                        "summaryMarkdown",
                        stringSchema("Concise Hinglish/English summary of extracted amounts, merchants, hidden AutoPay mandates, cards, and warnings.")
                    )
                    .put(
                        "transactions",
                        JSONObject()
                            .put("type", "ARRAY")
                            .put(
                                "items",
                                JSONObject()
                                    .put("type", "OBJECT")
                                    .put("properties", txProperties)
                                    .put("required", JSONArray(txProperties.keys().asSequence().toList()))
                            )
                    )
                    .put(
                        "creditCards",
                        JSONObject()
                            .put("type", "ARRAY")
                            .put(
                                "items",
                                JSONObject()
                                    .put("type", "OBJECT")
                                    .put("properties", cardProperties)
                                    .put("required", JSONArray(cardProperties.keys().asSequence().toList()))
                            )
                    )
                    .put(
                        "loans",
                        JSONObject()
                            .put("type", "ARRAY")
                            .put(
                                "items",
                                JSONObject()
                                    .put("type", "OBJECT")
                                    .put("properties", loanProperties)
                                    .put("required", JSONArray(loanProperties.keys().asSequence().toList()))
                            )
                    )
            )
            .put(
                "required",
                JSONArray(listOf("summaryMarkdown", "transactions", "creditCards", "loans"))
            )
    }

    private val emailBatchSchemaJson: JSONObject by lazy {
        val itemProperties = JSONObject()
            .put("messageId", stringSchema("Exact messageId from input email"))
            .put(
                "isFinancialOrSubscription",
                booleanSchema("True if email is an autopay mandate, subscription, trial, invoice, receipt, credit card bill, or EMI notice")
            )
            .put("serviceName", stringSchema("Clean merchant or service name"))
            .put(
                "amountInr",
                numberSchema("Charge amount in INR (convert approximate USD if only USD appears, else numeric ₹ amount)")
            )
            .put("paymentSource", stringSchema("Payment app, bank account, or card source label"))
            .put("mandateRefId", stringSchema("Mandate ID, UMRN, Invoice #, or Subscription ID if found, else empty string"))
            .put("nextDueDate", stringSchema("Next renewal / due date or trial end date, else empty string"))
            .put("isTrial", booleanSchema("True if introductory/low-cost/free trial that will convert to paid subscription"))
            .put("postTrialAmountInr", numberSchema("Upcoming full recurring price in INR after trial ends, 0 if not a trial"))
            .put(
                "transactionType",
                enumSchema(
                    listOf("AUTOPAY", "CARD_EMI", "EXPENSE"),
                    "AUTOPAY for recurring mandates/subscriptions/trials, CARD_EMI for credit card bills/loans, EXPENSE for one-time receipts"
                )
            )
            .put(
                "spendCategory",
                enumSchema(
                    listOf(
                        "OTT_APPS",
                        "FOOD_DINING",
                        "SHOPPING",
                        "CARD_EMI",
                        "UTILITIES",
                        "ENTERTAINMENT",
                        "UPI_TRANSFER",
                        "OTHER"
                    ),
                    "Best matching SpendCategory"
                )
            )
            .put("summary", stringSchema("One concise line summarizing the mandate, bill, or trial end warning"))

        JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject().put(
                    "items",
                    JSONObject()
                        .put("type", "ARRAY")
                        .put(
                            "items",
                            JSONObject()
                                .put("type", "OBJECT")
                                .put("properties", itemProperties)
                                .put("required", JSONArray(itemProperties.keys().asSequence().toList()))
                        )
                )
            )
            .put("required", JSONArray().put("items"))
    }

    fun resetConversation() {
        chatHistory.clear()
    }

    suspend fun ask(userQuery: String): ChatMessage {
        val pendingActions = mutableListOf<AiActionButton>()
        return try {
            val workingHistory = chatHistory.map { JSONObject(it.toString()) }.toMutableList()
            workingHistory.add(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userQuery)))
            )

            var response = callGemini(
                contents = workingHistory,
                systemInstruction = chatSystemInstruction,
                tools = chatToolsJson
            )

            var loopCount = 0
            while (response.functionCalls.isNotEmpty() && loopCount < 5) {
                loopCount++
                response.modelContent?.let { workingHistory.add(it) }

                val functionResponseParts = JSONArray()
                for (call in response.functionCalls) {
                    val toolJson = executeTool(call.name, jsonObjectToMap(call.args), pendingActions)
                    functionResponseParts.put(
                        JSONObject().put(
                            "functionResponse",
                            JSONObject()
                                .put("name", call.name)
                                .put("response", JSONObject(toolJson.toString()))
                        )
                    )
                }
                workingHistory.add(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", functionResponseParts)
                )

                response = callGemini(
                    contents = workingHistory,
                    systemInstruction = chatSystemInstruction,
                    tools = chatToolsJson
                )
            }

            response.modelContent?.let { workingHistory.add(it) }
            chatHistory.clear()
            chatHistory.addAll(workingHistory)

            val replyText = response.text?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Maine aapka ledger check kar liya, par response generate nahi ho paya. Dobara poochein."

            ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                isUser = false,
                text = replyText,
                actions = pendingActions.distinctBy { it.label }
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chanakya AI timed out", e)
            ChatMessage(
                id = "err_${System.currentTimeMillis()}",
                isUser = false,
                text = "Chanakya AI request timed out after 60 seconds. Please check internet connectivity and try again.",
                isError = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chanakya AI failed", e)
            ChatMessage(
                id = "err_${System.currentTimeMillis()}",
                isUser = false,
                text = "Chanakya AI error: ${e.localizedMessage ?: "Unable to reach Gemini API."}",
                isError = true
            )
        }
    }

    suspend fun analyzeAttachment(
        bytes: ByteArray,
        mimeType: String,
        fileName: String,
        userPrompt: String
    ): AttachmentAuditResult {
        return try {
            if (bytes.size > 18 * 1024 * 1024) {
                return AttachmentAuditResult(
                    chatMessage = ChatMessage(
                        id = "err_${System.currentTimeMillis()}",
                        isUser = false,
                        text = "File size is over 18 MB limit. Please upload a smaller bank PDF or screenshot.",
                        isError = true
                    )
                )
            }

            val promptText = userPrompt.ifBlank {
                "Extract all visible transactions, AutoPay mandates, credit card bills, and loan EMIs from $fileName accurately, and explain the findings concisely in Hinglish/English."
            }

            val resolvedMime = if (mimeType.isBlank() || mimeType == "application/octet-stream") {
                "application/pdf"
            } else {
                mimeType
            }

            val (payloadBytes, payloadMime) = if (resolvedMime.startsWith("image/")) {
                val bitmap = decodeScaledBitmap(bytes, maxDim = 1600)
                    ?: throw IllegalArgumentException("Could not decode image screenshot.")
                bitmapToJpegBytes(bitmap) to "image/jpeg"
            } else {
                bytes to resolvedMime
            }

            val base64Data = Base64.encodeToString(payloadBytes, Base64.NO_WRAP)
            val userContent = JSONObject()
                .put("role", "user")
                .put(
                    "parts",
                    JSONArray()
                        .put(
                            JSONObject().put(
                                "inlineData",
                                JSONObject()
                                    .put("mimeType", payloadMime)
                                    .put("data", base64Data)
                            )
                        )
                        .put(JSONObject().put("text", promptText))
                )

            val response = callGemini(
                contents = listOf(userContent),
                systemInstruction = attachmentExtractorSystemInstruction,
                responseSchema = attachmentExtractionSchemaJson
            )

            val rawJson = response.text?.trim().orEmpty()
            val root = runCatching { JSONObject(rawJson) }.getOrNull()
            val now = System.currentTimeMillis()

            val parsedTransactions = mutableListOf<TransactionEntity>()
            val txArray = root?.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val obj = txArray.optJSONObject(i) ?: continue
                    val merchant = obj.optString("merchantName").trim()
                    val amount = obj.optDouble("amountInr", 0.0)
                    if (merchant.isBlank() || amount <= 0.0) continue

                    val txType = runCatching {
                        TransactionType.valueOf(obj.optString("transactionType", "EXPENSE"))
                    }.getOrDefault(TransactionType.EXPENSE)
                    val category = runCatching {
                        SpendCategory.valueOf(obj.optString("spendCategory", "OTHER"))
                    }.getOrDefault(SpendCategory.OTHER)
                    val last4 = obj.optString("accountLast4").filter { it.isDigit() }.takeLast(4).ifBlank { "0000" }
                    val bank = obj.optString("bankName").trim().ifBlank { "Uploaded Document" }
                    val sourceLabel = obj.optString("paymentSource").trim().ifBlank { "$bank • XX$last4" }
                    val mandateRef = obj.optString("mandateRefId").trim().takeIf { it.isNotBlank() }
                    val nextDue = obj.optString("nextDueDate").trim().takeIf { it.isNotBlank() }
                    val upiId = obj.optString("upiId").trim().takeIf { it.isNotBlank() }
                    val isTrial = obj.optBoolean("isTrial", false)
                    val postTrial = obj.optDouble("postTrialAmountInr", 0.0)

                    parsedTransactions.add(
                        TransactionEntity(
                            amount = amount,
                            merchantName = merchant,
                            upiId = upiId,
                            type = txType,
                            category = category,
                            accountLast4 = last4,
                            bankName = bank,
                            timestamp = now - i * 1000L,
                            rawMessage = "Parsed from $fileName: $merchant ₹${amount.toInt()} ($sourceLabel)",
                            source = if (resolvedMime.startsWith("image/")) "SCREENSHOT" else "PDF_STATEMENT",
                            isTrial = isTrial,
                            trialExpiryTimestamp = if (isTrial) now + 44L * 3_600_000L else null,
                            postTrialAmount = if (isTrial && postTrial > 0.0) postTrial else if (isTrial) 499.0 else null,
                            mandateRefId = mandateRef,
                            nextDueDateLabel = nextDue,
                            paymentSource = sourceLabel,
                            externalMessageId = "doc_${fileName.hashCode()}_${merchant.lowercase()}_${amount.toInt()}_$i"
                        )
                    )
                }
            }

            val parsedCards = mutableListOf<CreditCardEntity>()
            val cardArray = root?.optJSONArray("creditCards")
            if (cardArray != null) {
                for (i in 0 until cardArray.length()) {
                    val obj = cardArray.optJSONObject(i) ?: continue
                    val cardName = obj.optString("cardName").trim()
                    val bankName = obj.optString("bankName").trim().ifBlank { "Card Issuer" }
                    val currentBill = obj.optDouble("currentBillInr", 0.0).coerceAtLeast(0.0)
                    if (cardName.isBlank()) continue
                    val last4 = obj.optString("last4").filter { it.isDigit() }.takeLast(4).ifBlank { "0000" }
                    val limit = obj.optDouble("creditLimitInr", currentBill).coerceAtLeast(currentBill)
                    val minDue = obj.optDouble("minDueInr", 0.0).coerceAtLeast(0.0)
                    val dueLabel = obj.optString("dueDateLabel").trim().ifBlank { "As per statement" }

                    parsedCards.add(
                        CreditCardEntity(
                            cardName = cardName,
                            bankName = bankName,
                            last4 = last4,
                            currentBill = currentBill,
                            creditLimit = limit,
                            dueDateLabel = dueLabel,
                            minDue = minDue,
                            isPaid = currentBill <= 0.0
                        )
                    )
                }
            }

            val parsedLoans = mutableListOf<LoanEmiEntity>()
            val loanArray = root?.optJSONArray("loans")
            if (loanArray != null) {
                for (i in 0 until loanArray.length()) {
                    val obj = loanArray.optJSONObject(i) ?: continue
                    val loanName = obj.optString("loanName").trim()
                    val emi = obj.optDouble("emiAmountInr", 0.0)
                    if (loanName.isBlank() || emi <= 0.0) continue
                    val lender = obj.optString("lenderName").trim().ifBlank { "Lender" }
                    val last4 = obj.optString("accountLast4").filter { it.isDigit() }.takeLast(4).ifBlank { "0000" }
                    val remMonths = obj.optInt("remainingMonths", 12).coerceAtLeast(1)
                    val totMonths = obj.optInt("totalMonths", remMonths).coerceAtLeast(remMonths)
                    val dueDay = obj.optInt("nextDueDayOfMonth", 5).coerceIn(1, 28)
                    val rate = obj.optDouble("interestRate", 0.0).coerceAtLeast(0.0)

                    parsedLoans.add(
                        LoanEmiEntity(
                            loanName = loanName,
                            lenderName = lender,
                            accountLast4 = last4,
                            emiAmount = emi,
                            remainingMonths = remMonths,
                            totalMonths = totMonths,
                            nextDueDayOfMonth = dueDay,
                            interestRate = rate,
                            autoDebitEnabled = true
                        )
                    )
                }
            }

            val summaryText = root?.optString("summaryMarkdown")?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: rawJson.takeIf { it.isNotEmpty() && root == null }
                ?: "Document analyze ho gaya, par koi financial entry detect nahi hui."

            AttachmentAuditResult(
                chatMessage = ChatMessage(
                    id = "ai_doc_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "Document Scan ($fileName):\n\n$summaryText"
                ),
                extractedTransactions = parsedTransactions,
                extractedCreditCards = parsedCards,
                extractedLoans = parsedLoans
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Multimodal scan timed out", e)
            AttachmentAuditResult(
                chatMessage = ChatMessage(
                    id = "err_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "Document scan timed out after 60 seconds. Try again with a smaller file.",
                    isError = true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Multimodal scan failed", e)
            AttachmentAuditResult(
                chatMessage = ChatMessage(
                    id = "err_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "Document scan error: ${e.localizedMessage ?: "Failed to analyze attachment."}",
                    isError = true
                )
            )
        }
    }

    suspend fun analyzeCibilReportPdf(
        bytes: ByteArray,
        mimeType: String,
        fileName: String,
        localEstimateSummary: String,
        userName: String
    ): CibilPdfAuditResult {
        return try {
            if (bytes.size > 18 * 1024 * 1024) {
                return CibilPdfAuditResult(
                    chatMessage = ChatMessage(
                        id = "err_${System.currentTimeMillis()}",
                        isUser = false,
                        text = "CIBIL PDF is over the 18 MB limit. Please upload a smaller PDF file.",
                        isError = true
                    ),
                    extractedScore = null
                )
            }

            val displayUser = userName.ifBlank { "Account Holder" }
            val promptText = """
                Analyze this uploaded Official CIBIL Credit Report PDF ($fileName) for $displayUser.
                MandateX Live SMS/Gmail Ledger Context:
                $localEstimateSummary

                Please output in crisp Hinglish + English:
                - CIBIL Score found in PDF (first line as: `CIBIL Score: <number>` if present)
                - Errors / Discrepancies Detected (DPD mistakes, closed loans still active, unfamiliar accounts, duplicate hard enquiries, limit mismatches)
                - Concrete Score-Boosting Action Plan (exact ₹ paydown targets & timeline)
                - Dispute Steps if any error needs correction on cibil.com
            """.trimIndent()

            val resolvedMime = if (mimeType.isBlank() || mimeType == "application/octet-stream") {
                "application/pdf"
            } else {
                mimeType
            }
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val userContent = JSONObject()
                .put("role", "user")
                .put(
                    "parts",
                    JSONArray()
                        .put(
                            JSONObject().put(
                                "inlineData",
                                JSONObject()
                                    .put("mimeType", resolvedMime)
                                    .put("data", base64Data)
                            )
                        )
                        .put(JSONObject().put("text", promptText))
                )

            val response = callGemini(
                contents = listOf(userContent),
                systemInstruction = cibilAuditSystemInstruction
            )

            val replyText = response.text?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "CIBIL PDF upload ho gaya, par readable credit accounts extract nahi ho paye. Kripya non-password-protected PDF upload karein."

            val extractedScore = Regex("""CIBIL\s*Score\s*[:\-]?\s*([3-8]\d{2}|900)\b""", RegexOption.IGNORE_CASE)
                .find(replyText)
                ?.groupValues?.get(1)
                ?.toIntOrNull()
                ?.takeIf { it in 300..900 }

            val disputeSubject = "CIBIL Report Error Dispute & Correction Request — $displayUser"
            val disputeBody = buildString {
                appendLine("To TransUnion CIBIL Dispute Resolution / Lending Bank Nodal Officer,")
                appendLine()
                appendLine("I am writing to report discrepancies detected in my recent CIBIL Credit Information Report ($fileName).")
                appendLine("Detected Observations / MandateX Audit:")
                appendLine(replyText.take(700))
                appendLine()
                appendLine("Kindly investigate and update the bureau record as per RBI Credit Information Companies norms within 30 days.")
                appendLine()
                appendLine("Regards,")
                appendLine(displayUser)
            }

            CibilPdfAuditResult(
                chatMessage = ChatMessage(
                    id = "ai_cibil_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "Official CIBIL Report Audit ($fileName):\n\n$replyText",
                    actions = listOf(
                        AiActionButton(
                            label = "Draft CIBIL Dispute Email",
                            type = AiActionType.SEND_EMAIL,
                            subject = disputeSubject,
                            body = disputeBody,
                            targetEmail = "dispute@transunion.com"
                        )
                    )
                ),
                extractedScore = extractedScore
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "CIBIL PDF audit timed out", e)
            CibilPdfAuditResult(
                chatMessage = ChatMessage(
                    id = "err_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "CIBIL PDF analysis timed out after 60 seconds. If your CIBIL PDF is password-protected (usually first 4 letters of name + DDMM or PAN), unlock it first and retry.",
                    isError = true
                ),
                extractedScore = null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "CIBIL PDF audit failed", e)
            CibilPdfAuditResult(
                chatMessage = ChatMessage(
                    id = "err_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "CIBIL PDF audit error: ${e.localizedMessage ?: "Could not process the PDF. Ensure it is not password-locked."}",
                    isError = true
                ),
                extractedScore = null
            )
        }
    }

    suspend fun parseGmailMessages(emails: List<GmailRawEmail>): Result<List<ParsedGmailMandate>> {
        if (emails.isEmpty()) return Result.success(emptyList())
        val extracted = mutableListOf<ParsedGmailMandate>()
        val byId = emails.associateBy { it.messageId }

        return try {
            for (batch in emails.chunked(8)) {
                val prompt = buildString {
                    appendLine("Extract active AutoPay mandates, hidden trials, credit card bills, EMIs, and subscription receipts from these emails:")
                    appendLine()
                    batch.forEach { email ->
                        appendLine("--- EMAIL ---")
                        appendLine("messageId: ${email.messageId}")
                        appendLine("from: ${email.from}")
                        appendLine("date: ${email.dateHeader}")
                        appendLine("subject: ${email.subject}")
                        appendLine("body: ${email.bodyText.take(1200)}")
                        appendLine()
                    }
                }

                val userContent = JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt)))

                val response = callGemini(
                    contents = listOf(userContent),
                    systemInstruction = emailParserSystemInstruction,
                    responseSchema = emailBatchSchemaJson
                )
                val jsonText = response.text ?: continue
                val root = JSONObject(jsonText)
                val itemsArray = root.optJSONArray("items") ?: continue

                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.optJSONObject(i) ?: continue
                    if (!obj.optBoolean("isFinancialOrSubscription", false)) continue

                    val msgId = obj.optString("messageId")
                    val sourceEmail = byId[msgId]
                    val serviceName = obj.optString("serviceName").trim()
                    val amount = obj.optDouble("amountInr", 0.0)
                    if (serviceName.isBlank() || amount < 0.0) continue

                    val isTrial = obj.optBoolean("isTrial", false)
                    val postTrial = obj.optDouble("postTrialAmountInr", 0.0)
                    val typeEnum = runCatching {
                        TransactionType.valueOf(obj.optString("transactionType", "AUTOPAY"))
                    }.getOrDefault(TransactionType.AUTOPAY)
                    val catEnum = runCatching {
                        SpendCategory.valueOf(obj.optString("spendCategory", "OTT_APPS"))
                    }.getOrDefault(SpendCategory.OTT_APPS)

                    extracted.add(
                        ParsedGmailMandate(
                            messageId = msgId.ifBlank { "batch_${System.currentTimeMillis()}_$i" },
                            serviceName = serviceName,
                            amountInr = if (amount > 0.0) amount else 1.0,
                            paymentSource = obj.optString("paymentSource").ifBlank { "Gmail Billing" },
                            mandateRefId = obj.optString("mandateRefId").takeIf { it.isNotBlank() },
                            nextDueDate = obj.optString("nextDueDate").takeIf { it.isNotBlank() },
                            isTrial = isTrial,
                            postTrialAmountInr = if (isTrial && postTrial > 0.0) postTrial else if (isTrial) 499.0 else null,
                            transactionType = typeEnum,
                            spendCategory = catEnum,
                            summary = obj.optString("summary").ifBlank { sourceEmail?.subject ?: serviceName },
                            emailTimestampMillis = sourceEmail?.internalDateMillis ?: System.currentTimeMillis(),
                            rawEmailSnippet = "${sourceEmail?.subject.orEmpty()} — ${sourceEmail?.from.orEmpty()}\n${sourceEmail?.snippet.orEmpty()}"
                        )
                    )
                }
            }
            Result.success(extracted)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini Gmail parsing timed out", e)
            Result.failure(IllegalStateException("Chanakya AI Gmail parsing timed out after 60 seconds.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Gmail parsing failed", e)
            Result.failure(e)
        }
    }

    private data class ParsedFunctionCall(
        val name: String,
        val args: JSONObject?
    )

    private data class GeminiResult(
        val text: String?,
        val functionCalls: List<ParsedFunctionCall>,
        val modelContent: JSONObject?
    )

    private fun resolveApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        if (key.isBlank() ||
            key == "GEMINI_API_KEY_DEFAULT_VALUE" ||
            key.contains("YOUR_", ignoreCase = true)
        ) {
            throw IllegalStateException(
                "Gemini API key is not configured. Add GEMINI_API_KEY in the AI Studio Secrets panel and rebuild."
            )
        }
        return key
    }

    private suspend fun callGemini(
        contents: List<JSONObject>,
        systemInstruction: String,
        tools: JSONArray? = null,
        responseSchema: JSONObject? = null
    ): GeminiResult {
        val apiKey = resolveApiKey()

        val payload = JSONObject()
            .put("contents", JSONArray(contents))
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            )

        if (tools != null) {
            payload.put("tools", tools)
        }
        if (responseSchema != null) {
            payload.put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", responseSchema)
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        val rawBody = withTimeout(60_000L) {
            executeCallSuspending(request)
        }

        val responseJson = JSONObject(rawBody)
        val candidate = responseJson.optJSONArray("candidates")?.optJSONObject(0)
        if (candidate == null) {
            val blockReason = responseJson.optJSONObject("promptFeedback")?.optString("blockReason")
            val message = if (!blockReason.isNullOrBlank()) {
                "Gemini blocked request ($blockReason)."
            } else {
                "Gemini API returned an empty response."
            }
            throw IllegalStateException(message)
        }

        val modelContent = candidate.optJSONObject("content")
        val parts = modelContent?.optJSONArray("parts")
        val functionCalls = mutableListOf<ParsedFunctionCall>()
        val textSegments = mutableListOf<String>()

        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                val fn = part.optJSONObject("functionCall")
                if (fn != null) {
                    val fnName = fn.optString("name")
                    if (fnName.isNotBlank()) {
                        functionCalls.add(ParsedFunctionCall(fnName, fn.optJSONObject("args")))
                    }
                } else if (!part.optBoolean("thought", false)) {
                    val txt = part.optString("text")
                    if (txt.isNotEmpty()) {
                        textSegments.add(txt)
                    }
                }
            }
        }

        return GeminiResult(
            text = textSegments.joinToString("\n").takeIf { it.isNotBlank() },
            functionCalls = functionCalls,
            modelContent = modelContent
        )
    }

    private suspend fun executeCallSuspending(request: Request): String =
        suspendCancellableCoroutine { cont ->
            val call = httpClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        val bodyStr = res.body?.string().orEmpty()
                        if (!res.isSuccessful) {
                            val apiMessage = runCatching {
                                JSONObject(bodyStr).optJSONObject("error")?.optString("message")
                            }.getOrNull()?.takeIf { it.isNotBlank() }
                                ?: "HTTP ${res.code}"
                            if (cont.isActive) {
                                cont.resumeWithException(
                                    IllegalStateException("Gemini API error (${res.code}): $apiMessage")
                                )
                            }
                            return
                        }
                        if (cont.isActive) cont.resume(bodyStr)
                    }
                }
            })
        }

    private fun jsonObjectToMap(obj: JSONObject?): Map<String, JsonElement> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, JsonElement>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.opt(k)
            result[k] = when (v) {
                is Boolean -> JsonPrimitive(v)
                is Number -> JsonPrimitive(v)
                else -> JsonPrimitive(v?.toString() ?: "")
            }
        }
        return result
    }

    private fun stringSchema(description: String): JSONObject =
        JSONObject().put("type", "STRING").put("description", description)

    private fun numberSchema(description: String): JSONObject =
        JSONObject().put("type", "NUMBER").put("description", description)

    private fun integerSchema(description: String): JSONObject =
        JSONObject().put("type", "INTEGER").put("description", description)

    private fun booleanSchema(description: String): JSONObject =
        JSONObject().put("type", "BOOLEAN").put("description", description)

    private fun enumSchema(values: List<String>, description: String): JSONObject =
        JSONObject()
            .put("type", "STRING")
            .put("enum", JSONArray(values))
            .put("description", description)

    private fun decodeScaledBitmap(bytes: ByteArray, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        var sampleSize = 1
        while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
        return stream.toByteArray()
    }

    private suspend fun executeTool(
        name: String,
        args: Map<String, JsonElement>,
        actionsOut: MutableList<AiActionButton>
    ): JsonObject {
        return when (name) {
            "getExpensesByMerchant" -> {
                val merchant = args["merchant"]?.jsonPrimitive?.content.orEmpty()
                val items = repository.getExpensesByMerchant(merchant)
                val total = items.filter { !it.isCancelled }.sumOf { it.amount }
                JsonObject(
                    mapOf(
                        "merchantQuery" to JsonPrimitive(merchant),
                        "matchCount" to JsonPrimitive(items.size),
                        "totalSpentInr" to JsonPrimitive(total),
                        "transactions" to JsonArray(items.take(15).map { tx ->
                            JsonObject(
                                mapOf(
                                    "merchant" to JsonPrimitive(tx.merchantName),
                                    "amountInr" to JsonPrimitive(tx.amount),
                                    "date" to JsonPrimitive(CurrencyFormatter.formatDateOnly(tx.timestamp)),
                                    "category" to JsonPrimitive(tx.category.label),
                                    "type" to JsonPrimitive(tx.type.label),
                                    "accountLast4" to JsonPrimitive("XX${tx.accountLast4}"),
                                    "upiId" to JsonPrimitive(tx.upiId.orEmpty())
                                )
                            )
                        })
                    )
                )
            }

            "getCardsAndLoans" -> {
                val txs = repository.getCardsAndLoans()
                val loans = repository.getLoansSnapshot()
                val cards = repository.getCreditCardsSnapshot()
                val totalMonthlyEmi = loans.sumOf { it.emiAmount }
                val totalCardDue = cards.sumOf { it.currentBill }
                JsonObject(
                    mapOf(
                        "totalMonthlyEmiInr" to JsonPrimitive(totalMonthlyEmi),
                        "totalCardBillDueInr" to JsonPrimitive(totalCardDue),
                        "loans" to JsonArray(loans.map { l ->
                            JsonObject(
                                mapOf(
                                    "loanName" to JsonPrimitive(l.loanName),
                                    "lender" to JsonPrimitive(l.lenderName),
                                    "emiAmountInr" to JsonPrimitive(l.emiAmount),
                                    "remainingMonths" to JsonPrimitive(l.remainingMonths),
                                    "dueDayOfMonth" to JsonPrimitive(l.nextDueDayOfMonth),
                                    "accountLast4" to JsonPrimitive("XX${l.accountLast4}")
                                )
                            )
                        }),
                        "creditCards" to JsonArray(cards.map { c ->
                            val utilPct = if (c.creditLimit > 0) ((c.currentBill / c.creditLimit) * 100).toInt() else 0
                            JsonObject(
                                mapOf(
                                    "cardName" to JsonPrimitive(c.cardName),
                                    "last4" to JsonPrimitive("XX${c.last4}"),
                                    "currentBillInr" to JsonPrimitive(c.currentBill),
                                    "creditLimitInr" to JsonPrimitive(c.creditLimit),
                                    "utilizationPercent" to JsonPrimitive(utilPct),
                                    "dueDate" to JsonPrimitive(c.dueDateLabel)
                                )
                            )
                        }),
                        "recentCardEmiLedgerCount" to JsonPrimitive(txs.size)
                    )
                )
            }

            "getExpiringTrials" -> {
                val trials = repository.getExpiringTrials()
                if (trials.isNotEmpty()) {
                    actionsOut.add(
                        AiActionButton(
                            label = "Open UPI AutoPay",
                            type = AiActionType.OPEN_UPI_AUTOPAY
                        )
                    )
                }
                JsonObject(
                    mapOf(
                        "activeTrialCount" to JsonPrimitive(trials.size),
                        "trials" to JsonArray(trials.map { tx ->
                            val countdown = tx.trialExpiryTimestamp?.let {
                                CurrencyFormatter.formatCountdown(it)
                            } ?: "Expiring soon"
                            JsonObject(
                                mapOf(
                                    "merchant" to JsonPrimitive(tx.merchantName),
                                    "trialPaidInr" to JsonPrimitive(tx.amount),
                                    "upcomingHikeInr" to JsonPrimitive(tx.postTrialAmount ?: 499.0),
                                    "timeRemaining" to JsonPrimitive(countdown),
                                    "accountLast4" to JsonPrimitive("XX${tx.accountLast4}")
                                )
                            )
                        })
                    )
                )
            }

            "getDuplicateCharges" -> {
                val duplicates = repository.getDuplicateAlertsSnapshot()
                if (duplicates.isNotEmpty()) {
                    val first = duplicates.first()
                    val userName = prefs.userName.first()
                    val (sub, body) = UpiAutoPayIntents.buildDuplicateRefundEmail(
                        userName = userName,
                        merchant = first.merchantName,
                        overchargedAmount = first.totalOvercharged,
                        accountDetails = first.charges.joinToString { "XX${it.accountLast4}" }
                    )
                    actionsOut.add(
                        AiActionButton(
                            label = "Draft ${first.merchantName} Refund Email",
                            type = AiActionType.SEND_EMAIL,
                            subject = sub,
                            body = body,
                            targetEmail = "support@${first.merchantName.lowercase().replace(" ", "")}.com"
                        )
                    )
                }
                JsonObject(
                    mapOf(
                        "duplicateCount" to JsonPrimitive(duplicates.size),
                        "alerts" to JsonArray(duplicates.map { alert ->
                            JsonObject(
                                mapOf(
                                    "merchant" to JsonPrimitive(alert.merchantName),
                                    "chargeCount" to JsonPrimitive(alert.charges.size),
                                    "potentialOverchargeInr" to JsonPrimitive(alert.totalOvercharged),
                                    "reason" to JsonPrimitive(alert.reason)
                                )
                            )
                        })
                    )
                )
            }

            "getCategoryBudgetStatus" -> {
                val filter = args["category"]?.jsonPrimitive?.content.orEmpty()
                val statuses = repository.getBudgetStatusesSnapshot()
                val filtered = if (filter.isBlank() || filter.equals("ALL", ignoreCase = true)) {
                    statuses
                } else {
                    statuses.filter { it.category.label.contains(filter, ignoreCase = true) }
                }
                JsonObject(
                    mapOf(
                        "budgets" to JsonArray(filtered.map { b ->
                            JsonObject(
                                mapOf(
                                    "category" to JsonPrimitive(b.category.label),
                                    "spentInr" to JsonPrimitive(b.spent),
                                    "limitInr" to JsonPrimitive(b.limit),
                                    "percentUsed" to JsonPrimitive((b.percentage * 100).toInt()),
                                    "isWarning80" to JsonPrimitive(b.isWarning80),
                                    "isExceeded100" to JsonPrimitive(b.isExceeded100)
                                )
                            )
                        })
                    )
                )
            }

            "draftCancellationEmail" -> {
                val merchant = args["merchant"]?.jsonPrimitive?.content ?: "Subscription"
                val reason = args["reason"]?.jsonPrimitive?.content ?: "Immediate revocation of AutoPay mandate"
                val userName = prefs.userName.first()
                val bankEmail = prefs.bankSupportEmail.first()
                val matching = repository.getExpensesByMerchant(merchant).firstOrNull()
                val (subject, body) = UpiAutoPayIntents.buildBankCancellationEmail(
                    userName = userName.ifBlank { "MandateX Account Holder" },
                    merchant = merchant,
                    upiOrCardLast4 = matching?.accountLast4 ?: "----",
                    amount = matching?.postTrialAmount ?: matching?.amount ?: 0.0,
                    reason = reason
                )
                actionsOut.add(
                    AiActionButton(
                        label = "Send $merchant Cancellation Email",
                        type = AiActionType.SEND_EMAIL,
                        subject = subject,
                        body = body,
                        targetEmail = bankEmail
                    )
                )
                JsonObject(
                    mapOf(
                        "status" to JsonPrimitive("draft_created"),
                        "toEmail" to JsonPrimitive(bankEmail),
                        "subject" to JsonPrimitive(subject),
                        "previewBody" to JsonPrimitive(body)
                    )
                )
            }

            else -> JsonObject(mapOf("error" to JsonPrimitive("Unknown tool: $name")))
        }
    }
}
