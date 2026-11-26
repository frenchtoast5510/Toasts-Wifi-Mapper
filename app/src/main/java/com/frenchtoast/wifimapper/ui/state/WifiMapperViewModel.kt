package com.frenchtoast.wifimapper.ui.state

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.frenchtoast.wifimapper.data.local.AppPreferences
import com.frenchtoast.wifimapper.data.local.AppPreferencesStore
import com.frenchtoast.wifimapper.domain.math.SignalMath
import com.frenchtoast.wifimapper.domain.model.BandFilter
import com.frenchtoast.wifimapper.domain.model.ChannelCongestionBin
import com.frenchtoast.wifimapper.domain.model.FloorPlanPoint
import com.frenchtoast.wifimapper.domain.model.FloorPlanSurvey
import com.frenchtoast.wifimapper.domain.model.GeoCoordinate
import com.frenchtoast.wifimapper.domain.model.IndoorHeatmapCell
import com.frenchtoast.wifimapper.domain.model.IndoorSample
import com.frenchtoast.wifimapper.domain.model.NetworkSortOrder
import com.frenchtoast.wifimapper.domain.model.OuiLookup
import com.frenchtoast.wifimapper.domain.model.OutdoorHeatmapPoint
import com.frenchtoast.wifimapper.domain.model.PlacementRun
import com.frenchtoast.wifimapper.domain.model.RoomCoverageBreakdown
import com.frenchtoast.wifimapper.domain.model.RouterPin
import com.frenchtoast.wifimapper.domain.model.SecurityFilter
import com.frenchtoast.wifimapper.domain.model.WalkSession
import com.frenchtoast.wifimapper.domain.model.WalkTrackPoint
import com.frenchtoast.wifimapper.domain.model.WifiAccessPointSummary
import com.frenchtoast.wifimapper.domain.model.WifiBand
import com.frenchtoast.wifimapper.domain.model.WifiFilterState
import com.frenchtoast.wifimapper.domain.model.WifiGeneration
import com.frenchtoast.wifimapper.domain.model.WifiReading
import com.frenchtoast.wifimapper.domain.model.WifiSecurity
import com.frenchtoast.wifimapper.domain.repository.ExportFormatter
import com.frenchtoast.wifimapper.domain.repository.LocationFix
import com.frenchtoast.wifimapper.domain.repository.LocationPort
import com.frenchtoast.wifimapper.domain.repository.ScanCooldownState
import com.frenchtoast.wifimapper.domain.repository.ScanTriggerResult
import com.frenchtoast.wifimapper.domain.repository.ScannedWifiNetwork
import com.frenchtoast.wifimapper.domain.repository.WifiRepository
import com.frenchtoast.wifimapper.domain.repository.WifiScannerPort
import com.frenchtoast.wifimapper.platform.android.FloorPlanImageManager
import com.frenchtoast.wifimapper.platform.android.PermissionHelper
import com.frenchtoast.wifimapper.platform.android.PermissionStatusSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class IndoorInteractionMode(val label: String) {
  INSPECT_PAN("Pan / Inspect"),
  PLACE_ROUTER("Place Router Pin"),
  SAMPLE_HERE("Tap to Sample"),
}

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
  CSV("Standard CSV", "csv", "text/csv"),
  GEOJSON("GeoJSON (QGIS / GIS)", "geojson", "application/geo+json"),
  WIGLE_CSV("WiGLE CSV (v1.6)", "csv", "text/csv"),
}

data class WifiMapperUiState(
  val isDemoModeEnabled: Boolean = true,
  val walks: List<WalkSession> = emptyList(),
  val activeWalk: WalkSession? = null,
  val activeWalkTrack: List<WalkTrackPoint> = emptyList(),
  val activeWalkReadings: List<WifiReading> = emptyList(),
  val allSummaries: List<WifiAccessPointSummary> = emptyList(),
  val filteredSummaries: List<WifiAccessPointSummary> = emptyList(),
  val outdoorHeatmapPoints: List<OutdoorHeatmapPoint> = emptyList(),
  val showOutdoorHeatmap: Boolean = true,
  val filterState: WifiFilterState = WifiFilterState(),
  val sortOrder: NetworkSortOrder = NetworkSortOrder.STRONGEST_SIGNAL,
  val isMappingActive: Boolean = false,
  val latestLocation: LocationFix? = null,
  val scanCooldown: ScanCooldownState = ScanCooldownState(),
  val liveVisibleAps: List<ScannedWifiNetwork> = emptyList(),
  val mapFocusTarget: GeoCoordinate? = null,
  val mapFocusTriggerId: Long = 0L,

  val surveys: List<FloorPlanSurvey> = emptyList(),
  val activeSurvey: FloorPlanSurvey? = null,
  val placementRuns: List<PlacementRun> = emptyList(),
  val activeRun: PlacementRun? = null,
  val comparisonRun: PlacementRun? = null,
  val isComparisonOpen: Boolean = false,
  val indoorMode: IndoorInteractionMode = IndoorInteractionMode.INSPECT_PAN,
  val activeRunPins: List<RouterPin> = emptyList(),
  val activeRunSamples: List<IndoorSample> = emptyList(),
  val activeRunHeatmap: List<IndoorHeatmapCell> = emptyList(),
  val activeRoomCoverage: List<RoomCoverageBreakdown> = emptyList(),
  val comparisonSamples: List<IndoorSample> = emptyList(),
  val comparisonPins: List<RouterPin> = emptyList(),
  val comparisonHeatmap: List<IndoorHeatmapCell> = emptyList(),
  val comparisonRoomCoverage: List<RoomCoverageBreakdown> = emptyList(),
  val indoorTargetBssid: String? = null,
  val indoorTargetSsid: String? = null,

  val inspectorAp: WifiAccessPointSummary? = null,
  val channelBand: WifiBand = WifiBand.BAND_2_4_GHZ,
  val channelBins: List<ChannelCongestionBin> = emptyList(),
  val permissionSnapshot: PermissionStatusSnapshot = PermissionStatusSnapshot(false, false, false, false, false),
  val showPermissionGateDialog: Boolean = false,
  val showThrottlingDialog: Boolean = false,
  val snackbarMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WifiMapperViewModel(
  private val appContext: Context,
  private val repository: WifiRepository,
  private val prefsStore: AppPreferencesStore,
  private val wifiScanner: WifiScannerPort,
  private val locationPort: LocationPort,
) : ViewModel() {

  private val filterStateFlow = MutableStateFlow(WifiFilterState())
  private val sortOrderFlow = MutableStateFlow(NetworkSortOrder.STRONGEST_SIGNAL)
  private val isMappingActiveFlow = MutableStateFlow(false)
  private val latestLocationFlow = MutableStateFlow<LocationFix?>(null)
  private val liveVisibleApsFlow = MutableStateFlow<List<ScannedWifiNetwork>>(emptyList())
  private val mapFocusFlow = MutableStateFlow<Pair<GeoCoordinate?, Long>>(null to 0L)

  private val activeRunIdOverride = MutableStateFlow(0L)
  private val comparisonRunIdOverride = MutableStateFlow(0L)
  private val isComparisonOpenFlow = MutableStateFlow(false)
  private val indoorModeFlow = MutableStateFlow(IndoorInteractionMode.INSPECT_PAN)
  private val indoorTargetFlow = MutableStateFlow<Pair<String?, String?>>(null to null)

  private val inspectorApFlow = MutableStateFlow<WifiAccessPointSummary?>(null)
  private val channelBandFlow = MutableStateFlow(WifiBand.BAND_2_4_GHZ)
  private val permissionSnapshotFlow = MutableStateFlow(PermissionHelper.snapshot(appContext))
  private val showThrottlingDialogFlow = MutableStateFlow(false)
  private val snackbarFlow = MutableStateFlow<String?>(null)

  private var mappingJob: Job? = null
  private var latestScannedBatchMillis: Long = 0L

  private val prefsFlow = prefsStore.preferencesFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

  private val walksFlow = repository.observeWalks()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val activeWalkFlow = combine(walksFlow, prefsFlow) { walks, prefs ->
    walks.firstOrNull { it.id == prefs.selectedWalkId } ?: walks.firstOrNull()
  }

  private val activeWalkTrackFlow = activeWalkFlow.flatMapLatest { walk ->
    if (walk == null) flowOf(emptyList()) else repository.observeTrackForWalk(walk.id)
  }

  private val activeWalkReadingsFlow = activeWalkFlow.flatMapLatest { walk ->
    if (walk == null) flowOf(emptyList()) else repository.observeReadingsForWalk(walk.id)
  }

  private val surveysFlow = repository.observeFloorPlanSurveys()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val activeSurveyFlow = combine(surveysFlow, prefsFlow) { surveys, prefs ->
    surveys.firstOrNull { it.id == prefs.selectedSurveyId } ?: surveys.firstOrNull()
  }

  private val placementRunsFlow = activeSurveyFlow.flatMapLatest { survey ->
    if (survey == null) flowOf(emptyList()) else repository.observePlacementRuns(survey.id)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val activeRunFlow = combine(placementRunsFlow, activeRunIdOverride) { runs, overrideId ->
    runs.firstOrNull { it.id == overrideId } ?: runs.firstOrNull()
  }

  private val comparisonRunFlow = combine(
    placementRunsFlow,
    activeRunFlow,
    comparisonRunIdOverride,
  ) { runs, active, compId ->
    runs.firstOrNull { it.id == compId && it.id != active?.id }
      ?: runs.firstOrNull { it.id != active?.id }
  }

  private val activeRunPinsFlow = activeRunFlow.flatMapLatest { run ->
    if (run == null) flowOf(emptyList()) else repository.observeRouterPins(run.id)
  }

  private val activeRunSamplesFlow = activeRunFlow.flatMapLatest { run ->
    if (run == null) flowOf(emptyList()) else repository.observeIndoorSamples(run.id)
  }

  private val comparisonPinsFlow = comparisonRunFlow.flatMapLatest { run ->
    if (run == null) flowOf(emptyList()) else repository.observeRouterPins(run.id)
  }

  private val comparisonSamplesFlow = comparisonRunFlow.flatMapLatest { run ->
    if (run == null) flowOf(emptyList()) else repository.observeIndoorSamples(run.id)
  }

  private val outdoorStateFlow = combine(
    prefsFlow,
    walksFlow,
    activeWalkFlow,
    activeWalkTrackFlow,
    activeWalkReadingsFlow,
    filterStateFlow,
    sortOrderFlow,
    isMappingActiveFlow,
    latestLocationFlow,
    wifiScanner.cooldownState,
    liveVisibleApsFlow,
    mapFocusFlow,
  ) { args ->
    @Suppress("UNCHECKED_CAST")
    val prefs = args[0] as AppPreferences
    @Suppress("UNCHECKED_CAST")
    val walks = args[1] as List<WalkSession>
    val activeWalk = args[2] as WalkSession?
    @Suppress("UNCHECKED_CAST")
    val track = args[3] as List<WalkTrackPoint>
    @Suppress("UNCHECKED_CAST")
    val readings = args[4] as List<WifiReading>
    val filter = args[5] as WifiFilterState
    val sort = args[6] as NetworkSortOrder
    val mapping = args[7] as Boolean
    val loc = args[8] as LocationFix?
    val cooldown = args[9] as ScanCooldownState
    @Suppress("UNCHECKED_CAST")
    val liveAps = args[10] as List<ScannedWifiNetwork>
    @Suppress("UNCHECKED_CAST")
    val focus = args[11] as Pair<GeoCoordinate?, Long>

    val summaries = SignalMath.summarizeAccessPoints(readings)
    val filteredSummaries = SignalMath.filterSummaries(summaries, filter, sort)
    val filteredReadings = SignalMath.filterReadings(readings, filter)
    val heatmap = SignalMath.computeOutdoorHeatmapPoints(filteredReadings)

    OutdoorBundle(
      isDemoModeEnabled = prefs.isDemoModeEnabled,
      walks = walks,
      activeWalk = activeWalk,
      track = track,
      readings = readings,
      allSummaries = summaries,
      filteredSummaries = filteredSummaries,
      heatmap = heatmap,
      showHeatmap = prefs.showOutdoorHeatmap,
      filter = filter,
      sortOrder = sort,
      isMappingActive = mapping,
      latestLocation = loc,
      cooldown = cooldown,
      liveAps = liveAps,
      focus = focus,
    )
  }

  private val indoorStateFlow = combine(
    surveysFlow,
    activeSurveyFlow,
    placementRunsFlow,
    activeRunFlow,
    comparisonRunFlow,
    isComparisonOpenFlow,
    indoorModeFlow,
    activeRunPinsFlow,
    activeRunSamplesFlow,
    comparisonPinsFlow,
    comparisonSamplesFlow,
    indoorTargetFlow,
  ) { args ->
    @Suppress("UNCHECKED_CAST")
    val surveys = args[0] as List<FloorPlanSurvey>
    val activeSurvey = args[1] as FloorPlanSurvey?
    @Suppress("UNCHECKED_CAST")
    val runs = args[2] as List<PlacementRun>
    val activeRun = args[3] as PlacementRun?
    val compRun = args[4] as PlacementRun?
    val compOpen = args[5] as Boolean
    val mode = args[6] as IndoorInteractionMode
    @Suppress("UNCHECKED_CAST")
    val pins = args[7] as List<RouterPin>
    @Suppress("UNCHECKED_CAST")
    val samples = args[8] as List<IndoorSample>
    @Suppress("UNCHECKED_CAST")
    val compPins = args[9] as List<RouterPin>
    @Suppress("UNCHECKED_CAST")
    val compSamples = args[10] as List<IndoorSample>
    @Suppress("UNCHECKED_CAST")
    val target = args[11] as Pair<String?, String?>

    val rooms = if (activeSurvey?.isBuiltInSample == true) SignalMath.defaultDemoRooms else emptyList()
    val heatmap = SignalMath.computeIndoorHeatmapGrid(samples)
    val coverage = if (rooms.isNotEmpty()) {
      SignalMath.computeRoomCoverage(samples, rooms)
    } else if (samples.isNotEmpty()) {
      val strong = samples.count { it.rssiDbm >= -60 } * 100 / samples.size
      val fair = samples.count { it.rssiDbm in -75..-61 } * 100 / samples.size
      val dead = (100 - strong - fair).coerceAtLeast(0)
      listOf(RoomCoverageBreakdown("All Sampled Area", strong, fair, dead, samples.map { it.rssiDbm }.average().toInt()))
    } else {
      emptyList()
    }

    val compHeatmap = SignalMath.computeIndoorHeatmapGrid(compSamples)
    val compCoverage = if (rooms.isNotEmpty()) {
      SignalMath.computeRoomCoverage(compSamples, rooms)
    } else if (compSamples.isNotEmpty()) {
      val strong = compSamples.count { it.rssiDbm >= -60 } * 100 / compSamples.size
      val fair = compSamples.count { it.rssiDbm in -75..-61 } * 100 / compSamples.size
      val dead = (100 - strong - fair).coerceAtLeast(0)
      listOf(RoomCoverageBreakdown("All Sampled Area", strong, fair, dead, compSamples.map { it.rssiDbm }.average().toInt()))
    } else {
      emptyList()
    }

    IndoorBundle(
      surveys = surveys,
      activeSurvey = activeSurvey,
      runs = runs,
      activeRun = activeRun,
      compRun = compRun,
      isComparisonOpen = compOpen,
      mode = mode,
      pins = pins,
      samples = samples,
      heatmap = heatmap,
      coverage = coverage,
      compPins = compPins,
      compSamples = compSamples,
      compHeatmap = compHeatmap,
      compCoverage = compCoverage,
      targetBssid = target.first ?: activeRun?.targetBssid,
      targetSsid = target.second ?: activeRun?.targetSsid,
    )
  }

  val uiState: StateFlow<WifiMapperUiState> = combine(
    outdoorStateFlow,
    indoorStateFlow,
    inspectorApFlow,
    channelBandFlow,
    permissionSnapshotFlow,
    showThrottlingDialogFlow,
    snackbarFlow,
  ) { args ->
    val outdoor = args[0] as OutdoorBundle
    val indoor = args[1] as IndoorBundle
    val inspector = args[2] as WifiAccessPointSummary?
    val chBand = args[3] as WifiBand
    val perms = args[4] as PermissionStatusSnapshot
    val throttleDialog = args[5] as Boolean
    val snackbar = args[6] as String?

    val bins = SignalMath.computeChannelCongestion(outdoor.allSummaries, chBand)
    val shouldGateLiveMode = !outdoor.isDemoModeEnabled && !perms.hasAllLivePermissions

    WifiMapperUiState(
      isDemoModeEnabled = outdoor.isDemoModeEnabled,
      walks = outdoor.walks,
      activeWalk = outdoor.activeWalk,
      activeWalkTrack = outdoor.track,
      activeWalkReadings = outdoor.readings,
      allSummaries = outdoor.allSummaries,
      filteredSummaries = outdoor.filteredSummaries,
      outdoorHeatmapPoints = outdoor.heatmap,
      showOutdoorHeatmap = outdoor.showHeatmap,
      filterState = outdoor.filter,
      sortOrder = outdoor.sortOrder,
      isMappingActive = outdoor.isMappingActive,
      latestLocation = outdoor.latestLocation,
      scanCooldown = outdoor.cooldown,
      liveVisibleAps = outdoor.liveAps,
      mapFocusTarget = outdoor.focus.first,
      mapFocusTriggerId = outdoor.focus.second,
      surveys = indoor.surveys,
      activeSurvey = indoor.activeSurvey,
      placementRuns = indoor.runs,
      activeRun = indoor.activeRun,
      comparisonRun = indoor.compRun,
      isComparisonOpen = indoor.isComparisonOpen,
      indoorMode = indoor.mode,
      activeRunPins = indoor.pins,
      activeRunSamples = indoor.samples,
      activeRunHeatmap = indoor.heatmap,
      activeRoomCoverage = indoor.coverage,
      comparisonSamples = indoor.compSamples,
      comparisonPins = indoor.compPins,
      comparisonHeatmap = indoor.compHeatmap,
      comparisonRoomCoverage = indoor.compCoverage,
      indoorTargetBssid = indoor.targetBssid,
      indoorTargetSsid = indoor.targetSsid,
      inspectorAp = inspector,
      channelBand = chBand,
      channelBins = bins,
      permissionSnapshot = perms,
      showPermissionGateDialog = shouldGateLiveMode,
      showThrottlingDialog = throttleDialog,
      snackbarMessage = snackbar,
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), WifiMapperUiState())

  init {
    viewModelScope.launch {
      repository.ensureDemoDataSeededIfEmpty()
    }
    viewModelScope.launch {
      wifiScanner.scanResults.collect { batch ->
        liveVisibleApsFlow.value = batch.networks
        latestScannedBatchMillis = batch.scannedAtMillis
        if (isMappingActiveFlow.value) {
          persistLiveBatch(batch.networks, batch.scannedAtMillis)
        }
      }
    }
    viewModelScope.launch {
      locationPort.locationUpdates.collect { fix ->
        latestLocationFlow.value = fix
      }
    }
    viewModelScope.launch {
      while (isActive) {
        wifiScanner.refreshCooldownState()
        delay(1_000L)
      }
    }
  }

  fun refreshPermissions() {
    permissionSnapshotFlow.value = PermissionHelper.snapshot(appContext)
    if (wifiScanner.hasWifiScanPermission()) {
      val cached = wifiScanner.readLatestCachedResults()
      liveVisibleApsFlow.value = cached.networks
    }
  }

  fun toggleDemoMode(enabled: Boolean) {
    viewModelScope.launch {
      if (enabled) {
        val hasDemoWalk = walksFlow.value.any { it.isDemo }
        if (!hasDemoWalk) {
          repository.resetDemoData()
        } else {
          prefsStore.setDemoModeEnabled(true)
          walksFlow.value.firstOrNull { it.isDemo }?.let { prefsStore.setSelectedWalkId(it.id) }
          surveysFlow.value.firstOrNull { it.isDemo }?.let { prefsStore.setSelectedSurveyId(it.id) }
        }
        stopMappingInternal()
        snackbarFlow.value = "Demo Mode enabled — preloaded neighborhood & house surveys active"
      } else {
        prefsStore.setDemoModeEnabled(false)
        val nonDemoWalk = walksFlow.value.firstOrNull { !it.isDemo }
        val walkId = nonDemoWalk?.id ?: repository.createWalk("Live Field Walk")
        prefsStore.setSelectedWalkId(walkId)
        val nonDemoSurvey = surveysFlow.value.firstOrNull { !it.isDemo }
        val surveyId = nonDemoSurvey?.id ?: repository.createFloorPlanSurvey(
          name = "Home Floor Survey",
          imageUri = null,
          isBuiltInSample = true,
        )
        prefsStore.setSelectedSurveyId(surveyId)
        snackbarFlow.value = "Switched to Live Mode — ready for real Wi-Fi & GPS scans"
      }
    }
  }

  fun resetDemoData() {
    viewModelScope.launch {
      stopMappingInternal()
      repository.resetDemoData()
      filterStateFlow.value = WifiFilterState()
      activeRunIdOverride.value = 0L
      snackbarFlow.value = "Demo data restored"
    }
  }

  fun clearAndSwitchToBlankLiveDb() {
    viewModelScope.launch {
      stopMappingInternal()
      repository.clearAllDataAndCreateBlankWorkspace()
      filterStateFlow.value = WifiFilterState()
      activeRunIdOverride.value = 0L
      snackbarFlow.value = "Cleared database — switched to blank live workspace"
    }
  }

  fun startOrPauseMapping() {
    if (isMappingActiveFlow.value) {
      stopMappingInternal()
      snackbarFlow.value = "Outdoor mapping paused"
      return
    }
    refreshPermissions()
    val perms = permissionSnapshotFlow.value
    if (!perms.canMapOutdoors) {
      snackbarFlow.value = "Grant Fine Location & Nearby Wi-Fi permissions to record live walks"
      return
    }
    if (uiState.value.isDemoModeEnabled) {
      viewModelScope.launch {
        toggleDemoMode(false)
        startMappingInternal()
      }
    } else {
      startMappingInternal()
    }
  }

  private fun startMappingInternal() {
    locationPort.startTracking()
    isMappingActiveFlow.value = true
    triggerManualScan()
    mappingJob?.cancel()
    mappingJob = viewModelScope.launch {
      while (isActive && isMappingActiveFlow.value) {
        delay(32_000L)
        if (isMappingActiveFlow.value) {
          wifiScanner.triggerScan()
        }
      }
    }
    snackbarFlow.value = "Live GPS + Wi-Fi mapping started"
  }

  private fun stopMappingInternal() {
    mappingJob?.cancel()
    mappingJob = null
    isMappingActiveFlow.value = false
    locationPort.stopTracking()
  }

  fun triggerManualScan() {
    refreshPermissions()
    when (val res = wifiScanner.triggerScan()) {
      ScanTriggerResult.Triggered -> {
        snackbarFlow.value = "Wi-Fi scan triggered"
      }
      is ScanTriggerResult.Throttled -> {
        val cached = wifiScanner.readLatestCachedResults()
        liveVisibleApsFlow.value = cached.networks
        if (isMappingActiveFlow.value && cached.networks.isNotEmpty()) {
          viewModelScope.launch { persistLiveBatch(cached.networks, cached.scannedAtMillis) }
        }
        snackbarFlow.value = "Android scan throttle active (${res.retryAfterSeconds}s). Tap cooldown badge for details."
      }
      ScanTriggerResult.MissingPermission -> {
        snackbarFlow.value = "Location & Wi-Fi permissions required to scan"
      }
      ScanTriggerResult.WifiDisabled -> {
        snackbarFlow.value = "Enable Wi-Fi in device settings to scan"
      }
    }
  }

  private suspend fun persistLiveBatch(networks: List<ScannedWifiNetwork>, scannedAt: Long) {
    val walk = uiState.value.activeWalk ?: return
    val fix = latestLocationFlow.value ?: return
    if (networks.isEmpty()) return

    val trackPoint = WalkTrackPoint(
      walkId = walk.id,
      latitude = fix.coordinate.latitude,
      longitude = fix.coordinate.longitude,
      accuracyMeters = fix.accuracyMeters,
      timestampMillis = scannedAt,
    )
    val readings = networks.map { net ->
      WifiReading(
        walkId = walk.id,
        bssid = net.bssid,
        ssid = net.ssid,
        rssiDbm = net.rssiDbm,
        frequencyMhz = net.frequencyMhz,
        primaryChannel = net.primaryChannel,
        channelWidthMhz = net.channelWidthMhz,
        band = net.band,
        security = net.security,
        capabilitiesRaw = net.capabilitiesRaw,
        wifiGeneration = net.wifiGeneration,
        latitude = fix.coordinate.latitude,
        longitude = fix.coordinate.longitude,
        altitudeMeters = fix.coordinate.altitudeMeters,
        accuracyMeters = fix.accuracyMeters,
        timestampMillis = scannedAt,
      )
    }
    repository.recordScanBatch(walk.id, trackPoint, readings)
  }

  fun updateSearchQuery(query: String) {
    filterStateFlow.value = filterStateFlow.value.copy(searchQuery = query)
  }

  fun selectBandFilter(band: BandFilter) {
    filterStateFlow.value = filterStateFlow.value.copy(bandFilter = band)
  }

  fun selectSecurityFilter(security: SecurityFilter) {
    filterStateFlow.value = filterStateFlow.value.copy(securityFilter = security)
  }

  fun isolateBssid(bssid: String) {
    filterStateFlow.value = filterStateFlow.value.copy(isolatedBssid = bssid)
    inspectorApFlow.value = null
    snackbarFlow.value = "Isolated $bssid on map & heatmap"
  }

  fun clearIsolation() {
    filterStateFlow.value = filterStateFlow.value.copy(isolatedBssid = null)
  }

  fun toggleOutdoorHeatmap() {
    viewModelScope.launch {
      prefsStore.setShowOutdoorHeatmap(!uiState.value.showOutdoorHeatmap)
    }
  }

  fun setSortOrder(order: NetworkSortOrder) {
    sortOrderFlow.value = order
  }

  fun selectChannelBand(band: WifiBand) {
    channelBandFlow.value = band
  }

  fun setShowThrottlingDialog(show: Boolean) {
    showThrottlingDialogFlow.value = show
  }

  fun selectWalk(walkId: Long) {
    viewModelScope.launch {
      prefsStore.setSelectedWalkId(walkId)
      filterStateFlow.value = filterStateFlow.value.copy(isolatedBssid = null)
    }
  }

  fun createNewWalk(name: String) {
    viewModelScope.launch {
      val clean = name.trim().ifBlank { "Field Walk" }
      val id = repository.createWalk(clean)
      prefsStore.setDemoModeEnabled(false)
      prefsStore.setSelectedWalkId(id)
      snackbarFlow.value = "Created walk \"$clean\""
    }
  }

  fun deleteWalk(walkId: Long) {
    viewModelScope.launch {
      repository.deleteWalk(walkId)
      snackbarFlow.value = "Deleted walk"
    }
  }

  fun setIndoorMode(mode: IndoorInteractionMode) {
    indoorModeFlow.value = mode
  }

  fun selectSurvey(surveyId: Long) {
    viewModelScope.launch {
      prefsStore.setSelectedSurveyId(surveyId)
      activeRunIdOverride.value = 0L
    }
  }

  fun importFloorPlan(uri: Uri, surveyName: String) {
    viewModelScope.launch {
      val storedPath = withContext(Dispatchers.IO) {
        FloorPlanImageManager.importFloorPlanToAppStorage(appContext, uri)
      }
      if (storedPath == null) {
        snackbarFlow.value = "Could not import floor-plan image"
        return@launch
      }
      val id = repository.createFloorPlanSurvey(
        name = surveyName.trim().ifBlank { "Custom Floor Plan" },
        imageUri = storedPath,
        isBuiltInSample = false,
      )
      prefsStore.setSelectedSurveyId(id)
      activeRunIdOverride.value = 0L
      snackbarFlow.value = "Imported floor plan"
    }
  }

  fun deleteSurvey(surveyId: Long) {
    viewModelScope.launch {
      repository.deleteFloorPlanSurvey(surveyId)
      snackbarFlow.value = "Deleted floor-plan survey"
    }
  }

  fun selectPlacementRun(runId: Long) {
    activeRunIdOverride.value = runId
  }

  fun selectComparisonRun(runId: Long) {
    comparisonRunIdOverride.value = runId
  }

  fun setComparisonOpen(open: Boolean) {
    isComparisonOpenFlow.value = open
  }

  fun createPlacementRun(name: String) {
    val survey = uiState.value.activeSurvey ?: return
    viewModelScope.launch {
      val runId = repository.createPlacementRun(
        surveyId = survey.id,
        name = name.trim().ifBlank { "Placement Run" },
        targetBssid = uiState.value.indoorTargetBssid,
        targetSsid = uiState.value.indoorTargetSsid ?: "All Visible APs",
      )
      activeRunIdOverride.value = runId
      snackbarFlow.value = "Created run \"$name\""
    }
  }

  fun deletePlacementRun(runId: Long) {
    viewModelScope.launch {
      repository.deletePlacementRun(runId)
      snackbarFlow.value = "Deleted placement run"
    }
  }

  fun placeOrMoveRouterPin(xNorm: Float, yNorm: Float, existingPinId: Long? = null) {
    val run = uiState.value.activeRun ?: return
    viewModelScope.launch {
      val existing = uiState.value.activeRunPins
      val pin = if (existingPinId != null) {
        existing.firstOrNull { it.id == existingPinId }?.copy(xNorm = xNorm, yNorm = yNorm)
      } else {
        RouterPin(
          runId = run.id,
          label = "Router ${existing.size + 1}",
          xNorm = xNorm,
          yNorm = yNorm,
        )
      }
      if (pin != null) {
        repository.upsertRouterPin(pin)
      }
    }
  }

  fun deleteRouterPin(pinId: Long) {
    viewModelScope.launch {
      repository.deleteRouterPin(pinId)
    }
  }

  fun setIndoorTargetAp(ap: WifiAccessPointSummary?) {
    indoorTargetFlow.value = ap?.bssid to ap?.ssid
    inspectorApFlow.value = null
    snackbarFlow.value = if (ap != null) {
      "Indoor target set to ${ap.ssid} (${ap.bssid})"
    } else {
      "Indoor target set to All Visible APs"
    }
  }

  fun captureIndoorSample(xNorm: Float, yNorm: Float) {
    val run = uiState.value.activeRun ?: return
    val targetBssid = uiState.value.indoorTargetBssid
    viewModelScope.launch {
      refreshPermissions()
      if (wifiScanner.hasWifiScanPermission() && wifiScanner.isWifiEnabled()) {
        val batch = wifiScanner.readLatestCachedResults()
        val matching = if (targetBssid != null) {
          batch.networks.filter { it.bssid.equals(targetBssid, ignoreCase = true) }
        } else {
          batch.networks.sortedByDescending { it.rssiDbm }.take(6)
        }
        if (matching.isNotEmpty()) {
          val samples = matching.map { net ->
            IndoorSample(
              runId = run.id,
              xNorm = xNorm,
              yNorm = yNorm,
              bssid = net.bssid,
              ssid = net.ssid,
              rssiDbm = net.rssiDbm,
              frequencyMhz = net.frequencyMhz,
              primaryChannel = net.primaryChannel,
              channelWidthMhz = net.channelWidthMhz,
              band = net.band,
              security = net.security,
              wifiGeneration = net.wifiGeneration,
              timestampMillis = System.currentTimeMillis(),
            )
          }
          repository.addIndoorSamples(samples)
          val best = samples.maxByOrNull { it.rssiDbm }
          snackbarFlow.value = "Sampled ${best?.ssid?.ifBlank { "AP" }} at ${best?.rssiDbm} dBm"
          wifiScanner.triggerScan()
          return@launch
        }
      }

      if (uiState.value.isDemoModeEnabled) {
        val pin = uiState.value.activeRunPins.firstOrNull()
        val dist = if (pin != null) {
          hypot((xNorm - pin.xNorm).toDouble(), (yNorm - pin.yNorm).toDouble())
        } else {
          0.35
        }
        val estimatedRssi = (-36.0 - dist * 55.0).roundToInt().coerceIn(-92, -33)
        repository.addIndoorSamples(
          listOf(
            IndoorSample(
              runId = run.id,
              xNorm = xNorm,
              yNorm = yNorm,
              bssid = targetBssid ?: "24:5A:4C:11:2A:02",
              ssid = uiState.value.indoorTargetSsid ?: "ToastLab-5G",
              rssiDbm = estimatedRssi,
              frequencyMhz = 5180,
              primaryChannel = 36,
              channelWidthMhz = 80,
              band = WifiBand.BAND_5_GHZ,
              security = WifiSecurity.WPA3,
              wifiGeneration = WifiGeneration.WIFI_6,
              timestampMillis = System.currentTimeMillis(),
            ),
          ),
        )
        snackbarFlow.value = "Demo sample logged: $estimatedRssi dBm"
      } else {
        snackbarFlow.value = "No live Wi-Fi results yet — check Wi-Fi & permissions"
      }
    }
  }

  fun openInspector(ap: WifiAccessPointSummary) {
    inspectorApFlow.value = ap
  }

  fun openInspectorForIndoorSample(sample: IndoorSample) {
    val existing = uiState.value.allSummaries.firstOrNull {
      it.bssid.equals(sample.bssid, ignoreCase = true)
    }
    if (existing != null) {
      inspectorApFlow.value = existing.copy(
        latestRssiDbm = sample.rssiDbm,
        strongestFloorPoint = FloorPlanPoint(sample.xNorm, sample.yNorm),
      )
    } else {
      val runSamplesForAp = uiState.value.activeRunSamples.filter {
        it.bssid.equals(sample.bssid, ignoreCase = true)
      }
      val peak = runSamplesForAp.maxByOrNull { it.rssiDbm } ?: sample
      inspectorApFlow.value = WifiAccessPointSummary(
        bssid = sample.bssid,
        ssid = sample.ssid.ifBlank { "Hidden Network" },
        isHidden = sample.ssid.isBlank(),
        manufacturer = OuiLookup.lookup(sample.bssid),
        band = sample.band,
        frequencyMhz = sample.frequencyMhz,
        primaryChannel = sample.primaryChannel,
        channelWidthMhz = sample.channelWidthMhz,
        wifiGeneration = sample.wifiGeneration,
        security = sample.security,
        capabilitiesRaw = sample.security.label,
        peakRssiDbm = peak.rssiDbm,
        latestRssiDbm = sample.rssiDbm,
        averageRssiDbm = runSamplesForAp.map { it.rssiDbm }.average().toInt(),
        sightingsCount = runSamplesForAp.size.coerceAtLeast(1),
        firstSeenMillis = runSamplesForAp.minOfOrNull { it.timestampMillis } ?: sample.timestampMillis,
        lastSeenMillis = runSamplesForAp.maxOfOrNull { it.timestampMillis } ?: sample.timestampMillis,
        strongestCoordinate = null,
        strongestFloorPoint = FloorPlanPoint(peak.xNorm, peak.yNorm),
      )
    }
  }

  fun dismissInspector() {
    inspectorApFlow.value = null
  }

  fun focusMapOnAp(ap: WifiAccessPointSummary) {
    val coord = ap.strongestCoordinate ?: return
    mapFocusFlow.value = coord to System.currentTimeMillis()
  }

  fun prepareExportPayload(format: ExportFormat): Pair<String, String> {
    val state = uiState.value
    val walkName = state.activeWalk?.name?.replace(Regex("[^A-Za-z0-9_-]"), "_") ?: "wifi_walk"
    return when (format) {
      ExportFormat.CSV -> "${walkName}.csv" to ExportFormatter.toCsv(state.activeWalkReadings)
      ExportFormat.GEOJSON -> "${walkName}.geojson" to ExportFormatter.toGeoJson(
        summaries = state.allSummaries,
        trackPoints = state.activeWalkTrack,
        walkName = state.activeWalk?.name ?: "Wi-Fi Walk",
      )
      ExportFormat.WIGLE_CSV -> "${walkName}_wigle.csv" to ExportFormatter.toWigleCsv(
        readings = state.activeWalkReadings,
        deviceModel = android.os.Build.MODEL ?: "Android",
      )
    }
  }

  fun notifyMessage(msg: String) {
    snackbarFlow.value = msg
  }

  fun clearSnackbar() {
    snackbarFlow.value = null
  }

  override fun onCleared() {
    super.onCleared()
    locationPort.stopTracking()
  }

  private data class OutdoorBundle(
    val isDemoModeEnabled: Boolean,
    val walks: List<WalkSession>,
    val activeWalk: WalkSession?,
    val track: List<WalkTrackPoint>,
    val readings: List<WifiReading>,
    val allSummaries: List<WifiAccessPointSummary>,
    val filteredSummaries: List<WifiAccessPointSummary>,
    val heatmap: List<OutdoorHeatmapPoint>,
    val showHeatmap: Boolean,
    val filter: WifiFilterState,
    val sortOrder: NetworkSortOrder,
    val isMappingActive: Boolean,
    val latestLocation: LocationFix?,
    val cooldown: ScanCooldownState,
    val liveAps: List<ScannedWifiNetwork>,
    val focus: Pair<GeoCoordinate?, Long>,
  )

  private data class IndoorBundle(
    val surveys: List<FloorPlanSurvey>,
    val activeSurvey: FloorPlanSurvey?,
    val runs: List<PlacementRun>,
    val activeRun: PlacementRun?,
    val compRun: PlacementRun?,
    val isComparisonOpen: Boolean,
    val mode: IndoorInteractionMode,
    val pins: List<RouterPin>,
    val samples: List<IndoorSample>,
    val heatmap: List<IndoorHeatmapCell>,
    val coverage: List<RoomCoverageBreakdown>,
    val compPins: List<RouterPin>,
    val compSamples: List<IndoorSample>,
    val compHeatmap: List<IndoorHeatmapCell>,
    val compCoverage: List<RoomCoverageBreakdown>,
    val targetBssid: String?,
    val targetSsid: String?,
  )

  class Factory(
    private val context: Context,
    private val repository: WifiRepository,
    private val prefsStore: AppPreferencesStore,
    private val wifiScanner: WifiScannerPort,
    private val locationPort: LocationPort,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return WifiMapperViewModel(
        appContext = context.applicationContext,
        repository = repository,
        prefsStore = prefsStore,
        wifiScanner = wifiScanner,
        locationPort = locationPort,
      ) as T
    }
  }
}
