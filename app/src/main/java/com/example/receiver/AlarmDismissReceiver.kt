package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.BatteryGuardianApp
import com.example.service.BatteryAlarmScheduler
import com.example.ui.alarm.AlarmAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmDismissReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    val app = BatteryGuardianApp.instance

    when (action) {
      ACTION_STOP_ALARM -> {
        // Stop audio and TTS immediately
        app.alarmController.stopAlarm()

        // Cancel notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(BatteryGuardianApp.NOTIFICATION_ALARM_ID)

        // Automatically dismiss the popup / full-screen activity via direct handle
        AlarmAlertActivity.dismissIfActive()

        // Send explicit broadcast as a guaranteed fallback
        val dismissIntent = Intent(ACTION_DISMISS_POPUP).apply {
          setPackage(context.packageName)
        }
        context.sendBroadcast(dismissIntent)
      }
      ACTION_SNOOZE_ALARM -> {
        app.alarmController.stopAlarm()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(BatteryGuardianApp.NOTIFICATION_ALARM_ID)

        AlarmAlertActivity.dismissIfActive()

        val dismissIntent = Intent(ACTION_DISMISS_POPUP).apply {
          setPackage(context.packageName)
        }
        context.sendBroadcast(dismissIntent)

        val target = intent.getIntExtra("TARGET_PERCENT", 80)
        CoroutineScope(Dispatchers.IO).launch {
          val settings = app.preferences.settingsFlow.first()
          val snoozeMinutes = settings.snoozeIntervalMinutes
          BatteryAlarmScheduler.scheduleSnooze(context, target, snoozeMinutes)
        }
        Toast.makeText(context, "Alarm ditunda", Toast.LENGTH_SHORT).show()
      }
    }
  }

  companion object {
    const val ACTION_STOP_ALARM = "com.example.action.STOP_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.example.action.SNOOZE_ALARM"
    const val ACTION_DISMISS_POPUP = "com.example.action.DISMISS_ALARM_POPUP"
  }
}
