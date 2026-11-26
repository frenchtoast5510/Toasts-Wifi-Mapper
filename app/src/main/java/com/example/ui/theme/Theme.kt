package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ObsidianGlassColorScheme = darkColorScheme(
  primary = ToastAmberPrimary,
  onPrimary = Color(0xFF3A1A00),
  primaryContainer = ToastAmberContainer,
  onPrimaryContainer = ToastOnAmberContainer,
  secondary = CyanAccentSecondary,
  onSecondary = Color(0xFF002A3A),
  secondaryContainer = SlateSecondaryContainer,
  onSecondaryContainer = SlateOnContainer,
  tertiary = EmeraldTertiary,
  onTertiary = Color(0xFF002E1E),
  tertiaryContainer = EmeraldContainer,
  onTertiaryContainer = Color(0xFF9DF7D3),
  background = ObsidianBg,
  onBackground = Color(0xFFEBF1F8),
  surface = ObsidianBg,
  onSurface = Color(0xFFEBF1F8),
  surfaceVariant = ObsidianSurfaceContainer,
  onSurfaceVariant = Color(0xFF9BAAC0),
  surfaceContainerLow = ObsidianSurface,
  surfaceContainer = ObsidianSurfaceContainer,
  surfaceContainerHigh = ObsidianSurfaceHigh,
  surfaceContainerHighest = Color(0xFF28364C),
  outline = Color(0xFF36455C),
  outlineVariant = Color(0xFF222E40),
)

val ExpressiveShapes = Shapes(
  extraSmall = RoundedCornerShape(12.dp),
  small = RoundedCornerShape(16.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun ToastsWifiMapperTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ObsidianGlassColorScheme,
    typography = AppTypography,
    shapes = ExpressiveShapes,
    content = content,
  )
}
