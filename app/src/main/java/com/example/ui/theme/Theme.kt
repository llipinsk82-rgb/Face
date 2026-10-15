package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = StudioPrimary,
  onPrimary = Color(0xFF082F49),
  primaryContainer = StudioPrimaryContainer,
  onPrimaryContainer = OnStudioPrimaryContainer,
  secondary = StudioSecondary,
  onSecondary = Color(0xFF1E1B4B),
  secondaryContainer = StudioSecondaryContainer,
  onSecondaryContainer = OnStudioSecondaryContainer,
  tertiary = StudioSuccess,
  onTertiary = Color(0xFF022C22),
  tertiaryContainer = StudioSuccessContainer,
  background = DarkBackground,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = DarkBorder,
  outlineVariant = Color(0xFF1E293B),
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
