package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = StudioCyan,
  onPrimary = Color(0xFF001F24),
  primaryContainer = StudioCyanContainer,
  onPrimaryContainer = OnStudioCyanContainer,
  secondary = StudioViolet,
  onSecondary = Color(0xFF1D0052),
  secondaryContainer = StudioVioletContainer,
  onSecondaryContainer = OnStudioVioletContainer,
  tertiary = StudioMint,
  onTertiary = Color(0xFF00210A),
  tertiaryContainer = StudioMintContainer,
  background = DarkBackground,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = DarkBorder,
  error = ErrorRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
