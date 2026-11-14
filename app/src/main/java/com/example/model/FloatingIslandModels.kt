package com.example.model

enum class IslandTone {
  PRIMARY,
  EMERALD,
  AMBER,
  ALERT,
}

sealed class FloatingIslandEvent(
  val id: Long = System.currentTimeMillis(),
) {
  abstract val title: String
  abstract val subtitle: String
  abstract val tone: IslandTone
  abstract val badgeText: String

  data class DeviceConnected(
    val deviceName: String,
    val batteryPercent: Int?,
    val codecLabel: String,
    override val tone: IslandTone = IslandTone.EMERALD,
  ) : FloatingIslandEvent() {
    override val title: String = deviceName
    override val subtitle: String = buildString {
      append("Connected • ")
      append(codecLabel)
      if (batteryPercent != null) append(" • $batteryPercent%")
    }
    override val badgeText: String = batteryPercent?.let { "$it%" } ?: "LINK"
  }

  data class DeviceDisconnected(
    val deviceName: String,
    override val tone: IslandTone = IslandTone.AMBER,
  ) : FloatingIslandEvent() {
    override val title: String = deviceName
    override val subtitle: String = "Bluetooth link disconnected"
    override val badgeText: String = "OFF"
  }

  data class DualSyncUpdated(
    val linked: Boolean,
    val offsetDeltaMs: Int,
    val volumePercent: Int,
    override val tone: IslandTone = IslandTone.PRIMARY,
  ) : FloatingIslandEvent() {
    override val title: String = if (linked) "Dual Output Linked" else "Independent Dual Sync"
    override val subtitle: String = "Offset ${if (offsetDeltaMs >= 0) "+$offsetDeltaMs" else offsetDeltaMs} ms • Output $volumePercent%"
    override val badgeText: String = if (linked) "SYNC" else "DUAL"
  }

  data class BatteryStatus(
    val deviceName: String,
    val batteryPercent: Int,
    val leftPercent: Int?,
    val rightPercent: Int?,
    override val tone: IslandTone = if (batteryPercent <= 20) IslandTone.ALERT else IslandTone.EMERALD,
  ) : FloatingIslandEvent() {
    override val title: String = "$deviceName Battery"
    override val subtitle: String = if (leftPercent != null && rightPercent != null) {
      "L $leftPercent% • R $rightPercent% • Overall $batteryPercent%"
    } else {
      "Battery level at $batteryPercent%"
    }
    override val badgeText: String = "$batteryPercent%"
  }

  data class RadarProximityAlert(
    val deviceName: String,
    val rssiDbm: Int,
    val proximityLabel: String,
    override val tone: IslandTone = if (rssiDbm >= -60) IslandTone.EMERALD else IslandTone.PRIMARY,
  ) : FloatingIslandEvent() {
    override val title: String = "Finder: $deviceName"
    override val subtitle: String = "$proximityLabel ($rssiDbm dBm)"
    override val badgeText: String = "$rssiDbm"
  }

  data class EqProfileApplied(
    val deviceName: String,
    val presetName: String,
    override val tone: IslandTone = IslandTone.PRIMARY,
  ) : FloatingIslandEvent() {
    override val title: String = "EQ Profile Active"
    override val subtitle: String = "$deviceName • $presetName"
    override val badgeText: String = "EQ"
  }
}
