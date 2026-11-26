package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.frenchtoast.wifimapper.domain.model.SignalTier

// RF Cartography & Toasted Amber brand palette
val ToastAmberPrimary = Color(0xFFD96B18)
val ToastAmberLight = Color(0xFFFFB77C)
val ToastAmberContainer = Color(0xFFFFDCC2)
val ToastOnAmberContainer = Color(0xFF331400)

val SlateSecondary = Color(0xFF3F5E75)
val SlateSecondaryLight = Color(0xFFA7CBE5)
val SlateContainer = Color(0xFFD1E5F6)
val SlateOnContainer = Color(0xFF0A1E2D)

val EmeraldTertiary = Color(0xFF0E9F6E)
val EmeraldTertiaryLight = Color(0xFF6CE5B5)
val EmeraldContainer = Color(0xFFC4F7DF)

val ObsidianBg = Color(0xFF0E141B)
val ObsidianSurface = Color(0xFF131B24)
val ObsidianSurfaceContainer = Color(0xFF1A2430)
val ObsidianSurfaceHigh = Color(0xFF222E3D)

val PaperBg = Color(0xFFF6F8FB)
val PaperSurface = Color(0xFFFFFFFF)
val PaperSurfaceContainer = Color(0xFFEDF1F7)
val PaperSurfaceHigh = Color(0xFFE3E9F2)

object SignalColors {
  val StrongGreen = Color(0xFF10B981)
  val FairAmber = Color(0xFFF59E0B)
  val WeakCrimson = Color(0xFFE11D48)
  val WeakViolet = Color(0xFF9333EA)

  val Band24 = Color(0xFF3B82F6)
  val Band5 = Color(0xFF10B981)
  val Band6 = Color(0xFFA855F7)

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

  fun heatmapArgb(rssiDbm: Float, alpha: Int = 155): Int {
    val clamped = rssiDbm.coerceIn(-95f, -30f)
    val t = (clamped + 95f) / 65f
    val r: Int
    val g: Int
    val b: Int
    when {
      t >= 0.72f -> { // -48 to -30: deep emerald
        r = 16
        g = 185
        b = 129
      }
      t >= 0.54f -> { // -60 to -49: bright green-lime blend
        val k = (t - 0.54f) / 0.18f
        r = (132 - k * 116).toInt()
        g = (204 - k * 19).toInt()
        b = (22 + k * 107).toInt()
      }
      t >= 0.31f -> { // -75 to -61: yellow to amber-orange
        val k = (t - 0.31f) / 0.23f
        r = (249 - k * 14).toInt()
        g = (115 + k * 85).toInt()
        b = (22).toInt()
      }
      else -> { // -95 to -76: violet to crimson
        val k = t / 0.31f
        r = (147 + k * 90).toInt()
        g = (35 + k * 35).toInt()
        b = (210 - k * 130).toInt()
      }
    }
    return android.graphics.Color.argb(alpha, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
  }
}
