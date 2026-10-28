package com.byoconnect.bulawayo.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import com.byoconnect.bulawayo.R

object NotificationHelper {
  const val CHANNEL_UPDATES_ID = "byo_connect_notifications"
  const val CHANNEL_EVENTS_ID = "byo_connect_events"

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      // Channel 1: Platform updates & community announcements
      val updatesName = context.getString(R.string.notification_channel_name)
      val updatesDesc = context.getString(R.string.notification_channel_desc)
      val updatesChannel = NotificationChannel(
        CHANNEL_UPDATES_ID,
        updatesName,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = updatesDesc
        enableLights(true)
        lightColor = Color.parseColor("#00F2EA")
        enableVibration(true)
        setShowBadge(true)
      }

      // Channel 2: Local events, meetups, and gatherings in Bulawayo
      val eventsName = context.getString(R.string.notification_channel_events_name)
      val eventsDesc = context.getString(R.string.notification_channel_events_desc)
      val eventsChannel = NotificationChannel(
        CHANNEL_EVENTS_ID,
        eventsName,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = eventsDesc
        enableLights(true)
        lightColor = Color.parseColor("#FF9800")
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 250, 150, 250)
        setShowBadge(true)
      }

      notificationManager.createNotificationChannel(updatesChannel)
      notificationManager.createNotificationChannel(eventsChannel)
    }
  }

  @Deprecated("Use createNotificationChannels", ReplaceWith("createNotificationChannels(context)"))
  fun createNotificationChannel(context: Context) {
    createNotificationChannels(context)
  }
}
