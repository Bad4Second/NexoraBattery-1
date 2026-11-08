package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.BatteryGuardianApp
import com.example.service.BatteryAlarmScheduler
import com.example.service.BatteryMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val isBoot = action == Intent.ACTION_BOOT_COMPLETED ||
      action == Intent.ACTION_MY_PACKAGE_REPLACED ||
      action == "android.intent.action.QUICKBOOT_POWERON" ||
      action == "com.htc.intent.action.QUICKBOOT_POWERON"

    if (!isBoot) return

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val wakeLock = powerManager.newWakeLock(
      PowerManager.PARTIAL_WAKE_LOCK,
      "BatteryGuardian::BootReceiverWakeLock"
    ).apply {
      setReferenceCounted(false)
      acquire(30 * 1000L)
    }

    val pendingResult = goAsync()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        BatteryAlarmReceiver.resetSessionTargets()

        val app = BatteryGuardianApp.instance
        val settings = app.preferences.settingsFlow.first()

        if (settings.smartAlarmEnabled) {
          // 1. Restart Foreground Service
          BatteryMonitorService.startService(context)

          // 2. Check if currently charging
          val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
          if (sticky != null) {
            val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
              status == BatteryManager.BATTERY_STATUS_FULL ||
              plugged > 0

            if (isCharging) {
              // Immediately schedule AlarmManager watchdog
              BatteryAlarmScheduler.scheduleNextBatteryCheck(context, 1000L)
            }
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
}
