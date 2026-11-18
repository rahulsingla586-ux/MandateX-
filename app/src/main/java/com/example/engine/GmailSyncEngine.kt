package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.example.R
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class GmailRawEmail(
    val messageId: String,
    val subject: String,
    val from: String,
    val dateHeader: String,
    val internalDateMillis: Long,
    val snippet: String,
    val bodyText: String
)

data class GmailAuthOutcome(
    val accessToken: String? = null,
    val resolutionIntentSender: android.content.IntentSender? = null,
    val errorMessage: String? = null
)

object GmailSyncEngine {

    private const val TAG = "GmailSyncEngine"

    const val GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
    const val EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"
    const val DEBUG_SHA1 =
        "73:5A:BF:34:27:E6:A2:03:24:E3:41:6A:BD:04:75:AD:64:02:89:CE"
    const val GMAIL_SEARCH_QUERY =
        "(autopay OR mandate OR subscription OR \"standing instruction\" OR invoice OR receipt) newer_than:180d"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getOAuthClientId(context: Context): String =
        context.getString(R.string.gmail_oauth_client_id).trim()

    /**
     * Legacy GoogleSignInClient builder kept aligned with the gmail.readonly scope for account management/sign-out.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GMAIL_READONLY_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Modern Google Identity AuthorizationClient flow requesting gmail.readonly + userinfo.email.
     * Directly yields an OAuth2 access token or a PendingIntent resolution if user consent is required.
     */
    suspend fun requestGmailAuthorization(activity: Activity): GmailAuthOutcome {
        val oauthClientId = getOAuthClientId(activity)
        if (oauthClientId.isBlank()) {
            return GmailAuthOutcome(
                errorMessage = "Missing gmail_oauth_client_id in strings.xml."
            )
        }
        return try {
            Log.d(TAG, "Starting Gmail OAuth using configured client ID: $oauthClientId")
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(
                    listOf(
                        Scope(GMAIL_READONLY_SCOPE),
                        Scope(EMAIL_SCOPE)
                    )
                )
                .build()

            val result = awaitTask(Identity.getAuthorizationClient(activity).authorize(request))
            if (result.hasResolution()) {
                GmailAuthOutcome(
                    resolutionIntentSender = result.pendingIntent?.intentSender
                )
            } else {
                GmailAuthOutcome(accessToken = result.accessToken)
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Gmail OAuth ApiException status=${e.statusCode} clientId=$oauthClientId", e)
            GmailAuthOutcome(
                errorMessage = mapAuthErrorMessage(e, oauthClientId)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gmail OAuth failed", e)
            GmailAuthOutcome(
                errorMessage = e.localizedMessage ?: "Google OAuth authorization failed."
            )
        }
    }

    fun extractTokenFromResolutionIntent(context: Context, data: Intent?): Result<String> {
        val oauthClientId = getOAuthClientId(context)
        return try {
            val result: AuthorizationResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token.isNullOrBlank()) {
                Result.failure(IllegalStateException("Google did not return an OAuth access token."))
            } else {
                Result.success(token)
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Resolution ApiException status=${e.statusCode} clientId=$oauthClientId", e)
            Result.failure(IllegalStateException(mapAuthErrorMessage(e, oauthClientId), e))
        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting token from intent", e)
            Result.failure(e)
        }
    }

    suspend fun fetchUserEmail(accessToken: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string().orEmpty()
                JSONObject(body).optString("email").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    /**
     * Queries Gmail REST API `users.messages.list` for the last 6 months matching autopay/mandate/subscription/invoice/receipt,
     * then retrieves full message payloads (`users.messages.get`).
     */
    suspend fun fetchRecentFinancialEmails(
        accessToken: String,
        alreadySyncedExternalIds: Set<String>,
        maxResults: Int = 30
    ): Result<List<GmailRawEmail>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(GMAIL_SEARCH_QUERY, "UTF-8")
            val listUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=$encodedQuery&maxResults=$maxResults"

            val listReq = Request.Builder()
                .url(listUrl)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val messageIds = httpClient.newCall(listReq).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Gmail API list error HTTP ${resp.code}: ${extractGoogleError(raw)}")
                }
                val json = JSONObject(raw)
                val arr = json.optJSONArray("messages") ?: return@use emptyList<String>()
                (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }
                }
            }

            val pendingIds = messageIds.filterNot { alreadySyncedExternalIds.contains("gmail_$it") }
            val fetched = mutableListOf<GmailRawEmail>()

            for (msgId in pendingIds) {
                val getUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$msgId?format=full"
                val getReq = Request.Builder()
                    .url(getUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                httpClient.newCall(getReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val msgJson = JSONObject(resp.body?.string().orEmpty())
                    val internalTs = msgJson.optString("internalDate").toLongOrNull()
                        ?: System.currentTimeMillis()
                    val snippet = msgJson.optString("snippet")
                    val payload = msgJson.optJSONObject("payload")

                    var subject = "(No Subject)"
                    var from = "Unknown Sender"
                    var dateHeader = ""
                    val headers = payload?.optJSONArray("headers")
                    if (headers != null) {
                        for (i in 0 until headers.length()) {
                            val h = headers.optJSONObject(i) ?: continue
                            when (h.optString("name").lowercase()) {
                                "subject" -> subject = h.optString("value", subject)
                                "from" -> from = h.optString("value", from)
                                "date" -> dateHeader = h.optString("value", "")
                            }
                        }
                    }

                    val bodyText = extractPlainBodyFromPayload(payload)
                        .ifBlank { snippet }
                        .take(3500)

                    fetched.add(
                        GmailRawEmail(
                            messageId = msgId,
                            subject = subject,
                            from = from,
                            dateHeader = dateHeader,
                            internalDateMillis = internalTs,
                            snippet = snippet,
                            bodyText = bodyText
                        )
                    )
                }
            }

            Result.success(fetched)
        } catch (e: Exception) {
            Log.e(TAG, "Gmail REST fetch failed", e)
            Result.failure(e)
        }
    }

    private fun extractPlainBodyFromPayload(payload: JSONObject?): String {
        if (payload == null) return ""
        val mime = payload.optString("mimeType")
        val directData = payload.optJSONObject("body")?.optString("data").orEmpty()
        if (directData.isNotBlank() && mime.startsWith("text/plain")) {
            return decodeBase64Url(directData)
        }

        val parts = payload.optJSONArray("parts")
        if (parts != null) {
            var htmlFallback = ""
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                val partMime = part.optString("mimeType")
                val nested = extractPlainBodyFromPayload(part)
                if (nested.isNotBlank()) {
                    if (partMime.startsWith("text/plain") || !partMime.startsWith("text/html")) {
                        return nested
                    } else if (htmlFallback.isBlank()) {
                        htmlFallback = nested
                    }
                }
            }
            if (htmlFallback.isNotBlank()) return htmlFallback
        }

        if (directData.isNotBlank()) {
            val decoded = decodeBase64Url(directData)
            return if (mime.contains("html")) {
                decoded.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
            } else {
                decoded
            }
        }
        return ""
    }

    private fun decodeBase64Url(encoded: String): String {
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            String(bytes, Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun extractGoogleError(rawJson: String): String {
        return runCatching {
            JSONObject(rawJson).optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() } ?: rawJson.take(180)
        }.getOrDefault(rawJson.take(180))
    }

    private fun mapAuthErrorMessage(e: ApiException, oauthClientId: String): String {
        return when (e.statusCode) {
            10 -> "OAuth Client ID ($oauthClientId) or SHA-1 mismatch in Google Cloud Console for com.aistudio.mandatex.xkpqzr (DEVELOPER_ERROR 10). Ensure an Android OAuth Client ID exists with SHA-1 $DEBUG_SHA1."
            12501, 16 -> "Google Sign-In cancelled by user."
            7 -> "Network error while contacting Google Auth. Check internet connection."
            else -> "Google OAuth error (${e.statusCode}): ${e.localizedMessage ?: "Check Google Cloud Console OAuth & Gmail API configuration."}"
        }
    }

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { res ->
                if (cont.isActive) cont.resume(res)
            }.addOnFailureListener { ex ->
                if (cont.isActive) cont.resumeWithException(ex)
            }
        }
}
