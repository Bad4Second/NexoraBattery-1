package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChargingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {
  @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
  fun getAllSessions(): Flow<List<ChargingSessionEntity>>

  @Query("SELECT * FROM charging_sessions WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
  suspend fun getActiveSession(): ChargingSessionEntity?

  @Query("SELECT * FROM charging_sessions WHERE isCompleted = 1 ORDER BY startTime DESC LIMIT 1")
  suspend fun getLatestCompletedSession(): ChargingSessionEntity?

  @Query("SELECT * FROM charging_sessions WHERE startTime >= :sinceTimestamp ORDER BY startTime DESC")
  fun getSessionsSince(sinceTimestamp: Long): Flow<List<ChargingSessionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: ChargingSessionEntity): Long

  @Update
  suspend fun updateSession(session: ChargingSessionEntity)

  @Query("DELETE FROM charging_sessions")
  suspend fun clearAll()
}
