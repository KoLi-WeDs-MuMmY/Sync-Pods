package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppPreferences
import com.example.model.AudioOutputSink
import com.example.model.BluetoothDeviceUiModel
import com.example.model.BondStatus
import com.example.model.CodecInfo
import com.example.model.DeviceCategory
import com.example.model.DualAudioRouteMode
import com.example.model.FeatureGuideId
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
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
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
  fun `expressive redesign dock, floating island, dual bridge, 3d modal, and guide sheet work`() {
    var currentTab = BluePulseTab.HUB
    var themeMode = ThemeMode.AMOLED
    var islandEvent: FloatingIslandEvent? = FloatingIslandEvent.DeviceConnected(
      deviceName = "Studio Buds Pro",
      batteryPercent = 92,
      codecLabel = "LDAC",
    )
    var active3DDevice: BluetoothDeviceUiModel? = null
    var activeGuide: FeatureGuideId? = null
    var selectedRouteMode = DualAudioRouteMode.BT_PLUS_WIRED_USB
    var quickPlayToggled = 0

    val sampleBuds = BluetoothDeviceUiModel(
      address = "AA:BB:CC:DD:EE:01",
      name = "Studio Buds Pro",
      category = DeviceCategory.EARBUDS,
      bondStatus = BondStatus.BONDED,
      isConnected = true,
      isAudioSink = true,
      isLeAudioSupported = false,
      activeProfile = "A2DP High-Res",
      rssiDbm = -49,
      batteryPercent = 92,
      leftBatteryPercent = 92,
      rightBatteryPercent = 90,
      caseBatteryPercent = 100,
      codecInfo = CodecInfo(codecName = "LDAC"),
    )

    val sampleSinks = listOf(
      AudioOutputSink(
        id = 1,
        name = "Studio Buds Pro",
        typeLabel = "Bluetooth A2DP",
        address = "AA:BB:CC:DD:EE:01",
        isBluetooth = true,
        isScoOrComm = false,
        isWiredOrUsb = false,
        isSpeaker = false,
      ),
      AudioOutputSink(
        id = 2,
        name = "USB-C DAC",
        typeLabel = "USB-C Audio",
        address = "",
        isBluetooth = false,
        isScoOrComm = false,
        isWiredOrUsb = true,
        isSpeaker = false,
      ),
    )

    composeTestRule.setContent {
      BluePulseApp(
        state = MainUiState(
          selectedTab = currentTab,
          devices = listOf(sampleBuds),
          availableAudioSinks = sampleSinks,
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
          active3DModalDevice = active3DDevice,
          activeFeatureGuide = activeGuide,
        ),
        onSelectTab = { currentTab = it },
        onToggleScan = {},
        onRefreshDevices = {},
        onLaunchCompanionPair = {},
        onOpen3DModal = { active3DDevice = it ?: sampleBuds },
        onClose3DModal = { active3DDevice = null },
        onOpenFeatureGuide = { activeGuide = it },
        onCloseFeatureGuide = { activeGuide = null },
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
        onSetDualRouteMode = { selectedRouteMode = it },
        onSetMonoMixSplit = {},
        onAssignSinkA = {},
        onAssignSinkB = {},
        onPickAudioFile = {},
        onClearAudioFile = {},
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
        onToggleCalibrationTone = { quickPlayToggled++ },
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
    composeTestRule.onNodeWithText("Pure Dual\nSound.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("floating_island_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("floating_pill_dock").assertIsDisplayed()

    // Trigger (i) Feature Guide button on Hub
    composeTestRule.onNodeWithTag("hub_info_btn").performClick()
    assertEquals(FeatureGuideId.HUB_PAIRING, activeGuide)

    // Trigger 3D Rotating Earbuds Pop-Up modal button
    composeTestRule.onNodeWithTag("hub_3d_popup_button").performClick()
    assertEquals("Studio Buds Pro", active3DDevice?.name)

    // Scroll to & verify Asymmetric Bento Grid cards + quick-play action
    composeTestRule.onNodeWithTag("hub_bento_dual_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("hub_bento_radar_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("hub_bento_eq_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("hub_bento_quick_play").performScrollTo().performClick()
    assertEquals(1, quickPlayToggled)

    // Clicking Radar Bento tile navigates to Radar tab
    composeTestRule.onNodeWithTag("hub_bento_radar_card").performScrollTo().performClick()
    assertEquals(BluePulseTab.RADAR, currentTab)

    // Navigate to Dual Sync using the Expressive Floating Pill Dock
    composeTestRule.onNodeWithTag("nav_tab_dual_sync").performClick()
    assertEquals(BluePulseTab.DUAL_SYNC, currentTab)
  }
}
