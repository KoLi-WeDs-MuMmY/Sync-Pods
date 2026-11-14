package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [DeviceProfileEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class BluePulseDatabase : RoomDatabase() {
  abstract fun deviceProfileDao(): DeviceProfileDao

  companion object {
    @Volatile
    private var INSTANCE: BluePulseDatabase? = null

    fun getInstance(context: Context): BluePulseDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          BluePulseDatabase::class.java,
          "bluepulse_profiles.db",
        ).build().also { INSTANCE = it }
      }
    }
  }
}
