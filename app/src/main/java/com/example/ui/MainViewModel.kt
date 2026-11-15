package com.example.ui

import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSyncDspEngine
import com.example.bluetooth.BluetoothHardwareController
import com.example.bluetooth.PermissionHelper
import com.example.data.AppPreferences
import com.example.data.AppPreferencesRepository
import com.example.data.BluePulseDatabase
import com.example.data.BluetoothAudioRepository
import com.example.data.DeviceProfileEntity
import com.example.model.AudioOutputSink
import com.example.model.BluetoothDeviceUiModel
import com.example.model.BondStatus
import com.example.model.CalibrationMode
import com.example.model.CodecInfo
import com.example.model.DeviceCategory
import com.example.model.DualAudioRouteMode
import com.example.model.DualSyncState
import com.example.model.EqBandUiModel
import com.example.model.FeatureGuideId
import com.example.model.FloatingIslandEvent
import com.example.model.MediaPlaybackState
import com.example.model.PermissionStatus
import com.example.model.RssiSample
import com.example.overlay.FloatingIslandEventBus
import com.example.overlay.FloatingIslandOverlayService
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BluePulseTab(val title: String) {
  HUB("Hub"),
  DUAL_SYNC("Dual Sync"),
  RADAR("Radar"),
  EQ_RULES("EQ & Rules"),
}

data class MainUiState(
  val selectedTab: BluePulseTab = BluePulseTab.HUB,
  val isBluetoothSupported: Boolean = true,
  val isBleSupported: Boolean = true,
  val isBluetoothEnabled: Boolean = false,
  val isScanning: Boolean = false,
  val devices: List<BluetoothDeviceUiModel> = emptyList(),
  val availableAudioSinks: List<AudioOutputSink> = emptyList(),
  val rssiHistory: Map<String, List<RssiSample>> = emptyMap(),
  val radarTargetAddress: String? = null,
  val selectedEqDeviceAddress: String? = null,
  val dualSync: DualSyncState = DualSyncState(),
  val mediaPlayback: MediaPlaybackState = MediaPlaybackState(),
  val eqBands: List<EqBandUiModel> = AudioSyncDspEngine.defaultBands(),
  val activeEqPreset: String = "Studio Flat",
  val bassBoostStrength: Int = 150,
  val virtualizerStrength: Int = 100,
  val channelBalance: Float = 0f,
  val dspHardwareAttached: Boolean = false,
  val savedProfiles: List<DeviceProfileEntity> = emptyList(),
  val preferences: AppPreferences = AppPreferences(),
  val permissions: PermissionStatus = PermissionStatus(),
  val activeIslandEvent: FloatingIslandEvent? = null,
  val isIslandExpanded: Boolean = false,
  val showPermissionSheet: Boolean = false,
  val showSettingsDialog: Boolean = false,
  val activeFeatureGuide: FeatureGuideId? = null,
  val active3DModalDevice: BluetoothDeviceUiModel? = null,
  val statusMessage: String? = null,
)

private data class HardwareSnapshot(
  val btSupported: Boolean,
  val bleSupported: Boolean,
  val btEnabled: Boolean,
  val scanning: Boolean,
  val devices: List<BluetoothDeviceUiModel>,
  val sinks: List<AudioOutputSink>,
  val rssiHistory: Map<String, List<RssiSample>>,
  val leAudioSupported: Boolean,
  val leBroadcastSupported: Boolean,
)

private data class DspSnapshot(
  val eqBands: List<EqBandUiModel>,
  val hardwareAttached: Boolean,
  val playingCalibration: Boolean,
  val beatTick: Int,
  val mediaPlayback: MediaPlaybackState,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val appContext = application.applicationContext
  private val btController = BluetoothHardwareController(appContext)
  private val dspEngine = AudioSyncDspEngine(appContext)
  private val prefsRepo = AppPreferencesRepository(appContext)
  private val db = BluePulseDatabase.getInstance(appContext)
  private val audioRepo = BluetoothAudioRepository(db.deviceProfileDao())

  private val selectedTab = MutableStateFlow(BluePulseTab.HUB)
  private val radarTargetAddress = MutableStateFlow<String?>(null)
  private val selectedEqAddress = MutableStateFlow<String?>(null)
  private val dualSyncState = MutableStateFlow(
    DualSyncState(
      systemMediaVolumePercent = dspEngine.getSystemMusicVolumePercent(),
    )
  )
  private val permissionStatus = MutableStateFlow(PermissionHelper.getPermissionStatus(appContext))
  private val showPermissionSheet = MutableStateFlow(false)
  private val showSettingsDialog = MutableStateFlow(false)
  private val activeFeatureGuide = MutableStateFlow<FeatureGuideId?>(null)
  private val active3DModalDevice = MutableStateFlow<BluetoothDeviceUiModel?>(null)
  private val statusMessage = MutableStateFlow<String?>(null)
  private val activeEqPreset = MutableStateFlow("Studio Flat")
  private val bassBoostStrength = MutableStateFlow(150)
  private val virtualizerStrength = MutableStateFlow(100)
  private val channelBalance = MutableStateFlow(0f)

  private val hardwareFlow = combine(
    btController.isBluetoothSupported,
    btController.isBleSupported,
    btController.isBluetoothEnabled,
    btController.isScanning,
    btController.devices,
    btController.availableAudioSinks,
    btController.rssiHistory,
    btController.leAudioSupported,
    btController.leAudioBroadcastSupported,
  ) { values ->
    @Suppress("UNCHECKED_CAST")
    HardwareSnapshot(
      btSupported = values[0] as Boolean,
      bleSupported = values[1] as Boolean,
      btEnabled = values[2] as Boolean,
      scanning = values[3] as Boolean,
      devices = values[4] as List<BluetoothDeviceUiModel>,
      sinks = values[5] as List<AudioOutputSink>,
      rssiHistory = values[6] as Map<String, List<RssiSample>>,
      leAudioSupported = values[7] as Boolean,
      leBroadcastSupported = values[8] as Boolean,
    )
  }

  private val dspFlow = combine(
    dspEngine.eqBands,
    dspEngine.dspAttachedToHardware,
    dspEngine.isPlayingCalibration,
    dspEngine.calibrationBeatTick,
    dspEngine.mediaPlayback,
  ) { bands, attached, playing, tick, media ->
    DspSnapshot(
      eqBands = bands,
      hardwareAttached = attached,
      playingCalibration = playing,
      beatTick = tick,
      mediaPlayback = media,
    )
  }

  private val islandFlow = combine(
    FloatingIslandEventBus.activeEvent,
    FloatingIslandEventBus.isExpanded,
  ) { event, expanded -> event to expanded }

  private val eqUiMetaFlow = combine(
    activeEqPreset,
    bassBoostStrength,
    virtualizerStrength,
    channelBalance,
  ) { preset, bass, virt, bal -> EqMeta(preset, bass, virt, bal) }

  private data class EqMeta(
    val preset: String,
    val bass: Int,
    val virt: Int,
    val balance: Float,
  )

  private val modalStateFlow = combine(
    showPermissionSheet,
    showSettingsDialog,
    activeFeatureGuide,
    active3DModalDevice,
    statusMessage,
  ) { perm, settings, guide, modal3D, msg ->
    ModalSnapshot(perm, settings, guide, modal3D, msg)
  }

  private data class ModalSnapshot(
    val permSheet: Boolean,
    val settingsDlg: Boolean,
    val featureGuide: FeatureGuideId?,
    val modal3DDevice: BluetoothDeviceUiModel?,
    val message: String?,
  )

  private val uiControlsFlow = combine(
    selectedTab,
    radarTargetAddress,
    selectedEqAddress,
    dualSyncState,
    permissionStatus,
    modalStateFlow,
  ) { values ->
    ControlsSnapshot(
      tab = values[0] as BluePulseTab,
      radarTarget = values[1] as String?,
      eqTarget = values[2] as String?,
      dualSync = values[3] as DualSyncState,
      perms = values[4] as PermissionStatus,
      modals = values[5] as ModalSnapshot,
    )
  }

  private data class ControlsSnapshot(
    val tab: BluePulseTab,
    val radarTarget: String?,
    val eqTarget: String?,
    val dualSync: DualSyncState,
    val perms: PermissionStatus,
    val modals: ModalSnapshot,
  )

  val uiState: StateFlow<MainUiState> = combine(
    hardwareFlow,
    dspFlow,
    audioRepo.profiles,
    prefsRepo.preferencesFlow,
    islandFlow,
    eqUiMetaFlow,
    uiControlsFlow,
  ) { values ->
    val hw = values[0] as HardwareSnapshot
    val dsp = values[1] as DspSnapshot
    @Suppress("UNCHECKED_CAST")
    val profiles = values[2] as List<DeviceProfileEntity>
    val prefs = values[3] as AppPreferences
    @Suppress("UNCHECKED_CAST")
    val island = values[4] as Pair<FloatingIslandEvent?, Boolean>
    val eqMeta = values[5] as EqMeta
    val ctrl = values[6] as ControlsSnapshot

    val defaultSinkA = hw.sinks.firstOrNull { it.isBluetooth } ?: hw.sinks.firstOrNull()
    val defaultSinkB = hw.sinks.firstOrNull { it.id != defaultSinkA?.id && (it.isWiredOrUsb || it.isBluetooth || it.isSpeaker) }
      ?: hw.sinks.lastOrNull()

    val resolvedDualSync = ctrl.dualSync.copy(
      isCalibrationPlaying = dsp.playingCalibration,
      calibrationBeatIndex = dsp.beatTick,
      leAudioSupported = hw.leAudioSupported,
      leAudioBroadcastSupported = hw.leBroadcastSupported,
      sinkAId = ctrl.dualSync.sinkAId ?: defaultSinkA?.id,
      sinkBId = ctrl.dualSync.sinkBId ?: defaultSinkB?.id,
      deviceAAddress = ctrl.dualSync.deviceAAddress ?: hw.devices.getOrNull(0)?.address,
      deviceBAddress = ctrl.dualSync.deviceBAddress ?: hw.devices.getOrNull(1)?.address,
    )

    MainUiState(
      selectedTab = ctrl.tab,
      isBluetoothSupported = hw.btSupported,
      isBleSupported = hw.bleSupported,
      isBluetoothEnabled = hw.btEnabled,
      isScanning = hw.scanning,
      devices = hw.devices,
      availableAudioSinks = hw.sinks,
      rssiHistory = hw.rssiHistory,
      radarTargetAddress = ctrl.radarTarget ?: hw.devices.firstOrNull()?.address,
      selectedEqDeviceAddress = ctrl.eqTarget ?: hw.devices.firstOrNull()?.address,
      dualSync = resolvedDualSync,
      mediaPlayback = dsp.mediaPlayback,
      eqBands = dsp.eqBands,
      activeEqPreset = eqMeta.preset,
      bassBoostStrength = eqMeta.bass,
      virtualizerStrength = eqMeta.virt,
      channelBalance = eqMeta.balance,
      dspHardwareAttached = dsp.hardwareAttached,
      savedProfiles = profiles,
      preferences = prefs,
      permissions = ctrl.perms,
      activeIslandEvent = island.first,
      isIslandExpanded = island.second,
      showPermissionSheet = ctrl.modals.permSheet || (!ctrl.perms.bluetoothGranted && !prefs.permissionOnboardingDismissed),
      showSettingsDialog = ctrl.modals.settingsDlg,
      activeFeatureGuide = ctrl.modals.featureGuide,
      active3DModalDevice = ctrl.modals.modal3DDevice,
      statusMessage = ctrl.modals.message,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000L),
    initialValue = MainUiState(),
  )

  init {
    btController.start()
    refreshPermissionsAndServices()
  }

  fun refreshPermissionsAndServices() {
    val perms = PermissionHelper.getPermissionStatus(appContext)
    permissionStatus.value = perms
    if (perms.bluetoothGranted) {
      btController.refreshBondedAndConnected()
    } else {
      btController.refreshAudioOutputSinks()
    }
    if (perms.overlayGranted && uiState.value.preferences.systemOverlayEnabled) {
      FloatingIslandOverlayService.startIfPermitted(appContext)
    }
  }

  fun selectTab(tab: BluePulseTab) {
    selectedTab.value = tab
    if (tab != BluePulseTab.RADAR) {
      btController.stopRssiRadarMonitor()
    }
  }

  fun openFeatureGuide(id: FeatureGuideId) {
    activeFeatureGuide.value = id
  }

  fun closeFeatureGuide() {
    activeFeatureGuide.value = null
  }

  fun open3DConnectionModal(device: BluetoothDeviceUiModel?) {
    val target = device ?: uiState.value.devices.firstOrNull() ?: BluetoothDeviceUiModel(
      address = "3D-STUDIO-STAGE",
      name = "Wireless Audio Buds",
      category = DeviceCategory.EARBUDS,
      bondStatus = BondStatus.NONE,
      isConnected = false,
      isAudioSink = true,
      isLeAudioSupported = uiState.value.dualSync.leAudioSupported,
      activeProfile = "Ready to Pair",
      rssiDbm = -48,
      batteryPercent = 94,
      leftBatteryPercent = 94,
      rightBatteryPercent = 92,
      caseBatteryPercent = 100,
      codecInfo = CodecInfo(),
    )
    active3DModalDevice.value = target
  }

  fun close3DConnectionModal() {
    active3DModalDevice.value = null
  }

  fun launchCompanionPairing(onChooserIntent: (IntentSender) -> Unit) {
    if (!PermissionHelper.hasBluetoothPermissions(appContext)) {
      showPermissionSheet.value = true
      return
    }
    btController.startCompanionPairing(
      onChooserLauncher = onChooserIntent,
      onError = { err -> statusMessage.value = err },
    )
  }

  fun handleCompanionDeviceSelected(device: android.bluetooth.BluetoothDevice?) {
    btController.bondDevice(device)
    btController.refreshBondedAndConnected()
    statusMessage.value = "Companion device linked"
  }

  fun setShowPermissionSheet(show: Boolean) {
    showPermissionSheet.value = show
    if (!show) {
      viewModelScope.launch {
        prefsRepo.setPermissionOnboardingDismissed(true)
      }
    }
  }

  fun setShowSettingsDialog(show: Boolean) {
    showSettingsDialog.value = show
  }

  fun clearStatusMessage() {
    statusMessage.value = null
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      prefsRepo.setThemeMode(mode)
    }
  }

  fun setSystemOverlayEnabled(enabled: Boolean) {
    viewModelScope.launch {
      prefsRepo.setSystemOverlayEnabled(enabled)
      if (enabled && PermissionHelper.canDrawOverlays(appContext)) {
        FloatingIslandOverlayService.startIfPermitted(appContext)
      } else if (!enabled) {
        FloatingIslandOverlayService.stop(appContext)
      }
    }
  }

  fun setRadarHapticsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      prefsRepo.setRadarHapticsEnabled(enabled)
    }
  }

  fun toggleScan() {
    if (!PermissionHelper.hasBluetoothPermissions(appContext)) {
      showPermissionSheet.value = true
      return
    }
    if (btController.isScanning.value) {
      btController.stopScan()
    } else {
      btController.startScan()
    }
  }

  fun refreshDevices() {
    btController.refreshBondedAndConnected()
  }

  fun pairOrConnectDevice(device: BluetoothDeviceUiModel) {
    if (device.address == "3D-STUDIO-STAGE") {
      openSystemBluetoothSettings()
      return
    }
    if (!PermissionHelper.hasBluetoothPermissions(appContext)) {
      showPermissionSheet.value = true
      return
    }
    val initiated = btController.pairOrConnectDevice(device.address)
    if (initiated) {
      statusMessage.value = "Requesting link with ${device.name}"
      FloatingIslandEventBus.postEvent(
        FloatingIslandEvent.DeviceConnected(
          deviceName = device.name,
          batteryPercent = device.batteryPercent,
          codecLabel = device.codecInfo.codecName,
        )
      )
    } else {
      statusMessage.value = "Opening system Bluetooth to finish connecting ${device.name}"
      openSystemBluetoothSettings()
    }
  }

  fun openSystemBluetoothSettings() {
    runCatching {
      appContext.startActivity(PermissionHelper.createBluetoothSettingsIntent())
    }
  }

  fun openOverlayPermissionSettings(): Intent {
    return PermissionHelper.createOverlaySettingsIntent(appContext)
  }

  // Dual Audio Bridge, Media File Player & Latency Calibration Controls
  fun setDualRouteMode(mode: DualAudioRouteMode) {
    dualSyncState.update { curr ->
      val next = curr.copy(routeMode = mode)
      pushRoutingToDsp(next)
      next
    }
  }

  fun setMonoMixForSplit(monoMix: Boolean) {
    dualSyncState.update { curr ->
      val next = curr.copy(monoMixForSplit = monoMix)
      pushRoutingToDsp(next)
      next
    }
  }

  fun assignAudioSinkA(sinkId: Int) {
    dualSyncState.update { curr ->
      val next = curr.copy(sinkAId = sinkId)
      pushRoutingToDsp(next)
      next
    }
  }

  fun assignAudioSinkB(sinkId: Int) {
    dualSyncState.update { curr ->
      val next = curr.copy(sinkBId = sinkId)
      pushRoutingToDsp(next)
      next
    }
  }

  fun assignDeviceA(address: String) {
    val savedOffset = uiState.value.savedProfiles.firstOrNull { it.address == address }?.latencyOffsetMs
    dualSyncState.update { curr ->
      val next = curr.copy(
        deviceAAddress = address,
        deviceALatencyMs = savedOffset ?: curr.deviceALatencyMs,
      )
      pushDspCalibrationParams(next)
      next
    }
  }

  fun assignDeviceB(address: String) {
    val savedOffset = uiState.value.savedProfiles.firstOrNull { it.address == address }?.latencyOffsetMs
    dualSyncState.update { curr ->
      val next = curr.copy(
        deviceBAddress = address,
        deviceBLatencyMs = savedOffset ?: curr.deviceBLatencyMs,
      )
      pushDspCalibrationParams(next)
      next
    }
  }

  fun loadAudioFile(uri: Uri) {
    dspEngine.loadAudioFile(uri)
    statusMessage.value = "Loaded track into Dual Audio Engine"
  }

  fun clearAudioFile() {
    dspEngine.clearCustomAudioFile()
  }

  fun setDualLinked(linked: Boolean) {
    dualSyncState.update { curr ->
      val next = if (linked) {
        curr.copy(
          isLinked = true,
          deviceAVolume = curr.masterVolume,
          deviceBVolume = curr.masterVolume,
          deviceBLatencyMs = curr.deviceALatencyMs,
          deviceBBalance = curr.deviceABalance,
        )
      } else {
        curr.copy(isLinked = false)
      }
      pushDspCalibrationParams(next)
      next
    }
    val curr = dualSyncState.value
    FloatingIslandEventBus.postEvent(
      FloatingIslandEvent.DualSyncUpdated(
        linked = linked,
        offsetDeltaMs = curr.deviceBLatencyMs - curr.deviceALatencyMs,
        volumePercent = (curr.masterVolume * 100).toInt(),
      ),
      autoExpand = false,
    )
  }

  fun updateMasterVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    dualSyncState.update { curr ->
      val next = curr.copy(
        masterVolume = clamped,
        deviceAVolume = if (curr.isLinked) clamped else curr.deviceAVolume,
        deviceBVolume = if (curr.isLinked) clamped else curr.deviceBVolume,
        systemMediaVolumePercent = (clamped * 100).toInt(),
      )
      pushDspCalibrationParams(next)
      next
    }
    dspEngine.setSystemMusicVolumePercent((clamped * 100).toInt())
  }

  fun updateDeviceAVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    dualSyncState.update { curr ->
      val next = if (curr.isLinked) {
        curr.copy(masterVolume = clamped, deviceAVolume = clamped, deviceBVolume = clamped)
      } else {
        curr.copy(deviceAVolume = clamped)
      }
      pushDspCalibrationParams(next)
      next
    }
  }

  fun updateDeviceBVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    dualSyncState.update { curr ->
      val next = if (curr.isLinked) {
        curr.copy(masterVolume = clamped, deviceAVolume = clamped, deviceBVolume = clamped)
      } else {
        curr.copy(deviceBVolume = clamped)
      }
      pushDspCalibrationParams(next)
      next
    }
  }

  fun updateDeviceALatency(offsetMs: Int) {
    val clamped = offsetMs.coerceIn(-250, 250)
    dualSyncState.update { curr ->
      val next = if (curr.isLinked) {
        curr.copy(deviceALatencyMs = clamped, deviceBLatencyMs = clamped)
      } else {
        curr.copy(deviceALatencyMs = clamped)
      }
      pushDspCalibrationParams(next)
      next
    }
    persistLatencyIfAssigned(dualSyncState.value.deviceAAddress, clamped)
  }

  fun updateDeviceBLatency(offsetMs: Int) {
    val clamped = offsetMs.coerceIn(-250, 250)
    dualSyncState.update { curr ->
      val next = if (curr.isLinked) {
        curr.copy(deviceALatencyMs = clamped, deviceBLatencyMs = clamped)
      } else {
        curr.copy(deviceBLatencyMs = clamped)
      }
      pushDspCalibrationParams(next)
      next
    }
    persistLatencyIfAssigned(dualSyncState.value.deviceBAddress, clamped)
  }

  fun updateDeviceABalance(balance: Float) {
    val clamped = balance.coerceIn(-1f, 1f)
    dualSyncState.update { curr ->
      val next = if (curr.isLinked) {
        curr.copy(deviceABalance = clamped, deviceBBalance = clamped)
      } else {
        curr.copy(deviceABalance = clamped)
      }
      pushDspCalibrationParams(next)
      next
    }
  }

  fun updateDeviceBBalance(balance: Float) {
    val clamped = balance.coerceIn(-1f, 1f)
    dualSyncState.update { curr ->
      val next = if (curr.isLinked) {
        curr.copy(deviceABalance = clamped, deviceBBalance = clamped)
      } else {
        curr.copy(deviceBBalance = clamped)
      }
      pushDspCalibrationParams(next)
      next
    }
  }

  fun setCalibrationMode(mode: CalibrationMode) {
    dualSyncState.update { curr ->
      val next = curr.copy(calibrationMode = mode)
      pushDspCalibrationParams(next)
      next
    }
  }

  fun toggleCalibrationTone() {
    val stateNow = uiState.value.dualSync
    pushRoutingToDsp(stateNow)
    pushDspCalibrationParams(stateNow)
    dspEngine.toggleCalibrationTone()
  }

  private fun pushRoutingToDsp(state: DualSyncState) {
    dspEngine.updateDualRouting(
      routeMode = state.routeMode,
      monoMixForSplit = state.monoMixForSplit,
      sinkA = btController.findAudioDeviceInfoById(state.sinkAId),
      sinkB = btController.findAudioDeviceInfoById(state.sinkBId),
    )
  }

  private fun pushDspCalibrationParams(state: DualSyncState) {
    dspEngine.updateDualCalibrationParameters(
      volA = state.deviceAVolume,
      volB = state.deviceBVolume,
      delayAMs = state.deviceALatencyMs,
      delayBMs = state.deviceBLatencyMs,
      balanceA = state.deviceABalance,
      balanceB = state.deviceBBalance,
      mode = state.calibrationMode,
    )
  }

  private fun persistLatencyIfAssigned(address: String?, offsetMs: Int) {
    val addr = address ?: return
    val name = uiState.value.devices.firstOrNull { it.address == addr }?.name ?: "Output Device"
    viewModelScope.launch {
      audioRepo.updateLatencyOffset(addr, name, offsetMs)
    }
  }

  // Radar & Signal Monitoring
  fun selectRadarTarget(address: String) {
    radarTargetAddress.value = address
    btController.startRssiRadarMonitor(address)
    val device = uiState.value.devices.firstOrNull { it.address == address }
    val rssi = device?.rssiDbm
    if (device != null && rssi != null) {
      FloatingIslandEventBus.postEvent(
        FloatingIslandEvent.RadarProximityAlert(
          deviceName = device.name,
          rssiDbm = rssi,
          proximityLabel = estimateProximityLabel(rssi),
        ),
        autoExpand = false,
      )
    }
  }

  fun stopRadarMonitor() {
    btController.stopRssiRadarMonitor()
  }

  // Per-Device EQ & Auto-Connect Rules
  fun selectEqDevice(address: String) {
    selectedEqAddress.value = address
    viewModelScope.launch {
      val profile = uiState.value.savedProfiles.firstOrNull { it.address == address }
      if (profile != null) {
        val levels = profile.eqBandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (levels.isNotEmpty()) {
          activeEqPreset.value = profile.eqPresetName
          bassBoostStrength.value = profile.bassBoostStrength
          virtualizerStrength.value = profile.virtualizerStrength
          channelBalance.value = profile.channelBalance
          dspEngine.applyEqPreset(levels, profile.bassBoostStrength, profile.virtualizerStrength)
        }
      }
    }
  }

  fun updateEqBand(bandIndex: Int, millibels: Int) {
    activeEqPreset.value = "Custom"
    dspEngine.setBandLevel(bandIndex, millibels)
    saveActiveEqToRoom()
  }

  fun applyEqPreset(presetName: String) {
    val triple = AudioSyncDspEngine.PRESETS[presetName] ?: return
    activeEqPreset.value = presetName
    bassBoostStrength.value = triple.second
    virtualizerStrength.value = triple.third
    dspEngine.applyEqPreset(triple.first, triple.second, triple.third)
    saveActiveEqToRoom()

    val devName = uiState.value.devices.firstOrNull {
      it.address == uiState.value.selectedEqDeviceAddress
    }?.name ?: "Active Output"
    FloatingIslandEventBus.postEvent(
      FloatingIslandEvent.EqProfileApplied(deviceName = devName, presetName = presetName),
      autoExpand = false,
    )
  }

  fun updateBassAndVirtualizer(bass: Int, virtualizer: Int, balance: Float) {
    bassBoostStrength.value = bass.coerceIn(0, 1000)
    virtualizerStrength.value = virtualizer.coerceIn(0, 1000)
    channelBalance.value = balance.coerceIn(-1f, 1f)
    val levels = dspEngine.eqBands.value.map { it.levelMillibels }
    dspEngine.applyEqPreset(levels, bassBoostStrength.value, virtualizerStrength.value)
    saveActiveEqToRoom()
  }

  fun updateAutoConnectRule(
    address: String,
    defaultName: String,
    applyEq: Boolean,
    launchIsland: Boolean,
    setVolume: Boolean,
    volumePercent: Int,
  ) {
    viewModelScope.launch {
      audioRepo.updateAutoConnectRules(
        address = address,
        defaultName = defaultName,
        applyEq = applyEq,
        launchIsland = launchIsland,
        setVolume = setVolume,
        volumePercent = volumePercent,
      )
    }
  }

  private fun saveActiveEqToRoom() {
    val addr = uiState.value.selectedEqDeviceAddress ?: "DEFAULT_PROFILE"
    val name = uiState.value.devices.firstOrNull { it.address == addr }?.name ?: "Default Audio Profile"
    val levels = dspEngine.eqBands.value.map { it.levelMillibels }
    viewModelScope.launch {
      audioRepo.updateEqSettings(
        address = addr,
        defaultName = name,
        presetName = activeEqPreset.value,
        bandLevels = levels,
        bassBoost = bassBoostStrength.value,
        virtualizer = virtualizerStrength.value,
        channelBalance = channelBalance.value,
      )
    }
  }

  // Floating Island Actions
  fun toggleIslandExpanded() {
    FloatingIslandEventBus.setExpanded(!FloatingIslandEventBus.isExpanded.value)
  }

  fun dismissIsland() {
    FloatingIslandEventBus.dismiss()
  }

  fun previewFloatingIslandPopup() {
    val activeDevice = uiState.value.devices.firstOrNull { it.isConnected }
      ?: uiState.value.devices.firstOrNull()
    val event = if (activeDevice != null) {
      FloatingIslandEvent.DeviceConnected(
        deviceName = activeDevice.name,
        batteryPercent = activeDevice.batteryPercent ?: 88,
        codecLabel = activeDevice.codecInfo.codecName,
      )
    } else {
      FloatingIslandEvent.DualSyncUpdated(
        linked = uiState.value.dualSync.isLinked,
        offsetDeltaMs = uiState.value.dualSync.deviceBLatencyMs - uiState.value.dualSync.deviceALatencyMs,
        volumePercent = (uiState.value.dualSync.masterVolume * 100).toInt(),
      )
    }
    FloatingIslandEventBus.postEvent(event, autoExpand = true)
  }

  override fun onCleared() {
    super.onCleared()
    btController.stop()
    dspEngine.release()
  }

  companion object {
    fun estimateProximityLabel(rssiDbm: Int): String = when {
      rssiDbm >= -52 -> "Immediate (< 1m)"
      rssiDbm >= -68 -> "Near (1–3m)"
      rssiDbm >= -82 -> "Room Range (3–8m)"
      else -> "Far (> 8m)"
    }
  }
}
