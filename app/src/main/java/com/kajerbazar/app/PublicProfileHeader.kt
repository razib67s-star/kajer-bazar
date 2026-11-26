package com.kajerbazar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun PublicProfileHeader(token: String?, userId: String?, profileUserId: String) {
    val scope = rememberCoroutineScope()
    var person by remember(profileUserId) { mutableStateOf<MessageRepository.Person?>(null) }
    var following by remember(profileUserId) { mutableStateOf(false) }
    var followers by remember(profileUserId) { mutableStateOf(0L) }
    var followingCount by remember(profileUserId) { mutableStateOf(0L) }
    var busy by remember(profileUserId) { mutableStateOf(false) }
    var status by remember(profileUserId) { mutableStateOf("প্রোফাইল লোড হচ্ছে…") }

    LaunchedEffect(token, userId, profileUserId) {
        val auth = token
        val uid = userId
        if (auth == null || uid == null) status = "প্রোফাইল দেখতে লগইন করুন।"
        else {
            busy = true
            val directory = MessageRepository.directory(auth, uid)
            person = directory.getOrNull()?.firstOrNull { it.id == profileUserId }
            val follows = MessageRepository.getFollowingIds(auth, uid)
            following = profileUserId in follows.getOrNull().orEmpty()
            MarketplaceRepository.getFollowCounts(auth, profileUserId).getOrNull()?.let {
                followers = it.first
                followingCount = it.second
            }
            status = if (person == null) directory.exceptionOrNull()?.message ?: "এই প্রোফাইল পাওয়া যায়নি।" else ""
            busy = false
        }
    }

    val profile = person
    if (profile == null) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth()) else Text(status, style = MaterialTheme.typography.bodyMedium)
        return
    }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(176.dp).background(
                Brush.linearGradient(listOf(Color(0xFF075B48), Color(0xFF239B78), Color(0xFFBCE8D8)))
            )) {
                if (profile.coverUrl.isNotBlank()) {
                    AsyncImage(model = profile.coverUrl, contentDescription = "কভার ছবি",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.24f)))
                }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (profile.avatarUrl.isNotBlank()) {
                        AsyncImage(model = profile.avatarUrl, contentDescription = "প্রোফাইল ছবি",
                            modifier = Modifier.size(78.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Surface(Modifier.size(78.dp), shape = CircleShape, color = Color(0xFFEAF2FF)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color(0xFF145DA0), modifier = Modifier.size(42.dp)) }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(profile.name.ifBlank { "ব্যবহারকারী" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(when(profile.role) { "worker" -> "কর্মী / কাজপ্রার্থী"; "employer" -> "কাজদাতা"; "admin" -> "অ্যাডমিন"; else -> "ব্যবহারকারী" },
                            color = Color(0xFF145DA0), fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(listOf(profile.district, profile.upazila).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "এলাকা যোগ করা হয়নি" },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column { Text("$followers", fontWeight = FontWeight.Bold); Text("ফলোয়ার", style = MaterialTheme.typography.labelSmall) }
                    Column { Text("$followingCount", fontWeight = FontWeight.Bold); Text("ফলো করছেন", style = MaterialTheme.typography.labelSmall) }
                }
                if (profile.workType.isNotBlank()) Text("কাজের ধরন: ${profile.workType}")
                if (profile.skills.isNotBlank()) Text("দক্ষতা: ${profile.skills}")
                if (profile.experience.isNotBlank()) Text("অভিজ্ঞতা: ${profile.experience}")
                if (profile.availability.isNotBlank()) Text("কাজের সময়: ${profile.availability}")
                if (profile.expectedWage.isNotBlank()) Text("প্রত্যাশিত মজুরি: ${profile.expectedWage}")
                if (userId != profileUserId) {
                    Button(enabled = !busy && token != null && userId != null, onClick = {
                        val auth = token
                        val uid = userId
                        if (auth != null && uid != null) scope.launch {
                            busy = true
                            val result = MarketplaceRepository.followUser(auth, uid, profileUserId, !following)
                            if (result.isSuccess) {
                                following = !following
                                followers = (followers + if (following) 1 else -1).coerceAtLeast(0)
                            } else status = result.exceptionOrNull()?.message ?: "ফলো আপডেট হয়নি।"
                            busy = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (following) "ফলো করছেন" else "ফলো করুন") }
                }
                if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
