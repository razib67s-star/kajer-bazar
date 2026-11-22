package com.kajerbazar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    LaunchedEffect(Unit) {
        connectionStatus = withContext(Dispatchers.IO) { SupabaseConnection.checkApi() }
    }
    var showLogin by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var pendingPhone by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf("") }
    var signedInPhone by remember { mutableStateOf<String?>(null) }
    var accessToken by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                item {
                    Card { Text(connectionStatus, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Column(Modifier.padding(14.dp)) {
                            Text("পরীক্ষামূলক সংস্করণ", fontWeight = FontWeight.Bold, color = Color(0xFF8A4B08))
                            Text("মোবাইল OTP লগইন সংযুক্ত করা হয়েছে। SMS পাঠাতে Supabase-এ Phone Auth ও SMS Provider সক্রিয় থাকতে হবে।")
                            Text("বাস্তব কাজের তালিকা ও আবেদন ব্যবস্থা এখনো সংযুক্ত করা বাকি।")
                        }
                    }
                }
                item {
                    Text("কাজের বাজারে স্বাগতম", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("বাংলাদেশের কর্মী ও কাজদাতাদের জন্য একটি নিরাপদ কাজের প্ল্যাটফর্ম।")
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { showLogin = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (signedInPhone == null) "মোবাইল নম্বর দিয়ে শুরু করুন" else "লগইন তথ্য দেখুন")
                    }
                }
                item { PaymentScreen() }
                item {
                    Text("নিরাপত্তা নির্দেশনা", fontWeight = FontWeight.Bold)
                    Text("কাউকে OTP, পাসওয়ার্ড, ব্যাংক তথ্য বা অগ্রিম টাকা দেবেন না। কাজের শর্ত ও পারিশ্রমিক যাচাই করে নিন।")
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
                            Text("এই সেশনে নিরাপদ অ্যাক্সেস টোকেন সক্রিয় আছে। অ্যাপ বন্ধ করলে আবার লগইন করতে হবে।")
                        } else {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                enabled = !otpSent && !busy,
                                label = { Text("মোবাইল নম্বর") },
                                placeholder = { Text("01XXXXXXXXX") },
                                singleLine = true
                            )
                            if (otpSent) {
                                OutlinedTextField(
                                    value = otp,
                                    onValueChange = { otp = it.filter(Char::isDigit).take(8) },
                                    enabled = !busy,
                                    label = { Text("SMS OTP কোড") },
                                    singleLine = true
                                )
                            }
                        }
                        if (authMessage.isNotBlank()) Text(authMessage, color = if (authMessage.contains("সম্পন্ন")) Color(0xFF18733B) else MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    if (signedInPhone != null) {
                        TextButton(onClick = {
                            signedInPhone = null
                            accessToken = null
                            showLogin = false
                        }) { Text("লগআউট") }
                    } else if (!otpSent) {
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            authMessage = ""
                            scope.launch {
                                val result = AuthRepository.sendPhoneOtp(phone)
                                busy = false
                                authMessage = result.message
                                if (result.success) {
                                    pendingPhone = result.phone.orEmpty()
                                    otpSent = true
                                }
                            }
                        }) { Text(if (busy) "অপেক্ষা করুন…" else "OTP পাঠান") }
                    } else {
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            authMessage = ""
                            scope.launch {
                                val result = AuthRepository.verifyPhoneOtp(pendingPhone, otp)
                                busy = false
                                authMessage = result.message
                                if (result.success) {
                                    signedInPhone = result.phone
                                    accessToken = result.accessToken
                                    showLogin = false
                                    otpSent = false
                                    otp = ""
                                }
                            }
                        }) { Text(if (busy) "যাচাই হচ্ছে…" else "OTP যাচাই করুন") }
                    }
                },
                dismissButton = {
                    TextButton(enabled = !busy, onClick = {
                        if (otpSent && signedInPhone == null) {
                            otpSent = false
                            otp = ""
                            authMessage = ""
                        } else showLogin = false
                    }) { Text(if (otpSent && signedInPhone == null) "নম্বর বদলান" else "বন্ধ") }
                }
            )
        }
    }
}
