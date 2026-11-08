package com.example

import com.example.data.model.BatteryInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun batteryInfo_calculationsAreAccurate() {
    val info = BatteryInfo(
      level = 85,
      scale = 100,
      percentage = 85,
      isCharging = true,
      voltageMillivolts = 4150,
      temperatureCelsius = 32.5f
    )
    assertEquals(85, info.percentage)
    assertEquals(4.15f, info.voltageVolts, 0.01f)
    assertTrue(info.formattedChargeTimeRemaining.isNotEmpty())
  }

  @Test
  fun batteryInfo_dischargingEstimate() {
    val info = BatteryInfo(
      percentage = 60,
      isCharging = false
    )
    assertTrue(info.formattedDischargeTimeRemaining.contains("tersisa"))
  }
}
