package com.kajerbazar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun PeopleDirectoryScreen(
    token: String?,
    userId: String?,
    onLogin: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var people by remember { mutableStateOf<List<MessageRepository.Person>>(emptyList()) }
    var followingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("ব্যবহারকারী খুঁজুন এবং পছন্দের মানুষকে ফলো করুন।") }

    fun refresh() {
        val auth = token
        val uid = userId
        if (auth.isNullOrBlank() || uid.isNullOrBlank()) {
            status = "ব্যবহারকারীদের দেখতে লগইন করুন।"
            return
        }
        scope.launch {
            loading = true
            val directoryResult = MessageRepository.directory(auth, uid)
            val followResult = MessageRepository.getFollowingIds(auth, uid)
            if (directoryResult.isSuccess) people = directoryResult.getOrNull().orEmpty()
            if (followResult.isSuccess) followingIds = followResult.getOrNull().orEmpty()
            status = when {
                directoryResult.isFailure -> directoryResult.exceptionOrNull()?.message ?: "ব্যবহারকারী তালিকা লোড হয়নি।"
                followResult.isFailure -> followResult.exceptionOrNull()?.message ?: "ফলো তালিকা লোড হয়নি।"
                else -> "মোট ${people.size} জন ব্যবহারকারী"
            }
            loading = false
        }
    }

    LaunchedEffect(token, userId) { refresh() }

    if (token.isNullOrBlank() || userId.isNullOrBlank()) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("মানুষ খুঁজুন", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("ব্যবহারকারী খুঁজতে, প্রোফাইল দেখতে এবং ফলো করতে লগইন করুন।")
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("লগইন করুন") }
            }
        }
        return
    }

    val filtered = people.filter { person ->
        search.isBlank() || listOf(person.name, person.role, person.district, person.upazila)
            .any { it.contains(search.trim(), ignoreCase = true) }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("মানুষ খুঁজুন", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            TextButton(onClick = { refresh() }, enabled = !loading) { Text("রিফ্রেশ") }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("নাম, ভূমিকা, জেলা বা উপজেলা দিয়ে খুঁজুন") }
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading && filtered.isEmpty()) {
            Text(if (people.isEmpty()) "এখনো কোনো ব্যবহারকারী পাওয়া যায়নি।" else "আপনার অনুসন্ধানের সঙ্গে মেলে এমন কাউকে পাওয়া যায়নি।")
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { person ->
                val isFollowing = person.id in followingIds
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (person.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = person.avatarUrl,
                                contentDescription = "প্রোফাইল ছবি",
                                modifier = Modifier.size(52.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(Modifier.size(52.dp), shape = CircleShape, color = Color(0xFFEAF2FF)) {
                                Box(contentAlignment = Alignment.Center) { Text("👤") }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(person.name.ifBlank { "ব্যবহারকারী" }, fontWeight = FontWeight.Bold)
                            Text(
                                buildString {
                                    append(when (person.role) {
                                        "worker" -> "কর্মী"
                                        "employer" -> "কাজদাতা"
                                        "admin" -> "অ্যাডমিন"
                                        else -> "ব্যবহারকারী"
                                    })
                                    val place = listOf(person.district, person.upazila).filter { it.isNotBlank() }.joinToString(", ")
                                    if (place.isNotBlank()) append(" • $place")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { onOpenProfile(person.id) },
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("প্রোফাইল ও পোস্ট দেখুন") }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                enabled = busyId != person.id,
                                onClick = {
                                    val auth = token
                                    val uid = userId
                                    if (auth == null || uid == null) {
                                        onLogin()
                                    } else {
                                        scope.launch {
                                            busyId = person.id
                                            val result = MarketplaceRepository.followUser(auth, uid, person.id, !isFollowing)
                                            if (result.isSuccess) {
                                                followingIds = if (isFollowing) followingIds - person.id else followingIds + person.id
                                                status = if (isFollowing) "ফলো করা বন্ধ হয়েছে।" else "আপনি ${person.name} কে ফলো করছেন।"
                                            } else {
                                                status = result.exceptionOrNull()?.message ?: "ফলো তালিকা হালনাগাদ হয়নি।"
                                            }
                                            busyId = null
                                        }
                                    }
                                }
                            ) {
                                Text(when {
                                    busyId == person.id -> "অপেক্ষা"
                                    isFollowing -> "ফলো করছেন"
                                    else -> "ফলো"
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
