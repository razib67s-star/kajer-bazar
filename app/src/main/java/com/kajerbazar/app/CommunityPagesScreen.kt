package com.kajerbazar.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CommunityPagesScreen(token:String?,userId:String?,onLogin:()->Unit) {
    val scope=rememberCoroutineScope()
    var pages by remember { mutableStateOf<List<CommunityPagesRepository.Page>>(emptyList()) }
    var selected by remember { mutableStateOf<CommunityPagesRepository.Page?>(null) }
    var posts by remember { mutableStateOf<List<CommunityPagesRepository.PagePost>>(emptyList()) }
    var feedPosts by remember { mutableStateOf<List<CommunityPagesRepository.PagePost>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("পেজ লোড হচ্ছে…") }
    var showCreate by remember { mutableStateOf(false) }
    var pageName by remember { mutableStateOf("") }
    var pageCategory by remember { mutableStateOf("") }
    var pageDescription by remember { mutableStateOf("") }
    var postText by remember { mutableStateOf("") }

    fun loadPages() {
        scope.launch {
            loading=true
            CommunityPagesRepository.pages(token,userId).onSuccess { list ->
                pages=list; status=if(list.isEmpty()) "এখনো কোনো পেজ নেই। নতুন পেজ তৈরি করুন।" else "পেজের তালিকা"
                selected=list.firstOrNull { it.id==selected?.id } ?: selected ?: list.firstOrNull()
            }.onFailure { status=it.message ?: "পেজ লোড হয়নি।" }
            loading=false
        }
    }
    fun loadPosts(page:CommunityPagesRepository.Page?) {
        if(page==null){posts=emptyList();return}
        scope.launch { CommunityPagesRepository.posts(token,page.id,page.name).onSuccess{posts=it}.onFailure{status=it.message ?: "পোস্ট লোড হয়নি"} }
    }
    LaunchedEffect(token,userId) { loadPages() }
    LaunchedEffect(selected?.id) { loadPosts(selected) }

    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("পেজ",style=MaterialTheme.typography.headlineSmall)
        Text("পেজ তৈরি করুন, অনুসরণ করুন এবং পেজের পোস্ট দেখুন।",style=MaterialTheme.typography.bodyMedium)
        Text(status,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={showCreate=true},enabled=!loading) { Text("＋ নতুন পেজ তৈরি") }
            OutlinedButton(onClick={loadPages},enabled=!loading) { Text("রিফ্রেশ") }
        }
        if(showCreate) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("নতুন পেজ",style=MaterialTheme.typography.titleLarge)
                    OutlinedTextField(pageName,{pageName=it.take(80)},label={Text("পেজের নাম")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                    OutlinedTextField(pageCategory,{pageCategory=it.take(60)},label={Text("বিভাগ (যেমন ব্যবসা, কমিউনিটি)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                    OutlinedTextField(pageDescription,{pageDescription=it.take(1000)},label={Text("পেজ পরিচিতি")},modifier=Modifier.fillMaxWidth(),minLines=2)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={
                            val t=token;val u=userId
                            if(t.isNullOrBlank()||u.isNullOrBlank()) onLogin()
                            else scope.launch {
                                loading=true
                                CommunityPagesRepository.createPage(t,u,pageName,pageCategory,pageDescription).onSuccess {
                                    status="পেজ তৈরি হয়েছে।";pageName="";pageCategory="";pageDescription="";showCreate=false;loadPages()
                                }.onFailure { status=it.message ?: "পেজ তৈরি হয়নি" }
                                loading=false
                            }
                        },enabled=!loading&&pageName.trim().length>=2) { Text("তৈরি করুন") }
                        OutlinedButton(onClick={showCreate=false}) { Text("বাতিল") }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("পেজ নিউজ ফিড",style=MaterialTheme.typography.titleLarge)
                if(feedPosts.isEmpty()) Text("পেজের পোস্ট প্রকাশ হলে এখানে দেখা যাবে।")
                feedPosts.forEach { item ->
                    Text(item.pageName,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                    Text(item.body)
                    HorizontalDivider()
                }
            }
        }
        if(pages.isEmpty()) Text("আপনার বা অন্যদের পেজ এখানে দেখা যাবে।",style=MaterialTheme.typography.bodyMedium)
        pages.forEach { page ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(page.name,style=MaterialTheme.typography.titleMedium)
                    Text(page.category.ifBlank{"সাধারণ"}+" · ${page.followerCount} অনুসারী",style=MaterialTheme.typography.bodySmall)
                    if(page.description.isNotBlank()) Text(page.description)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={selected=page},enabled=selected?.id!=page.id) { Text("পেজ দেখুন") }
                        if(userId!=null&&page.ownerId!=userId) Button(onClick={
                            val t=token;val u=userId
                            if(t.isNullOrBlank())onLogin() else scope.launch {
                                loading=true
                                CommunityPagesRepository.follow(t,u,page.id,!page.followedByMe).onSuccess { status=if(page.followedByMe)"আনফলো করা হয়েছে" else "পেজ ফলো করেছেন";loadPages() }.onFailure{status=it.message ?: "ফলো আপডেট ব্যর্থ"}
                                loading=false
                            },enabled=!loading) { Text(if(page.followedByMe)"ফলো করা আছে" else "ফলো") }
                        }
                    }
                }
            }
        }
        selected?.let { page ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("পেজ: ${page.name}",style=MaterialTheme.typography.titleLarge)
                    Text(page.description.ifBlank{"এই পেজের পরিচিতি যোগ করা হয়নি।"})
                    if(userId==page.ownerId) {
                        Text("আপনি এই পেজের মালিক।",color=MaterialTheme.colorScheme.primary)
                        OutlinedTextField(postText,{postText=it.take(2000)},label={Text("পেজ থেকে পোস্ট লিখুন")},modifier=Modifier.fillMaxWidth(),minLines=2)
                        Button(onClick={
                            val t=token;val u=userId
                            if(t.isNullOrBlank()||u.isNullOrBlank())onLogin() else scope.launch {
                                loading=true
                                CommunityPagesRepository.publish(t,u,page.id,postText).onSuccess{postText="";status="পেজ পোস্ট প্রকাশ হয়েছে";loadPosts(page)}.onFailure{status=it.message ?: "পোস্ট হয়নি"}
                                loading=false
                            },enabled=!loading&&postText.trim().isNotEmpty()) { Text("পেজ থেকে পোস্ট প্রকাশ") }
                    }
                    Text("পেজের পোস্ট",style=MaterialTheme.typography.titleMedium)
                    if(posts.isEmpty()) Text("এই পেজে এখনো কোনো পোস্ট নেই।")
                    posts.forEach { p ->
                        Text(p.body)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
