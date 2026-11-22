package com.kajerbazar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var connectionStatus by remember { mutableStateOf("Supabase সংযোগ পরীক্ষা হচ্ছে…") }
    LaunchedEffect(Unit) { connectionStatus = withContext(Dispatchers.IO) { SupabaseConnection.checkApi() } }
    var showLogin by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var pendingPhone by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf("") }
    var signedInPhone by remember { mutableStateOf<String?>(null) }
    var accessToken by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<MarketplaceRepository.Profile?>(null) }
    var jobs by remember { mutableStateOf<List<MarketplaceRepository.Job>>(emptyList()) }
    var jobMessage by remember { mutableStateOf("কাজ দেখতে OTP দিয়ে লগইন করুন।") }
    var showEmployerForm by remember { mutableStateOf(false) }
    var showJobForm by remember { mutableStateOf(false) }
    var businessName by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var employerDistrict by remember { mutableStateOf("") }
    var employerUpazila by remember { mutableStateOf("") }
    var employerDetails by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var jobCategory by remember { mutableStateOf("") }
    var jobDistrict by remember { mutableStateOf("") }
    var jobUpazila by remember { mutableStateOf("") }
    var jobWage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Blue)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column {
                        Text("কাজের বাজার", fontWeight = FontWeight.Bold)
                        Text("কাজ খুঁজুন, কাজ দিন", style = MaterialTheme.typography.labelSmall)
                    } },
                    actions = {
                        TextButton(onClick = { showLogin = true }) {
                            Text(if (signedInPhone == null) "লগইন" else "অ্যাকাউন্ট", color = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue, titleContentColor = Color.White)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Card { Text(connectionStatus, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) } }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Column(Modifier.padding(14.dp)) {
                            Text("নিরাপদ পরীক্ষামূলক সংস্করণ", fontWeight = FontWeight.Bold, color = Color(0xFF8A4B08))
                            Text("OTP পাঠাতে Supabase Phone Auth ও SMS Provider সক্রিয় থাকতে হবে।")
                            Text("বাস্তব কাজের বিজ্ঞাপন প্রকাশের আগে নিয়োগদাতার অনুমোদন প্রয়োজন।")
                        }
                    }
                }
                item {
                    Text("বাংলাদেশের কাজের বাজার", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("জেলা ও উপজেলা অনুযায়ী প্রকাশিত কাজ দেখুন।")
                    Button(onClick = { showLogin = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (signedInPhone == null) "মোবাইল নম্বর দিয়ে শুরু করুন" else "অ্যাকাউন্ট দেখুন")
                    }
                }
                if (profile != null) {
                    item {
                        Card {
                            Column(Modifier.padding(14.dp)) {
                                Text("আপনার প্রোফাইল", fontWeight = FontWeight.Bold)
                                Text("নাম: ${profile?.fullName?.ifBlank { "নাম যোগ করা হয়নি" }}")
                                Text("অ্যাকাউন্ট: ${if (profile?.role == "employer") "কাজদাতা" else "কর্মী"}")
                                Text("এলাকা: ${listOfNotNull(profile?.district, profile?.upazila).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "যোগ করা হয়নি" }}")
                                if (profile?.role == "worker") {
                                    Button(onClick = { showEmployerForm = true }, modifier = Modifier.fillMaxWidth()) { Text("কাজদাতা হতে আবেদন করুন") }
                                }
                                if (profile?.role == "employer") {
                                    Button(onClick = { showJobForm = true }, modifier = Modifier.fillMaxWidth()) { Text("নতুন কাজ পোস্ট করুন") }
                                }
                            }
                        }
                    }
                }
                item { Text("প্রকাশিত কাজ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                if (jobs.isEmpty()) {
                    item { Text(jobMessage, style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(jobs, key = { it.id }) { job ->
                        Card {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(job.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${job.category} • ${job.district}${if (job.upazila.isNotBlank()) ", ${job.upazila}" else ""}")
                                Text(job.description, maxLines = 4)
                                Text("কর্মীর মজুরি: ৳${job.wagePaisa / 100}", color = Blue, fontWeight = FontWeight.Bold)
                                if (profile?.role == "worker") {
                                    Button(onClick = {
                                        val token = accessToken
                                        if (token != null) scope.launch {
                                            val result = MarketplaceRepository.applyToJob(token, job.id)
                                            jobMessage = if (result.isSuccess) "আবেদন জমা হয়েছে।" else result.exceptionOrNull()?.message ?: "আবেদন করা যায়নি।"
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) { Text("এই কাজে আবেদন করুন") }
                                }
                            }
                        }
                    }
                }
                item { Text(jobMessage, style = MaterialTheme.typography.bodySmall) }
                item { PaymentScreen() }
                item {
                    Text("নিরাপত্তা: কাউকে OTP, পাসওয়ার্ড, ব্যাংক তথ্য বা অগ্রিম টাকা দেবেন না। কাজের শর্ত যাচাই করুন।",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (showLogin) {
            AlertDialog(
                onDismissRequest = { if (!busy) showLogin = false },
                title = { Text(if (signedInPhone != null) "লগইন হয়েছে" else "মোবাইল OTP লগইন") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (signedInPhone != null) {
                            Text("যাচাইকৃত নম্বর: $signedInPhone")
                            Text("এই সেশনে অ্যাকাউন্ট সক্রিয়। নিরাপত্তার জন্য অ্যাপ বন্ধ করলে আবার লগইন করতে হবে।")
                        } else {
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, enabled = !otpSent && !busy,
                                label = { Text("মোবাইল নম্বর") }, placeholder = { Text("01XXXXXXXXX") }, singleLine = true)
                            if (otpSent) OutlinedTextField(value = otp, onValueChange = { otp = it.filter(Char::isDigit).take(8) },
                                enabled = !busy, label = { Text("SMS OTP কোড") }, singleLine = true)
                        }
                        if (authMessage.isNotBlank()) Text(authMessage,
                            color = if (authMessage.contains("সম্পন্ন")) Color(0xFF18733B) else MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    if (signedInPhone != null) {
                        TextButton(onClick = {
                            signedInPhone = null; accessToken = null; userId = null; profile = null; jobs = emptyList()
                            jobMessage = "কাজ দেখতে OTP দিয়ে লগইন করুন।"; showLogin = false
                        }) { Text("লগআউট") }
                    } else if (!otpSent) {
                        TextButton(enabled = !busy, onClick = {
                            busy = true; authMessage = ""
                            scope.launch {
                                val result = AuthRepository.sendPhoneOtp(phone)
                                busy = false; authMessage = result.message
                                if (result.success) { pendingPhone = result.phone.orEmpty(); otpSent = true }
                            }
                        }) { Text(if (busy) "অপেক্ষা করুন…" else "OTP পাঠান") }
                    } else {
                        TextButton(enabled = !busy, onClick = {
                            busy = true; authMessage = ""
                            scope.launch {
                                val result = AuthRepository.verifyPhoneOtp(pendingPhone, otp)
                                busy = false; authMessage = result.message
                                if (result.success) {
                                    signedInPhone = result.phone; accessToken = result.accessToken; userId = result.userId
                                    otpSent = false; otp = ""; showLogin = false
                                    val token = result.accessToken.orEmpty()
                                    val id = result.userId.orEmpty()
                                    val profileResult = MarketplaceRepository.getProfile(token, id)
                                    profile = profileResult.getOrNull()
                                    val jobResult = MarketplaceRepository.getPublishedJobs(token)
                                    jobs = jobResult.getOrNull().orEmpty()
                                    jobMessage = if (jobResult.isSuccess && jobs.isEmpty()) "এখনো কোনো কাজ প্রকাশিত হয়নি।" else jobResult.exceptionOrNull()?.message ?: "কাজের তালিকা হালনাগাদ হয়েছে।"
                                }
                            }
                        }) { Text(if (busy) "যাচাই হচ্ছে…" else "OTP যাচাই করুন") }
                    }
                },
                dismissButton = {
                    TextButton(enabled = !busy, onClick = {
                        if (otpSent && signedInPhone == null) { otpSent = false; otp = ""; authMessage = "" }
                        else showLogin = false
                    }) { Text(if (otpSent && signedInPhone == null) "নম্বর বদলান" else "বন্ধ") }
                }
            )
        }

        if (showEmployerForm) {
            AlertDialog(
                onDismissRequest = { if (!busy) showEmployerForm = false },
                title = { Text("কাজদাতা হওয়ার আবেদন") },
                text = {
                    Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("নিরাপত্তার জন্য অ্যাডমিন যাচাইয়ের পর কাজদাতা সুবিধা চালু হবে।")
                        OutlinedTextField(businessName, { businessName = it }, label = { Text("প্রতিষ্ঠান/কাজদাতার নাম") }, singleLine = true)
                        OutlinedTextField(contactName, { contactName = it }, label = { Text("যোগাযোগকারীর নাম") }, singleLine = true)
                        OutlinedTextField(employerDistrict, { employerDistrict = it }, label = { Text("জেলা") }, singleLine = true)
                        OutlinedTextField(employerUpazila, { employerUpazila = it }, label = { Text("উপজেলা") }, singleLine = true)
                        OutlinedTextField(employerDetails, { employerDetails = it }, label = { Text("অতিরিক্ত তথ্য (ঐচ্ছিক)") })
                        if (authMessage.isNotBlank()) Text(authMessage)
                    }
                },
                confirmButton = {
                    TextButton(enabled = !busy, onClick = {
                        val token = accessToken
                        if (token == null) { authMessage = "আবার লগইন করুন।" } else {
                            busy = true
                            scope.launch {
                                val result = MarketplaceRepository.submitEmployerRequest(token, businessName, contactName, employerDistrict, employerUpazila, employerDetails)
                                busy = false
                                authMessage = if (result.isSuccess) "আবেদন জমা হয়েছে। অ্যাডমিন যাচাইয়ের পর সিদ্ধান্ত জানানো হবে।" else result.exceptionOrNull()?.message ?: "আবেদন করা যায়নি।"
                                if (result.isSuccess) showEmployerForm = false
                            }
                        }
                    }) { Text(if (busy) "জমা হচ্ছে…" else "আবেদন জমা দিন") }
                },
                dismissButton = { TextButton(enabled = !busy, onClick = { showEmployerForm = false }) { Text("বন্ধ") } }
            )
        }

        if (showJobForm) {
            AlertDialog(
                onDismissRequest = { if (!busy) showJobForm = false },
                title = { Text("নতুন কাজ পোস্ট করুন") },
                text = {
                    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("জমা দিলে কাজটি সরাসরি প্রকাশ হবে না; অ্যাডমিন পর্যালোচনায় যাবে।")
                        OutlinedTextField(jobTitle, { jobTitle = it }, label = { Text("কাজের শিরোনাম") }, singleLine = true)
                        OutlinedTextField(jobDescription, { jobDescription = it }, label = { Text("কাজের বিস্তারিত (কমপক্ষে ২০ অক্ষর)") })
                        OutlinedTextField(jobCategory, { jobCategory = it }, label = { Text("কাজের বিভাগ") }, singleLine = true)
                        OutlinedTextField(jobDistrict, { jobDistrict = it }, label = { Text("জেলা") }, singleLine = true)
                        OutlinedTextField(jobUpazila, { jobUpazila = it }, label = { Text("উপজেলা") }, singleLine = true)
                        OutlinedTextField(jobWage, { jobWage = it.filter(Char::isDigit) }, label = { Text("কর্মীর মজুরি (টাকা)") }, singleLine = true)
                        Text("কর্মী পুরো মজুরি পাবেন। নিয়োগদাতা পরে এর অতিরিক্ত ১০% কমিশন দেবেন।")
                    }
                },
                confirmButton = {
                    TextButton(enabled = !busy, onClick = {
                        val token = accessToken
                        val wage = jobWage.toLongOrNull()
                        if (jobTitle.length !in 5..120 || jobDescription.length !in 20..5000 || jobCategory.isBlank() || jobDistrict.isBlank() || jobUpazila.isBlank() || wage == null || wage <= 0) {
                            jobMessage = "সব তথ্য সঠিকভাবে পূরণ করুন। শিরোনাম ৫-১২০ এবং বিস্তারিত ২০-৫০০০ অক্ষর হতে হবে।"
                        } else if (token == null) {
                            jobMessage = "আবার লগইন করুন।"
                        } else {
                            busy = true
                            scope.launch {
                                val result = MarketplaceRepository.createJobForReview(token, jobTitle, jobDescription, jobCategory, jobDistrict, jobUpazila, wage)
                                busy = false
                                jobMessage = if (result.isSuccess) "কাজটি অ্যাডমিন পর্যালোচনার জন্য জমা হয়েছে।" else result.exceptionOrNull()?.message ?: "কাজ পোস্ট করা যায়নি।"
                                if (result.isSuccess) {
                                    showJobForm = false
                                    jobTitle = ""; jobDescription = ""; jobCategory = ""; jobDistrict = ""; jobUpazila = ""; jobWage = ""
                                }
                            }
                        }
                    }) { Text(if (busy) "জমা হচ্ছে…" else "পর্যালোচনায় পাঠান") }
                },
                dismissButton = { TextButton(enabled = !busy, onClick = { showJobForm = false }) { Text("বন্ধ") } }
            )
        }
    }
}
