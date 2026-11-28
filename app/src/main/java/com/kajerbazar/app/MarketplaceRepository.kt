package com.kajerbazar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MarketplaceRepository {
    data class Profile(
        val fullName: String, val role: String, val district: String, val upazila: String,
        val authPhone: String, val contactPhone: String, val currentAddress: String, val permanentAddress: String,
        val workType: String, val skills: String, val avatarUrl: String, val coverUrl: String,
        val organizationName: String, val organizationType: String, val organizationDetails: String,
        val contactPerson: String, val experience: String, val availability: String,
        val expectedWage: String, val portfolio: String
    )
    data class Job(val id: String, val title: String, val description: String, val category: String, val district: String, val upazila: String, val wagePaisa: Long, val createdAt: String = "")
    data class AdminProfile(
        val id: String, val fullName: String, val phone: String, val contactPhone: String,
        val district: String, val upazila: String, val role: String, val accountStatus: String
    )

    private fun request(path: String, token: String?, method: String = "GET", body: JSONObject? = null, prefer: String = "return=minimal"): Pair<Int, String> {
        val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            if (!token.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Prefer", prefer)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            return status to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getProfile(token: String, userId: String): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/profiles?select=full_name,role,district,upazila,phone,contact_phone,current_address,permanent_address,work_type,skills,avatar_url,cover_url,organization_name,organization_type,organization_details,contact_person,experience,availability,expected_wage,portfolio&id=eq.${URLEncoder.encode(userId, "UTF-8")}"
            val (status, body) = request(path, token)
            if (status !in 200..299) error("প্রোফাইল পাওয়া যায়নি ($status)")
            val rows = JSONArray(body)
            if (rows.length() == 0) error("আপনার প্রোফাইল তৈরি হয়নি।")
            val row = rows.getJSONObject(0)
            Profile(
                row.optString("full_name"), row.optString("role", "worker"),
                row.optString("district"), row.optString("upazila"),
                row.optString("phone"), row.optString("contact_phone"), row.optString("current_address"),
                row.optString("permanent_address"), row.optString("work_type"),
                row.optString("skills"), row.optString("avatar_url"), row.optString("cover_url"),
                row.optString("organization_name"), row.optString("organization_type"),
                row.optString("organization_details"), row.optString("contact_person"),
                row.optString("experience"), row.optString("availability"),
                row.optString("expected_wage"), row.optString("portfolio")
            )
        }
    }

    suspend fun updateMyProfile(
        token: String, fullName: String, contactPhone: String, district: String, upazila: String,
        currentAddress: String, permanentAddress: String, workType: String, skills: String, avatarUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("p_full_name", fullName)
                .put("p_contact_phone", contactPhone)
                .put("p_district", district)
                .put("p_upazila", upazila)
                .put("p_current_address", currentAddress)
                .put("p_permanent_address", permanentAddress)
                .put("p_work_type", workType)
                .put("p_skills", skills)
                .put("p_avatar_url", avatarUrl)
            val (status, response) = request("/rest/v1/rpc/update_my_profile", token, "POST", body)
            if (status !in 200..299) error("প্রোফাইল আপডেট হয়নি ($status): $response")
        }
    }


    suspend fun updateRoleProfile(
        token: String, organizationName: String, organizationType: String, organizationDetails: String,
        contactPerson: String, experience: String, availability: String, expectedWage: String, portfolio: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("p_organization_name", organizationName)
                .put("p_organization_type", organizationType)
                .put("p_organization_details", organizationDetails)
                .put("p_contact_person", contactPerson)
                .put("p_experience", experience)
                .put("p_availability", availability)
                .put("p_expected_wage", expectedWage)
                .put("p_portfolio", portfolio)
            val (status, response) = request("/rest/v1/rpc/update_role_profile", token, "POST", body)
            if (status !in 200..299) error("ভূমিকা অনুযায়ী প্রোফাইল আপডেট হয়নি ($status): $response")
        }
    }

    suspend fun uploadAvatar(token: String, userId: String, bytes: ByteArray, mimeType: String, extension: String): String =
        withContext(Dispatchers.IO) {
            require(bytes.isNotEmpty() && bytes.size <= 3 * 1024 * 1024) { "ছবির আকার ৩ MB-এর মধ্যে হতে হবে।" }
            require(mimeType in setOf("image/jpeg", "image/png", "image/webp")) { "শুধু JPG, PNG অথবা WebP ছবি ব্যবহার করুন।" }
            val path = "/storage/v1/object/avatars/$userId/profile.$extension"
            val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                connection.doOutput = true
                connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", mimeType)
                connection.setRequestProperty("x-upsert", "true")
                connection.outputStream.use { it.write(bytes) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) error("ছবি আপলোড হয়নি ($status): $response")
                BuildConfig.SUPABASE_URL.trimEnd() + "/storage/v1/object/public/avatars/$userId/profile.$extension"
            } finally {
                connection.disconnect()
            }
        }

    suspend fun uploadCoverPhoto(token: String, userId: String, bytes: ByteArray, mimeType: String, extension: String): String =
        withContext(Dispatchers.IO) {
            require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) { "কভার ছবির আকার ৫ MB-এর মধ্যে হতে হবে।" }
            require(mimeType in setOf("image/jpeg", "image/png", "image/webp")) { "শুধু JPG, PNG অথবা WebP ছবি ব্যবহার করুন।" }
            val path = "/storage/v1/object/avatars/$userId/cover.$extension"
            val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                connection.doOutput = true
                connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", mimeType)
                connection.setRequestProperty("x-upsert", "true")
                connection.outputStream.use { it.write(bytes) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) error("কভার ছবি আপলোড হয়নি ($status): $response")
                BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/avatars/$userId/cover.$extension"
            } finally {
                connection.disconnect()
            }
        }

    suspend fun updateCoverPhoto(token: String, coverUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("p_cover_url", coverUrl)
            val (status, response) = request("/rest/v1/rpc/update_my_cover_photo", token, "POST", body)
            if (status !in 200..299) error("কভার ছবি সংরক্ষণ হয়নি ($status): $response")
        }
    }

    suspend fun registerPushToken(token: String, userId: String, fcmToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(userId.isNotBlank()) { "ব্যবহারকারীর পরিচয় পাওয়া যায়নি।" }
            require(fcmToken.isNotBlank()) { "Firebase নোটিফিকেশন টোকেন পাওয়া যায়নি।" }
            val body = JSONObject()
                .put("user_id", userId)
                .put("fcm_token", fcmToken)
                .put("platform", "android")
            val (status, response) = request(
                "/rest/v1/device_push_tokens?on_conflict=user_id,fcm_token",
                token,
                "POST",
                body,
                "resolution=merge-duplicates,return=minimal"
            )
            if (status !in 200..299) error("নোটিফিকেশন ডিভাইস নিবন্ধন হয়নি ($status): $response")
        }
    }

    suspend fun getPublishedJobsPublic(): Result<List<Job>> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/jobs?select=id,title,description,category,district,upazila,agreed_wage_paisa,created_at&status=eq.published&order=created_at.desc&limit=50"
            val (status, body) = request(path, null)
            if (status !in 200..299) error("কাজের তালিকা পাওয়া যায়নি ($status)")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                Job(
                    row.getString("id"), row.getString("title"), row.getString("description"),
                    row.getString("category"), row.getString("district"), row.optString("upazila"),
                    row.getLong("agreed_wage_paisa"), row.optString("created_at")
                )
            }
        }
    }

    suspend fun getPublishedJobs(token: String): Result<List<Job>> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/jobs?select=id,title,description,category,district,upazila,agreed_wage_paisa,created_at&status=eq.published&order=created_at.desc&limit=50"
            val (status, body) = request(path, token)
            if (status !in 200..299) error("কাজের তালিকা পাওয়া যায়নি ($status)")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                Job(
                    row.getString("id"), row.getString("title"), row.getString("description"),
                    row.getString("category"), row.getString("district"), row.optString("upazila"),
                    row.getLong("agreed_wage_paisa"), row.optString("created_at")
                )
            }
        }
    }

    suspend fun getSavedJobIds(token: String, userId: String): Result<Set<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = URLEncoder.encode(userId, "UTF-8")
            val (status, body) = request("/rest/v1/saved_jobs?select=job_id&user_id=eq.$uid&order=created_at.desc&limit=200", token)
            if (status !in 200..299) error("সংরক্ষিত কাজ পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i -> rows.getJSONObject(i).getString("job_id") }.toSet()
        }
    }

    suspend fun setJobSaved(token: String, userId: String, jobId: String, saved: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = URLEncoder.encode(userId, "UTF-8")
            val jid = URLEncoder.encode(jobId, "UTF-8")
            val result = if (saved) {
                request("/rest/v1/saved_jobs", token, "POST",
                    JSONObject().put("user_id", userId).put("job_id", jobId))
            } else {
                request("/rest/v1/saved_jobs?user_id=eq.$uid&job_id=eq.$jid", token, "DELETE")
            }
            if (result.first !in 200..299) error("সংরক্ষিত কাজ হালনাগাদ হয়নি (${result.first}): ${result.second}")
        }
    }

    suspend fun applyToJob(token: String, jobId: String, message: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("job_id", jobId)
            if (message.isNotBlank()) body.put("message", message)
            val (status, response) = request("/rest/v1/rpc/apply_to_job", token, "POST", body)
            if (status !in 200..299) error("আবেদন জমা হয়নি ($status): $response")
        }
    }
    suspend fun submitEmployerRequest(
        token: String, businessName: String, contactName: String, district: String, upazila: String, details: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("p_business_name", businessName)
                .put("p_contact_name", contactName)
                .put("p_district", district)
                .put("p_upazila", upazila)
                .put("p_details", details)
            val (status, response) = request("/rest/v1/rpc/submit_employer_request", token, "POST", body)
            if (status !in 200..299) error("কাজদাতা আবেদন জমা হয়নি ($status): $response")
        }
    }

    suspend fun createJobForReview(
        token: String, employerId: String, title: String, description: String, category: String,
        district: String, upazila: String, wageTaka: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(wageTaka > 0) { "মজুরি শূন্যের বেশি হতে হবে।" }
            val body = JSONObject()
                .put("employer_id", employerId)
                .put("title", title)
                .put("description", description)
                .put("category", category)
                .put("district", district)
                .put("upazila", upazila)
                .put("agreed_wage_paisa", wageTaka * 100)
                .put("status", "draft")
            val (status, response) = request("/rest/v1/jobs?select=id", token, "POST", body, "return=representation")
            if (status !in 200..299) error("খসড়া কাজ তৈরি হয়নি ($status): $response")
            val rows = org.json.JSONArray(response)
            if (rows.length() == 0) error("সার্ভার কাজের আইডি ফেরত দেয়নি।")
            val jobId = rows.getJSONObject(0).getString("id")
            val publishBody = JSONObject().put("p_job_id", jobId)
            val (publishStatus, publishResponse) = request("/rest/v1/rpc/publish_job_for_review", token, "POST", publishBody)
            if (publishStatus !in 200..299) error("কাজটি পর্যালোচনার জন্য পাঠানো যায়নি ($publishStatus): $publishResponse")
        }
    }


    data class DirectoryWorker(val id: String, val fullName: String, val district: String, val upazila: String, val avatarUrl: String)

    suspend fun getWorkerDirectory(token: String): Result<List<DirectoryWorker>> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/messaging_directory?select=id,full_name,district,upazila,avatar_url&role=eq.worker&order=updated_at.desc&limit=100", token)
            if (status !in 200..299) error("কর্মীর তালিকা পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                DirectoryWorker(row.getString("id"), row.optString("full_name"), row.optString("district"), row.optString("upazila"), row.optString("avatar_url"))
            }
        }
    }

    suspend fun getSavedWorkerIds(token: String): Result<Set<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/saved_workers?select=worker_id&order=created_at.desc&limit=300", token)
            if (status !in 200..299) error("সংরক্ষিত কর্মীর তালিকা পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i -> rows.getJSONObject(i).getString("worker_id") }.toSet()
        }
    }

    suspend fun setWorkerSaved(token: String, employerId: String, workerId: String, saved: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val employer = URLEncoder.encode(employerId, "UTF-8")
            val worker = URLEncoder.encode(workerId, "UTF-8")
            val result = if (saved) request("/rest/v1/saved_workers", token, "POST",
                JSONObject().put("employer_id", employerId).put("worker_id", workerId))
            else request("/rest/v1/saved_workers?employer_id=eq.$employer&worker_id=eq.$worker", token, "DELETE")
            if (result.first !in 200..299) error("পছন্দের কর্মী তালিকা হালনাগাদ হয়নি (${result.first}): ${result.second}")
        }
    }

    data class SupportTicket(
        val id: String, val category: String, val subject: String, val body: String,
        val status: String, val adminReply: String, val createdAt: String
    )

    suspend fun getSupportTickets(token: String): Result<List<SupportTicket>> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/support_tickets?select=id,category,subject,body,status,admin_reply,created_at&order=created_at.desc&limit=50", token)
            if (status !in 200..299) error("অভিযোগ ও সহায়তার তালিকা পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                SupportTicket(row.getString("id"), row.optString("category"), row.optString("subject"),
                    row.optString("body"), row.optString("status"), row.optString("admin_reply"), row.optString("created_at"))
            }
        }
    }

    suspend fun createSupportTicket(token: String, userId: String, category: String, subject: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(category in setOf("account","job","worker","employer","message","safety","other")) { "সঠিক বিভাগ নির্বাচন করুন।" }
            require(subject.trim().length in 3..120) { "বিষয় ৩ থেকে ১২০ অক্ষরের মধ্যে লিখুন।" }
            require(body.trim().length in 10..3000) { "বিস্তারিত ১০ থেকে ৩০০০ অক্ষরের মধ্যে লিখুন।" }
            val (status, response) = request("/rest/v1/support_tickets", token, "POST",
                JSONObject().put("user_id", userId).put("category", category).put("subject", subject.trim()).put("body", body.trim()))
            if (status !in 200..299) error("অভিযোগ/সহায়তা পাঠানো যায়নি ($status): $response")
        }
    }

    data class AppSettings(val announcement: String, val supportPhone: String, val maintenanceMode: Boolean)

    suspend fun getAppSettings(): Result<AppSettings> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/app_settings?select=announcement,support_phone,maintenance_mode&id=eq.1", null)
            if (status !in 200..299) error("অ্যাপ সেটিংস পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            if (rows.length() == 0) error("অ্যাপ সেটিংস পাওয়া যায়নি।")
            val row = rows.getJSONObject(0)
            AppSettings(row.optString("announcement"), row.optString("support_phone"), row.optBoolean("maintenance_mode"))
        }
    }

    suspend fun updateAppSettings(token: String, announcement: String, supportPhone: String, maintenanceMode: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(announcement.length <= 500 && supportPhone.length <= 20) { "ঘোষণা বা নম্বরের দৈর্ঘ্য বেশি।" }
            val body = JSONObject().put("announcement", announcement).put("support_phone", supportPhone)
                .put("maintenance_mode", maintenanceMode)
            val (status, response) = request("/rest/v1/app_settings?id=eq.1", token, "PATCH", body)
            if (status !in 200..299) error("অ্যাপ সেটিংস সংরক্ষণ হয়নি ($status): $response")
        }
    }

    data class EmployerRequest(val id: String, val businessName: String, val contactName: String, val district: String, val upazila: String)
    data class EmployerJob(val id: String, val title: String, val status: String)
    data class EmployerApplication(
        val id: String, val workerName: String, val district: String, val upazila: String,
        val message: String, val status: String,
        val workType: String, val skills: String, val experience: String,
        val availability: String, val expectedWage: String, val avatarUrl: String
    )
    data class WorkerApplication(
        val id: String, val jobTitle: String, val district: String, val upazila: String,
        val wagePaisa: Long, val applicationStatus: String, val jobStatus: String
    )

    suspend fun getWorkerApplications(token: String): Result<List<WorkerApplication>> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/rpc/get_worker_applications", token, "POST", JSONObject())
            if (status !in 200..299) error("আপনার আবেদনের তালিকা পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                WorkerApplication(
                    row.getString("application_id"), row.optString("job_title"),
                    row.optString("district"), row.optString("upazila"),
                    row.optLong("agreed_wage_paisa"), row.optString("application_status"),
                    row.optString("job_status")
                )
            }
        }
    }

    suspend fun withdrawWorkerApplication(token: String, applicationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/rpc/withdraw_job_application", token, "POST",
                JSONObject().put("p_application_id", applicationId))
            if (status !in 200..299) error("আবেদন প্রত্যাহার করা যায়নি ($status): $body")
        }
    }

    suspend fun getEmployerJobs(token: String, employerId: String): Result<List<EmployerJob>> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedId = URLEncoder.encode(employerId, "UTF-8")
            val (status, body) = request("/rest/v1/jobs?select=id,title,status&employer_id=eq.$encodedId&order=created_at.desc&limit=50", token)
            if (status !in 200..299) error("আপনার কাজের তালিকা পাওয়া যায়নি ($status)")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                EmployerJob(row.getString("id"), row.getString("title"), row.optString("status"))
            }
        }
    }

    suspend fun getEmployerApplications(token: String, jobId: String): Result<List<EmployerApplication>> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/rpc/get_employer_applications", token, "POST", JSONObject().put("p_job_id", jobId))
            if (status !in 200..299) error("আবেদনকারীদের তালিকা পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                EmployerApplication(
                    row.getString("application_id"), row.optString("worker_name"),
                    row.optString("worker_district"), row.optString("worker_upazila"),
                    row.optString("message"), row.optString("status"),
                    row.optString("worker_work_type"), row.optString("worker_skills"),
                    row.optString("worker_experience"), row.optString("worker_availability"),
                    row.optString("worker_expected_wage"), row.optString("worker_avatar_url")
                )
            }
        }
    }

    suspend fun selectEmployerApplication(token: String, applicationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/rpc/select_job_application", token, "POST",
                JSONObject().put("p_application_id", applicationId))
            if (status !in 200..299) error("কর্মী নির্বাচন করা যায়নি ($status): $body")
        }
    }

    suspend fun getAdminQueues(token: String): Result<Pair<List<Job>, List<EmployerRequest>>> = withContext(Dispatchers.IO) {
        runCatching {
            val (jobStatus, jobBody) = request("/rest/v1/jobs?select=id,title,description,category,district,upazila,agreed_wage_paisa&status=eq.pending_review&order=created_at.asc&limit=100", token)
            if (jobStatus !in 200..299) error("Pending job queue unavailable ($jobStatus)")
            val jobRows = JSONArray(jobBody)
            val pendingJobs = (0 until jobRows.length()).map { i ->
                val row = jobRows.getJSONObject(i)
                Job(row.getString("id"), row.getString("title"), row.getString("description"), row.getString("category"),
                    row.getString("district"), row.optString("upazila"), row.getLong("agreed_wage_paisa"))
            }
            val (requestStatus, requestBody) = request("/rest/v1/employer_requests?select=id,business_name,contact_name,district,upazila&status=eq.pending&order=created_at.asc&limit=100", token)
            if (requestStatus !in 200..299) error("Employer request queue unavailable ($requestStatus)")
            val requestRows = JSONArray(requestBody)
            val employerRequests = (0 until requestRows.length()).map { i ->
                val row = requestRows.getJSONObject(i)
                EmployerRequest(row.getString("id"), row.getString("business_name"), row.getString("contact_name"),
                    row.getString("district"), row.getString("upazila"))
            }
            pendingJobs to employerRequests
        }
    }

    suspend fun adminListProfiles(token: String): Result<List<AdminProfile>> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/rpc/admin_list_profiles", token, "POST", JSONObject())
            if (status !in 200..299) error("ব্যবহারকারী তালিকা পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                AdminProfile(
                    row.optString("id"), row.optString("full_name"), row.optString("phone"),
                    row.optString("contact_phone"), row.optString("district"), row.optString("upazila"),
                    row.optString("role"), row.optString("account_status", "active")
                )
            }
        }
    }

    suspend fun adminUpdateProfile(
        token: String, userId: String, fullName: String, contactPhone: String,
        district: String, upazila: String, accountStatus: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("p_user_id", userId)
                .put("p_full_name", fullName)
                .put("p_contact_phone", contactPhone)
                .put("p_district", district)
                .put("p_upazila", upazila)
                .put("p_account_status", accountStatus)
            val (status, response) = request("/rest/v1/rpc/admin_update_profile", token, "POST", body)
            if (status !in 200..299) error("ব্যবহারকারী আপডেট হয়নি ($status): $response")
        }
    }

    suspend fun adminReviewJob(token: String, jobId: String, approve: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/rpc/admin_review_job", token, "POST",
                JSONObject().put("p_job_id", jobId).put("p_approve", approve))
            if (status !in 200..299) error("Job review failed ($status): $body")
        }
    }

    suspend fun adminReviewEmployerRequest(token: String, requestId: String, approve: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/rpc/admin_review_employer_request", token, "POST",
                JSONObject().put("p_request_id", requestId).put("p_approve", approve))
            if (status !in 200..299) error("Employer review failed ($status): $body")
        }
    }


    data class CommunityNotification(val id: String, val title: String, val body: String, val kind: String, val createdAt: String, val readAt: String?)
    suspend fun getCommunityNotifications(token: String, userId: String): Result<List<CommunityNotification>> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/notifications?select=id,title,body,kind,created_at,read_at&recipient_id=eq.${URLEncoder.encode(userId, "UTF-8")}&order=created_at.desc&limit=50"
            val (status, body) = request(path, token)
            if (status !in 200..299) error("নোটিফিকেশন পাওয়া যায়নি ($status): $body")
            val rows = JSONArray(body)
            (0 until rows.length()).map { i -> rows.getJSONObject(i).let { CommunityNotification(it.optString("id"), it.optString("title"), it.optString("body"), it.optString("kind"), it.optString("created_at"), it.optString("read_at").takeIf { v -> v.isNotBlank() && v != "null" }) } }
        }
    }
    suspend fun markNotificationRead(token: String, userId: String, notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/notifications?id=eq.${URLEncoder.encode(notificationId, "UTF-8")}&recipient_id=eq.${URLEncoder.encode(userId, "UTF-8")}"
            val readAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
            val (status, body) = request(path, token, "PATCH", JSONObject().put("read_at", readAt))
            if (status !in 200..299) error("নোটিফিকেশন পঠিত করা যায়নি ($status): $body")
        }
    }

    suspend fun markAllNotificationsRead(token: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/notifications?recipient_id=eq.${URLEncoder.encode(userId, "UTF-8")}&read_at=is.null"
            val readAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
            val (status, body) = request(path, token, "PATCH", JSONObject().put("read_at", readAt))
            if (status !in 200..299) error("সব নোটিফিকেশন পঠিত করা যায়নি ($status): $body")
        }
    }

    suspend fun followUser(token: String, followerId: String, followingId: String, follow: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "/rest/v1/user_follows?follower_id=eq.${URLEncoder.encode(followerId, "UTF-8")}&following_id=eq.${URLEncoder.encode(followingId, "UTF-8")}"
            val (status, body) = if (follow) request("/rest/v1/user_follows", token, "POST", JSONObject().put("follower_id", followerId).put("following_id", followingId)) else request(path, token, "DELETE")
            if (status !in 200..299) error("ফলো আপডেট হয়নি ($status): $body")
        }
    }
    suspend fun createJobAlert(token: String, userId: String, category: String, district: String, upazila: String, keyword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("user_id", userId).put("active", true)
            if (category.isNotBlank()) body.put("category", category)
            if (district.isNotBlank()) body.put("district", district)
            if (upazila.isNotBlank()) body.put("upazila", upazila)
            if (keyword.isNotBlank()) body.put("keyword", keyword)
            val (status, response) = request("/rest/v1/job_alerts", token, "POST", body)
            if (status !in 200..299) error("জব অ্যালার্ট সংরক্ষণ হয়নি ($status): $response")
        }
    }
    suspend fun submitVerification(token: String, userId: String, details: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/worker_verification_requests", token, "POST", JSONObject().put("worker_id", userId).put("details", details))
            if (status !in 200..299) error("ভেরিফিকেশন আবেদন জমা হয়নি ($status): $body")
        }
    }
    suspend fun submitContentReport(token: String, reporterId: String, targetType: String, targetId: String, reason: String, details: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (status, body) = request("/rest/v1/content_reports", token, "POST", JSONObject().put("reporter_id", reporterId).put("target_type", targetType).put("target_id", targetId).put("reason", reason).put("details", details))
            if (status !in 200..299) error("রিপোর্ট জমা হয়নি ($status): $body")
        }
    }

}
