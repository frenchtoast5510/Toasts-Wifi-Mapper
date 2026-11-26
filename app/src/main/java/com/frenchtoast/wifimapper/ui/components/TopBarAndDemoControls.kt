package com.frenchtoast.wifimapper.ui.components

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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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

  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.WifiFind,
            contentDescription = "Toasts Wifi Mapper icon",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
              .padding(6.dp)
              .size(20.dp),
          )
        }
        Column {
          Text(
            text = "Toasts Wifi Mapper",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = if (isDemoModeEnabled) "Demo Dataset Active" else "Live Local DB",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDemoModeEnabled) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.tertiary
            },
          )
        }
      }
    },
    actions = {
      Surface(
        color = if (isDemoModeEnabled) {
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
          MaterialTheme.colorScheme.surfaceContainerHigh
        },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(end = 4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        ) {
          Text(
            text = "Demo",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(Modifier.width(6.dp))
          Switch(
            checked = isDemoModeEnabled,
            onCheckedChange = onToggleDemoMode,
            modifier = Modifier.testTag("demo_mode_switch"),
            colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
              checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
          )
        }
      }

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
        ) {
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
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
