package com.kajerbazar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Blue = androidx.compose.ui.graphics.Color(0xFF145DA0)

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
    val categories = listOf("সব কাজ", "বাসার কাজ", "ডেলিভারি", "দোকানের কাজ", "অন্যান্য")
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Blue, secondary = Blue)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("কাজের বাজার", fontWeight = FontWeight.Bold)
                            Text("কাজ খুঁজুন, কাজ দিন", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Blue,
                        titleContentColor = androidx.compose.ui.graphics.Color.White
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            Text("আপনার কাজের বিজ্ঞাপন তৈরি করার সুবিধা শিগগিরই যোগ হবে।")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {}, enabled = false) { Text("কাজের বিজ্ঞাপন দিন") }
                        }
                    }
                }
                item { Text("সাম্প্রতিক কাজ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(listOf("দোকানের সহকারী প্রয়োজন", "বাসার জন্য পরিচ্ছন্নতাকর্মী", "পণ্য ডেলিভারি কর্মী")) { job ->
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(job, fontWeight = FontWeight.SemiBold)
                            Text("বেতন ও যোগাযোগের তথ্য শিগগিরই যুক্ত হবে।",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Text("নোট: এটি প্রাথমিক অ্যাপের নমুনা। লগইন, বিজ্ঞাপন প্রকাশ, পেমেন্ট ও কমিশনের ব্যবস্থা পরের ধাপে যুক্ত করতে হবে।",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
