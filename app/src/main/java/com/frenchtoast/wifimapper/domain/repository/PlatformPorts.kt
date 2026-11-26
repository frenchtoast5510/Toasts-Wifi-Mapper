package com.frenchtoast.wifimapper.domain.repository

import com.frenchtoast.wifimapper.domain.model.GeoCoordinate
import com.frenchtoast.wifimapper.domain.model.WifiBand
import com.frenchtoast.wifimapper.domain.model.WifiGeneration
import com.frenchtoast.wifimapper.domain.model.WifiSecurity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class ScannedWifiNetwork(
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
  val timestampMillis: Long,
)

data class WifiScanBatch(
  val networks: List<ScannedWifiNetwork>,
  val scannedAtMillis: Long,
  val fromSystemCache: Boolean,
)

data class ScanCooldownState(
  val scansUsedInWindow: Int = 0,
  val maxScansPerWindow: Int = 4,
  val windowSeconds: Int = 120,
  val secondsUntilNextSlot: Int = 0,
  val isThrottled: Boolean = false,
  val lastScanStatusMessage: String = "Ready to scan",
) {
  val remainingScans: Int
    get() = (maxScansPerWindow - scansUsedInWindow).coerceAtLeast(0)
}

sealed interface ScanTriggerResult {
  data object Triggered : ScanTriggerResult
  data class Throttled(val retryAfterSeconds: Int) : ScanTriggerResult
  data object MissingPermission : ScanTriggerResult
  data object WifiDisabled : ScanTriggerResult
}

interface WifiScannerPort {
  val scanResults: Flow<WifiScanBatch>
  val cooldownState: StateFlow<ScanCooldownState>
  fun hasWifiScanPermission(): Boolean
  fun isWifiEnabled(): Boolean
  fun triggerScan(): ScanTriggerResult
  fun readLatestCachedResults(): WifiScanBatch
  fun refreshCooldownState()
}

data class LocationFix(
  val coordinate: GeoCoordinate,
  val accuracyMeters: Float,
  val speedMps: Float,
  val provider: String,
  val timestampMillis: Long,
)

interface LocationPort {
  val locationUpdates: Flow<LocationFix>
  val isTracking: StateFlow<Boolean>
  fun hasLocationPermission(): Boolean
  fun startTracking()
  fun stopTracking()
}
