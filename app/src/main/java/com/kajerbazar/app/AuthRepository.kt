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
        val userId: String? = null,
        val email: String? = null,
        val refreshToken: String? = null,
        val sessionExpired: Boolean = false
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

    private fun request(path: String, body: JSONObject, bearerToken: String? = null): Pair<Int, JSONObject> {
        val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (!bearerToken.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $bearerToken")
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
                AuthResult(
                    success = true,
                    message = "লগইন সফল হয়েছে।",
                    accessToken = token,
                    phone = phone,
                    userId = json.optJSONObject("user")?.optString("id"),
                    refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
                )
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
    suspend fun signUpEmail(emailInput: String, password: String, fullName: String): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return@withContext AuthResult(false, "সঠিক ইমেইল ঠিকানা লিখুন।")
        if (password.length < 8) return@withContext AuthResult(false, "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।")
        if (fullName.isBlank()) return@withContext AuthResult(false, "আপনার পূর্ণ নাম লিখুন।")
        try {
            val body = JSONObject().put("email", email).put("password", password)
                .put("data", JSONObject().put("full_name", fullName.trim()))
            val (status, json) = request("/auth/v1/signup", body)
            val token = json.optString("access_token")
            if (status in 200..299) {
                if (token.isNotBlank()) AuthResult(true, "অ্যাকাউন্ট তৈরি ও লগইন সফল হয়েছে।", token, userId = json.optJSONObject("user")?.optString("id"), email = email, refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() })
                else AuthResult(true, "ইমেইলে ৬ সংখ্যার যাচাইকরণ কোড পাঠানো হয়েছে। কোডটি লিখুন।", email = email)
            } else {
                val msg = json.optString("msg", json.optString("message", json.optString("error_description", "")))
                AuthResult(false, if (status == 429) "অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।" else "অ্যাকাউন্ট তৈরি করা যায়নি। $msg")
            }
        } catch (_: Exception) { AuthResult(false, "সার্ভারে সংযোগ করা যায়নি। ইন্টারনেট পরীক্ষা করুন।") }
    }

    suspend fun verifyEmailOtp(emailInput: String, otpInput: String): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return@withContext AuthResult(false, "সঠিক ইমেইল ঠিকানা লিখুন।")
        val otp = otpInput.filter(Char::isDigit)
        if (otp.length != 6) return@withContext AuthResult(false, "ইমেইলে পাওয়া ৬ সংখ্যার কোড লিখুন।")
        try {
            val body = JSONObject().put("email", email).put("token", otp).put("type", "signup")
            val (status, json) = request("/auth/v1/verify", body)
            val token = json.optString("access_token")
            if (status in 200..299 && token.isNotBlank()) {
                AuthResult(
                    success = true,
                    message = "ইমেইল যাচাই ও লগইন সফল হয়েছে।",
                    accessToken = token,
                    userId = json.optJSONObject("user")?.optString("id"),
                    email = email,
                    refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
                )
            } else {
                val msg = json.optString("msg", json.optString("message", json.optString("error_description", "")))
                AuthResult(false, when (status) {
                    400, 401, 422 -> "কোডটি ভুল অথবা মেয়াদ শেষ হয়েছে। আবার চেষ্টা করুন।"
                    429 -> "অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ অপেক্ষা করুন।"
                    else -> "ইমেইল কোড যাচাই করা যায়নি ($status)। $msg"
                })
            }
        } catch (_: Exception) {
            AuthResult(false, "ইমেইল কোড যাচাই সার্ভারে সংযোগ করা যায়নি।")
        }
    }

    suspend fun signInEmail(emailInput: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return@withContext AuthResult(false, "সঠিক ইমেইল ঠিকানা লিখুন।")
        if (password.isBlank()) return@withContext AuthResult(false, "পাসওয়ার্ড লিখুন।")
        try {
            val (status, json) = request("/auth/v1/token?grant_type=password", JSONObject().put("email", email).put("password", password))
            val token = json.optString("access_token")
            if (status in 200..299 && token.isNotBlank()) {
                AuthResult(true, "ইমেইল দিয়ে লগইন সফল হয়েছে।", token, userId = json.optJSONObject("user")?.optString("id"), email = email, refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() })
            } else {
                val msg = json.optString("msg", json.optString("message", json.optString("error_description", "")))
                AuthResult(false, when (status) {
                    400, 401 -> if (msg.contains("confirm", true)) "আগে ইমেইল যাচাই সম্পন্ন করুন।" else "ইমেইল অথবা পাসওয়ার্ড সঠিক নয়।"
                    429 -> "অনেকবার চেষ্টা হয়েছে। কিছুক্ষণ অপেক্ষা করুন।"
                    else -> "লগইন করা যায়নি। $msg"
                })
            }
        } catch (_: Exception) { AuthResult(false, "লগইন সার্ভারে সংযোগ করা যায়নি।") }
    }


    suspend fun refreshSession(refreshTokenInput: String): AuthResult = withContext(Dispatchers.IO) {
        if (refreshTokenInput.isBlank()) return@withContext AuthResult(false, "সেশন টোকেন পাওয়া যায়নি।", sessionExpired = true)
        try {
            val (status, json) = request(
                "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refreshTokenInput)
            )
            val token = json.optString("access_token")
            if (status in 200..299 && token.isNotBlank()) {
                val user = json.optJSONObject("user")
                AuthResult(
                    success = true,
                    message = "সেশন পুনরুদ্ধার হয়েছে।",
                    accessToken = token,
                    phone = user?.optString("phone")?.takeIf { it.isNotBlank() },
                    userId = user?.optString("id")?.takeIf { it.isNotBlank() },
                    email = user?.optString("email")?.takeIf { it.isNotBlank() },
                    refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
                )
            } else {
                AuthResult(
                    false,
                    if (status in 400..403) "সেশনের মেয়াদ শেষ। আবার লগইন করুন।" else "সেশন যাচাই করা যায়নি ($status)।",
                    sessionExpired = status in 400..403
                )
            }
        } catch (_: Exception) {
            AuthResult(false, "সার্ভারে সংযোগ হয়নি; পরে আবার চেষ্টা করুন।")
        }
    }


    suspend fun deleteAccount(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(accessToken.isNotBlank()) { "লগইন সেশন পাওয়া যায়নি। আবার লগইন করুন।" }
            val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/delete-account").openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val message = runCatching { JSONObject(response).optString("error") }.getOrNull().orEmpty()
                    error(message.ifBlank { "অ্যাকাউন্ট ডিলিট করা যায়নি ($status)।" })
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun signOut(accessToken: String) = withContext(Dispatchers.IO) {
        if (accessToken.isNotBlank()) {
            try {
                request("/auth/v1/logout", JSONObject(), accessToken)
            } catch (_: Exception) {
                // Local encrypted session is cleared by the caller even if offline.
            }
        }
    }

}
