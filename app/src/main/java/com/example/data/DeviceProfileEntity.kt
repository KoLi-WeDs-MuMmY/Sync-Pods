package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
  @PrimaryKey val address: String,
  val customName: String,
  val latencyOffsetMs: Int = 0,
  val eqPresetName: String = "Studio Flat",
  val eqBandLevelsCsv: String = "0,0,0,0,0",
  val bassBoostStrength: Int = 250,
  val virtualizerStrength: Int = 200,
  val channelBalance: Float = 0f,
  val autoConnectApplyEq: Boolean = true,
  val autoLaunchIsland: Boolean = true,
  val autoSetVolume: Boolean = false,
  val autoVolumePercent: Int = 65,
  val lastRssiDbm: Int? = null,
  val lastBatteryPercent: Int? = null,
  val lastConnectedTimestamp: Long = System.currentTimeMillis(),
)
