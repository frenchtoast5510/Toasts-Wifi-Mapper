package com.frenchtoast.wifimapper.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.GlassBorderColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SignalColors
import com.frenchtoast.wifimapper.domain.model.BandFilter
import com.frenchtoast.wifimapper.domain.model.OutdoorHeatmapPoint
import com.frenchtoast.wifimapper.domain.model.SecurityFilter
import com.frenchtoast.wifimapper.domain.model.WifiAccessPointSummary
import com.frenchtoast.wifimapper.ui.components.GlassSurface
import com.frenchtoast.wifimapper.ui.components.SignalQualityLegendPill
import com.frenchtoast.wifimapper.ui.state.WifiMapperUiState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import java.io.File
import java.util.Locale

@Composable
fun OutdoorMapScreen(
  uiState: WifiMapperUiState,
  onSearchQueryChange: (String) -> Unit,
  onSelectBand: (BandFilter) -> Unit,
  onSelectSecurity: (SecurityFilter) -> Unit,
  onClearIsolation: () -> Unit,
  onToggleHeatmap: () -> Unit,
  onToggleMapping: () -> Unit,
  onTriggerManualScan: () -> Unit,
  onOpenThrottleDialog: () -> Unit,
  onApMarkerClicked: (WifiAccessPointSummary) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val isOnline = remember { checkOnlineStatus(context) }

  val darkTileFilter = remember {
    ColorMatrixColorFilter(
      ColorMatrix(
        floatArrayOf(
          -0.82f, 0f, 0f, 0f, 224f,
          0f, -0.80f, 0f, 0f, 228f,
          0f, 0f, -0.74f, 0f, 238f,
          0f, 0f, 0f, 1f, 0f,
        ),
      ),
    )
  }

  val mapView = remember {
    configureOsmdroid(context)
    MapView(context).apply {
      setTileSource(TileSourceFactory.MAPNIK)
      setBackgroundColor(AndroidColor.parseColor("#0B0F17"))
      overlayManager.tilesOverlay.setColorFilter(darkTileFilter)
      setMultiTouchControls(true)
      isTilesScaledToDpi = true
      controller.setZoom(17.5)
      controller.setCenter(GeoPoint(37.7532, -122.4183))
      setUseDataConnection(isOnline)
    }
  }

  val compassOverlay = remember(mapView) {
    CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
      enableCompass()
    }
  }

  DisposableEffect(lifecycleOwner, mapView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapView.onDetach()
    }
  }

  var hasCenteredInitially by remember { mutableStateOf(false) }

  LaunchedEffect(uiState.activeWalk?.id, uiState.activeWalkTrack.size) {
    if (!hasCenteredInitially && uiState.activeWalkTrack.isNotEmpty()) {
      val first = uiState.activeWalkTrack.first()
      mapView.controller.setCenter(GeoPoint(first.latitude, first.longitude))
      mapView.controller.setZoom(17.4)
      hasCenteredInitially = true
    }
  }

  LaunchedEffect(uiState.mapFocusTriggerId) {
    val target = uiState.mapFocusTarget
    if (target != null && uiState.mapFocusTriggerId > 0L) {
      mapView.controller.animateTo(GeoPoint(target.latitude, target.longitude), 18.5, 600L)
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
      factory = { mapView },
      modifier = Modifier.fillMaxSize(),
      update = { view ->
        view.overlayManager.tilesOverlay.setColorFilter(darkTileFilter)
        view.overlays.clear()

        view.overlays.add(OfflineGridBackgroundOverlay(isOnline = isOnline))

        if (uiState.showOutdoorHeatmap && uiState.outdoorHeatmapPoints.isNotEmpty()) {
          view.overlays.add(OutdoorSignalHeatmapOverlay(uiState.outdoorHeatmapPoints))
        }

        if (uiState.activeWalkTrack.size >= 2) {
          val trackLine = Polyline(view).apply {
            setPoints(uiState.activeWalkTrack.map { GeoPoint(it.latitude, it.longitude) })
            outlinePaint.color = AndroidColor.parseColor("#56CCF2")
            outlinePaint.strokeWidth = 8f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
          }
          view.overlays.add(trackLine)
        }

        uiState.latestLocation?.let { fix ->
          val meMarker = Marker(view).apply {
            position = GeoPoint(fix.coordinate.latitude, fix.coordinate.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createCurrentLocationDrawable(context)
            title = "Current Position (±${fix.accuracyMeters.toInt()}m)"
          }
          view.overlays.add(meMarker)
        }

        for (ap in uiState.filteredSummaries) {
          val coord = ap.strongestCoordinate ?: continue
          val marker = Marker(view).apply {
            position = GeoPoint(coord.latitude, coord.longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createApPinDrawable(context, ap.peakRssiDbm, ap.band.shortLabel)
            title = "${ap.ssid} (${SignalColors.plainQualityLabel(ap.peakRssiDbm)} · ${ap.peakRssiDbm} dBm)"
            setOnMarkerClickListener { _, _ ->
              onApMarkerClicked(ap)
              true
            }
          }
          view.overlays.add(marker)
        }

        view.overlays.add(compassOverlay)
        view.invalidate()
      },
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      GlassSurface(
        shape = RoundedCornerShape(24.dp),
        alpha = 0.74f,
        shadowElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp),
      ) {
        OutlinedTextField(
          value = uiState.filterState.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Search SSID, BSSID, vendor, or channel…") },
          leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = "Search access points")
          },
          trailingIcon = {
            if (uiState.filterState.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("outdoor_search_input"),
        )
      }

      LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item {
          GlassFilterChip(
            selected = uiState.showOutdoorHeatmap,
            onClick = onToggleHeatmap,
            label = "Heatmap",
            leadingIcon = {
              Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("chip_heatmap_toggle"),
          )
        }

        if (uiState.filterState.isolatedBssid != null) {
          item {
            GlassFilterChip(
              selected = true,
              onClick = onClearIsolation,
              label = "Isolated: ${uiState.filterState.isolatedBssid}",
              trailingIcon = {
                Icon(Icons.Outlined.Close, contentDescription = "Clear isolation", modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.testTag("chip_clear_isolation"),
            )
          }
        }

        BandFilter.entries.forEach { band ->
          item {
            GlassFilterChip(
              selected = uiState.filterState.bandFilter == band,
              onClick = { onSelectBand(band) },
              label = band.label,
              modifier = Modifier.testTag("chip_band_${band.name}"),
            )
          }
        }

        SecurityFilter.entries.forEach { sec ->
          if (sec != SecurityFilter.ALL || uiState.filterState.securityFilter != SecurityFilter.ALL) {
            item {
              GlassFilterChip(
                selected = uiState.filterState.securityFilter == sec,
                onClick = { onSelectSecurity(sec) },
                label = sec.label,
                modifier = Modifier.testTag("chip_sec_${sec.name}"),
              )
            }
          }
        }
      }

      AnimatedVisibility(visible = !isOnline) {
        GlassSurface(
          shape = RoundedCornerShape(16.dp),
          alpha = 0.78f,
          modifier = Modifier.padding(horizontal = 14.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          ) {
            Icon(
              Icons.Outlined.WifiOff,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
              text = "Offline Grid Mode — cached tiles & local DB active",
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(start = 14.dp, end = 14.dp, bottom = 114.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SignalQualityLegendPill()

      GlassSurface(
        shape = CircleShape,
        alpha = 0.8f,
        shadowElevation = 6.dp,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .clickable {
            val target = uiState.latestLocation?.coordinate
              ?: uiState.activeWalkTrack.lastOrNull()?.let {
                com.frenchtoast.wifimapper.domain.model.GeoCoordinate(it.latitude, it.longitude)
              }
              ?: uiState.filteredSummaries.firstOrNull()?.strongestCoordinate
            if (target != null) {
              mapView.controller.animateTo(GeoPoint(target.latitude, target.longitude), 17.6, 500L)
            }
          }
          .testTag("outdoor_recenter_button"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Outlined.MyLocation,
            contentDescription = "Recenter map",
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    }

    GlassSurface(
      shape = RoundedCornerShape(24.dp),
      alpha = 0.74f,
      shadowElevation = 10.dp,
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = onToggleMapping,
            shape = RoundedCornerShape(20.dp),
            colors = if (uiState.isMappingActive) {
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
              )
            } else {
              ButtonDefaults.buttonColors()
            },
            modifier = Modifier.testTag("outdoor_mapping_toggle_button"),
          ) {
            Icon(
              imageVector = if (uiState.isMappingActive) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
              contentDescription = null,
            )
            Spacer(Modifier.width(6.dp))
            Text(if (uiState.isMappingActive) "Pause Mapping" else "Start Mapping")
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
              border = BorderStroke(1.dp, GlassBorderColor),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onTriggerManualScan)
                .testTag("outdoor_scan_button"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  Icons.Outlined.Radar,
                  contentDescription = "Trigger Wi-Fi scan",
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = if (uiState.scanCooldown.isThrottled && uiState.scanCooldown.secondsUntilNextSlot > 0) {
                    "${uiState.scanCooldown.secondsUntilNextSlot}s"
                  } else {
                    "${uiState.scanCooldown.remainingScans}/${uiState.scanCooldown.maxScansPerWindow}"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = JetBrainsMonoFontFamily,
                )
              }
            }

            IconButton(
              onClick = onOpenThrottleDialog,
              modifier = Modifier.testTag("outdoor_throttle_info_button"),
            ) {
              Icon(
                Icons.Outlined.Info,
                contentDescription = "Wi-Fi scan throttling info",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${uiState.filteredSummaries.size} APs shown · ${uiState.activeWalkReadings.size} logs",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.GpsFixed,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = if (uiState.latestLocation != null) SignalColors.StrongGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val gpsLabel = when {
              uiState.latestLocation != null -> String.format(
                Locale.US,
                "GPS ±%.1fm",
                uiState.latestLocation.accuracyMeters,
              )
              uiState.isDemoModeEnabled -> "Demo Track"
              else -> "Waiting GPS"
            }
            Text(
              text = gpsLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun GlassFilterChip(
  selected: Boolean,
  onClick: () -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label) },
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    shape = RoundedCornerShape(20.dp),
    border = FilterChipDefaults.filterChipBorder(
      enabled = true,
      selected = selected,
      borderColor = GlassBorderColor,
      selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
    ),
    colors = FilterChipDefaults.filterChipColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.84f),
    ),
    modifier = modifier,
  )
}

private fun configureOsmdroid(context: Context) {
  val config = Configuration.getInstance()
  config.userAgentValue = "ToastsWifiMapper/1.1 (${context.packageName})"
  val basePath = File(context.cacheDir, "osmdroid").apply { mkdirs() }
  val tileCache = File(basePath, "tiles").apply { mkdirs() }
  config.osmdroidBasePath = basePath
  config.osmdroidTileCache = tileCache
}

private fun checkOnlineStatus(context: Context): Boolean {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
  val net = cm.activeNetwork ?: return false
  val caps = cm.getNetworkCapabilities(net) ?: return false
  return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private class OfflineGridBackgroundOverlay(private val isOnline: Boolean) : Overlay() {
  private val gridPaint = Paint().apply {
    color = AndroidColor.argb(if (isOnline) 18 else 42, 148, 163, 184)
    strokeWidth = 1.2f
  }

  override fun draw(canvas: AndroidCanvas, mapView: MapView, shadow: Boolean) {
    if (shadow) return
    val w = mapView.width.toFloat()
    val h = mapView.height.toFloat()
    val step = 110f
    var x = 0f
    while (x < w) {
      canvas.drawLine(x, 0f, x, h, gridPaint)
      x += step
    }
    var y = 0f
    while (y < h) {
      canvas.drawLine(0f, y, w, y, gridPaint)
      y += step
    }
  }
}

private class OutdoorSignalHeatmapOverlay(
  private val points: List<OutdoorHeatmapPoint>,
) : Overlay() {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val screenPt = Point()

  override fun draw(canvas: AndroidCanvas, mapView: MapView, shadow: Boolean) {
    if (shadow || points.isEmpty()) return
    val projection = mapView.projection
    val radiusPx = (projection.metersToPixels(36f)).coerceIn(30f, 150f)

    for (pt in points) {
      projection.toPixels(GeoPoint(pt.latitude, pt.longitude), screenPt)
      val x = screenPt.x.toFloat()
      val y = screenPt.y.toFloat()
      if (x < -radiusPx || y < -radiusPx || x > mapView.width + radiusPx || y > mapView.height + radiusPx) {
        continue
      }
      val coreColor = SignalColors.heatmapArgb(pt.rssiDbm.toFloat(), alpha = 132)
      val midColor = SignalColors.heatmapArgb(pt.rssiDbm.toFloat(), alpha = 68)
      val outerColor = SignalColors.heatmapArgb(pt.rssiDbm.toFloat(), alpha = 20)
      val edgeColor = SignalColors.heatmapArgb(pt.rssiDbm.toFloat(), alpha = 0)
      paint.shader = RadialGradient(
        x,
        y,
        radiusPx,
        intArrayOf(coreColor, midColor, outerColor, edgeColor),
        floatArrayOf(0f, 0.42f, 0.76f, 1f),
        Shader.TileMode.CLAMP,
      )
      canvas.drawCircle(x, y, radiusPx, paint)
    }
    paint.shader = null
  }
}

private fun createApPinDrawable(context: Context, rssiDbm: Int, bandShort: String): BitmapDrawable {
  val size = 64
  val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = AndroidCanvas(bmp)
  val cx = size / 2f
  val cy = size / 2f

  val fillColor = when {
    rssiDbm >= -60 -> AndroidColor.parseColor("#10B981")
    rssiDbm >= -75 -> AndroidColor.parseColor("#F59E0B")
    else -> AndroidColor.parseColor("#E11D48")
  }
  val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = AndroidColor.argb(72, AndroidColor.red(fillColor), AndroidColor.green(fillColor), AndroidColor.blue(fillColor))
  }
  canvas.drawCircle(cx, cy, 28f, haloPaint)

  val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = AndroidColor.parseColor("#0B0F17")
    style = Paint.Style.FILL
  }
  canvas.drawCircle(cx, cy, 20f, borderPaint)

  val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = fillColor
    style = Paint.Style.FILL
  }
  canvas.drawCircle(cx, cy, 16.5f, fillPaint)

  val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = AndroidColor.WHITE
    textSize = 14f
    textAlign = Paint.Align.CENTER
    isFakeBoldText = true
  }
  canvas.drawText(bandShort, cx, cy + 5f, textPaint)

  return BitmapDrawable(context.resources, bmp)
}

private fun createCurrentLocationDrawable(context: Context): BitmapDrawable {
  val size = 54
  val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = AndroidCanvas(bmp)
  val cx = size / 2f
  val cy = size / 2f

  val pulse = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = AndroidColor.argb(75, 86, 204, 242)
  }
  canvas.drawCircle(cx, cy, 24f, pulse)

  val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = AndroidColor.WHITE
  }
  canvas.drawCircle(cx, cy, 13f, ring)

  val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = AndroidColor.parseColor("#38BDF8")
  }
  canvas.drawCircle(cx, cy, 9.5f, dot)

  return BitmapDrawable(context.resources, bmp)
}
