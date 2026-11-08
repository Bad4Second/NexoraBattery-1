package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Reference UI Style: Soft Pastel Neumorphic / Lilac Purple Palette (Zomo File style)
val ZomoPurplePrimary = Color(0xFF7E42F5)
val ZomoPurpleLight = Color(0xFF9D65F6)
val ZomoPurpleGlow = Color(0xFFB57EFA)
val ZomoPurpleDark = Color(0xFF6B28D9)

// Accent Colors from reference
val CyanAccent = Color(0xFF00D2FF)
val TurquoiseAccent = Color(0xFF2DD4BF)
val MintCharging = Color(0xFF10B981)
val PinkAccent = Color(0xFFFF548F)
val AmberWarning = Color(0xFFF59E0B)
val CoralAlert = Color(0xFFFF486A)

// Gradients from reference image
val HeroPurpleGradient = Brush.horizontalGradient(
  colors = listOf(
    Color(0xFF6F35E8),
    Color(0xFF8E4BF8),
    Color(0xFFA867FA)
  )
)

val CyanTurquoiseGradient = Brush.horizontalGradient(
  colors = listOf(
    Color(0xFF00D2FF),
    Color(0xFF06B6D4),
    Color(0xFF2DD4BF)
  )
)

val PastelPinkGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFFFF5B99),
    Color(0xFFFF88B8)
  )
)

val PastelOrangeGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFFFFA439),
    Color(0xFFFFC168)
  )
)

val PastelCyanGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFF00D2FF),
    Color(0xFF67E8F9)
  )
)

val PastelMintGradient = Brush.linearGradient(
  colors = listOf(
    Color(0xFF10B981),
    Color(0xFF34D399)
  )
)

// Light Theme Palette (Soft airy lilac background, pure white cards, high-contrast dark text)
val LightBg = Color(0xFFF5F2FC)
val LightCardSurface = Color(0xFFFFFFFF)
val LightCardInner = Color(0xFFF1ECFA)
val LightCardBorder = Color(0xFFE8E2F5)
val LightTextPrimary = Color(0xFF1E1735)
val LightTextSecondary = Color(0xFF756C8C)
val LightChipActive = Color(0xFF7E42F5)
val LightChipInactive = Color(0xFFEDE8F8)

// Dark Theme Palette (Deep rich obsidian-violet background, sleek elevated violet cards, crisp light text)
val DarkBg = Color(0xFF0C0917)
val DarkCardSurface = Color(0xFF1A142E)
val DarkCardInner = Color(0xFF251E3D)
val DarkCardBorder = Color(0xFF2F2552)
val DarkTextPrimary = Color(0xFFFAF7FF)
val DarkTextSecondary = Color(0xFFA59BC2)
val DarkChipActive = Color(0xFF9E77ED)
val DarkChipInactive = Color(0xFF261F42)

// Legacy alias mappings for backward compatibility
val LavenderPrimary = ZomoPurplePrimary
val LavenderSecondary = ZomoPurpleGlow
val LavenderTertiary = Color(0xFFDDD6FE)
val DeepVioletDark = DarkBg
val DeepVioletSurface = DarkCardSurface
val DeepVioletCard = DarkCardSurface
val DeepVioletBorder = DarkCardBorder
val TextDark = LightTextPrimary
val TextDarkSecondary = LightTextSecondary
val TextLight = DarkTextPrimary
val TextLightSecondary = DarkTextSecondary
