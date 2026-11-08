package com.example.ui.history

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AlarmHistoryEntity
import com.example.data.local.entity.ChargingSessionEntity
import com.example.ui.BatteryViewModel
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.MintCharging
import com.example.ui.theme.TurquoiseAccent
import com.example.ui.theme.ZomoPurplePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  viewModel: BatteryViewModel,
  modifier: Modifier = Modifier
) {
  val chargingSessions by viewModel.chargingSessions.collectAsState()
  val alarmHistories by viewModel.alarmHistories.collectAsState()
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Sesi Pengisian, 1: Riwayat Alarm
  var showClearDialog by remember { mutableStateOf(false) }

  val totalSessions = chargingSessions.size
  val avgDurationMinutes = if (totalSessions > 0) {
    chargingSessions.map { it.durationMinutes }.average().toInt()
  } else 0
  val maxRecordedTemp = if (totalSessions > 0) {
    chargingSessions.maxOfOrNull { it.maxTemperature } ?: 0f
  } else 0f

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    // Header & Clear Action
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Riwayat & Statistik",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Catatan sesi charging dan log alarm",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (chargingSessions.isNotEmpty() || alarmHistories.isNotEmpty()) {
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { showClearDialog = true }
            .padding(8.dp)
            .testTag("clear_history_icon_button")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Hapus Riwayat",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3 Quick Stat Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StatSmallCard(
        title = "Total Sesi",
        value = "$totalSessions",
        unit = "kali",
        color = ZomoPurplePrimary,
        modifier = Modifier.weight(1f)
      )
      StatSmallCard(
        title = "Rata Durasi",
        value = "$avgDurationMinutes",
        unit = "menit",
        color = TurquoiseAccent,
        modifier = Modifier.weight(1f)
      )
      StatSmallCard(
        title = "Suhu Max",
        value = if (maxRecordedTemp > 0) String.format(Locale.US, "%.1f", maxRecordedTemp) else "-",
        unit = "°C",
        color = if (maxRecordedTemp >= 42f) CoralAlert else MintCharging,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Weekly Activity Bar Chart
    WeeklyActivityChart(sessions = chargingSessions)

    Spacer(modifier = Modifier.height(16.dp))

    // Sub-tab selector (Pill style matching reference screenshot)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        .padding(4.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(if (selectedTab == 0) ZomoPurplePrimary else Color.Transparent)
          .clickable { selectedTab = 0 }
          .padding(vertical = 10.dp)
          .testTag("tab_charging_sessions")
      ) {
        Text(
          text = "Sesi Pengisian (${chargingSessions.size})",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(if (selectedTab == 1) ZomoPurplePrimary else Color.Transparent)
          .clickable { selectedTab = 1 }
          .padding(vertical = 10.dp)
          .testTag("tab_alarm_history")
      ) {
        Text(
          text = "Alarm (${alarmHistories.size})",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // List content
    if (selectedTab == 0) {
      if (chargingSessions.isEmpty()) {
        EmptyHistoryView("Belum ada riwayat pengisian.", "Colokkan pengisi daya untuk mulai mencatat.")
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(chargingSessions, key = { it.id }) { session ->
            ChargingSessionItem(session = session)
          }
        }
      }
    } else {
      if (alarmHistories.isEmpty()) {
        EmptyHistoryView("Belum ada riwayat alarm.", "Alarm yang berbunyi akan tercatat di sini.")
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(alarmHistories, key = { it.id }) { alarm ->
            AlarmHistoryItem(alarm = alarm)
          }
        }
      }
    }
  }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      title = { Text("Hapus Semua Riwayat?", fontWeight = FontWeight.Bold) },
      text = { Text("Semua data sesi pengisian dan riwayat alarm akan dihapus permanen.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearHistory()
            showClearDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }
}

@Composable
fun StatSmallCard(
  title: String,
  value: String,
  unit: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .shadow(1.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x10000000), spotColor = Color(0x147E42F5))
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
      .padding(14.dp)
  ) {
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = value,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = unit,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun WeeklyActivityChart(sessions: List<ChargingSessionEntity>) {
  val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
  val barBgColor = MaterialTheme.colorScheme.surfaceVariant
  val onSurfaceColor = MaterialTheme.colorScheme.onSurface
  val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

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
          Icon(Icons.Default.Timeline, contentDescription = null, tint = ZomoPurplePrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Aktivitas Charging 7 Hari",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor
          )
        }
        Text(
          text = "Mingguan",
          style = MaterialTheme.typography.labelSmall,
          color = ZomoPurplePrimary,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Custom 7-day Bar chart with modern rounded capsules
      val now = System.currentTimeMillis()
      val dayMillis = 24 * 60 * 60 * 1000L
      val counts = IntArray(7) { 0 }

      sessions.forEach { s ->
        val diffDays = ((now - s.startTime) / dayMillis).toInt()
        if (diffDays in 0..6) {
          val dayIndex = (6 - diffDays).coerceIn(0, 6)
          counts[dayIndex]++
        }
      }

      val maxCount = (counts.maxOrNull() ?: 1).coerceAtLeast(1)

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
      ) {
        days.forEachIndexed { i, dayName ->
          val count = counts[i]
          val ratio = count.toFloat() / maxCount.toFloat()

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
          ) {
            Canvas(
              modifier = Modifier
                .width(18.dp)
                .height(90.dp)
            ) {
              val barHeight = (size.height * ratio).coerceAtLeast(8.dp.toPx())
              // Background track
              drawRoundRect(
                color = barBgColor,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
              )
              // Active fill (Gradient)
              drawRoundRect(
                brush = Brush.verticalGradient(
                  colors = listOf(
                    ZomoPurplePrimary,
                    TurquoiseAccent
                  )
                ),
                topLeft = Offset(0f, size.height - barHeight),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = dayName,
              style = MaterialTheme.typography.labelSmall,
              color = onSurfaceVariantColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChargingSessionItem(session: ChargingSessionEntity) {
  val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
  val startDateStr = dateFormat.format(Date(session.startTime))
  val endPercent = session.endLevel ?: session.startLevel

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(1.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x10000000), spotColor = Color(0x147E42F5))
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MintCharging.copy(alpha = 0.15f))
        ) {
          Icon(Icons.Default.Bolt, contentDescription = null, tint = MintCharging, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = "${session.startLevel}% → $endPercent%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$startDateStr • ${session.powerSource}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ZomoPurplePrimary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "+${session.gainedPercentage}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ZomoPurplePrimary
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${session.durationMinutes} mnt",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun AlarmHistoryItem(alarm: AlarmHistoryEntity) {
  val dateFormat = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())
  val dateStr = dateFormat.format(Date(alarm.timestamp))

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(1.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x10000000), spotColor = Color(0x147E42F5))
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ZomoPurplePrimary.copy(alpha = 0.15f))
        ) {
          Icon(Icons.Default.Alarm, contentDescription = null, tint = ZomoPurplePrimary, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = "Alarm Target ${alarm.targetPercentage}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$dateStr • ${alarm.alarmMode}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(MintCharging.copy(alpha = 0.15f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "Baterai ${alarm.batteryLevel}%",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MintCharging
        )
      }
    }
  }
}

@Composable
fun EmptyHistoryView(title: String, subtitle: String) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxWidth()
      .height(180.dp)
      .padding(16.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.History,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(44.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
