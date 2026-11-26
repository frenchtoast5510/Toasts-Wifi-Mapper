package com.frenchtoast.wifimapper.domain.model

enum class WifiBand(val label: String, val shortLabel: String) {
  BAND_2_4_GHZ("2.4 GHz", "2.4G"),
  BAND_5_GHZ("5 GHz", "5G"),
  BAND_6_GHZ("6 GHz", "6G"),
  UNKNOWN("Unknown", "?"),
}

enum class BandFilter(val label: String) {
  ALL("All Bands"),
  BAND_2_4("2.4 GHz"),
  BAND_5("5 GHz"),
  BAND_6("6 GHz"),
}

enum class WifiSecurity(val label: String) {
  OPEN("Open"),
  WEP("WEP"),
  WPA2_PSK("WPA2-PSK"),
  WPA3("WPA3"),
  ENTERPRISE("Enterprise"),
  UNKNOWN("Unknown"),
}

enum class SecurityFilter(val label: String) {
  ALL("All Security"),
  OPEN("Open"),
  WPA2("WPA2"),
  WPA3("WPA3"),
}

enum class WifiGeneration(val label: String, val badge: String) {
  LEGACY("802.11a/b/g", "Legacy"),
  WIFI_4("Wi-Fi 4 (802.11n)", "Wi-Fi 4"),
  WIFI_5("Wi-Fi 5 (802.11ac)", "Wi-Fi 5"),
  WIFI_6("Wi-Fi 6 (802.11ax)", "Wi-Fi 6"),
  WIFI_6E("Wi-Fi 6E (6 GHz)", "Wi-Fi 6E"),
  WIFI_7("Wi-Fi 7 (802.11be)", "Wi-Fi 7"),
  UNKNOWN("Standard", "802.11"),
}

enum class SignalTier(val label: String) {
  STRONG("Strong"),
  FAIR("Fair"),
  WEAK("Weak"),
}

enum class NetworkSortOrder(val label: String) {
  STRONGEST_SIGNAL("Strongest Signal"),
  MOST_RECENT("Most Recent"),
  MOST_SIGHTINGS("Most Sightings"),
  OPEN_NETWORKS("Open Networks"),
}

data class GeoCoordinate(
  val latitude: Double,
  val longitude: Double,
  val altitudeMeters: Double = 0.0,
)

data class FloorPlanPoint(
  val xNorm: Float,
  val yNorm: Float,
)

data class WifiReading(
  val id: Long = 0L,
  val walkId: Long,
  val bssid: String,
  val ssid: String,
  val rssiDbm: Int,
  val frequencyMhz: Int,
  val primaryChannel: Int,
  val channelWidthMhz: Int,
  val band: WifiBand,
  val security: WifiSecurity,
  val capabilitiesRaw: String,
  val wifiGeneration: WifiGeneration,
  val latitude: Double,
  val longitude: Double,
  val altitudeMeters: Double = 0.0,
  val accuracyMeters: Float = 5f,
  val timestampMillis: Long,
)

data class WifiAccessPointSummary(
  val bssid: String,
  val ssid: String,
  val isHidden: Boolean,
  val manufacturer: String,
  val band: WifiBand,
  val frequencyMhz: Int,
  val primaryChannel: Int,
  val channelWidthMhz: Int,
  val wifiGeneration: WifiGeneration,
  val security: WifiSecurity,
  val capabilitiesRaw: String,
  val peakRssiDbm: Int,
  val latestRssiDbm: Int,
  val averageRssiDbm: Int,
  val sightingsCount: Int,
  val firstSeenMillis: Long,
  val lastSeenMillis: Long,
  val strongestCoordinate: GeoCoordinate?,
  val strongestFloorPoint: FloorPlanPoint? = null,
)

data class WalkSession(
  val id: Long = 0L,
  val name: String,
  val createdAtMillis: Long,
  val isDemo: Boolean = false,
)

data class WalkTrackPoint(
  val id: Long = 0L,
  val walkId: Long,
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val timestampMillis: Long,
)

data class RoomZone(
  val id: String,
  val name: String,
  val xMin: Float,
  val yMin: Float,
  val xMax: Float,
  val yMax: Float,
)

data class FloorPlanSurvey(
  val id: Long = 0L,
  val name: String,
  val imageUri: String?,
  val isBuiltInSample: Boolean = true,
  val createdAtMillis: Long,
  val isDemo: Boolean = false,
)

data class PlacementRun(
  val id: Long = 0L,
  val surveyId: Long,
  val name: String,
  val targetBssid: String? = null,
  val targetSsid: String? = null,
  val createdAtMillis: Long,
  val isDemo: Boolean = false,
)

data class RouterPin(
  val id: Long = 0L,
  val runId: Long,
  val label: String,
  val xNorm: Float,
  val yNorm: Float,
)

data class IndoorSample(
  val id: Long = 0L,
  val runId: Long,
  val xNorm: Float,
  val yNorm: Float,
  val bssid: String,
  val ssid: String,
  val rssiDbm: Int,
  val frequencyMhz: Int,
  val primaryChannel: Int,
  val channelWidthMhz: Int,
  val band: WifiBand,
  val security: WifiSecurity,
  val wifiGeneration: WifiGeneration,
  val timestampMillis: Long,
)

data class OutdoorHeatmapPoint(
  val latitude: Double,
  val longitude: Double,
  val rssiDbm: Int,
)

data class IndoorHeatmapCell(
  val xNorm: Float,
  val yNorm: Float,
  val rssiDbm: Float,
)

data class RoomCoverageBreakdown(
  val roomName: String,
  val strongPercent: Int,
  val fairPercent: Int,
  val deadZonePercent: Int,
  val averageDbm: Int,
)

data class ChannelCongestionBin(
  val channel: Int,
  val band: WifiBand,
  val networkCount: Int,
  val congestionScore: Float,
  val strongestDbm: Int?,
)

data class WifiFilterState(
  val searchQuery: String = "",
  val bandFilter: BandFilter = BandFilter.ALL,
  val securityFilter: SecurityFilter = SecurityFilter.ALL,
  val isolatedBssid: String? = null,
)
