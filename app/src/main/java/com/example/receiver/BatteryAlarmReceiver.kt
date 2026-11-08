package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.BatteryGuardianApp
import com.example.R
import com.example.data.local.entity.AlarmHistoryEntity
import com.example.service.BatteryAlarmScheduler
import com.example.service.BatteryMonitorService
import com.example.ui.alarm.AlarmAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BatteryAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val wakeLock = powerManager.newWakeLock(
      PowerManager.PARTIAL_WAKE_LOCK,
      "BatteryGuardian::AlarmReceiverWakeLock"
    ).apply {
      setReferenceCounted(false)
      acquire(60 * 1000L) // 60 seconds max
    }

    val pendingResult = goAsync()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val app = BatteryGuardianApp.instance
        val settings = app.preferences.settingsFlow.first()

        when (action) {
          BatteryAlarmScheduler.ACTION_CHECK_BATTERY_THRESHOLD -> {
            checkBatteryAndTriggerIfNeeded(context, settings)
          }
          BatteryAlarmScheduler.ACTION_TRIGGER_THRESHOLD_ALARM -> {
            val target = intent.getIntExtra("TARGET_PERCENT", 80)
            val current = intent.getIntExtra("CURRENT_PERCENT", target)
            fireAlarm(context, target, current, settings)
          }
          BatteryAlarmScheduler.ACTION_TRIGGER_SNOOZE -> {
            val target = intent.getIntExtra("TARGET_PERCENT", 80)
            val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val current = getCurrentPercentage(sticky) ?: target
            fireAlarm(context, target, current, settings)
          }
        }
      } catch (_: Exception) {
      } finally {
        try {
          if (wakeLock.isHeld) {
            wakeLock.release()
          }
        } catch (_: Exception) {}
        pendingResult.finish()
      }
    }
  }

  private suspend fun checkBatteryAndTriggerIfNeeded(
    context: Context,
    settings: com.example.data.local.UserSettings
  ) {
    if (!settings.smartAlarmEnabled) {
      BatteryAlarmScheduler.cancelWatchdog(context)
      return
    }

    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
    val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val percentage = if (scale > 0 && level >= 0) (level * 100) / scale else level
    val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val plugged = sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
      status == BatteryManager.BATTERY_STATUS_FULL ||
      plugged > 0

    if (!isCharging) {
      // Stopped charging, stop recurring watchdog alarms
      BatteryAlarmScheduler.cancelWatchdog(context)
      return
    }

    // Check targets (including full charge)
    var hasTriggeredAny = false
    val isFull = status == BatteryManager.BATTERY_STATUS_FULL || percentage >= 100

    if (isFull && !triggeredTargetsSet.contains(100)) {
      triggeredTargetsSet.add(100)
      fireAlarm(context, 100, percentage.coerceAtLeast(100), settings)
      hasTriggeredAny = true
    } else {
      for (target in settings.targetLevels.sorted()) {
        if (percentage >= target && !triggeredTargetsSet.contains(target)) {
          triggeredTargetsSet.add(target)
          fireAlarm(context, target, percentage, settings)
          hasTriggeredAny = true
          break
        }
      }
    }

    // If still charging and there are higher targets, re-schedule watchdog
    val hasHigherTargets = settings.targetLevels.any { it > percentage }
    if (hasHigherTargets || !hasTriggeredAny) {
      BatteryAlarmScheduler.scheduleNextBatteryCheck(context)
    }
  }

  private fun fireAlarm(
    context: Context,
    target: Int,
    current: Int,
    settings: com.example.data.local.UserSettings
  ) {
    // Initiate Foreground Service to hold foreground notification, wakelock, and synchronously play audio + TTS
    BatteryMonitorService.startWithAlarmTrigger(context, target, current)
  }

  private fun sendAlarmNotification(context: Context, target: Int, current: Int) {
    val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("TARGET_PERCENT", target)
      putExtra("CURRENT_PERCENT", current)
    }
    val fullScreenPending = PendingIntent.getActivity(
      context,
      2,
      fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
      action = AlarmDismissReceiver.ACTION_STOP_ALARM
    }
    val stopPending = PendingIntent.getBroadcast(
      context,
      3,
      stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val snoozeIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
      action = AlarmDismissReceiver.ACTION_SNOOZE_ALARM
      putExtra("TARGET_PERCENT", target)
    }
    val snoozePending = PendingIntent.getBroadcast(
      context,
      4,
      snoozeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, BatteryGuardianApp.CHANNEL_ALARM_ID)
      .setContentTitle("Baterai Mencapai Target $target%!")
      .setContentText("Persentase saat ini $current%. Segera cabut charger untuk melindungi baterai.")
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setFullScreenIntent(fullScreenPending, true)
      .setContentIntent(fullScreenPending)
      .setAutoCancel(true)
      .addAction(0, "Hentikan", stopPending)
      .addAction(0, "Tunda 5m", snoozePending)
      .build()

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(BatteryGuardianApp.NOTIFICATION_ALARM_ID, notification)
  }

  private fun getCurrentPercentage(intent: Intent?): Int? {
    if (intent == null) return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (scale > 0 && level >= 0) (level * 100) / scale else null
  }

  companion object {
    val triggeredTargetsSet = mutableSetOf<Int>()

    fun resetSessionTargets() {
      triggeredTargetsSet.clear()
    }
  }
}
