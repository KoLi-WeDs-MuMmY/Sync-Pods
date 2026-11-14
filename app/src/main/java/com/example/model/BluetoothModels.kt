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

enum class CalibrationMode(val title: String, val description: String) {
  SYNC_PULSE("Sync Pulse", "Rhythmic dual-channel click track for phase & lip-sync"),
  STEREO_SWEEP("Stereo Sweep", "Alternating L/R acoustic pulse across Output A & B"),
  PHASE_CHIRP("Phase Chirp", "Crisp 880 Hz timing chirp to align millisecond drift"),
}

data class DualSyncState(
  val deviceAAddress: String? = null,
  val deviceBAddress: String? = null,
  val isLinked: Boolean = true,
  val masterVolume: Float = 0.72f,
  val deviceAVolume: Float = 0.72f,
  val deviceBVolume: Float = 0.72f,
  val deviceALatencyMs: Int = 0,
  val deviceBLatencyMs: Int = 0,
  val deviceABalance: Float = 0f,
  val deviceBBalance: Float = 0f,
  val isCalibrationPlaying: Boolean = false,
  val calibrationMode: CalibrationMode = CalibrationMode.SYNC_PULSE,
  val calibrationBeatIndex: Int = 0,
  val leAudioSupported: Boolean = false,
  val leAudioBroadcastSupported: Boolean = false,
  val systemMediaVolumePercent: Int = 70,
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
