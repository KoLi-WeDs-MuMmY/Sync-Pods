package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BluetoothDeviceUiModel
import com.example.model.CalibrationMode
import com.example.model.DualAudioRouteMode
import com.example.model.FeatureGuideId
import com.example.model.FeatureGuides
import com.example.ui.components.Earbuds3DConnectionSheet
import com.example.ui.components.FeatureGuideBottomSheet
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
  onLaunchCompanionPair: () -> Unit,
  onOpen3DModal: (BluetoothDeviceUiModel?) -> Unit,
  onClose3DModal: () -> Unit,
  onOpenFeatureGuide: (FeatureGuideId) -> Unit,
  onCloseFeatureGuide: () -> Unit,
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
  onSetDualRouteMode: (DualAudioRouteMode) -> Unit,
  onSetMonoMixSplit: (Boolean) -> Unit,
  onAssignSinkA: (Int) -> Unit,
  onAssignSinkB: (Int) -> Unit,
  onPickAudioFile: () -> Unit,
  onClearAudioFile: () -> Unit,
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
          snackbarHost = {
            SnackbarHost(
              hostState = snackbarHostState,
              modifier = if (!useNavigationRail) Modifier.padding(bottom = 84.dp) else Modifier,
            )
          },
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
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                  ) {
                    Text(
                      text = when (state.preferences.themeMode) {
                        ThemeMode.AMOLED -> "AMOLED"
                        ThemeMode.DARK -> "PLUM DARK"
                        ThemeMode.LIGHT -> "LAVENDER"
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                  }
                }
              },
              navigationIcon = {
                IconButton(
                  onClick = { onSetShowPermissionSheet(true) },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("topbar_permissions_button"),
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
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("topbar_cycle_theme_button"),
                ) {
                  Icon(
                    imageVector = when (state.preferences.themeMode) {
                      ThemeMode.AMOLED -> Icons.Rounded.NightsStay
                      ThemeMode.DARK -> Icons.Rounded.DarkMode
                      ThemeMode.LIGHT -> Icons.Rounded.LightMode
                    },
                    contentDescription = "Cycle Light, Plum Dark, and True-Black AMOLED themes",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(
                  onClick = { onSetShowSettingsDialog(true) },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("topbar_settings_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = "Open appearance and Floating Island settings",
                  )
                }
              },
            )
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
                  onLaunchCompanionPair = onLaunchCompanionPair,
                  onOpen3DModal = onOpen3DModal,
                  onPairOrConnect = onPairOrConnect,
                  onOpenDualSyncTab = { onSelectTab(BluePulseTab.DUAL_SYNC) },
                  onQuickToggleDualPlay = onToggleCalibrationTone,
                  onOpenRadarTab = { onSelectTab(BluePulseTab.RADAR) },
                  onOpenEqTab = { onSelectTab(BluePulseTab.EQ_RULES) },
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
                  onOpenGuide = onOpenFeatureGuide,
                )

                BluePulseTab.DUAL_SYNC -> DualSyncScreen(
                  state = state,
                  onSetDualRouteMode = onSetDualRouteMode,
                  onSetMonoMixSplit = onSetMonoMixSplit,
                  onAssignSinkA = onAssignSinkA,
                  onAssignSinkB = onAssignSinkB,
                  onPickAudioFile = onPickAudioFile,
                  onClearAudioFile = onClearAudioFile,
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
                  onOpenGuide = onOpenFeatureGuide,
                )

                BluePulseTab.RADAR -> RadarSignalScreen(
                  state = state,
                  onSelectTarget = onSelectRadarTarget,
                  onToggleScan = onToggleScan,
                  onStopRadar = onStopRadarMonitor,
                  onOpenGuide = onOpenFeatureGuide,
                )

                BluePulseTab.EQ_RULES -> EqRulesScreen(
                  state = state,
                  onSelectEqDevice = onSelectEqDevice,
                  onApplyPreset = onApplyEqPreset,
                  onBandChange = onEqBandChange,
                  onBassVirtualizerBalanceChange = onBassVirtualizerBalanceChange,
                  onUpdateAutoRules = onUpdateAutoRules,
                  onOpenGuide = onOpenFeatureGuide,
                )
              }
            }
          }
        }

        // Material 3 Expressive Floating Pill Navigation Dock (Compact Viewports)
        if (!useNavigationRail) {
          FloatingPillNavigationDock(
            selectedTab = state.selectedTab,
            onSelectTab = onSelectTab,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 10.dp),
          )
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

    state.active3DModalDevice?.let { modalDevice ->
      Earbuds3DConnectionSheet(
        device = modalDevice,
        onConnectAndRoute = {
          onPairOrConnect(modalDevice)
          onClose3DModal()
          onSelectTab(BluePulseTab.DUAL_SYNC)
        },
        onOpenDualStudio = {
          onClose3DModal()
          onSelectTab(BluePulseTab.DUAL_SYNC)
        },
        onShowGuide = { onOpenFeatureGuide(FeatureGuideId.EARBUDS_3D) },
        onDismiss = onClose3DModal,
      )
    }

    state.activeFeatureGuide?.let { guideId ->
      FeatureGuideBottomSheet(
        guide = FeatureGuides.get(guideId),
        onDismiss = onCloseFeatureGuide,
      )
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

@Composable
private fun FloatingPillNavigationDock(
  selectedTab: BluePulseTab,
  onSelectTab: (BluePulseTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val extended = LocalBluePulseColors.current
  Surface(
    modifier = modifier
      .widthIn(max = 460.dp)
      .fillMaxWidth()
      .testTag("floating_pill_dock"),
    shape = RoundedCornerShape(32.dp),
    color = if (extended.isAmoled) {
      MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
      MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f)
    },
    tonalElevation = 6.dp,
    shadowElevation = 10.dp,
    border = extended.cardBorder,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BluePulseTab.entries.forEach { tab ->
        val selected = selectedTab == tab
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(
              if (selected) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0f)
              }
            )
            .clickable { onSelectTab(tab) }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        ) {
          Icon(
            imageVector = tabIcon(tab, selected),
            contentDescription = tab.title,
            tint = if (selected) {
              MaterialTheme.colorScheme.onPrimaryContainer
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
              MaterialTheme.colorScheme.onPrimaryContainer
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

private fun tabIcon(tab: BluePulseTab, selected: Boolean) = when (tab) {
  BluePulseTab.HUB -> if (selected) Icons.Rounded.Bluetooth else Icons.Outlined.Bluetooth
  BluePulseTab.DUAL_SYNC -> if (selected) Icons.Rounded.Tune else Icons.Outlined.Tune
  BluePulseTab.RADAR -> if (selected) Icons.Rounded.Radar else Icons.Outlined.Radar
  BluePulseTab.EQ_RULES -> if (selected) Icons.Rounded.GraphicEq else Icons.Outlined.GraphicEq
}
