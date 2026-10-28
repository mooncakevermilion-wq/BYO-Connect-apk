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

    // Default Topics
    const val TOPIC_UPDATES = "byo_connect_updates"
    const val TOPIC_EVENTS = "byo_connect_events"

    /**
     * Helper to show a notification programmatically (used by service and JS bridge).
     */
    fun dispatchNotification(
      context: Context,
      title: String,
      body: String,
      targetUrl: String?,
      isEvent: Boolean = false
    ) {
      // Ensure all notification channels exist
      NotificationHelper.createNotificationChannels(context)

      // Runtime permission check on Android 13+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
          Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification")
          return
        }
      }

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (!targetUrl.isNullOrEmpty()) {
          putExtra(EXTRA_NOTIFICATION_URL, targetUrl)
        }
      }

      val requestCode = System.currentTimeMillis().toInt()
      val pendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val channelId = if (isEvent) {
        NotificationHelper.CHANNEL_EVENTS_ID
      } else {
        NotificationHelper.CHANNEL_UPDATES_ID
      }

      val accentColor = if (isEvent) 0xFFFF9800.toInt() else 0xFF00F2EA.toInt()

      val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_byo_app_icon)
        .setColor(accentColor)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setContentIntent(pendingIntent)

      // Add quick "View Details" action button if a URL is attached
      if (!targetUrl.isNullOrEmpty()) {
        val actionTitle = if (isEvent) {
          context.getString(R.string.notification_action_view_event)
        } else {
          context.getString(R.string.notification_action_view_event)
        }
        notificationBuilder.addAction(
          android.R.drawable.ic_menu_view,
          actionTitle,
          pendingIntent
        )
      }

      val notificationId = (System.currentTimeMillis() % 100000).toInt()
      try {
        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
      } catch (e: SecurityException) {
        Log.e(TAG, "Failed to display notification due to security exception", e)
      }
    }
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
    Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

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

    // Detect if this is a local event notification
    val isEvent = remoteMessage.data["type"]?.equals("event", ignoreCase = true) == true ||
      remoteMessage.data["category"]?.contains("event", ignoreCase = true) == true ||
      remoteMessage.from?.contains("events", ignoreCase = true) == true ||
      title.contains("Event", ignoreCase = true) ||
      body.contains("Event", ignoreCase = true)

    dispatchNotification(
      context = this,
      title = title,
      body = body,
      targetUrl = targetUrl,
      isEvent = isEvent
    )
  }
}
