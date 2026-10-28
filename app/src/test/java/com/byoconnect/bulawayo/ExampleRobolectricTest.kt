package com.byoconnect.bulawayo

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.byoconnect.bulawayo.ui.widget.SafeSwipeRefreshLayout
import org.junit.Assert.assertEquals
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
