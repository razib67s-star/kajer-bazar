package com.kajerbazar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
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
fun WorkerProfileScreen(
    profile: MarketplaceRepository.Profile,
    profileId: String?,
    viewerId: String?,
    token: String?,
    isOwnProfile: Boolean,
    onEdit: () -> Unit,
    onLogin: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onChangeCoverPhoto: () -> Unit = {},
    coverUrl: String = profile.coverUrl
) {
    var selectedTab by remember(profileId) { mutableStateOf("পোস্ট") }
    val area = listOf(profile.upazila, profile.district).filter { it.isNotBlank() }.joinToString(", ")
    val displayName = profile.fullName.ifBlank { "নাম যোগ করা হয়নি" }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(190.dp).background(
                        Brush.linearGradient(listOf(Color(0xFF0B5B50), Color(0xFF168B78), Color(0xFFB8E5D8)))
                    )
                ) {
                    if (coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = "কর্মীর কভার ফটো",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(Modifier.fillMaxSize().background(Color(0x55000000)))
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("কাজের বাজার", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("কর্মী পরিচিতি", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    if (isOwnProfile) {
                        FilledTonalButton(
                            onClick = onChangeCoverPhoto,
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xDDFFFFFF), contentColor = Color(0xFF173F37))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("কভার ফটো পরিবর্তন")
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.padding(top = 0.dp).size(108.dp).offset(y = (-24).dp)
                                .clip(CircleShape).background(Color.White).padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.avatarUrl.isNotBlank()) {
                                AsyncImage(profile.avatarUrl, contentDescription = "কর্মীর প্রোফাইল ছবি",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color(0xFFE7F2EF)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color(0xFF126B5A))
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("কর্মী · কাজপ্রার্থী", color = Color(0xFF126B5A), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (area.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Text(area, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        listOf(profile.workType, profile.skills).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "আপনার কাজের দক্ষতা ও পরিচিতি" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    ProfileFollowCard(token = token, viewerId = viewerId, profileId = profileId, onLogin = onLogin)
                    if (isOwnProfile) {
                        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("প্রোফাইল সম্পাদনা করুন")
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("পোস্ট", "সম্পর্কে", "কাজের নমুনা").forEach { tab ->
                            val active = selectedTab == tab
                            TextButton(
                                onClick = { selectedTab = tab },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(contentColor = if (active) Color(0xFF126B5A) else MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text(tab, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(2.dp))
                }
            }
        }

        when (selectedTab) {
            "সম্পর্কে" -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ব্যক্তিগত ও কাজের তথ্য", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    WorkerInfo("কাজের ধরন", profile.workType)
                    WorkerInfo("দক্ষতা", profile.skills)
                    WorkerInfo("অভিজ্ঞতা", profile.experience)
                    WorkerInfo("কাজের সময়", profile.availability)
                    WorkerInfo("প্রত্যাশিত মজুরি", profile.expectedWage.ifBlank { "আলোচনা সাপেক্ষ" })
                    WorkerInfo("জেলা", profile.district)
                    WorkerInfo("উপজেলা", profile.upazila)
                    WorkerInfo("বর্তমান ঠিকানা", profile.currentAddress)
                    WorkerInfo("স্থায়ী ঠিকানা", profile.permanentAddress)
                    WorkerInfo("যোগাযোগ", profile.contactPhone)
                }
            }
            "কাজের নমুনা" -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("কাজের নমুনা ও পোর্টফোলিও", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(profile.portfolio.ifBlank { "এখনো কোনো কাজের নমুনা যোগ করা হয়নি।" })
                    WorkerInfo("আগের কাজের অভিজ্ঞতা", profile.experience)
                }
            }
            else -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (isOwnProfile) "আপনার পোস্ট" else "কর্মীর পোস্ট", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("ছবি, ভিডিও ও কাজের আপডেট এখানে দেখা যাবে।", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                MediaPostsSection(token = token, userId = viewerId, onLogin = onLogin, profileUserId = profileId, onOpenProfile = onOpenProfile)
            }
        }
    }
}

@Composable
private fun WorkerInfo(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "তথ্য যোগ করা হয়নি" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}


@Composable
fun PublicWorkerProfileLoader(
    token: String?,
    viewerId: String?,
    profileId: String?,
    onLogin: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onEditOwnProfile: () -> Unit = {},
    onChangeOwnCoverPhoto: () -> Unit = {}
) {
    var profile by remember(profileId) { mutableStateOf<MarketplaceRepository.Profile?>(null) }
    var loading by remember(profileId) { mutableStateOf(true) }
    var error by remember(profileId) { mutableStateOf("") }
    LaunchedEffect(token, profileId) {
        if (token.isNullOrBlank() || profileId.isNullOrBlank()) {
            loading = false
            error = "প্রোফাইল দেখতে লগইন করুন।"
        } else {
            loading = true
            val result = MarketplaceRepository.getProfile(token, profileId)
            profile = result.getOrNull()
            error = result.exceptionOrNull()?.message.orEmpty()
            loading = false
        }
    }
    when {
        loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        profile == null -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("প্রোফাইল লোড হয়নি", fontWeight = FontWeight.Bold)
                Text(error.ifBlank { "এই প্রোফাইলটি পাওয়া যায়নি।" })
                Button(onClick = onLogin) { Text("লগইন করুন") }
            }
        }
        profile?.role == "worker" -> WorkerProfileScreen(
            profile = profile!!,
            profileId = profileId,
            viewerId = viewerId,
            token = token,
            isOwnProfile = profileId == viewerId,
            onEdit = onEditOwnProfile,
            onLogin = onLogin,
            onOpenProfile = onOpenProfile,
            onChangeCoverPhoto = onChangeOwnCoverPhoto
        )
        else -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(profile?.fullName.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("এই প্রোফাইলটি কর্মী প্রোফাইল নয়।")
            }
        }
    }
}
