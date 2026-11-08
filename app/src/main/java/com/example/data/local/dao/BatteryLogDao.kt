package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BatteryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryLogDao {
  @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentLogs(limit: Int = 100): Flow<List<BatteryLogEntity>>

  @Query("SELECT * FROM battery_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
  fun getLogsSince(sinceTimestamp: Long): Flow<List<BatteryLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: BatteryLogEntity): Long

  @Query("DELETE FROM battery_logs WHERE timestamp < :olderThanTimestamp")
  suspend fun deleteOldLogs(olderThanTimestamp: Long)
}
