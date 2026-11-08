package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "battery_guardian_prefs")

enum class AlarmMode {
  ALARM_AND_VOICE,
  VOICE_ONLY,
  ALARM_ONLY,
  SILENT
}

data class UserSettings(
  val targetLevels: Set<Int> = setOf(80, 100),
  val smartAlarmEnabled: Boolean = true,
  val ttsEnabled: Boolean = true,
  val ttsTemplate: String = "Baterai sudah mencapai {target} persen, waktunya mencabut charger.",
  val ttsPitch: Float = 1.0f,
  val ttsSpeechRate: Float = 1.0f,
  val alarmMode: AlarmMode = AlarmMode.ALARM_AND_VOICE,
  val alarmDurationSeconds: Int = 30, // 10, 30, 60, 300, -1
  val alarmVolume: Int = 85,
  val snoozeIntervalMinutes: Int = 5,
  val ringtoneUri: String? = null,
  val ringtoneTitle: String = "Nada Alarm Bawaan",
  val isCustomRingtone: Boolean = false,
  val tempWarningEnabled: Boolean = true,
  val tempWarningThreshold: Float = 42.0f,
  val longPluggedReminderEnabled: Boolean = true,
  val sleepModeEnabled: Boolean = false,
  val sleepStartHour: Int = 22,
  val sleepStartMinute: Int = 0,
  val sleepEndHour: Int = 6,
  val sleepEndMinute: Int = 0,
  val sleepDimDisplay: Boolean = true,
  val themeMode: String = "LIGHT" // "LIGHT", "DARK", "SYSTEM"
)

class BatteryPreferences(private val context: Context) {

  private object Keys {
    val TARGET_LEVELS = stringSetPreferencesKey("target_levels")
    val SMART_ALARM_ENABLED = booleanPreferencesKey("smart_alarm_enabled")
    val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    val TTS_TEMPLATE = stringPreferencesKey("tts_template")
    val TTS_PITCH = floatPreferencesKey("tts_pitch")
    val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
    val ALARM_MODE = stringPreferencesKey("alarm_mode")
    val ALARM_DURATION_SECONDS = intPreferencesKey("alarm_duration_seconds")
    val ALARM_VOLUME = intPreferencesKey("alarm_volume")
    val SNOOZE_INTERVAL_MINUTES = intPreferencesKey("snooze_interval_minutes")
    val RINGTONE_URI = stringPreferencesKey("ringtone_uri")
    val RINGTONE_TITLE = stringPreferencesKey("ringtone_title")
    val IS_CUSTOM_RINGTONE = booleanPreferencesKey("is_custom_ringtone")
    val TEMP_WARNING_ENABLED = booleanPreferencesKey("temp_warning_enabled")
    val TEMP_WARNING_THRESHOLD = floatPreferencesKey("temp_warning_threshold")
    val LONG_PLUGGED_REMINDER = booleanPreferencesKey("long_plugged_reminder")
    val SLEEP_MODE_ENABLED = booleanPreferencesKey("sleep_mode_enabled")
    val SLEEP_START_HOUR = intPreferencesKey("sleep_start_hour")
    val SLEEP_START_MINUTE = intPreferencesKey("sleep_start_minute")
    val SLEEP_END_HOUR = intPreferencesKey("sleep_end_hour")
    val SLEEP_END_MINUTE = intPreferencesKey("sleep_end_minute")
    val SLEEP_DIM_DISPLAY = booleanPreferencesKey("sleep_dim_display")
    val THEME_MODE = stringPreferencesKey("theme_mode")
  }

  val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
    val targetStrings = prefs[Keys.TARGET_LEVELS] ?: setOf("80", "100")
    val targets = targetStrings.mapNotNull { it.toIntOrNull() }.toSet().ifEmpty { setOf(80, 100) }
    val modeStr = prefs[Keys.ALARM_MODE] ?: AlarmMode.ALARM_AND_VOICE.name
    val mode = try { AlarmMode.valueOf(modeStr) } catch (_: Exception) { AlarmMode.ALARM_AND_VOICE }

    UserSettings(
      targetLevels = targets,
      smartAlarmEnabled = prefs[Keys.SMART_ALARM_ENABLED] ?: true,
      ttsEnabled = prefs[Keys.TTS_ENABLED] ?: true,
      ttsTemplate = prefs[Keys.TTS_TEMPLATE] ?: "Baterai sudah mencapai {target} persen, waktunya mencabut charger.",
      ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
      ttsSpeechRate = prefs[Keys.TTS_SPEECH_RATE] ?: 1.0f,
      alarmMode = mode,
      alarmDurationSeconds = prefs[Keys.ALARM_DURATION_SECONDS] ?: 30,
      alarmVolume = prefs[Keys.ALARM_VOLUME] ?: 85,
      snoozeIntervalMinutes = prefs[Keys.SNOOZE_INTERVAL_MINUTES] ?: 5,
      ringtoneUri = prefs[Keys.RINGTONE_URI],
      ringtoneTitle = prefs[Keys.RINGTONE_TITLE] ?: "Nada Alarm Bawaan",
      isCustomRingtone = prefs[Keys.IS_CUSTOM_RINGTONE] ?: false,
      tempWarningEnabled = prefs[Keys.TEMP_WARNING_ENABLED] ?: true,
      tempWarningThreshold = prefs[Keys.TEMP_WARNING_THRESHOLD] ?: 42.0f,
      longPluggedReminderEnabled = prefs[Keys.LONG_PLUGGED_REMINDER] ?: true,
      sleepModeEnabled = prefs[Keys.SLEEP_MODE_ENABLED] ?: false,
      sleepStartHour = prefs[Keys.SLEEP_START_HOUR] ?: 22,
      sleepStartMinute = prefs[Keys.SLEEP_START_MINUTE] ?: 0,
      sleepEndHour = prefs[Keys.SLEEP_END_HOUR] ?: 6,
      sleepEndMinute = prefs[Keys.SLEEP_END_MINUTE] ?: 0,
      sleepDimDisplay = prefs[Keys.SLEEP_DIM_DISPLAY] ?: true,
      themeMode = prefs[Keys.THEME_MODE] ?: "LIGHT"
    )
  }

  suspend fun updateTargetLevels(targets: Set<Int>) {
    context.dataStore.edit { prefs ->
      prefs[Keys.TARGET_LEVELS] = targets.map { it.toString() }.toSet()
    }
  }

  suspend fun toggleTargetLevel(level: Int) {
    context.dataStore.edit { prefs ->
      val current = (prefs[Keys.TARGET_LEVELS] ?: setOf("80", "100")).toMutableSet()
      val str = level.toString()
      if (current.contains(str)) {
        if (current.size > 1) { // keep at least 1
          current.remove(str)
        }
      } else {
        current.add(str)
      }
      prefs[Keys.TARGET_LEVELS] = current
    }
  }

  suspend fun updateSmartAlarmEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SMART_ALARM_ENABLED] = enabled }
  }

  suspend fun updateTtsTemplate(template: String) {
    context.dataStore.edit { it[Keys.TTS_TEMPLATE] = template }
  }

  suspend fun updateTtsVoiceParams(pitch: Float, rate: Float) {
    context.dataStore.edit {
      it[Keys.TTS_PITCH] = pitch
      it[Keys.TTS_SPEECH_RATE] = rate
    }
  }

  suspend fun updateAlarmMode(mode: AlarmMode) {
    context.dataStore.edit { it[Keys.ALARM_MODE] = mode.name }
  }

  suspend fun updateAlarmDuration(seconds: Int) {
    context.dataStore.edit { it[Keys.ALARM_DURATION_SECONDS] = seconds }
  }

  suspend fun updateAlarmVolume(volume: Int) {
    context.dataStore.edit { it[Keys.ALARM_VOLUME] = volume }
  }

  suspend fun updateSnoozeInterval(minutes: Int) {
    context.dataStore.edit { it[Keys.SNOOZE_INTERVAL_MINUTES] = minutes }
  }

  suspend fun updateRingtone(uri: String?, title: String, isCustom: Boolean) {
    context.dataStore.edit { prefs ->
      if (uri != null) {
        prefs[Keys.RINGTONE_URI] = uri
      } else {
        prefs.remove(Keys.RINGTONE_URI)
      }
      prefs[Keys.RINGTONE_TITLE] = title
      prefs[Keys.IS_CUSTOM_RINGTONE] = isCustom
    }
  }

  suspend fun updateTempWarning(enabled: Boolean, threshold: Float) {
    context.dataStore.edit {
      it[Keys.TEMP_WARNING_ENABLED] = enabled
      it[Keys.TEMP_WARNING_THRESHOLD] = threshold
    }
  }

  suspend fun updateSleepMode(
    enabled: Boolean,
    startH: Int = 22,
    startM: Int = 0,
    endH: Int = 6,
    endM: Int = 0,
    dim: Boolean = true
  ) {
    context.dataStore.edit {
      it[Keys.SLEEP_MODE_ENABLED] = enabled
      it[Keys.SLEEP_START_HOUR] = startH
      it[Keys.SLEEP_START_MINUTE] = startM
      it[Keys.SLEEP_END_HOUR] = endH
      it[Keys.SLEEP_END_MINUTE] = endM
      it[Keys.SLEEP_DIM_DISPLAY] = dim
    }
  }

  suspend fun updateThemeMode(mode: String) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode }
  }

  suspend fun updateLongPluggedReminder(enabled: Boolean) {
    context.dataStore.edit { it[Keys.LONG_PLUGGED_REMINDER] = enabled }
  }
}
