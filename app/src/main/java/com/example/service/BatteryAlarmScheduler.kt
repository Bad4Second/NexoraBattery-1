package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.BatteryAlarmReceiver

object BatteryAlarmScheduler {

  const val ACTION_CHECK_BATTERY_THRESHOLD = "com.example.action.CHECK_BATTERY_THRESHOLD"
  const val ACTION_TRIGGER_THRESHOLD_ALARM = "com.example.action.TRIGGER_THRESHOLD_ALARM"
  const val ACTION_TRIGGER_SNOOZE = "com.example.action.TRIGGER_SNOOZE"

  private const val REQUEST_CODE_CHECK = 2001
  private const val REQUEST_CODE_SNOOZE = 2002
  private const val REQUEST_CODE_SCHEDULE = 2003

  // Polling interval for AlarmManager watchdog while charging (e.g. every 3 minutes)
  const val WATCHDOG_INTERVAL_MS = 3 * 60 * 1000L

  /**
   * Schedules an exact or low-latency watchdog alarm using AlarmManager.
   * This guarantees that even if the OS enters deep Doze mode or aggressive OEM app standby,
   * the device wakes up to check if battery has reached user target thresholds.
   */
  fun scheduleNextBatteryCheck(context: Context, delayMs: Long = WATCHDOG_INTERVAL_MS) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerAtMillis = System.currentTimeMillis() + delayMs

    val intent = Intent(context, BatteryAlarmReceiver::class.java).apply {
      action = ACTION_CHECK_BATTERY_THRESHOLD
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_CHECK,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    setAlarm(alarmManager, triggerAtMillis, pendingIntent)
  }

  /**
   * Schedules a Snooze alarm for a specified target percentage.
   */
  fun scheduleSnooze(context: Context, targetPercent: Int, snoozeMinutes: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

    val intent = Intent(context, BatteryAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_SNOOZE
      putExtra("TARGET_PERCENT", targetPercent)
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_SNOOZE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    setAlarm(alarmManager, triggerAtMillis, pendingIntent)
  }

  /**
   * Direct trigger for a specific threshold reached
   */
  fun triggerDirectThresholdAlarm(context: Context, targetPercent: Int, currentPercent: Int) {
    val intent = Intent(context, BatteryAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_THRESHOLD_ALARM
      putExtra("TARGET_PERCENT", targetPercent)
      putExtra("CURRENT_PERCENT", currentPercent)
    }
    context.sendBroadcast(intent)
  }

  fun cancelWatchdog(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, BatteryAlarmReceiver::class.java).apply {
      action = ACTION_CHECK_BATTERY_THRESHOLD
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_CHECK,
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  fun cancelSnooze(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, BatteryAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_SNOOZE
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_SNOOZE,
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
              AlarmManager.RTC_WAKEUP,
              triggerAtMillis,
              pendingIntent
            )
          } else {
            alarmManager.setAndAllowWhileIdle(
              AlarmManager.RTC_WAKEUP,
              triggerAtMillis,
              pendingIntent
            )
          }
        } else {
          alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
          )
        }
      } else {
        alarmManager.setExact(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )
      }
    } catch (_: SecurityException) {
      // Fallback if exact alarm permissions not permitted
      alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent
      )
    }
  }
}
