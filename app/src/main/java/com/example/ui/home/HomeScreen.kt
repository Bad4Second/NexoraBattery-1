package com.example.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BatteryViewModel
import com.example.ui.components.MetricCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanTurquoiseGradient
import com.example.ui.theme.HeroPurpleGradient
import com.example.ui.theme.MintCharging
import com.example.ui.theme.PastelCyanGradient
import com.example.ui.theme.PastelMintGradient
import com.example.ui.theme.PastelOrangeGradient
import com.example.ui.theme.PastelPinkGradient
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.TurquoiseAccent
import com.example.ui.theme.ZomoPurplePrimary

@Composable
fun HomeScreen(
  viewModel: BatteryViewModel,
  onNavigateToAlarm: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val batteryInfo by viewModel.batteryState.collectAsState()
  val isServiceRunning by viewModel.isServiceRunning.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()
  val isAlarmActive by com.example.BatteryGuardianApp.instance.alarmController.isAlarmActive.collectAsState()
  val scrollState = rememberScrollState()

  val animatedProgress by animateFloatAsState(
    targetValue = (batteryInfo.percentage / 100f).coerceIn(0.04f, 1f),
    animationSpec = tween(durationMillis = 800),
    label = "batteryProgress"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Bar (1:1 with reference screenshot: left squircle, title, right squircle)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp, bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left Squircle Action (Grid / Apps icon)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(44.dp)
          .shadow(
            elevation = 2.dp,
            shape = RoundedCornerShape(14.dp),
            ambientColor = Color(0x10000000),
            spotColor = Color(0x187E42F5)
          )
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
      ) {
        Icon(
          imageVector = Icons.Default.GridView,
          contentDescription = "Menu",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(20.dp)
        )
      }

      // Screen Title (Zomo File style centered title)
      Text(
        text = "Battery Guardian",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        letterSpacing = (-0.3).sp
      )

      // Right Squircle Action (Shield / Service Toggle)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(44.dp)
          .shadow(
            elevation = 2.dp,
            shape = RoundedCornerShape(14.dp),
            ambientColor = Color(0x10000000),
            spotColor = Color(0x187E42F5)
          )
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
          .clickable {
            if (isServiceRunning) viewModel.stopMonitoring() else viewModel.startMonitoring()
          }
      ) {
        Icon(
          imageVector = if (isServiceRunning) Icons.Default.Shield else Icons.Default.NotificationsActive,
          contentDescription = "Status",
          tint = if (isServiceRunning) MintCharging else ZomoPurplePrimary,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // =========================================================================
    // Signature Purple Hero Card (1:1 with "Cloude Storage" in reference image)
    // =========================================================================
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 8.dp,
          shape = RoundedCornerShape(32.dp),
          ambientColor = Color(0x286F35E8),
          spotColor = Color(0x408E4BF8)
        )
        .clip(RoundedCornerShape(32.dp))
        .background(HeroPurpleGradient)
        .padding(22.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Top row: 3D glowing badge + title & subtitle
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          // 3D Glassmorphic Icon Badge (matching folder with star in reference)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(54.dp)
              .clip(RoundedCornerShape(18.dp))
              .background(
                Brush.linearGradient(
                  listOf(
                    Color.White.copy(alpha = 0.32f),
                    Color.White.copy(alpha = 0.12f)
                  )
                )
              )
              .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
          ) {
            Icon(
              imageVector = if (batteryInfo.isCharging) Icons.Default.Power else Icons.Default.ElectricBolt,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (batteryInfo.isCharging) "Pengisian Daya" else "Daya Baterai",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (batteryInfo.isCharging) {
                "${batteryInfo.pluggedSource} • ${batteryInfo.status}"
              } else {
                "Mode Mandiri • ${batteryInfo.status}"
              },
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFE5D5FF),
              fontWeight = FontWeight.Medium
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Capacity and Time Remaining (matching "25.4 GB of 64 GB used" in reference)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = "${batteryInfo.percentage}% Terisi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 17.sp
          )

          Text(
            text = if (batteryInfo.isCharging) {
              "Selesai: ${batteryInfo.formattedChargeTimeRemaining}"
            } else {
              "Sisa: ${batteryInfo.formattedDischargeTimeRemaining}"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE5D5FF)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Signature Cyan-to-Mint Capsule Progress Bar with Thumb Circle (1:1 with reference)
        BoxWithConstraints(
          modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          val totalWidth = maxWidth

          // Outer Track
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(16.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x35000000))
          )

          // Active Gradient Progress
          Box(
            modifier = Modifier
              .width(totalWidth * animatedProgress)
              .height(16.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(CyanTurquoiseGradient)
          )

          // Glowing Thumb Circle (exact thumb from reference image at progress end)
          val thumbOffset = ((totalWidth - 24.dp) * animatedProgress).coerceAtLeast(0.dp)
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .offset(x = thumbOffset)
              .size(24.dp)
              .shadow(4.dp, CircleShape, ambientColor = Color(0x30000000), spotColor = Color(0xFF00D2FF))
              .clip(CircleShape)
              .background(Color.White)
              .border(2.5.dp, TurquoiseAccent, CircleShape)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TurquoiseAccent)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Quick Pill Badges
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            "Suhu ${batteryInfo.temperatureCelsius}°C",
            "${batteryInfo.voltageVolts} V",
            batteryInfo.health
          ).forEach { tag ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // =========================================================================
    // 2x2 Grid of Square Cards (Picture, Video, My File, Apps aesthetic)
    // =========================================================================
    val tempColor = when {
      batteryInfo.temperatureCelsius >= 42f -> CoralAlert
      batteryInfo.temperatureCelsius >= 37f -> AmberWarning
      else -> TurquoiseAccent
    }
    val tempStatus = when {
      batteryInfo.temperatureCelsius >= 42f -> "Panas!"
      batteryInfo.temperatureCelsius >= 37f -> "Hangat"
      else -> "Sejuk & Aman"
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. Suhu Baterai (Top-Left, matching "Picture" with Pink Pastel Badge)
      MetricCard(
        title = "Suhu Baterai",
        value = "${batteryInfo.temperatureCelsius}°C",
        subtitle = tempStatus,
        icon = Icons.Default.Thermostat,
        badgeGradient = PastelPinkGradient,
        accentColor = PinkAccent,
        modifier = Modifier.weight(1f)
      )

      // 2. Tegangan (Top-Right, matching "Video" with Orange Pastel Badge)
      MetricCard(
        title = "Tegangan",
        value = "${batteryInfo.voltageVolts} V",
        subtitle = "${batteryInfo.voltageMillivolts} mV",
        icon = Icons.Default.ElectricBolt,
        badgeGradient = PastelOrangeGradient,
        accentColor = AmberWarning,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 3. Kondisi Baterai (Bottom-Left, matching "My File" with Cyan Pastel Badge)
      MetricCard(
        title = "Kondisi",
        value = batteryInfo.health,
        subtitle = batteryInfo.technology,
        icon = Icons.Default.Favorite,
        badgeGradient = PastelCyanGradient,
        accentColor = TurquoiseAccent,
        modifier = Modifier.weight(1f)
      )

      // 4. Sumber Daya (Bottom-Right, matching "Apps" with Mint Pastel Badge)
      MetricCard(
        title = "Sumber Daya",
        value = if (batteryInfo.isCharging) batteryInfo.pluggedSource else "Baterai",
        subtitle = batteryInfo.status,
        icon = Icons.Default.Power,
        badgeGradient = PastelMintGradient,
        accentColor = MintCharging,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(22.dp))

    // =========================================================================
    // "Target Alarm Baterai" Section (1:1 with "Recents Files" with "View All")
    // =========================================================================
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 2.dp,
          shape = RoundedCornerShape(26.dp),
          ambientColor = Color(0x10000000),
          spotColor = Color(0x147E42F5)
        )
        .clip(RoundedCornerShape(26.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
        .padding(18.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = ZomoPurplePrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Target Alarm Baterai",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = "Atur Semua",
            style = MaterialTheme.typography.labelMedium,
            color = ZomoPurplePrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clickable { onNavigateToAlarm() }
              .padding(4.dp)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Alarm berdering saat charger terhubung & mencapai target:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pill Capsule Chips (25%, 50%, 80%, 85%, 90%, 100%)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          listOf(25, 50, 80, 85, 90, 100).forEach { target ->
            val isSelected = userSettings.targetLevels.contains(target)
            val chipBg = if (isSelected) ZomoPurplePrimary else MaterialTheme.colorScheme.surfaceVariant
            val chipBorder = if (isSelected) ZomoPurplePrimary else MaterialTheme.colorScheme.outline
            val chipTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(chipBg)
                .border(1.dp, chipBorder, RoundedCornerShape(14.dp))
                .clickable { viewModel.toggleTargetLevel(target) }
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .testTag("target_chip_$target")
            ) {
              Text(
                text = "$target%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = chipTextColor
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // =========================================================================
    // Smart Service Switch Card
    // =========================================================================
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 2.dp,
          shape = RoundedCornerShape(26.dp),
          ambientColor = Color(0x10000000),
          spotColor = Color(0x147E42F5)
        )
        .clip(RoundedCornerShape(26.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Layanan Pemantau Pintar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Tetap siaga di latar belakang agar alarm berbunyi saat layar mati.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
          checked = isServiceRunning,
          onCheckedChange = { checked ->
            if (checked) viewModel.startMonitoring() else viewModel.stopMonitoring()
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ZomoPurplePrimary
          ),
          modifier = Modifier.testTag("service_toggle_switch")
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // =========================================================================
    // Action Buttons (Gradient Pill "Uji Alarm" & Outlined Pill "Set Baterai OS")
    // =========================================================================
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = {
          if (isAlarmActive) {
            viewModel.stopAlarm()
          } else {
            viewModel.triggerTestAlarm()
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isAlarmActive) CoralAlert else ZomoPurplePrimary
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
          .testTag("test_alarm_button")
      ) {
        Icon(
          imageVector = if (isAlarmActive) Icons.Default.Stop else Icons.Default.PlayArrow,
          contentDescription = null,
          tint = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isAlarmActive) "Hentikan Alarm" else "Uji Alarm",
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      OutlinedButton(
        onClick = {
          try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            context.startActivity(intent)
          } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
          }
        },
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
          .testTag("battery_settings_button"),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = ZomoPurplePrimary
        )
      ) {
        Icon(Icons.Default.Tune, contentDescription = null, tint = ZomoPurplePrimary)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Set Baterai OS", fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
