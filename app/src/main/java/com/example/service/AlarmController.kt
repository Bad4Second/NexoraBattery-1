package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.R
import com.example.data.local.AlarmMode
import com.example.data.local.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RingtoneItem(
  val title: String,
  val uri: String? // null for default
)

class AlarmController(private val context: Context) : TextToSpeech.OnInitListener {

  private var tts: TextToSpeech? = null
  private var isTtsReady = false
  private var pendingSpeechText: String? = null
  private var pendingPitch: Float = 1.0f
  private var pendingRate: Float = 1.0f

  private var activeMediaPlayer: MediaPlayer? = null
  private var activeRingtone: Ringtone? = null
  private var fallbackToneGenerator: ToneGenerator? = null
  private var toneRunnable: Runnable? = null
  private var speechRunnable: Runnable? = null

  private var wakeLock: PowerManager.WakeLock? = null
  private val handler = Handler(Looper.getMainLooper())

  private val _isAlarmActive = MutableStateFlow(false)
  val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

  private val _currentAlarmTarget = MutableStateFlow<Int?>(null)
  val currentAlarmTarget: StateFlow<Int?> = _currentAlarmTarget.asStateFlow()

  private val vibrator: Vibrator by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
      vm.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
  }

  private val powerManager: PowerManager by lazy {
    context.getSystemService(Context.POWER_SERVICE) as PowerManager
  }

  private val audioManager: AudioManager by lazy {
    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  // Audio Attributes for Alarm (prioritizes USAGE_ALARM and enforces audibility)
  val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
    .build()

  // Audio Attributes for Alarm TTS speech
  val speechAudioAttributes: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
    .build()

  private var audioFocusRequest: AudioFocusRequest? = null
  private var hasAudioFocus = false

  private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS -> {
        Log.w("AlarmController", "AudioFocus permanently lost")
        hasAudioFocus = false
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        Log.w("AlarmController", "AudioFocus transiently lost")
        hasAudioFocus = false
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        Log.d("AlarmController", "AudioFocus can duck; maintaining high priority alarm output")
      }
      AudioManager.AUDIOFOCUS_GAIN -> {
        Log.d("AlarmController", "AudioFocus gained / restored")
        hasAudioFocus = true
        ensureAudibleVolume(90)
        routeAudioToSpeaker()
      }
    }
  }

  init {
    try {
      tts = TextToSpeech(context.applicationContext, this)
    } catch (e: Exception) {
      Log.e("AlarmController", "TTS init failed: ${e.message}")
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val indonesian = Locale("id", "ID")
      val result = tts?.setLanguage(indonesian)
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        tts?.language = Locale.getDefault()
      }
      isTtsReady = true

      // Play any pending speech queued before initialization
      pendingSpeechText?.let { text ->
        speak(text, pendingPitch, pendingRate)
        pendingSpeechText = null
      }
    }
  }

  /**
   * Requests AudioManager audio focus specifically on the Alarm stream
   * to gain transient exclusive focus and prioritize alarm output over any media.
   */
  fun requestAudioFocus(): Boolean {
    return try {
      val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
          .setAudioAttributes(alarmAudioAttributes)
          .setAcceptsDelayedFocusGain(true)
          .setWillPauseWhenDucked(true)
          .setOnAudioFocusChangeListener(focusChangeListener, handler)
          .build()
        audioFocusRequest = request
        audioManager.requestAudioFocus(request)
      } else {
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus(
          focusChangeListener,
          AudioManager.STREAM_ALARM,
          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        )
      }
      hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
      Log.d("AlarmController", "requestAudioFocus granted=$hasAudioFocus")
      hasAudioFocus
    } catch (e: Exception) {
      Log.e("AlarmController", "requestAudioFocus failed: ${e.message}")
      false
    }
  }

  /**
   * Abandons audio focus cleanly when the alarm stops.
   */
  fun abandonAudioFocus() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        audioFocusRequest?.let {
          audioManager.abandonAudioFocusRequest(it)
        }
        audioFocusRequest = null
      } else {
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(focusChangeListener)
      }
      hasAudioFocus = false
      Log.d("AlarmController", "abandonAudioFocus executed")
    } catch (e: Exception) {
      Log.w("AlarmController", "abandonAudioFocus failed: ${e.message}")
    }
  }

  /**
   * Prioritizes and forces audio routing to the device speaker
   * to prevent sound from being suppressed by connected accessories or muted call routing.
   */
  fun routeAudioToSpeaker() {
    try {
      audioManager.mode = AudioManager.MODE_NORMAL

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val devices = audioManager.availableCommunicationDevices
        val speakerDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        if (speakerDevice != null) {
          val ok = audioManager.setCommunicationDevice(speakerDevice)
          Log.d("AlarmController", "setCommunicationDevice(BUILTIN_SPEAKER): $ok")
        }
      } else {
        @Suppress("DEPRECATION")
        if (!audioManager.isSpeakerphoneOn) {
          audioManager.isSpeakerphoneOn = true
        }
      }
    } catch (e: Exception) {
      Log.w("AlarmController", "routeAudioToSpeaker error: ${e.message}")
    }
  }

  /**
   * Restores audio routing when the alarm is dismissed.
   */
  fun restoreAudioRouting() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        audioManager.clearCommunicationDevice()
      } else {
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false
      }
    } catch (e: Exception) {
      Log.w("AlarmController", "restoreAudioRouting error: ${e.message}")
    }
  }

  /**
   * Prioritizes the alarm volume channel, unmuting streams and setting volume to high.
   * Also ensures music stream is audible for virtual/emulator outputs.
   */
  fun ensureAudibleVolume(volumePercent: Int) {
    try {
      // 1. Unmute STREAM_ALARM & STREAM_MUSIC if muted on Android M+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
          if (audioManager.isStreamMute(AudioManager.STREAM_ALARM)) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
          }
          if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
          }
        } catch (_: Exception) {}
      }

      // 2. Prioritize and boost STREAM_ALARM (Primary Alarm Volume Channel)
      val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
      val targetAlarm = (maxAlarm * (volumePercent.coerceIn(30, 100) / 100f)).toInt().coerceIn(1, maxAlarm)
      audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetAlarm, 0)

      // 3. Also boost STREAM_MUSIC (crucial for emulator audio and streaming outputs)
      val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
      val targetMusic = (maxMusic * (volumePercent.coerceIn(30, 100) / 100f)).toInt().coerceIn(1, maxMusic)
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetMusic, 0)

      Log.d("AlarmController", "Volume set: ALARM=$targetAlarm/$maxAlarm, MUSIC=$targetMusic/$maxMusic")
    } catch (e: Exception) {
      Log.w("AlarmController", "ensureAudibleVolume failed: ${e.message}")
    }
  }

  fun triggerAlarm(
    targetLevel: Int,
    currentLevel: Int,
    settings: UserSettings,
    onFinish: (() -> Unit)? = null
  ) {
    // If an alarm is already playing, stop previous one cleanly and restart
    stopAlarm()

    _isAlarmActive.value = true
    _currentAlarmTarget.value = targetLevel

    acquireWakeLock()
    requestAudioFocus()
    routeAudioToSpeaker()
    ensureAudibleVolume(settings.alarmVolume)

    // 1. Vibrate if not silent
    if (settings.alarmMode != AlarmMode.SILENT) {
      startVibration()
    }

    // 2. Format speech message
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val spokenText = settings.ttsTemplate
      .replace("{battery}", currentLevel.toString())
      .replace("{target}", targetLevel.toString())
      .replace("{time}", timeStr)

    val normalVol = (settings.alarmVolume.coerceIn(10, 100) / 100f)

    // 3. Play sound based on mode
    when (settings.alarmMode) {
      AlarmMode.ALARM_AND_VOICE -> {
        startRingtone(settings.alarmVolume, settings.ringtoneUri)
        // Duck ringtone slightly when voice speaks so speech is loud and clear
        handler.postDelayed({
          if (_isAlarmActive.value) {
            activeMediaPlayer?.setVolume(0.25f, 0.25f)
            speak(spokenText, settings.ttsPitch, settings.ttsSpeechRate)
            // Restore ringtone volume after speech utterance finishes (~4s)
            handler.postDelayed({
              if (_isAlarmActive.value) {
                activeMediaPlayer?.setVolume(1.0f, 1.0f)
              }
            }, 4000L)
          }
        }, 500L)

        scheduleRepeatSpeech(spokenText, settings.ttsPitch, settings.ttsSpeechRate, normalVol)
      }
      AlarmMode.VOICE_ONLY -> {
        speak(spokenText, settings.ttsPitch, settings.ttsSpeechRate)
        scheduleRepeatSpeech(spokenText, settings.ttsPitch, settings.ttsSpeechRate, normalVol)
      }
      AlarmMode.ALARM_ONLY -> {
        startRingtone(settings.alarmVolume, settings.ringtoneUri)
      }
      AlarmMode.SILENT -> {
        // Notification only, no sound
      }
    }

    // 4. Handle duration timeout if not continuous (-1)
    if (settings.alarmDurationSeconds > 0) {
      handler.postDelayed({
        if (_isAlarmActive.value) {
          stopAlarm()
          onFinish?.invoke()
        }
      }, settings.alarmDurationSeconds * 1000L)
    }
  }

  fun startRingtone(volumePercent: Int, ringtoneUriStr: String? = null) {
    stopRingtoneOnly()
    ensureAudibleVolume(volumePercent)

    var playedSuccessfully = false

    // Method 1: If user picked a custom audio file or system ringtone
    if (!ringtoneUriStr.isNullOrEmpty()) {
      try {
        val targetUri = Uri.parse(ringtoneUriStr)
        val player = MediaPlayer().apply {
          setAudioAttributes(alarmAudioAttributes)
          setDataSource(context, targetUri)
          isLooping = true
          setVolume(1.0f, 1.0f)
          prepare()
          start()
        }
        activeMediaPlayer = player
        playedSuccessfully = true
        Log.d("AlarmController", "Started MediaPlayer with URI: $ringtoneUriStr")
      } catch (e: Exception) {
        Log.w("AlarmController", "MediaPlayer with ringtoneUri failed: ${e.message}")
      }
    }

    // Method 2: Play guaranteed bundled high quality alarm sound (alarm_chime.wav)
    if (!playedSuccessfully) {
      try {
        val afd = context.resources.openRawResourceFd(R.raw.alarm_chime)
        if (afd != null) {
          val player = MediaPlayer().apply {
            setAudioAttributes(alarmAudioAttributes)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = true
            setVolume(1.0f, 1.0f)
            prepare()
            start()
          }
          activeMediaPlayer = player
          playedSuccessfully = true
          Log.d("AlarmController", "Started MediaPlayer with bundled R.raw.alarm_chime")
        }
      } catch (e: Exception) {
        Log.w("AlarmController", "MediaPlayer with bundled R.raw.alarm_chime failed: ${e.message}")
      }
    }

    // Method 3: Try system RingtoneManager fallback
    if (!playedSuccessfully) {
      try {
        val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (fallbackUri != null) {
          val ringtone = RingtoneManager.getRingtone(context, fallbackUri)
          ringtone?.audioAttributes = alarmAudioAttributes
          ringtone?.play()
          activeRingtone = ringtone
          playedSuccessfully = true
        }
      } catch (e: Exception) {
        Log.w("AlarmController", "RingtoneManager fallback failed: ${e.message}")
      }
    }

    // Method 4: Hardware ToneGenerator fallback directly on STREAM_ALARM
    if (!playedSuccessfully) {
      startFallbackToneBeeps(volumePercent)
    }
  }

  private fun startFallbackToneBeeps(volumePercent: Int) {
    try {
      val vol = volumePercent.coerceIn(30, 100)
      fallbackToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, vol)
      toneRunnable = object : Runnable {
        override fun run() {
          try {
            fallbackToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
          } catch (_: Exception) {}
          handler.postDelayed(this, 900)
        }
      }
      toneRunnable?.let { handler.post(it) }
    } catch (e: Exception) {
      Log.e("AlarmController", "Fallback ToneGenerator failed: ${e.message}")
    }
  }

  private fun stopRingtoneOnly() {
    try {
      activeMediaPlayer?.stop()
      activeMediaPlayer?.release()
    } catch (_: Exception) {}
    activeMediaPlayer = null

    try {
      activeRingtone?.stop()
    } catch (_: Exception) {}
    activeRingtone = null

    toneRunnable?.let { handler.removeCallbacks(it) }
    toneRunnable = null

    try {
      fallbackToneGenerator?.stopTone()
      fallbackToneGenerator?.release()
    } catch (_: Exception) {}
    fallbackToneGenerator = null
  }

  private fun startVibration() {
    try {
      if (vibrator.hasVibrator()) {
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(pattern, 0)
        }
      }
    } catch (_: Exception) {}
  }

  private fun stopVibration() {
    try {
      vibrator.cancel()
    } catch (_: Exception) {}
  }

  fun speak(text: String, pitch: Float = 1.0f, rate: Float = 1.0f) {
    if (!isTtsReady) {
      pendingSpeechText = text
      pendingPitch = pitch
      pendingRate = rate
      return
    }
    try {
      ensureAudibleVolume(90)
      tts?.setPitch(pitch)
      tts?.setSpeechRate(rate)

      // Route speech to USAGE_ALARM so TTS is prioritized over media and audible through speaker
      tts?.setAudioAttributes(speechAudioAttributes)
      tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "BatteryGuardianUtterance")
    } catch (e: Exception) {
      Log.e("AlarmController", "TTS speak failed: ${e.message}")
    }
  }

  private fun scheduleRepeatSpeech(
    text: String,
    pitch: Float,
    rate: Float,
    normalVolume: Float = 0.85f
  ) {
    speechRunnable?.let { handler.removeCallbacks(it) }
    speechRunnable = object : Runnable {
      override fun run() {
        if (_isAlarmActive.value) {
          activeMediaPlayer?.setVolume(0.25f, 0.25f)
          speak(text, pitch, rate)
          handler.postDelayed({
            if (_isAlarmActive.value) {
              activeMediaPlayer?.setVolume(1.0f, 1.0f)
            }
          }, 4000L)
          handler.postDelayed(this, 10000L) // Repeat speech every 10 seconds while alarm is active
        }
      }
    }
    speechRunnable?.let { handler.postDelayed(it, 9000L) }
  }

  fun previewVoice(template: String, target: Int, pitch: Float, rate: Float) {
    requestAudioFocus()
    routeAudioToSpeaker()
    ensureAudibleVolume(90)
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val spokenText = template
      .replace("{battery}", target.toString())
      .replace("{target}", target.toString())
      .replace("{time}", timeStr)
    speak(spokenText, pitch, rate)
    handler.postDelayed({
      restoreAudioRouting()
      abandonAudioFocus()
    }, 4000L)
  }

  fun previewAlarmSound(volumePercent: Int, ringtoneUriStr: String? = null) {
    stopAlarm()
    requestAudioFocus()
    routeAudioToSpeaker()
    startRingtone(volumePercent, ringtoneUriStr)
    handler.postDelayed({
      stopRingtoneOnly()
      restoreAudioRouting()
      abandonAudioFocus()
    }, 4000)
  }

  fun stopAlarm() {
    handler.removeCallbacksAndMessages(null)
    speechRunnable?.let { handler.removeCallbacks(it) }
    speechRunnable = null
    stopRingtoneOnly()
    stopVibration()
    try {
      tts?.stop()
    } catch (_: Exception) {}
    _isAlarmActive.value = false
    _currentAlarmTarget.value = null
    restoreAudioRouting()
    abandonAudioFocus()
    releaseWakeLock()
  }

  private fun acquireWakeLock() {
    if (wakeLock == null) {
      wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "BatteryGuardian::AlarmWakeLock"
      )
    }
    if (wakeLock?.isHeld == false) {
      wakeLock?.acquire(10 * 60 * 1000L)
    }
  }

  private fun releaseWakeLock() {
    try {
      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }
    } catch (_: Exception) {}
  }

  fun release() {
    stopAlarm()
    try {
      tts?.shutdown()
    } catch (_: Exception) {}
  }

  /**
   * Helper to query system available alarm and ringtone options
   */
  fun querySystemRingtones(): List<RingtoneItem> {
    val items = mutableListOf<RingtoneItem>()
    items.add(RingtoneItem("Nada Alarm Bawaan (Melodi Guardian)", null))

    try {
      val rm = RingtoneManager(context).apply {
        setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
      }
      val cursor = rm.cursor
      while (cursor != null && cursor.moveToNext()) {
        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
        val uri = rm.getRingtoneUri(cursor.position).toString()
        if (!title.isNullOrEmpty()) {
          items.add(RingtoneItem(title, uri))
        }
      }
    } catch (e: Exception) {
      Log.w("AlarmController", "Query ringtones failed: ${e.message}")
    }

    return items
  }

  /**
   * Copies user selected audio file to app storage to ensure persistence
   */
  fun saveCustomAudioFile(sourceUri: Uri, originalName: String?): Pair<String, String>? {
    return try {
      val extension = originalName?.substringAfterLast(".", "mp3") ?: "mp3"
      val targetFile = File(context.filesDir, "custom_battery_alarm.$extension")

      context.contentResolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(targetFile).use { output ->
          input.copyTo(output)
        }
      }

      val title = originalName ?: "Audio Kustom (${targetFile.name})"
      val savedUri = Uri.fromFile(targetFile).toString()
      Pair(savedUri, title)
    } catch (e: Exception) {
      Log.e("AlarmController", "Save custom audio file failed: ${e.message}")
      null
    }
  }
}
