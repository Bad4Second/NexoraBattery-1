package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.alarm.AlarmScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.sleep.SleepScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ZomoPurplePrimary

enum class NavTab(val title: String) {
  HOME("Home"),
  ALARM("Alarm"),
  HISTORY("Riwayat"),
  SLEEP("Sleep"),
  SETTINGS("Setelan")
}

@Composable
fun MainScreen(viewModel: BatteryViewModel) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()
  var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }

  // Handle system back navigation between tabs
  BackHandler(enabled = currentTabIndex != 0) {
    currentTabIndex = 0
  }

  // Request notification permission if needed on Android 13+
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { _ ->
    // Handled gracefully
  }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    // Start foreground monitoring if enabled
    if (userSettings.smartAlarmEnabled) {
      viewModel.startMonitoring()
    }
  }

  val darkTheme = when (userSettings.themeMode) {
    "DARK" -> true
    "LIGHT" -> false
    else -> isSystemInDarkTheme()
  }

  MyApplicationTheme(darkTheme = darkTheme) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 6.dp,
          modifier = Modifier
            .shadow(
              elevation = 8.dp,
              shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
              ambientColor = Color(0x10000000),
              spotColor = Color(0x1A7E42F5)
            )
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .border(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
              RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
        ) {
          val items = listOf(
            NavTab.HOME to (Icons.Filled.Home to Icons.Outlined.Home),
            NavTab.ALARM to (Icons.Filled.Alarm to Icons.Outlined.Alarm),
            NavTab.HISTORY to (Icons.Filled.History to Icons.Outlined.History),
            NavTab.SLEEP to (Icons.Filled.Bedtime to Icons.Outlined.Bedtime),
            NavTab.SETTINGS to (Icons.Filled.Settings to Icons.Outlined.Settings)
          )

          items.forEachIndexed { index, (tab, icons) ->
            val isSelected = currentTabIndex == index
            NavigationBarItem(
              selected = isSelected,
              onClick = { currentTabIndex = index },
              icon = {
                Icon(
                  imageVector = if (isSelected) icons.first else icons.second,
                  contentDescription = tab.title,
                  modifier = Modifier.size(24.dp)
                )
              },
              label = {
                Text(
                  text = tab.title,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 11.sp
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ZomoPurplePrimary,
                selectedTextColor = ZomoPurplePrimary,
                indicatorColor = ZomoPurplePrimary.copy(alpha = 0.15f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
      ) {
        when (currentTabIndex) {
          0 -> HomeScreen(
            viewModel = viewModel,
            onNavigateToAlarm = { currentTabIndex = 1 }
          )
          1 -> AlarmScreen(viewModel = viewModel)
          2 -> HistoryScreen(viewModel = viewModel)
          3 -> SleepScreen(viewModel = viewModel)
          4 -> SettingsScreen(viewModel = viewModel)
        }
      }
    }
  }
}
