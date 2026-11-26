package com.frenchtoast.wifimapper.platform.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionStatusSnapshot(
  val fineLocationGranted: Boolean,
  val coarseLocationGranted: Boolean,
  val nearbyWifiGranted: Boolean,
  val wifiStateGranted: Boolean,
  val changeWifiStateGranted: Boolean,
) {
  val canMapOutdoors: Boolean
    get() = fineLocationGranted && nearbyWifiGranted && changeWifiStateGranted

  val canScanIndoors: Boolean
    get() = (fineLocationGranted || nearbyWifiGranted) && changeWifiStateGranted

  val hasAllLivePermissions: Boolean
    get() = fineLocationGranted && nearbyWifiGranted && wifiStateGranted && changeWifiStateGranted
}

object PermissionHelper {

  fun snapshot(context: Context): PermissionStatusSnapshot {
    val fine = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    val nearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      isGranted(context, Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
      true
    }
    val wifiState = isGranted(context, Manifest.permission.ACCESS_WIFI_STATE)
    val changeWifiState = isGranted(context, Manifest.permission.CHANGE_WIFI_STATE)
    return PermissionStatusSnapshot(
      fineLocationGranted = fine,
      coarseLocationGranted = coarse,
      nearbyWifiGranted = nearby,
      wifiStateGranted = wifiState,
      changeWifiStateGranted = changeWifiState,
    )
  }

  fun requiredRuntimePermissions(): Array<String> {
    return buildList {
      add(Manifest.permission.ACCESS_FINE_LOCATION)
      add(Manifest.permission.ACCESS_COARSE_LOCATION)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
      }
    }.toTypedArray()
  }

  private fun isGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
  }
}
