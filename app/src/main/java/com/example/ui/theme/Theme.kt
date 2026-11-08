package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = ZomoPurpleLight,
  onPrimary = Color.White,
  primaryContainer = DarkCardInner,
  onPrimaryContainer = Color(0xFFDDD6FE),
  secondary = CyanAccent,
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF1E3A4B),
  onSecondaryContainer = Color(0xFF67E8F9),
  tertiary = PinkAccent,
  onTertiary = Color.White,
  background = DarkBg,
  onBackground = DarkTextPrimary,
  surface = DarkCardSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkCardInner,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkCardBorder,
  outlineVariant = DarkCardBorder.copy(alpha = 0.6f),
  error = CoralAlert,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = ZomoPurplePrimary,
  onPrimary = Color.White,
  primaryContainer = LightCardInner,
  onPrimaryContainer = Color(0xFF3F198A),
  secondary = Color(0xFF0284C7),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0F2FE),
  onSecondaryContainer = Color(0xFF0369A1),
  tertiary = PinkAccent,
  onTertiary = Color.White,
  background = LightBg,
  onBackground = LightTextPrimary,
  surface = LightCardSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightCardInner,
  onSurfaceVariant = LightTextSecondary,
  outline = LightCardBorder,
  outlineVariant = LightCardBorder.copy(alpha = 0.6f),
  error = CoralAlert,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
