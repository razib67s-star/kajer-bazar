package com.kajerbazar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MessageRepository {
    data class Person(
        val id: String, val name: String, val role: String,
        val district: String, val upazila: String, val avatarUrl: String
    )
    data class Message(
        val id: String, val senderId: String, val receiverId: String,
        val body: String, val createdAt: String, val readAt: String?
    )
    data class Thread(val person: Person, val lastMessage: Message, val unreadCount: Int)

    private fun request(path: String, token: String, method: String = "GET", body: JSONObject? = null): Pair<Int, String> {
        val conn = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.requestMethod = method
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Prefer", "return=minimal")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return code to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally { conn.disconnect() }
    }

    suspend fun directory(token: String, currentUserId: String): Result<List<Person>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = request("/rest/v1/messaging_directory?select=id,full_name,role,district,upazila,avatar_url&order=full_name.asc&limit=500", token)
            if (code !in 200..299) error("ব্যবহারকারীর তালিকা পাওয়া যায়নি ($code): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val r = rows.getJSONObject(i)
                Person(r.getString("id"), r.optString("full_name", "ব্যবহারকারী"), r.optString("role"),
                    r.optString("district"), r.optString("upazila"), r.optString("avatar_url"))
            }.filter { it.id != currentUserId }
        }
    }


    suspend fun getFollowingIds(token: String, userId: String): Result<Set<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = URLEncoder.encode(userId, "UTF-8")
            val (code, body) = request("/rest/v1/user_follows?select=following_id&follower_id=eq.$uid&order=created_at.desc&limit=500", token)
            if (code !in 200..299) error("আপনার ফলো তালিকা পাওয়া যায়নি ($code): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i -> rows.getJSONObject(i).getString("following_id") }.toSet()
        }
    }

    suspend fun allMessages(token: String, userId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = URLEncoder.encode(userId, "UTF-8")
            val path = "/rest/v1/direct_messages?select=id,sender_id,receiver_id,body,created_at,read_at&or=(sender_id.eq.$uid,receiver_id.eq.$uid)&order=created_at.desc&limit=1000"
            val (code, body) = request(path, token)
            if (code !in 200..299) error("ইনবক্স লোড হয়নি ($code): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val r = rows.getJSONObject(i)
                Message(r.getString("id"), r.getString("sender_id"), r.getString("receiver_id"),
                    r.optString("body"), r.optString("created_at"), r.optString("read_at").takeIf { it.isNotBlank() })
            }
        }
    }

    suspend fun thread(token: String, userId: String, peerId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = URLEncoder.encode(userId, "UTF-8")
            val pid = URLEncoder.encode(peerId, "UTF-8")
            val path = "/rest/v1/direct_messages?select=id,sender_id,receiver_id,body,created_at,read_at&or=(and(sender_id.eq.$uid,receiver_id.eq.$pid),and(sender_id.eq.$pid,receiver_id.eq.$uid))&order=created_at.asc&limit=500"
            val (code, body) = request(path, token)
            if (code !in 200..299) error("কথোপকথন লোড হয়নি ($code): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val r = rows.getJSONObject(i)
                Message(r.getString("id"), r.getString("sender_id"), r.getString("receiver_id"),
                    r.optString("body"), r.optString("created_at"), r.optString("read_at").takeIf { it.isNotBlank() })
            }
        }
    }

    suspend fun uploadChatImage(token: String, userId: String, bytes: ByteArray, mimeType: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(bytes.isNotEmpty() && bytes.size <= 8 * 1024 * 1024) { "ছবির আকার ৮ MB-এর মধ্যে হতে হবে।" }
                val extension = when (mimeType.lowercase()) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> error("শুধু JPG, PNG অথবা WebP ছবি পাঠানো যাবে।")
                }
                val path = "$userId/chat-${java.util.UUID.randomUUID()}.$extension"
                val encoded = path.split("/").joinToString("/") {
                    URLEncoder.encode(it, "UTF-8").replace("+", "%20")
                }
                val conn = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') +
                    "/storage/v1/object/community-media/$encoded").openConnection() as HttpURLConnection)
                try {
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 20000
                    conn.readTimeout = 20000
                    conn.doOutput = true
                    conn.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.setRequestProperty("Content-Type", mimeType)
                    conn.setRequestProperty("x-upsert", "false")
                    conn.outputStream.use { it.write(bytes) }
                    val status = conn.responseCode
                    val response = (if (status in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (status !in 200..299) error("ছবি আপলোড হয়নি ($status): $response")
                } finally { conn.disconnect() }
                BuildConfig.SUPABASE_URL.trimEnd('/') +
                    "/storage/v1/object/public/community-media/$path"
            }
        }

    const val IMAGE_MESSAGE_PREFIX = "[[KAJER_BAZAR_IMAGE]]"

    suspend fun send(token: String, senderId: String, receiverId: String, body: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = body.trim()
                require(text.isNotEmpty() && text.length <= 4000) { "বার্তা ১ থেকে ৪০০০ অক্ষরের মধ্যে লিখুন।" }
                require(senderId != receiverId) { "নিজেকে বার্তা পাঠানো যাবে না।" }
                val json = JSONObject().put("sender_id", senderId).put("receiver_id", receiverId).put("body", text)
                val (code, response) = request("/rest/v1/direct_messages", token, "POST", json)
                if (code !in 200..299) error("বার্তা পাঠানো যায়নি ($code): $response")
            }
        }

    suspend fun markRead(token: String, userId: String, peerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = URLEncoder.encode(userId, "UTF-8")
            val pid = URLEncoder.encode(peerId, "UTF-8")
            val path = "/rest/v1/direct_messages?sender_id=eq.$pid&receiver_id=eq.$uid&read_at=is.null"
            val (code, response) = request(path, token, "PATCH", JSONObject().put("read_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())))
            if (code !in 200..299) error("পড়া হয়েছে চিহ্নিত করা যায়নি ($code): $response")
        }
    }

    fun buildThreads(messages: List<Message>, people: List<Person>, userId: String): List<Thread> {
        val byId = people.associateBy { it.id }
        return messages.groupBy { if (it.senderId == userId) it.receiverId else it.senderId }
            .mapNotNull { (peerId, rows) ->
                val person = byId[peerId] ?: return@mapNotNull null
                val latest = rows.maxByOrNull { it.createdAt } ?: return@mapNotNull null
                Thread(person, latest, rows.count { it.receiverId == userId && it.senderId == peerId && it.readAt == null })
            }.sortedByDescending { it.lastMessage.createdAt }
    }
}
