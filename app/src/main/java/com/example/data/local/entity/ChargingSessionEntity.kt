package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val startTime: Long,
  val endTime: Long? = null,
  val startLevel: Int,
  val endLevel: Int? = null,
  val powerSource: String = "AC",
  val maxTemperature: Float = 0f,
  val minTemperature: Float = 0f,
  val isCompleted: Boolean = false
) {
  val durationMinutes: Long
    get() {
      val end = endTime ?: System.currentTimeMillis()
      return ((end - startTime) / 60000).coerceAtLeast(0)
    }

  val gainedPercentage: Int
    get() {
      val end = endLevel ?: startLevel
      return (end - startLevel).coerceAtLeast(0)
    }
}
