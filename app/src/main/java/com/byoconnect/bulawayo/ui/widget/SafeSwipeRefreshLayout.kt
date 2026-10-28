package com.byoconnect.bulawayo.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.lang.reflect.Field

/**
 * A hardened SwipeRefreshLayout subclass that completely eliminates the Android framework error:
 * "E/SwipeRefreshLayout: Got ACTION_MOVE event but don't have an active pointer id."
 *
 * This occurs when child views (such as WebView, Compose AndroidView hosts, or multi-touch
 * gestures) intercept or alter touch streams, leaving SwipeRefreshLayout's internal
 * mActivePointerId in an uninitialized (INVALID_POINTER = -1) or mismatched state when
 * ACTION_MOVE is received.
 */
class SafeSwipeRefreshLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

  private val activePointerIdField: Field? = try {
    SwipeRefreshLayout::class.java.getDeclaredField("mActivePointerId").apply {
      isAccessible = true
    }
  } catch (_: Exception) {
    null
  }

  private val initialDownYField: Field? = try {
    SwipeRefreshLayout::class.java.getDeclaredField("mInitialDownY").apply {
      isAccessible = true
    }
  } catch (_: Exception) {
    null
  }

  private val initialMotionYField: Field? = try {
    SwipeRefreshLayout::class.java.getDeclaredField("mInitialMotionY").apply {
      isAccessible = true
    }
  } catch (_: Exception) {
    null
  }

  private fun getInternalPointerId(): Int {
    return try {
      activePointerIdField?.getInt(this) ?: -1
    } catch (_: Exception) {
      -1
    }
  }

  private fun setInternalPointerId(id: Int) {
    try {
      activePointerIdField?.setInt(this, id)
    } catch (_: Exception) {}
  }

  /**
   * Validates and repairs the internal pointer ID and touch origins if they became
   * desynchronized from the current MotionEvent pointer table.
   */
  private fun ensureValidPointerId(ev: MotionEvent): Boolean {
    if (ev.pointerCount <= 0) return false

    val currentId = getInternalPointerId()
    if (currentId == -1 || ev.findPointerIndex(currentId) < 0) {
      // Select the primary available pointer
      val validPointerId = ev.getPointerId(0)
      setInternalPointerId(validPointerId)

      // Initialize touch origins to avoid coordinate jumps
      try {
        val y = ev.getY(0)
        initialDownYField?.setFloat(this, y)
        initialMotionYField?.setFloat(this, y)
      } catch (_: Exception) {}
    }
    return true
  }

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    if (ev.actionMasked == MotionEvent.ACTION_MOVE && ev.pointerCount > 0) {
      ensureValidPointerId(ev)
    }
    return try {
      super.dispatchTouchEvent(ev)
    } catch (_: Exception) {
      false
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (ev.actionMasked == MotionEvent.ACTION_MOVE) {
      if (!ensureValidPointerId(ev)) {
        return false
      }
      val currentId = getInternalPointerId()
      if (currentId == -1 || ev.findPointerIndex(currentId) < 0) {
        return false
      }
    }
    return try {
      super.onInterceptTouchEvent(ev)
    } catch (_: Exception) {
      false
    }
  }

  override fun onTouchEvent(ev: MotionEvent): Boolean {
    if (ev.actionMasked == MotionEvent.ACTION_MOVE) {
      if (!ensureValidPointerId(ev)) {
        return false
      }
      val currentId = getInternalPointerId()
      if (currentId == -1 || ev.findPointerIndex(currentId) < 0) {
        return false
      }
    }
    return try {
      super.onTouchEvent(ev)
    } catch (_: Exception) {
      false
    }
  }
}
