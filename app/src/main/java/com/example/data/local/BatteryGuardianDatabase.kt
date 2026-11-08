package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AlarmHistoryDao
import com.example.data.local.dao.BatteryLogDao
import com.example.data.local.dao.ChargingSessionDao
import com.example.data.local.entity.AlarmHistoryEntity
import com.example.data.local.entity.BatteryLogEntity
import com.example.data.local.entity.ChargingSessionEntity

@Database(
  entities = [
    ChargingSessionEntity::class,
    BatteryLogEntity::class,
    AlarmHistoryEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class BatteryGuardianDatabase : RoomDatabase() {
  abstract fun chargingSessionDao(): ChargingSessionDao
  abstract fun batteryLogDao(): BatteryLogDao
  abstract fun alarmHistoryDao(): AlarmHistoryDao

  companion object {
    @Volatile
    private var INSTANCE: BatteryGuardianDatabase? = null

    fun getDatabase(context: Context): BatteryGuardianDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          BatteryGuardianDatabase::class.java,
          "battery_guardian_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
