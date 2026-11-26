package com.kajerbazar.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Facebook-inspired social landing page for Kajer Bazar.
 * Navigation is delegated to the host so each destination keeps its own
 * existing authentication and data-loading rules.
 */
@Composable
fun SocialHomeScreen(
    displayName: String,
    onOpen: (String) -> Unit
) {
    val shortcuts = listOf(
        SocialShortcut("নিউজ ফিড", "সবার নতুন পোস্ট ও আপডেট", Icons.Default.Home, "media"),
        SocialShortcut("বন্ধু ও ফলো", "মানুষ ও কর্মী খুঁজুন", Icons.Default.Groups, "workers"),
        SocialShortcut("ভিডিও", "ছবি, ভিডিও ও রিলস", Icons.Default.VideoLibrary, "media"),
        SocialShortcut("মেসেজ", "ব্যক্তিগত কথোপকথন", Icons.Default.Chat, "messages"),
        SocialShortcut("নোটিফিকেশন", "আপডেট ও কার্যক্রম", Icons.Default.Notifications, "community"),
        SocialShortcut("কাজের বাজার", "কাজ খুঁজুন ও কাজ দিন", Icons.Default.Work, "jobs"),
        SocialShortcut("আমার প্রোফাইল", "আপনার পরিচিতি ও পোস্ট", Icons.Default.Person, "profile"),
        SocialShortcut("গ্রুপ ও কমিউনিটি", "কমিউনিটির সঙ্গে যুক্ত হোন", Icons.Default.Groups, "community"),
        SocialShortcut("সার্চ", "কাজ, মানুষ ও পোস্ট খুঁজুন", Icons.Default.Search, "workers"),
        SocialShortcut("সেটিংস", "অ্যাকাউন্ট ও গোপনীয়তা", Icons.Default.Settings, "settings")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("কাজের বাজার", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text("স্বাগতম, $displayName", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("মানুষের সঙ্গে যুক্ত হোন, কাজ শেয়ার করুন, সুযোগ খুঁজুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpen("media") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("আপনার মনে কী আছে?", fontWeight = FontWeight.Bold)
                        Text("লেখা, ছবি বা ভিডিও শেয়ার করুন",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onOpen("media") }) { Text("পোস্ট") }
                }
            }
        }
        item {
            Text("স্টোরি ও ভিডিও", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp)) {
                items(listOf("স্টোরি তৈরি", "ভিডিও দেখুন", "ছবি শেয়ার", "লাইভ কল")) { label ->
                    OutlinedCard(modifier = Modifier.width(116.dp).height(94.dp)
                        .clickable { onOpen("media") }) {
                        Column(Modifier.fillMaxSize().padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Text(label, style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            Text("আপনার জন্য", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }
        items(shortcuts) { shortcut ->
            SocialShortcutCard(shortcut = shortcut, onClick = { onOpen(shortcut.destination) })
        }
        item {
            Text(
                "নোট: স্টোরি, গ্রুপ, রিলস, বন্ধু অনুরোধ ও পুশ নোটিফিকেশনের পূর্ণ ব্যাকএন্ড ধাপে ধাপে যুক্ত ও পরীক্ষা করা হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class SocialShortcut(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destination: String
)

@Composable
private fun SocialShortcutCard(shortcut: SocialShortcut, onClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(shortcut.icon, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(shortcut.title, fontWeight = FontWeight.Bold)
                Text(shortcut.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
