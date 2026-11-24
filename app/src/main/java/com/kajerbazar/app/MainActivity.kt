package com.kajerbazar.app

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Blue = Color(0xFF145DA0)

private fun jobStatusLabel(status: String): String = when (status) {
    "draft" -> "খসড়া"
    "pending_review" -> "অনুমোদনের অপেক্ষায়"
    "published" -> "প্রকাশিত"
    "paused" -> "বিরত"
    "closed" -> "বন্ধ"
    "in_progress" -> "চলমান"
    "completed" -> "সম্পন্ন"
    "disputed" -> "বিরোধে"
    "admin_review" -> "অ্যাডমিন পর্যালোচনায়"
    else -> status
}

private fun applicationStatusLabel(status: String): String = when (status) {
    "submitted" -> "জমা হয়েছে"
    "withdrawn" -> "প্রত্যাহার করা হয়েছে"
    "selected" -> "নির্বাচিত"
    "rejected" -> "প্রত্যাখ্যাত"
    else -> status
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KajerBazarApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KajerBazarApp() {
    val appContext = LocalContext.current.applicationContext
    var connectionStatus by remember { mutableStateOf("Supabase সংযোগ পরীক্ষা হচ্ছে…") }
    var showLogin by remember { mutableStateOf(false) }
    var showAppSettings by remember { mutableStateOf(false) }
    var appAnnouncement by remember { mutableStateOf("") }
    var supportPhone by remember { mutableStateOf("") }
    var maintenanceMode by remember { mutableStateOf(false) }
    var settingsSaving by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var usePhoneOtp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var emailOtpSent by remember { mutableStateOf(false) }
    var emailOtp by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf("") }
    var signedInAccount by remember { mutableStateOf<String?>(null) }
    var accessToken by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<MarketplaceRepository.Profile?>(null) }
    var jobs by remember { mutableStateOf<List<MarketplaceRepository.Job>>(emptyList()) }
    var adminJobs by remember { mutableStateOf<List<MarketplaceRepository.Job>>(emptyList()) }
    var adminEmployerRequests by remember { mutableStateOf<List<MarketplaceRepository.EmployerRequest>>(emptyList()) }
    var adminProfiles by remember { mutableStateOf<List<MarketplaceRepository.AdminProfile>>(emptyList()) }
    var showAdminUsersDialog by remember { mutableStateOf(false) }
    var showAdminEditDialog by remember { mutableStateOf(false) }
    var editingAdminProfile by remember { mutableStateOf<MarketplaceRepository.AdminProfile?>(null) }
    var adminEditName by remember { mutableStateOf("") }
    var adminEditPhone by remember { mutableStateOf("") }
    var adminEditDistrict by remember { mutableStateOf("") }
    var adminEditUpazila by remember { mutableStateOf("") }
    var adminEditStatus by remember { mutableStateOf("active") }
    var jobMessage by remember { mutableStateOf("প্রকাশিত কাজ লোড হচ্ছে…") }
    var jobSearch by remember { mutableStateOf("") }
    var districtFilter by remember { mutableStateOf("") }
    var upazilaFilter by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var savedJobIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSavedOnly by remember { mutableStateOf(false) }
    val filteredJobs = jobs.filter { job ->
        (jobSearch.isBlank() || listOf(job.title, job.description, job.category, job.district, job.upazila).any { it.contains(jobSearch.trim(), ignoreCase = true) }) &&
        (districtFilter.isBlank() || job.district.contains(districtFilter.trim(), ignoreCase = true)) &&
        (upazilaFilter.isBlank() || job.upazila.contains(upazilaFilter.trim(), ignoreCase = true)) &&
        (categoryFilter.isBlank() || job.category.contains(categoryFilter.trim(), ignoreCase = true)) &&
        (!showSavedOnly || job.id in savedJobIds)
    }
    LaunchedEffect(Unit) {
        connectionStatus = withContext(Dispatchers.IO) { SupabaseConnection.checkApi() }
        val publicSettings = MarketplaceRepository.getAppSettings()
        publicSettings.getOrNull()?.let { settings ->
            appAnnouncement = settings.announcement
            supportPhone = settings.supportPhone
            maintenanceMode = settings.maintenanceMode
        }
        val publicJobs = MarketplaceRepository.getPublishedJobsPublic()
        jobs = publicJobs.getOrNull().orEmpty()
        jobMessage = if (publicJobs.isSuccess && jobs.isEmpty()) "এখনো কোনো কাজ প্রকাশিত হয়নি।" else publicJobs.exceptionOrNull()?.message ?: "প্রকাশিত কাজ দেখুন।"

        val savedSession = withContext(Dispatchers.IO) { SessionStore.load(appContext) }
        if (savedSession != null) {
            val renewed = AuthRepository.refreshSession(savedSession.refreshToken)
            val renewedToken = renewed.accessToken
            val renewedUserId = renewed.userId
            if (renewed.success && !renewedToken.isNullOrBlank() && !renewedUserId.isNullOrBlank()) {
                val renewedRefresh = renewed.refreshToken ?: savedSession.refreshToken
                val accountLabel = renewed.phone ?: renewed.email ?: savedSession.account
                signedInAccount = accountLabel
                accessToken = renewedToken
                userId = renewedUserId
                withContext(Dispatchers.IO) {
                    SessionStore.save(appContext, SessionStore.SavedSession(renewedRefresh, renewedUserId, accountLabel))
                }
                val profileResult = MarketplaceRepository.getProfile(renewedToken, renewedUserId)
                profile = profileResult.getOrNull()
                val jobResult = MarketplaceRepository.getPublishedJobs(renewedToken)
                jobs = jobResult.getOrNull().orEmpty()
                if (profile?.role == "admin") {
                    val queues = MarketplaceRepository.getAdminQueues(renewedToken)
                    adminJobs = queues.getOrNull()?.first.orEmpty()
                    adminEmployerRequests = queues.getOrNull()?.second.orEmpty()
                }
                jobMessage = if (jobResult.isSuccess && jobs.isEmpty()) "এখনো কোনো কাজ প্রকাশিত হয়নি।" else jobResult.exceptionOrNull()?.message ?: "আগের অ্যাকাউন্টে আবার প্রবেশ করা হয়েছে।"
            } else if (renewed.sessionExpired) {
                withContext(Dispatchers.IO) { SessionStore.clear(appContext) }
                jobMessage = "আগের লগইন সেশনের মেয়াদ শেষ। প্রয়োজন হলে আবার লগইন করুন।"
            } else {
                jobMessage = "আগের সেশন যাচাই করা যায়নি। ইন্টারনেট পেলে আবার চেষ্টা করুন।"
            }
        }
    }
    LaunchedEffect(accessToken, userId, profile?.role) {
        val token = accessToken
        val id = userId
        if (token != null && id != null && profile?.role == "worker") {
            savedJobIds = MarketplaceRepository.getSavedJobIds(token, id).getOrNull().orEmpty()
        } else if (token == null) {
            savedJobIds = emptySet()
            showSavedOnly = false
        }
    }
    var showProfileForm by remember { mutableStateOf(false) }
    var profileFullName by remember { mutableStateOf("") }
    var profileDistrict by remember { mutableStateOf("") }
    var profileUpazila by remember { mutableStateOf("") }
    var profileContactPhone by remember { mutableStateOf("") }
    var profileCurrentAddress by remember { mutableStateOf("") }
    var profilePermanentAddress by remember { mutableStateOf("") }
    var profileWorkType by remember { mutableStateOf("") }
    var profileSkills by remember { mutableStateOf("") }
    var profileAvatarUrl by remember { mutableStateOf("") }
    var profileOrganizationName by remember { mutableStateOf("") }
    var profileOrganizationType by remember { mutableStateOf("") }
    var profileOrganizationDetails by remember { mutableStateOf("") }
    var profileContactPerson by remember { mutableStateOf("") }
    var profileExperience by remember { mutableStateOf("") }
    var profileAvailability by remember { mutableStateOf("") }
    var profileExpectedWage by remember { mutableStateOf("") }
    var profilePortfolio by remember { mutableStateOf("") }
    var showMyApplicationsDialog by remember { mutableStateOf(false) }
    var myApplications by remember { mutableStateOf<List<MarketplaceRepository.WorkerApplication>>(emptyList()) }
    var showApplicationsDialog by remember { mutableStateOf(false) }
    var employerJobs by remember { mutableStateOf<List<MarketplaceRepository.EmployerJob>>(emptyList()) }
    var employerApplications by remember { mutableStateOf<List<MarketplaceRepository.EmployerApplication>>(emptyList()) }
    var selectedEmployerJob by remember { mutableStateOf<MarketplaceRepository.EmployerJob?>(null) }
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedPage by remember { mutableStateOf("home") }
    val mainListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val token = accessToken
            val id = userId
            if (token == null || id == null) {
                jobMessage = "ছবি আপলোড করতে আবার লগইন করুন।"
            } else {
                busy = true
                scope.launch {
                    val mime = appContext.contentResolver.getType(uri).orEmpty()
                    val extension = when (mime.lowercase()) {
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        "image/jpeg", "image/jpg" -> "jpg"
                        else -> ""
                    }
                    val result = runCatching {
                        require(extension.isNotBlank()) { "শুধু JPG, PNG অথবা WebP ছবি নির্বাচন করুন।" }
                        val bytes = withContext(Dispatchers.IO) {
                            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: error("ছবিটি পড়া যায়নি।")
                        }
                        require(bytes.isNotEmpty() && bytes.size <= 3 * 1024 * 1024) { "ছবির আকার ৩ MB-এর মধ্যে হতে হবে।" }
                        MarketplaceRepository.uploadAvatar(token, id, bytes, mime, extension)
                    }
                    busy = false
                    if (result.isSuccess) {
                        profileAvatarUrl = result.getOrThrow()
                        jobMessage = "প্রোফাইল ছবি আপলোড হয়েছে। সংরক্ষণ করুন।"
                    } else {
                        jobMessage = result.exceptionOrNull()?.message ?: "ছবি আপলোড হয়নি।"
                    }
                }
            }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Blue)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("কাজের বাজার", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Blue)
                        Text(profile?.fullName?.ifBlank { signedInAccount ?: "অতিথি ব্যবহারকারী" } ?: "অতিথি ব্যবহারকারী")
                        Text(when (profile?.role) { "admin" -> "অ্যাডমিন"; "employer" -> "কাজদাতা"; "worker" -> "কর্মী"; else -> "অতিথি" }, color = Blue)
                    }
                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("হোম") }, selected = true,
                        onClick = { scope.launch { drawerState.close(); mainListState.animateScrollToItem(0) } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("কাজ খুঁজুন") }, selected = false,
                        onClick = { scope.launch { drawerState.close(); mainListState.animateScrollToItem(0) } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text(if (profile == null) "লগইন / অ্যাকাউন্ট" else "আমার প্রোফাইল") }, selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (profile == null) showLogin = true else {
                                profileFullName = profile?.fullName.orEmpty()
                                profileDistrict = profile?.district.orEmpty()
                                profileUpazila = profile?.upazila.orEmpty()
                                profileContactPhone = profile?.contactPhone?.ifBlank { profile?.authPhone.orEmpty() }.orEmpty()
                                profileCurrentAddress = profile?.currentAddress.orEmpty()
                                profilePermanentAddress = profile?.permanentAddress.orEmpty()
                                profileWorkType = profile?.workType.orEmpty()
                                profileSkills = profile?.skills.orEmpty()
                                profileAvatarUrl = profile?.avatarUrl.orEmpty()
                                profileOrganizationName = profile?.organizationName.orEmpty()
                                profileOrganizationType = profile?.organizationType.orEmpty()
                                profileOrganizationDetails = profile?.organizationDetails.orEmpty()
                                profileContactPerson = profile?.contactPerson.orEmpty()
                                profileExperience = profile?.experience.orEmpty()
                                profileAvailability = profile?.availability.orEmpty()
                                profileExpectedWage = profile?.expectedWage.orEmpty()
                                profilePortfolio = profile?.portfolio.orEmpty()
                                showProfileForm = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    if (profile?.role == "worker") {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                            label = { Text("আমার আবেদন") }, selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                val token = accessToken
                                if (token != null) {
                                    showMyApplicationsDialog = true; busy = true
                                    scope.launch {
                                        val result = MarketplaceRepository.getWorkerApplications(token)
                                        busy = false; myApplications = result.getOrNull().orEmpty()
                                        if (result.isFailure) jobMessage = result.exceptionOrNull()?.message ?: "আবেদন পাওয়া যায়নি।"
                                    }
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    if (profile?.role == "employer") {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Work, contentDescription = null) },
                            label = { Text("নতুন কাজ পোস্ট") }, selected = false,
                            onClick = { scope.launch { drawerState.close() }; showJobForm = true },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                            label = { Text("কাজের আবেদন দেখুন") }, selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                val token = accessToken; val id = userId
                                if (token != null && id != null) {
                                    selectedEmployerJob = null; employerApplications = emptyList()
                                    showApplicationsDialog = true; busy = true
                                    scope.launch {
                                        val result = MarketplaceRepository.getEmployerJobs(token, id)
                                        busy = false; employerJobs = result.getOrNull().orEmpty()
                                        if (result.isFailure) jobMessage = result.exceptionOrNull()?.message ?: "কাজের তালিকা পাওয়া যায়নি।"
                                    }
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    if (profile?.role == "admin") {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                            label = { Text("অ্যাডমিন ড্যাশবোর্ড") }, selected = false,
                            onClick = {
                                val adminIndex = 1 +
                                    (if (appAnnouncement.isNotBlank()) 1 else 0) +
                                    (if (maintenanceMode) 1 else 0) + 2 +
                                    (if (profile != null) 1 else 0) + 1 +
                                    (if (jobs.isEmpty() || filteredJobs.isEmpty()) 1 else filteredJobs.size)
                                scope.launch { drawerState.close(); mainListState.animateScrollToItem(adminIndex.coerceAtLeast(0)) }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("অ্যাপ সেটিংস") }, selected = false,
                        onClick = { scope.launch { drawerState.close() }; showAppSettings = true },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                        label = { Text(if (profile == null) "লগইন করুন" else "অ্যাকাউন্ট তথ্য") }, selected = false,
                        onClick = { scope.launch { drawerState.close() }; showLogin = true },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Spacer(Modifier.height(18.dp))
                    Text("নিরাপদে কাজ খুঁজুন • নিরাপদে কাজ দিন", modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column {
                        Text("কাজের বাজার", fontWeight = FontWeight.Bold)
                        Text("কাজ খুঁজুন, কাজ দিন", style = MaterialTheme.typography.labelSmall)
                    } },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "মেনু খুলুন", tint = Color.White)
                        }
                    },
                    actions = {
                        TextButton(onClick = { showLogin = true }) {
                            Text(if (signedInAccount == null) "লগইন" else "অ্যাকাউন্ট", color = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue, titleContentColor = Color.White)
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedPage == "home", onClick = { selectedPage = "home"; scope.launch { mainListState.animateScrollToItem(0) } }, icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text("হোম") })
                    NavigationBarItem(selected = selectedPage == "jobs", onClick = { selectedPage = "jobs"; scope.launch { mainListState.animateScrollToItem(0) } }, icon = { Icon(Icons.Default.Work, contentDescription = null) }, label = { Text("কাজ খুঁজুন") })
                    NavigationBarItem(selected = selectedPage == "activity", onClick = {
                        selectedPage = "activity"
                        scope.launch { mainListState.animateScrollToItem(0) }
                    }, icon = { Icon(Icons.Default.ListAlt, contentDescription = null) }, label = { Text("কার্যক্রম") })
                    NavigationBarItem(selected = selectedPage == "messages", onClick = { selectedPage = "messages"; scope.launch { mainListState.animateScrollToItem(0) } }, icon = { Icon(Icons.Default.Email, contentDescription = null) }, label = { Text("মেসেজ") })
                    NavigationBarItem(selected = selectedPage == "media", onClick = { selectedPage = "media"; scope.launch { mainListState.animateScrollToItem(0) } }, icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) }, label = { Text("ভিডিও") })
                    NavigationBarItem(selected = selectedPage == "profile", onClick = { selectedPage = "profile"; scope.launch { mainListState.animateScrollToItem(0) } }, icon = { Icon(Icons.Default.Person, contentDescription = null) }, label = { Text("প্রোফাইল") })
                }
            }
        ) { padding ->
            LazyColumn(
                state = mainListState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedPage == "messages") item {
                    MessagesScreen(token = accessToken, userId = userId, onLogin = { showLogin = true })
                }
                if (selectedPage == "media") item {
                    MediaHubScreen(token = accessToken, userId = userId, onLogin = { showLogin = true })
                }
                if (selectedPage == "activity") item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("আপনার কার্যক্রম", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            when (profile?.role) {
                                "worker" -> {
                                    Text("আপনার পাঠানো কাজের আবেদন ও সেগুলোর অবস্থা এখানে দেখুন।")
                                    Button(enabled = !busy, onClick = {
                                        val token = accessToken
                                        if (token == null) showLogin = true else {
                                            busy = true
                                            scope.launch {
                                                val result = MarketplaceRepository.getWorkerApplications(token)
                                                myApplications = result.getOrNull().orEmpty()
                                                busy = false
                                                if (result.isFailure) jobMessage = result.exceptionOrNull()?.message ?: "আবেদন পাওয়া যায়নি।"
                                                showMyApplicationsDialog = true
                                            }
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (busy) "লোড হচ্ছে…" else "আমার আবেদন দেখুন")
                                    }
                                }
                                "employer" -> {
                                    Text("আপনার পোস্ট করা কাজ, আবেদনকারী এবং কর্মী নির্বাচনের কাজ এখান থেকে পরিচালনা করুন।")
                                    Button(enabled = !busy, onClick = {
                                        val token = accessToken
                                        val id = userId
                                        if (token == null || id == null) showLogin = true else {
                                            busy = true
                                            scope.launch {
                                                val result = MarketplaceRepository.getEmployerJobs(token, id)
                                                employerJobs = result.getOrNull().orEmpty()
                                                selectedEmployerJob = null
                                                employerApplications = emptyList()
                                                busy = false
                                                if (result.isFailure) jobMessage = result.exceptionOrNull()?.message ?: "কাজের তালিকা পাওয়া যায়নি।"
                                                showApplicationsDialog = true
                                            }
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (busy) "লোড হচ্ছে…" else "আমার পোস্ট ও আবেদন দেখুন")
                                    }
                                    OutlinedButton(onClick = { showJobForm = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text("নতুন কাজ পোস্ট করুন")
                                    }
                                }
                                "admin" -> {
                                    Text("কাজ ও কাজদাতা আবেদনের পর্যালোচনা এখানে পরিচালনা করুন।")
                                    Button(enabled = !busy, onClick = {
                                        val token = accessToken
                                        if (token != null) scope.launch {
                                            busy = true
                                            val queues = MarketplaceRepository.getAdminQueues(token)
                                            adminJobs = queues.getOrNull()?.first.orEmpty()
                                            adminEmployerRequests = queues.getOrNull()?.second.orEmpty()
                                            busy = false
                                            jobMessage = if (queues.isSuccess) "অ্যাডমিন তালিকা হালনাগাদ হয়েছে।" else queues.exceptionOrNull()?.message ?: "তালিকা পাওয়া যায়নি।"
                                            selectedPage = "home"
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (busy) "লোড হচ্ছে…" else "অ্যাডমিন পর্যালোচনা খুলুন")
                                    }
                                }
                                else -> {
                                    Text("লগইন করে আপনার আবেদন বা কাজদাতার পোস্টের অগ্রগতি দেখতে পারবেন।")
                                    Button(onClick = { showLogin = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text("লগইন / অ্যাকাউন্ট খুলুন")
                                    }
                                }
                            }
                        }
                    }
                }
                if (selectedPage == "home") {
                item { Card { Text(connectionStatus, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) } }
                if (appAnnouncement.isNotBlank()) item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF))) {
                        Text("ঘোষণা: $appAnnouncement", modifier = Modifier.padding(12.dp), color = Blue)
                    }
                }
                if (maintenanceMode) item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Text("অ্যাপ রক্ষণাবেক্ষণ চলছে। কিছু সুবিধা সাময়িকভাবে সীমিত থাকতে পারে।", modifier = Modifier.padding(12.dp))
                    }
                }
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
                        Text(if (signedInAccount == null) "ইমেইল বা মোবাইল দিয়ে শুরু করুন" else "অ্যাকাউন্ট দেখুন")
                    }
                }
                }
                if (selectedPage == "profile" && profile != null) {
                    item {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = if (profile?.role == "employer") Color(0xFFEAF2FF) else Color(0xFFEAF7F0)
                        )) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(if (profile?.role == "employer") "কাজদাতা প্রোফাইল" else "কর্মী প্রোফাইল", fontWeight = FontWeight.Bold, color = Blue)
                                if (!profile?.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = profile?.avatarUrl,
                                        contentDescription = "প্রোফাইল ছবি",
                                        modifier = Modifier.size(76.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text("নাম: ${profile?.fullName?.ifBlank { "নাম যোগ করা হয়নি" }}")
                                Text("মোবাইল: ${profile?.contactPhone?.ifBlank { profile?.authPhone?.ifBlank { "যোগ করা হয়নি" } ?: "যোগ করা হয়নি" }}")
                                if (profile?.role == "employer") {
                                    Text("কাজদাতার ব্যবসা / প্রতিষ্ঠান", fontWeight = FontWeight.Bold, color = Blue)
                                    Text("প্রতিষ্ঠান: ${profile?.organizationName?.ifBlank { "যোগ করা হয়নি" }}")
                                    Text("ধরন: ${profile?.organizationType?.ifBlank { "যোগ করা হয়নি" }}")
                                    Text("যোগাযোগকারী: ${profile?.contactPerson?.ifBlank { "যোগ করা হয়নি" }}")
                                    Text("প্রতিষ্ঠান পরিচিতি: ${profile?.organizationDetails?.ifBlank { "যোগ করা হয়নি" }}")
                                } else if (profile?.role == "worker") {
                                    Text("কর্মীর দক্ষতা প্রোফাইল", fontWeight = FontWeight.Bold, color = Blue)
                                    Text("কাজের ধরন: ${profile?.workType?.ifBlank { "যোগ করা হয়নি" }}")
                                    Text("অভিজ্ঞতা: ${profile?.experience?.ifBlank { "যোগ করা হয়নি" }}")
                                    Text("কাজের সময়: ${profile?.availability?.ifBlank { "যোগ করা হয়নি" }}")
                                    Text("প্রত্যাশিত মজুরি: ${profile?.expectedWage?.ifBlank { "আলোচনা সাপেক্ষ" }}")
                                    Text("কাজের নমুনা / পোর্টফোলিও: ${profile?.portfolio?.ifBlank { "যোগ করা হয়নি" }}")
                                    Text("দক্ষতা: ${profile?.skills?.ifBlank { "যোগ করা হয়নি" }}")
                                }
                                OutlinedButton(onClick = {
                                    profileFullName = profile?.fullName.orEmpty()
                                    profileDistrict = profile?.district.orEmpty()
                                    profileUpazila = profile?.upazila.orEmpty()
                                    profileContactPhone = profile?.contactPhone?.ifBlank { profile?.authPhone.orEmpty() }.orEmpty()
                                    profileCurrentAddress = profile?.currentAddress.orEmpty()
                                    profilePermanentAddress = profile?.permanentAddress.orEmpty()
                                    profileWorkType = profile?.workType.orEmpty()
                                    profileSkills = profile?.skills.orEmpty()
                                    profileAvatarUrl = profile?.avatarUrl.orEmpty()
                                    profileOrganizationName = profile?.organizationName.orEmpty()
                                    profileOrganizationType = profile?.organizationType.orEmpty()
                                    profileOrganizationDetails = profile?.organizationDetails.orEmpty()
                                    profileContactPerson = profile?.contactPerson.orEmpty()
                                    profileExperience = profile?.experience.orEmpty()
                                    profileAvailability = profile?.availability.orEmpty()
                                    profileExpectedWage = profile?.expectedWage.orEmpty()
                                    profilePortfolio = profile?.portfolio.orEmpty()
                                    showProfileForm = true
                                }, modifier = Modifier.fillMaxWidth()) { Text("প্রোফাইল সম্পাদনা করুন") }
                                Text("অ্যাকাউন্ট: ${when (profile?.role) { "employer" -> "কাজদাতা / নিয়োগদাতা"; "admin" -> "অ্যাডমিন"; else -> "কর্মী / কাজপ্রার্থী" }}")
                                Text("এলাকা: ${listOfNotNull(profile?.district, profile?.upazila).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "যোগ করা হয়নি" }}")
                                if (profile?.role == "worker") {
                                    Button(onClick = { showEmployerForm = true }, modifier = Modifier.fillMaxWidth()) { Text("কাজদাতা হতে আবেদন করুন") }
                                    OutlinedButton(onClick = {
                                        val token = accessToken
                                        if (token != null) {
                                            showMyApplicationsDialog = true
                                            busy = true
                                            scope.launch {
                                                val result = MarketplaceRepository.getWorkerApplications(token)
                                                busy = false
                                                myApplications = result.getOrNull().orEmpty()
                                                if (result.isFailure) jobMessage = result.exceptionOrNull()?.message ?: "আবেদন পাওয়া যায়নি।"
                                            }
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) { Text("আমার আবেদনগুলো দেখুন") }
                                }
                                if (profile?.role == "employer") {
                                    Button(onClick = { showJobForm = true }, modifier = Modifier.fillMaxWidth()) { Text("নতুন কাজ পোস্ট করুন") }
                                    OutlinedButton(onClick = {
                                        val token = accessToken
                                        val id = userId
                                        if (token != null && id != null) {
                                            selectedEmployerJob = null
                                            employerApplications = emptyList()
                                            showApplicationsDialog = true
                                            busy = true
                                            scope.launch {
                                                val result = MarketplaceRepository.getEmployerJobs(token, id)
                                                busy = false
                                                employerJobs = result.getOrNull().orEmpty()
                                                if (result.isFailure) jobMessage = result.exceptionOrNull()?.message ?: "কাজের তালিকা পাওয়া যায়নি।"
                                            }
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) { Text("কাজের আবেদনগুলো দেখুন") }
                                }
                            }
                        }
                    }
                }
                if (selectedPage == "profile" && profile != null) item { MediaPostsSection(token = accessToken, userId = userId, onLogin = { showLogin = true }) }
                if (selectedPage == "profile" && profile == null) item {
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("আপনার প্রোফাইল", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("প্রোফাইল দেখতে ও সম্পাদনা করতে লগইন করুন।")
                        Button(onClick = { showLogin = true }, modifier = Modifier.fillMaxWidth()) { Text("লগইন / অ্যাকাউন্ট খুলুন") }
                    } }
                }
                if (selectedPage == "home" || selectedPage == "jobs") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (selectedPage == "home") "সাম্প্রতিক কাজ" else "প্রকাশিত কাজ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = jobSearch, onValueChange = { jobSearch = it.take(100) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("কাজের নাম বা শব্দ দিয়ে খুঁজুন") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = districtFilter, onValueChange = { districtFilter = it.take(60) },
                                modifier = Modifier.weight(1f), singleLine = true,
                                label = { Text("জেলা") }
                            )
                            OutlinedTextField(
                                value = upazilaFilter, onValueChange = { upazilaFilter = it.take(60) },
                                modifier = Modifier.weight(1f), singleLine = true,
                                label = { Text("উপজেলা") }
                            )
                        }
                        OutlinedTextField(
                            value = categoryFilter, onValueChange = { categoryFilter = it.take(60) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("কাজের বিভাগ") }
                        )
                        if (profile?.role == "worker") {
                            OutlinedButton(onClick = { showSavedOnly = !showSavedOnly }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (showSavedOnly) "সব কাজ দেখুন" else "সংরক্ষিত কাজ দেখুন (${savedJobIds.size})")
                            }
                        }
                        Text("দেখানো হচ্ছে: ${filteredJobs.size}টি কাজ", style = MaterialTheme.typography.bodySmall)
                    }
                }
                val visibleJobs = if (selectedPage == "home") filteredJobs.take(3) else filteredJobs
                if (jobs.isEmpty()) {
                    item { Text(jobMessage, style = MaterialTheme.typography.bodyMedium) }
                } else if (visibleJobs.isEmpty()) {
                    item { Text("এই অনুসন্ধান বা এলাকার সঙ্গে মেলে এমন কোনো কাজ পাওয়া যায়নি। ফিল্টার পরিবর্তন করে দেখুন।", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(visibleJobs, key = { it.id }) { job ->
                        Card {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(job.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${job.category} • ${job.district}${if (job.upazila.isNotBlank()) ", ${job.upazila}" else ""}")
                                Text(job.description, maxLines = 4)
                                Text("কর্মীর মজুরি: ৳${job.wagePaisa / 100}", color = Blue, fontWeight = FontWeight.Bold)
                                if (profile?.role == "worker") {
                                    OutlinedButton(enabled = !busy, onClick = {
                                        val token = accessToken
                                        val id = userId
                                        if (token != null && id != null) scope.launch {
                                            val shouldSave = job.id !in savedJobIds
                                            val result = MarketplaceRepository.setJobSaved(token, id, job.id, shouldSave)
                                            if (result.isSuccess) {
                                                savedJobIds = if (shouldSave) savedJobIds + job.id else savedJobIds - job.id
                                                jobMessage = if (shouldSave) "কাজটি সংরক্ষিত হয়েছে।" else "সংরক্ষিত তালিকা থেকে সরানো হয়েছে।"
                                            } else {
                                                jobMessage = result.exceptionOrNull()?.message ?: "কাজ সংরক্ষণ করা যায়নি।"
                                            }
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (job.id in savedJobIds) "★ সংরক্ষিত — সরাতে চাপ দিন" else "☆ কাজটি সংরক্ষণ করুন")
                                    }
                                }
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
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("অ্যাডমিন ড্যাশবোর্ড", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Blue)
                                Text("অপেক্ষমাণ কাজ: ${adminJobs.size}টি")
                                Text("কাজদাতা আবেদন: ${adminEmployerRequests.size}টি")
                                OutlinedButton(onClick = { showAppSettings = true }, modifier = Modifier.fillMaxWidth()) { Text("অ্যাপ সেটিংস পরিচালনা") }
                                OutlinedButton(enabled = !busy, onClick = {
                                    val token = accessToken
                                    if (token != null) scope.launch {
                                        busy = true
                                        val result = MarketplaceRepository.adminListProfiles(token)
                                        adminProfiles = result.getOrNull().orEmpty()
                                        busy = false
                                        if (result.isSuccess) showAdminUsersDialog = true
                                        else jobMessage = result.exceptionOrNull()?.message ?: "ব্যবহারকারী তালিকা পাওয়া যায়নি।"
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("ব্যবহারকারী পরিচালনা") }
                            }
                        }
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
        }
        }

        if (showLogin) {
            AlertDialog(
                onDismissRequest = { if (!busy) showLogin = false },
                title = { Text(if (signedInAccount != null) "লগইন হয়েছে" else if (isSignUp) "নতুন অ্যাকাউন্ট খুলুন" else "লগইন করুন") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (signedInAccount != null) {
                            Text("লগইন করা অ্যাকাউন্ট: $signedInAccount")
                            Text("নিরাপত্তার জন্য অ্যাপ বন্ধ করলে আবার লগইন করতে হতে পারে।")
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { if (!busy) { usePhoneOtp = true; authMessage = ""; otpSent = false } }) {
                                    Text(if (usePhoneOtp) "✓ মোবাইল OTP" else "মোবাইল OTP")
                                }
                                TextButton(onClick = { if (!busy) { usePhoneOtp = false; otpSent = false; authMessage = "" } }) {
                                    Text(if (!usePhoneOtp) "✓ ইমেইল" else "ইমেইল")
                                }
                            }
                            if (isSignUp && ((!usePhoneOtp && !emailOtpSent) || (usePhoneOtp && !otpSent))) {
                                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, enabled = !busy, label = { Text("আপনার পূর্ণ নাম") }, singleLine = true)
                            }
                            if (usePhoneOtp) {
                                OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16) }, enabled = !busy && !otpSent, label = { Text("মোবাইল নম্বর") }, placeholder = { Text("01XXXXXXXXX") }, singleLine = true)
                                if (otpSent) OutlinedTextField(value = otp, onValueChange = { otp = it.filter(Char::isDigit).take(8) }, enabled = !busy, label = { Text("SMS-এ পাওয়া OTP কোড") }, singleLine = true)
                            } else {
                                OutlinedTextField(value = email, onValueChange = { email = it }, enabled = !busy && !emailOtpSent, label = { Text("ইমেইল ঠিকানা") }, placeholder = { Text("name@example.com") }, singleLine = true)
                                if (isSignUp && emailOtpSent) {
                                    OutlinedTextField(value = emailOtp, onValueChange = { emailOtp = it.filter(Char::isDigit).take(6) }, enabled = !busy, label = { Text("Gmail-এ পাওয়া ৬ সংখ্যার OTP") }, singleLine = true)
                                } else {
                                    OutlinedTextField(value = password, onValueChange = { password = it }, enabled = !busy, label = { Text("পাসওয়ার্ড") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                                }
                            }
                        }
                        if (authMessage.isNotBlank()) Text(authMessage,
                            color = if (authMessage.contains("সফল") || authMessage.contains("তৈরি হয়েছে") || authMessage.contains("পাঠানোর অনুরোধ সফল")) Color(0xFF18733B) else MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    if (signedInAccount != null) {
                        TextButton(onClick = {
                            val currentToken = accessToken
                            signedInAccount = null; accessToken = null; userId = null; profile = null
                            scope.launch {
                                if (!currentToken.isNullOrBlank()) AuthRepository.signOut(currentToken)
                                withContext(Dispatchers.IO) { SessionStore.clear(appContext) }
                            }
                            jobMessage = "প্রকাশিত কাজ দেখতে লগইন প্রয়োজন নেই।"; showLogin = false
                        }) { Text("লগআউট") }
                    } else {
                        TextButton(enabled = !busy, onClick = {
                            busy = true; authMessage = ""
                            scope.launch {
                                val result = when {
                                    !usePhoneOtp && isSignUp && !emailOtpSent -> AuthRepository.signUpEmail(email, password, fullName)
                                    !usePhoneOtp && isSignUp && emailOtpSent -> AuthRepository.verifyEmailOtp(email, emailOtp)
                                    !usePhoneOtp -> AuthRepository.signInEmail(email, password)
                                    !otpSent -> AuthRepository.requestOtp(phone, fullName, isSignUp)
                                    else -> AuthRepository.verifyOtp(phone, otp)
                                }
                                busy = false
                                authMessage = result.message
                                if (result.success && !usePhoneOtp && isSignUp && !emailOtpSent && result.accessToken == null) {
                                    emailOtpSent = true
                                    authMessage = result.message
                                } else if (result.success && usePhoneOtp && !otpSent) {
                                    phone = result.phone ?: phone
                                    otpSent = true
                                } else if (result.success && (result.accessToken != null)) {
                                    signedInAccount = result.phone ?: result.email
                                    accessToken = result.accessToken
                                    userId = result.userId
                                    val newRefreshToken = result.refreshToken
                                    val accountLabel = result.phone ?: result.email ?: ""
                                    if (!newRefreshToken.isNullOrBlank() && !result.userId.isNullOrBlank()) {
                                        withContext(Dispatchers.IO) {
                                            SessionStore.save(appContext, SessionStore.SavedSession(newRefreshToken, result.userId, accountLabel))
                                        }
                                    }
                                    otp = ""; otpSent = false; emailOtp = ""; emailOtpSent = false
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
                        }) {
                            Text(if (busy) "অপেক্ষা করুন…" else when {
                                !usePhoneOtp && isSignUp && emailOtpSent -> "ইমেইল OTP যাচাই করুন"
                                !usePhoneOtp && isSignUp -> "অ্যাকাউন্ট তৈরি ও OTP পাঠান"
                                !usePhoneOtp -> "ইমেইল দিয়ে লগইন"
                                otpSent -> "OTP যাচাই করুন"
                                else -> "OTP পাঠান"
                            })
                        }
                    }
                },
                dismissButton = {
                    if (signedInAccount == null) {
                        TextButton(enabled = !busy, onClick = {
                            if (usePhoneOtp && otpSent) {
                                otpSent = false; otp = ""; authMessage = "মোবাইল নম্বর পরিবর্তন করতে পারেন。"
                            } else if (!usePhoneOtp && emailOtpSent) {
                                emailOtpSent = false; emailOtp = ""; authMessage = "ইমেইল ও তথ্য পরিবর্তন করতে পারেন।"
                            } else {
                                isSignUp = !isSignUp; authMessage = ""
                            }
                        }) {
                            Text(if (usePhoneOtp && otpSent) "নম্বর পরিবর্তন" else if (!usePhoneOtp && emailOtpSent) "তথ্য পরিবর্তন" else if (isSignUp) "আগে অ্যাকাউন্ট আছে? লগইন" else "নতুন অ্যাকাউন্ট খুলুন")
                        }
                    } else TextButton(onClick = { showLogin = false }) { Text("বন্ধ") }
                }
            )
        }

        if (showProfileForm) {
            AlertDialog(
                onDismissRequest = { if (!busy) showProfileForm = false },
                title = { Text("প্রোফাইল সম্পাদনা") },
                text = {
                    Column(
                        Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (profileAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = profileAvatarUrl,
                                contentDescription = "প্রোফাইল ছবি",
                                modifier = Modifier.size(88.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        OutlinedButton(onClick = { avatarPicker.launch("image/*") }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth()) { Text("প্রোফাইল ছবি যোগ / পরিবর্তন করুন") }
                        OutlinedTextField(profileFullName, { profileFullName = it.take(100) }, enabled = !busy,
                            label = { Text("পূর্ণ নাম") }, singleLine = true)
                        OutlinedTextField(profileContactPhone, { profileContactPhone = it.take(20) }, enabled = !busy,
                            label = { Text("যোগাযোগের মোবাইল নম্বর") }, singleLine = true)
                        OutlinedTextField(profileCurrentAddress, { profileCurrentAddress = it.take(300) }, enabled = !busy,
                            label = { Text("বর্তমান ঠিকানা") }, minLines = 2)
                        OutlinedTextField(profilePermanentAddress, { profilePermanentAddress = it.take(300) }, enabled = !busy,
                            label = { Text("স্থায়ী ঠিকানা") }, minLines = 2)
                        OutlinedTextField(profileDistrict, { profileDistrict = it.take(100) }, enabled = !busy,
                            label = { Text("জেলা") }, singleLine = true)
                        OutlinedTextField(profileUpazila, { profileUpazila = it.take(100) }, enabled = !busy,
                            label = { Text("উপজেলা") }, singleLine = true)
                        if (profile?.role == "employer") {
                            Text("কাজদাতা / প্রতিষ্ঠানের পরিচিতি", fontWeight = FontWeight.Bold, color = Blue)
                            OutlinedTextField(profileOrganizationName, { profileOrganizationName = it.take(160) }, enabled = !busy,
                                label = { Text("প্রতিষ্ঠান বা ব্যবসার নাম") }, singleLine = true)
                            OutlinedTextField(profileOrganizationType, { profileOrganizationType = it.take(100) }, enabled = !busy,
                                label = { Text("প্রতিষ্ঠানের ধরন (দোকান, কোম্পানি, ব্যক্তিগত)") }, singleLine = true)
                            OutlinedTextField(profileContactPerson, { profileContactPerson = it.take(100) }, enabled = !busy,
                                label = { Text("যোগাযোগের দায়িত্বে") }, singleLine = true)
                            OutlinedTextField(profileOrganizationDetails, { profileOrganizationDetails = it.take(1500) }, enabled = !busy,
                                label = { Text("প্রতিষ্ঠান ও কী ধরনের কাজ দেন") }, minLines = 3)
                        } else {
                            Text("কর্মীর কাজের প্রোফাইল", fontWeight = FontWeight.Bold, color = Blue)
                            OutlinedTextField(profileWorkType, { profileWorkType = it.take(100) }, enabled = !busy,
                                label = { Text("কোন ধরনের কাজ খুঁজছেন") }, singleLine = true)
                            OutlinedTextField(profileSkills, { profileSkills = it.take(1000) }, enabled = !busy,
                                label = { Text("দক্ষতা ও কাজের অভিজ্ঞতা") }, minLines = 3)
                            OutlinedTextField(profileExperience, { profileExperience = it.take(1000) }, enabled = !busy,
                                label = { Text("আগের কাজের অভিজ্ঞতা") }, minLines = 2)
                            OutlinedTextField(profileAvailability, { profileAvailability = it.take(100) }, enabled = !busy,
                                label = { Text("কখন কাজ করতে পারবেন") }, singleLine = true)
                            OutlinedTextField(profileExpectedWage, { profileExpectedWage = it.take(100) }, enabled = !busy,
                                label = { Text("প্রত্যাশিত মজুরি (ঐচ্ছিক)") }, singleLine = true)
                            OutlinedTextField(profilePortfolio, { profilePortfolio = it.take(1500) }, enabled = !busy,
                                label = { Text("কাজের নমুনা / পোর্টফোলিও (ঐচ্ছিক)") }, minLines = 2)
                        }
                        Text("পূর্ণ নাম আবশ্যক। ছবি JPG, PNG বা WebP; সর্বোচ্চ ৩ MB।")
                    }
                },
                confirmButton = {
                    TextButton(enabled = !busy, onClick = {
                        val token = accessToken
                        if (token == null) {
                            jobMessage = "আবার লগইন করুন।"
                        } else if (profileFullName.trim().length !in 2..100) {
                            jobMessage = "পূর্ণ নাম ২ থেকে ১০০ অক্ষরের মধ্যে লিখুন।"
                        } else {
                            busy = true
                            scope.launch {
                                val result = MarketplaceRepository.updateMyProfile(
                                    token,
                                    profileFullName.trim(),
                                    profileContactPhone.trim(),
                                    profileDistrict.trim(),
                                    profileUpazila.trim(),
                                    profileCurrentAddress.trim(),
                                    profilePermanentAddress.trim(),
                                    profileWorkType.trim(),
                                    profileSkills.trim(),
                                    profileAvatarUrl.trim()
                                )
                                val roleResult = if (!result.isSuccess) {
                                    Result.failure<Unit>(result.exceptionOrNull() ?: IllegalStateException("প্রোফাইল আপডেট হয়নি।"))
                                } else if (profile?.role == "admin") {
                                    Result.success(Unit)
                                } else {
                                    MarketplaceRepository.updateRoleProfile(
                                        token,
                                        profileOrganizationName.trim(),
                                        profileOrganizationType.trim(),
                                        profileOrganizationDetails.trim(),
                                        profileContactPerson.trim(),
                                        profileExperience.trim(),
                                        profileAvailability.trim(),
                                        profileExpectedWage.trim(),
                                        profilePortfolio.trim()
                                    )
                                }
                                busy = false
                                if (result.isSuccess && roleResult.isSuccess) {
                                    profile = profile?.copy(
                                        fullName = profileFullName.trim(),
                                        district = profileDistrict.trim(),
                                        upazila = profileUpazila.trim(),
                                        contactPhone = profileContactPhone.trim(),
                                        currentAddress = profileCurrentAddress.trim(),
                                        permanentAddress = profilePermanentAddress.trim(),
                                        workType = profileWorkType.trim(),
                                        skills = profileSkills.trim(),
                                        avatarUrl = profileAvatarUrl.trim(),
                                        organizationName = profileOrganizationName.trim(),
                                        organizationType = profileOrganizationType.trim(),
                                        organizationDetails = profileOrganizationDetails.trim(),
                                        contactPerson = profileContactPerson.trim(),
                                        experience = profileExperience.trim(),
                                        availability = profileAvailability.trim(),
                                        expectedWage = profileExpectedWage.trim(),
                                        portfolio = profilePortfolio.trim()
                                    )
                                    jobMessage = "প্রোফাইল আপডেট হয়েছে।"
                                    showProfileForm = false
                                } else {
                                    jobMessage = if (result.isFailure) result.exceptionOrNull()?.message ?: "প্রোফাইল আপডেট হয়নি।"
                                        else roleResult.exceptionOrNull()?.message ?: "ভূমিকা অনুযায়ী প্রোফাইল আপডেট হয়নি।"
                                }
                            }
                        }
                    }) { Text(if (busy) "সংরক্ষণ হচ্ছে…" else "সংরক্ষণ করুন") }
                },
                dismissButton = { TextButton(enabled = !busy, onClick = { showProfileForm = false }) { Text("বাতিল") } }
            )
        }

        if (showAppSettings) {
            AlertDialog(
                onDismissRequest = { if (!settingsSaving) showAppSettings = false },
                title = { Text(if (profile?.role == "admin") "অ্যাপ সেটিংস / অ্যাডমিন" else "অ্যাপ সেটিংস") },
                text = {
                    Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("কাজের বাজার • সংস্করণ 1.2", fontWeight = FontWeight.Bold, color = Blue)
                        if (appAnnouncement.isNotBlank()) Text("সর্বশেষ ঘোষণা: $appAnnouncement")
                        if (supportPhone.isNotBlank()) Text("সহায়তা নম্বর: $supportPhone")
                        if (maintenanceMode) Text("রক্ষণাবেক্ষণ অবস্থা চালু আছে।", color = Color(0xFF8A4B08))
                        if (profile?.role == "admin") {
                            Divider()
                            Text("অ্যাডমিন নিয়ন্ত্রণ", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = appAnnouncement, onValueChange = { appAnnouncement = it.take(500) },
                                enabled = !settingsSaving, label = { Text("সবার জন্য অ্যাপ ঘোষণা") },
                                minLines = 2, modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = supportPhone, onValueChange = { supportPhone = it.take(20) },
                                enabled = !settingsSaving, label = { Text("সহায়তার মোবাইল নম্বর") },
                                singleLine = true, modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("রক্ষণাবেক্ষণ মোড", modifier = Modifier.weight(1f))
                                Switch(checked = maintenanceMode, onCheckedChange = { maintenanceMode = it }, enabled = !settingsSaving)
                            }
                            Text("এই পরিবর্তন সব ব্যবহারকারীর অ্যাপে দেখা যাবে।", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("সহায়তা বা অ্যাপ-সংক্রান্ত তথ্যের জন্য উপরের নম্বর ব্যবহার করুন।", style = MaterialTheme.typography.bodySmall)
                        }
                        if (profile != null) {
                            Divider()
                            Text("আপনার অ্যাকাউন্ট", fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                enabled = !busy && !settingsSaving,
                                onClick = {
                                    profileFullName = profile?.fullName.orEmpty()
                                    profileDistrict = profile?.district.orEmpty()
                                    profileUpazila = profile?.upazila.orEmpty()
                                    profileContactPhone = profile?.contactPhone?.ifBlank { profile?.authPhone.orEmpty() }.orEmpty()
                                    profileCurrentAddress = profile?.currentAddress.orEmpty()
                                    profilePermanentAddress = profile?.permanentAddress.orEmpty()
                                    profileWorkType = profile?.workType.orEmpty()
                                    profileSkills = profile?.skills.orEmpty()
                                    profileAvatarUrl = profile?.avatarUrl.orEmpty()
                                    profileOrganizationName = profile?.organizationName.orEmpty()
                                    profileOrganizationType = profile?.organizationType.orEmpty()
                                    profileOrganizationDetails = profile?.organizationDetails.orEmpty()
                                    profileContactPerson = profile?.contactPerson.orEmpty()
                                    profileExperience = profile?.experience.orEmpty()
                                    profileAvailability = profile?.availability.orEmpty()
                                    profileExpectedWage = profile?.expectedWage.orEmpty()
                                    profilePortfolio = profile?.portfolio.orEmpty()
                                    showAppSettings = false
                                    showProfileForm = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (profile?.role == "worker") "কর্মী প্রোফাইল সম্পাদনা করুন" else "প্রোফাইল সম্পাদনা করুন") }
                            if (profile?.role != "admin") {
                                OutlinedButton(
                                    enabled = !busy && !settingsSaving,
                                    onClick = { showDeleteAccountConfirm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Text("স্থায়ীভাবে অ্যাকাউন্ট ডিলিট করুন") }
                            }
                            Text("অ্যাকাউন্ট ডিলিট করলে প্রোফাইল, কাজ ও আবেদন-সংক্রান্ত ব্যক্তিগত তথ্য মুছে যাবে। পেমেন্ট রেকর্ড থাকলে আগে সহায়তা নিতে হবে।",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        } else {
                            TextButton(onClick = { showAppSettings = false; showLogin = true }) { Text("লগইন / নতুন অ্যাকাউন্ট") }
                        }
                    }
                },
                confirmButton = {
                    if (profile?.role == "admin") TextButton(enabled = !settingsSaving, onClick = {
                        val token = accessToken
                        if (token.isNullOrBlank()) {
                            jobMessage = "সেটিংস পরিবর্তনের জন্য অ্যাডমিন হিসেবে লগইন করুন।"
                        } else {
                            settingsSaving = true
                            scope.launch {
                                val result = MarketplaceRepository.updateAppSettings(token, appAnnouncement.trim(), supportPhone.trim(), maintenanceMode)
                                settingsSaving = false
                                jobMessage = if (result.isSuccess) "অ্যাপ সেটিংস সংরক্ষণ হয়েছে।" else result.exceptionOrNull()?.message ?: "সেটিংস সংরক্ষণ হয়নি।"
                                if (result.isSuccess) showAppSettings = false
                            }
                        }
                    }) { Text(if (settingsSaving) "সংরক্ষণ হচ্ছে…" else "সেটিংস সংরক্ষণ") }
                },
                dismissButton = { TextButton(enabled = !settingsSaving, onClick = { showAppSettings = false }) { Text("বন্ধ") } }
            )
        }

        if (showDeleteAccountConfirm) {
            AlertDialog(
                onDismissRequest = { if (!busy) showDeleteAccountConfirm = false },
                title = { Text("অ্যাকাউন্ট স্থায়ীভাবে ডিলিট করবেন?") },
                text = {
                    Text("এটি ফিরিয়ে আনা যাবে না। আপনার প্রোফাইল, পোস্ট করা কাজ, আবেদন ও সংশ্লিষ্ট ব্যক্তিগত তথ্য মুছে যাবে। কোনো পেমেন্ট রেকর্ড থাকলে সুরক্ষার কারণে ডিলিট বন্ধ হবে এবং সহায়তায় যোগাযোগ করতে হবে।")
                },
                confirmButton = {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            val token = accessToken
                            if (token.isNullOrBlank()) {
                                jobMessage = "অ্যাকাউন্ট ডিলিট করতে আবার লগইন করুন।"
                                showDeleteAccountConfirm = false
                            } else {
                                busy = true
                                scope.launch {
                                    val result = AuthRepository.deleteAccount(token)
                                    busy = false
                                    if (result.isSuccess) {
                                        AuthRepository.signOut(token)
                                        withContext(Dispatchers.IO) { SessionStore.clear(appContext) }
                                        signedInAccount = null
                                        accessToken = null
                                        userId = null
                                        profile = null
                                        showDeleteAccountConfirm = false
                                        showAppSettings = false
                                        jobMessage = "আপনার অ্যাকাউন্ট স্থায়ীভাবে ডিলিট হয়েছে।"
                                    } else {
                                        jobMessage = result.exceptionOrNull()?.message ?: "অ্যাকাউন্ট ডিলিট করা যায়নি।"
                                        showDeleteAccountConfirm = false
                                    }
                                }
                            }
                        }
                    ) { Text(if (busy) "ডিলিট হচ্ছে…" else "হ্যাঁ, ডিলিট করুন", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(enabled = !busy, onClick = { showDeleteAccountConfirm = false }) { Text("বাতিল") }
                }
            )
        }

        if (showMyApplicationsDialog) {
            AlertDialog(
                onDismissRequest = { if (!busy) showMyApplicationsDialog = false },
                title = { Text("আমার কাজের আবেদন") },
                text = {
                    Column(
                        Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (busy) Text("আবেদন লোড হচ্ছে…")
                        else if (myApplications.isEmpty()) Text("আপনি এখনো কোনো কাজে আবেদন করেননি।")
                        myApplications.forEach { application ->
                            Card {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(application.jobTitle, fontWeight = FontWeight.Bold)
                                    Text("${application.district}${if (application.upazila.isNotBlank()) ", ${application.upazila}" else ""}")
                                    Text("মজুরি: ৳${application.wagePaisa / 100}")
                                    Text("আবেদন: ${applicationStatusLabel(application.applicationStatus)} • কাজ: ${jobStatusLabel(application.jobStatus)}")
                                    if (application.applicationStatus == "submitted") {
                                        OutlinedButton(enabled = !busy, onClick = {
                                            val token = accessToken
                                            if (token != null) {
                                                busy = true
                                                scope.launch {
                                                    val result = MarketplaceRepository.withdrawWorkerApplication(token, application.id)
                                                    val refreshed = MarketplaceRepository.getWorkerApplications(token)
                                                    myApplications = refreshed.getOrNull().orEmpty()
                                                    busy = false
                                                    jobMessage = if (result.isSuccess) "আবেদন প্রত্যাহার করা হয়েছে।" else result.exceptionOrNull()?.message ?: "আবেদন প্রত্যাহার করা যায়নি।"
                                                }
                                            }
                                        }, modifier = Modifier.fillMaxWidth()) { Text("আবেদন প্রত্যাহার করুন") }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(enabled = !busy, onClick = { showMyApplicationsDialog = false }) { Text("বন্ধ") }
                }
            )
        }

        if (showApplicationsDialog) {
            AlertDialog(
                onDismissRequest = { if (!busy) showApplicationsDialog = false },
                title = { Text(if (selectedEmployerJob == null) "আপনার পোস্ট করা কাজ" else "কাজের আবেদন") },
                text = {
                    Column(
                        Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectedEmployerJob == null) {
                            if (busy) Text("কাজের তালিকা লোড হচ্ছে…")
                            else if (employerJobs.isEmpty()) Text("আপনার কোনো কাজ পাওয়া যায়নি।")
                            employerJobs.forEach { employerJob ->
                                Card {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(employerJob.title, fontWeight = FontWeight.Bold)
                                        Text("অবস্থা: ${jobStatusLabel(employerJob.status)}")
                                        OutlinedButton(enabled = !busy, onClick = {
                                            val token = accessToken
                                            if (token != null) {
                                                selectedEmployerJob = employerJob
                                                busy = true
                                                scope.launch {
                                                    val result = MarketplaceRepository.getEmployerApplications(token, employerJob.id)
                                                    busy = false
                                                    employerApplications = result.getOrNull().orEmpty()
                                                    if (result.isFailure) jobMessage = result.exceptionOrNull()?.message ?: "আবেদন পাওয়া যায়নি।"
                                                }
                                            }
                                        }, modifier = Modifier.fillMaxWidth()) { Text("আবেদন দেখুন") }
                                    }
                                }
                            }
                        } else {
                            Text(selectedEmployerJob?.title.orEmpty(), fontWeight = FontWeight.Bold)
                            TextButton(enabled = !busy, onClick = { selectedEmployerJob = null; employerApplications = emptyList() }) { Text("কাজের তালিকায় ফিরুন") }
                            if (busy) Text("আবেদন লোড হচ্ছে…")
                            else if (employerApplications.isEmpty()) Text("এই কাজে এখনো কোনো আবেদন আসেনি।")
                            employerApplications.forEach { application ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F7FF))) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("আবেদনকারীর কর্মী প্রোফাইল", fontWeight = FontWeight.Bold, color = Blue)
                                        Text(application.workerName.ifBlank { "নাম দেওয়া হয়নি" }, fontWeight = FontWeight.Bold)
                                        Text(listOf(application.district, application.upazila).filter { it.isNotBlank() }.joinToString(", "))
                                        if (application.avatarUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = application.avatarUrl,
                                                contentDescription = "কর্মীর প্রোফাইল ছবি",
                                                modifier = Modifier.size(64.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Text("কাজের ধরন: ${application.workType.ifBlank { "উল্লেখ নেই" }}")
                                        Text("দক্ষতা: ${application.skills.ifBlank { "উল্লেখ নেই" }}")
                                        Text("অভিজ্ঞতা: ${application.experience.ifBlank { "উল্লেখ নেই" }}")
                                        Text("কাজের সময়: ${application.availability.ifBlank { "আলোচনা সাপেক্ষ" }}")
                                        Text("প্রত্যাশিত মজুরি: ${application.expectedWage.ifBlank { "আলোচনা সাপেক্ষ" }}")
                                        if (application.message.isNotBlank()) Text("আবেদন বার্তা: ${application.message}")
                                        Text("আবেদনের অবস্থা: ${applicationStatusLabel(application.status)}")
                                        if (application.status == "submitted") {
                                            Text("এই কর্মীকে নির্বাচন করলে অন্য জমা দেওয়া আবেদনগুলো প্রত্যাখ্যাত হবে।")
                                            Button(enabled = !busy, onClick = {
                                                val token = accessToken
                                                if (token != null) {
                                                    busy = true
                                                    scope.launch {
                                                        val result = MarketplaceRepository.selectEmployerApplication(token, application.id)
                                                        busy = false
                                                        jobMessage = if (result.isSuccess) "কর্মী নির্বাচন হয়েছে। অন্য আবেদনগুলো প্রত্যাখ্যাত হয়েছে।" else result.exceptionOrNull()?.message ?: "কর্মী নির্বাচন করা যায়নি।"
                                                        val refreshed = MarketplaceRepository.getEmployerApplications(token, selectedEmployerJob?.id.orEmpty())
                                                        employerApplications = refreshed.getOrNull().orEmpty()
                                                    }
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) { Text("এই কর্মীকে নির্বাচন করুন") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(enabled = !busy, onClick = { showApplicationsDialog = false }) { Text("বন্ধ") } }
            )
        }


        if (showAdminUsersDialog) {
            AlertDialog(
                onDismissRequest = { if (!busy) showAdminUsersDialog = false },
                title = { Text("ব্যবহারকারী পরিচালনা (${adminProfiles.size})") },
                text = {
                    Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (busy) Text("তালিকা লোড হচ্ছে…")
                        if (adminProfiles.isEmpty() && !busy) Text("কোনো ব্যবহারকারী পাওয়া যায়নি।")
                        adminProfiles.forEach { person ->
                            Card {
                                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(person.fullName.ifBlank { "নাম দেওয়া হয়নি" }, fontWeight = FontWeight.Bold)
                                    Text("ভূমিকা: ${person.role} • অবস্থা: ${if (person.accountStatus == "active") "সক্রিয়" else "স্থগিত"}")
                                    Text("মোবাইল: ${person.contactPhone.ifBlank { person.phone }}")
                                    Text(listOf(person.district, person.upazila).filter { it.isNotBlank() }.joinToString(", "))
                                    OutlinedButton(enabled = !busy && person.role != "admin", onClick = {
                                        editingAdminProfile = person
                                        adminEditName = person.fullName
                                        adminEditPhone = person.contactPhone
                                        adminEditDistrict = person.district
                                        adminEditUpazila = person.upazila
                                        adminEditStatus = person.accountStatus
                                        showAdminUsersDialog = false
                                        showAdminEditDialog = true
                                    }, modifier = Modifier.fillMaxWidth()) { Text("সম্পাদনা / অ্যাকাউন্ট নিয়ন্ত্রণ") }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(enabled = !busy, onClick = { showAdminUsersDialog = false }) { Text("বন্ধ") } }
            )
        }

        if (showAdminEditDialog) {
            AlertDialog(
                onDismissRequest = { if (!busy) showAdminEditDialog = false },
                title = { Text("ব্যবহারকারী সম্পাদনা") },
                text = {
                    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(editingAdminProfile?.fullName.orEmpty(), fontWeight = FontWeight.Bold)
                        OutlinedTextField(adminEditName, { adminEditName = it.take(120) }, label = { Text("পূর্ণ নাম") }, singleLine = true)
                        OutlinedTextField(adminEditPhone, { adminEditPhone = it.take(20) }, label = { Text("যোগাযোগ নম্বর") }, singleLine = true)
                        OutlinedTextField(adminEditDistrict, { adminEditDistrict = it.take(60) }, label = { Text("জেলা") }, singleLine = true)
                        OutlinedTextField(adminEditUpazila, { adminEditUpazila = it.take(60) }, label = { Text("উপজেলা") }, singleLine = true)
                        Text("অ্যাকাউন্টের অবস্থা", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = adminEditStatus == "active", onClick = { adminEditStatus = "active" }, label = { Text("সক্রিয়") })
                            FilterChip(selected = adminEditStatus == "suspended", onClick = { adminEditStatus = "suspended" }, label = { Text("স্থগিত") })
                        }
                        Text("অ্যাডমিন অ্যাকাউন্ট এই তালিকা থেকে পরিবর্তন করা যায় না।", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(enabled = !busy && adminEditName.isNotBlank(), onClick = {
                        val token = accessToken
                        val person = editingAdminProfile
                        if (token != null && person != null) {
                            busy = true
                            scope.launch {
                                val result = MarketplaceRepository.adminUpdateProfile(
                                    token, person.id, adminEditName.trim(), adminEditPhone.trim(),
                                    adminEditDistrict.trim(), adminEditUpazila.trim(), adminEditStatus
                                )
                                busy = false
                                if (result.isSuccess) {
                                    adminProfiles = MarketplaceRepository.adminListProfiles(token).getOrNull().orEmpty()
                                    showAdminEditDialog = false
                                    jobMessage = "ব্যবহারকারীর তথ্য সংরক্ষণ হয়েছে।"
                                } else {
                                    jobMessage = result.exceptionOrNull()?.message ?: "ব্যবহারকারীর তথ্য সংরক্ষণ হয়নি।"
                                }
                            }
                        }
                    }) { Text(if (busy) "সংরক্ষণ হচ্ছে…" else "সংরক্ষণ করুন") }
                },
                dismissButton = {
                    TextButton(enabled = !busy, onClick = {
                        showAdminEditDialog = false
                        showAdminUsersDialog = true
                    }) { Text("ফিরে যান") }
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
                        val employerId = userId
                        val wage = jobWage.toLongOrNull()
                        if (jobTitle.length !in 5..120 || jobDescription.length !in 20..5000 || jobCategory.isBlank() || jobDistrict.isBlank() || jobUpazila.isBlank() || wage == null || wage <= 0) {
                            jobMessage = "সব তথ্য সঠিকভাবে পূরণ করুন। শিরোনাম ৫-১২০ এবং বিস্তারিত ২০-৫০০০ অক্ষর হতে হবে।"
                        } else if (token == null || employerId == null) {
                            jobMessage = "আবার লগইন করুন।"
                        } else {
                            busy = true
                            scope.launch {
                                val result = MarketplaceRepository.createJobForReview(token, employerId, jobTitle, jobDescription, jobCategory, jobDistrict, jobUpazila, wage)
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
