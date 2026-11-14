package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppPreferences
import com.example.model.FloatingIslandEvent
import com.example.model.PermissionStatus
import com.example.ui.BluePulseApp
import com.example.ui.BluePulseTab
import com.example.ui.MainUiState
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `app name matches BluePulse`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BluePulse", appName)
  }

  @Test
  fun `amoled theme badge and floating island render cleanly`() {
    var currentTab = BluePulseTab.HUB
    var themeMode = ThemeMode.AMOLED
    var islandEvent: FloatingIslandEvent? = FloatingIslandEvent.DeviceConnected(
      deviceName = "Studio Buds Pro",
      batteryPercent = 92,
      codecLabel = "LDAC",
    )

    composeTestRule.setContent {
      BluePulseApp(
        state = MainUiState(
          selectedTab = currentTab,
          preferences = AppPreferences(
            themeMode = themeMode,
            permissionOnboardingDismissed = true,
          ),
          permissions = PermissionStatus(
            bluetoothGranted = true,
            notificationsGranted = true,
            overlayGranted = false,
          ),
          activeIslandEvent = islandEvent,
          isIslandExpanded = false,
          showPermissionSheet = false,
        ),
        onSelectTab = { currentTab = it },
        onToggleScan = {},
        onRefreshDevices = {},
        onPairOrConnect = {},
        onOpenSystemBt = {},
        onRequestBluetoothPermissions = {},
        onRequestNotificationPermission = {},
        onRequestOverlayPermission = {},
        onSetShowPermissionSheet = {},
        onSetShowSettingsDialog = {},
        onSelectTheme = { themeMode = it },
        onToggleSystemOverlay = {},
        onToggleRadarHaptics = {},
        onPreviewIsland = {},
        onToggleIslandExpanded = {},
        onDismissIsland = { islandEvent = null },
        onClearStatusMessage = {},
        onSetDualLinked = {},
        onMasterVolumeChange = {},
        onAssignDeviceA = {},
        onAssignDeviceB = {},
        onDeviceAVolumeChange = {},
        onDeviceBVolumeChange = {},
        onDeviceALatencyChange = {},
        onDeviceBLatencyChange = {},
        onDeviceABalanceChange = {},
        onDeviceBBalanceChange = {},
        onCalibrationModeChange = {},
        onToggleCalibrationTone = {},
        onSelectRadarTarget = {},
        onStopRadarMonitor = {},
        onSelectEqDevice = {},
        onApplyEqPreset = {},
        onEqBandChange = { _, _ -> },
        onBassVirtualizerBalanceChange = { _, _, _ -> },
        onUpdateAutoRules = { _, _, _, _, _, _ -> },
      )
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("AMOLED").assertIsDisplayed()
    composeTestRule.onNodeWithTag("floating_island_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tab_dual_sync").performClick()
    assertEquals(BluePulseTab.DUAL_SYNC, currentTab)
  }
}
