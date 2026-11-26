package com.frenchtoast.wifimapper.domain.repository

import com.frenchtoast.wifimapper.domain.model.WalkTrackPoint
import com.frenchtoast.wifimapper.domain.model.WifiAccessPointSummary
import com.frenchtoast.wifimapper.domain.model.WifiReading
import com.frenchtoast.wifimapper.domain.model.WifiSecurity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ExportFormatter {

  fun toCsv(readings: List<WifiReading>): String {
    val sb = StringBuilder()
    sb.appendLine("bssid,ssid,rssi_dbm,frequency_mhz,channel,channel_width_mhz,band,security,wifi_generation,latitude,longitude,altitude_m,accuracy_m,timestamp_iso")
    val fmt = isoFormatter()
    for (r in readings) {
      sb.append(csvEscape(r.bssid)).append(',')
      sb.append(csvEscape(r.ssid.ifBlank { "Hidden Network" })).append(',')
      sb.append(r.rssiDbm).append(',')
      sb.append(r.frequencyMhz).append(',')
      sb.append(r.primaryChannel).append(',')
      sb.append(r.channelWidthMhz).append(',')
      sb.append(csvEscape(r.band.label)).append(',')
      sb.append(csvEscape(r.security.label)).append(',')
      sb.append(csvEscape(r.wifiGeneration.badge)).append(',')
      sb.append(String.format(Locale.US, "%.7f", r.latitude)).append(',')
      sb.append(String.format(Locale.US, "%.7f", r.longitude)).append(',')
      sb.append(String.format(Locale.US, "%.1f", r.altitudeMeters)).append(',')
      sb.append(String.format(Locale.US, "%.1f", r.accuracyMeters)).append(',')
      sb.append(csvEscape(fmt.format(Date(r.timestampMillis))))
      sb.appendLine()
    }
    return sb.toString()
  }

  fun toWigleCsv(readings: List<WifiReading>, deviceModel: String = "Android"): String {
    val sb = StringBuilder()
    sb.appendLine("WigleWifi-1.6,appRelease=1.0,model=${csvEscape(deviceModel)},release=15,device=ToastsWifiMapper,display=ToastsWifiMapper,board=android,brand=Toasts")
    sb.appendLine("MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type")
    val fmt = wigleDateFormatter()
    for (r in readings) {
      val authMode = when {
        r.capabilitiesRaw.isNotBlank() -> r.capabilitiesRaw
        r.security == WifiSecurity.OPEN -> "[ESS]"
        r.security == WifiSecurity.WPA3 -> "[WPA3-SAE-CCMP][ESS]"
        r.security == WifiSecurity.WPA2_PSK -> "[WPA2-PSK-CCMP][ESS]"
        r.security == WifiSecurity.ENTERPRISE -> "[WPA2-EAP-CCMP][ESS]"
        r.security == WifiSecurity.WEP -> "[WEP][ESS]"
        else -> "[ESS]"
      }
      sb.append(csvEscape(r.bssid)).append(',')
      sb.append(csvEscape(r.ssid)).append(',')
      sb.append(csvEscape(authMode)).append(',')
      sb.append(csvEscape(fmt.format(Date(r.timestampMillis)))).append(',')
      sb.append(r.primaryChannel).append(',')
      sb.append(r.rssiDbm).append(',')
      sb.append(String.format(Locale.US, "%.7f", r.latitude)).append(',')
      sb.append(String.format(Locale.US, "%.7f", r.longitude)).append(',')
      sb.append(String.format(Locale.US, "%.1f", r.altitudeMeters)).append(',')
      sb.append(String.format(Locale.US, "%.1f", r.accuracyMeters)).append(',')
      sb.append("WIFI")
      sb.appendLine()
    }
    return sb.toString()
  }

  fun toGeoJson(
    summaries: List<WifiAccessPointSummary>,
    trackPoints: List<WalkTrackPoint>,
    walkName: String,
  ): String {
    val features = mutableListOf<String>()

    if (trackPoints.size >= 2) {
      val coords = trackPoints.joinToString(",") {
        String.format(Locale.US, "[%.7f,%.7f]", it.longitude, it.latitude)
      }
      features.add(
        """
        {
          "type": "Feature",
          "properties": {
            "feature_type": "walk_track",
            "walk_name": ${jsonString(walkName)},
            "point_count": ${trackPoints.size}
          },
          "geometry": {
            "type": "LineString",
            "coordinates": [$coords]
          }
        }
        """.trimIndent(),
      )
    }

    for (ap in summaries) {
      val coord = ap.strongestCoordinate ?: continue
      features.add(
        """
        {
          "type": "Feature",
          "properties": {
            "feature_type": "access_point",
            "bssid": ${jsonString(ap.bssid)},
            "ssid": ${jsonString(ap.ssid)},
            "manufacturer": ${jsonString(ap.manufacturer)},
            "band": ${jsonString(ap.band.label)},
            "frequency_mhz": ${ap.frequencyMhz},
            "channel": ${ap.primaryChannel},
            "channel_width_mhz": ${ap.channelWidthMhz},
            "wifi_generation": ${jsonString(ap.wifiGeneration.badge)},
            "security": ${jsonString(ap.security.label)},
            "peak_rssi_dbm": ${ap.peakRssiDbm},
            "average_rssi_dbm": ${ap.averageRssiDbm},
            "sightings": ${ap.sightingsCount}
          },
          "geometry": {
            "type": "Point",
            "coordinates": [${String.format(Locale.US, "%.7f", coord.longitude)}, ${String.format(Locale.US, "%.7f", coord.latitude)}]
          }
        }
        """.trimIndent(),
      )
    }

    return """
    {
      "type": "FeatureCollection",
      "name": ${jsonString(walkName)},
      "features": [
        ${features.joinToString(",\n")}
      ]
    }
    """.trimIndent()
  }

  private fun csvEscape(value: String): String {
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
    return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
  }

  private fun jsonString(value: String): String {
    val escaped = value
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
    return "\"$escaped\""
  }

  private fun isoFormatter(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }

  private fun wigleDateFormatter(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
}
