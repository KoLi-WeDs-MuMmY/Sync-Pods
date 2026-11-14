package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.PermissionHelper
import com.example.ui.BluePulseApp
import com.example.ui.MainViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()

      val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
      ) {
        viewModel.refreshPermissionsAndServices()
      }

      val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
      ) {
        viewModel.refreshPermissionsAndServices()
      }

      val overlaySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
      ) {
        viewModel.refreshPermissionsAndServices()
      }

      BluePulseApp(
        state = state,
        onSelectTab = viewModel::selectTab,
        onToggleScan = viewModel::toggleScan,
        onRefreshDevices = viewModel::refreshDevices,
        onPairOrConnect = viewModel::pairOrConnectDevice,
        onOpenSystemBt = viewModel::openSystemBluetoothSettings,
        onRequestBluetoothPermissions = {
          btPermissionLauncher.launch(PermissionHelper.requiredBluetoothPermissions())
        },
        onRequestNotificationPermission = {
          val perm = PermissionHelper.requiredNotificationPermission()
          if (perm != null) {
            notifPermissionLauncher.launch(perm)
          } else {
            viewModel.refreshPermissionsAndServices()
          }
        },
        onRequestOverlayPermission = {
          overlaySettingsLauncher.launch(viewModel.openOverlayPermissionSettings())
        },
        onSetShowPermissionSheet = viewModel::setShowPermissionSheet,
        onSetShowSettingsDialog = viewModel::setShowSettingsDialog,
        onSelectTheme = viewModel::setThemeMode,
        onToggleSystemOverlay = viewModel::setSystemOverlayEnabled,
        onToggleRadarHaptics = viewModel::setRadarHapticsEnabled,
        onPreviewIsland = viewModel::previewFloatingIslandPopup,
        onToggleIslandExpanded = viewModel::toggleIslandExpanded,
        onDismissIsland = viewModel::dismissIsland,
        onClearStatusMessage = viewModel::clearStatusMessage,
        onSetDualLinked = viewModel::setDualLinked,
        onMasterVolumeChange = viewModel::updateMasterVolume,
        onAssignDeviceA = viewModel::assignDeviceA,
        onAssignDeviceB = viewModel::assignDeviceB,
        onDeviceAVolumeChange = viewModel::updateDeviceAVolume,
        onDeviceBVolumeChange = viewModel::updateDeviceBVolume,
        onDeviceALatencyChange = viewModel::updateDeviceALatency,
        onDeviceBLatencyChange = viewModel::updateDeviceBLatency,
        onDeviceABalanceChange = viewModel::updateDeviceABalance,
        onDeviceBBalanceChange = viewModel::updateDeviceBBalance,
        onCalibrationModeChange = viewModel::setCalibrationMode,
        onToggleCalibrationTone = viewModel::toggleCalibrationTone,
        onSelectRadarTarget = viewModel::selectRadarTarget,
        onStopRadarMonitor = viewModel::stopRadarMonitor,
        onSelectEqDevice = viewModel::selectEqDevice,
        onApplyEqPreset = viewModel::applyEqPreset,
        onEqBandChange = viewModel::updateEqBand,
        onBassVirtualizerBalanceChange = viewModel::updateBassAndVirtualizer,
        onUpdateAutoRules = viewModel::updateAutoConnectRule,
      )
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshPermissionsAndServices()
  }
}
