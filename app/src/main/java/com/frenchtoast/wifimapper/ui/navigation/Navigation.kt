package com.frenchtoast.wifimapper.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.frenchtoast.wifimapper.ui.components.ScanThrottlingInfoDialog
import com.frenchtoast.wifimapper.ui.components.ToastsTopAppBar
import com.frenchtoast.wifimapper.ui.components.WifiInspectorBottomSheet
import com.frenchtoast.wifimapper.ui.screens.AnalyzerExportScreen
import com.frenchtoast.wifimapper.ui.screens.IndoorFloorPlanScreen
import com.frenchtoast.wifimapper.ui.screens.OutdoorMapScreen
import com.frenchtoast.wifimapper.ui.screens.PermissionsPrivacyScreen
import com.frenchtoast.wifimapper.ui.state.WifiMapperViewModel
import kotlinx.serialization.Serializable

@Serializable
data object OutdoorMapRoute

@Serializable
data object IndoorSurveyRoute

@Serializable
data object AnalyzerExportRoute

@Serializable
data object PrivacyPermissionsRoute

private data class TopLevelTab(
  val label: String,
  val route: Any,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val topLevelTabs = listOf(
  TopLevelTab(
    label = "Outdoor",
    route = OutdoorMapRoute,
    selectedIcon = Icons.Filled.Map,
    unselectedIcon = Icons.Outlined.Map,
    testTag = "nav_outdoor",
  ),
  TopLevelTab(
    label = "Indoor",
    route = IndoorSurveyRoute,
    selectedIcon = Icons.Filled.HomeWork,
    unselectedIcon = Icons.Outlined.HomeWork,
    testTag = "nav_indoor",
  ),
  TopLevelTab(
    label = "Networks",
    route = AnalyzerExportRoute,
    selectedIcon = Icons.Filled.Analytics,
    unselectedIcon = Icons.Outlined.Analytics,
    testTag = "nav_networks",
  ),
)

@Composable
fun ToastsWifiMapperApp(
  viewModel: WifiMapperViewModel,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val snackbarHostState = remember { SnackbarHostState() }

  val isPrivacyScreen = currentDestination?.hasRoute(PrivacyPermissionsRoute::class) == true
  val containerWidthPx = LocalWindowInfo.current.containerSize.width
  val density = androidx.compose.ui.platform.LocalDensity.current.density
  val useNavRail = (containerWidthPx / density) >= 600f

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
    topBar = {
      if (!isPrivacyScreen) {
        ToastsTopAppBar(
          isDemoModeEnabled = uiState.isDemoModeEnabled,
          onToggleDemoMode = viewModel::toggleDemoMode,
          onResetDemoData = viewModel::resetDemoData,
          onClearToBlankLiveDb = viewModel::clearAndSwitchToBlankLiveDb,
          onOpenPrivacyScreen = { navController.navigate(PrivacyPermissionsRoute) },
        )
      }
    },
    bottomBar = {
      if (!isPrivacyScreen && !useNavRail) {
        NavigationBar {
          topLevelTabs.forEach { tab ->
            val selected = currentDestination?.hasRoute(tab.route::class) == true
            NavigationBarItem(
              selected = selected,
              onClick = {
                navController.navigate(tab.route) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.label,
                )
              },
              label = { Text(tab.label) },
              modifier = Modifier.testTag(tab.testTag),
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      if (!isPrivacyScreen && useNavRail) {
        NavigationRail {
          topLevelTabs.forEach { tab ->
            val selected = currentDestination?.hasRoute(tab.route::class) == true
            NavigationRailItem(
              selected = selected,
              onClick = {
                navController.navigate(tab.route) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.label,
                )
              },
              label = { Text(tab.label) },
              modifier = Modifier.testTag(tab.testTag),
            )
          }
        }
      }

      Box(modifier = Modifier.weight(1f)) {
        NavHost(
          navController = navController,
          startDestination = OutdoorMapRoute,
        ) {
          composable<OutdoorMapRoute> {
            OutdoorMapScreen(
              uiState = uiState,
              onSearchQueryChange = viewModel::updateSearchQuery,
              onSelectBand = viewModel::selectBandFilter,
              onSelectSecurity = viewModel::selectSecurityFilter,
              onClearIsolation = viewModel::clearIsolation,
              onToggleHeatmap = viewModel::toggleOutdoorHeatmap,
              onToggleMapping = viewModel::startOrPauseMapping,
              onTriggerManualScan = viewModel::triggerManualScan,
              onOpenThrottleDialog = { viewModel.setShowThrottlingDialog(true) },
              onApMarkerClicked = viewModel::openInspector,
            )
          }

          composable<IndoorSurveyRoute> {
            IndoorFloorPlanScreen(
              uiState = uiState,
              onSetMode = viewModel::setIndoorMode,
              onSelectRun = viewModel::selectPlacementRun,
              onCreateRun = viewModel::createPlacementRun,
              onPlaceOrMoveRouterPin = viewModel::placeOrMoveRouterPin,
              onCaptureSample = viewModel::captureIndoorSample,
              onSampleClicked = viewModel::openInspectorForIndoorSample,
              onImportFloorPlan = viewModel::importFloorPlan,
              onOpenComparison = viewModel::setComparisonOpen,
              onSelectComparisonRun = viewModel::selectComparisonRun,
              onSetTargetAp = viewModel::setIndoorTargetAp,
            )
          }

          composable<AnalyzerExportRoute> {
            AnalyzerExportScreen(
              uiState = uiState,
              onSearchQueryChange = viewModel::updateSearchQuery,
              onSelectSortOrder = viewModel::setSortOrder,
              onSelectChannelBand = viewModel::selectChannelBand,
              onInspectAp = viewModel::openInspector,
              onLocateApOnMap = { ap ->
                viewModel.focusMapOnAp(ap)
                navController.navigate(OutdoorMapRoute) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              onSelectWalk = viewModel::selectWalk,
              onCreateWalk = viewModel::createNewWalk,
              onDeleteWalk = viewModel::deleteWalk,
              onSelectSurvey = viewModel::selectSurvey,
              onDeleteSurvey = viewModel::deleteSurvey,
              onPrepareExport = viewModel::prepareExportPayload,
              onNotify = viewModel::notifyMessage,
            )
          }

          composable<PrivacyPermissionsRoute> {
            PermissionsPrivacyScreen(
              snapshot = uiState.permissionSnapshot,
              onPermissionsUpdated = viewModel::refreshPermissions,
              onBack = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }

  uiState.inspectorAp?.let { ap ->
    WifiInspectorBottomSheet(
      ap = ap,
      onDismiss = viewModel::dismissInspector,
      onIsolateOnHeatmap = { bssid ->
        viewModel.isolateBssid(bssid)
        navController.navigate(OutdoorMapRoute) {
          popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
          }
          launchSingleTop = true
          restoreState = true
        }
      },
      onSetAsIndoorTarget = { target ->
        viewModel.setIndoorTargetAp(target)
        navController.navigate(IndoorSurveyRoute) {
          popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
          }
          launchSingleTop = true
          restoreState = true
        }
      },
      onCopiedToClipboard = { msg -> viewModel.notifyMessage(msg) },
    )
  }

  if (uiState.showThrottlingDialog) {
    ScanThrottlingInfoDialog(
      scansUsed = uiState.scanCooldown.scansUsedInWindow,
      maxScans = uiState.scanCooldown.maxScansPerWindow,
      secondsUntilNextSlot = uiState.scanCooldown.secondsUntilNextSlot,
      onDismiss = { viewModel.setShowThrottlingDialog(false) },
    )
  }
}
