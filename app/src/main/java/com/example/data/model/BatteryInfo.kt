package com.example.data.model

data class BatteryInfo(
  val level: Int = 0,
  val scale: Int = 100,
  val percentage: Int = 0,
  val isCharging: Boolean = false,
  val status: String = "Unknown",
  val health: String = "Good",
  val temperatureCelsius: Float = 25.0f,
  val voltageMillivolts: Int = 3800,
  val pluggedSource: String = "Unplugged",
  val technology: String = "Li-ion",
  val chargeTimeRemainingMs: Long = -1L,
  val dischargeTimeRemainingMs: Long = -1L,
  val lastUpdated: Long = System.currentTimeMillis()
) {
  val voltageVolts: Float get() = voltageMillivolts / 1000f

  val formattedChargeTimeRemaining: String
    get() {
      if (chargeTimeRemainingMs <= 0) {
        // If not provided directly by API, estimate based on remaining percentage
        val remainingPercent = 100 - percentage
        if (!isCharging || remainingPercent <= 0) return "Baterai Penuh"
        val estimatedMinutes = (remainingPercent * 1.5).toLong() // ~90 min for 0-100%
        val h = estimatedMinutes / 60
        val m = estimatedMinutes % 60
        return if (h > 0) "~${h}j ${m}m hingga penuh" else "~${m}m hingga penuh"
      }
      val minutes = chargeTimeRemainingMs / (1000 * 60)
      val h = minutes / 60
      val m = minutes % 60
      return if (h > 0) "${h}j ${m}m hingga penuh" else "${m}m hingga penuh"
    }

  val formattedDischargeTimeRemaining: String
    get() {
      if (isCharging) return "Sedang Mengisi Daya"
      // Estimate ~12-15 hours full discharge
      val estimatedMinutes = (percentage * 8.5).toLong()
      val h = estimatedMinutes / 60
      val m = estimatedMinutes % 60
      return if (h > 0) "~${h}j ${m}m tersisa" else "~${m}m tersisa"
    }
}
