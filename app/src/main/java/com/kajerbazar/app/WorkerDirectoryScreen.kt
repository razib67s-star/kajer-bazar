package com.kajerbazar.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun WorkerDirectoryScreen(
    token: String?,
    userId: String?,
    isEmployer: Boolean,
    onLogin: () -> Unit
) {
    var workers by remember { mutableStateOf<List<MarketplaceRepository.DirectoryWorker>>(emptyList()) }
    var savedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var search by remember { mutableStateOf("") }
    var showSavedOnly by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token, userId, isEmployer) {
        if (token != null && userId != null && isEmployer) {
            loading = true
            val directory = MarketplaceRepository.getWorkerDirectory(token)
            workers = directory.getOrNull().orEmpty()
            val saved = MarketplaceRepository.getSavedWorkerIds(token)
            savedIds = saved.getOrNull().orEmpty()
            message = directory.exceptionOrNull()?.message ?: saved.exceptionOrNull()?.message.orEmpty()
            loading = false
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("কর্মী খুঁজুন", style = MaterialTheme.typography.titleLarge)
            Text("জেলা ও উপজেলা অনুযায়ী সক্রিয় কর্মী দেখুন এবং পছন্দের কর্মী সংরক্ষণ করুন।")
            if (token == null || userId == null) {
                Text("কর্মী খুঁজতে ও সংরক্ষণ করতে লগইন করুন।")
                Button(onClick = onLogin) { Text("লগইন করুন") }
            } else if (!isEmployer) {
                Text("কর্মী সংরক্ষণ করতে কাজদাতা অ্যাকাউন্ট প্রয়োজন।")
            } else {
                OutlinedTextField(value = search, onValueChange = { search = it.take(100) }, label = { Text("নাম, জেলা বা উপজেলা দিয়ে খুঁজুন") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = !showSavedOnly, onClick = { showSavedOnly = false }, label = { Text("সব কর্মী") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = showSavedOnly, onClick = { showSavedOnly = true }, label = { Text("সংরক্ষিত কর্মী (${savedIds.size})") })
                }
                if (loading) CircularProgressIndicator()
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
                val visible = workers.filter {
                    (!showSavedOnly || it.id in savedIds) &&
                        listOf(it.fullName, it.district, it.upazila).any { value -> value.contains(search.trim(), ignoreCase = true) }
                }
                if (!loading && visible.isEmpty()) Text(if (showSavedOnly) "এখনো কোনো কর্মী সংরক্ষণ করা হয়নি।" else "এখনো কোনো সক্রিয় কর্মী পাওয়া যায়নি।")
                visible.forEach { worker ->
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (worker.avatarUrl.isNotBlank()) {
                            AsyncImage(model = worker.avatarUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape))
                        } else {
                            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Text(worker.fullName.take(1).ifBlank { "ক" }) }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(worker.fullName.ifBlank { "নাম দেওয়া হয়নি" }, style = MaterialTheme.typography.titleMedium)
                            Text(listOf(worker.district, worker.upazila).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "এলাকা দেওয়া হয়নি" }, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(enabled = !loading, onClick = {
                            val currentToken = token
                            val currentUser = userId
                            if (currentToken != null && currentUser != null) scope.launch {
                                val wasSaved = worker.id in savedIds
                                val result = MarketplaceRepository.setWorkerSaved(currentToken, currentUser, worker.id, !wasSaved)
                                if (result.isSuccess) savedIds = if (wasSaved) savedIds - worker.id else savedIds + worker.id
                                else message = result.exceptionOrNull()?.message ?: "পরিবর্তন করা যায়নি।"
                            }
                        }) { Text(if (worker.id in savedIds) "সংরক্ষিত ✓" else "সংরক্ষণ") }
                    }
                }
            }
        }
    }
}
