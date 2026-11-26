package com.frenchtoast.wifimapper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GlassBorderColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToastsTopAppBar(
  isDemoModeEnabled: Boolean,
  onToggleDemoMode: (Boolean) -> Unit,
  onResetDemoData: () -> Unit,
  onClearToBlankLiveDb: () -> Unit,
  onOpenPrivacyScreen: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Column {
    TopAppBar(
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
      ),
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.WifiFind,
              contentDescription = "Toasts Wifi Mapper icon",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .padding(7.dp)
                .size(19.dp),
            )
          }
          Text(
            text = "Toasts Wifi Mapper",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (isDemoModeEnabled) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("demo_badge_pill"),
            ) {
              Text(
                text = "DEMO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          }
        }
      },
      actions = {
        Box {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.testTag("top_menu_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.MoreVert,
              contentDescription = "Workspace and privacy options",
            )
          }
          DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(18.dp),
          ) {
            DropdownMenuItem(
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text("Demo Mode")
                  Spacer(Modifier.width(14.dp))
                  Switch(
                    checked = isDemoModeEnabled,
                    onCheckedChange = { enabled ->
                      menuExpanded = false
                      onToggleDemoMode(enabled)
                    },
                    modifier = Modifier
                      .scale(0.82f)
                      .testTag("demo_mode_switch"),
                  )
                }
              },
              leadingIcon = {
                Icon(Icons.Outlined.Science, contentDescription = null)
              },
              onClick = {
                menuExpanded = false
                onToggleDemoMode(!isDemoModeEnabled)
              },
              modifier = Modifier.testTag("menu_demo_mode_item"),
            )
            HorizontalDivider(
              color = GlassBorderColor,
              modifier = Modifier.padding(vertical = 4.dp),
            )
            DropdownMenuItem(
              text = { Text("Reset Demo Data") },
              leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onResetDemoData()
              },
            )
            DropdownMenuItem(
              text = { Text("Clear & Blank Live DB") },
              leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onClearToBlankLiveDb()
              },
            )
            DropdownMenuItem(
              text = { Text("Permissions & Privacy") },
              leadingIcon = { Icon(Icons.Outlined.PrivacyTip, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onOpenPrivacyScreen()
              },
            )
          }
        }
      },
    )
    HorizontalDivider(thickness = 1.dp, color = GlassBorderColor)
  }
}

@Composable
fun LivePermissionsGateDialog(
  shouldOpenSettingsFallback: Boolean,
  onGrantPermissions: () -> Unit,
  onOpenSettings: () -> Unit,
  onStayInDemoMode: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = {},
    properties = DialogProperties(
      dismissOnBackPress = false,
      dismissOnClickOutside = false,
    ),
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(26.dp),
    icon = {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(12.dp),
        )
      }
    },
    title = {
      Text(
        text = "Permissions Needed for Live Mode",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Live Wi-Fi scanning and GPS mapping cannot function without Location and Nearby Wi-Fi permissions until they are granted and allowed.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
          border = BorderStroke(1.dp, GlassBorderColor),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "All scans stay 100% offline in your local SQLite database — zero cloud sync or tracking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    },
    confirmButton = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Button(
          onClick = {
            if (shouldOpenSettingsFallback) onOpenSettings() else onGrantPermissions()
          },
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_gate_grant_button"),
        ) {
          Text(if (shouldOpenSettingsFallback) "Open App Settings" else "Grant Permissions")
        }
        OutlinedButton(
          onClick = onStayInDemoMode,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_gate_stay_demo_button"),
        ) {
          Text("Stay in Demo Mode")
        }
      }
    },
  )
}

@Composable
fun ScanThrottlingInfoDialog(
  scansUsed: Int,
  maxScans: Int,
  secondsUntilNextSlot: Int,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(24.dp),
    icon = {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = "Android Wi-Fi Scan Throttling",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Android 9+ limits foreground apps to $maxScans active Wi-Fi scans per 2 minutes (used: $scansUsed/$maxScans" +
            if (secondsUntilNextSlot > 0) ", next slot in ${secondsUntilNextSlot}s)." else ").",
          style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
          border = BorderStroke(1.dp, GlassBorderColor),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "Faster sweeps in Developer Options:",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "1. Open Android Settings > System > Developer options\n" +
                "2. Find \"Wi-Fi scan throttling\"\n" +
                "3. Toggle it OFF for unrestricted local surveys.",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
        Text(
          text = "When throttled, Toasts Wifi Mapper safely reads the latest system scan cache without dropping your GPS track.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dismiss_throttle_dialog_button"),
      ) {
        Text("Got it")
      }
    },
  )
}
