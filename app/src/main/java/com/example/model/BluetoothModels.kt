package com.example.model

enum class DeviceCategory(val label: String) {
  EARBUDS("Earbuds"),
  HEADPHONES("Headphones"),
  SPEAKER("Speaker"),
  WATCH("Wearable"),
  OTHER("Audio / BT"),
}

enum class BondStatus(val label: String) {
  BONDED("Paired"),
  BONDING("Pairing..."),
  NONE("Nearby"),
}

data class CodecInfo(
  val codecName: String = "AAC / Auto",
  val sampleRateKhz: String = "48.0 kHz",
  val bitsPerSample: String = "24-bit",
  val channelMode: String = "Stereo",
  val bitrateLabel: String = "Adaptive",
  val estimatedLatencyMs: Int = 140,
)

data class BluetoothDeviceUiModel(
  val address: String,
  val name: String,
  val category: DeviceCategory,
  val bondStatus: BondStatus,
  val isConnected: Boolean,
  val isAudioSink: Boolean,
  val isLeAudioSupported: Boolean,
  val activeProfile: String,
  val rssiDbm: Int?,
  val batteryPercent: Int?,
  val leftBatteryPercent: Int? = null,
  val rightBatteryPercent: Int? = null,
  val caseBatteryPercent: Int? = null,
  val codecInfo: CodecInfo = CodecInfo(),
  val lastSeenTimestamp: Long = System.currentTimeMillis(),
)

data class RssiSample(
  val timestampMs: Long,
  val rssiDbm: Int,
)

data class AudioOutputSink(
  val id: Int,
  val name: String,
  val typeLabel: String,
  val address: String,
  val isBluetooth: Boolean,
  val isScoOrComm: Boolean,
  val isWiredOrUsb: Boolean,
  val isSpeaker: Boolean,
)

enum class DualAudioRouteMode(
  val title: String,
  val badge: String,
  val subtitle: String,
) {
  NATIVE_MULTI_BT(
    title = "LE Audio / Multi-BT",
    badge = "NATIVE",
    subtitle = "Streams to two Bluetooth sinks when Android LE Audio or OEM Dual Audio is active",
  ),
  BT_PLUS_WIRED_USB(
    title = "BT + USB-C / Wired",
    badge = "BRIDGE",
    subtitle = "Simultaneous Bluetooth A2DP + Wired/USB-C or Speaker output on ANY phone",
  ),
  BT_A2DP_PLUS_SCO(
    title = "A2DP + SCO Dual BT",
    badge = "HYBRID",
    subtitle = "Routes Stream A via A2DP media and Stream B via Bluetooth HFP/SCO channel",
  ),
  TWS_SPLIT_LR(
    title = "TWS Split L / R",
    badge = "UNIVERSAL",
    subtitle = "Independent Left & Right earbud volume, mono mix & ms delay on any earbuds",
  ),
}

enum class CalibrationMode(val title: String, val description: String) {
  AMBIENT_GROOVE("Synth Loop", "Warm harmonic chord loop to test Dual Audio & EQ live"),
  SYNC_PULSE("Sync Pulse", "Rhythmic dual-channel click track for phase & lip-sync"),
  STEREO_SWEEP("Stereo Sweep", "Alternating L/R acoustic pulse across Output A & B"),
  PHASE_CHIRP("Phase Chirp", "Crisp 880 Hz timing chirp to align millisecond drift"),
}

data class MediaPlaybackState(
  val trackTitle: String = "Built-In Synth Loop",
  val selectedUriString: String? = null,
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 30_000L,
  val isCustomFile: Boolean = false,
)

data class DualSyncState(
  val routeMode: DualAudioRouteMode = DualAudioRouteMode.BT_PLUS_WIRED_USB,
  val sinkAId: Int? = null,
  val sinkBId: Int? = null,
  val deviceAAddress: String? = null,
  val deviceBAddress: String? = null,
  val isLinked: Boolean = true,
  val monoMixForSplit: Boolean = true,
  val masterVolume: Float = 0.78f,
  val deviceAVolume: Float = 0.78f,
  val deviceBVolume: Float = 0.78f,
  val deviceALatencyMs: Int = 0,
  val deviceBLatencyMs: Int = 0,
  val deviceABalance: Float = 0f,
  val deviceBBalance: Float = 0f,
  val isCalibrationPlaying: Boolean = false,
  val calibrationMode: CalibrationMode = CalibrationMode.AMBIENT_GROOVE,
  val calibrationBeatIndex: Int = 0,
  val leAudioSupported: Boolean = false,
  val leAudioBroadcastSupported: Boolean = false,
  val systemMediaVolumePercent: Int = 75,
)

data class EqBandUiModel(
  val index: Int,
  val centerFreqLabel: String,
  val levelMillibels: Int,
  val minMillibels: Int = -1500,
  val maxMillibels: Int = 1500,
)

data class PermissionStatus(
  val bluetoothGranted: Boolean = false,
  val notificationsGranted: Boolean = false,
  val overlayGranted: Boolean = false,
) {
  val allCoreGranted: Boolean
    get() = bluetoothGranted && notificationsGranted
}
