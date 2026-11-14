package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceProfileDao {
  @Query("SELECT * FROM device_profiles ORDER BY lastConnectedTimestamp DESC")
  fun observeAllProfiles(): Flow<List<DeviceProfileEntity>>

  @Query("SELECT * FROM device_profiles WHERE address = :address LIMIT 1")
  fun observeProfile(address: String): Flow<DeviceProfileEntity?>

  @Query("SELECT * FROM device_profiles WHERE address = :address LIMIT 1")
  suspend fun getProfileOnce(address: String): DeviceProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProfile(profile: DeviceProfileEntity)

  @Query("DELETE FROM device_profiles WHERE address = :address")
  suspend fun deleteProfile(address: String)
}
