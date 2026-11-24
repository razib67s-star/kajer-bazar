package com.kajerbazar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun MessagesScreen(token: String?, userId: String?, onLogin: () -> Unit) {
    val scope = rememberCoroutineScope()
    var people by remember { mutableStateOf<List<MessageRepository.Person>>(emptyList()) }
    var messages by remember { mutableStateOf<List<MessageRepository.Message>>(emptyList()) }
    var selectedPerson by remember { mutableStateOf<MessageRepository.Person?>(null) }
    var search by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun refreshInbox() {
        val auth = token
        val uid = userId
        if (auth == null || uid == null) return
        scope.launch {
            loading = true
            val directoryResult = MessageRepository.directory(auth, uid)
            val messageResult = MessageRepository.allMessages(auth, uid)
            people = directoryResult.getOrNull().orEmpty()
            messages = messageResult.getOrNull().orEmpty()
            error = directoryResult.exceptionOrNull()?.message
                ?: messageResult.exceptionOrNull()?.message.orEmpty()
            loading = false
        }
    }

    fun refreshThread(person: MessageRepository.Person) {
        val auth = token
        val uid = userId
        if (auth == null || uid == null) return
        scope.launch {
            loading = true
            val result = MessageRepository.thread(auth, uid, person.id)
            messages = MessageRepository.allMessages(auth, uid).getOrNull().orEmpty()
            error = result.exceptionOrNull()?.message.orEmpty()
            loading = false
            MessageRepository.markRead(auth, uid, person.id)
        }
    }

    LaunchedEffect(token, userId) {
        if (token != null && userId != null) refreshInbox()
    }

    if (token == null || userId == null) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ব্যক্তিগত মেসেজ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("বার্তা পাঠাতে ও পেতে লগইন করুন।")
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("লগইন করুন") }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selectedPerson == null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("মেসেজ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { refreshInbox() }) { Text("রিফ্রেশ") }
            }
            OutlinedTextField(search, { search = it.take(80) }, Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("নাম, জেলা বা উপজেলা দিয়ে খুঁজুন") })
            Text("সাম্প্রতিক কথোপকথন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val threads = MessageRepository.buildThreads(messages, people, userId)
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            threads.forEach { thread ->
                Card(onClick = {
                    selectedPerson = thread.person
                    refreshThread(thread.person)
                }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PersonAvatar(thread.person)
                        Column(Modifier.weight(1f)) {
                            Text(thread.person.name, fontWeight = FontWeight.Bold)
                            Text(thread.lastMessage.body, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                        if (thread.unreadCount > 0) {
                            Badge { Text(thread.unreadCount.toString()) }
                        }
                    }
                }
            }
            Text("নতুন মেসেজ শুরু করুন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val matches = people.filter {
                search.isBlank() || listOf(it.name, it.role, it.district, it.upazila)
                    .any { field -> field.contains(search.trim(), ignoreCase = true) }
            }.take(100)
            if (matches.isEmpty() && !loading) Text("কোনো ব্যবহারকারী পাওয়া যায়নি।")
            matches.forEach { person ->
                Card(onClick = {
                    selectedPerson = person
                    refreshThread(person)
                }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PersonAvatar(person)
                        Column(Modifier.weight(1f)) {
                            Text(person.name, fontWeight = FontWeight.SemiBold)
                            Text(buildString {
                                append(when (person.role) { "worker" -> "কর্মী"; "employer" -> "কাজদাতা"; "admin" -> "অ্যাডমিন"; else -> "ব্যবহারকারী" })
                                if (person.district.isNotBlank()) append(" • ${person.district}")
                                if (person.upazila.isNotBlank()) append(", ${person.upazila}")
                            }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text("মেসেজ", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        } else {
            val person = selectedPerson!!
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedPerson = null; refreshInbox() }) { Text("← ইনবক্স") }
                PersonAvatar(person)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(person.name, fontWeight = FontWeight.Bold)
                    Text("ব্যক্তিগত কথোপকথন", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { refreshThread(person) }) { Text("রিফ্রেশ") }
            }
            val thread = messages.filter {
                (it.senderId == userId && it.receiverId == person.id) ||
                    (it.senderId == person.id && it.receiverId == userId)
            }.sortedBy { it.createdAt }
            Column(Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 520.dp)
                .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (thread.isEmpty()) {
                    Text("এখনো কোনো বার্তা নেই। প্রথম বার্তাটি পাঠান।",
                        modifier = Modifier.fillMaxWidth().padding(24.dp), color = Color.Gray)
                }
                thread.forEach { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement =
                        if (message.senderId == userId) Arrangement.End else Arrangement.Start) {
                        Column(Modifier.widthIn(max = 300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (message.senderId == userId) Color(0xFFE4F0FF) else Color(0xFFF0F1F3))
                            .padding(horizontal = 13.dp, vertical = 9.dp)) {
                            Text(message.body)
                            Text(message.createdAt.take(16).replace("T", " "),
                                style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft, { draft = it.take(4000) }, Modifier.weight(1f),
                    placeholder = { Text("বার্তা লিখুন…") }, maxLines = 4)
                Button(enabled = draft.isNotBlank() && !loading, onClick = {
                    val text = draft
                    scope.launch {
                        loading = true
                        val result = MessageRepository.send(token, userId, person.id, text)
                        if (result.isSuccess) {
                            draft = ""
                            messages = MessageRepository.allMessages(token, userId).getOrNull().orEmpty()
                            MessageRepository.markRead(token, userId, person.id)
                            error = ""
                        } else error = result.exceptionOrNull()?.message ?: "বার্তা পাঠানো যায়নি।"
                        loading = false
                    }
                }) { Text("পাঠান") }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PersonAvatar(person: MessageRepository.Person) {
    if (person.avatarUrl.isNotBlank()) {
        AsyncImage(model = person.avatarUrl, contentDescription = "প্রোফাইল ছবি",
            modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color(0xFFEAF2FF)) {
            Box(contentAlignment = Alignment.Center) { Text("👤") }
        }
    }
}
