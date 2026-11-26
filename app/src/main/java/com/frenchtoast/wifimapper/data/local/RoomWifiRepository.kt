package com.frenchtoast.wifimapper.data.local

import androidx.room.withTransaction
import com.frenchtoast.wifimapper.data.demo.DemoDatasetFactory
import com.frenchtoast.wifimapper.domain.model.FloorPlanSurvey
import com.frenchtoast.wifimapper.domain.model.IndoorSample
import com.frenchtoast.wifimapper.domain.model.PlacementRun
import com.frenchtoast.wifimapper.domain.model.RouterPin
import com.frenchtoast.wifimapper.domain.model.WalkSession
import com.frenchtoast.wifimapper.domain.model.WalkTrackPoint
import com.frenchtoast.wifimapper.domain.model.WifiBand
import com.frenchtoast.wifimapper.domain.model.WifiGeneration
import com.frenchtoast.wifimapper.domain.model.WifiReading
import com.frenchtoast.wifimapper.domain.model.WifiSecurity
import com.frenchtoast.wifimapper.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomWifiRepository(
  private val db: WifiMapperDatabase,
  private val prefsStore: AppPreferencesStore,
) : WifiRepository {

  private val dao = db.wifiMapperDao()

  override fun observeWalks(): Flow<List<WalkSession>> =
    dao.observeWalks().map { list ->
      list.map { WalkSession(it.id, it.name, it.createdAtMillis, it.isDemo) }
    }

  override fun observeReadingsForWalk(walkId: Long): Flow<List<WifiReading>> =
    dao.observeReadingsForWalk(walkId).map { list -> list.map { it.toDomain() } }

  override fun observeAllReadings(): Flow<List<WifiReading>> =
    dao.observeAllReadings().map { list -> list.map { it.toDomain() } }

  override fun observeTrackForWalk(walkId: Long): Flow<List<WalkTrackPoint>> =
    dao.observeTrackForWalk(walkId).map { list ->
      list.map { WalkTrackPoint(it.id, it.walkId, it.latitude, it.longitude, it.accuracyMeters, it.timestampMillis) }
    }

  override fun observeFloorPlanSurveys(): Flow<List<FloorPlanSurvey>> =
    dao.observeFloorPlanSurveys().map { list ->
      list.map { FloorPlanSurvey(it.id, it.name, it.imageUri, it.isBuiltInSample, it.createdAtMillis, it.isDemo) }
    }

  override fun observePlacementRuns(surveyId: Long): Flow<List<PlacementRun>> =
    dao.observePlacementRuns(surveyId).map { list ->
      list.map { PlacementRun(it.id, it.surveyId, it.name, it.targetBssid, it.targetSsid, it.createdAtMillis, it.isDemo) }
    }

  override fun observeRouterPins(runId: Long): Flow<List<RouterPin>> =
    dao.observeRouterPins(runId).map { list ->
      list.map { RouterPin(it.id, it.runId, it.label, it.xNorm, it.yNorm) }
    }

  override fun observeIndoorSamples(runId: Long): Flow<List<IndoorSample>> =
    dao.observeIndoorSamples(runId).map { list -> list.map { it.toDomain() } }

  override suspend fun createWalk(name: String): Long {
    return dao.insertWalk(
      WalkSessionEntity(name = name, createdAtMillis = System.currentTimeMillis(), isDemo = false),
    )
  }

  override suspend fun deleteWalk(walkId: Long) {
    dao.deleteWalk(walkId)
  }

  override suspend fun recordScanBatch(
    walkId: Long,
    trackPoint: WalkTrackPoint?,
    readings: List<WifiReading>,
  ) {
    db.withTransaction {
      if (trackPoint != null) {
        dao.insertTrackPoints(
          listOf(
            WalkTrackPointEntity(
              walkId = walkId,
              latitude = trackPoint.latitude,
              longitude = trackPoint.longitude,
              accuracyMeters = trackPoint.accuracyMeters,
              timestampMillis = trackPoint.timestampMillis,
            ),
          ),
        )
      }
      if (readings.isNotEmpty()) {
        dao.insertReadings(readings.map { it.toEntity(walkId) })
      }
    }
  }

  override suspend fun createFloorPlanSurvey(
    name: String,
    imageUri: String?,
    isBuiltInSample: Boolean,
  ): Long {
    return db.withTransaction {
      val surveyId = dao.insertFloorPlanSurvey(
        FloorPlanSurveyEntity(
          name = name,
          imageUri = imageUri,
          isBuiltInSample = isBuiltInSample,
          createdAtMillis = System.currentTimeMillis(),
          isDemo = false,
        ),
      )
      dao.insertPlacementRun(
        PlacementRunEntity(
          surveyId = surveyId,
          name = "Spot A - Initial Run",
          targetBssid = null,
          targetSsid = "All Visible APs",
          createdAtMillis = System.currentTimeMillis(),
          isDemo = false,
        ),
      )
      surveyId
    }
  }

  override suspend fun deleteFloorPlanSurvey(surveyId: Long) {
    dao.deleteFloorPlanSurvey(surveyId)
  }

  override suspend fun createPlacementRun(
    surveyId: Long,
    name: String,
    targetBssid: String?,
    targetSsid: String?,
  ): Long {
    return dao.insertPlacementRun(
      PlacementRunEntity(
        surveyId = surveyId,
        name = name,
        targetBssid = targetBssid,
        targetSsid = targetSsid,
        createdAtMillis = System.currentTimeMillis(),
        isDemo = false,
      ),
    )
  }

  override suspend fun deletePlacementRun(runId: Long) {
    dao.deletePlacementRun(runId)
  }

  override suspend fun upsertRouterPin(pin: RouterPin): Long {
    return dao.upsertRouterPin(
      RouterPinEntity(
        id = pin.id,
        runId = pin.runId,
        label = pin.label,
        xNorm = pin.xNorm.coerceIn(0.02f, 0.98f),
        yNorm = pin.yNorm.coerceIn(0.02f, 0.98f),
      ),
    )
  }

  override suspend fun deleteRouterPin(pinId: Long) {
    dao.deleteRouterPin(pinId)
  }

  override suspend fun addIndoorSamples(samples: List<IndoorSample>) {
    if (samples.isEmpty()) return
    dao.insertIndoorSamples(samples.map { it.toEntity() })
  }

  override suspend fun ensureDemoDataSeededIfEmpty() {
    val prefs = prefsStore.preferencesFlow.first()
    if (!prefs.demoSeededOnce && dao.countWalks() == 0 && dao.countFloorPlanSurveys() == 0) {
      val (walkId, surveyId) = seedDemoDataInternal()
      prefsStore.setDemoSeededOnce(true)
      prefsStore.setDemoModeEnabled(true)
      prefsStore.setSelectedWalkId(walkId)
      prefsStore.setSelectedSurveyId(surveyId)
    }
  }

  override suspend fun resetDemoData(): Pair<Long, Long> {
    val ids = db.withTransaction {
      dao.deleteAllWalks()
      dao.deleteAllFloorPlanSurveys()
      seedDemoDataInternal()
    }
    prefsStore.setDemoSeededOnce(true)
    prefsStore.setDemoModeEnabled(true)
    prefsStore.setSelectedWalkId(ids.first)
    prefsStore.setSelectedSurveyId(ids.second)
    return ids
  }

  override suspend fun clearAllDataAndCreateBlankWorkspace(): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val ids = db.withTransaction {
      dao.deleteAllWalks()
      dao.deleteAllFloorPlanSurveys()
      val blankWalkId = dao.insertWalk(
        WalkSessionEntity(
          name = "Live Field Walk",
          createdAtMillis = now,
          isDemo = false,
        ),
      )
      val blankSurveyId = dao.insertFloorPlanSurvey(
        FloorPlanSurveyEntity(
          name = "Home Floor Survey",
          imageUri = null,
          isBuiltInSample = true,
          createdAtMillis = now,
          isDemo = false,
        ),
      )
      dao.insertPlacementRun(
        PlacementRunEntity(
          surveyId = blankSurveyId,
          name = "Spot A - Baseline",
          targetBssid = null,
          targetSsid = "All Visible APs",
          createdAtMillis = now,
          isDemo = false,
        ),
      )
      blankWalkId to blankSurveyId
    }
    prefsStore.setDemoModeEnabled(false)
    prefsStore.setSelectedWalkId(ids.first)
    prefsStore.setSelectedSurveyId(ids.second)
    return ids
  }

  private suspend fun seedDemoDataInternal(): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val walkBaseTime = now - 60 * 60 * 1000L

    val walkId = dao.insertWalk(
      WalkSessionEntity(
        name = "Demo Walk · Mission & 24th",
        createdAtMillis = walkBaseTime,
        isDemo = true,
      ),
    )
    val track = DemoDatasetFactory.buildOutdoorTrack(walkId, walkBaseTime)
    dao.insertTrackPoints(
      track.map {
        WalkTrackPointEntity(
          walkId = walkId,
          latitude = it.latitude,
          longitude = it.longitude,
          accuracyMeters = it.accuracyMeters,
          timestampMillis = it.timestampMillis,
        )
      },
    )
    val readings = DemoDatasetFactory.buildOutdoorReadings(walkId, walkBaseTime)
    dao.insertReadings(readings.map { it.toEntity(walkId) })

    val surveyId = dao.insertFloorPlanSurvey(
      FloorPlanSurveyEntity(
        name = "Craftsman Main Floor (Demo)",
        imageUri = null,
        isBuiltInSample = true,
        createdAtMillis = walkBaseTime,
        isDemo = true,
      ),
    )

    val run1Id = dao.insertPlacementRun(
      PlacementRunEntity(
        surveyId = surveyId,
        name = "Router in Living Room",
        targetBssid = "24:5A:4C:11:2A:02",
        targetSsid = "ToastLab-5G",
        createdAtMillis = walkBaseTime,
        isDemo = true,
      ),
    )
    val (pin1, samples1) = DemoDatasetFactory.buildIndoorRunLivingRoom(run1Id, walkBaseTime)
    dao.upsertRouterPin(
      RouterPinEntity(runId = run1Id, label = pin1.label, xNorm = pin1.xNorm, yNorm = pin1.yNorm),
    )
    dao.insertIndoorSamples(samples1.map { it.toEntity() })

    val run2Id = dao.insertPlacementRun(
      PlacementRunEntity(
        surveyId = surveyId,
        name = "Router in Central Hallway",
        targetBssid = "24:5A:4C:11:2A:02",
        targetSsid = "ToastLab-5G",
        createdAtMillis = walkBaseTime + 60_000L,
        isDemo = true,
      ),
    )
    val (pin2, samples2) = DemoDatasetFactory.buildIndoorRunHallway(run2Id, walkBaseTime + 60_000L)
    dao.upsertRouterPin(
      RouterPinEntity(runId = run2Id, label = pin2.label, xNorm = pin2.xNorm, yNorm = pin2.yNorm),
    )
    dao.insertIndoorSamples(samples2.map { it.toEntity() })

    return walkId to surveyId
  }

  private fun WifiReadingEntity.toDomain() = WifiReading(
    id = id,
    walkId = walkId,
    bssid = bssid,
    ssid = ssid,
    rssiDbm = rssiDbm,
    frequencyMhz = frequencyMhz,
    primaryChannel = primaryChannel,
    channelWidthMhz = channelWidthMhz,
    band = enumValueOrDefault(bandName, WifiBand.UNKNOWN),
    security = enumValueOrDefault(securityName, WifiSecurity.UNKNOWN),
    capabilitiesRaw = capabilitiesRaw,
    wifiGeneration = enumValueOrDefault(wifiGenerationName, WifiGeneration.UNKNOWN),
    latitude = latitude,
    longitude = longitude,
    altitudeMeters = altitudeMeters,
    accuracyMeters = accuracyMeters,
    timestampMillis = timestampMillis,
  )

  private fun WifiReading.toEntity(targetWalkId: Long) = WifiReadingEntity(
    walkId = targetWalkId,
    bssid = bssid,
    ssid = ssid,
    rssiDbm = rssiDbm,
    frequencyMhz = frequencyMhz,
    primaryChannel = primaryChannel,
    channelWidthMhz = channelWidthMhz,
    bandName = band.name,
    securityName = security.name,
    capabilitiesRaw = capabilitiesRaw,
    wifiGenerationName = wifiGeneration.name,
    latitude = latitude,
    longitude = longitude,
    altitudeMeters = altitudeMeters,
    accuracyMeters = accuracyMeters,
    timestampMillis = timestampMillis,
  )

  private fun IndoorSampleEntity.toDomain() = IndoorSample(
    id = id,
    runId = runId,
    xNorm = xNorm,
    yNorm = yNorm,
    bssid = bssid,
    ssid = ssid,
    rssiDbm = rssiDbm,
    frequencyMhz = frequencyMhz,
    primaryChannel = primaryChannel,
    channelWidthMhz = channelWidthMhz,
    band = enumValueOrDefault(bandName, WifiBand.UNKNOWN),
    security = enumValueOrDefault(securityName, WifiSecurity.UNKNOWN),
    wifiGeneration = enumValueOrDefault(wifiGenerationName, WifiGeneration.UNKNOWN),
    timestampMillis = timestampMillis,
  )

  private fun IndoorSample.toEntity() = IndoorSampleEntity(
    runId = runId,
    xNorm = xNorm,
    yNorm = yNorm,
    bssid = bssid,
    ssid = ssid,
    rssiDbm = rssiDbm,
    frequencyMhz = frequencyMhz,
    primaryChannel = primaryChannel,
    channelWidthMhz = channelWidthMhz,
    bandName = band.name,
    securityName = security.name,
    wifiGenerationName = wifiGeneration.name,
    timestampMillis = timestampMillis,
  )

  private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T {
    return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
  }
}
