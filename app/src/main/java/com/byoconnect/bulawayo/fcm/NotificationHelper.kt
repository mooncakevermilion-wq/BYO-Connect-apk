package com.byoconnect.bulawayo.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import com.byoconnect.bulawayo.R

object NotificationHelper {
  const val CHANNEL_ID = "byo_connect_notifications"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = context.getString(R.string.notification_channel_name)
      val descriptionText = context.getString(R.string.notification_channel_desc)
      val importance = NotificationManager.IMPORTANCE_HIGH

      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
        enableLights(true)
        lightColor = Color.parseColor("#00F2EA")
        enableVibration(true)
        setShowBadge(true)
      }

      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }
}
