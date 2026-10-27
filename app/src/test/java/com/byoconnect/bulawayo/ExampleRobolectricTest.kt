package com.byoconnect.bulawayo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
}
