package com.frenchtoast.wifimapper.platform.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.frenchtoast.wifimapper.domain.math.SignalMath
import com.frenchtoast.wifimapper.domain.model.WifiBand
import com.frenchtoast.wifimapper.domain.model.WifiGeneration
import com.frenchtoast.wifimapper.domain.repository.ScanCooldownState
import com.frenchtoast.wifimapper.domain.repository.ScanTriggerResult
import com.frenchtoast.wifimapper.domain.repository.ScannedWifiNetwork
import com.frenchtoast.wifimapper.domain.repository.WifiScanBatch
import com.frenchtoast.wifimapper.domain.repository.WifiScannerPort
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

class AndroidWifiScanner(
  private val context: Context,
) : WifiScannerPort {

  private val wifiManager: WifiManager? =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

  private val scanTimestamps = ArrayDeque<Long>()
  private val windowMillis = 120_000L
  private val maxScans = 4

  private val _cooldownState = MutableStateFlow(ScanCooldownState())
  override val cooldownState: StateFlow<ScanCooldownState> = _cooldownState.asStateFlow()

  override val scanResults: Flow<WifiScanBatch> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        val updated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true) ?: true
        } else {
          true
        }
        val batch = readLatestCachedResults().copy(fromSystemCache = !updated)
        trySend(batch)
      }
    }

    val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
    ContextCompat.registerReceiver(
      context.applicationContext,
      receiver,
      filter,
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )

    if (hasWifiScanPermission()) {
      trySend(readLatestCachedResults())
    }

    awaitClose {
      runCatching { context.applicationContext.unregisterReceiver(receiver) }
    }
  }

  override fun hasWifiScanPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val nearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.NEARBY_WIFI_DEVICES,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    return fine || nearby
  }

  override fun isWifiEnabled(): Boolean = wifiManager?.isWifiEnabled == true

  @Suppress("DEPRECATION")
  override fun triggerScan(): ScanTriggerResult {
    if (!hasWifiScanPermission()) {
      _cooldownState.value = _cooldownState.value.copy(
        lastScanStatusMessage = "Location / Wi-Fi permission required",
      )
      return ScanTriggerResult.MissingPermission
    }
    val wm = wifiManager ?: return ScanTriggerResult.WifiDisabled
    if (!wm.isWifiEnabled) {
      _cooldownState.value = _cooldownState.value.copy(
        lastScanStatusMessage = "Turn on Wi-Fi to scan",
      )
      return ScanTriggerResult.WifiDisabled
    }

    pruneWindow(System.currentTimeMillis())
    if (scanTimestamps.size >= maxScans) {
      val retrySec = computeRetrySeconds(System.currentTimeMillis())
      refreshCooldownState()
      return ScanTriggerResult.Throttled(retrySec)
    }

    val now = System.currentTimeMillis()
    val started = runCatching { wm.startScan() }.getOrDefault(false)
    scanTimestamps.addLast(now)
    pruneWindow(now)

    val retrySec = if (scanTimestamps.size >= maxScans) computeRetrySeconds(now) else 0
    _cooldownState.value = ScanCooldownState(
      scansUsedInWindow = scanTimestamps.size,
      maxScansPerWindow = maxScans,
      windowSeconds = 120,
      secondsUntilNextSlot = retrySec,
      isThrottled = !started || scanTimestamps.size >= maxScans,
      lastScanStatusMessage = if (started) {
        "Scan triggered (${scanTimestamps.size}/$maxScans in 2m)"
      } else {
        "System throttled — using cached results"
      },
    )

    return if (started) {
      ScanTriggerResult.Triggered
    } else {
      ScanTriggerResult.Throttled(retrySec.coerceAtLeast(15))
    }
  }

  override fun refreshCooldownState() {
    val now = System.currentTimeMillis()
    pruneWindow(now)
    val used = scanTimestamps.size
    val retrySec = if (used >= maxScans) computeRetrySeconds(now) else 0
    _cooldownState.value = _cooldownState.value.copy(
      scansUsedInWindow = used,
      secondsUntilNextSlot = retrySec,
      isThrottled = used >= maxScans,
    )
  }

  @SuppressLint("MissingPermission")
  override fun readLatestCachedResults(): WifiScanBatch {
    val wm = wifiManager ?: return WifiScanBatch(emptyList(), System.currentTimeMillis(), true)
    if (!hasWifiScanPermission()) {
      return WifiScanBatch(emptyList(), System.currentTimeMillis(), true)
    }
    val raw = runCatching { wm.scanResults }.getOrDefault(emptyList())
    val now = System.currentTimeMillis()
    val mapped = raw.mapNotNull { sr -> sr.toDomain(now) }
    return WifiScanBatch(
      networks = mapped,
      scannedAtMillis = now,
      fromSystemCache = true,
    )
  }

  private fun pruneWindow(now: Long) {
    while (scanTimestamps.isNotEmpty() && now - scanTimestamps.first() >= windowMillis) {
      scanTimestamps.removeFirst()
    }
  }

  private fun computeRetrySeconds(now: Long): Int {
    val oldest = scanTimestamps.firstOrNull() ?: return 0
    val remainingMs = (windowMillis - (now - oldest)).coerceAtLeast(0L)
    return ((remainingMs + 999L) / 1000L).toInt()
  }

  @Suppress("DEPRECATION")
  private fun ScanResult.toDomain(nowMillis: Long): ScannedWifiNetwork? {
    val bssidVal = BSSID?.uppercase()?.takeIf { it.isNotBlank() } ?: return null
    val rawSsid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      wifiSsid?.toString()?.removeSurrounding("\"") ?: ""
    } else {
      SSID ?: ""
    }
    val (band, channel) = SignalMath.frequencyToBandAndChannel(frequency)
    val widthMhz = when (channelWidth) {
      ScanResult.CHANNEL_WIDTH_40MHZ -> 40
      ScanResult.CHANNEL_WIDTH_80MHZ -> 80
      ScanResult.CHANNEL_WIDTH_160MHZ,
      ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ,
      -> 160
      else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        channelWidth == ScanResult.CHANNEL_WIDTH_320MHZ
      ) {
        320
      } else {
        20
      }
    }
    val generation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      when (wifiStandard) {
        ScanResult.WIFI_STANDARD_LEGACY -> WifiGeneration.LEGACY
        ScanResult.WIFI_STANDARD_11N -> WifiGeneration.WIFI_4
        ScanResult.WIFI_STANDARD_11AC -> WifiGeneration.WIFI_5
        ScanResult.WIFI_STANDARD_11AX -> {
          if (band == WifiBand.BAND_6_GHZ) WifiGeneration.WIFI_6E else WifiGeneration.WIFI_6
        }
        else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
          wifiStandard == ScanResult.WIFI_STANDARD_11BE
        ) {
          WifiGeneration.WIFI_7
        } else {
          WifiGeneration.UNKNOWN
        }
      }
    } else {
      WifiGeneration.UNKNOWN
    }

    val cap = capabilities ?: ""
    return ScannedWifiNetwork(
      bssid = bssidVal,
      ssid = rawSsid.trim(),
      rssiDbm = level.coerceIn(-100, -15),
      frequencyMhz = frequency,
      primaryChannel = channel,
      channelWidthMhz = widthMhz,
      band = band,
      security = SignalMath.parseSecurity(cap),
      capabilitiesRaw = cap,
      wifiGeneration = generation,
      timestampMillis = nowMillis,
    )
  }
}
