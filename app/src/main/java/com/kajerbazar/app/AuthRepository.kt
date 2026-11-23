package com.kajerbazar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthRepository {
    data class AuthResult(
        val success: Boolean,
        val message: String,
        val accessToken: String? = null,
        val phone: String? = null,
        val userId: String? = null
    )

    private fun normalizeBangladeshPhone(input: String): String? {
        val digits = input.filter(Char::isDigit)
        val normalized = when {
            digits.length == 11 && digits.startsWith("01") -> "+88$digits"
            digits.length == 13 && digits.startsWith("880") -> "+$digits"
            input.trim().startsWith("+") && digits.length == 13 && digits.startsWith("880") -> "+$digits"
            else -> return null
        }
        return normalized.takeIf { Regex("^\\+8801[3-9][0-9]{8}$").matches(it) }
    }

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
        } finally { connection.disconnect() }
    }

    suspend fun requestOtp(phoneInput: String, fullName: String, createUser: Boolean): AuthResult = withContext(Dispatchers.IO) {
        val phone = normalizeBangladeshPhone(phoneInput)
            ?: return@withContext AuthResult(false, "সঠিক বাংলাদেশি মোবাইল নম্বর লিখুন। যেমন: 01XXXXXXXXX")
        if (createUser && fullName.isBlank()) return@withContext AuthResult(false, "আপনার পূর্ণ নাম লিখুন।")
        try {
            val body = JSONObject().put("phone", phone).put("create_user", createUser)
            if (createUser) body.put("data", JSONObject().put("full_name", fullName.trim()))
            val (status, json) = request("/auth/v1/otp", body)
            if (status in 200..299) AuthResult(true, "OTP পাঠানোর অনুরোধ সফল হয়েছে। SMS এলে কোড লিখুন।", phone = phone)
            else {
                val message = json.optString("msg", json.optString("message", json.optString("error_description", "")))
                AuthResult(false, when (status) {
                    429 -> "অনেকবার OTP চাওয়া হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                    400, 422 -> "OTP পাঠানো যায়নি। Supabase Phone Auth ও SMS provider সেটিংস যাচাই করুন। $message"
                    else -> "OTP পাঠানো যায়নি (কোড: $status)। $message"
                })
            }
        } catch (_: Exception) {
            AuthResult(false, "OTP সার্ভারে সংযোগ করা যায়নি। ইন্টারনেট পরীক্ষা করুন।")
        }
    }

    suspend fun verifyOtp(phoneInput: String, otpInput: String): AuthResult = withContext(Dispatchers.IO) {
        val phone = normalizeBangladeshPhone(phoneInput)
            ?: return@withContext AuthResult(false, "মোবাইল নম্বরটি আবার যাচাই করুন।")
        val otp = otpInput.filter(Char::isDigit)
        if (otp.length !in 4..8) return@withContext AuthResult(false, "SMS-এ পাওয়া OTP কোড লিখুন।")
        try {
            val body = JSONObject().put("phone", phone).put("token", otp).put("type", "sms")
            val (status, json) = request("/auth/v1/verify", body)
            val token = json.optString("access_token")
            if (status in 200..299 && token.isNotBlank()) {
                AuthResult(true, "লগইন সফল হয়েছে।", token, phone, json.optJSONObject("user")?.optString("id"))
            } else {
                val message = json.optString("msg", json.optString("message", json.optString("error_description", "")))
                AuthResult(false, when (status) {
                    400, 401, 422 -> "OTP ভুল অথবা মেয়াদ শেষ হয়েছে। নতুন কোড নিন।"
                    429 -> "অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ অপেক্ষা করুন।"
                    else -> "OTP যাচাই করা যায়নি (কোড: $status)। $message"
                })
            }
        } catch (_: Exception) {
            AuthResult(false, "OTP যাচাই সার্ভারে সংযোগ করা যায়নি।")
        }
    }
}