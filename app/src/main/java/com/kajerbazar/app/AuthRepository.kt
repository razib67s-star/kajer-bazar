package com.kajerbazar.app

import android.util.Patterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthRepository {
    data class AuthResult(val success: Boolean, val message: String, val accessToken: String? = null, val email: String? = null, val userId: String? = null)

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

    suspend fun signUp(emailInput: String, password: String, fullName: String): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return@withContext AuthResult(false, "সঠিক ইমেইল ঠিকানা লিখুন।")
        if (password.length < 8) return@withContext AuthResult(false, "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।")
        if (fullName.isBlank()) return@withContext AuthResult(false, "আপনার নাম লিখুন।")
        try {
            val body = JSONObject().put("email", email).put("password", password).put("data", JSONObject().put("full_name", fullName.trim()))
            val (status, json) = request("/auth/v1/signup", body)
            if (status in 200..299) {
                val token = json.optString("access_token")
                val user = json.optJSONObject("user")
                if (token.isNotBlank()) AuthResult(true, "অ্যাকাউন্ট তৈরি হয়েছে এবং লগইন সম্পন্ন।", token, email, user?.optString("id"))
                else AuthResult(true, "অ্যাকাউন্ট তৈরি হয়েছে। ইমেইলে যাচাইয়ের লিংক এলে সেটি খুলে তারপর লগইন করুন।", email = email)
            } else {
                val msg = json.optString("msg", json.optString("message", json.optString("error_description", "অ্যাকাউন্ট তৈরি করা যায়নি।")))
                AuthResult(false, when (status) {
                    429 -> "অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                    400, 422 -> if (msg.contains("already", true) || msg.contains("registered", true)) "এই ইমেইল দিয়ে আগে অ্যাকাউন্ট খোলা হয়েছে। লগইন করুন।" else "তথ্য যাচাই করুন: $msg"
                    else -> "$msg (কোড: $status)"
                })
            }
        } catch (_: Exception) { AuthResult(false, "সার্ভারে সংযোগ করা যায়নি। ইন্টারনেট পরীক্ষা করুন।") }
    }

    suspend fun signIn(emailInput: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return@withContext AuthResult(false, "সঠিক ইমেইল ঠিকানা লিখুন।")
        if (password.isBlank()) return@withContext AuthResult(false, "পাসওয়ার্ড লিখুন।")
        try {
            val (status, json) = request("/auth/v1/token?grant_type=password", JSONObject().put("email", email).put("password", password))
            val token = json.optString("access_token")
            if (status in 200..299 && token.isNotBlank()) AuthResult(true, "লগইন সফল হয়েছে।", token, email, json.optJSONObject("user")?.optString("id"))
            else {
                val msg = json.optString("msg", json.optString("message", json.optString("error_description", "")))
                AuthResult(false, when (status) {
                    400, 401 -> if (msg.contains("confirm", true)) "ইমেইল যাচাই সম্পন্ন করে আবার চেষ্টা করুন।" else "ইমেইল অথবা পাসওয়ার্ড সঠিক নয়।"
                    429 -> "অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ অপেক্ষা করুন।"
                    else -> "লগইন করা যায়নি। আবার চেষ্টা করুন। (কোড: $status)"
                })
            }
        } catch (_: Exception) { AuthResult(false, "লগইন সার্ভারে সংযোগ করা যায়নি।") }
    }
}