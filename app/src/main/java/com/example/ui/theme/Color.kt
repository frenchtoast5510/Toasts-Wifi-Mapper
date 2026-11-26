package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.frenchtoast.wifimapper.domain.model.SignalTier

val ToastAmberPrimary = Color(0xFFFF9B42)
val ToastAmberContainer = Color(0xFF422306)
val ToastOnAmberContainer = Color(0xFFFFD8B8)

val CyanAccentSecondary = Color(0xFF56CCF2)
val SlateSecondaryContainer = Color(0xFF172C42)
val SlateOnContainer = Color(0xFFCDE8FF)

val EmeraldTertiary = Color(0xFF10B981)
val EmeraldContainer = Color(0xFF063B2A)

val ObsidianBg = Color(0xFF0B0F17)
val ObsidianSurface = Color(0xFF101622)
val ObsidianSurfaceContainer = Color(0xFF161F2E)
val ObsidianSurfaceHigh = Color(0xFF1F2B3E)
val GlassBorderColor = Color(0x1FFFFFFF)

object SignalColors {
  val StrongGreen = Color(0xFF10B981)
  val FairAmber = Color(0xFFF59E0B)
  val WeakCrimson = Color(0xFFE11D48)
  val WeakViolet = Color(0xFF9333EA)

  val Band24 = Color(0xFF38BDF8)
  val Band5 = Color(0xFF10B981)
  val Band6 = Color(0xFFC084FC)

  fun forRssi(rssiDbm: Int): Color = when {
    rssiDbm >= -60 -> StrongGreen
    rssiDbm >= -75 -> FairAmber
    rssiDbm >= -85 -> WeakCrimson
    else -> WeakViolet
  }

  fun forTier(tier: SignalTier): Color = when (tier) {
    SignalTier.STRONG -> StrongGreen
    SignalTier.FAIR -> FairAmber
    SignalTier.WEAK -> WeakCrimson
  }

  fun plainQualityLabel(rssiDbm: Int): String = when {
    rssiDbm >= -60 -> "Great"
    rssiDbm >= -75 -> "Okay"
    else -> "Weak"
  }

  fun heatmapArgb(rssiDbm: Float, alpha: Int = 155): Int {
    val clamped = rssiDbm.coerceIn(-95f, -30f)
    val t = (clamped + 95f) / 65f
    val r: Int
    val g: Int
    val b: Int
    when {
      t >= 0.72f -> {
        r = 16
        g = 195
        b = 136
      }
      t >= 0.54f -> {
        val k = (t - 0.54f) / 0.18f
        r = (140 - k * 124).toInt()
        g = (212 - k * 17).toInt()
        b = (28 + k * 108).toInt()
      }
      t >= 0.31f -> {
        val k = (t - 0.31f) / 0.23f
        r = (249 - k * 12).toInt()
        g = (115 + k * 88).toInt()
        b = 24
      }
      else -> {
        val k = t / 0.31f
        r = (147 + k * 88).toInt()
        g = (35 + k * 38).toInt()
        b = (212 - k * 132).toInt()
      }
    }
    return android.graphics.Color.argb(
      alpha.coerceIn(0, 255),
      r.coerceIn(0, 255),
      g.coerceIn(0, 255),
      b.coerceIn(0, 255),
    )
  }
}
