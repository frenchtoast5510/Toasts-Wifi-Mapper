package com.frenchtoast.wifimapper.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FilterCenterFocus
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SignalColors
import com.frenchtoast.wifimapper.domain.math.SignalMath
import com.frenchtoast.wifimapper.domain.model.WifiAccessPointSummary
import com.frenchtoast.wifimapper.domain.model.WifiSecurity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WifiInspectorBottomSheet(
  ap: WifiAccessPointSummary,
  onDismiss: () -> Unit,
  onIsolateOnHeatmap: (String) -> Unit,
  onSetAsIndoorTarget: (WifiAccessPointSummary) -> Unit,
  onCopiedToClipboard: (String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  val signalColor = SignalColors.forRssi(ap.peakRssiDbm)
  val plainQuality = SignalColors.plainQualityLabel(ap.peakRssiDbm)
  val quality = SignalMath.rssiToNormalizedQuality(ap.peakRssiDbm)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(signalColor.copy(alpha = 0.16f)),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${ap.peakRssiDbm}",
              style = MaterialTheme.typography.titleMedium,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              color = signalColor,
            )
            Text(
              text = "dBm",
              style = MaterialTheme.typography.labelSmall,
              color = signalColor,
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (ap.isHidden) "Hidden Network" else ap.ssid,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = ap.bssid,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Router,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = ap.manufacturer,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        InspectorSpecPill(label = ap.band.label, highlight = true)
        InspectorSpecPill(label = "Ch ${ap.primaryChannel} · ${ap.frequencyMhz} MHz")
        InspectorSpecPill(label = "${ap.channelWidthMhz} MHz")
        InspectorSpecPill(label = ap.wifiGeneration.badge)
        InspectorSpecPill(
          label = ap.security.label,
          icon = if (ap.security == WifiSecurity.OPEN) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
        )
      }

      GlassSurface(
        shape = RoundedCornerShape(20.dp),
        alpha = 0.72f,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.SignalWifi4Bar,
                contentDescription = null,
                tint = signalColor,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Signal: $plainQuality (${ap.peakRssiDbm} dBm)",
                style = MaterialTheme.typography.titleSmall,
                color = signalColor,
              )
            }
            Text(
              text = "${ap.sightingsCount} sightings",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          LinearProgressIndicator(
            progress = { quality },
            color = signalColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            MetricColumn("Peak", "${SignalColors.plainQualityLabel(ap.peakRssiDbm)} (${ap.peakRssiDbm} dBm)")
            MetricColumn("Latest", "${ap.latestRssiDbm} dBm")
            MetricColumn("Average", "${ap.averageRssiDbm} dBm")
          }

          HorizontalDivider(color = GlassBorderColor)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            MetricColumn("First Seen", formatShortTime(ap.firstSeenMillis))
            MetricColumn("Last Seen", formatShortTime(ap.lastSeenMillis))
          }

          val coordText = when {
            ap.strongestCoordinate != null -> String.format(
              Locale.US,
              "%.6f, %.6f",
              ap.strongestCoordinate.latitude,
              ap.strongestCoordinate.longitude,
            )
            ap.strongestFloorPoint != null -> String.format(
              Locale.US,
              "Floor X: %d%%, Y: %d%%",
              (ap.strongestFloorPoint.xNorm * 100).toInt(),
              (ap.strongestFloorPoint.yNorm * 100).toInt(),
            )
            else -> "No coordinate recorded"
          }
          MetricColumn("Strongest Location", coordText)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { onIsolateOnHeatmap(ap.bssid) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inspector_isolate_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.Outlined.FilterCenterFocus, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Isolate This Wi-Fi on Heatmap")
        }

        FilledTonalButton(
          onClick = { onSetAsIndoorTarget(ap) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inspector_set_indoor_target_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.Outlined.HomeWork, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Set as Indoor Target Router")
        }

        OutlinedButton(
          onClick = {
            val specs = buildString {
              appendLine("SSID: ${ap.ssid}")
              appendLine("BSSID: ${ap.bssid}")
              appendLine("Vendor: ${ap.manufacturer}")
              appendLine("Band: ${ap.band.label} (${ap.frequencyMhz} MHz, Ch ${ap.primaryChannel}, ${ap.channelWidthMhz} MHz)")
              appendLine("Generation: ${ap.wifiGeneration.label}")
              appendLine("Security: ${ap.security.label} ${ap.capabilitiesRaw}")
              appendLine("RSSI Peak/Latest/Avg: ${ap.peakRssiDbm} / ${ap.latestRssiDbm} / ${ap.averageRssiDbm} dBm")
              appendLine("Sightings: ${ap.sightingsCount}")
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Wi-Fi AP Specs", specs))
            onCopiedToClipboard("Copied ${ap.bssid} & RF specs")
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("inspector_copy_specs_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.Outlined.ContentCopy, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Copy BSSID & Specs")
        }
      }
    }
  }
}

@Composable
private fun InspectorSpecPill(
  label: String,
  highlight: Boolean = false,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
  Surface(
    color = if (highlight) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    },
    border = BorderStroke(1.dp, GlassBorderColor),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (highlight) {
          MaterialTheme.colorScheme.onPrimaryContainer
        } else {
          MaterialTheme.colorScheme.onSurface
        },
      )
    }
  }
}

@Composable
private fun MetricColumn(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      fontFamily = JetBrainsMonoFontFamily,
    )
  }
}

private fun formatShortTime(millis: Long): String {
  val fmt = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
  return fmt.format(Date(millis))
}
