package com.frenchtoast.wifimapper.platform.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.frenchtoast.wifimapper.domain.model.GeoCoordinate
import com.frenchtoast.wifimapper.domain.repository.LocationFix
import com.frenchtoast.wifimapper.domain.repository.LocationPort
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidLocationProvider(
  private val context: Context,
) : LocationPort {

  private val locationManager: LocationManager? =
    context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

  private val _locationUpdates = MutableSharedFlow<LocationFix>(
    replay = 1,
    extraBufferCapacity = 8,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  override val locationUpdates: Flow<LocationFix> = _locationUpdates.asSharedFlow()

  private val _isTracking = MutableStateFlow(false)
  override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

  private val listener = object : LocationListener {
    override fun onLocationChanged(location: Location) {
      _locationUpdates.tryEmit(location.toFix())
    }

    @Deprecated("Deprecated in Android API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
  }

  override fun hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  override fun startTracking() {
    if (!hasLocationPermission()) return
    val lm = locationManager ?: return
    if (_isTracking.value) return

    val providers = buildList {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        lm.allProviders.contains(LocationManager.FUSED_PROVIDER)
      ) {
        add(LocationManager.FUSED_PROVIDER)
      }
      if (lm.allProviders.contains(LocationManager.GPS_PROVIDER)) {
        add(LocationManager.GPS_PROVIDER)
      }
      if (lm.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
        add(LocationManager.NETWORK_PROVIDER)
      }
    }

    val lastBest = providers
      .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
      .maxByOrNull { it.time }
    lastBest?.let { _locationUpdates.tryEmit(it.toFix()) }

    providers.forEach { provider ->
      runCatching {
        lm.requestLocationUpdates(
          provider,
          2_000L,
          1.5f,
          listener,
          Looper.getMainLooper(),
        )
      }
    }
    _isTracking.value = true
  }

  override fun stopTracking() {
    val lm = locationManager ?: return
    runCatching { lm.removeUpdates(listener) }
    _isTracking.value = false
  }

  private fun Location.toFix() = LocationFix(
    coordinate = GeoCoordinate(
      latitude = latitude,
      longitude = longitude,
      altitudeMeters = if (hasAltitude()) altitude else 0.0,
    ),
    accuracyMeters = if (hasAccuracy()) accuracy else 12f,
    speedMps = if (hasSpeed()) speed else 0f,
    provider = provider ?: "gps",
    timestampMillis = time.takeIf { it > 0L } ?: System.currentTimeMillis(),
  )
}
