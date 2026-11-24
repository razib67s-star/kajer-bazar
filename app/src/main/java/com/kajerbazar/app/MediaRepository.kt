package com.kajerbazar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

object MediaRepository {
    data class Post(val id: String, val userId: String, val mediaType: String, val url: String, val caption: String, val createdAt: String)

    private fun connection(path: String, token: String?): HttpURLConnection =
        (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }

    suspend fun upload(token: String, userId: String, bytes: ByteArray, mime: String, caption: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(bytes.isNotEmpty()) { "ফাইলটি খালি।" }
                val ext = when (mime.lowercase()) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    "video/mp4" -> "mp4"
                    "video/quicktime" -> "mov"
                    "video/3gpp" -> "3gp"
                    else -> error("এই ফাইলের ফরম্যাট সমর্থিত নয়।")
                }
                val type = if (mime.startsWith("video/")) "video" else "image"
                val path = "$userId/${UUID.randomUUID()}.$ext"
                val encoded = path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                val upload = connection("/storage/v1/object/community-media/$encoded", token)
                try {
                    upload.requestMethod = "POST"
                    upload.doOutput = true
                    upload.setRequestProperty("Content-Type", mime)
                    upload.setRequestProperty("x-upsert", "false")
                    upload.outputStream.use { it.write(bytes) }
                    val status = upload.responseCode
                    val response = (if (status in 200..299) upload.inputStream else upload.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (status !in 200..299) error("মিডিয়া আপলোড হয়নি ($status): $response")
                } finally { upload.disconnect() }

                val post = JSONObject().put("user_id", userId).put("media_type", type)
                    .put("storage_path", path).put("caption", caption.take(500))
                val insert = connection("/rest/v1/media_posts", token)
                try {
                    insert.requestMethod = "POST"
                    insert.doOutput = true
                    insert.setRequestProperty("Content-Type", "application/json")
                    insert.setRequestProperty("Prefer", "return=minimal")
                    insert.outputStream.use { it.write(post.toString().toByteArray(Charsets.UTF_8)) }
                    val status = insert.responseCode
                    val response = (if (status in 200..299) insert.inputStream else insert.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (status !in 200..299) error("পোস্ট প্রকাশ হয়নি ($status): $response")
                } finally { insert.disconnect() }
            }
        }

    suspend fun feed(): Result<List<Post>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = connection("/rest/v1/media_posts?select=id,user_id,media_type,storage_path,caption,created_at&order=created_at.desc&limit=50", null)
            try {
                val status = request.responseCode
                val body = (if (status in 200..299) request.inputStream else request.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) error("ভিডিও ফিড লোড হয়নি ($status): $body")
                val rows = JSONArray(body)
                (0 until rows.length()).map { index ->
                    val row = rows.getJSONObject(index)
                    val path = row.getString("storage_path").split("/").joinToString("/") {
                        URLEncoder.encode(it, "UTF-8").replace("+", "%20")
                    }
                    Post(row.getString("id"), row.getString("user_id"), row.getString("media_type"),
                        BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/community-media/$path",
                        row.optString("caption"), row.optString("created_at"))
                }
            } finally { request.disconnect() }
        }
    }
}
