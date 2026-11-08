package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.local.BatteryGuardianDatabase
import com.example.data.local.BatteryPreferences
import com.example.service.AlarmController

class BatteryGuardianApp : Application() {

  lateinit var database: BatteryGuardianDatabase
    private set

  lateinit var preferences: BatteryPreferences
    private set

  lateinit var alarmController: AlarmController
    private set

  override fun onCreate() {
    super.onCreate()
    instance = this
    database = BatteryGuardianDatabase.getDatabase(this)
    preferences = BatteryPreferences(this)
    alarmController = AlarmController(this)

    createNotificationChannels()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      // 1. Ongoing Foreground Monitoring Channel
      val monitorChannel = NotificationChannel(
        CHANNEL_MONITOR_ID,
        "Pemantauan Baterai",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Status pemantauan persentase dan kesehatan baterai"
        setShowBadge(false)
      }

      // 2. High-Priority Alarm Channel
      val alarmChannel = NotificationChannel(
        CHANNEL_ALARM_ID,
        "Peringatan Smart Charging Alarm",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alarm ketika baterai mencapai target persentase pengisian"
        // Audio and TTS engine are driven synchronously by the Foreground Service AlarmController
        setSound(null, null)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 800, 400, 800)
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          setBypassDnd(true)
        }
      }

      // 3. Temperature & Warning Channel
      val alertChannel = NotificationChannel(
        CHANNEL_ALERT_ID,
        "Peringatan Suhu & Keamanan",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Pemberitahuan suhu baterai tinggi dan pengisian terlalu lama"
        enableVibration(true)
      }

      notificationManager.createNotificationChannels(
        listOf(monitorChannel, alarmChannel, alertChannel)
      )
    }
  }

  companion object {
    const val CHANNEL_MONITOR_ID = "channel_battery_monitor"
    const val CHANNEL_ALARM_ID = "channel_battery_alarm"
    const val CHANNEL_ALERT_ID = "channel_battery_alert"

    const val NOTIFICATION_MONITOR_ID = 1001
    const val NOTIFICATION_ALARM_ID = 1002
    const val NOTIFICATION_ALERT_ID = 1003

    lateinit var instance: BatteryGuardianApp
      private set
  }
}
