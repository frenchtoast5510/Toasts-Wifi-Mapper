package com.frenchtoast.wifimapper.domain.repository

import com.frenchtoast.wifimapper.domain.model.FloorPlanSurvey
import com.frenchtoast.wifimapper.domain.model.IndoorSample
import com.frenchtoast.wifimapper.domain.model.PlacementRun
import com.frenchtoast.wifimapper.domain.model.RouterPin
import com.frenchtoast.wifimapper.domain.model.WalkSession
import com.frenchtoast.wifimapper.domain.model.WalkTrackPoint
import com.frenchtoast.wifimapper.domain.model.WifiReading
import kotlinx.coroutines.flow.Flow

interface WifiRepository {
  fun observeWalks(): Flow<List<WalkSession>>
  fun observeReadingsForWalk(walkId: Long): Flow<List<WifiReading>>
  fun observeAllReadings(): Flow<List<WifiReading>>
  fun observeTrackForWalk(walkId: Long): Flow<List<WalkTrackPoint>>

  fun observeFloorPlanSurveys(): Flow<List<FloorPlanSurvey>>
  fun observePlacementRuns(surveyId: Long): Flow<List<PlacementRun>>
  fun observeRouterPins(runId: Long): Flow<List<RouterPin>>
  fun observeIndoorSamples(runId: Long): Flow<List<IndoorSample>>

  suspend fun createWalk(name: String): Long
  suspend fun deleteWalk(walkId: Long)
  suspend fun recordScanBatch(
    walkId: Long,
    trackPoint: WalkTrackPoint?,
    readings: List<WifiReading>,
  )

  suspend fun createFloorPlanSurvey(name: String, imageUri: String?, isBuiltInSample: Boolean): Long
  suspend fun deleteFloorPlanSurvey(surveyId: Long)
  suspend fun createPlacementRun(
    surveyId: Long,
    name: String,
    targetBssid: String?,
    targetSsid: String?,
  ): Long
  suspend fun deletePlacementRun(runId: Long)
  suspend fun upsertRouterPin(pin: RouterPin): Long
  suspend fun deleteRouterPin(pinId: Long)
  suspend fun addIndoorSamples(samples: List<IndoorSample>)

  suspend fun ensureDemoDataSeededIfEmpty()
  suspend fun resetDemoData(): Pair<Long, Long>
  suspend fun clearAllDataAndCreateBlankWorkspace(): Pair<Long, Long>
}
