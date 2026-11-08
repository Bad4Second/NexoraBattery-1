package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_history")
data class AlarmHistoryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val targetPercentage: Int,
  val batteryLevel: Int,
  val alarmMode: String,
  val actionTaken: String // "Dismissed", "Stopped", "Snoozed"
)
