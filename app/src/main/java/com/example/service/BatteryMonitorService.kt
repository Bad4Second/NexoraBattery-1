package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.BatteryGuardianApp
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserSettings
import com.example.data.local.entity.AlarmHistoryEntity
import com.example.data.local.entity.BatteryLogEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.model.BatteryInfo
import com.example.receiver.AlarmDismissReceiver
import com.example.receiver.BatteryAlarmReceiver
import com.example.ui.alarm.AlarmAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BatteryMonitorService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var currentSettings = UserSettings()
  private var currentSessionId: Long? = null
  private val triggeredTargetsInSession = mutableSetOf<Int>()
  private var wasCharging = false
  private var lastLogTime = 0L
  private var hasWarnedOverheat = false

  private val batteryReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
        handleBatteryChanged(intent)
      } else if (intent.action == Intent.ACTION_POWER_CONNECTED) {
        onPowerConnected()
      } else if (intent.action == Intent.ACTION_POWER_DISCONNECTED) {
        onPowerDisconnected()
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    _isServiceRunning.value = true

    serviceScope.launch {
      val app = BatteryGuardianApp.instance
      app.preferences.settingsFlow.collectLatest { settings ->
        currentSettings = settings
      }
    }

    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_BATTERY_CHANGED)
      addAction(Intent.ACTION_POWER_CONNECTED)
      addAction(Intent.ACTION_POWER_DISCONNECTED)
    }
    registerReceiver(batteryReceiver, filter)

    // Initial check from sticky battery intent
    val stickyIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    stickyIntent?.let { handleBatteryChanged(it) }

    startForeground(
      BatteryGuardianApp.NOTIFICATION_MONITOR_ID,
      buildForegroundNotification(_batteryState.value)
    )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        BatteryGuardianApp.instance.alarmController.stopAlarm()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(BatteryGuardianApp.NOTIFICATION_ALARM_ID)
        com.example.ui.alarm.AlarmAlertActivity.dismissIfActive()
        sendBroadcast(Intent(com.example.receiver.AlarmDismissReceiver.ACTION_DISMISS_POPUP).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_TRIGGER_ALARM -> {
        val target = intent.getIntExtra("TARGET_PERCENT", 80)
        val current = intent.getIntExtra("CURRENT_PERCENT", target)
        serviceScope.launch {
          val app = BatteryGuardianApp.instance
          val settings = app.preferences.settingsFlow.first()
          triggerTargetAlarm(target, current, settings)
        }
      }
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    super.onDestroy()
    _isServiceRunning.value = false
    try {
      unregisterReceiver(batteryReceiver)
    } catch (_: Exception) {}
    BatteryAlarmScheduler.cancelWatchdog(this)
    serviceScope.cancel()
  }

  private fun handleBatteryChanged(intent: Intent) {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val percentage = if (scale > 0 && level >= 0) (level * 100) / scale else level
    val statusInt = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
    val pluggedInt = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
    val tempInt = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
    val tempCelsius = tempInt / 10.0f
    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
    val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

    val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
      statusInt == BatteryManager.BATTERY_STATUS_FULL ||
      pluggedInt > 0

    val statusText = when (statusInt) {
      BatteryManager.BATTERY_STATUS_CHARGING -> "Mengisi Daya"
      BatteryManager.BATTERY_STATUS_DISCHARGING -> "Tidak Mengisi"
      BatteryManager.BATTERY_STATUS_FULL -> "Baterai Penuh"
      BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Terhubung tapi Tidak Mengisi"
      else -> "Status Normal"
    }

    val healthText = when (healthInt) {
      BatteryManager.BATTERY_HEALTH_GOOD -> "Baik (Good)"
      BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Terlalu Panas (Overheat)"
      BatteryManager.BATTERY_HEALTH_DEAD -> "Rusak (Dead)"
      BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tegangan Lebih (Over Voltage)"
      BatteryManager.BATTERY_HEALTH_COLD -> "Terlalu Dingin (Cold)"
      else -> "Normal"
    }

    val pluggedText = when (pluggedInt) {
      BatteryManager.BATTERY_PLUGGED_AC -> "Charger Dinding (AC)"
      BatteryManager.BATTERY_PLUGGED_USB -> "Kabel USB"
      BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging"
      else -> "Tidak Terhubung"
    }

    val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val chargeTimeRemainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      try {
        batteryManager?.computeChargeTimeRemaining() ?: -1L
      } catch (_: Exception) {
        -1L
      }
    } else {
      -1L
    }

    val info = BatteryInfo(
      level = level,
      scale = scale,
      percentage = percentage,
      isCharging = isCharging,
      status = statusText,
      health = healthText,
      temperatureCelsius = tempCelsius,
      voltageMillivolts = voltage,
      pluggedSource = pluggedText,
      technology = technology,
      chargeTimeRemainingMs = chargeTimeRemainingMs,
      lastUpdated = System.currentTimeMillis()
    )

    _batteryState.value = info

    // Update Notification
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(
      BatteryGuardianApp.NOTIFICATION_MONITOR_ID,
      buildForegroundNotification(info)
    )

    // Check Charging Session state transitions
    if (isCharging && !wasCharging) {
      onPowerConnected()
    } else if (!isCharging && wasCharging) {
      onPowerDisconnected()
    }
    wasCharging = isCharging

    // Check Smart Charging Alarm Triggers
    if (isCharging && currentSettings.smartAlarmEnabled) {
      val isFull = statusInt == BatteryManager.BATTERY_STATUS_FULL || percentage >= 100
      checkAlarmTargets(percentage, isFull)
    }

    // Check Temperature Overheat Warning
    if (currentSettings.tempWarningEnabled && tempCelsius >= currentSettings.tempWarningThreshold) {
      if (!hasWarnedOverheat) {
        hasWarnedOverheat = true
        sendOverheatNotification(tempCelsius)
      }
    } else if (tempCelsius < (currentSettings.tempWarningThreshold - 2f)) {
      hasWarnedOverheat = false
    }

    // Periodic Database Logging (every 5 minutes or significant events)
    val now = System.currentTimeMillis()
    if (now - lastLogTime > 5 * 60 * 1000L) {
      lastLogTime = now
      serviceScope.launch {
        BatteryGuardianApp.instance.database.batteryLogDao().insertLog(
          BatteryLogEntity(
            level = percentage,
            temperature = tempCelsius,
            voltage = voltage,
            isCharging = isCharging
          )
        )
      }
    }
  }

  private fun checkAlarmTargets(currentPercentage: Int, isFull: Boolean = false) {
    serviceScope.launch {
      val app = BatteryGuardianApp.instance
      val settings = app.preferences.settingsFlow.first()
      if (!settings.smartAlarmEnabled) return@launch

      // Full-charge alert priority trigger
      if (isFull && !triggeredTargetsInSession.contains(100)) {
        triggeredTargetsInSession.add(100)
        triggerTargetAlarm(100, currentPercentage.coerceAtLeast(100), settings)
        return@launch
      }

      for (target in settings.targetLevels.sorted()) {
        if (currentPercentage >= target && !triggeredTargetsInSession.contains(target)) {
          triggeredTargetsInSession.add(target)
          triggerTargetAlarm(target, currentPercentage, settings)
          break
        }
      }
    }
  }

  private fun triggerTargetAlarm(target: Int, current: Int, settings: UserSettings) {
    serviceScope.launch {
      val app = BatteryGuardianApp.instance

      // Record in DB
      app.database.alarmHistoryDao().insertAlarm(
        AlarmHistoryEntity(
          targetPercentage = target,
          batteryLevel = current,
          alarmMode = settings.alarmMode.name,
          actionTaken = "Triggered"
        )
      )

      // Ring and speak
      app.alarmController.triggerAlarm(target, current, settings)

      // Post high priority heads-up / full-screen notification
      sendAlarmNotification(target, current)

      // Launch AlarmAlertActivity for full screen wake
      val alertIntent = Intent(applicationContext, AlarmAlertActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("TARGET_PERCENT", target)
        putExtra("CURRENT_PERCENT", current)
      }
      startActivity(alertIntent)
    }
  }

  private fun onPowerConnected() {
    triggeredTargetsInSession.clear()
    BatteryAlarmReceiver.resetSessionTargets()
    if (currentSettings.smartAlarmEnabled) {
      BatteryAlarmScheduler.scheduleNextBatteryCheck(this, 1000L)
    }
    val now = System.currentTimeMillis()
    val currentInfo = _batteryState.value
    serviceScope.launch {
      val app = BatteryGuardianApp.instance
      val sessionId = app.database.chargingSessionDao().insertSession(
        ChargingSessionEntity(
          startTime = now,
          startLevel = currentInfo.percentage,
          powerSource = currentInfo.pluggedSource,
          maxTemperature = currentInfo.temperatureCelsius,
          minTemperature = currentInfo.temperatureCelsius,
          isCompleted = false
        )
      )
      currentSessionId = sessionId
    }
  }

  private fun onPowerDisconnected() {
    triggeredTargetsInSession.clear()
    BatteryAlarmReceiver.resetSessionTargets()
    BatteryAlarmScheduler.cancelWatchdog(this)
    val now = System.currentTimeMillis()
    val currentInfo = _batteryState.value
    serviceScope.launch {
      val app = BatteryGuardianApp.instance
      val activeSession = app.database.chargingSessionDao().getActiveSession()
      if (activeSession != null) {
        val updated = activeSession.copy(
          endTime = now,
          endLevel = currentInfo.percentage,
          maxTemperature = maxOf(activeSession.maxTemperature, currentInfo.temperatureCelsius),
          isCompleted = true
        )
        app.database.chargingSessionDao().updateSession(updated)
      }
      currentSessionId = null
    }
  }

  private fun buildForegroundNotification(info: BatteryInfo): Notification {
    val openIntent = Intent(this, MainActivity::class.java)
    val openPending = PendingIntent.getActivity(
      this, 0, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(this, BatteryMonitorService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPending = PendingIntent.getService(
      this, 1, stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val title = "${info.percentage}% • ${if (info.isCharging) info.status else "Battery Guardian Aktif"}"
    val content = "${info.temperatureCelsius}°C • ${info.pluggedSource} • ${if (info.isCharging) info.formattedChargeTimeRemaining else info.formattedDischargeTimeRemaining}"

    return NotificationCompat.Builder(this, BatteryGuardianApp.CHANNEL_MONITOR_ID)
      .setContentTitle(title)
      .setContentText(content)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openPending)
      .addAction(0, "Hentikan", stopPending)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  private fun sendAlarmNotification(target: Int, current: Int) {
    val fullScreenIntent = Intent(this, AlarmAlertActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("TARGET_PERCENT", target)
      putExtra("CURRENT_PERCENT", current)
    }
    val fullScreenPending = PendingIntent.getActivity(
      this, 2, fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
      action = AlarmDismissReceiver.ACTION_STOP_ALARM
    }
    val stopPending = PendingIntent.getBroadcast(
      this, 3, stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, BatteryGuardianApp.CHANNEL_ALARM_ID)
      .setContentTitle("Baterai Mencapai Target $target%!")
      .setContentText("Persentase baterai saat ini $current%. Segera cabut charger.")
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setFullScreenIntent(fullScreenPending, true)
      .setContentIntent(fullScreenPending)
      .setAutoCancel(true)
      .addAction(0, "Hentikan Alarm", stopPending)
      .build()

    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(BatteryGuardianApp.NOTIFICATION_ALARM_ID, notification)
  }

  private fun sendOverheatNotification(temp: Float) {
    val openIntent = Intent(this, MainActivity::class.java)
    val openPending = PendingIntent.getActivity(
      this, 0, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, BatteryGuardianApp.CHANNEL_ALERT_ID)
      .setContentTitle("Peringatan Suhu Baterai Panas! (${temp}°C)")
      .setContentText("Suhu baterai telah melampaui batas aman. Lepaskan casing atau hentikan pengisian daya.")
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(openPending)
      .setAutoCancel(true)
      .build()

    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(BatteryGuardianApp.NOTIFICATION_ALERT_ID, notification)
  }

  companion object {
    const val ACTION_START = "com.example.action.START_MONITOR"
    const val ACTION_STOP = "com.example.action.STOP_MONITOR"
    const val ACTION_TRIGGER_ALARM = "com.example.action.TRIGGER_ALARM"

    private val _batteryState = MutableStateFlow(BatteryInfo())
    val batteryState: StateFlow<BatteryInfo> = _batteryState.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun startService(context: Context) {
      val intent = Intent(context, BatteryMonitorService::class.java).apply {
        action = ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun startWithAlarmTrigger(context: Context, target: Int, current: Int) {
      val intent = Intent(context, BatteryMonitorService::class.java).apply {
        action = ACTION_TRIGGER_ALARM
        putExtra("TARGET_PERCENT", target)
        putExtra("CURRENT_PERCENT", current)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, BatteryMonitorService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }

    fun updateBatteryStateManually(info: BatteryInfo) {
      _batteryState.value = info
    }
  }
}
