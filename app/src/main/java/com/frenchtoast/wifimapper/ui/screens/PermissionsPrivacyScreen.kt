package com.frenchtoast.wifimapper.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderColor
import com.example.ui.theme.SignalColors
import com.frenchtoast.wifimapper.platform.android.PermissionHelper
import com.frenchtoast.wifimapper.platform.android.PermissionStatusSnapshot
import com.frenchtoast.wifimapper.ui.components.GlassCard
import com.frenchtoast.wifimapper.ui.components.GlassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsPrivacyScreen(
  snapshot: PermissionStatusSnapshot,
  onPermissionsUpdated: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current

  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) {
    onPermissionsUpdated()
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Column {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
          ),
          title = { Text("Permissions & Privacy") },
          navigationIcon = {
            IconButton(
              onClick = onBack,
              modifier = Modifier.testTag("privacy_back_button"),
            ) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
          },
        )
        HorizontalDivider(thickness = 1.dp, color = GlassBorderColor)
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      GlassSurface(
        shape = RoundedCornerShape(20.dp),
        alpha = 0.78f,
        borderColor = SignalColors.StrongGreen.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            Icons.Outlined.Shield,
            contentDescription = null,
            tint = SignalColors.StrongGreen,
            modifier = Modifier.size(28.dp),
          )
          Column {
            Text(
              text = "100% Local Offline Storage",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Zero cloud accounts, zero analytics, zero telemetry, zero ads. All Wi-Fi scans, GPS tracks, and floor-plan surveys stay inside your on-device Room SQLite database.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      PermissionCard(
        title = "ACCESS_FINE_LOCATION",
        granted = snapshot.fineLocationGranted,
        description = "Required by Android to read GPS coordinates during outdoor walks and to allow Wi-Fi BSSID/SSID scan results to be delivered to foreground apps.",
      )

      PermissionCard(
        title = "NEARBY_WIFI_DEVICES & CHANGE_WIFI_STATE",
        granted = snapshot.nearbyWifiGranted && snapshot.wifiStateGranted && snapshot.changeWifiStateGranted,
        description = "Required on Android 13+ (API 33+) to discover nearby Wi-Fi access points and trigger active Wi-Fi sweeps for indoor and outdoor mapping.",
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = { launcher.launch(PermissionHelper.requiredRuntimePermissions()) },
          modifier = Modifier
            .weight(1f)
            .testTag("grant_permissions_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Grant Permissions")
        }

        OutlinedButton(
          onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
          },
          border = BorderStroke(1.dp, GlassBorderColor),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("App Settings")
        }
      }

      GlassCard(
        shape = RoundedCornerShape(20.dp),
        alpha = 0.72f,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Platform Notes & iOS Roadmap", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "• Android Scan Throttling: Android 9+ restricts foreground Wi-Fi scans to 4 calls per 2 minutes unless \"Wi-Fi scan throttling\" is disabled in Developer Options.\n" +
              "• Map Tiles: Only OpenStreetMap tiles fetch over the network when online; scanning, heatmaps, SQLite storage, and exports work offline.\n" +
              "• Future iOS KMP Port: Apple restricts raw Wi-Fi scanning via CoreLocation and NEHotspotHelper (special entitlement required), whereas Android allows full local multi-AP RF sweeps.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun PermissionCard(
  title: String,
  granted: Boolean,
  description: String,
) {
  GlassCard(
    shape = RoundedCornerShape(18.dp),
    alpha = 0.72f,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Icon(
          imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
          contentDescription = if (granted) "Granted" else "Not granted",
          tint = if (granted) SignalColors.StrongGreen else SignalColors.FairAmber,
        )
      }
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
