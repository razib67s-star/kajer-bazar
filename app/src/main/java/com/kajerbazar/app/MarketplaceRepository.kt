package com.kajerbazar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MarketplaceRepository {
    data class Profile(val fullName: String, val role: String, val district: String, val upazila: String)
    data class Job(val id: String, val title: String, val description: String, val category: String, val district: String, val upazila: String, val wagePaisa: Long)

    private fun request(path: String, token: String, method: String = "GET", body: JSONObject? = null): Pair<Int, String> {
        val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Prefer", "return=minimal")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            return status to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getProfile(token: String, userId: String): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/profiles?select=full_name,role,district,upazila&id=eq.${URLEncoder.encode(userId, "UTF-8")}"
            val (status, body) = request(path, token)
            if (status !in 200..299) error("প্রোফাইল পাওয়া যায়নি ($status)")
            val rows = JSONArray(body)
            if (rows.length() == 0) error("আপনার প্রোফাইল তৈরি হয়নি।")
            val row = rows.getJSONObject(0)
            Profile(row.optString("full_name"), row.optString("role", "worker"), row.optString("district"), row.optString("upazila"))
        }
    }

    suspend fun getPublishedJobs(token: String): Result<List<Job>> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/jobs?select=id,title,description,category,district,upazila,agreed_wage_paisa&status=eq.published&order=created_at.desc&limit=50"
            val (status, body) = request(path, token)
            if (status !in 200..299) error("কাজের তালিকা পাওয়া যায়নি ($status)")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                Job(
                    row.getString("id"), row.getString("title"), row.getString("description"),
                    row.getString("category"), row.getString("district"), row.optString("upazila"),
                    row.getLong("agreed_wage_paisa")
                )
            }
        }
    }

    suspend fun applyToJob(token: String, jobId: String, message: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("job_id", jobId)
            if (message.isNotBlank()) body.put("message", message)
            val (status, response) = request("/rest/v1/rpc/apply_to_job", token, "POST", body)
            if (status !in 200..299) error("আবেদন জমা হয়নি ($status): $response")
        }
    }
}
