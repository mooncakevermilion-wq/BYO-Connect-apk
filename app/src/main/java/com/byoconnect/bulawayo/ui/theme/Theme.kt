package com.byoconnect.bulawayo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
  primary = CyanAccent,
  onPrimary = Black,
  primaryContainer = CyanDark,
  onPrimaryContainer = TextPrimary,
  secondary = CyanGlow,
  onSecondary = Black,
  background = Black,
  onBackground = TextPrimary,
  surface = SurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceCard,
  onSurfaceVariant = TextSecondary,
  outline = BorderDark,
  error = ErrorRed
)

@Composable
fun ByoConnectTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  // BYO Connect uses a sleek dark/cyan aesthetic by design
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
