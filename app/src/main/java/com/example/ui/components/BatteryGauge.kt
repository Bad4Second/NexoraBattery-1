package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MintCharging
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.TurquoiseAccent
import com.example.ui.theme.ZomoPurplePrimary
import kotlin.math.sin

@Composable
fun BatteryGauge(
  percentage: Int,
  isCharging: Boolean,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "battery_anim")

  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 2f * Math.PI.toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(2400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wavePhase"
  )

  val pulseGlow by infiniteTransition.animateFloat(
    initialValue = 0.25f,
    targetValue = 0.75f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseGlow"
  )

  val primaryColor = when {
    isCharging -> MintCharging
    percentage <= 20 -> PinkAccent
    else -> TurquoiseAccent
  }

  val secondaryColor = when {
    isCharging -> TurquoiseAccent
    percentage <= 20 -> Color(0xFFFF486A)
    else -> ZomoPurplePrimary
  }

  val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
  val innerBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  val onSurfaceColor = MaterialTheme.colorScheme.onSurface
  val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(240.dp)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokeWidth = 14.dp.toPx()
      val diameter = size.minDimension - strokeWidth * 2
      val radius = diameter / 2
      val center = Offset(size.width / 2, size.height / 2)

      // Background Track
      drawCircle(
        color = trackColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
      )

      // Glowing Outer Ring
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = pulseGlow * 0.2f),
            Color.Transparent
          ),
          center = center,
          radius = radius + 28.dp.toPx()
        ),
        radius = radius + 18.dp.toPx(),
        center = center
      )

      // Progress Arc
      val sweepAngle = (percentage / 100f).coerceIn(0f, 1f) * 360f
      drawArc(
        brush = Brush.sweepGradient(
          listOf(secondaryColor, primaryColor, secondaryColor)
        ),
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(diameter, diameter),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // Inner Liquid Wave
      val innerRadius = radius - strokeWidth * 1.2f
      val clipPath = Path().apply {
        addOval(
          androidx.compose.ui.geometry.Rect(
            center.x - innerRadius,
            center.y - innerRadius,
            center.x + innerRadius,
            center.y + innerRadius
          )
        )
      }

      val fillHeight = (percentage / 100f).coerceIn(0f, 1f)
      val waterLevelY = (center.y + innerRadius) - (innerRadius * 2 * fillHeight)

      val wavePath = Path().apply {
        moveTo(center.x - innerRadius, center.y + innerRadius)
        lineTo(center.x - innerRadius, waterLevelY)

        val waveWidth = innerRadius * 2
        var x = center.x - innerRadius
        while (x <= center.x + innerRadius) {
          val relativeX = (x - (center.x - innerRadius)) / waveWidth
          val y = waterLevelY + sin(relativeX * 2 * Math.PI + wavePhase).toFloat() * 6.dp.toPx()
          lineTo(x, y)
          x += 4.dp.toPx()
        }

        lineTo(center.x + innerRadius, center.y + innerRadius)
        close()
      }

      // Draw clipped wave
      drawContext.canvas.save()
      drawContext.canvas.clipPath(clipPath)

      // Translucent inner container background
      drawCircle(
        color = innerBgColor,
        radius = innerRadius,
        center = center
      )

      // Wave fill
      drawPath(
        path = wavePath,
        brush = Brush.verticalGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.35f),
            secondaryColor.copy(alpha = 0.65f)
          ),
          startY = waterLevelY - 10.dp.toPx(),
          endY = center.y + innerRadius
        )
      )

      drawContext.canvas.restore()
    }

    // Centered Content (Percentage, Bolt, Subtitle) adapting cleanly to theme
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (isCharging) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "Sedang Mengisi",
            tint = MintCharging,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = "CHARGING",
            style = MaterialTheme.typography.labelSmall,
            color = MintCharging,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
          )
        }
      } else {
        Text(
          text = "BATTERY",
          style = MaterialTheme.typography.labelSmall,
          color = onSurfaceVariantColor,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = "$percentage%",
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.ExtraBold,
        color = onSurfaceColor
      )

      Text(
        text = if (isCharging) "Daya Terhubung" else "Discharging",
        style = MaterialTheme.typography.bodySmall,
        color = onSurfaceVariantColor,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
