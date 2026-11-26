package com.frenchtoast.wifimapper.data.demo

import com.frenchtoast.wifimapper.domain.math.SignalMath
import com.frenchtoast.wifimapper.domain.model.IndoorSample
import com.frenchtoast.wifimapper.domain.model.RouterPin
import com.frenchtoast.wifimapper.domain.model.WalkTrackPoint
import com.frenchtoast.wifimapper.domain.model.WifiBand
import com.frenchtoast.wifimapper.domain.model.WifiGeneration
import com.frenchtoast.wifimapper.domain.model.WifiReading
import com.frenchtoast.wifimapper.domain.model.WifiSecurity
import kotlin.math.hypot
import kotlin.math.roundToInt

object DemoDatasetFactory {

  data class DemoApSeed(
    val bssid: String,
    val ssid: String,
    val frequencyMhz: Int,
    val widthMhz: Int,
    val security: WifiSecurity,
    val capabilities: String,
    val generation: WifiGeneration,
    val lat: Double,
    val lon: Double,
    val txDbmOffset: Int = 0,
  )

  private val demoTrackCoords: List<Pair<Double, Double>> = listOf(
    37.75220 to -122.41980,
    37.75255 to -122.41975,
    37.75290 to -122.41970,
    37.75325 to -122.41966,
    37.75360 to -122.41962,
    37.75395 to -122.41958,
    37.75398 to -122.41905,
    37.75402 to -122.41850,
    37.75405 to -122.41795,
    37.75408 to -122.41740,
    37.75410 to -122.41685,
    37.75375 to -122.41680,
    37.75340 to -122.41676,
    37.75305 to -122.41672,
    37.75270 to -122.41668,
    37.75235 to -122.41664,
    37.75230 to -122.41720,
    37.75228 to -122.41775,
    37.75225 to -122.41830,
    37.75223 to -122.41885,
    37.75221 to -122.41935,
    37.75265 to -122.41840,
    37.75310 to -122.41835,
    37.75355 to -122.41830,
  )

  private val demoAps: List<DemoApSeed> = listOf(
    DemoApSeed("24:5A:4C:11:2A:01", "ToastLab-6E", 6115, 160, WifiSecurity.WPA3, "[WPA3-SAE-CCMP][ESS]", WifiGeneration.WIFI_6E, 37.75245, -122.41965, 4),
    DemoApSeed("24:5A:4C:11:2A:02", "ToastLab-5G", 5180, 80, WifiSecurity.WPA3, "[WPA3-SAE-CCMP][ESS]", WifiGeneration.WIFI_6, 37.75248, -122.41962, 5),
    DemoApSeed("24:5A:4C:11:2A:03", "ToastLab-IoT", 2412, 20, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_4, 37.75250, -122.41960, 3),
    DemoApSeed("00:18:0A:42:99:10", "Philz_Guest_WiFi", 2437, 20, WifiSecurity.OPEN, "[ESS]", WifiGeneration.WIFI_5, 37.75318, -122.41964, 4),
    DemoApSeed("00:18:0A:42:99:11", "Philz_Guest_5G", 5220, 40, WifiSecurity.OPEN, "[ESS]", WifiGeneration.WIFI_6, 37.75320, -122.41960, 2),
    DemoApSeed("F4:F5:E8:88:12:4A", "SourdoughMesh", 5745, 80, WifiSecurity.WPA3, "[WPA3-SAE-CCMP][ESS]", WifiGeneration.WIFI_6, 37.75378, -122.41942, 4),
    DemoApSeed("F4:F5:E8:88:12:4B", "SourdoughMesh", 2462, 20, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_6, 37.75380, -122.41940, 2),
    DemoApSeed("34:D2:70:C1:04:88", "MissionBakery-7", 6275, 160, WifiSecurity.WPA3, "[WPA3-SAE-CCMP][ESS]", WifiGeneration.WIFI_7, 37.75401, -122.41875, 5),
    DemoApSeed("34:D2:70:C1:04:89", "MissionBakery-Guest", 2412, 20, WifiSecurity.OPEN, "[ESS]", WifiGeneration.WIFI_6, 37.75402, -122.41870, 2),
    DemoApSeed("E4:9E:12:7B:33:C0", "xfinitywifi", 2437, 20, WifiSecurity.OPEN, "[ESS]", WifiGeneration.WIFI_5, 37.75404, -122.41795, 1),
    DemoApSeed("E4:9E:12:7B:33:C1", "XFSETUP-9A2B", 5240, 80, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_6, 37.75406, -122.41790, -2),
    DemoApSeed("78:D2:94:5E:10:04", "Orbi-Studio-6E", 6435, 160, WifiSecurity.WPA3, "[WPA3-SAE-CCMP][ESS]", WifiGeneration.WIFI_6E, 37.75395, -122.41712, 3),
    DemoApSeed("78:D2:94:5E:10:05", "Orbi-Studio", 5500, 80, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_6, 37.75392, -122.41708, 1),
    DemoApSeed("00:1A:1E:90:44:E2", "BayClinic-Corp", 5180, 40, WifiSecurity.ENTERPRISE, "[WPA2-EAP-CCMP][ESS]", WifiGeneration.WIFI_6, 37.75345, -122.41678, 3),
    DemoApSeed("00:1A:1E:90:44:E3", "BayClinic-Staff", 6195, 80, WifiSecurity.ENTERPRISE, "[WPA3-EAP-CCMP][ESS]", WifiGeneration.WIFI_6E, 37.75342, -122.41675, 1),
    DemoApSeed("50:C7:BF:22:7C:19", "Apt4B_Archer", 2462, 40, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_5, 37.75290, -122.41674, 0),
    DemoApSeed("50:C7:BF:22:7C:1A", "Apt4B_Archer_5G", 5785, 80, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_5, 37.75288, -122.41670, -2),
    DemoApSeed("04:D4:C4:6A:D1:09", "ROG-Rapture-6G", 6595, 160, WifiSecurity.WPA3, "[WPA3-SAE-CCMP][ESS]", WifiGeneration.WIFI_7, 37.75240, -122.41685, 4),
    DemoApSeed("B0:B9:8A:31:55:AA", "Nighthawk_Home", 5200, 80, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_5, 37.75229, -122.41755, 2),
    DemoApSeed("DC:A6:32:0F:E2:11", "PiHole-Telemetry", 2427, 20, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_4, 37.75226, -122.41820, -4),
    DemoApSeed("24:0A:C4:91:08:77", "", 2412, 20, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_4, 37.75224, -122.41880, -3),
    DemoApSeed("00:11:32:AA:4B:19", "Synology-Mesh", 5660, 80, WifiSecurity.WPA3, "[WPA3-SAE-CCMP][ESS]", WifiGeneration.WIFI_6, 37.75272, -122.41838, 3),
    DemoApSeed("A8:66:7F:19:C3:40", "24thSt-Studio", 2437, 20, WifiSecurity.WPA2_PSK, "[WPA2-PSK-CCMP][ESS]", WifiGeneration.WIFI_5, 37.75314, -122.41832, 1),
    DemoApSeed("B4:FB:E4:77:21:90", "Parklet_Public", 2462, 20, WifiSecurity.OPEN, "[ESS]", WifiGeneration.WIFI_6, 37.75355, -122.41832, 4),
    DemoApSeed("B4:FB:E4:77:21:91", "Parklet_Public_5G", 5805, 80, WifiSecurity.OPEN, "[ESS]", WifiGeneration.WIFI_6, 37.75356, -122.41828, 2),
    DemoApSeed("C8:3A:35:04:19:D2", "OldPrintServer", 2447, 20, WifiSecurity.WEP, "[WEP][ESS]", WifiGeneration.LEGACY, 37.75262, -122.41910, -6),
  )

  fun buildOutdoorTrack(walkId: Long, baseTimeMillis: Long): List<WalkTrackPoint> {
    return demoTrackCoords.mapIndexed { idx, (lat, lon) ->
      WalkTrackPoint(
        walkId = walkId,
        latitude = lat,
        longitude = lon,
        accuracyMeters = 3.5f + (idx % 4) * 0.8f,
        timestampMillis = baseTimeMillis + idx * 18_000L,
      )
    }
  }

  fun buildOutdoorReadings(walkId: Long, baseTimeMillis: Long): List<WifiReading> {
    val readings = mutableListOf<WifiReading>()
    demoTrackCoords.forEachIndexed { idx, (tLat, tLon) ->
      val tMillis = baseTimeMillis + idx * 18_000L
      for (ap in demoAps) {
        val dLatMeters = (tLat - ap.lat) * 111_139.0
        val dLonMeters = (tLon - ap.lon) * 88_100.0
        val distMeters = hypot(dLatMeters, dLonMeters)
        val (band, channel) = SignalMath.frequencyToBandAndChannel(ap.frequencyMhz)
        val bandAttenuation = when (band) {
          WifiBand.BAND_2_4_GHZ -> 0.24
          WifiBand.BAND_5_GHZ -> 0.34
          WifiBand.BAND_6_GHZ -> 0.42
          WifiBand.UNKNOWN -> 0.30
        }
        val estimatedRssi = (-36 - distMeters * bandAttenuation + ap.txDbmOffset).roundToInt()
        if (estimatedRssi >= -93) {
          readings.add(
            WifiReading(
              walkId = walkId,
              bssid = ap.bssid,
              ssid = ap.ssid,
              rssiDbm = estimatedRssi.coerceIn(-94, -32),
              frequencyMhz = ap.frequencyMhz,
              primaryChannel = channel,
              channelWidthMhz = ap.widthMhz,
              band = band,
              security = ap.security,
              capabilitiesRaw = ap.capabilities,
              wifiGeneration = ap.generation,
              latitude = tLat,
              longitude = tLon,
              altitudeMeters = 24.5,
              accuracyMeters = 4.0f,
              timestampMillis = tMillis,
            ),
          )
        }
      }
    }
    return readings
  }

  fun buildIndoorRunLivingRoom(runId: Long, baseTimeMillis: Long): Pair<RouterPin, List<IndoorSample>> {
    val routerX = 0.14f
    val routerY = 0.20f
    val pin = RouterPin(
      runId = runId,
      label = "Router (Living Corner)",
      xNorm = routerX,
      yNorm = routerY,
    )
    val sampleSpots = indoorSampleGridPoints()
    val samples = sampleSpots.mapIndexed { idx, (sx, sy) ->
      val dist = hypot((sx - routerX).toDouble(), (sy - routerY).toDouble())
      // Living room corner suffers heavier multi-wall drop reaching Home Office & Primary Bedroom
      val wallPenalty = when {
        sx > 0.62f && sy > 0.44f -> 18.0
        sx < 0.36f && sy > 0.54f -> 14.0
        sx > 0.44f -> 7.0
        else -> 0.0
      }
      val rssi = (-35.0 - dist * 58.0 - wallPenalty).roundToInt().coerceIn(-92, -33)
      IndoorSample(
        runId = runId,
        xNorm = sx,
        yNorm = sy,
        bssid = "24:5A:4C:11:2A:02",
        ssid = "ToastLab-5G",
        rssiDbm = rssi,
        frequencyMhz = 5180,
        primaryChannel = 36,
        channelWidthMhz = 80,
        band = WifiBand.BAND_5_GHZ,
        security = WifiSecurity.WPA3,
        wifiGeneration = WifiGeneration.WIFI_6,
        timestampMillis = baseTimeMillis + idx * 9_000L,
      )
    }
    return pin to samples
  }

  fun buildIndoorRunHallway(runId: Long, baseTimeMillis: Long): Pair<RouterPin, List<IndoorSample>> {
    val routerX = 0.49f
    val routerY = 0.54f
    val pin = RouterPin(
      runId = runId,
      label = "Router (Central Hallway)",
      xNorm = routerX,
      yNorm = routerY,
    )
    val sampleSpots = indoorSampleGridPoints()
    val samples = sampleSpots.mapIndexed { idx, (sx, sy) ->
      val dist = hypot((sx - routerX).toDouble(), (sy - routerY).toDouble())
      val wallPenalty = if (dist > 0.34) 5.0 else 0.0
      val rssi = (-34.0 - dist * 46.0 - wallPenalty).roundToInt().coerceIn(-82, -33)
      IndoorSample(
        runId = runId,
        xNorm = sx,
        yNorm = sy,
        bssid = "24:5A:4C:11:2A:02",
        ssid = "ToastLab-5G",
        rssiDbm = rssi,
        frequencyMhz = 5180,
        primaryChannel = 36,
        channelWidthMhz = 80,
        band = WifiBand.BAND_5_GHZ,
        security = WifiSecurity.WPA3,
        wifiGeneration = WifiGeneration.WIFI_6,
        timestampMillis = baseTimeMillis + idx * 9_000L,
      )
    }
    return pin to samples
  }

  private fun indoorSampleGridPoints(): List<Pair<Float, Float>> = listOf(
    // Living Room
    0.14f to 0.18f, 0.28f to 0.16f, 0.15f to 0.38f, 0.33f to 0.38f, 0.24f to 0.28f,
    // Kitchen & Dining
    0.50f to 0.18f, 0.70f to 0.16f, 0.86f to 0.20f, 0.56f to 0.34f, 0.80f to 0.34f,
    // Central Hallway
    0.42f to 0.52f, 0.50f to 0.54f, 0.59f to 0.54f,
    // Home Office
    0.14f to 0.64f, 0.28f to 0.64f, 0.14f to 0.84f, 0.29f to 0.84f,
    // Guest & Bath
    0.44f to 0.74f, 0.56f to 0.74f, 0.50f to 0.86f,
    // Primary Bedroom
    0.72f to 0.56f, 0.87f to 0.56f, 0.72f to 0.74f, 0.87f to 0.84f, 0.78f to 0.85f,
  )
}
