package com.example.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BatteryViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.MintCharging
import com.example.ui.theme.TurquoiseAccent
import com.example.ui.theme.ZomoPurplePrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SleepScreen(
  viewModel: BatteryViewModel,
  modifier: Modifier = Modifier
) {
  val userSettings by viewModel.userSettings.collectAsState()
  val batteryInfo by viewModel.batteryState.collectAsState()
  val chargingSessions by viewModel.chargingSessions.collectAsState()
  val scrollState = rememberScrollState()

  var isNightClockOpen by remember { mutableStateOf(false) }

  // If Night Clock is opened, render full-screen night clock
  if (isNightClockOpen) {
    NightClockView(
      batteryPercentage = batteryInfo.percentage,
      isCharging = batteryInfo.isCharging,
      onClose = { isNightClockOpen = false }
    )
    return
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Text(
      text = "Sleep Charging Mode",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Perlindungan baterai otomatis saat Anda tidur di malam hari",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 1. Sleep Mode Switch Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10000000), spotColor = Color(0x147E42F5))
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(ZomoPurplePrimary.copy(alpha = 0.15f))
          ) {
            Icon(
              imageVector = Icons.Default.Bedtime,
              contentDescription = null,
              tint = ZomoPurplePrimary,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = "Mode Tidur Otomatis",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Aktifkan alarm pintar dan perlindungan suhu saat tidur.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
          checked = userSettings.sleepModeEnabled,
          onCheckedChange = { checked ->
            viewModel.updateSleepMode(
              enabled = checked,
              startH = userSettings.sleepStartHour,
              startM = userSettings.sleepStartMinute,
              endH = userSettings.sleepEndHour,
              endM = userSettings.sleepEndMinute,
              dim = userSettings.sleepDimDisplay
            )
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ZomoPurplePrimary
          ),
          modifier = Modifier.testTag("sleep_mode_switch")
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Bedtime Schedule Configuration
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10000000), spotColor = Color(0x147E42F5))
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        .padding(18.dp)
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = ZomoPurplePrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Jadwal Jam Tidur",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Bedtime Start Card
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
              .padding(14.dp)
          ) {
            Column {
              Text(
                text = "Mulai Tidur",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", userSettings.sleepStartHour, userSettings.sleepStartMinute),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = ZomoPurplePrimary
              )
              Text(
                text = "Malam Hari",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
              )
            }
          }

          // Wakeup Card
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
              .padding(14.dp)
          ) {
            Column {
              Text(
                text = "Bangun Pagi",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = String.format(Locale.getDefault(), "%02d:%02d", userSettings.sleepEndHour, userSettings.sleepEndMinute),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TurquoiseAccent
              )
              Text(
                text = "Pagi Hari",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dim display option
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Redupkan Layar Saat Tidur",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Switch(
            checked = userSettings.sleepDimDisplay,
            onCheckedChange = { checked ->
              viewModel.updateSleepMode(
                enabled = userSettings.sleepModeEnabled,
                startH = userSettings.sleepStartHour,
                startM = userSettings.sleepStartMinute,
                endH = userSettings.sleepEndHour,
                endM = userSettings.sleepEndMinute,
                dim = checked
              )
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = ZomoPurplePrimary
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Open Night Clock Button
    Button(
      onClick = { isNightClockOpen = true },
      colors = ButtonDefaults.buttonColors(containerColor = ZomoPurplePrimary),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("open_night_clock_button")
    ) {
      Icon(Icons.Default.NightsStay, contentDescription = null, tint = Color.White)
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = "Buka Jam Malam Minimalis (Night Clock)",
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4. Morning Wake-Up Charging Summary Card
    val latestSession = chargingSessions.firstOrNull()
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10000000), spotColor = Color(0x147E42F5))
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        .padding(18.dp)
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.WbSunny, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Ringkasan Sesi Pengisian Terbaru",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (latestSession != null) {
          val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
          val startTimeStr = dateFormat.format(Date(latestSession.startTime))
          val endTimeStr = latestSession.endTime?.let { dateFormat.format(Date(it)) } ?: "Berlangsung"

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Waktu Mulai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(startTimeStr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Column {
              Text("Waktu Selesai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(endTimeStr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Column {
              Text("Total Durasi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${latestSession.durationMinutes} mnt", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MintCharging)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Level Pengisian", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${latestSession.startLevel}% → ${latestSession.endLevel ?: batteryInfo.percentage}% (+${latestSession.gainedPercentage}%)", style = MaterialTheme.typography.bodyMedium, color = TurquoiseAccent, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Suhu Maksimal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${String.format(Locale.US, "%.1f", latestSession.maxTemperature)}°C", style = MaterialTheme.typography.bodyMedium, color = ZomoPurplePrimary, fontWeight = FontWeight.SemiBold)
            }
          }
        } else {
          Text(
            text = "Belum ada sesi pengisian malam hari yang tercatat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. Mini Calendar View of Charging Activity
    MiniCalendarWidget()

    Spacer(modifier = Modifier.height(24.dp))
  }
}

@Composable
fun MiniCalendarWidget() {
  val calendar = Calendar.getInstance()
  val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
  val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
  val monthStr = monthFormat.format(calendar.time)

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10000000), spotColor = Color(0x147E42F5))
      .clip(RoundedCornerShape(24.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
      .padding(18.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TurquoiseAccent, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Kalender Charging",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Text(
          text = monthStr,
          style = MaterialTheme.typography.labelMedium,
          color = ZomoPurplePrimary,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 7-day mini strip around current day
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        for (i in -3..3) {
          val dayNum = currentDay + i
          val isToday = i == 0
          val dayName = when ((calendar.get(Calendar.DAY_OF_WEEK) + i + 7) % 7) {
            1 -> "Min"
            2 -> "Sen"
            3 -> "Sel"
            4 -> "Rab"
            5 -> "Kam"
            6 -> "Jum"
            else -> "Sab"
          }

          val pillBg = if (isToday) ZomoPurplePrimary else MaterialTheme.colorScheme.surfaceVariant
          val pillBorder = if (isToday) ZomoPurplePrimary else MaterialTheme.colorScheme.outline
          val pillTextColor = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .background(pillBg)
              .border(1.dp, pillBorder, RoundedCornerShape(14.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp)
          ) {
            Text(
              text = dayName,
              style = MaterialTheme.typography.labelSmall,
              color = if (isToday) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (dayNum > 0) dayNum.toString() else "1",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
              color = pillTextColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Charging dot indicator
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isToday) Color.White else MintCharging)
            )
          }
        }
      }
    }
  }
}

@Composable
fun NightClockView(
  batteryPercentage: Int,
  isCharging: Boolean,
  onClose: () -> Unit
) {
  var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())) }
  var currentDate by remember { mutableStateOf(SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())) }

  LaunchedEffect(Unit) {
    while (true) {
      currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
      currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
      delay(1000)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF07040D))
      .padding(24.dp)
  ) {
    // Close button top-right
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.12f))
        .clickable { onClose() }
        .padding(10.dp)
        .testTag("close_night_clock_button")
    ) {
      Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
    }

    // Centered Minimal Night Clock
    Column(
      modifier = Modifier
        .fillMaxSize()
        .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Weather summary widget
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(Color.White.copy(alpha = 0.08f))
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Icon(Icons.Default.Cloud, contentDescription = null, tint = TurquoiseAccent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "25°C • Cerah Berawan • Jakarta",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFA59DBA)
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Giant Time Display
      Text(
        text = currentTime,
        style = MaterialTheme.typography.displayLarge.copy(
          fontSize = 72.sp,
          letterSpacing = 2.sp
        ),
        fontWeight = FontWeight.Bold,
        color = Color(0xFFC7B5FD)
      )

      Text(
        text = currentDate,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFF867E9D)
      )

      Spacer(modifier = Modifier.height(40.dp))

      // Battery ring/pill
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0xFF160F2B))
          .border(1.dp, TurquoiseAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
          .padding(horizontal = 18.dp, vertical = 10.dp)
      ) {
        Icon(
          imageVector = if (isCharging) Icons.Default.ElectricBolt else Icons.Default.Favorite,
          contentDescription = null,
          tint = if (isCharging) MintCharging else TurquoiseAccent,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "$batteryPercentage% • ${if (isCharging) "Charging" else "Baterai"}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "Smart Charging Alarm Siaga di Background",
        style = MaterialTheme.typography.labelSmall,
        color = MintCharging.copy(alpha = 0.8f)
      )
    }
  }
}
