package com.kajerbazar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Creates a pending sandbox payment order through the trusted Edge Function.
 * Requires a valid Supabase user access token. Never marks an order as paid.
 */
object PaymentRepository {
    data class OrderResult(
        val success: Boolean,
        val message: String,
        val orderId: String? = null,
        val agreedWagePaisa: Long? = null,
        val commissionPaisa: Long? = null,
        val employerTotalPaisa: Long? = null,
        val workerNetPaisa: Long? = null,
        val status: String? = null
    )

    suspend fun createSandboxOrder(
        accessToken: String,
        jobId: String,
        applicationId: String,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): OrderResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext OrderResult(false, "নিরাপদ লগইন প্রয়োজন।")
        val uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        if (!uuid.matches(jobId) || !uuid.matches(applicationId)) {
            return@withContext OrderResult(false, "কাজ ও আবেদনের সঠিক আইডি পাওয়া যায়নি।")
        }
        var connection: HttpURLConnection? = null
        try {
            val url = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/create-payment-order")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            val body = JSONObject()
                .put("job_id", jobId)
                .put("application_id", applicationId)
                .put("idempotency_key", idempotencyKey)
                .toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = JSONObject(responseText.ifBlank { "{}" })
            if (code !in 200..299) {
                val error = json.optString("error", "payment_order_creation_failed")
                val message = when (error) {
                    "authentication_required", "invalid_session" -> "লগইন সেশন বৈধ নয়। আবার নিরাপদে লগইন করুন।"
                    "employer_account_required" -> "শুধু নিয়োগদাতার অ্যাকাউন্ট থেকে পেমেন্ট অর্ডার তৈরি করা যাবে।"
                    "job_not_found_for_employer" -> "এই কাজটি আপনার অ্যাকাউন্টের নয় বা পাওয়া যায়নি।"
                    "selected_application_required" -> "নির্বাচিত কর্মীর আবেদন পাওয়া যায়নি।"
                    else -> "অর্ডার তৈরি হয়নি। তথ্য ও সংযোগ যাচাই করুন। ($code)"
                }
                return@withContext OrderResult(false, message)
            }
            val payment = json.optJSONObject("payment")
                ?: return@withContext OrderResult(false, "সার্ভার থেকে অর্ডারের তথ্য পাওয়া যায়নি।")
            OrderResult(
                success = true,
                message = "পরীক্ষামূলক অর্ডার তৈরি হয়েছে। কোনো টাকা নেওয়া হয়নি।",
                orderId = payment.optString("id").takeIf { it.isNotBlank() },
                agreedWagePaisa = payment.optLong("amount_paisa"),
                commissionPaisa = payment.optLong("commission_paisa"),
                employerTotalPaisa = payment.optLong("employer_total_paisa"),
                workerNetPaisa = payment.optLong("worker_net_paisa"),
                status = payment.optString("status")
            )
        } catch (_: Exception) {
            OrderResult(false, "সার্ভারে সংযোগ করা যায়নি। ইন্টারনেট পরীক্ষা করে আবার চেষ্টা করুন।")
        } finally {
            connection?.disconnect()
        }
    }
}
