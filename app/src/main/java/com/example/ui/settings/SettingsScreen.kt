package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BatteryViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.MintCharging
import com.example.ui.theme.ZomoPurplePrimary
import java.util.Locale

@Composable
fun SettingsScreen(
  viewModel: BatteryViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()
  val scrollState = rememberScrollState()

  val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
  val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
  } else true

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Text(
      text = "Pengaturan",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Konfigurasi keamanan baterai, tema, dan optimasi",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 1. High Temperature Alert Settings
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
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Thermostat, contentDescription = null, tint = CoralAlert, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Peringatan Suhu Panas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Bunyikan alarm saat suhu melampaui batas aman.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = userSettings.tempWarningEnabled,
            onCheckedChange = { viewModel.updateTempWarning(it, userSettings.tempWarningThreshold) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = CoralAlert
            ),
            modifier = Modifier.testTag("temp_warning_switch")
          )
        }

        if (userSettings.tempWarningEnabled) {
          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Batas Suhu Maksimal: ${String.format(Locale.US, "%.0f", userSettings.tempWarningThreshold)}°C",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
          )

          Slider(
            value = userSettings.tempWarningThreshold,
            onValueChange = { viewModel.updateTempWarning(true, it) },
            valueRange = 38f..50f,
            steps = 11,
            colors = SliderDefaults.colors(
              thumbColor = CoralAlert,
              activeTrackColor = CoralAlert
            ),
            modifier = Modifier.testTag("temp_threshold_slider")
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Long Charging Reminder
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
          Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Pengingat Charger Terlalu Lama",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Beri peringatan jika charger tetap terhubung lebih dari 1 jam setelah baterai 100%.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Switch(
          checked = userSettings.longPluggedReminderEnabled,
          onCheckedChange = { viewModel.updateLongPluggedReminder(it) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AmberWarning
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Theme Selector (Pill style matching reference screenshot)
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
          Icon(Icons.Default.DarkMode, contentDescription = null, tint = ZomoPurplePrimary, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Tema Tampilan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("SYSTEM" to "Sistem", "DARK" to "Gelap", "LIGHT" to "Terang").forEach { (mode, label) ->
            val isSelected = userSettings.themeMode == mode
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) ZomoPurplePrimary else Color.Transparent)
                .clickable { viewModel.updateThemeMode(mode) }
                .padding(vertical = 10.dp)
                .testTag("theme_button_$mode")
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Background & Battery Optimization
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
          Icon(Icons.Default.Security, contentDescription = null, tint = ZomoPurplePrimary, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Izin & Ketahanan Background",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = if (isIgnoringBatteryOptimizations)
            "Aplikasi tidak dibatasi oleh penghemat baterai sistem. Alarm dapat berbunyi tepat waktu saat tidur."
          else
            "Sistem mungkin menghentikan aplikasi di background jika optimasi baterai belum diabaikan.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (!isIgnoringBatteryOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          Button(
            onClick = {
              try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                  data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
              } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ZomoPurplePrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("optimize_battery_permission_button")
          ) {
            Text("Nonaktifkan Batasan Baterai untuk App Ini", color = Color.White, fontWeight = FontWeight.Bold)
          }
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MintCharging, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Optimasi baterai telah disesuaikan", style = MaterialTheme.typography.labelSmall, color = MintCharging, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 5. Battery Health Tips
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
          Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Tips Menjaga Kesehatan Baterai",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        listOf(
          "Aturan 20-80%: Jangan biarkan baterai sering drop di bawah 20% atau diisi hingga 100% terlalu lama untuk memperpanjang siklus hidup baterai.",
          "Hindari Panas Berlebih: Jangan mengecas ponsel di bawah bantal saat tidur. Suhu tinggi mempercepat degradasi sel baterai.",
          "Gunakan Charger Asli: Charger dan kabel original menjaga stabilitas tegangan (voltage) saat pengisian cepat."
        ).forEachIndexed { idx, tip ->
          Text(
            text = "${idx + 1}. $tip",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.padding(vertical = 4.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
