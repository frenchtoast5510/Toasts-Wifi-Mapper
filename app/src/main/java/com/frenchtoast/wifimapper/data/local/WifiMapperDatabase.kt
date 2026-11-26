package com.frenchtoast.wifimapper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    WalkSessionEntity::class,
    WalkTrackPointEntity::class,
    WifiReadingEntity::class,
    FloorPlanSurveyEntity::class,
    PlacementRunEntity::class,
    RouterPinEntity::class,
    IndoorSampleEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class WifiMapperDatabase : RoomDatabase() {
  abstract fun wifiMapperDao(): WifiMapperDao

  companion object {
    @Volatile
    private var instance: WifiMapperDatabase? = null

    fun getInstance(context: Context): WifiMapperDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          WifiMapperDatabase::class.java,
          "toasts_wifi_mapper.db",
        ).build().also { instance = it }
      }
    }
  }
}
