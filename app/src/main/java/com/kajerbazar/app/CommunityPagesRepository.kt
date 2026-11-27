package com.kajerbazar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object CommunityPagesRepository {
    data class Page(val id:String, val ownerId:String, val name:String, val slug:String, val category:String,
        val description:String, val avatarUrl:String, val coverUrl:String, val followerCount:Int, val followedByMe:Boolean)
    data class PagePost(val id:String, val pageId:String, val body:String, val createdAt:String)

    private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
    private fun request(path:String, token:String?, method:String="GET", body:JSONObject?=null, prefer:String="return=minimal"):Pair<Int,String> {
        val c=(URL(BuildConfig.SUPABASE_URL.trimEnd('/')+path).openConnection() as HttpURLConnection)
        try {
            c.requestMethod=method; c.connectTimeout=15000; c.readTimeout=20000
            c.setRequestProperty("apikey",BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (!token.isNullOrBlank()) c.setRequestProperty("Authorization","Bearer $token")
            c.setRequestProperty("Accept","application/json")
            if (body!=null) { c.doOutput=true; c.setRequestProperty("Content-Type","application/json"); c.setRequestProperty("Prefer",prefer); c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) } }
            val code=c.responseCode; val stream=if(code in 200..299)c.inputStream else c.errorStream
            return code to (stream?.bufferedReader()?.use{it.readText()}.orEmpty())
        } finally { c.disconnect() }
    }

    suspend fun pages(token:String?, userId:String?):Result<List<Page>> = withContext(Dispatchers.IO) { runCatching {
        val (code,body)=request("/rest/v1/community_pages?select=id,owner_id,name,slug,category,description,avatar_url,cover_url&order=created_at.desc&limit=100",token)
        if(code !in 200..299) error("পেজ লোড হয়নি ($code): $body")
        val rows=JSONArray(body)
        val following=if(!token.isNullOrBlank()&&!userId.isNullOrBlank()) {
            val (fcode,fb)=request("/rest/v1/community_page_followers?select=page_id&user_id=eq.${enc(userId)}",token)
            if(fcode in 200..299) { val f=JSONArray(fb); (0 until f.length()).map{f.getJSONObject(it).optString("page_id")}.toSet() } else emptySet()
        } else emptySet()
        (0 until rows.length()).map { i ->
            val row=rows.getJSONObject(i); val id=row.getString("id")
            val (cc,cb)=request("/rest/v1/community_page_followers?select=page_id&page_id=eq.${enc(id)}",token)
            val count=if(cc in 200..299) JSONArray(cb).length() else 0
            Page(id,row.optString("owner_id"),row.optString("name"),row.optString("slug"),row.optString("category"),row.optString("description"),row.optString("avatar_url"),row.optString("cover_url"),count,id in following)
        }
    } }

    suspend fun createPage(token:String, ownerId:String, name:String, category:String, description:String):Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val clean=name.trim(); require(clean.length in 2..80) { "পেজের নাম ২ থেকে ৮০ অক্ষর লিখুন।" }
        val slug=clean.lowercase().replace(Regex("[^a-z0-9\\u0980-\\u09ff]+"),"-").trim('-').take(90).ifBlank{"page"}+"-"+ownerId.take(6)
        val json=JSONObject().put("owner_id",ownerId).put("name",clean).put("slug",slug).put("category",category.trim().ifBlank{"সাধারণ"}).put("description",description.trim().take(1000))
        val (code,body)=request("/rest/v1/community_pages",token,"POST",json)
        if(code !in 200..299) error("পেজ তৈরি হয়নি ($code): $body")
    } }

    suspend fun follow(token:String,userId:String,pageId:String,follow:Boolean):Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val path="/rest/v1/community_page_followers?page_id=eq.${enc(pageId)}&user_id=eq.${enc(userId)}"
        val (code,body)=if(follow) request("/rest/v1/community_page_followers",token,"POST",JSONObject().put("page_id",pageId).put("user_id",userId)) else request(path,token,"DELETE")
        if(code !in 200..299) error(if(follow)"ফলো করা যায়নি ($code): $body" else "আনফলো করা যায়নি ($code): $body")
    } }

    suspend fun posts(token:String?,pageId:String):Result<List<PagePost>> = withContext(Dispatchers.IO) { runCatching {
        val (code,body)=request("/rest/v1/community_page_posts?select=id,page_id,body,created_at&page_id=eq.${enc(pageId)}&order=created_at.desc&limit=100",token)
        if(code !in 200..299) error("পেজ পোস্ট লোড হয়নি ($code): $body")
        val rows=JSONArray(body); (0 until rows.length()).map{val r=rows.getJSONObject(it);PagePost(r.optString("id"),r.optString("page_id"),r.optString("body"),r.optString("created_at"))}
    } }

    suspend fun publish(token:String,ownerId:String,pageId:String,body:String):Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val clean=body.trim(); require(clean.length in 1..2000){"পোস্ট ১ থেকে ২০০০ অক্ষরের মধ্যে লিখুন।"}
        val (code,response)=request("/rest/v1/community_page_posts",token,"POST",JSONObject().put("page_id",pageId).put("author_id",ownerId).put("body",clean))
        if(code !in 200..299) error("পোস্ট প্রকাশ হয়নি ($code): $response")
    } }
}
