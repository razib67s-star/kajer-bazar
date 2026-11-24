package com.kajerbazar.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SupportCenterScreen(token: String?, userId: String?, onLogin: () -> Unit) {
    val categories = listOf(
        "account" to "অ্যাকাউন্ট",
        "job" to "চাকরি/কাজ",
        "worker" to "কর্মী",
        "employer" to "নিয়োগকর্তা",
        "message" to "মেসেজ",
        "safety" to "নিরাপত্তা/প্রতারণা",
        "other" to "অন্যান্য"
    )
    var category by remember { mutableStateOf("other") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tickets by remember { mutableStateOf<List<MarketplaceRepository.SupportTicket>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token, userId) {
        if (token != null && userId != null) {
            loading = true
            val result = MarketplaceRepository.getSupportTickets(token)
            tickets = result.getOrNull().orEmpty()
            message = result.exceptionOrNull()?.message.orEmpty()
            loading = false
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("অভিযোগ ও সহায়তা", style = MaterialTheme.typography.titleLarge)
            Text("সমস্যা, ভুল তথ্য, প্রতারণার চেষ্টা বা অ্যাপ ব্যবহারের অসুবিধা আমাদের জানান।")
            if (token == null || userId == null) {
                Text("অভিযোগ পাঠাতে ও উত্তর দেখতে লগইন করুন।")
                Button(onClick = onLogin) { Text("লগইন করুন") }
            } else {
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("বিষয়: ${categories.first { it.first == category }.second}")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { item ->
                            DropdownMenuItem(text = { Text(item.second) }, onClick = { category = item.first; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = subject, onValueChange = { subject = it.take(120) }, label = { Text("সংক্ষিপ্ত বিষয়") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it.take(3000) }, label = { Text("সমস্যার বিস্তারিত লিখুন") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Button(enabled = !sending, onClick = {
                    sending = true
                    message = ""
                    scope.launch {
                        val result = MarketplaceRepository.createSupportTicket(token, userId, category, subject, body)
                        if (result.isSuccess) {
                            subject = ""; body = ""
                            tickets = MarketplaceRepository.getSupportTickets(token).getOrNull().orEmpty()
                            message = "আপনার অভিযোগ/সহায়তার অনুরোধ জমা হয়েছে।"
                        } else message = result.exceptionOrNull()?.message ?: "পাঠানো যায়নি।"
                        sending = false
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (sending) "পাঠানো হচ্ছে…" else "জমা দিন") }
                if (message.isNotBlank()) Text(message, color = if (message.contains("জমা হয়েছে")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                HorizontalDivider()
                Text("আপনার অনুরোধের অবস্থা", style = MaterialTheme.typography.titleMedium)
                if (loading) CircularProgressIndicator()
                if (!loading && tickets.isEmpty()) Text("এখনো কোনো অভিযোগ বা সহায়তার অনুরোধ নেই।")
                tickets.forEach { ticket ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ticket.subject, style = MaterialTheme.typography.titleSmall)
                        Text("অবস্থা: " + when (ticket.status) {
                            "open" -> "নতুন"
                            "in_review" -> "পর্যালোচনায়"
                            "resolved" -> "সমাধান হয়েছে"
                            "closed" -> "বন্ধ"
                            else -> ticket.status
                        }, style = MaterialTheme.typography.bodySmall)
                        Text(ticket.body, style = MaterialTheme.typography.bodySmall)
                        if (ticket.adminReply.isNotBlank()) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                Text("সহায়তা দলের উত্তর: ${ticket.adminReply}", modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
