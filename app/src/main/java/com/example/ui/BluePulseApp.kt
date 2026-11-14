package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BluetoothDeviceUiModel
import com.example.model.CalibrationMode
import com.example.ui.components.InAppFloatingIslandHost
import com.example.ui.components.PermissionOnboardingSheet
import com.example.ui.components.ThemeAndSettingsDialog
import com.example.ui.screens.DualSyncScreen
import com.example.ui.screens.EqRulesScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.RadarSignalScreen
import com.example.ui.theme.BluePulseTheme
import com.example.ui.theme.LocalBluePulseColors
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluePulseApp(
  state: MainUiState,
  onSelectTab: (BluePulseTab) -> Unit,
  onToggleScan: () -> Unit,
  onRefreshDevices: () -> Unit,
  onPairOrConnect: (BluetoothDeviceUiModel) -> Unit,
  onOpenSystemBt: () -> Unit,
  onRequestBluetoothPermissions: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onRequestOverlayPermission: () -> Unit,
  onSetShowPermissionSheet: (Boolean) -> Unit,
  onSetShowSettingsDialog: (Boolean) -> Unit,
  onSelectTheme: (ThemeMode) -> Unit,
  onToggleSystemOverlay: (Boolean) -> Unit,
  onToggleRadarHaptics: (Boolean) -> Unit,
  onPreviewIsland: () -> Unit,
  onToggleIslandExpanded: () -> Unit,
  onDismissIsland: () -> Unit,
  onClearStatusMessage: () -> Unit,
  onSetDualLinked: (Boolean) -> Unit,
  onMasterVolumeChange: (Float) -> Unit,
  onAssignDeviceA: (String) -> Unit,
  onAssignDeviceB: (String) -> Unit,
  onDeviceAVolumeChange: (Float) -> Unit,
  onDeviceBVolumeChange: (Float) -> Unit,
  onDeviceALatencyChange: (Int) -> Unit,
  onDeviceBLatencyChange: (Int) -> Unit,
  onDeviceABalanceChange: (Float) -> Unit,
  onDeviceBBalanceChange: (Float) -> Unit,
  onCalibrationModeChange: (CalibrationMode) -> Unit,
  onToggleCalibrationTone: () -> Unit,
  onSelectRadarTarget: (String) -> Unit,
  onStopRadarMonitor: () -> Unit,
  onSelectEqDevice: (String) -> Unit,
  onApplyEqPreset: (String) -> Unit,
  onEqBandChange: (Int, Int) -> Unit,
  onBassVirtualizerBalanceChange: (Int, Int, Float) -> Unit,
  onUpdateAutoRules: (String, String, Boolean, Boolean, Boolean, Int) -> Unit,
) {
  BluePulseTheme(themeMode = state.preferences.themeMode) {
    val snackbarHostState = remember { SnackbarHostState() }
    val extended = LocalBluePulseColors.current

    LaunchedEffect(state.statusMessage) {
      val msg = state.statusMessage
      if (msg != null) {
        snackbarHostState.showSnackbar(msg)
        onClearStatusMessage()
      }
    }

    if (state.selectedTab != BluePulseTab.HUB) {
      BackHandler {
        onSelectTab(BluePulseTab.HUB)
      }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavigationRail = maxWidth >= 600.dp

      Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
          containerColor = MaterialTheme.colorScheme.background,
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          topBar = {
            CenterAlignedTopAppBar(
              colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
              ),
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "BluePulse",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                  ) {
                    Text(
                      text = when (state.preferences.themeMode) {
                        ThemeMode.AMOLED -> "AMOLED"
                        ThemeMode.DARK -> "DARK"
                        ThemeMode.LIGHT -> "LIGHT"
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
              },
              navigationIcon = {
                IconButton(
                  onClick = { onSetShowPermissionSheet(true) },
                  modifier = Modifier.testTag("topbar_permissions_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = "Manage Bluetooth and Overlay Permissions",
                    tint = if (state.permissions.allCoreGranted && state.permissions.overlayGranted) {
                      extended.signalGood
                    } else {
                      extended.signalWarn
                    },
                  )
                }
              },
              actions = {
                IconButton(
                  onClick = {
                    val next = when (state.preferences.themeMode) {
                      ThemeMode.AMOLED -> ThemeMode.DARK
                      ThemeMode.DARK -> ThemeMode.LIGHT
                      ThemeMode.LIGHT -> ThemeMode.AMOLED
                    }
                    onSelectTheme(next)
                  },
                  modifier = Modifier.testTag("topbar_cycle_theme_button"),
                ) {
                  Icon(
                    imageVector = when (state.preferences.themeMode) {
                      ThemeMode.AMOLED -> Icons.Rounded.NightsStay
                      ThemeMode.DARK -> Icons.Rounded.DarkMode
                      ThemeMode.LIGHT -> Icons.Rounded.LightMode
                    },
                    contentDescription = "Cycle Light, Dark, and True-Black AMOLED themes",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(
                  onClick = { onSetShowSettingsDialog(true) },
                  modifier = Modifier.testTag("topbar_settings_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = "Open appearance and Floating Island settings",
                  )
                }
              },
            )
          },
          bottomBar = {
            if (!useNavigationRail) {
              NavigationBar(
                containerColor = if (extended.isAmoled) {
                  MaterialTheme.colorScheme.background
                } else {
                  MaterialTheme.colorScheme.surfaceContainer
                },
              ) {
                BluePulseTab.entries.forEach { tab ->
                  val selected = state.selectedTab == tab
                  NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                      Icon(
                        imageVector = tabIcon(tab, selected),
                        contentDescription = tab.title,
                      )
                    },
                    label = { Text(tab.title, maxLines = 1) },
                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            if (useNavigationRail) {
              NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = if (extended.isAmoled) {
                  MaterialTheme.colorScheme.background
                } else {
                  MaterialTheme.colorScheme.surfaceContainer
                },
              ) {
                BluePulseTab.entries.forEach { tab ->
                  val selected = state.selectedTab == tab
                  NavigationRailItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                      Icon(
                        imageVector = tabIcon(tab, selected),
                        contentDescription = tab.title,
                      )
                    },
                    label = { Text(tab.title) },
                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                  )
                }
              }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
              when (state.selectedTab) {
                BluePulseTab.HUB -> HubScreen(
                  state = state,
                  onToggleScan = onToggleScan,
                  onRefresh = onRefreshDevices,
                  onPairOrConnect = onPairOrConnect,
                  onOpenRadarForDevice = { addr ->
                    onSelectRadarTarget(addr)
                    onSelectTab(BluePulseTab.RADAR)
                  },
                  onOpenEqForDevice = { addr ->
                    onSelectEqDevice(addr)
                    onSelectTab(BluePulseTab.EQ_RULES)
                  },
                  onOpenPermissions = { onSetShowPermissionSheet(true) },
                  onOpenSystemBt = onOpenSystemBt,
                  onPreviewIsland = onPreviewIsland,
                )

                BluePulseTab.DUAL_SYNC -> DualSyncScreen(
                  state = state,
                  onSetLinked = onSetDualLinked,
                  onMasterVolumeChange = onMasterVolumeChange,
                  onAssignDeviceA = onAssignDeviceA,
                  onAssignDeviceB = onAssignDeviceB,
                  onDeviceAVolumeChange = onDeviceAVolumeChange,
                  onDeviceBVolumeChange = onDeviceBVolumeChange,
                  onDeviceALatencyChange = onDeviceALatencyChange,
                  onDeviceBLatencyChange = onDeviceBLatencyChange,
                  onDeviceABalanceChange = onDeviceABalanceChange,
                  onDeviceBBalanceChange = onDeviceBBalanceChange,
                  onCalibrationModeChange = onCalibrationModeChange,
                  onToggleCalibrationTone = onToggleCalibrationTone,
                )

                BluePulseTab.RADAR -> RadarSignalScreen(
                  state = state,
                  onSelectTarget = onSelectRadarTarget,
                  onToggleScan = onToggleScan,
                  onStopRadar = onStopRadarMonitor,
                )

                BluePulseTab.EQ_RULES -> EqRulesScreen(
                  state = state,
                  onSelectEqDevice = onSelectEqDevice,
                  onApplyPreset = onApplyEqPreset,
                  onBandChange = onEqBandChange,
                  onBassVirtualizerBalanceChange = onBassVirtualizerBalanceChange,
                  onUpdateAutoRules = onUpdateAutoRules,
                )
              }
            }
          }
        }

        // In-App Dynamic Island Capsule (mirrors & previews live events inside app)
        InAppFloatingIslandHost(
          event = state.activeIslandEvent,
          isExpanded = state.isIslandExpanded,
          onToggleExpand = onToggleIslandExpanded,
          onDismiss = onDismissIsland,
          onOpenDualSync = { onSelectTab(BluePulseTab.DUAL_SYNC) },
          modifier = Modifier.align(Alignment.TopCenter),
        )
      }
    }

    if (state.showPermissionSheet) {
      PermissionOnboardingSheet(
        permissions = state.permissions,
        onRequestBluetooth = onRequestBluetoothPermissions,
        onRequestNotifications = onRequestNotificationPermission,
        onRequestOverlay = onRequestOverlayPermission,
        onDismiss = { onSetShowPermissionSheet(false) },
      )
    }

    if (state.showSettingsDialog) {
      ThemeAndSettingsDialog(
        preferences = state.preferences,
        permissions = state.permissions,
        onSelectTheme = onSelectTheme,
        onToggleSystemOverlay = onToggleSystemOverlay,
        onToggleRadarHaptics = onToggleRadarHaptics,
        onOpenOverlaySettings = onRequestOverlayPermission,
        onPreviewIsland = onPreviewIsland,
        onOpenPermissions = { onSetShowPermissionSheet(true) },
        onDismiss = { onSetShowSettingsDialog(false) },
      )
    }
  }
}

private fun tabIcon(tab: BluePulseTab, selected: Boolean) = when (tab) {
  BluePulseTab.HUB -> if (selected) Icons.Rounded.Bluetooth else Icons.Outlined.Bluetooth
  BluePulseTab.DUAL_SYNC -> if (selected) Icons.Rounded.Tune else Icons.Outlined.Tune
  BluePulseTab.RADAR -> if (selected) Icons.Rounded.Radar else Icons.Outlined.Radar
  BluePulseTab.EQ_RULES -> if (selected) Icons.Rounded.GraphicEq else Icons.Outlined.GraphicEq
}
