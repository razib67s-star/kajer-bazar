package com.kajerbazar.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ProfileFollowCard(token: String?, viewerId: String?, profileId: String?, onLogin: () -> Unit) {
    if (profileId.isNullOrBlank()) return
    val scope = rememberCoroutineScope()
    var followers by remember(profileId) { mutableStateOf(0) }
    var following by remember(profileId) { mutableStateOf(0) }
    var isFollowing by remember(profileId, viewerId) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val self = viewerId == profileId

    suspend fun refresh() {
        val stats = SocialFollowRepository.stats(profileId)
        followers = stats.first
        following = stats.second
        if (viewerId != null && token != null) isFollowing = SocialFollowRepository.isFollowing(token, viewerId, profileId)
    }
    LaunchedEffect(profileId, viewerId, token) { refresh() }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column { Text(followers.toString(), style = MaterialTheme.typography.titleLarge); Text("ফলোয়ার") }
                Column { Text(following.toString(), style = MaterialTheme.typography.titleLarge); Text("ফলো করছেন") }
            }
            if (!self) {
                Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    val t = token
                    val v = viewerId
                    if (t == null || v == null) onLogin() else scope.launch {
                        busy = true
                        val result = SocialFollowRepository.setFollowing(t, v, profileId, !isFollowing)
                        status = result.fold({ isFollowing = !isFollowing; "আপডেট হয়েছে"; "" }, { it.message ?: "অনুরোধ ব্যর্থ" })
                        refresh()
                        busy = false
                    }
                }) { Text(if (isFollowing) "ফলো করছেন · আনফলো" else "＋ ফলো করুন") }
            } else Text("এটি আপনার নিজের প্রোফাইল।", style = MaterialTheme.typography.bodySmall)
            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private object SocialFollowRepository {
    private fun request(path: String, token: String? = null, method: String = "GET"): Pair<Int, String> {
        val conn = (java.net.URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as java.net.HttpURLConnection)
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (method == "POST") conn.setRequestProperty("Prefer", "resolution=ignore-duplicates,return=minimal")
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return code to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally { conn.disconnect() }
    }
    suspend fun stats(profileId: String): Pair<Int, Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val enc = java.net.URLEncoder.encode(profileId, "UTF-8")
            val (a, followersBody) = request("/rest/v1/profile_follows?select=follower_id&followed_id=eq.$enc")
            val (b, followingBody) = request("/rest/v1/profile_follows?select=followed_id&follower_id=eq.$enc")
            if (a !in 200..299 || b !in 200..299) error("ফলো তথ্য লোড হয়নি")
            org.json.JSONArray(followersBody).length() to org.json.JSONArray(followingBody).length()
        }.getOrDefault(0 to 0)
    }
    suspend fun isFollowing(token: String, viewerId: String, profileId: String): Boolean = kotlinx.coroutines.withContext(kotlin.coroutines.kotlinx.coroutines.Dispatchers.IO) {
        val f = java.net.URLEncoder.encode(viewerId, "UTF-8")
        val p = java.net.URLEncoder.encode(profileId, "UTF-8")
        val (code, body) = request("/rest/v1/profile_follows?select=follower_id&follower_id=eq.$f&followed_id=eq.$p", token)
        code in 200..299 && org.json.JSONArray(body).length() > 0
    }
    suspend fun setFollowing(token: String, viewerId: String, profileId: String, follow: Boolean): Result<Unit> =
        kotlinx.coroutines.withContext(kotlin.coroutines.kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                require(viewerId != profileId) { "নিজেকে ফলো করা যাবে না" }
                val f = java.net.URLEncoder.encode(viewerId, "UTF-8")
                val p = java.net.URLEncoder.encode(profileId, "UTF-8")
                val path = "/rest/v1/profile_follows?follower_id=eq.$f&followed_id=eq.$p"
                val (code, body) = if (follow) {
                    val conn = (java.net.URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/profile_follows").openConnection() as java.net.HttpURLConnection)
                    try {
                        conn.requestMethod = "POST"; conn.doOutput = true
                        conn.connectTimeout = 15000; conn.readTimeout = 15000
                        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                        conn.setRequestProperty("Authorization", "Bearer $token")
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Prefer", "resolution=ignore-duplicates,return=minimal")
                        conn.outputStream.use { it.write(org.json.JSONObject().put("follower_id",viewerId).put("followed_id",profileId).toString().toByteArray()) }
                        val c=conn.responseCode; val s=if(c in 200..299) conn.inputStream else conn.errorStream
                        c to (s?.bufferedReader()?.use { it.readText() }.orEmpty())
                    } finally { conn.disconnect() }
                } else request(path, token, "DELETE")
                if (code !in 200..299) error("ফলো পরিবর্তন ব্যর্থ: $body")
            }
        }
}
