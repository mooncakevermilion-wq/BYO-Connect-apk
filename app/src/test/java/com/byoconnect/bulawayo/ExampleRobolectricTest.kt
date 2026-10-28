package com.byoconnect.bulawayo

import android.app.NotificationManager
import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.byoconnect.bulawayo.fcm.ByoFirebaseMessagingService
import com.byoconnect.bulawayo.fcm.NotificationHelper
import com.byoconnect.bulawayo.ui.theme.BackgroundDark
import com.byoconnect.bulawayo.ui.theme.BackgroundLight
import com.byoconnect.bulawayo.ui.theme.PrimaryDark
import com.byoconnect.bulawayo.ui.theme.PrimaryLight
import com.byoconnect.bulawayo.ui.widget.SafeSwipeRefreshLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testAppNameResource() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BYO Connect", appName)
  }

  @Test
  fun testTargetUrlConstant() {
    assertEquals("https://mooncakevermilion-wq.github.io/BYO-Connect-app/", MainActivity.TARGET_URL)
  }

  @Test
  fun testThemeColorPaletteResolution() {
    assertNotNull(PrimaryDark)
    assertNotNull(PrimaryLight)
    assertNotNull(BackgroundDark)
    assertNotNull(BackgroundLight)
  }

  @Test
  fun testNotificationChannelsCreation() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    NotificationHelper.createNotificationChannels(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertNotNull(notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_UPDATES_ID))
    assertNotNull(notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_EVENTS_ID))
  }

  @Test
  fun testFcmTopicConstants() {
    assertEquals("byo_connect_updates", ByoFirebaseMessagingService.TOPIC_UPDATES)
    assertEquals("byo_connect_events", ByoFirebaseMessagingService.TOPIC_EVENTS)
  }

  @Test
  fun testSafeSwipeRefreshLayoutActionMoveWithoutDownDoesNotCrash() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val safeLayout = SafeSwipeRefreshLayout(context)

    val moveEvent = MotionEvent.obtain(
      0L,
      System.currentTimeMillis(),
      MotionEvent.ACTION_MOVE,
      100f,
      200f,
      0
    )

    // Must handle ACTION_MOVE safely without errors or exceptions
    safeLayout.onInterceptTouchEvent(moveEvent)
    safeLayout.onTouchEvent(moveEvent)
    safeLayout.dispatchTouchEvent(moveEvent)
    moveEvent.recycle()
  }
}
