package com.kajerbazar.app

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.widget.MediaController
import android.widget.VideoView
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun MediaHubScreen(token: String?, userId: String?, onLogin: () -> Unit, onOpenProfile: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var storyText by remember { mutableStateOf("") }
    var stories by remember { mutableStateOf<List<MediaRepository.Story>>(emptyList()) }
    var status by remember { mutableStateOf("ছবি বা ভিডিও শেয়ার করুন।") }
    var busy by remember { mutableStateOf(false) }
    var posts by remember { mutableStateOf<List<MediaRepository.Post>>(emptyList()) }
    var liveRoomUrl by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            busy = true
            val result = MediaRepository.feed(token, userId)
            posts = result.getOrNull().orEmpty()
            stories = MediaRepository.activeStories(token).getOrNull().orEmpty()
            status = if (result.isSuccess) "সর্বশেষ পোস্টগুলো" else (result.exceptionOrNull()?.message ?: "ফিড লোড হয়নি।")
            busy = false
        }
    }

    suspend fun publish(uri: Uri, isCameraImage: Boolean = false, bitmap: Bitmap? = null, mimeOverride: String? = null) {
        val auth = token
        val uid = userId
        if (auth == null || uid == null) { onLogin(); return }
        busy = true
        status = "আপলোড হচ্ছে…"
        val result = runCatching {
            val mime = if (isCameraImage) "image/jpeg" else (mimeOverride ?: context.contentResolver.getType(uri).orEmpty())
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
        if (result.isSuccess) {
            caption = ""
            status = "আপনার পোস্ট প্রকাশ হয়েছে।"
            refresh()
        } else status = result.exceptionOrNull()?.message ?: "আপলোড ব্যর্থ হয়েছে।"
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { image ->
        if (image != null) scope.launch { publish(Uri.EMPTY, true, image) }
    }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    val recordVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { saved ->
        val uri = pendingVideoUri
        pendingVideoUri = null
        if (saved && uri != null) scope.launch { publish(uri, mimeOverride = "video/mp4") }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { publish(uri) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { publish(uri) }
    }

    LaunchedEffect(token, userId) { refresh() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("কমিউনিটি ওয়াল", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("স্টোরি • ২৪ ঘণ্টা", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = storyText,
                    onValueChange = { storyText = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("আজকের স্টোরি লিখুন…") },
                    minLines = 2
                )
                Button(
                    onClick = {
                        val auth = token
                        val uid = userId
                        if (auth == null || uid == null) onLogin() else scope.launch {
                            busy = true
                            val result = MediaRepository.publishStory(auth, uid, storyText)
                            busy = false
                            if (result.isSuccess) {
                                storyText = ""
                                status = "আপনার ২৪ ঘণ্টার স্টোরি প্রকাশ হয়েছে।"
                                refresh()
                            } else status = result.exceptionOrNull()?.message ?: "স্টোরি প্রকাশ হয়নি।"
                        }
                    },
                    enabled = !busy && storyText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("＋ স্টোরি প্রকাশ করুন") }
                if (stories.isEmpty()) {
                    Text("এখনো কোনো সক্রিয় স্টোরি নেই।", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("সাম্প্রতিক স্টোরি", fontWeight = FontWeight.SemiBold)
                    stories.forEach { story ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ব্যবহারকারী • " + story.userId.take(8), style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold)
                                Text(story.body, style = MaterialTheme.typography.bodyLarge)
                                Text("২৪ ঘণ্টার মধ্যে মুছে যাবে", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("আপনার পোস্ট", fontWeight = FontWeight.Bold)
                OutlinedTextField(caption, { caption = it.take(500) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("আপনার কথা লিখুন…") }, minLines = 2)
                Button(
                    onClick = {
                        val auth = token
                        val uid = userId
                        if (auth == null || uid == null) onLogin() else scope.launch {
                            busy = true
                            status = "পোস্ট প্রকাশ হচ্ছে…"
                            val result = MediaRepository.publishText(auth, uid, caption)
                            busy = false
                            if (result.isSuccess) {
                                caption = ""
                                status = "আপনার লেখা পোস্ট প্রকাশ হয়েছে।"
                                refresh()
                            } else status = result.exceptionOrNull()?.message ?: "পোস্ট প্রকাশ হয়নি।"
                        }
                    },
                    enabled = !busy && caption.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📝 লেখা পোস্ট প্রকাশ করুন") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { if (token == null) onLogin() else camera.launch(null) },
                        enabled = !busy, modifier = Modifier.weight(1f)) { Text("📷 ছবি তুলুন") }
                    OutlinedButton(onClick = { if (token == null) onLogin() else imagePicker.launch("image/*") },
                        enabled = !busy, modifier = Modifier.weight(1f)) { Text("🖼 ছবি দিন") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        if (token == null) onLogin() else runCatching {
                            val dir = File(context.cacheDir, "videos").apply { mkdirs() }
                            val file = File(dir, "kajerbazar-${UUID.randomUUID()}.mp4")
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                            pendingVideoUri = uri
                            recordVideo.launch(uri)
                        }.onFailure { status = it.message ?: "ক্যামেরা চালু করা যায়নি।" }
                    }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("🎥 ভিডিও") }
                    OutlinedButton(onClick = { if (token == null) onLogin() else videoPicker.launch("video/*") },
                        enabled = !busy, modifier = Modifier.weight(1f)) { Text("ভিডিও আপলোড") }
                }
                Text("ছবি সর্বোচ্চ ৮ MB, ভিডিও সর্বোচ্চ ৫০ MB। ছবি বা ভিডিও নির্বাচন করলেই ক্যাপশনসহ পোস্ট হবে।",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = {
            if (token == null || userId == null) onLogin() else {
                val room = "kajerbazar-" + UUID.randomUUID().toString().replace("-", "").take(18)
                liveRoomUrl = "https://meet.jit.si/$room"
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("🔴 ভিডিও কল রুম তৈরি করুন") }
        Text("এটি ব্যক্তিগত ভিডিও কল রুমের লিংক (Jitsi Meet)—পাবলিক লাইভ সম্প্রচার নয়। কল করতে লগইন লাগবে; অপর পক্ষকে একই লিংক পাঠান।",
            style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("সাম্প্রতিক পোস্ট", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { refresh() }) { Text("রিফ্রেশ") }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(status, style = MaterialTheme.typography.bodySmall)
        if (posts.isEmpty() && !busy) Text("এখনো কোনো ছবি বা ভিডিও পোস্ট নেই।")
        posts.forEach { post -> MediaPostCard(post, token, userId, onLogin, onOpenProfile) { refresh() } }
    }
    liveRoomUrl?.let { roomUrl ->
        AlertDialog(
            onDismissRequest = { liveRoomUrl = null },
            title = { Text("ভিডিও কল রুম তৈরি হয়েছে") },
            text = { Text("এটি Jitsi Meet-এর বাহ্যিক ভিডিও কল রুম, পাবলিক লাইভ সম্প্রচার নয়। যাঁর সঙ্গে কথা বলবেন তাঁকে লিংক পাঠান। লিংকটি যাঁদের কাছে থাকবে তাঁরা রুমে প্রবেশ করতে পারবেন।\n\n$roomUrl") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(roomUrl)))
                    liveRoomUrl = null
                }) { Text("কল খুলুন") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "কাজের বাজার লাইভ ভিডিও কল: $roomUrl")
                        }
                        context.startActivity(Intent.createChooser(share, "লাইভ কলের লিংক শেয়ার করুন"))
                    }) { Text("লিংক শেয়ার") }
                    TextButton(onClick = { liveRoomUrl = null }) { Text("বন্ধ") }
                }
            }
        )
    }
}

@Composable
fun MediaPostsSection(token: String?, userId: String?, onLogin: () -> Unit, profileUserId: String? = userId, onOpenProfile: (String) -> Unit = {}) {
    var posts by remember { mutableStateOf<List<MediaRepository.Post>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun refresh() {
        scope.launch {
            loading = true
            posts = MediaRepository.feed(token, userId, profileUserId).getOrNull().orEmpty()
            loading = false
        }
    }
    LaunchedEffect(token, userId) { refresh() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider()
        Text(if (profileUserId == userId) "আমার পোস্ট" else "এই প্রোফাইলের পোস্ট", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading && posts.isEmpty()) Text("আপনার প্রকাশিত ছবি ও ভিডিও এখানে দেখা যাবে।")
        posts.forEach { post -> MediaPostCard(post, token, userId, onLogin, onOpenProfile) { refresh() } }
    }
}

@Composable
private fun MediaPostCard(
    post: MediaRepository.Post, token: String?, userId: String?, onLogin: () -> Unit,
    onOpenProfile: (String) -> Unit = {}, onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var liked by remember(post.id, post.likedByMe) { mutableStateOf(post.likedByMe) }
    var likeCount by remember(post.id, post.likeCount) { mutableStateOf(post.likeCount) }
    var showComments by remember(post.id) { mutableStateOf(false) }
    var comments by remember(post.id) { mutableStateOf<List<MediaRepository.Comment>>(emptyList()) }
    var commentText by remember(post.id) { mutableStateOf("") }
    var commentLoading by remember(post.id) { mutableStateOf(false) }
    var showReport by remember(post.id) { mutableStateOf(false) }
    var reportReason by remember(post.id) { mutableStateOf("") }
    var reportSending by remember(post.id) { mutableStateOf(false) }
    var reportMessage by remember(post.id) { mutableStateOf("") }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (post.authorAvatarUrl.isNotBlank()) {
                    AsyncImage(post.authorAvatarUrl, contentDescription = "প্রোফাইল ছবি",
                        modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Surface(Modifier.size(42.dp), shape = CircleShape, color = Color(0xFFEAF2FF)) {
                        Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Text("👤") }
                    }
                }
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { onOpenProfile(post.userId) }, contentPadding = PaddingValues(0.dp)) {
                        Text(post.authorName.ifBlank { "ব্যবহারকারী" }, fontWeight = FontWeight.Bold)
                    }
                    Text(post.createdAt.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text(when (post.mediaType) { "video" -> "ভিডিও"; "image" -> "ছবি"; else -> "লেখা পোস্ট" },
                style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            if (post.caption.isNotBlank()) Text(post.caption, style = MaterialTheme.typography.bodyLarge)
            if (post.mediaType == "video") {
                AndroidView(factory = { ctx ->
                    VideoView(ctx).apply { setMediaController(MediaController(ctx)); setVideoURI(Uri.parse(post.url)) }
                }, modifier = Modifier.fillMaxWidth().height(230.dp))
            } else if (post.mediaType == "image") {
                AsyncImage(model = post.url, contentDescription = "কমিউনিটি পোস্টের ছবি",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 420.dp),
                    contentScale = ContentScale.Crop)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$likeCount লাইক", style = MaterialTheme.typography.labelMedium)
                Text("${post.commentCount} মন্তব্য", style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = {
                    val auth = token
                    val uid = userId
                    if (auth == null || uid == null) onLogin() else scope.launch {
                        val next = !liked
                        val result = MediaRepository.setLiked(auth, post.id, uid, next)
                        if (result.isSuccess) {
                            liked = next
                            likeCount = (likeCount + if (next) 1 else -1).coerceAtLeast(0)
                            onChanged()
                        }
                    }
                }) { Text(if (liked) "♥ লাইক দিয়েছেন" else "♡ লাইক") }
                TextButton(onClick = {
                    showComments = !showComments
                    if (showComments) scope.launch {
                        commentLoading = true
                        comments = MediaRepository.getComments(post.id).getOrNull().orEmpty()
                        commentLoading = false
                    }
                }) { Text("💬 কমেন্ট") }
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, post.caption.ifBlank { "কাজের বাজারের একটি পোস্ট" } + "\n" + post.url)
                    }
                    context.startActivity(Intent.createChooser(share, "পোস্ট শেয়ার করুন"))
                }) { Text("↗ শেয়ার") }
            }
            TextButton(onClick = {
                if (token == null || userId == null) onLogin() else {
                    reportReason = ""
                    reportMessage = ""
                    showReport = true
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("⚑ পোস্ট রিপোর্ট করুন") }
            if (showComments) {
                HorizontalDivider()
                if (commentLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                comments.forEach { comment ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (comment.authorAvatarUrl.isNotBlank()) AsyncImage(comment.authorAvatarUrl, null,
                            modifier = Modifier.size(30.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f)) {
                            Text(comment.authorName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            Text(comment.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (token == null || userId == null) {
                    TextButton(onClick = onLogin) { Text("মন্তব্য করতে লগইন করুন") }
                } else {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(commentText, { commentText = it.take(2000) },
                            modifier = Modifier.weight(1f), singleLine = true, label = { Text("মন্তব্য লিখুন") })
                        Button(enabled = commentText.isNotBlank() && !commentLoading, onClick = {
                            val auth = token
                            val uid = userId
                            if (auth != null && uid != null) scope.launch {
                                commentLoading = true
                                val result = MediaRepository.addComment(auth, post.id, uid, commentText)
                                if (result.isSuccess) {
                                    commentText = ""
                                    comments = MediaRepository.getComments(post.id).getOrNull().orEmpty()
                                    onChanged()
                                }
                                commentLoading = false
                            }
                        }) { Text("পাঠান") }
                    }
                }
            }
        }
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = { if (!reportSending) showReport = false },
            title = { Text("পোস্ট রিপোর্ট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("নিরাপত্তা বা অনুপযুক্ত পোস্টের রিপোর্ট সাপোর্ট টিমের কাছে জমা হবে।")
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it.take(2000) },
                        label = { Text("রিপোর্টের কারণ") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (reportMessage.isNotBlank()) Text(reportMessage, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(enabled = !reportSending && reportReason.trim().length >= 10, onClick = {
                    val auth = token
                    val uid = userId
                    if (auth != null && uid != null) scope.launch {
                        reportSending = true
                        val reportBody = "পোস্ট ID: ${post.id}\nলেখক ID: ${post.userId}\nপোস্টের লেখা: ${post.caption.take(500)}\nরিপোর্টের কারণ: ${reportReason.trim()}"
                        val result = MarketplaceRepository.createSupportTicket(
                            auth, uid, "safety", "কমিউনিটি পোস্ট রিপোর্ট", reportBody
                        )
                        reportSending = false
                        if (result.isSuccess) {
                            showReport = false
                            reportReason = ""
                        } else reportMessage = result.exceptionOrNull()?.message ?: "রিপোর্ট জমা হয়নি।"
                    }
                }) { Text(if (reportSending) "পাঠানো হচ্ছে…" else "রিপোর্ট পাঠান") }
            },
            dismissButton = {
                TextButton(enabled = !reportSending, onClick = { showReport = false }) { Text("বাতিল") }
            }
        )
    }
}
