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
    data class Post(val id: String, val userId: String, val mediaType: String, val url: String,
        val caption: String, val createdAt: String, val authorName: String, val authorAvatarUrl: String,
        val likeCount: Long, val commentCount: Long, val likedByMe: Boolean)
    data class Comment(val id: String, val postId: String, val userId: String, val body: String,
        val createdAt: String, val authorName: String, val authorAvatarUrl: String)

    private fun connection(path: String, token: String?): HttpURLConnection =
        (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }

    private fun request(path: String, token: String?, method: String = "GET", body: JSONObject? = null): Pair<Int, String> {
        val conn = connection(path, token)
        try {
            conn.requestMethod = method
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            return status to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally { conn.disconnect() }
    }

    suspend fun upload(token: String, userId: String, bytes: ByteArray, mime: String, caption: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(bytes.isNotEmpty()) { "ফাইলটি খালি।" }
                val ext = when (mime.lowercase()) {
                    "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"
                    "video/mp4" -> "mp4"; "video/quicktime" -> "mov"; "video/3gpp" -> "3gp"
                    else -> error("এই ফাইলের ফরম্যাট সমর্থিত নয়।")
                }
                val type = if (mime.startsWith("video/")) "video" else "image"
                val path = "$userId/${UUID.randomUUID()}.$ext"
                val encoded = path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                val upload = connection("/storage/v1/object/community-media/$encoded", token)
                try {
                    upload.requestMethod = "POST"; upload.doOutput = true
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
                val (status, response) = request("/rest/v1/media_posts", token, "POST", post)
                if (status !in 200..299) error("পোস্ট প্রকাশ হয়নি ($status): $response")
            }
        }

    suspend fun publishText(token: String, userId: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = text.trim()
                require(clean.isNotEmpty() && clean.length <= 500) { "পোস্ট ১ থেকে ৫০০ অক্ষরের মধ্যে লিখুন।" }
                val post = JSONObject().put("user_id", userId).put("media_type", "text")
                    .put("caption", clean)
                val (status, response) = request("/rest/v1/media_posts", token, "POST", post)
                if (status !in 200..299) error("পোস্ট প্রকাশ হয়নি ($status): $response")
            }
        }

    suspend fun feed(token: String? = null, userId: String? = null, authorId: String? = null): Result<List<Post>> = withContext(Dispatchers.IO) {
        runCatching {
            val authorFilter = if (authorId.isNullOrBlank()) "" else "&user_id=eq.${URLEncoder.encode(authorId, "UTF-8")}"
            val (status, body) = request("/rest/v1/media_posts?select=id,user_id,media_type,storage_path,caption,created_at,author_name,author_avatar_url,like_count,comment_count&order=created_at.desc&limit=100$authorFilter", token)
            if (status !in 200..299) error("ভিডিও ফিড লোড হয়নি ($status): $body")
            val likedIds = if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
                val uid = URLEncoder.encode(userId, "UTF-8")
                val (s, b) = request("/rest/v1/media_post_likes?select=post_id&user_id=eq.$uid&limit=500", token)
                if (s !in 200..299) emptySet() else {
                    val arr = JSONArray(b)
                    (0 until arr.length()).map { arr.getJSONObject(it).optString("post_id") }.toSet()
                }
            } else emptySet()
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                val rawPath = row.optString("storage_path")
                val mediaUrl = if (rawPath.isBlank()) "" else rawPath.split("/").joinToString("/") {
                    URLEncoder.encode(it, "UTF-8").replace("+", "%20")
                }.let { BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/community-media/$it" }
                Post(row.getString("id"), row.getString("user_id"), row.getString("media_type"),
                    mediaUrl,
                    row.optString("caption"), row.optString("created_at"), row.optString("author_name", "ব্যবহারকারী"),
                    row.optString("author_avatar_url"), row.optLong("like_count"), row.optLong("comment_count"),
                    row.getString("id") in likedIds)
            }
        }
    }

    suspend fun setLiked(token: String, postId: String, userId: String, liked: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pid = URLEncoder.encode(postId, "UTF-8")
                val uid = URLEncoder.encode(userId, "UTF-8")
                val result = if (liked) request("/rest/v1/media_post_likes", token, "POST",
                    JSONObject().put("post_id", postId).put("user_id", userId))
                else request("/rest/v1/media_post_likes?post_id=eq.$pid&user_id=eq.$uid", token, "DELETE")
                if (result.first !in 200..299) error("লাইক আপডেট হয়নি (${result.first}): ${result.second}")
            }
        }

    suspend fun getComments(postId: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        runCatching {
            val pid = URLEncoder.encode(postId, "UTF-8")
            val (status, body) = request("/rest/v1/media_post_comments?select=id,post_id,user_id,body,created_at,author_name,author_avatar_url&post_id=eq.$pid&order=created_at.asc&limit=200", null)
            if (status !in 200..299) error("মন্তব্য লোড হয়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                Comment(row.getString("id"), row.getString("post_id"), row.getString("user_id"),
                    row.optString("body"), row.optString("created_at"), row.optString("author_name", "ব্যবহারকারী"),
                    row.optString("author_avatar_url"))
            }
        }
    }

    suspend fun addComment(token: String, postId: String, userId: String, body: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(body.trim().isNotEmpty() && body.length <= 2000) { "মন্তব্য ১ থেকে ২০০০ অক্ষরের মধ্যে লিখুন।" }
                val json = JSONObject().put("post_id", postId).put("user_id", userId).put("body", body.trim())
                val (status, response) = request("/rest/v1/media_post_comments", token, "POST", json)
                if (status !in 200..299) error("মন্তব্য প্রকাশ হয়নি ($status): $response")
            }
        }
    data class Story(val id: String, val userId: String, val body: String, val createdAt: String, val expiresAt: String)

    suspend fun publishStory(token: String, userId: String, body: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = body.trim()
                require(clean.isNotEmpty() && clean.length <= 500) { "স্টোরি ১ থেকে ৫০০ অক্ষরের মধ্যে লিখুন।" }
                val payload = JSONObject().put("user_id", userId).put("body", clean)
                val (status, response) = request("/rest/v1/user_stories", token, "POST", payload)
                if (status !in 200..299) error("স্টোরি প্রকাশ হয়নি ($status): $response")
            }
        }

    suspend fun activeStories(token: String? = null): Result<List<Story>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val (status, body) = request(
                    "/rest/v1/user_stories?select=id,user_id,body,created_at,expires_at&expires_at=gt.now()&order=created_at.desc&limit=100",
                    token
                )
                if (status !in 200..299) error("স্টোরি লোড হয়নি ($status): $body")
                val rows = JSONArray(body)
                (0 until rows.length()).map { i ->
                    rows.getJSONObject(i).let { row ->
                        Story(row.getString("id"), row.getString("user_id"), row.getString("body"),
                            row.optString("created_at"), row.optString("expires_at"))
                    }
                }
            }
        }

}
