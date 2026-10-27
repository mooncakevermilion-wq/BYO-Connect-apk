package com.byoconnect.bulawayo.fcm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.byoconnect.bulawayo.MainActivity
import com.byoconnect.bulawayo.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ByoFirebaseMessagingService : FirebaseMessagingService() {

  companion object {
    private const val TAG = "ByoFCMService"
    const val PREFS_NAME = "byo_connect_fcm_prefs"
    const val KEY_FCM_TOKEN = "fcm_token"
    const val EXTRA_NOTIFICATION_URL = "extra_notification_url"
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d(TAG, "New FCM Token received: $token")

    // Persist token in SharedPreferences for WebView JavaScript Interface bridge
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    Log.d(TAG, "From: ${remoteMessage.from}")

    // Extract title, body, and custom URL from notification or data payload
    val title = remoteMessage.notification?.title
      ?: remoteMessage.data["title"]
      ?: getString(R.string.app_name)

    val body = remoteMessage.notification?.body
      ?: remoteMessage.data["body"]
      ?: remoteMessage.data["message"]
      ?: "New update from BYO Connect"

    val targetUrl = remoteMessage.data["url"]
      ?: remoteMessage.data["link"]
      ?: remoteMessage.data["target_url"]

    showNotification(title, body, targetUrl)
  }

  private fun showNotification(title: String, body: String, targetUrl: String?) {
    // Ensure channel is created
    NotificationHelper.createNotificationChannel(this)

    // Check runtime notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.POST_NOTIFICATIONS
      )
      if (permission != PackageManager.PERMISSION_GRANTED) {
        Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification")
        return
      }
    }

    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      if (!targetUrl.isNullOrEmpty()) {
        putExtra(EXTRA_NOTIFICATION_URL, targetUrl)
      }
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      System.currentTimeMillis().toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationBuilder = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_byo_logo)
      .setColor(0xFF00F2EA.toInt())
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setContentIntent(pendingIntent)

    val notificationId = (System.currentTimeMillis() % 100000).toInt()
    try {
      NotificationManagerCompat.from(this).notify(notificationId, notificationBuilder.build())
    } catch (e: SecurityException) {
      Log.e(TAG, "Failed to display notification due to security exception", e)
    }
  }
}
