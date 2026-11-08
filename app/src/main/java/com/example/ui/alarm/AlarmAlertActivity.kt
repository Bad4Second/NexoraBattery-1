package com.example.ui.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.BatteryGuardianApp
import com.example.service.BatteryAlarmScheduler
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TurquoiseAccent
import com.example.ui.theme.ZomoPurplePrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmAlertActivity : ComponentActivity() {

  companion object {
    @Volatile
    var activeInstance: AlarmAlertActivity? = null

    fun dismissIfActive() {
      activeInstance?.runOnUiThread {
        if (activeInstance?.isFinishing == false) {
          activeInstance?.finish()
        }
      }
      activeInstance = null
    }
  }

  private val dismissReceiver = object : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (!isFinishing) {
        finish()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activeInstance = this
    enableEdgeToEdge()

    // Register broadcast receiver to guarantee immediate dismiss when notification action is pressed
    val filter = android.content.IntentFilter().apply {
      addAction(com.example.receiver.AlarmDismissReceiver.ACTION_STOP_ALARM)
      addAction(com.example.receiver.AlarmDismissReceiver.ACTION_DISMISS_POPUP)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(dismissReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
      registerReceiver(dismissReceiver, filter)
    }

    // Configure window to turn screen on and show over lock screen
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
      val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
      keyguardManager.requestDismissKeyguard(this, null)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
      )
    }

    // Auto-close if alarm is stopped anywhere (e.g. from notification "Hentikan" or service)
    lifecycleScope.launch {
      BatteryGuardianApp.instance.alarmController.isAlarmActive.collect { isActive ->
        if (!isActive && !isFinishing) {
          finish()
        }
      }
    }

    val targetPercent = intent.getIntExtra("TARGET_PERCENT", 80)
    val currentPercent = intent.getIntExtra("CURRENT_PERCENT", targetPercent)

    setContent {
      MyApplicationTheme(darkTheme = true) {
        AlarmAlertScreen(
          targetPercent = targetPercent,
          currentPercent = currentPercent,
          onStop = {
            stopAlarm()
            finish()
          },
          onSnooze = {
            snoozeAlarm(targetPercent)
            finish()
          },
          onDismiss = {
            stopAlarm()
            finish()
          }
        )
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(dismissReceiver)
    } catch (_: Exception) {}
    if (activeInstance == this) {
      activeInstance = null
    }
  }

  private fun stopAlarm() {
    BatteryGuardianApp.instance.alarmController.stopAlarm()
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(BatteryGuardianApp.NOTIFICATION_ALARM_ID)
  }

  private fun snoozeAlarm(target: Int) {
    stopAlarm()
    val app = BatteryGuardianApp.instance
    lifecycleScope.launch {
      val settings = app.preferences.settingsFlow.first()
      val interval = settings.snoozeIntervalMinutes
      BatteryAlarmScheduler.scheduleSnooze(
        this@AlarmAlertActivity,
        target,
        interval
      )
      Toast.makeText(this@AlarmAlertActivity, "Alarm ditunda $interval menit", Toast.LENGTH_SHORT).show()
    }
  }
}

@Composable
fun AlarmAlertScreen(
  targetPercent: Int,
  currentPercent: Int,
  onStop: () -> Unit,
  onSnooze: () -> Unit,
  onDismiss: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = Color(0xFF0F0B1E)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0xFF221147),
              Color(0xFF140B2B),
              Color(0xFF080414)
            )
          )
        )
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Glowing Icon (Matching purple and cyan aesthetic from reference)
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(130.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                listOf(
                  TurquoiseAccent.copy(alpha = 0.35f),
                  ZomoPurplePrimary.copy(alpha = 0.2f),
                  Color.Transparent
                )
              )
            )
            .border(2.dp, TurquoiseAccent, CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.Power,
            contentDescription = "Target Reached",
            tint = TurquoiseAccent,
            modifier = Modifier.size(64.dp)
          )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
          text = "Target Baterai Tercapai!",
          style = MaterialTheme.typography.headlineLarge,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Baterai telah mencapai ${currentPercent}% (Target: ${targetPercent}%)\nCabut pengisi daya untuk menjaga kesehatan baterai.",
          style = MaterialTheme.typography.bodyLarge,
          color = Color(0xFFDDD6FE),
          textAlign = TextAlign.Center,
          lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(44.dp))

        // Main Stop Button (Rounded pill)
        Button(
          onClick = onStop,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF486A)
          ),
          shape = RoundedCornerShape(26.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("alarm_stop_button")
        ) {
          Icon(Icons.Default.Stop, contentDescription = "Hentikan", tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Hentikan Alarm",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Snooze Button
          OutlinedButton(
            onClick = onSnooze,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("alarm_snooze_button"),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = TurquoiseAccent
            )
          ) {
            Icon(Icons.Default.Snooze, contentDescription = "Tunda", tint = TurquoiseAccent)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tunda 5m", fontWeight = FontWeight.Bold)
          }

          // Dismiss Button
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("alarm_dismiss_button"),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = Color(0xFFA59BC2)
            )
          ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFFA59BC2))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tutup", fontWeight = FontWeight.Medium)
          }
        }
      }
    }
  }
}
