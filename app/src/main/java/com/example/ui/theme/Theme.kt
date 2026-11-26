package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = ToastAmberLight,
  onPrimary = Color(0xFF4F2500),
  primaryContainer = Color(0xFF703700),
  onPrimaryContainer = ToastAmberContainer,
  secondary = SlateSecondaryLight,
  onSecondary = Color(0xFF0A2E44),
  secondaryContainer = Color(0xFF23465D),
  onSecondaryContainer = SlateContainer,
  tertiary = EmeraldTertiaryLight,
  onTertiary = Color(0xFF003824),
  tertiaryContainer = Color(0xFF005136),
  onTertiaryContainer = EmeraldContainer,
  background = ObsidianBg,
  onBackground = Color(0xFFE6ECF2),
  surface = ObsidianSurface,
  onSurface = Color(0xFFE6ECF2),
  surfaceVariant = ObsidianSurfaceContainer,
  onSurfaceVariant = Color(0xFFAAB8C5),
  surfaceContainerLow = ObsidianSurface,
  surfaceContainer = ObsidianSurfaceContainer,
  surfaceContainerHigh = ObsidianSurfaceHigh,
  outline = Color(0xFF3B4959),
)

private val LightColorScheme = lightColorScheme(
  primary = ToastAmberPrimary,
  onPrimary = Color.White,
  primaryContainer = ToastAmberContainer,
  onPrimaryContainer = ToastOnAmberContainer,
  secondary = SlateSecondary,
  onSecondary = Color.White,
  secondaryContainer = SlateContainer,
  onSecondaryContainer = SlateOnContainer,
  tertiary = EmeraldTertiary,
  onTertiary = Color.White,
  tertiaryContainer = EmeraldContainer,
  onTertiaryContainer = Color(0xFF002114),
  background = PaperBg,
  onBackground = Color(0xFF121820),
  surface = PaperSurface,
  onSurface = Color(0xFF121820),
  surfaceVariant = PaperSurfaceContainer,
  onSurfaceVariant = Color(0xFF4B5B6D),
  surfaceContainerLow = PaperBg,
  surfaceContainer = PaperSurfaceContainer,
  surfaceContainerHigh = PaperSurfaceHigh,
  outline = Color(0xFFCBD5E1),
)

val ExpressiveShapes = Shapes(
  extraSmall = RoundedCornerShape(10.dp),
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ToastsWifiMapperTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = ExpressiveShapes,
    content = content,
  )
}
