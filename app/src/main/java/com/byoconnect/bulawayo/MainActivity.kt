package com.byoconnect.bulawayo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.byoconnect.bulawayo.fcm.ByoFirebaseMessagingService
import com.byoconnect.bulawayo.fcm.NotificationHelper
import com.byoconnect.bulawayo.ui.theme.Black
import com.byoconnect.bulawayo.ui.theme.ByoConnectTheme
import com.byoconnect.bulawayo.ui.theme.CyanAccent
import com.byoconnect.bulawayo.ui.theme.CyanGlow
import com.byoconnect.bulawayo.ui.theme.SurfaceCard
import com.byoconnect.bulawayo.ui.theme.SurfaceDark
import com.byoconnect.bulawayo.ui.theme.TextPrimary
import com.byoconnect.bulawayo.ui.theme.TextSecondary
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

  companion object {
    private const val TAG = "MainActivity"
    const val TARGET_URL = "https://mooncakevermilion-wq.github.io/BYO-Connect-app/"
  }

  private var webView: WebView? = null
  private var swipeRefreshLayout: SwipeRefreshLayout? = null

  // File chooser callback and camera capture URI
  private var filePathCallback: ValueCallback<Array<Uri>>? = null
  private var cameraPhotoUri: Uri? = null

  // Notification URL passed from FCM push payload
  private var pendingNotificationUrl: String? = null

  // Notification permission launcher (Android 13+)
  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        Log.d(TAG, "Notification permission granted")
        fetchAndRegisterFcmToken()
      } else {
        Log.w(TAG, "Notification permission denied by user")
      }
    }

  // File chooser activity launcher
  private val fileChooserLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (filePathCallback == null) return@registerForActivityResult

      var results: Array<Uri>? = null

      if (result.resultCode == Activity.RESULT_OK) {
        val intentData = result.data
        if (intentData?.data != null) {
          // Photo chosen from gallery or picker
          results = arrayOf(intentData.data!!)
        } else if (cameraPhotoUri != null) {
          // Photo captured via camera
          results = arrayOf(cameraPhotoUri!!)
        }
      }

      filePathCallback?.onReceiveValue(results)
      filePathCallback = null
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    // Switch from splash theme to main app theme
    setTheme(R.style.Theme_ByoConnect)
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize FCM notification channel
    NotificationHelper.createNotificationChannel(this)

    // Check for notification deep-link intent
    pendingNotificationUrl = intent?.getStringExtra(ByoFirebaseMessagingService.EXTRA_NOTIFICATION_URL)

    // Request notification permission on Android 13+ and fetch FCM token
    initNotificationSystem()

    // Handle Android system back navigation
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (webView != null && webView!!.canGoBack()) {
            webView!!.goBack()
          } else {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
          }
        }
      }
    )

    val launchUrl = pendingNotificationUrl ?: TARGET_URL

    setContent {
      ByoConnectTheme {
        MainScreen(
          targetUrl = launchUrl,
          isOnline = isNetworkAvailable(),
          onRegisterWebView = { wv, srl ->
            webView = wv
            swipeRefreshLayout = srl
            // If a notification URL arrived during initialization
            pendingNotificationUrl?.let { url ->
              wv.loadUrl(url)
              pendingNotificationUrl = null
            }
          },
          onShowFileChooser = { callback ->
            openFileChooser(callback)
          },
          onHandleWhatsApp = { url ->
            handleWhatsAppLink(url)
          },
          onHandleTel = { url ->
            handleTelLink(url)
          },
          onHandleMailto = { url ->
            handleMailtoLink(url)
          }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val url = intent.getStringExtra(ByoFirebaseMessagingService.EXTRA_NOTIFICATION_URL)
    if (!url.isNullOrEmpty()) {
      webView?.loadUrl(url)
    }
  }

  /**
   * Initializes notification permissions and fetches the FCM registration token.
   */
  private fun initNotificationSystem() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val hasPermission = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED

      if (!hasPermission) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        fetchAndRegisterFcmToken()
      }
    } else {
      fetchAndRegisterFcmToken()
    }
  }

  /**
   * Fetches the current FCM token and subscribes to general BYO Connect updates.
   */
  private fun fetchAndRegisterFcmToken() {
    try {
      FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val token = task.result
          Log.d(TAG, "FCM registration token: $token")
          val prefs = getSharedPreferences(ByoFirebaseMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
          prefs.edit().putString(ByoFirebaseMessagingService.KEY_FCM_TOKEN, token).apply()
          notifyWebViewOfFcmToken(token)
        } else {
          Log.w(TAG, "Fetching FCM registration token failed", task.exception)
        }
      }

      // Automatically subscribe all app users to general community announcements
      FirebaseMessaging.getInstance().subscribeToTopic("byo_connect_updates")
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            Log.d(TAG, "Subscribed to byo_connect_updates topic successfully")
          }
        }
    } catch (e: Exception) {
      Log.w(TAG, "FirebaseMessaging not initialized yet (missing google-services.json)", e)
    }
  }

  /**
   * Dispatches the FCM registration token to the web application via JavaScript.
   */
  private fun notifyWebViewOfFcmToken(token: String) {
    webView?.post {
      webView?.evaluateJavascript(
        "if (typeof window.onFcmTokenReceived === 'function') { window.onFcmTokenReceived('$token'); }",
        null
      )
    }
  }

  /**
   * Opens the file chooser dialog offering both Camera capture and Photo Gallery.
   */
  private fun openFileChooser(callback: ValueCallback<Array<Uri>>) {
    filePathCallback?.onReceiveValue(null)
    filePathCallback = callback

    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    if (takePictureIntent.resolveActivity(packageManager) != null) {
      try {
        val photoFile = createImageFile()
        cameraPhotoUri = FileProvider.getUriForFile(
          this,
          "${applicationContext.packageName}.fileprovider",
          photoFile
        )
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
      } catch (ex: IOException) {
        cameraPhotoUri = null
      }
    } else {
      cameraPhotoUri = null
    }

    val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "image/*"
      putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp", "image/gif"))
    }

    val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
      putExtra(Intent.EXTRA_INTENT, galleryIntent)
      putExtra(Intent.EXTRA_TITLE, getString(R.string.file_chooser_title))
      if (cameraPhotoUri != null) {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))
      }
    }

    try {
      fileChooserLauncher.launch(chooserIntent)
    } catch (e: Exception) {
      filePathCallback?.onReceiveValue(null)
      filePathCallback = null
      Toast.makeText(this, "Could not open photo picker", Toast.LENGTH_SHORT).show()
    }
  }

  @Throws(IOException::class)
  private fun createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: cacheDir
    val imagesDir = File(storageDir, "images").apply { mkdirs() }
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", imagesDir)
  }

  /**
   * Handles WhatsApp direct links (wa.me, api.whatsapp.com, or whatsapp://).
   */
  private fun handleWhatsAppLink(url: String): Boolean {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      startActivity(intent)
      return true
    } catch (e: Exception) {
      try {
        val webUrl = if (url.startsWith("whatsapp://send")) {
          url.replace("whatsapp://send", "https://api.whatsapp.com/send")
        } else {
          url
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
        startActivity(browserIntent)
        return true
      } catch (_: Exception) {
        Toast.makeText(this, R.string.error_whatsapp_not_installed, Toast.LENGTH_LONG).show()
        return true
      }
    }
  }

  /**
   * Handles telephone dialer links (tel:).
   */
  private fun handleTelLink(url: String): Boolean {
    try {
      val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
      startActivity(intent)
      return true
    } catch (e: Exception) {
      Toast.makeText(this, "Unable to launch phone dialer", Toast.LENGTH_SHORT).show()
      return true
    }
  }

  /**
   * Handles email links (mailto:).
   */
  private fun handleMailtoLink(url: String): Boolean {
    try {
      val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
      startActivity(intent)
      return true
    } catch (_: Exception) {
      return false
    }
  }

  private fun isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  override fun onDestroy() {
    webView?.destroy()
    webView = null
    super.onDestroy()
  }
}

/**
 * JavaScript Interface bridge exposed to the web application inside the WebView.
 */
class ByoWebAppInterface(private val context: Context, private val webView: WebView?) {

  @JavascriptInterface
  fun getFcmToken(): String {
    val prefs = context.getSharedPreferences(
      ByoFirebaseMessagingService.PREFS_NAME,
      Context.MODE_PRIVATE
    )
    return prefs.getString(ByoFirebaseMessagingService.KEY_FCM_TOKEN, "") ?: ""
  }

  @JavascriptInterface
  fun subscribeToTopic(topic: String) {
    try {
      FirebaseMessaging.getInstance().subscribeToTopic(topic)
    } catch (e: Exception) {
      Log.w("ByoWebAppInterface", "Failed to subscribe to topic: $topic", e)
    }
  }

  @JavascriptInterface
  fun unsubscribeFromTopic(topic: String) {
    try {
      FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
    } catch (e: Exception) {
      Log.w("ByoWebAppInterface", "Failed to unsubscribe from topic: $topic", e)
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainScreen(
  targetUrl: String,
  isOnline: Boolean,
  onRegisterWebView: (WebView, SwipeRefreshLayout) -> Unit,
  onShowFileChooser: (ValueCallback<Array<Uri>>) -> Unit,
  onHandleWhatsApp: (String) -> Boolean,
  onHandleTel: (String) -> Boolean,
  onHandleMailto: (String) -> Boolean
) {
  var loadProgress by remember { mutableIntStateOf(0) }
  var isInitialSplashVisible by remember { mutableStateOf(true) }
  var hasNetworkError by remember { mutableStateOf(!isOnline) }
  var activeWebView by remember { mutableStateOf<WebView?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Black)
      .windowInsetsPadding(WindowInsets.statusBars)
  ) {
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.navigationBars),
      factory = { context ->
        val swipeRefresh = SwipeRefreshLayout(context).apply {
          setColorSchemeColors(CyanAccent.toArgb(), CyanGlow.toArgb())
          setProgressBackgroundColorSchemeColor(SurfaceDark.toArgb())
        }

        val wv = WebView(context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          setBackgroundColor(Black.toArgb())

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = "$userAgentString BYOConnectAndroidApp/1.0"
          }

          // Register JavaScript Interface bridge for FCM and native controls
          addJavascriptInterface(ByoWebAppInterface(context, this), "AndroidBridge")

          webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
              view: WebView?,
              request: WebResourceRequest?
            ): Boolean {
              val url = request?.url?.toString() ?: return false

              // Handle WhatsApp links
              if (url.contains("wa.me") || url.contains("whatsapp.com") || url.startsWith("whatsapp://")) {
                return onHandleWhatsApp(url)
              }

              // Handle tel links
              if (url.startsWith("tel:")) {
                return onHandleTel(url)
              }

              // Handle mailto links
              if (url.startsWith("mailto:")) {
                return onHandleMailto(url)
              }

              return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              hasNetworkError = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              swipeRefresh.isRefreshing = false
              isInitialSplashVisible = false

              // Inject FCM token into web app if available
              val prefs = context.getSharedPreferences(
                ByoFirebaseMessagingService.PREFS_NAME,
                Context.MODE_PRIVATE
              )
              val token = prefs.getString(ByoFirebaseMessagingService.KEY_FCM_TOKEN, null)
              if (!token.isNullOrEmpty()) {
                view?.evaluateJavascript(
                  "if (typeof window.onFcmTokenReceived === 'function') { window.onFcmTokenReceived('$token'); }",
                  null
                )
              }
            }

            override fun onReceivedError(
              view: WebView?,
              request: WebResourceRequest?,
              error: WebResourceError?
            ) {
              super.onReceivedError(view, request, error)
              if (request?.isForMainFrame == true) {
                hasNetworkError = true
                swipeRefresh.isRefreshing = false
              }
            }
          }

          webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              super.onProgressChanged(view, newProgress)
              loadProgress = newProgress
              if (newProgress >= 100) {
                isInitialSplashVisible = false
              }
            }

            override fun onShowFileChooser(
              webView: WebView?,
              filePathCallback: ValueCallback<Array<Uri>>?,
              fileChooserParams: FileChooserParams?
            ): Boolean {
              if (filePathCallback != null) {
                onShowFileChooser(filePathCallback)
                return true
              }
              return false
            }
          }

          loadUrl(targetUrl)
        }

        swipeRefresh.addView(wv)
        swipeRefresh.setOnRefreshListener {
          hasNetworkError = false
          wv.reload()
        }
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
          wv.scrollY > 0
        }

        activeWebView = wv
        onRegisterWebView(wv, swipeRefresh)
        swipeRefresh
      }
    )

    // Top Cyan Loading Bar
    if (loadProgress in 1..99 && !isInitialSplashVisible && !hasNetworkError) {
      LinearProgressIndicator(
        progress = { loadProgress / 100f },
        color = CyanAccent,
        trackColor = Color.Transparent,
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .align(Alignment.TopCenter)
          .testTag("loading_progress_bar")
      )
    }

    // Styled Dark and Cyan Offline Recovery Screen
    AnimatedVisibility(
      visible = hasNetworkError,
      enter = fadeIn(animationSpec = tween(250)),
      exit = fadeOut(animationSpec = tween(200))
    ) {
      OfflineErrorScreen(
        onRetry = {
          hasNetworkError = false
          activeWebView?.reload()
        }
      )
    }

    // Splash Screen with Logo BYO Connect, Black Background, Cyan Accent
    AnimatedVisibility(
      visible = isInitialSplashVisible,
      enter = fadeIn(),
      exit = fadeOut(animationSpec = tween(400))
    ) {
      SplashScreenOverlay()
    }
  }
}

@Composable
fun SplashScreenOverlay() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Black)
      .testTag("splash_screen_overlay"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.ic_byo_logo),
        contentDescription = "BYO Connect Logo",
        modifier = Modifier.size(120.dp)
      )

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "BYO CONNECT",
        color = TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Bulawayo's Community Network",
        color = CyanAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
      )

      Spacer(modifier = Modifier.height(36.dp))

      LinearProgressIndicator(
        color = CyanAccent,
        trackColor = SurfaceDark,
        modifier = Modifier
          .fillMaxWidth(0.45f)
          .height(3.dp)
      )
    }
  }
}

@Composable
fun OfflineErrorScreen(
  onRetry: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Black)
      .padding(32.dp)
      .testTag("offline_error_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .background(SurfaceDark, RoundedCornerShape(20.dp))
        .border(1.dp, SurfaceCard, RoundedCornerShape(20.dp))
        .padding(32.dp)
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .background(SurfaceCard, CircleShape)
          .border(1.5.dp, CyanGlow, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.WifiOff,
          contentDescription = "Offline Icon",
          tint = CyanAccent,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = stringResource(id = R.string.offline_title),
        color = TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(id = R.string.offline_desc),
        color = TextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
      )

      Spacer(modifier = Modifier.height(28.dp))

      Button(
        onClick = onRetry,
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanAccent,
          contentColor = Black
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("retry_connection_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = stringResource(id = R.string.offline_retry),
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }
    }
  }
}
