package com.kajerbazar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Blue = Color(0xFF145DA0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KajerBazarApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KajerBazarApp() {
    var selected by remember { mutableStateOf("সব কাজ") }
    var showLogin by remember { mutableStateOf(false) }
    val categories = listOf("সব কাজ", "বাসার কাজ", "ডেলিভারি", "দোকানের কাজ", "অন্যান্য")

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Blue)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("কাজের বাজার", fontWeight = FontWeight.Bold)
                            Text("কাজ খুঁজুন, কাজ দিন", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        TextButton(onClick = { showLogin = true }) {
                            Text("লগইন", color = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Blue,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Column(Modifier.padding(14.dp)) {
                            Text("নিরাপত্তা সতর্কতা", fontWeight = FontWeight.Bold, color = Color(0xFF8A4B08))
                            Spacer(Modifier.height(4.dp))
                            Text("এটি এখন পরীক্ষামূলক নমুনা অ্যাপ। এখানে এখনো আসল চাকরি, লগইন বা নিয়োগদাতা যাচাই চালু হয়নি। কাউকে OTP, পাসওয়ার্ড, ব্যাংক তথ্য বা অগ্রিম টাকা দেবেন না।")
                        }
                    }
                }
                item {
                    Text("আপনার পছন্দের কাজ খুঁজুন", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("কাজ বা এলাকার নাম লিখুন") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("কাজের ধরন", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.take(3).forEach { category ->
                            FilterChip(
                                selected = selected == category,
                                onClick = { selected = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Blue.copy(alpha = 0.08f))) {
                        Column(Modifier.padding(16.dp)) {
                            Text("আপনি কি কাজ দিতে চান?", fontWeight = FontWeight.Bold)
                            Text("কাজের বিজ্ঞাপন প্রকাশের সুবিধা এখনো চালু হয়নি।")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {}, enabled = false) { Text("কাজের বিজ্ঞাপন দিন") }
                        }
                    }
                }
                item { Text("সাম্প্রতিক নমুনা কাজ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(listOf("দোকানের সহকারী প্রয়োজন", "বাসার জন্য পরিচ্ছন্নতাকর্মী", "পণ্য ডেলিভারি কর্মী")) { job ->
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(job, fontWeight = FontWeight.SemiBold)
                            Text("এগুলো শুধু নমুনা বিজ্ঞাপন; বাস্তব চাকরির অফার নয়।",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Text("নিরাপত্তা: যাচাই না করা চাকরির প্রস্তাবে টাকা পাঠাবেন না। সন্দেহ হলে ব্যক্তিগত তথ্য দেবেন না।",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (showLogin) {
            AlertDialog(
                onDismissRequest = { showLogin = false },
                title = { Text("লগইন এখনো চালু হয়নি") },
                text = {
                    Text("আপনার মোবাইল নম্বর বা Gmail এখানে লিখবেন না। নিরাপদ OTP ও Google যাচাইকরণ ব্যবস্থা তৈরি ও পরীক্ষা না হওয়া পর্যন্ত অ্যাকাউন্ট খোলা যাবে না।")
                },
                confirmButton = {
                    TextButton(onClick = { showLogin = false }) { Text("বুঝেছি") }
                }
            )
        }
    }
}
