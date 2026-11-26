package com.frenchtoast.wifimapper.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "walk_sessions")
data class WalkSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val createdAtMillis: Long,
  val isDemo: Boolean = false,
)

@Entity(
  tableName = "walk_track_points",
  foreignKeys = [
    ForeignKey(
      entity = WalkSessionEntity::class,
      parentColumns = ["id"],
      childColumns = ["walkId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("walkId")],
)
data class WalkTrackPointEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val walkId: Long,
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val timestampMillis: Long,
)

@Entity(
  tableName = "wifi_readings",
  foreignKeys = [
    ForeignKey(
      entity = WalkSessionEntity::class,
      parentColumns = ["id"],
      childColumns = ["walkId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("walkId"), Index("bssid")],
)
data class WifiReadingEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val walkId: Long,
  val bssid: String,
  val ssid: String,
  val rssiDbm: Int,
  val frequencyMhz: Int,
  val primaryChannel: Int,
  val channelWidthMhz: Int,
  val bandName: String,
  val securityName: String,
  val capabilitiesRaw: String,
  val wifiGenerationName: String,
  val latitude: Double,
  val longitude: Double,
  val altitudeMeters: Double,
  val accuracyMeters: Float,
  val timestampMillis: Long,
)

@Entity(tableName = "floor_plan_surveys")
data class FloorPlanSurveyEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val imageUri: String?,
  val isBuiltInSample: Boolean,
  val createdAtMillis: Long,
  val isDemo: Boolean = false,
)

@Entity(
  tableName = "placement_runs",
  foreignKeys = [
    ForeignKey(
      entity = FloorPlanSurveyEntity::class,
      parentColumns = ["id"],
      childColumns = ["surveyId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("surveyId")],
)
data class PlacementRunEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val surveyId: Long,
  val name: String,
  val targetBssid: String?,
  val targetSsid: String?,
  val createdAtMillis: Long,
  val isDemo: Boolean = false,
)

@Entity(
  tableName = "router_pins",
  foreignKeys = [
    ForeignKey(
      entity = PlacementRunEntity::class,
      parentColumns = ["id"],
      childColumns = ["runId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("runId")],
)
data class RouterPinEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val runId: Long,
  val label: String,
  val xNorm: Float,
  val yNorm: Float,
)

@Entity(
  tableName = "indoor_samples",
  foreignKeys = [
    ForeignKey(
      entity = PlacementRunEntity::class,
      parentColumns = ["id"],
      childColumns = ["runId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("runId")],
)
data class IndoorSampleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val runId: Long,
  val xNorm: Float,
  val yNorm: Float,
  val bssid: String,
  val ssid: String,
  val rssiDbm: Int,
  val frequencyMhz: Int,
  val primaryChannel: Int,
  val channelWidthMhz: Int,
  val bandName: String,
  val securityName: String,
  val wifiGenerationName: String,
  val timestampMillis: Long,
)
