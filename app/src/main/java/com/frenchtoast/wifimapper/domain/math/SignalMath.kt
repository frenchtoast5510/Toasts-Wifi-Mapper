package com.frenchtoast.wifimapper.domain.math

import com.frenchtoast.wifimapper.domain.model.BandFilter
import com.frenchtoast.wifimapper.domain.model.ChannelCongestionBin
import com.frenchtoast.wifimapper.domain.model.IndoorHeatmapCell
import com.frenchtoast.wifimapper.domain.model.IndoorSample
import com.frenchtoast.wifimapper.domain.model.NetworkSortOrder
import com.frenchtoast.wifimapper.domain.model.OuiLookup
import com.frenchtoast.wifimapper.domain.model.OutdoorHeatmapPoint
import com.frenchtoast.wifimapper.domain.model.RoomCoverageBreakdown
import com.frenchtoast.wifimapper.domain.model.RoomZone
import com.frenchtoast.wifimapper.domain.model.SecurityFilter
import com.frenchtoast.wifimapper.domain.model.SignalTier
import com.frenchtoast.wifimapper.domain.model.WifiAccessPointSummary
import com.frenchtoast.wifimapper.domain.model.WifiBand
import com.frenchtoast.wifimapper.domain.model.WifiFilterState
import com.frenchtoast.wifimapper.domain.model.WifiGeneration
import com.frenchtoast.wifimapper.domain.model.WifiReading
import com.frenchtoast.wifimapper.domain.model.WifiSecurity
import com.frenchtoast.wifimapper.domain.model.GeoCoordinate
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object SignalMath {

  fun frequencyToBandAndChannel(frequencyMhz: Int): Pair<WifiBand, Int> {
    return when {
      frequencyMhz == 2484 -> WifiBand.BAND_2_4_GHZ to 14
      frequencyMhz in 2412..2472 -> {
        val ch = (frequencyMhz - 2407) / 5
        WifiBand.BAND_2_4_GHZ to ch
      }
      frequencyMhz in 5160..5885 -> {
        val ch = (frequencyMhz - 5000) / 5
        WifiBand.BAND_5_GHZ to ch
      }
      frequencyMhz in 5925..7125 -> {
        val ch = (frequencyMhz - 5950) / 5
        WifiBand.BAND_6_GHZ to max(1, ch)
      }
      else -> WifiBand.UNKNOWN to 0
    }
  }

  fun parseSecurity(capabilities: String): WifiSecurity {
    val upper = capabilities.uppercase()
    return when {
      upper.contains("EAP") || upper.contains("802.1X") -> WifiSecurity.ENTERPRISE
      upper.contains("SAE") || upper.contains("WPA3") -> WifiSecurity.WPA3
      upper.contains("WPA2") || upper.contains("RSN") || upper.contains("PSK") -> WifiSecurity.WPA2_PSK
      upper.contains("WEP") -> WifiSecurity.WEP
      upper.contains("ESS") || upper.isBlank() || upper == "[ESS]" -> WifiSecurity.OPEN
      else -> WifiSecurity.UNKNOWN
    }
  }

  fun rssiToSignalTier(rssiDbm: Int): SignalTier {
    return when {
      rssiDbm >= -60 -> SignalTier.STRONG
      rssiDbm >= -75 -> SignalTier.FAIR
      else -> SignalTier.WEAK
    }
  }

  fun rssiToNormalizedQuality(rssiDbm: Int): Float {
    val clamped = rssiDbm.coerceIn(-95, -30)
    return (clamped + 95) / 65f
  }

  fun haversineMeters(a: GeoCoordinate, b: GeoCoordinate): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * r * atan2(sqrt(h), sqrt(1 - h))
  }

  fun summarizeAccessPoints(readings: List<WifiReading>): List<WifiAccessPointSummary> {
    if (readings.isEmpty()) return emptyList()
    return readings.groupBy { it.bssid.uppercase() }.map { (bssid, group) ->
      val sorted = group.sortedBy { it.timestampMillis }
      val strongest = group.maxByOrNull { it.rssiDbm } ?: sorted.last()
      val latest = sorted.last()
      val nonBlankSsid = group.lastOrNull { it.ssid.isNotBlank() }?.ssid.orEmpty()
      WifiAccessPointSummary(
        bssid = bssid,
        ssid = nonBlankSsid.ifBlank { "Hidden Network" },
        isHidden = nonBlankSsid.isBlank(),
        manufacturer = OuiLookup.lookup(bssid),
        band = latest.band,
        frequencyMhz = latest.frequencyMhz,
        primaryChannel = latest.primaryChannel,
        channelWidthMhz = latest.channelWidthMhz,
        wifiGeneration = latest.wifiGeneration,
        security = latest.security,
        capabilitiesRaw = latest.capabilitiesRaw,
        peakRssiDbm = strongest.rssiDbm,
        latestRssiDbm = latest.rssiDbm,
        averageRssiDbm = group.map { it.rssiDbm }.average().toInt(),
        sightingsCount = group.size,
        firstSeenMillis = sorted.first().timestampMillis,
        lastSeenMillis = latest.timestampMillis,
        strongestCoordinate = GeoCoordinate(
          latitude = strongest.latitude,
          longitude = strongest.longitude,
          altitudeMeters = strongest.altitudeMeters,
        ),
      )
    }
  }

  fun filterSummaries(
    summaries: List<WifiAccessPointSummary>,
    filter: WifiFilterState,
    sortOrder: NetworkSortOrder = NetworkSortOrder.STRONGEST_SIGNAL,
  ): List<WifiAccessPointSummary> {
    val q = filter.searchQuery.trim().lowercase()
    val filtered = summaries.filter { ap ->
      val matchesIsolation = filter.isolatedBssid == null ||
        ap.bssid.equals(filter.isolatedBssid, ignoreCase = true)
      val matchesBand = when (filter.bandFilter) {
        BandFilter.ALL -> true
        BandFilter.BAND_2_4 -> ap.band == WifiBand.BAND_2_4_GHZ
        BandFilter.BAND_5 -> ap.band == WifiBand.BAND_5_GHZ
        BandFilter.BAND_6 -> ap.band == WifiBand.BAND_6_GHZ
      }
      val matchesSecurity = when (filter.securityFilter) {
        SecurityFilter.ALL -> true
        SecurityFilter.OPEN -> ap.security == WifiSecurity.OPEN
        SecurityFilter.WPA2 -> ap.security == WifiSecurity.WPA2_PSK
        SecurityFilter.WPA3 -> ap.security == WifiSecurity.WPA3
      }
      val matchesQuery = q.isEmpty() ||
        ap.ssid.lowercase().contains(q) ||
        ap.bssid.lowercase().contains(q) ||
        ap.manufacturer.lowercase().contains(q) ||
        ap.primaryChannel.toString() == q
      matchesIsolation && matchesBand && matchesSecurity && matchesQuery
    }

    return when (sortOrder) {
      NetworkSortOrder.STRONGEST_SIGNAL -> filtered.sortedByDescending { it.peakRssiDbm }
      NetworkSortOrder.MOST_RECENT -> filtered.sortedByDescending { it.lastSeenMillis }
      NetworkSortOrder.MOST_SIGHTINGS -> filtered.sortedByDescending { it.sightingsCount }
      NetworkSortOrder.OPEN_NETWORKS -> filtered.sortedWith(
        compareByDescending<WifiAccessPointSummary> { it.security == WifiSecurity.OPEN }
          .thenByDescending { it.peakRssiDbm },
      )
    }
  }

  fun filterReadings(
    readings: List<WifiReading>,
    filter: WifiFilterState,
  ): List<WifiReading> {
    val q = filter.searchQuery.trim().lowercase()
    return readings.filter { r ->
      val matchesIsolation = filter.isolatedBssid == null ||
        r.bssid.equals(filter.isolatedBssid, ignoreCase = true)
      val matchesBand = when (filter.bandFilter) {
        BandFilter.ALL -> true
        BandFilter.BAND_2_4 -> r.band == WifiBand.BAND_2_4_GHZ
        BandFilter.BAND_5 -> r.band == WifiBand.BAND_5_GHZ
        BandFilter.BAND_6 -> r.band == WifiBand.BAND_6_GHZ
      }
      val matchesSecurity = when (filter.securityFilter) {
        SecurityFilter.ALL -> true
        SecurityFilter.OPEN -> r.security == WifiSecurity.OPEN
        SecurityFilter.WPA2 -> r.security == WifiSecurity.WPA2_PSK
        SecurityFilter.WPA3 -> r.security == WifiSecurity.WPA3
      }
      val matchesQuery = q.isEmpty() ||
        r.ssid.lowercase().contains(q) ||
        r.bssid.lowercase().contains(q)
      matchesIsolation && matchesBand && matchesSecurity && matchesQuery
    }
  }

  /**
   * Collapses raw outdoor readings into quantized spatial heatmap anchors (~12m grid)
   * retaining peak RSSI per cell for crisp, responsive map overlay rendering.
   */
  fun computeOutdoorHeatmapPoints(readings: List<WifiReading>): List<OutdoorHeatmapPoint> {
    if (readings.isEmpty()) return emptyList()
    val cellLatStep = 0.00011
    val cellLonStep = 0.00013
    return readings
      .groupBy { r ->
        val latBucket = (r.latitude / cellLatStep).toInt()
        val lonBucket = (r.longitude / cellLonStep).toInt()
        latBucket to lonBucket
      }
      .map { (_, bucket) ->
        val avgLat = bucket.map { it.latitude }.average()
        val avgLon = bucket.map { it.longitude }.average()
        val peakRssi = bucket.maxOf { it.rssiDbm }
        OutdoorHeatmapPoint(
          latitude = avgLat,
          longitude = avgLon,
          rssiDbm = peakRssi,
        )
      }
  }

  /**
   * Computes a smooth Inverse Distance Weighting (IDW) grid across normalized [0..1]
   * floor-plan coordinates from captured indoor Wi-Fi samples.
   */
  fun computeIndoorHeatmapGrid(
    samples: List<IndoorSample>,
    cols: Int = 36,
    rows: Int = 26,
    power: Double = 2.4,
  ): List<IndoorHeatmapCell> {
    if (samples.isEmpty()) return emptyList()
    // Aggregate multiple AP sightings at the exact same spot by taking strongest signal at that spot
    val spotSamples = samples
      .groupBy { (it.xNorm * 100).toInt() to (it.yNorm * 100).toInt() }
      .map { (_, group) ->
        IndoorSampleSpot(
          x = group.map { it.xNorm }.average().toFloat(),
          y = group.map { it.yNorm }.average().toFloat(),
          rssiDbm = group.maxOf { it.rssiDbm }.toFloat(),
        )
      }

    val cells = ArrayList<IndoorHeatmapCell>(cols * rows)
    for (row in 0 until rows) {
      val y = (row + 0.5f) / rows
      for (col in 0 until cols) {
        val x = (col + 0.5f) / cols
        val interpolated = interpolateIdw(x, y, spotSamples, power)
        cells.add(IndoorHeatmapCell(xNorm = x, yNorm = y, rssiDbm = interpolated))
      }
    }
    return cells
  }

  private data class IndoorSampleSpot(val x: Float, val y: Float, val rssiDbm: Float)

  private fun interpolateIdw(
    x: Float,
    y: Float,
    spots: List<IndoorSampleSpot>,
    power: Double,
  ): Float {
    var weightedSum = 0.0
    var weightTotal = 0.0
    var minDist = Double.MAX_VALUE

    for (spot in spots) {
      val dx = (x - spot.x).toDouble()
      val dy = (y - spot.y).toDouble()
      val dist = sqrt(dx * dx + dy * dy)
      if (dist < 0.008) {
        return spot.rssiDbm
      }
      if (dist < minDist) minDist = dist
      val w = 1.0 / dist.pow(power)
      weightedSum += w * spot.rssiDbm
      weightTotal += w
    }

    val raw = if (weightTotal > 0.0) (weightedSum / weightTotal).toFloat() else -95f
    // Gentle distance rolloff far from any sampled point so unsampled corners decay realistically
    val farPenalty = ((minDist - 0.30).coerceAtLeast(0.0) * 28.0).toFloat()
    return (raw - farPenalty).coerceIn(-95f, -30f)
  }

  fun computeRoomCoverage(
    samples: List<IndoorSample>,
    rooms: List<RoomZone>,
  ): List<RoomCoverageBreakdown> {
    if (samples.isEmpty()) {
      return rooms.map {
        RoomCoverageBreakdown(
          roomName = it.name,
          strongPercent = 0,
          fairPercent = 0,
          deadZonePercent = 100,
          averageDbm = -95,
        )
      }
    }
    val grid = computeIndoorHeatmapGrid(samples, cols = 40, rows = 30)
    return rooms.map { room ->
      val inside = grid.filter { cell ->
        cell.xNorm in room.xMin..room.xMax && cell.yNorm in room.yMin..room.yMax
      }
      if (inside.isEmpty()) {
        RoomCoverageBreakdown(room.name, 0, 0, 100, -95)
      } else {
        val total = inside.size.toFloat()
        val strong = inside.count { it.rssiDbm >= -60f }
        val fair = inside.count { it.rssiDbm < -60f && it.rssiDbm >= -75f }
        val strongPct = ((strong / total) * 100).toInt()
        val fairPct = ((fair / total) * 100).toInt()
        val deadPct = max(0, 100 - strongPct - fairPct)
        RoomCoverageBreakdown(
          roomName = room.name,
          strongPercent = strongPct,
          fairPercent = fairPct,
          deadZonePercent = deadPct,
          averageDbm = inside.map { it.rssiDbm }.average().toInt(),
        )
      }
    }
  }

  fun computeOverallCoverage(rooms: List<RoomCoverageBreakdown>): RoomCoverageBreakdown {
    if (rooms.isEmpty()) {
      return RoomCoverageBreakdown("Whole Floor Plan", 0, 0, 100, -95)
    }
    val strong = rooms.map { it.strongPercent }.average().toInt()
    val fair = rooms.map { it.fairPercent }.average().toInt()
    val dead = max(0, 100 - strong - fair)
    val avgDbm = rooms.map { it.averageDbm }.average().toInt()
    return RoomCoverageBreakdown("Whole Floor Plan", strong, fair, dead, avgDbm)
  }

  fun computeChannelCongestion(
    summaries: List<WifiAccessPointSummary>,
    band: WifiBand,
  ): List<ChannelCongestionBin> {
    val channels = when (band) {
      WifiBand.BAND_2_4_GHZ -> (1..13).toList()
      WifiBand.BAND_5_GHZ -> listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 132, 136, 140, 149, 153, 157, 161, 165)
      WifiBand.BAND_6_GHZ -> listOf(1, 5, 9, 13, 17, 21, 25, 29, 33, 37, 45, 53, 61, 69, 77, 85, 93)
      WifiBand.UNKNOWN -> emptyList()
    }
    val bandAps = summaries.filter { it.band == band }
    return channels.map { targetCh ->
      var directCount = 0
      var weightedLoad = 0f
      var strongest: Int? = null

      for (ap in bandAps) {
        val halfSpanChannels = when (band) {
          WifiBand.BAND_2_4_GHZ -> if (ap.channelWidthMhz >= 40) 4 else 2
          WifiBand.BAND_5_GHZ -> when {
            ap.channelWidthMhz >= 160 -> 14
            ap.channelWidthMhz >= 80 -> 6
            ap.channelWidthMhz >= 40 -> 2
            else -> 0
          }
          WifiBand.BAND_6_GHZ -> when {
            ap.channelWidthMhz >= 160 -> 14
            ap.channelWidthMhz >= 80 -> 6
            ap.channelWidthMhz >= 40 -> 2
            else -> 0
          }
          WifiBand.UNKNOWN -> 0
        }
        val delta = abs(ap.primaryChannel - targetCh)
        if (delta == 0) {
          directCount++
          strongest = if (strongest == null) ap.peakRssiDbm else max(strongest, ap.peakRssiDbm)
          weightedLoad += 0.5f + rssiToNormalizedQuality(ap.peakRssiDbm)
        } else if (delta <= halfSpanChannels) {
          val overlapWeight = 1f - (delta.toFloat() / (halfSpanChannels + 1f))
          weightedLoad += overlapWeight * (0.35f + 0.5f * rssiToNormalizedQuality(ap.peakRssiDbm))
        }
      }
      ChannelCongestionBin(
        channel = targetCh,
        band = band,
        networkCount = directCount,
        congestionScore = weightedLoad,
        strongestDbm = strongest,
      )
    }
  }

  val defaultDemoRooms: List<RoomZone> = listOf(
    RoomZone("living", "Living Room", 0.06f, 0.08f, 0.42f, 0.52f),
    RoomZone("kitchen", "Kitchen & Dining", 0.42f, 0.08f, 0.94f, 0.44f),
    RoomZone("hallway", "Central Hallway", 0.36f, 0.44f, 0.64f, 0.64f),
    RoomZone("office", "Home Office", 0.06f, 0.52f, 0.36f, 0.92f),
    RoomZone("primary_bed", "Primary Bedroom", 0.64f, 0.44f, 0.94f, 0.92f),
    RoomZone("guest_bath", "Guest & Bath", 0.36f, 0.64f, 0.64f, 0.92f),
  )
}
