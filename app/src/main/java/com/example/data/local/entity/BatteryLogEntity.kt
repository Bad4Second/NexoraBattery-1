package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_logs")
data class BatteryLogEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val level: Int,
  val temperature: Float,
  val voltage: Int,
  val isCharging: Boolean
)
