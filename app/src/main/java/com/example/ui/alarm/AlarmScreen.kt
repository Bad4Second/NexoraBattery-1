package com.example.ui.alarm

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AlarmMode
import com.example.service.RingtoneItem
import com.example.ui.BatteryViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.MintCharging
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.TurquoiseAccent
import com.example.ui.theme.ZomoPurplePrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmScreen(
  viewModel: BatteryViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()
  val scrollState = rememberScrollState()

  var ttsInputText by remember(userSettings.ttsTemplate) {
    mutableStateOf(userSettings.ttsTemplate)
  }

  var showRingtoneDialog by remember { mutableStateOf(false) }
  var systemRingtones by remember { mutableStateOf<List<RingtoneItem>>(emptyList()) }
  var previewingUri by remember { mutableStateOf<String?>(null) }

  // Custom Audio File Picker
  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      var displayName: String? = null
      try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
              displayName = cursor.getString(nameIndex)
            }
          }
        }
      } catch (_: Exception) {}

      val fileName = displayName ?: "Audio_Kustom_${System.currentTimeMillis()}.mp3"
      viewModel.saveCustomAudio(uri, fileName)
      Toast.makeText(context, "Nada dering kustom berhasil dipasang: $fileName", Toast.LENGTH_SHORT).show()
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Text(
      text = "Smart Charging Alarm",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "Peringatan otomatis saat baterai mencapai persentase pilihan",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 1. Master Smart Alarm Toggle
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Aktifkan Smart Alarm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Alarm berbunyi otomatis saat charger terpasang & mencapai target.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
          checked = userSettings.smartAlarmEnabled,
          onCheckedChange = { viewModel.toggleSmartAlarm(it) },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ZomoPurplePrimary
          ),
          modifier = Modifier.testTag("smart_alarm_master_switch")
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Target Percentages Selector
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
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = ZomoPurplePrimary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Target Persentase Pengisian",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Pilih satu atau beberapa target. Alarm akan berbunyi di setiap target yang tercapai.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(25, 50, 80, 85, 90, 100).forEach { level ->
            val isSelected = userSettings.targetLevels.contains(level)
            FilterChip(
              selected = isSelected,
              onClick = { viewModel.toggleTargetLevel(level) },
              label = {
                Text(
                  text = "$level%",
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ZomoPurplePrimary,
                selectedLabelColor = Color.White,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              shape = RoundedCornerShape(14.dp),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = if (isSelected) ZomoPurplePrimary else MaterialTheme.colorScheme.outline,
                enabled = true,
                selected = isSelected
              ),
              modifier = Modifier.testTag("target_chip_$level")
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Alarm Mode Selector
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
          Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = null,
            tint = PinkAccent,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Mode Alarm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val modes = listOf(
          AlarmMode.ALARM_AND_VOICE to "Alarm + Suara (TTS)",
          AlarmMode.VOICE_ONLY to "Hanya Suara (TTS)",
          AlarmMode.ALARM_ONLY to "Hanya Ringtone",
          AlarmMode.SILENT to "Senyap (Notifikasi Saja)"
        )

        modes.forEach { (mode, label) ->
          val isSelected = userSettings.alarmMode == mode
          val itemBg = if (isSelected) ZomoPurplePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          val itemBorder = if (isSelected) ZomoPurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(itemBg)
              .border(1.dp, itemBorder, RoundedCornerShape(16.dp))
              .clickable { viewModel.updateAlarmMode(mode) }
              .padding(horizontal = 14.dp, vertical = 11.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) ZomoPurplePrimary else MaterialTheme.colorScheme.onSurface
              )
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ZomoPurplePrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                  Text(
                    text = "Aktif",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZomoPurplePrimary,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4. Ringtone Selection (Android System + Custom Audio Upload)
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
          Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = TurquoiseAccent,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Nada Dering Alarm (Ringtone)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Ringtone Info Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Nada Dering Aktif:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = userSettings.ringtoneTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (userSettings.isCustomRingtone) "File Audio Kustom Pengguna" else "Nada Dering Bawaan / Sistem",
                style = MaterialTheme.typography.bodySmall,
                color = if (userSettings.isCustomRingtone) MintCharging else ZomoPurplePrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(ZomoPurplePrimary.copy(alpha = 0.15f))
                .clickable { viewModel.previewAlarmSound() }
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Putar Nada",
                tint = ZomoPurplePrimary,
                modifier = Modifier.size(24.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions: System Ringtone & Upload Custom File
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // System Ringtones Button
          OutlinedButton(
            onClick = {
              systemRingtones = viewModel.getSystemRingtones()
              showRingtoneDialog = true
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("select_system_ringtone_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZomoPurplePrimary)
          ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Nada Sistem", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }

          // Upload Custom Audio Button
          OutlinedButton(
            onClick = { audioPickerLauncher.launch("audio/*") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("upload_audio_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MintCharging)
          ) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Upload Audio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        if (userSettings.isCustomRingtone || userSettings.ringtoneUri != null) {
          Spacer(modifier = Modifier.height(8.dp))
          TextButton(
            onClick = {
              viewModel.selectRingtone(null, "Nada Alarm Bawaan", false)
            },
            modifier = Modifier.align(Alignment.End)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset ke Nada Bawaan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. Advanced Text-to-Speech Editor
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
          Icon(
            imageVector = Icons.Default.RecordVoiceOver,
            contentDescription = null,
            tint = ZomoPurplePrimary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Kustomisasi Suara (Text-to-Speech)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Gunakan variabel {battery}, {target}, dan {time} untuk pesan dinamis:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Variable insertion chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("{target}", "{battery}", "{time}").forEach { placeholder ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(ZomoPurplePrimary.copy(alpha = 0.12f))
                .border(1.dp, ZomoPurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .clickable {
                  ttsInputText += " $placeholder"
                  viewModel.updateTtsTemplate(ttsInputText)
                }
                .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
              Text(
                text = "+ $placeholder",
                style = MaterialTheme.typography.labelSmall,
                color = ZomoPurplePrimary,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = ttsInputText,
          onValueChange = {
            ttsInputText = it
            viewModel.updateTtsTemplate(it)
          },
          label = { Text("Teks Ucapan") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tts_text_field"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZomoPurplePrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
          ),
          shape = RoundedCornerShape(16.dp),
          maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Speed Slider
        Text(
          text = "Kecepatan Bicara: ${(userSettings.ttsSpeechRate * 100).toInt()}%",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
        )
        Slider(
          value = userSettings.ttsSpeechRate,
          onValueChange = { viewModel.updateTtsVoiceParams(userSettings.ttsPitch, it) },
          valueRange = 0.6f..1.6f,
          colors = SliderDefaults.colors(
            thumbColor = ZomoPurplePrimary,
            activeTrackColor = ZomoPurplePrimary
          ),
          modifier = Modifier.testTag("tts_speed_slider")
        )

        // Pitch Slider
        Text(
          text = "Nada Suara (Pitch): ${(userSettings.ttsPitch * 100).toInt()}%",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
        )
        Slider(
          value = userSettings.ttsPitch,
          onValueChange = { viewModel.updateTtsVoiceParams(it, userSettings.ttsSpeechRate) },
          valueRange = 0.6f..1.6f,
          colors = SliderDefaults.colors(
            thumbColor = TurquoiseAccent,
            activeTrackColor = TurquoiseAccent
          ),
          modifier = Modifier.testTag("tts_pitch_slider")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = { viewModel.previewVoice() },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("preview_voice_button")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ZomoPurplePrimary)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Dengarkan Contoh Suara", color = ZomoPurplePrimary, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 6. Volume & Duration Settings
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
          Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = AmberWarning,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Volume & Durasi Alarm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Volume
        Text(
          text = "Volume Alarm: ${userSettings.alarmVolume}%",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
        )
        Slider(
          value = userSettings.alarmVolume.toFloat(),
          onValueChange = { viewModel.updateAlarmVolume(it.toInt()) },
          valueRange = 10f..100f,
          colors = SliderDefaults.colors(
            thumbColor = AmberWarning,
            activeTrackColor = AmberWarning
          ),
          modifier = Modifier.testTag("alarm_volume_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Duration selector
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Timer, contentDescription = null, tint = ZomoPurplePrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Durasi Berdering:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val durations = listOf(
          10 to "10d",
          30 to "30d",
          60 to "1m",
          300 to "5m",
          -1 to "Terus Berdering"
        )

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          durations.forEach { (sec, label) ->
            val isSelected = userSettings.alarmDurationSeconds == sec
            val pillBg = if (isSelected) AmberWarning else MaterialTheme.colorScheme.surfaceVariant
            val pillBorder = if (isSelected) AmberWarning else MaterialTheme.colorScheme.outline
            val pillText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(pillBg)
                .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                .clickable { viewModel.updateAlarmDuration(sec) }
                .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = pillText,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Snooze duration selector
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Snooze, contentDescription = null, tint = TurquoiseAccent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Interval Tunda (Snooze):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(2, 5, 10, 15).forEach { min ->
            val isSelected = userSettings.snoozeIntervalMinutes == min
            val pillBg = if (isSelected) TurquoiseAccent else MaterialTheme.colorScheme.surfaceVariant
            val pillBorder = if (isSelected) TurquoiseAccent else MaterialTheme.colorScheme.outline
            val pillText = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(pillBg)
                .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                .clickable { viewModel.updateSnoozeInterval(min) }
                .padding(vertical = 7.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$min mnt",
                style = MaterialTheme.typography.labelSmall,
                color = pillText,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
          onClick = { viewModel.previewAlarmSound() },
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("preview_alarm_sound_button"),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning)
        ) {
          Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AmberWarning)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Uji Suara Ringtone (4 Detik)", fontWeight = FontWeight.SemiBold)
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }

  // System Ringtone Chooser Dialog
  if (showRingtoneDialog) {
    AlertDialog(
      onDismissRequest = {
        viewModel.stopAlarm()
        showRingtoneDialog = false
      },
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.MusicNote, contentDescription = null, tint = ZomoPurplePrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Pilih Nada Dering Sistem", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Box(modifier = Modifier.height(320.dp)) {
          if (systemRingtones.isEmpty()) {
            Text("Tidak ditemukan nada dering sistem tambahan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              items(systemRingtones) { item ->
                val isCurrent = userSettings.ringtoneUri == item.uri
                val isPlayingThis = previewingUri == item.uri
                val rowBg = if (isCurrent) ZomoPurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                val rowBorder = if (isCurrent) ZomoPurplePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(rowBg)
                    .border(1.dp, rowBorder, RoundedCornerShape(14.dp))
                    .clickable {
                      viewModel.selectRingtone(item.uri, item.title, false)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.title,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                      color = if (isCurrent) ZomoPurplePrimary else MaterialTheme.colorScheme.onSurface
                    )
                  }

                  // Preview button for this tone
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(36.dp)
                      .clip(CircleShape)
                      .background(if (isPlayingThis) PinkAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
                      .clickable {
                        if (isPlayingThis) {
                          viewModel.stopAlarm()
                          previewingUri = null
                        } else {
                          previewingUri = item.uri
                          viewModel.previewAlarmSound(item.uri)
                        }
                      }
                  ) {
                    Icon(
                      imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                      contentDescription = "Preview",
                      tint = if (isPlayingThis) PinkAccent else ZomoPurplePrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.stopAlarm()
            showRingtoneDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = ZomoPurplePrimary),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text("Selesai", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    )
  }
}
