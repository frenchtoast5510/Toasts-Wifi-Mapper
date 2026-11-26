package com.frenchtoast.wifimapper.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SignalColors
import com.frenchtoast.wifimapper.domain.model.ChannelCongestionBin
import com.frenchtoast.wifimapper.domain.model.NetworkSortOrder
import com.frenchtoast.wifimapper.domain.model.WifiAccessPointSummary
import com.frenchtoast.wifimapper.domain.model.WifiBand
import com.frenchtoast.wifimapper.platform.android.SafExporter
import com.frenchtoast.wifimapper.ui.components.GlassCard
import com.frenchtoast.wifimapper.ui.components.GlassSurface
import com.frenchtoast.wifimapper.ui.state.ExportFormat
import com.frenchtoast.wifimapper.ui.state.WifiMapperUiState

@Composable
fun AnalyzerExportScreen(
  uiState: WifiMapperUiState,
  onSearchQueryChange: (String) -> Unit,
  onSelectSortOrder: (NetworkSortOrder) -> Unit,
  onSelectChannelBand: (WifiBand) -> Unit,
  onInspectAp: (WifiAccessPointSummary) -> Unit,
  onLocateApOnMap: (WifiAccessPointSummary) -> Unit,
  onSelectWalk: (Long) -> Unit,
  onCreateWalk: (String) -> Unit,
  onDeleteWalk: (Long) -> Unit,
  onSelectSurvey: (Long) -> Unit,
  onDeleteSurvey: (Long) -> Unit,
  onPrepareExport: (ExportFormat) -> Pair<String, String>,
  onNotify: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
  var showNewWalkDialog by remember { mutableStateOf(false) }

  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("*/*"),
  ) { uri ->
    val format = pendingExportFormat ?: return@rememberLauncherForActivityResult
    if (uri != null) {
      val (name, payload) = onPrepareExport(format)
      SafExporter.writeToUri(context, uri, payload)
        .onSuccess { onNotify("Saved $name") }
        .onFailure { onNotify("Export failed: ${it.message}") }
    }
    pendingExportFormat = null
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      ChannelCongestionCard(
        selectedBand = uiState.channelBand,
        bins = uiState.channelBins,
        onSelectBand = onSelectChannelBand,
      )
    }

    item {
      ExportDatasetsCard(
        readingsCount = uiState.activeWalkReadings.size,
        onSaveSaf = { format ->
          pendingExportFormat = format
          val (defaultName, _) = onPrepareExport(format)
          createDocumentLauncher.launch(defaultName)
        },
        onShareSheet = { format ->
          val (name, payload) = onPrepareExport(format)
          SafExporter.shareViaSheet(context, name, format.mimeType, payload)
            .onFailure { onNotify("Share failed: ${it.message}") }
        },
      )
    }

    item {
      SavedWorkspacesCard(
        uiState = uiState,
        onSelectWalk = onSelectWalk,
        onCreateWalk = { showNewWalkDialog = true },
        onDeleteWalk = onDeleteWalk,
        onSelectSurvey = onSelectSurvey,
        onDeleteSurvey = onDeleteSurvey,
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Discovered Networks (${uiState.filteredSummaries.size})",
          style = MaterialTheme.typography.titleMedium,
        )
        GlassSurface(
          shape = RoundedCornerShape(20.dp),
          alpha = 0.72f,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedTextField(
            value = uiState.filterState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Filter by SSID, BSSID, or manufacturer…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
              if (uiState.filterState.searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(Icons.Outlined.Close, contentDescription = "Clear filter")
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
              .testTag("analyzer_search_input"),
          )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(NetworkSortOrder.entries) { order ->
            FilterChip(
              selected = uiState.sortOrder == order,
              onClick = { onSelectSortOrder(order) },
              label = { Text(order.label) },
              shape = RoundedCornerShape(20.dp),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = uiState.sortOrder == order,
                borderColor = GlassBorderColor,
              ),
              colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.84f),
              ),
              modifier = Modifier.testTag("sort_${order.name}"),
            )
          }
        }
      }
    }

    if (uiState.filteredSummaries.isEmpty()) {
      item {
        GlassSurface(
          shape = RoundedCornerShape(20.dp),
          alpha = 0.72f,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "No Wi-Fi networks match the active filters yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
          )
        }
      }
    } else {
      items(uiState.filteredSummaries, key = { it.bssid }) { ap ->
        NetworkSummaryRow(
          ap = ap,
          onRowClick = {
            onInspectAp(ap)
            onLocateApOnMap(ap)
          },
          onCenterMapClick = { onLocateApOnMap(ap) },
        )
      }
    }
  }

  if (showNewWalkDialog) {
    NewWalkDialog(
      onDismiss = { showNewWalkDialog = false },
      onConfirm = { name ->
        showNewWalkDialog = false
        onCreateWalk(name)
      },
    )
  }
}

@Composable
private fun ChannelCongestionCard(
  selectedBand: WifiBand,
  bins: List<ChannelCongestionBin>,
  onSelectBand: (WifiBand) -> Unit,
) {
  val bandOptions = listOf(WifiBand.BAND_2_4_GHZ, WifiBand.BAND_5_GHZ, WifiBand.BAND_6_GHZ)
  GlassCard(
    shape = RoundedCornerShape(22.dp),
    alpha = 0.72f,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Channel Congestion Analyzer", style = MaterialTheme.typography.titleMedium)

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        bandOptions.forEachIndexed { idx, band ->
          SegmentedButton(
            selected = selectedBand == band,
            onClick = { onSelectBand(band) },
            shape = SegmentedButtonDefaults.itemShape(idx, bandOptions.size),
            modifier = Modifier.testTag("channel_band_${band.name}"),
          ) {
            Text(band.label)
          }
        }
      }

      val maxScore = (bins.maxOfOrNull { it.congestionScore } ?: 1f).coerceAtLeast(1f)
      val barColor = when (selectedBand) {
        WifiBand.BAND_2_4_GHZ -> SignalColors.Band24
        WifiBand.BAND_5_GHZ -> SignalColors.Band5
        WifiBand.BAND_6_GHZ -> SignalColors.Band6
        WifiBand.UNKNOWN -> MaterialTheme.colorScheme.primary
      }
      val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        ) {
          if (bins.isEmpty()) return@Canvas
          val slotW = size.width / bins.size
          val barW = (slotW * 0.64f).coerceAtLeast(4f)
          bins.forEachIndexed { idx, bin ->
            val left = idx * slotW + (slotW - barW) / 2f
            drawRoundRect(
              color = trackColor,
              topLeft = Offset(left, 0f),
              size = Size(barW, size.height),
              cornerRadius = CornerRadius(6f, 6f),
            )
            if (bin.congestionScore > 0f) {
              val h = (bin.congestionScore / maxScore).coerceIn(0.08f, 1f) * size.height
              val fill = if (bin.congestionScore > maxScore * 0.72f) {
                SignalColors.WeakCrimson
              } else {
                barColor
              }
              drawRoundRect(
                color = fill,
                topLeft = Offset(left, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(6f, 6f),
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          val stride = if (bins.size > 15) 3 else 1
          bins.forEachIndexed { idx, bin ->
            if (idx % stride == 0 || idx == bins.lastIndex) {
              Text(
                text = "${bin.channel}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = if (bin.networkCount > 0) {
                  MaterialTheme.colorScheme.onSurface
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
            }
          }
        }
      }

      val quietest = bins.minByOrNull { it.congestionScore }
      val busiest = bins.maxByOrNull { it.congestionScore }
      if (quietest != null && busiest != null) {
        Text(
          text = "Cleanest: Ch ${quietest.channel} · Busiest: Ch ${busiest.channel} (${busiest.networkCount} direct APs)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ExportDatasetsCard(
  readingsCount: Int,
  onSaveSaf: (ExportFormat) -> Unit,
  onShareSheet: (ExportFormat) -> Unit,
) {
  GlassCard(
    shape = RoundedCornerShape(22.dp),
    alpha = 0.72f,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Offline Dataset Exports", style = MaterialTheme.typography.titleMedium)
        Text(
          text = "$readingsCount logs",
          style = MaterialTheme.typography.labelSmall,
          fontFamily = JetBrainsMonoFontFamily,
        )
      }

      ExportFormat.entries.forEach { format ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = format.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { onSaveSaf(format) },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              border = BorderStroke(1.dp, GlassBorderColor),
              modifier = Modifier.testTag("export_saf_${format.name}"),
            ) {
              Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Save")
            }
            FilledTonalButton(
              onClick = { onShareSheet(format) },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.testTag("export_share_${format.name}"),
            ) {
              Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Share")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SavedWorkspacesCard(
  uiState: WifiMapperUiState,
  onSelectWalk: (Long) -> Unit,
  onCreateWalk: () -> Unit,
  onDeleteWalk: (Long) -> Unit,
  onSelectSurvey: (Long) -> Unit,
  onDeleteSurvey: (Long) -> Unit,
) {
  GlassCard(
    shape = RoundedCornerShape(22.dp),
    alpha = 0.72f,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Saved Walks & Surveys", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onCreateWalk) {
          Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("New Walk")
        }
      }

      uiState.walks.forEach { walk ->
        val isActive = uiState.activeWalk?.id == walk.id
        Surface(
          color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
          } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
          },
          border = BorderStroke(1.dp, GlassBorderColor),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectWalk(walk.id) },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(walk.name, style = MaterialTheme.typography.labelLarge)
              Text(
                text = if (walk.isDemo) "Outdoor Walk (Demo)" else "Outdoor Walk",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (uiState.walks.size > 1) {
              IconButton(onClick = { onDeleteWalk(walk.id) }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete walk")
              }
            }
          }
        }
      }

      uiState.surveys.forEach { survey ->
        val isActive = uiState.activeSurvey?.id == survey.id
        Surface(
          color = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
          } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
          },
          border = BorderStroke(1.dp, GlassBorderColor),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectSurvey(survey.id) },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(survey.name, style = MaterialTheme.typography.labelLarge)
              Text(
                text = if (survey.isBuiltInSample) "Indoor Survey · Sample Plan" else "Indoor Survey · Custom Image",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (uiState.surveys.size > 1) {
              IconButton(onClick = { onDeleteSurvey(survey.id) }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete survey")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NetworkSummaryRow(
  ap: WifiAccessPointSummary,
  onRowClick: () -> Unit,
  onCenterMapClick: () -> Unit,
) {
  val rssiColor = SignalColors.forRssi(ap.peakRssiDbm)
  val qualityWord = SignalColors.plainQualityLabel(ap.peakRssiDbm)
  GlassCard(
    shape = RoundedCornerShape(20.dp),
    alpha = 0.72f,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onRowClick)
      .testTag("network_row_${ap.bssid}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(rssiColor.copy(alpha = 0.16f)),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "${ap.peakRssiDbm}",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = rssiColor,
          )
          Text(
            text = qualityWord,
            style = MaterialTheme.typography.labelSmall,
            color = rssiColor,
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = ap.ssid,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${ap.bssid} · ${ap.manufacturer}",
          style = MaterialTheme.typography.bodySmall,
          fontFamily = JetBrainsMonoFontFamily,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${ap.band.label} · Ch ${ap.primaryChannel} (${ap.channelWidthMhz} MHz) · ${ap.security.label}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (ap.strongestCoordinate != null) {
        IconButton(
          onClick = onCenterMapClick,
          modifier = Modifier.testTag("locate_map_${ap.bssid}"),
        ) {
          Icon(
            Icons.Outlined.LocationOn,
            contentDescription = "Center map on ${ap.ssid}",
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    }
  }
}

@Composable
private fun NewWalkDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember { mutableStateOf("Afternoon Field Sweep") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Outdoor Walk") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Walk Name") },
        singleLine = true,
      )
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(name) }) { Text("Create") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
