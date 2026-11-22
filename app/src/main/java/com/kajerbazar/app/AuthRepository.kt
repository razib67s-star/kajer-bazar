package com.kajerbazar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthRepository {
    data class AuthResult(val success: Boolean, val message: String, val accessToken: String? = null, val phone: String? = null, val userId: String? = null)

    private fun request(path: String, body: JSONObject): Pair<Int, JSONObject> {
        val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return status to JSONObject(text.ifBlank { "{}" })
        } finally {
            connection.disconnect()
        }
    }

    suspend fun sendPhoneOtp(rawPhone: String): AuthResult = withContext(Dispatchers.IO) {
        val digits = rawPhone.filter { it.isDigit() }
        val phone = when {
            digits.startsWith("880") && digits.length == 13 -> "+$digits"
            digits.startsWith("01") && digits.length == 11 -> "+88$digits"
            else -> return@withContext AuthResult(false, "বাংলাদেশের ১১ সংখ্যার মোবাইল নম্বর লিখুন। যেমন 01XXXXXXXXX")
        }
        try {
            val (status, json) = request("/auth/v1/otp", JSONObject().put("phone", phone).put("create_user", true))
            if (status in 200..299) AuthResult(true, "OTP পাঠানোর অনুরোধ গ্রহণ করা হয়েছে। SMS এলে কোড লিখুন।", phone = phone)
            else {
                val error = json.optString("msg", json.optString("message", json.optString("error_description", "OTP পাঠানো যায়নি।")))
                AuthResult(false, when {
                    status == 429 -> "অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                    status == 400 || status == 422 -> "নম্বর বা Supabase SMS সেটিংস যাচাই করুন।"
                    else -> "$error (কোড: $status)"
                })
            }
        } catch (_: Exception) {
            AuthResult(false, "সার্ভারে সংযোগ করা যায়নি। ইন্টারনেট পরীক্ষা করুন।")
        }
    }

    suspend fun verifyPhoneOtp(phone: String, code: String): AuthResult = withContext(Dispatchers.IO) {
        if (code.length !in 4..8 || !code.all { it.isDigit() }) {
            return@withContext AuthResult(false, "SMS-এ পাওয়া OTP কোডটি লিখুন।")
        }
        try {
            val (status, json) = request("/auth/v1/verify", JSONObject().put("phone", phone).put("token", code).put("type", "sms"))
            val token = json.optString("access_token")
            if (status in 200..299 && token.isNotBlank()) {
                AuthResult(true, "মোবাইল নম্বর যাচাই সম্পন্ন হয়েছে।", accessToken = token, phone = phone, userId = json.optJSONObject("user")?.optString("id"))
            } else {
                AuthResult(false, when (status) {
                    400, 422 -> "OTP ভুল অথবা মেয়াদ শেষ হয়েছে। নতুন কোড নিন।"
                    429 -> "অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ অপেক্ষা করুন।"
                    else -> "যাচাই করা যায়নি। আবার চেষ্টা করুন। (কোড: $status)"
                })
            }
        } catch (_: Exception) {
            AuthResult(false, "যাচাইকরণ সার্ভারে সংযোগ করা যায়নি।")
        }
    }
}
