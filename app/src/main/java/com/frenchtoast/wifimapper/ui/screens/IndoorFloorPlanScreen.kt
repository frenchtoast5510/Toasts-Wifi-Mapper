package com.frenchtoast.wifimapper.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorderColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SignalColors
import com.frenchtoast.wifimapper.domain.math.SignalMath
import com.frenchtoast.wifimapper.domain.model.IndoorHeatmapCell
import com.frenchtoast.wifimapper.domain.model.IndoorSample
import com.frenchtoast.wifimapper.domain.model.RoomCoverageBreakdown
import com.frenchtoast.wifimapper.domain.model.RouterPin
import com.frenchtoast.wifimapper.domain.model.WifiAccessPointSummary
import com.frenchtoast.wifimapper.platform.android.FloorPlanImageManager
import com.frenchtoast.wifimapper.ui.components.GlassCard
import com.frenchtoast.wifimapper.ui.components.GlassSurface
import com.frenchtoast.wifimapper.ui.components.SignalQualityLegendPill
import com.frenchtoast.wifimapper.ui.state.IndoorInteractionMode
import com.frenchtoast.wifimapper.ui.state.WifiMapperUiState
import kotlin.math.roundToInt

@Composable
fun IndoorFloorPlanScreen(
  uiState: WifiMapperUiState,
  onSetMode: (IndoorInteractionMode) -> Unit,
  onSelectRun: (Long) -> Unit,
  onCreateRun: (String) -> Unit,
  onPlaceOrMoveRouterPin: (Float, Float, Long?) -> Unit,
  onCaptureSample: (Float, Float) -> Unit,
  onSampleClicked: (IndoorSample) -> Unit,
  onImportFloorPlan: (android.net.Uri, String) -> Unit,
  onOpenComparison: (Boolean) -> Unit,
  onSelectComparisonRun: (Long) -> Unit,
  onSetTargetAp: (WifiAccessPointSummary?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeSurvey = uiState.activeSurvey
  val floorPlanBitmap: ImageBitmap = remember(activeSurvey?.id, activeSurvey?.imageUri, activeSurvey?.isBuiltInSample) {
    if (activeSurvey == null || activeSurvey.isBuiltInSample || activeSurvey.imageUri == null) {
      FloorPlanImageManager.getBuiltInSampleFloorPlan()
    } else {
      FloorPlanImageManager.loadImageBitmapFromPath(activeSurvey.imageUri)
        ?: FloorPlanImageManager.getBuiltInSampleFloorPlan()
    }
  }

  var showNewRunDialog by remember { mutableStateOf(false) }
  var showTargetPickerDialog by remember { mutableStateOf(false) }
  var showImportNameDialog by remember { mutableStateOf<android.net.Uri?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      showImportNameDialog = uri
    }
  }

  val safLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      showImportNameDialog = uri
    }
  }

  var zoom by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  val bestRunId: Long? = remember(
    uiState.placementRuns,
    uiState.activeRun?.id,
    uiState.comparisonRun?.id,
    uiState.activeRoomCoverage,
    uiState.comparisonRoomCoverage,
  ) {
    if (uiState.placementRuns.size < 2 || uiState.activeRun == null || uiState.comparisonRun == null) {
      null
    } else {
      val overallA = SignalMath.computeOverallCoverage(uiState.activeRoomCoverage)
      val overallB = SignalMath.computeOverallCoverage(uiState.comparisonRoomCoverage)
      if (overallA.strongPercent >= overallB.strongPercent) uiState.activeRun.id else uiState.comparisonRun.id
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    GlassSurface(
      shape = RoundedCornerShape(18.dp),
      alpha = 0.68f,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("indoor_tip_banner"),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Lightbulb,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "Tap 'Sample', walk to a room, and tap the map to test signal",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = activeSurvey?.name ?: "Floor-Plan Survey",
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "Target: ${uiState.indoorTargetSsid ?: "All Visible APs"}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary,
          modifier = Modifier
            .clickable { showTargetPickerDialog = true }
            .padding(vertical = 2.dp),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
          },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          border = BorderStroke(1.dp, GlassBorderColor),
          modifier = Modifier.testTag("indoor_import_photo_button"),
        ) {
          Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Photo")
        }
        IconButton(
          onClick = { safLauncher.launch(arrayOf("image/*")) },
          modifier = Modifier.testTag("indoor_import_saf_button"),
        ) {
          Icon(Icons.Outlined.FolderOpen, contentDescription = "Import floor plan via Files")
        }
      }
    }

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items(uiState.placementRuns, key = { it.id }) { run ->
        val isBest = bestRunId == run.id
        FilterChip(
          selected = uiState.activeRun?.id == run.id,
          onClick = { onSelectRun(run.id) },
          label = {
            Text(if (isBest) "${run.name} · Best" else run.name)
          },
          leadingIcon = if (isBest) {
            {
              Icon(
                Icons.Outlined.EmojiEvents,
                contentDescription = "Best coverage",
                tint = SignalColors.StrongGreen,
                modifier = Modifier.size(16.dp),
              )
            }
          } else {
            null
          },
          shape = RoundedCornerShape(20.dp),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = uiState.activeRun?.id == run.id,
            borderColor = if (isBest) SignalColors.StrongGreen.copy(alpha = 0.55f) else GlassBorderColor,
          ),
          colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
          ),
          modifier = Modifier.testTag("indoor_run_chip_${run.id}"),
        )
      }
      item {
        FilledTonalButton(
          onClick = { showNewRunDialog = true },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("indoor_new_run_button"),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("New Run")
        }
      }
      if (uiState.placementRuns.size >= 2) {
        item {
          Button(
            onClick = { onOpenComparison(true) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("indoor_compare_runs_button"),
          ) {
            Icon(Icons.Outlined.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Compare")
          }
        }
      }
    }

    GlassSurface(
      shape = RoundedCornerShape(20.dp),
      alpha = 0.72f,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(6.dp),
      ) {
        IndoorInteractionMode.entries.forEachIndexed { index, mode ->
          SegmentedButton(
            selected = uiState.indoorMode == mode,
            onClick = { onSetMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(
              index = index,
              count = IndoorInteractionMode.entries.size,
            ),
            icon = {
              val icon = when (mode) {
                IndoorInteractionMode.INSPECT_PAN -> Icons.Outlined.PanTool
                IndoorInteractionMode.PLACE_ROUTER -> Icons.Outlined.PushPin
                IndoorInteractionMode.SAMPLE_HERE -> Icons.Outlined.TouchApp
              }
              Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("indoor_mode_${mode.name}"),
          ) {
            Text(
              text = when (mode) {
                IndoorInteractionMode.INSPECT_PAN -> "Pan"
                IndoorInteractionMode.PLACE_ROUTER -> "Place Pin"
                IndoorInteractionMode.SAMPLE_HERE -> "Sample"
              },
              maxLines = 1,
            )
          }
        }
      }
    }

    val aspect = floorPlanBitmap.width.toFloat() / floorPlanBitmap.height.coerceAtLeast(1).toFloat()
    GlassCard(
      shape = RoundedCornerShape(24.dp),
      alpha = 0.72f,
      modifier = Modifier.fillMaxWidth(),
    ) {
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(aspect.coerceIn(0.8f, 1.9f))
          .clipToBounds()
          .background(MaterialTheme.colorScheme.surfaceContainerLow),
      ) {
        val containerW = with(LocalDensity.current) { maxWidth.toPx() }
        val containerH = with(LocalDensity.current) { maxHeight.toPx() }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(uiState.indoorMode, zoom, panOffset) {
              detectTransformGestures { _, pan, gestureZoom, _ ->
                val newZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                val maxPanX = (containerW * (newZoom - 1f)) / 2f
                val maxPanY = (containerH * (newZoom - 1f)) / 2f
                zoom = newZoom
                panOffset = Offset(
                  x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                  y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY),
                )
              }
            }
            .pointerInput(uiState.indoorMode, uiState.activeRun?.id, zoom, panOffset) {
              detectTapGestures { tapOffset ->
                val norm = screenToNormalized(tapOffset, containerW, containerH, zoom, panOffset)
                when (uiState.indoorMode) {
                  IndoorInteractionMode.PLACE_ROUTER -> {
                    onPlaceOrMoveRouterPin(norm.x, norm.y, null)
                  }
                  IndoorInteractionMode.SAMPLE_HERE -> {
                    onCaptureSample(norm.x, norm.y)
                  }
                  IndoorInteractionMode.INSPECT_PAN -> {
                    val nearest = findNearestSample(norm, uiState.activeRunSamples)
                    if (nearest != null) onSampleClicked(nearest)
                  }
                }
              }
            }
            .graphicsLayer {
              scaleX = zoom
              scaleY = zoom
              translationX = panOffset.x
              translationY = panOffset.y
            },
        ) {
          Image(
            bitmap = floorPlanBitmap,
            contentDescription = "Indoor floor plan layout",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )

          SmoothIndoorHeatmapCanvas(
            cells = uiState.activeRunHeatmap,
            modifier = Modifier.fillMaxSize(),
          )

          val groupedSpots = remember(uiState.activeRunSamples) {
            uiState.activeRunSamples.groupBy {
              (it.xNorm * 100).roundToInt() to (it.yNorm * 100).roundToInt()
            }.values.mapNotNull { list -> list.maxByOrNull { it.rssiDbm } }
          }

          groupedSpots.forEach { sample ->
            val dotPxX = sample.xNorm * containerW
            val dotPxY = sample.yNorm * containerH
            val dotColor = SignalColors.forRssi(sample.rssiDbm)
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .offset { IntOffset((dotPxX - 18).roundToInt(), (dotPxY - 18).roundToInt()) }
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onSampleClicked(sample) },
            ) {
              Surface(
                color = dotColor,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.background),
                shape = CircleShape,
                shadowElevation = 3.dp,
                modifier = Modifier.size(15.dp),
              ) {}
            }
          }

          uiState.activeRunPins.forEach { pin ->
            DraggableRouterMarker(
              pin = pin,
              containerW = containerW,
              containerH = containerH,
              draggable = uiState.indoorMode == IndoorInteractionMode.PLACE_ROUTER,
              onMoved = { nx, ny -> onPlaceOrMoveRouterPin(nx, ny, pin.id) },
            )
          }
        }

        SignalQualityLegendPill(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(10.dp),
        )

        if (zoom > 1.05f || panOffset != Offset.Zero) {
          GlassSurface(
            shape = RoundedCornerShape(14.dp),
            alpha = 0.85f,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(10.dp)
              .clickable {
                zoom = 1f
                panOffset = Offset.Zero
              },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(Icons.Outlined.ZoomOutMap, contentDescription = null, modifier = Modifier.size(14.dp))
              Text("${String.format("%.1f", zoom)}x Reset", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = when (uiState.indoorMode) {
            IndoorInteractionMode.INSPECT_PAN -> "Pinch to zoom · Tap any sample dot to see dBm"
            IndoorInteractionMode.PLACE_ROUTER -> "Tap floor plan to drop a pin · Drag pin to move"
            IndoorInteractionMode.SAMPLE_HERE -> "Walk to a spot & tap the floor plan to log signal"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "${uiState.activeRunSamples.size} pts",
          style = MaterialTheme.typography.labelSmall,
          fontFamily = JetBrainsMonoFontFamily,
        )
      }
    }

    if (uiState.activeRoomCoverage.isNotEmpty()) {
      val overall = SignalMath.computeOverallCoverage(uiState.activeRoomCoverage)
      val activeIsBest = bestRunId != null && bestRunId == uiState.activeRun?.id
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text("Coverage Breakdown", style = MaterialTheme.typography.titleMedium)
              if (activeIsBest) {
                BestCoverageBadge()
              }
            }
            Text(
              text = "${overall.strongPercent}% Great · ${overall.averageDbm} dBm",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = JetBrainsMonoFontFamily,
              color = SignalColors.forRssi(overall.averageDbm),
            )
          }
          uiState.activeRoomCoverage.forEach { room ->
            RoomCoverageBarRow(room)
          }
        }
      }
    }
  }

  if (showNewRunDialog) {
    NewRunDialog(
      onDismiss = { showNewRunDialog = false },
      onConfirm = { name ->
        showNewRunDialog = false
        onCreateRun(name)
      },
    )
  }

  showImportNameDialog?.let { pendingUri ->
    ImportFloorPlanDialog(
      onDismiss = { showImportNameDialog = null },
      onConfirm = { name ->
        showImportNameDialog = null
        onImportFloorPlan(pendingUri, name)
      },
    )
  }

  if (showTargetPickerDialog) {
    TargetApPickerDialog(
      aps = uiState.allSummaries,
      selectedBssid = uiState.indoorTargetBssid,
      onSelect = {
        showTargetPickerDialog = false
        onSetTargetAp(it)
      },
      onDismiss = { showTargetPickerDialog = false },
    )
  }

  if (uiState.isComparisonOpen && uiState.activeRun != null && uiState.comparisonRun != null) {
    RunComparisonBottomSheet(
      uiState = uiState,
      floorPlanBitmap = floorPlanBitmap,
      onSelectComparisonRun = onSelectComparisonRun,
      onDismiss = { onOpenComparison(false) },
    )
  }
}

@Composable
private fun BestCoverageBadge() {
  Surface(
    color = SignalColors.StrongGreen.copy(alpha = 0.2f),
    border = BorderStroke(1.dp, SignalColors.StrongGreen.copy(alpha = 0.55f)),
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier.testTag("best_coverage_badge"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
      Icon(
        Icons.Outlined.EmojiEvents,
        contentDescription = null,
        tint = SignalColors.StrongGreen,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = "Best Coverage",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = SignalColors.StrongGreen,
      )
    }
  }
}

@Composable
private fun SmoothIndoorHeatmapCanvas(
  cells: List<IndoorHeatmapCell>,
  modifier: Modifier = Modifier,
) {
  val cols = 36
  val rows = 26
  val heatmapBitmap = remember(cells) {
    if (cells.size != cols * rows) {
      null
    } else {
      val pixels = IntArray(cells.size) { idx ->
        SignalColors.heatmapArgb(cells[idx].rssiDbm, alpha = 122)
      }
      Bitmap.createBitmap(pixels, cols, rows, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
  }

  Canvas(modifier = modifier) {
    val bmp = heatmapBitmap ?: return@Canvas
    drawImage(
      image = bmp,
      dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
      filterQuality = FilterQuality.High,
    )
  }
}

@Composable
private fun DraggableRouterMarker(
  pin: RouterPin,
  containerW: Float,
  containerH: Float,
  draggable: Boolean,
  onMoved: (Float, Float) -> Unit,
) {
  var dragOffset by remember(pin.id, pin.xNorm, pin.yNorm) {
    mutableStateOf(Offset(pin.xNorm * containerW, pin.yNorm * containerH))
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .offset { IntOffset((dragOffset.x - 24).roundToInt(), (dragOffset.y - 24).roundToInt()) }
      .pointerInput(pin.id, draggable, containerW, containerH) {
        if (!draggable) return@pointerInput
        detectDragGestures(
          onDrag = { change, dragAmount ->
            change.consume()
            dragOffset = Offset(
              x = (dragOffset.x + dragAmount.x).coerceIn(0f, containerW),
              y = (dragOffset.y + dragAmount.y).coerceIn(0f, containerH),
            )
          },
          onDragEnd = {
            onMoved(
              (dragOffset.x / containerW).coerceIn(0.02f, 0.98f),
              (dragOffset.y / containerH).coerceIn(0.02f, 0.98f),
            )
          },
        )
      },
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.background),
      shape = CircleShape,
      shadowElevation = 6.dp,
      modifier = Modifier.size(34.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          Icons.Outlined.Router,
          contentDescription = pin.label,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    GlassSurface(
      shape = RoundedCornerShape(8.dp),
      alpha = 0.85f,
      shadowElevation = 2.dp,
    ) {
      Text(
        text = pin.label,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }
  }
}

@Composable
fun RoomCoverageBarRow(room: RoomCoverageBreakdown) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(room.roomName, style = MaterialTheme.typography.labelLarge)
      Text(
        text = "${room.strongPercent}% Great · ${room.fairPercent}% Okay · ${room.deadZonePercent}% Weak",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = JetBrainsMonoFontFamily,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(9.dp)
        .clip(RoundedCornerShape(5.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
      if (room.strongPercent > 0) {
        Box(
          modifier = Modifier
            .weight(room.strongPercent.toFloat())
            .height(9.dp)
            .background(SignalColors.StrongGreen),
        )
      }
      if (room.fairPercent > 0) {
        Box(
          modifier = Modifier
            .weight(room.fairPercent.toFloat())
            .height(9.dp)
            .background(SignalColors.FairAmber),
        )
      }
      if (room.deadZonePercent > 0) {
        Box(
          modifier = Modifier
            .weight(room.deadZonePercent.toFloat())
            .height(9.dp)
            .background(SignalColors.WeakCrimson),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunComparisonBottomSheet(
  uiState: WifiMapperUiState,
  floorPlanBitmap: ImageBitmap,
  onSelectComparisonRun: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val runA = uiState.activeRun ?: return
  val runB = uiState.comparisonRun ?: return
  val overallA = SignalMath.computeOverallCoverage(uiState.activeRoomCoverage)
  val overallB = SignalMath.computeOverallCoverage(uiState.comparisonRoomCoverage)
  val aspect = floorPlanBitmap.width.toFloat() / floorPlanBitmap.height.coerceAtLeast(1).toFloat()
  val aWins = overallA.strongPercent >= overallB.strongPercent

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text("Placement Run Comparison", style = MaterialTheme.typography.headlineSmall)

      if (uiState.placementRuns.size > 2) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(uiState.placementRuns.filter { it.id != runA.id }) { candidate ->
            FilterChip(
              selected = candidate.id == runB.id,
              onClick = { onSelectComparisonRun(candidate.id) },
              label = { Text("vs ${candidate.name}") },
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ComparisonMiniMapCard(
          title = runA.name,
          isBest = aWins,
          overall = overallA,
          floorPlanBitmap = floorPlanBitmap,
          aspect = aspect,
          cells = uiState.activeRunHeatmap,
          modifier = Modifier.weight(1f),
        )
        ComparisonMiniMapCard(
          title = runB.name,
          isBest = !aWins,
          overall = overallB,
          floorPlanBitmap = floorPlanBitmap,
          aspect = aspect,
          cells = uiState.comparisonHeatmap,
          modifier = Modifier.weight(1f),
        )
      }

      val winner = if (aWins) runA.name else runB.name
      GlassSurface(
        shape = RoundedCornerShape(16.dp),
        alpha = 0.8f,
        borderColor = SignalColors.StrongGreen.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = SignalColors.StrongGreen,
          )
          Text(
            text = "Best Coverage: $winner",
            style = MaterialTheme.typography.titleSmall,
            color = SignalColors.StrongGreen,
          )
        }
      }

      Text("Room-by-Room Comparison", style = MaterialTheme.typography.titleMedium)
      uiState.activeRoomCoverage.forEachIndexed { idx, roomA ->
        val roomB = uiState.comparisonRoomCoverage.getOrNull(idx)
        GlassCard(
          shape = RoundedCornerShape(16.dp),
          alpha = 0.72f,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(roomA.roomName, style = MaterialTheme.typography.titleSmall)
            Text(
              text = "${runA.name}: ${roomA.strongPercent}% Great (${roomA.averageDbm} dBm)",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = JetBrainsMonoFontFamily,
            )
            if (roomB != null) {
              Text(
                text = "${runB.name}: ${roomB.strongPercent}% Great (${roomB.averageDbm} dBm)",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ComparisonMiniMapCard(
  title: String,
  isBest: Boolean,
  overall: RoomCoverageBreakdown,
  floorPlanBitmap: ImageBitmap,
  aspect: Float,
  cells: List<IndoorHeatmapCell>,
  modifier: Modifier = Modifier,
) {
  GlassCard(
    shape = RoundedCornerShape(18.dp),
    alpha = 0.72f,
    borderColor = if (isBest) SignalColors.StrongGreen.copy(alpha = 0.6f) else GlassBorderColor,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (isBest) {
        BestCoverageBadge()
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(aspect.coerceIn(0.9f, 1.8f))
          .clip(RoundedCornerShape(12.dp)),
      ) {
        Image(
          bitmap = floorPlanBitmap,
          contentDescription = title,
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )
        SmoothIndoorHeatmapCanvas(cells = cells, modifier = Modifier.fillMaxSize())
      }
      Text(
        text = "${overall.strongPercent}% Great · ${overall.deadZonePercent}% Weak",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun NewRunDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember { mutableStateOf("Spot C - Upstairs Desk") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Placement Run") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Run Name") },
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

@Composable
private fun ImportFloorPlanDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember { mutableStateOf("My Floor Plan") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Name Floor-Plan Survey") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Survey Name") },
        singleLine = true,
      )
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(name) }) { Text("Import") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun TargetApPickerDialog(
  aps: List<WifiAccessPointSummary>,
  selectedBssid: String?,
  onSelect: (WifiAccessPointSummary?) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Indoor Target Router") },
    text = {
      LazyColumn(
        modifier = Modifier.height(280.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        item {
          Surface(
            color = if (selectedBssid == null) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(null) },
          ) {
            Text(
              text = "All Visible APs (Strongest per spot)",
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
        items(aps, key = { it.bssid }) { ap ->
          Surface(
            color = if (ap.bssid.equals(selectedBssid, ignoreCase = true)) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainer
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(ap) },
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(ap.ssid, style = MaterialTheme.typography.labelLarge)
              Text(
                "${ap.bssid} · ${ap.band.label} · ${ap.peakRssiDbm} dBm",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

private fun screenToNormalized(
  tap: Offset,
  width: Float,
  height: Float,
  zoom: Float,
  pan: Offset,
): Offset {
  val cx = width / 2f
  val cy = height / 2f
  val unprojectedX = (tap.x - cx - pan.x) / zoom + cx
  val unprojectedY = (tap.y - cy - pan.y) / zoom + cy
  return Offset(
    x = (unprojectedX / width).coerceIn(0.02f, 0.98f),
    y = (unprojectedY / height).coerceIn(0.02f, 0.98f),
  )
}

private fun findNearestSample(norm: Offset, samples: List<IndoorSample>): IndoorSample? {
  return samples.minByOrNull { s ->
    val dx = s.xNorm - norm.x
    val dy = s.yNorm - norm.y
    dx * dx + dy * dy
  }?.takeIf { s ->
    val dx = s.xNorm - norm.x
    val dy = s.yNorm - norm.y
    (dx * dx + dy * dy) < 0.004f
  }
}
