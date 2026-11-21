package com.kajerbazar.app

import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight reachability check using only the public publishable key.
 * This does not grant database access; RLS and table grants still apply.
 */
object SupabaseConnection {
    fun checkApi(): String {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer " + BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            }
            val code = connection.responseCode
            if (code in 200..299) {
                "Supabase API-তে সংযোগ সফল (HTTP $code)"
            } else {
                "Supabase API সাড়া দিয়েছে, কিন্তু অনুমতি/কনফিগারেশন যাচাই দরকার (HTTP $code)"
            }
        } catch (_: Exception) {
            "Supabase API-তে সংযোগ হয়নি। ইন্টারনেট বা API সেটিংস পরীক্ষা করুন।"
        } finally {
            connection?.disconnect()
        }
    }
}
