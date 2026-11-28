package com.kajerbazar.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

class KajerBazarFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceJob = kotlinx.coroutines.SupervisorJob()
    private val serviceScope = kotlinx.coroutines.CoroutineScope(serviceJob + kotlinx.coroutines.Dispatchers.IO)

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Firebase may rotate the device token at any time. Re-register it using
        // a freshly refreshed Supabase access token and the encrypted saved session.
        val saved = SessionStore.load(applicationContext) ?: return
        serviceScope.launch {
            runCatching {
                val refreshed = AuthRepository.refreshSession(saved.refreshToken)
                val accessToken = refreshed.accessToken
                    ?: error(refreshed.message.ifBlank { "সেশন যাচাই করা যায়নি।" })
                val userId = refreshed.userId?.takeIf { it.isNotBlank() } ?: saved.userId
                val newRefresh = refreshed.refreshToken?.takeIf { it.isNotBlank() } ?: saved.refreshToken
                SessionStore.save(
                    applicationContext,
                    SessionStore.SavedSession(newRefresh, userId, refreshed.phone ?: refreshed.email ?: saved.account)
                )
                MarketplaceRepository.registerPushToken(accessToken, userId, token).getOrThrow()
            }.onFailure { error ->
                android.util.Log.w("KajerBazarPush", "Refreshed Firebase token registration failed", error)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "কাজের বাজার"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
        if (!body.isNullOrBlank()) showNotification(title, body, message.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val channelId = "kajer_bazar_updates"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "কাজের বাজার আপডেট", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "কাজ, মেসেজ ও সামাজিক আপডেট" }
            )
        }
        val deepLink = data["deep_link"]
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!deepLink.isNullOrBlank()) {
                setData(android.net.Uri.parse(deepLink))
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (_: SecurityException) {
            // Notification permission may be denied by the user.
        }
    }
}