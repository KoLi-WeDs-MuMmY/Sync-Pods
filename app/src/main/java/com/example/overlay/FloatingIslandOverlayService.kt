package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.ui.components.FloatingIslandPillCard
import com.example.ui.theme.BluePulseTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingIslandOverlayService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var windowManager: WindowManager? = null
  private var overlayComposeView: ComposeView? = null
  private var lifecycleOwner: OverlayLifecycleOwner? = null
  private var autoCollapseJob: Job? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    startAsForegroundService()
    attachOverlayIfPermitted()
    observeAutoCollapse()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (overlayComposeView == null) {
      attachOverlayIfPermitted()
    }
    return START_STICKY
  }

  private fun startAsForegroundService() {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(getString(R.string.overlay_service_title))
      .setContentText(getString(R.string.overlay_service_text))
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()

    val serviceTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
    } else {
      0
    }
    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceTypes)
  }

  private fun attachOverlayIfPermitted() {
    if (!Settings.canDrawOverlays(this) || overlayComposeView != null) return

    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager = wm

    val owner = OverlayLifecycleOwner().also {
      it.performRestore()
      it.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
      it.handleLifecycleEvent(Lifecycle.Event.ON_START)
      it.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    lifecycleOwner = owner

    val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      layoutType,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
      y = 32
    }

    val composeView = ComposeView(this).apply {
      setViewTreeLifecycleOwner(owner)
      setViewTreeViewModelStoreOwner(owner)
      setViewTreeSavedStateRegistryOwner(owner)
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val activeEvent by FloatingIslandEventBus.activeEvent.collectAsStateWithLifecycle()
        val isExpanded by FloatingIslandEventBus.isExpanded.collectAsStateWithLifecycle()

        BluePulseTheme(themeMode = ThemeMode.AMOLED) {
          Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.TopCenter,
          ) {
            AnimatedVisibility(
              visible = activeEvent != null,
              enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessMediumLow,
                ),
              ) + scaleIn(initialScale = 0.8f) + fadeIn(),
              exit = slideOutVertically(targetOffsetY = { -it }) +
                scaleOut(targetScale = 0.85f) + fadeOut(),
            ) {
              activeEvent?.let { event ->
                FloatingIslandPillCard(
                  event = event,
                  isExpanded = isExpanded,
                  onToggleExpand = { FloatingIslandEventBus.setExpanded(!isExpanded) },
                  onDismiss = { FloatingIslandEventBus.dismiss() },
                  onOpenAction = {
                    val launch = Intent(this@FloatingIslandOverlayService, MainActivity::class.java).apply {
                      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(launch)
                  },
                )
              }
            }
          }
        }
      }
    }

    runCatching {
      wm.addView(composeView, params)
      overlayComposeView = composeView
    }
  }

  private fun observeAutoCollapse() {
    serviceScope.launch {
      FloatingIslandEventBus.activeEvent.collectLatest { event ->
        autoCollapseJob?.cancel()
        if (event != null) {
          autoCollapseJob = launch {
            delay(4_500L)
            FloatingIslandEventBus.setExpanded(false)
            delay(4_500L)
            FloatingIslandEventBus.dismiss()
          }
        }
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)
      val channel = NotificationChannel(
        CHANNEL_ID,
        getString(R.string.overlay_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = getString(R.string.overlay_channel_desc)
      }
      manager.createNotificationChannel(channel)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    autoCollapseJob?.cancel()
    overlayComposeView?.let { view ->
      runCatching { windowManager?.removeView(view) }
    }
    overlayComposeView = null
    lifecycleOwner?.apply {
      handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
      handleLifecycleEvent(Lifecycle.Event.ON_STOP)
      handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
      viewModelStore.clear()
    }
    lifecycleOwner = null
    serviceScope.cancel()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private class OverlayLifecycleOwner :
    androidx.lifecycle.LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
      get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
      get() = store

    override val savedStateRegistry: SavedStateRegistry
      get() = savedStateController.savedStateRegistry

    fun performRestore() {
      savedStateController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
      lifecycleRegistry.handleLifecycleEvent(event)
    }
  }

  companion object {
    private const val CHANNEL_ID = "bluepulse_island_channel"
    private const val NOTIFICATION_ID = 1042

    fun startIfPermitted(context: Context) {
      if (!Settings.canDrawOverlays(context)) return
      val intent = Intent(context, FloatingIslandOverlayService::class.java)
      ContextCompatStartHelper.startForeground(context, intent)
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, FloatingIslandOverlayService::class.java))
    }
  }
}

private object ContextCompatStartHelper {
  fun startForeground(context: Context, intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }
  }
}
