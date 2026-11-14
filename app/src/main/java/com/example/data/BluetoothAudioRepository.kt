package com.example.data

import kotlinx.coroutines.flow.Flow

class BluetoothAudioRepository(
  private val profileDao: DeviceProfileDao,
) {
  val profiles: Flow<List<DeviceProfileEntity>> = profileDao.observeAllProfiles()

  fun observeProfile(address: String): Flow<DeviceProfileEntity?> =
    profileDao.observeProfile(address)

  suspend fun getOrCreateProfile(address: String, defaultName: String): DeviceProfileEntity {
    return profileDao.getProfileOnce(address) ?: DeviceProfileEntity(
      address = address,
      customName = defaultName,
    ).also { profileDao.upsertProfile(it) }
  }

  suspend fun saveProfile(profile: DeviceProfileEntity) {
    profileDao.upsertProfile(profile)
  }

  suspend fun updateLatencyOffset(address: String, defaultName: String, offsetMs: Int) {
    val existing = getOrCreateProfile(address, defaultName)
    profileDao.upsertProfile(
      existing.copy(
        latencyOffsetMs = offsetMs.coerceIn(-250, 250),
        lastConnectedTimestamp = System.currentTimeMillis(),
      )
    )
  }

  suspend fun updateEqSettings(
    address: String,
    defaultName: String,
    presetName: String,
    bandLevels: List<Int>,
    bassBoost: Int,
    virtualizer: Int,
    channelBalance: Float,
  ) {
    val existing = getOrCreateProfile(address, defaultName)
    profileDao.upsertProfile(
      existing.copy(
        eqPresetName = presetName,
        eqBandLevelsCsv = bandLevels.joinToString(","),
        bassBoostStrength = bassBoost.coerceIn(0, 1000),
        virtualizerStrength = virtualizer.coerceIn(0, 1000),
        channelBalance = channelBalance.coerceIn(-1f, 1f),
      )
    )
  }

  suspend fun updateAutoConnectRules(
    address: String,
    defaultName: String,
    applyEq: Boolean,
    launchIsland: Boolean,
    setVolume: Boolean,
    volumePercent: Int,
  ) {
    val existing = getOrCreateProfile(address, defaultName)
    profileDao.upsertProfile(
      existing.copy(
        autoConnectApplyEq = applyEq,
        autoLaunchIsland = launchIsland,
        autoSetVolume = setVolume,
        autoVolumePercent = volumePercent.coerceIn(5, 100),
      )
    )
  }

  suspend fun deleteProfile(address: String) {
    profileDao.deleteProfile(address)
  }
}
