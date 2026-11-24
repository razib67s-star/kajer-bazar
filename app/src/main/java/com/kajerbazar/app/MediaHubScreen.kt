package com.kajerbazar.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

@Composable
fun MediaHubScreen(token: String?, userId: String?, onLogin: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ছবি বা ভিডিও শেয়ার করুন।") }
    var busy by remember { mutableStateOf(false) }
    var posts by remember { mutableStateOf<List<MediaRepository.Post>>(emptyList()) }

    fun refresh() {
        scope.launch {
            busy = true
            val result = MediaRepository.feed()
            posts = result.getOrNull().orEmpty()
            status = if (result.isSuccess) "সর্বশেষ পোস্টগুলো" else (result.exceptionOrNull()?.message ?: "ফিড লোড হয়নি।")
            busy = false
        }
    }

    suspend fun publish(uri: Uri, isCameraImage: Boolean = false, bitmap: Bitmap? = null) {
        val auth = token
        val uid = userId
        if (auth == null || uid == null) { onLogin(); return }
        busy = true
        status = "আপলোড হচ্ছে…"
        val result = runCatching {
            val mime = if (isCameraImage) "image/jpeg" else context.contentResolver.getType(uri).orEmpty()
            val bytes = withContext(Dispatchers.IO) {
                if (bitmap != null) ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out); out.toByteArray() }
                else context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("ফাইলটি পড়া যায়নি।")
            }
            val limit = if (mime.startsWith("video/")) 50 * 1024 * 1024 else 8 * 1024 * 1024
            require(bytes.isNotEmpty() && bytes.size <= limit) {
                if (mime.startsWith("video/")) "ভিডিও সর্বোচ্চ ৫০ MB হতে হবে।" else "ছবি সর্বোচ্চ ৮ MB হতে হবে।"
            }
            MediaRepository.upload(auth, uid, bytes, mime, caption)
        }
        busy = false
        status = if (result.isSuccess) {
            caption = ""
            refresh()
            "আপনার পোস্ট প্রকাশ হয়েছে।"
        } else result.exceptionOrNull()?.message ?: "আপলোড ব্যর্থ হয়েছে।"
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { image ->
        if (image != null) scope.launch { publish(Uri.EMPTY, true, image) }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { publish(uri) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { publish(uri) }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ক্যামেরা ও ভিডিও", style = MaterialTheme.typography.headlineSmall)
        Text("নিজের কাজের নমুনা, দক্ষতা ও ছবি শেয়ার করুন।")
        OutlinedTextField(caption, { caption = it.take(500) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("ক্যাপশন (সর্বোচ্চ ৫০০ অক্ষর)") })
        Button(onClick = { if (token == null) onLogin() else camera.launch(null) },
            enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("📷 ক্যামেরা দিয়ে ছবি তুলুন") }
        OutlinedButton(onClick = { if (token == null) onLogin() else imagePicker.launch("image/*") },
            enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("গ্যালারি থেকে ছবি দিন") }
        OutlinedButton(onClick = { if (token == null) onLogin() else videoPicker.launch("video/*") },
            enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("ভিডিও আপলোড করুন") }
        OutlinedButton(onClick = {
            val room = "kajerbazar-" + UUID.randomUUID().toString().replace("-", "").take(18)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://meet.jit.si/$room"))
            context.startActivity(intent)
        }, modifier = Modifier.fillMaxWidth()) { Text("🔴 লাইভ ভিডিও কল শুরু করুন") }
        Text("লাইভ কল Jitsi Meet-এর নিরাপদ রুম লিংকে খুলবে। অপর পক্ষকে একই রুম লিংক পাঠাতে হবে। অচেনা কারও সঙ্গে ব্যক্তিগত তথ্য শেয়ার করবেন না।",
            style = MaterialTheme.typography.bodySmall)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("কমিউনিটি ভিডিও ফিড", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { refresh() }) { Text("রিফ্রেশ") }
        }
        if (posts.isEmpty() && !busy) Text("এখনো কোনো ছবি বা ভিডিও পোস্ট নেই।")
        posts.forEach { post ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (post.mediaType == "video") {
                        AndroidView(factory = { ctx -> VideoView(ctx).apply { setVideoURI(Uri.parse(post.url)); setOnPreparedListener { it.isLooping = false } } },
                            modifier = Modifier.fillMaxWidth().height(220.dp), update = { it.setVideoURI(Uri.parse(post.url)) })
                    } else {
                        AsyncImage(model = post.url, contentDescription = "কমিউনিটি ছবি", modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp))
                    }
                    if (post.caption.isNotBlank()) Text(post.caption)
                    Text("পোস্টকারী: ${post.userId.take(8)}…", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
