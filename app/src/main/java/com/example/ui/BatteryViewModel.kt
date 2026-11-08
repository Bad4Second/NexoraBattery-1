package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BatteryGuardianApp
import com.example.data.local.AlarmMode
import com.example.data.local.UserSettings
import com.example.data.local.entity.AlarmHistoryEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.data.model.BatteryInfo
import com.example.service.BatteryMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as BatteryGuardianApp

  val batteryState: StateFlow<BatteryInfo> = BatteryMonitorService.batteryState
  val isServiceRunning: StateFlow<Boolean> = BatteryMonitorService.isServiceRunning

  val userSettings: StateFlow<UserSettings> = app.preferences.settingsFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = UserSettings()
    )

  val chargingSessions: StateFlow<List<ChargingSessionEntity>> = app.database.chargingSessionDao()
    .getAllSessions()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val alarmHistories: StateFlow<List<AlarmHistoryEntity>> = app.database.alarmHistoryDao()
    .getAllAlarms()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  fun startMonitoring() {
    BatteryMonitorService.startService(app)
  }

  fun stopMonitoring() {
    BatteryMonitorService.stopService(app)
  }

  fun toggleSmartAlarm(enabled: Boolean) {
    viewModelScope.launch {
      app.preferences.updateSmartAlarmEnabled(enabled)
      if (enabled) {
        if (!isServiceRunning.value) {
          startMonitoring()
        }
        if (batteryState.value.isCharging) {
          com.example.service.BatteryAlarmScheduler.scheduleNextBatteryCheck(app, 1000L)
        }
      } else {
        com.example.service.BatteryAlarmScheduler.cancelWatchdog(app)
      }
    }
  }

  fun toggleTargetLevel(level: Int) {
    viewModelScope.launch {
      app.preferences.toggleTargetLevel(level)
    }
  }

  fun setTargetLevels(targets: Set<Int>) {
    viewModelScope.launch {
      app.preferences.updateTargetLevels(targets)
    }
  }

  fun updateTtsTemplate(template: String) {
    viewModelScope.launch {
      app.preferences.updateTtsTemplate(template)
    }
  }

  fun updateTtsVoiceParams(pitch: Float, rate: Float) {
    viewModelScope.launch {
      app.preferences.updateTtsVoiceParams(pitch, rate)
    }
  }

  fun updateAlarmMode(mode: AlarmMode) {
    viewModelScope.launch {
      app.preferences.updateAlarmMode(mode)
    }
  }

  fun updateAlarmDuration(seconds: Int) {
    viewModelScope.launch {
      app.preferences.updateAlarmDuration(seconds)
    }
  }

  fun updateAlarmVolume(volume: Int) {
    viewModelScope.launch {
      app.preferences.updateAlarmVolume(volume)
    }
  }

  fun updateSnoozeInterval(minutes: Int) {
    viewModelScope.launch {
      app.preferences.updateSnoozeInterval(minutes)
    }
  }

  fun updateTempWarning(enabled: Boolean, threshold: Float) {
    viewModelScope.launch {
      app.preferences.updateTempWarning(enabled, threshold)
    }
  }

  fun updateSleepMode(
    enabled: Boolean,
    startH: Int = 22,
    startM: Int = 0,
    endH: Int = 6,
    endM: Int = 0,
    dim: Boolean = true
  ) {
    viewModelScope.launch {
      app.preferences.updateSleepMode(enabled, startH, startM, endH, endM, dim)
      if (enabled && !isServiceRunning.value) {
        startMonitoring()
      }
    }
  }

  fun updateThemeMode(mode: String) {
    viewModelScope.launch {
      app.preferences.updateThemeMode(mode)
    }
  }

  fun updateLongPluggedReminder(enabled: Boolean) {
    viewModelScope.launch {
      app.preferences.updateLongPluggedReminder(enabled)
    }
  }

  fun previewVoice() {
    val settings = userSettings.value
    val target = settings.targetLevels.firstOrNull() ?: 80
    app.alarmController.previewVoice(
      template = settings.ttsTemplate,
      target = target,
      pitch = settings.ttsPitch,
      rate = settings.ttsSpeechRate
    )
  }

  fun previewAlarmSound(customUriStr: String? = null) {
    val settings = userSettings.value
    val uri = customUriStr ?: settings.ringtoneUri
    app.alarmController.previewAlarmSound(settings.alarmVolume, uri)
  }

  fun selectRingtone(uri: String?, title: String, isCustom: Boolean) {
    viewModelScope.launch {
      app.preferences.updateRingtone(uri, title, isCustom)
    }
  }

  fun getSystemRingtones(): List<com.example.service.RingtoneItem> {
    return app.alarmController.querySystemRingtones()
  }

  fun saveCustomAudio(sourceUri: android.net.Uri, name: String?) {
    val result = app.alarmController.saveCustomAudioFile(sourceUri, name)
    if (result != null) {
      selectRingtone(result.first, result.second, true)
    }
  }

  fun triggerTestAlarm() {
    val settings = userSettings.value
    val current = batteryState.value.percentage
    val target = settings.targetLevels.firstOrNull() ?: 80
    app.alarmController.triggerAlarm(target, current, settings)
  }

  fun stopAlarm() {
    app.alarmController.stopAlarm()
  }

  fun clearHistory() {
    viewModelScope.launch {
      app.database.chargingSessionDao().clearAll()
      app.database.alarmHistoryDao().clearAll()
    }
  }
}
