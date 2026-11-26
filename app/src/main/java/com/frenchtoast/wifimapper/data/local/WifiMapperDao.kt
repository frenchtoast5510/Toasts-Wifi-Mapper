package com.frenchtoast.wifimapper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiMapperDao {
  @Query("SELECT * FROM walk_sessions ORDER BY createdAtMillis DESC")
  fun observeWalks(): Flow<List<WalkSessionEntity>>

  @Query("SELECT COUNT(*) FROM walk_sessions")
  suspend fun countWalks(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWalk(walk: WalkSessionEntity): Long

  @Query("DELETE FROM walk_sessions WHERE id = :walkId")
  suspend fun deleteWalk(walkId: Long)

  @Query("DELETE FROM walk_sessions")
  suspend fun deleteAllWalks()

  @Query("SELECT * FROM walk_track_points WHERE walkId = :walkId ORDER BY timestampMillis ASC")
  fun observeTrackForWalk(walkId: Long): Flow<List<WalkTrackPointEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrackPoints(points: List<WalkTrackPointEntity>)

  @Query("SELECT * FROM wifi_readings WHERE walkId = :walkId ORDER BY timestampMillis DESC")
  fun observeReadingsForWalk(walkId: Long): Flow<List<WifiReadingEntity>>

  @Query("SELECT * FROM wifi_readings ORDER BY timestampMillis DESC")
  fun observeAllReadings(): Flow<List<WifiReadingEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReadings(readings: List<WifiReadingEntity>)

  @Query("SELECT * FROM floor_plan_surveys ORDER BY createdAtMillis DESC")
  fun observeFloorPlanSurveys(): Flow<List<FloorPlanSurveyEntity>>

  @Query("SELECT COUNT(*) FROM floor_plan_surveys")
  suspend fun countFloorPlanSurveys(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFloorPlanSurvey(survey: FloorPlanSurveyEntity): Long

  @Query("DELETE FROM floor_plan_surveys WHERE id = :surveyId")
  suspend fun deleteFloorPlanSurvey(surveyId: Long)

  @Query("DELETE FROM floor_plan_surveys")
  suspend fun deleteAllFloorPlanSurveys()

  @Query("SELECT * FROM placement_runs WHERE surveyId = :surveyId ORDER BY createdAtMillis ASC")
  fun observePlacementRuns(surveyId: Long): Flow<List<PlacementRunEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlacementRun(run: PlacementRunEntity): Long

  @Query("DELETE FROM placement_runs WHERE id = :runId")
  suspend fun deletePlacementRun(runId: Long)

  @Query("SELECT * FROM router_pins WHERE runId = :runId ORDER BY id ASC")
  fun observeRouterPins(runId: Long): Flow<List<RouterPinEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRouterPin(pin: RouterPinEntity): Long

  @Query("DELETE FROM router_pins WHERE id = :pinId")
  suspend fun deleteRouterPin(pinId: Long)

  @Query("SELECT * FROM indoor_samples WHERE runId = :runId ORDER BY timestampMillis ASC")
  fun observeIndoorSamples(runId: Long): Flow<List<IndoorSampleEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertIndoorSamples(samples: List<IndoorSampleEntity>)
}
