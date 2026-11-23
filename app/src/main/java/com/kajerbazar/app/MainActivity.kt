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
    var showLogin by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf("") }
    var signedInEmail by remember { mutableStateOf<String?>(null) }
    var accessToken by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<MarketplaceRepository.Profile?>(null) }
    var jobs by remember { mutableStateOf<List<MarketplaceRepository.Job>>(emptyList()) }
    var adminJobs by remember { mutableStateOf<List<MarketplaceRepository.Job>>(emptyList()) }
    var adminEmployerRequests by remember { mutableStateOf<List<MarketplaceRepository.EmployerRequest>>(emptyList()) }
    var jobMessage by remember { mutableStateOf("প্রকাশিত কাজ লোড হচ্ছে…") }
    LaunchedEffect(Unit) {
        connectionStatus = withContext(Dispatchers.IO) { SupabaseConnection.checkApi() }
        val publicJobs = MarketplaceRepository.getPublishedJobsPublic()
        jobs = publicJobs.getOrNull().orEmpty()
        jobMessage = if (publicJobs.isSuccess && jobs.isEmpty()) "এখনো কোনো কাজ প্রকাশিত হয়নি।" else publicJobs.exceptionOrNull()?.message ?: "প্রকাশিত কাজ দেখুন।"
    }
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
                            Text(if (signedInEmail == null) "লগইন" else "অ্যাকাউন্ট", color = Color.White)
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
                            Text("প্রকাশিত কাজ দেখতে লগইন লাগবে না। আবেদন করতে বিনামূল্যে অ্যাকাউন্ট খুলুন।")
                            Text("বাস্তব কাজের বিজ্ঞাপন প্রকাশের আগে নিয়োগদাতার অনুমোদন প্রয়োজন।")
                        }
                    }
                }
                item {
                    Text("বাংলাদেশের কাজের বাজার", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("জেলা ও উপজেলা অনুযায়ী প্রকাশিত কাজ দেখুন। কাজ ব্রাউজ করতে অ্যাকাউন্ট প্রয়োজন নেই।")
                    Button(onClick = { showLogin = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (signedInEmail == null) "ইমেইল দিয়ে শুরু করুন" else "অ্যাকাউন্ট দেখুন")
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
                if (profile?.role == "admin") {
                    item {
                        Text("অ্যাডমিন পর্যালোচনা", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            val token = accessToken
                            if (token != null) scope.launch {
                                val queues = MarketplaceRepository.getAdminQueues(token)
                                adminJobs = queues.getOrNull()?.first.orEmpty()
                                adminEmployerRequests = queues.getOrNull()?.second.orEmpty()
                                jobMessage = if (queues.isSuccess) "পর্যালোচনা তালিকা হালনাগাদ হয়েছে।" else queues.exceptionOrNull()?.message ?: "তালিকা পাওয়া যায়নি।"
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("পর্যালোচনা তালিকা হালনাগাদ") }
                    }
                    item { Text("কাজ অনুমোদন", fontWeight = FontWeight.Bold) }
                    if (adminJobs.isEmpty()) item { Text("অনুমোদনের অপেক্ষায় কোনো কাজ নেই।") }
                    items(adminJobs, key = { "admin-job-${it.id}" }) { job ->
                        Card {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(job.title, fontWeight = FontWeight.Bold)
                                Text("${job.district} • ৳${job.wagePaisa / 100}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val token = accessToken
                                        if (token != null) scope.launch {
                                            val result = MarketplaceRepository.adminReviewJob(token, job.id, true)
                                            jobMessage = if (result.isSuccess) "কাজ অনুমোদিত হয়েছে।" else result.exceptionOrNull()?.message ?: "ব্যর্থ হয়েছে।"
                                            adminJobs = adminJobs.filterNot { it.id == job.id }
                                        }
                                    }) { Text("অনুমোদন") }
                                    OutlinedButton(onClick = {
                                        val token = accessToken
                                        if (token != null) scope.launch {
                                            val result = MarketplaceRepository.adminReviewJob(token, job.id, false)
                                            jobMessage = if (result.isSuccess) "কাজ প্রত্যাখ্যান হয়েছে।" else result.exceptionOrNull()?.message ?: "ব্যর্থ হয়েছে।"
                                            adminJobs = adminJobs.filterNot { it.id == job.id }
                                        }
                                    }) { Text("প্রত্যাখ্যান") }
                                }
                            }
                        }
                    }
                    item { Text("কাজদাতা হওয়ার আবেদন", fontWeight = FontWeight.Bold) }
                    if (adminEmployerRequests.isEmpty()) item { Text("অনুমোদনের অপেক্ষায় কোনো কাজদাতা আবেদন নেই।") }
                    items(adminEmployerRequests, key = { "employer-${it.id}" }) { request ->
                        Card {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(request.businessName, fontWeight = FontWeight.Bold)
                                Text("${request.contactName} • ${request.district}, ${request.upazila}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val token = accessToken
                                        if (token != null) scope.launch {
                                            val result = MarketplaceRepository.adminReviewEmployerRequest(token, request.id, true)
                                            jobMessage = if (result.isSuccess) "কাজদাতা অনুমোদিত হয়েছে।" else result.exceptionOrNull()?.message ?: "ব্যর্থ হয়েছে।"
                                            adminEmployerRequests = adminEmployerRequests.filterNot { it.id == request.id }
                                        }
                                    }) { Text("অনুমোদন") }
                                    OutlinedButton(onClick = {
                                        val token = accessToken
                                        if (token != null) scope.launch {
                                            val result = MarketplaceRepository.adminReviewEmployerRequest(token, request.id, false)
                                            jobMessage = if (result.isSuccess) "কাজদাতা আবেদন প্রত্যাখ্যান হয়েছে।" else result.exceptionOrNull()?.message ?: "ব্যর্থ হয়েছে।"
                                            adminEmployerRequests = adminEmployerRequests.filterNot { it.id == request.id }
                                        }
                                    }) { Text("প্রত্যাখ্যান") }
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
                title = { Text(if (signedInEmail != null) "লগইন হয়েছে" else if (isSignUp) "নতুন অ্যাকাউন্ট খুলুন" else "ইমেইল দিয়ে লগইন") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (signedInEmail != null) {
                            Text("লগইন করা ইমেইল: $signedInEmail")
                            Text("নিরাপত্তার জন্য অ্যাপ বন্ধ করলে আবার লগইন করতে হতে পারে।")
                        } else {
                            if (isSignUp) OutlinedTextField(value = fullName, onValueChange = { fullName = it }, enabled = !busy, label = { Text("আপনার পূর্ণ নাম") }, singleLine = true)
                            OutlinedTextField(value = email, onValueChange = { email = it.trim() }, enabled = !busy, label = { Text("ইমেইল ঠিকানা") }, placeholder = { Text("name@example.com") }, singleLine = true)
                            OutlinedTextField(value = password, onValueChange = { password = it }, enabled = !busy, label = { Text("পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)") }, singleLine = true)
                        }
                        if (authMessage.isNotBlank()) Text(authMessage,
                            color = if (authMessage.contains("সফল") || authMessage.contains("তৈরি হয়েছে")) Color(0xFF18733B) else MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    if (signedInEmail != null) {
                        TextButton(onClick = {
                            signedInEmail = null; accessToken = null; userId = null; profile = null; jobs = emptyList()
                            jobMessage = "কাজ দেখতে ইমেইল দিয়ে লগইন করুন।"; showLogin = false
                        }) { Text("লগআউট") }
                    } else {
                        TextButton(enabled = !busy, onClick = {
                            busy = true; authMessage = ""
                            scope.launch {
                                val result = if (isSignUp) AuthRepository.signUp(email, password, fullName)
                                    else AuthRepository.signIn(email, password)
                                busy = false; authMessage = result.message
                                if (result.success && result.accessToken != null) {
                                    signedInEmail = result.email; accessToken = result.accessToken; userId = result.userId
                                    showLogin = false
                                    val token = result.accessToken
                                    val id = result.userId.orEmpty()
                                    val profileResult = MarketplaceRepository.getProfile(token, id)
                                    profile = profileResult.getOrNull()
                                    val jobResult = MarketplaceRepository.getPublishedJobs(token)
                                    jobs = jobResult.getOrNull().orEmpty()
                                    if (profile?.role == "admin") {
                                        val queues = MarketplaceRepository.getAdminQueues(token)
                                        adminJobs = queues.getOrNull()?.first.orEmpty()
                                        adminEmployerRequests = queues.getOrNull()?.second.orEmpty()
                                    }
                                    jobMessage = if (jobResult.isSuccess && jobs.isEmpty()) "এখনো কোনো কাজ প্রকাশিত হয়নি।" else jobResult.exceptionOrNull()?.message ?: "কাজের তালিকা হালনাগাদ হয়েছে।"
                                }
                            }
                        }) { Text(if (busy) "অপেক্ষা করুন…" else if (isSignUp) "অ্যাকাউন্ট তৈরি করুন" else "লগইন") }
                    }
                },
                dismissButton = {
                    if (signedInEmail == null) {
                        TextButton(enabled = !busy, onClick = { isSignUp = !isSignUp; authMessage = "" }) {
                            Text(if (isSignUp) "আগে অ্যাকাউন্ট আছে? লগইন" else "নতুন অ্যাকাউন্ট খুলুন")
                        }
                    } else TextButton(onClick = { showLogin = false }) { Text("বন্ধ") }
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
