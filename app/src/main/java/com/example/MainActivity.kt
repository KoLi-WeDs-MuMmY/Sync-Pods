package com.example

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
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

      val companionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
      ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
          val selectedDevice = extractCompanionDevice(result.data)
          viewModel.handleCompanionDeviceSelected(selectedDevice)
        }
      }

      val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
      ) { uri ->
        if (uri != null) {
          runCatching {
            contentResolver.takePersistableUriPermission(
              uri,
              Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
          }
          viewModel.loadAudioFile(uri)
        }
      }

      BluePulseApp(
        state = state,
        onSelectTab = viewModel::selectTab,
        onToggleScan = viewModel::toggleScan,
        onRefreshDevices = viewModel::refreshDevices,
        onLaunchCompanionPair = {
          viewModel.launchCompanionPairing { intentSender ->
            companionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
          }
        },
        onOpen3DModal = viewModel::open3DConnectionModal,
        onClose3DModal = viewModel::close3DConnectionModal,
        onOpenFeatureGuide = viewModel::openFeatureGuide,
        onCloseFeatureGuide = viewModel::closeFeatureGuide,
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
        onSetDualRouteMode = viewModel::setDualRouteMode,
        onSetMonoMixSplit = viewModel::setMonoMixForSplit,
        onAssignSinkA = viewModel::assignAudioSinkA,
        onAssignSinkB = viewModel::assignAudioSinkB,
        onPickAudioFile = { audioFilePickerLauncher.launch(arrayOf("audio/*")) },
        onClearAudioFile = viewModel::clearAudioFile,
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

  @Suppress("DEPRECATION")
  private fun extractCompanionDevice(data: Intent?): BluetoothDevice? {
    if (data == null) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
      data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshPermissionsAndServices()
  }
}
