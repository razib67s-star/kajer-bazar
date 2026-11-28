package com.kajerbazar.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun CommunityFeaturesScreen(token: String?, userId: String?, onLogin: () -> Unit) {
    val scope = rememberCoroutineScope()
    var notices by remember { mutableStateOf<List<MarketplaceRepository.CommunityNotification>>(emptyList()) }
    var message by remember { mutableStateOf("নতুন কমিউনিটি ফিচার ব্যবহার করুন।") }
    var busy by remember { mutableStateOf(false) }
    var targetUser by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var upazila by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var verificationDetails by remember { mutableStateOf("") }
    var reportType by remember { mutableStateOf("post") }
    var reportId by remember { mutableStateOf("") }
    var reportReason by remember { mutableStateOf("spam") }
    var reportDetails by remember { mutableStateOf("") }

    fun requireSession(action: suspend (String, String) -> Unit) {
        val t = token
        val u = userId
        if (t.isNullOrBlank() || u.isNullOrBlank()) { onLogin(); return }
        scope.launch {
            busy = true
            runCatching { action(t, u) }.onFailure { message = it.message ?: "কাজটি সম্পন্ন হয়নি।" }
            busy = false
        }
    }

    LaunchedEffect(token, userId) {
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            while (true) {
                MarketplaceRepository.getCommunityNotifications(token, userId).onSuccess { notices = it }
                    .onFailure { message = it.message ?: "নোটিফিকেশন লোড হয়নি।" }
                delay(30000L)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("কমিউনিটি ও নিরাপত্তা", style = MaterialTheme.typography.headlineSmall)
        Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("নোটিফিকেশন", style = MaterialTheme.typography.titleLarge)
                    val unread = notices.count { it.readAt == null }
                    if (unread > 0) Text("নতুন $unread", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge)
                }
                if (notices.isEmpty()) Text("এখনো কোনো নোটিফিকেশন নেই।")
                notices.forEach { n ->
                    Card(
                        onClick = {
                            val t = token
                            val u = userId
                            if (t != null && u != null && n.readAt == null) scope.launch {
                                MarketplaceRepository.markNotificationRead(t, u, n.id)
                                    .onSuccess {
                                        notices = notices.map { item ->
                                            if (item.id == n.id) item.copy(readAt = "read") else item
                                        }
                                    }.onFailure { message = it.message ?: "নোটিফিকেশন পঠিত করা যায়নি।" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor =
                            if (n.readAt == null) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(n.title, style = MaterialTheme.typography.titleMedium)
                            if (n.body.isNotBlank()) Text(n.body)
                            Text(if (n.readAt == null) "● নতুন • চাপলে পঠিত হবে" else "পঠিত",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
                            Text(n.createdAt.take(16).replace("T", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (token.isNullOrBlank() || userId.isNullOrBlank()) onLogin()
                        else scope.launch {
                            MarketplaceRepository.getCommunityNotifications(token, userId)
                                .onSuccess { notices = it }
                                .onFailure { message = it.message ?: "লোড হয়নি" }
                        }
                    }, enabled = !busy) { Text("রিফ্রেশ") }
                    OutlinedButton(onClick = {
                        val t = token
                        val u = userId
                        if (t == null || u == null) onLogin()
                        else scope.launch {
                            busy = true
                            MarketplaceRepository.markAllNotificationsRead(t, u)
                                .onSuccess {
                                    notices = notices.map { it.copy(readAt = it.readAt ?: "read") }
                                    message = "সব নোটিফিকেশন পঠিত হয়েছে।"
                                }.onFailure { message = it.message ?: "আপডেট হয়নি।" }
                            busy = false
                        }
                    }, enabled = !busy && notices.any { it.readAt == null }) { Text("সব পঠিত করুন") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("কাউকে ফলো করুন", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(targetUser, { targetUser = it }, label = { Text("ব্যবহারকারীর প্রোফাইল ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        requireSession { t, u ->
                            val result = MarketplaceRepository.followUser(t, u, targetUser.trim(), true)
                            result.onSuccess { message = "ফলো করা হয়েছে।" }.onFailure { message = it.message ?: "ফলো করা যায়নি।" }
                        }
                    }, enabled = !busy && targetUser.isNotBlank()) { Text("ফলো") }
                    OutlinedButton(onClick = {
                        requireSession { t, u ->
                            val result = MarketplaceRepository.followUser(t, u, targetUser.trim(), false)
                            result.onSuccess { message = "আনফলো করা হয়েছে।" }.onFailure { message = it.message ?: "আনফলো করা যায়নি।" }
                        }
                    }, enabled = !busy && targetUser.isNotBlank()) { Text("আনফলো") }
                }
                Text("প্রোফাইলের সঠিক ID ব্যবহার করুন।", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("চাকরির অ্যালার্ট তৈরি করুন", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(category, { category = it }, label = { Text("কাজের বিভাগ") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(district, { district = it }, label = { Text("জেলা") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(upazila, { upazila = it }, label = { Text("উপজেলা") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(keyword, { keyword = it }, label = { Text("কীওয়ার্ড") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    requireSession { t, u ->
                        val result = MarketplaceRepository.createJobAlert(t, u, category.trim(), district.trim(), upazila.trim(), keyword.trim())
                        result.onSuccess { message = "আপনার চাকরির অ্যালার্ট সংরক্ষণ হয়েছে।" }.onFailure { message = it.message ?: "অ্যালার্ট তৈরি হয়নি।" }
                    }
                }, enabled = !busy && listOf(category, district, upazila, keyword).any { it.isNotBlank() }) { Text("অ্যালার্ট সংরক্ষণ") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("কর্মী ভেরিফিকেশন", style = MaterialTheme.typography.titleLarge)
                Text("আপনার পরিচয়, কাজের অভিজ্ঞতা বা দক্ষতার প্রমাণ সম্পর্কে লিখুন।")
                OutlinedTextField(verificationDetails, { verificationDetails = it }, label = { Text("আবেদনের বিবরণ") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Button(onClick = {
                    requireSession { t, u ->
                        val result = MarketplaceRepository.submitVerification(t, u, verificationDetails.trim())
                        result.onSuccess { message = "ভেরিফিকেশন আবেদন জমা হয়েছে।" }.onFailure { message = it.message ?: "আবেদন জমা হয়নি।" }
                    }
                }, enabled = !busy && verificationDetails.trim().length >= 5) { Text("ভেরিফিকেশনের আবেদন") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("কনটেন্ট রিপোর্ট করুন", style = MaterialTheme.typography.titleLarge)
                Text("ধরন: post, comment, job, user অথবা message")
                OutlinedTextField(reportType, { reportType = it }, label = { Text("কনটেন্টের ধরন") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(reportId, { reportId = it }, label = { Text("কনটেন্ট ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(reportReason, { reportReason = it }, label = { Text("কারণ: spam/fraud/harassment/unsafe/other") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(reportDetails, { reportDetails = it }, label = { Text("বিস্তারিত (ঐচ্ছিক)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(onClick = {
                    requireSession { t, u ->
                        val result = MarketplaceRepository.submitContentReport(t, u, reportType.trim(), reportId.trim(), reportReason.trim(), reportDetails.trim())
                        result.onSuccess { message = "রিপোর্ট জমা হয়েছে। আমাদের টিম পর্যালোচনা করবে।" }.onFailure { message = it.message ?: "রিপোর্ট জমা হয়নি।" }
                    }
                }, enabled = !busy && reportId.isNotBlank()) { Text("রিপোর্ট জমা দিন") }
            }
        }
    }
}
