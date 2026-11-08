package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AlarmHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmHistoryDao {
  @Query("SELECT * FROM alarm_history ORDER BY timestamp DESC")
  fun getAllAlarms(): Flow<List<AlarmHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAlarm(alarm: AlarmHistoryEntity): Long

  @Query("DELETE FROM alarm_history")
  suspend fun clearAll()
}
