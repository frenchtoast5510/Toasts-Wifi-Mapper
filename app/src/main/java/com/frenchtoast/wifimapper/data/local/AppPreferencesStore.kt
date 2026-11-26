package com.frenchtoast.wifimapper.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "toasts_wifi_prefs")

data class AppPreferences(
  val isDemoModeEnabled: Boolean = true,
  val demoSeededOnce: Boolean = false,
  val showOutdoorHeatmap: Boolean = true,
  val selectedWalkId: Long = 0L,
  val selectedSurveyId: Long = 0L,
)

class AppPreferencesStore(private val context: Context) {
  private object Keys {
    val DEMO_MODE = booleanPreferencesKey("demo_mode_enabled")
    val DEMO_SEEDED = booleanPreferencesKey("demo_seeded_once")
    val SHOW_OUTDOOR_HEATMAP = booleanPreferencesKey("show_outdoor_heatmap")
    val SELECTED_WALK_ID = longPreferencesKey("selected_walk_id")
    val SELECTED_SURVEY_ID = longPreferencesKey("selected_survey_id")
  }

  val preferencesFlow: Flow<AppPreferences> = context.appDataStore.data.map { prefs ->
    AppPreferences(
      isDemoModeEnabled = prefs[Keys.DEMO_MODE] ?: true,
      demoSeededOnce = prefs[Keys.DEMO_SEEDED] ?: false,
      showOutdoorHeatmap = prefs[Keys.SHOW_OUTDOOR_HEATMAP] ?: true,
      selectedWalkId = prefs[Keys.SELECTED_WALK_ID] ?: 0L,
      selectedSurveyId = prefs[Keys.SELECTED_SURVEY_ID] ?: 0L,
    )
  }

  suspend fun setDemoModeEnabled(enabled: Boolean) {
    context.appDataStore.edit { it[Keys.DEMO_MODE] = enabled }
  }

  suspend fun setDemoSeededOnce(seeded: Boolean) {
    context.appDataStore.edit { it[Keys.DEMO_SEEDED] = seeded }
  }

  suspend fun setShowOutdoorHeatmap(show: Boolean) {
    context.appDataStore.edit { it[Keys.SHOW_OUTDOOR_HEATMAP] = show }
  }

  suspend fun setSelectedWalkId(walkId: Long) {
    context.appDataStore.edit { it[Keys.SELECTED_WALK_ID] = walkId }
  }

  suspend fun setSelectedSurveyId(surveyId: Long) {
    context.appDataStore.edit { it[Keys.SELECTED_SURVEY_ID] = surveyId }
  }
}
