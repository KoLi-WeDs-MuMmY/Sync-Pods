package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bluePulseDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "bluepulse_preferences"
)

data class AppPreferences(
  val themeMode: ThemeMode = ThemeMode.AMOLED,
  val systemOverlayEnabled: Boolean = true,
  val islandAutoCollapseSec: Int = 5,
  val radarHapticsEnabled: Boolean = true,
  val permissionOnboardingDismissed: Boolean = false,
)

class AppPreferencesRepository(private val context: Context) {
  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val SYSTEM_OVERLAY_ENABLED = booleanPreferencesKey("system_overlay_enabled")
    val ISLAND_AUTO_COLLAPSE_SEC = intPreferencesKey("island_auto_collapse_sec")
    val RADAR_HAPTICS_ENABLED = booleanPreferencesKey("radar_haptics_enabled")
    val ONBOARDING_DISMISSED = booleanPreferencesKey("onboarding_dismissed")
  }

  val preferencesFlow: Flow<AppPreferences> = context.bluePulseDataStore.data.map { prefs ->
    val modeName = prefs[Keys.THEME_MODE] ?: ThemeMode.AMOLED.name
    val themeMode = ThemeMode.entries.firstOrNull { it.name == modeName } ?: ThemeMode.AMOLED
    AppPreferences(
      themeMode = themeMode,
      systemOverlayEnabled = prefs[Keys.SYSTEM_OVERLAY_ENABLED] ?: true,
      islandAutoCollapseSec = prefs[Keys.ISLAND_AUTO_COLLAPSE_SEC] ?: 5,
      radarHapticsEnabled = prefs[Keys.RADAR_HAPTICS_ENABLED] ?: true,
      permissionOnboardingDismissed = prefs[Keys.ONBOARDING_DISMISSED] ?: false,
    )
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.bluePulseDataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setSystemOverlayEnabled(enabled: Boolean) {
    context.bluePulseDataStore.edit { it[Keys.SYSTEM_OVERLAY_ENABLED] = enabled }
  }

  suspend fun setIslandAutoCollapseSec(seconds: Int) {
    context.bluePulseDataStore.edit { it[Keys.ISLAND_AUTO_COLLAPSE_SEC] = seconds.coerceIn(2, 15) }
  }

  suspend fun setRadarHapticsEnabled(enabled: Boolean) {
    context.bluePulseDataStore.edit { it[Keys.RADAR_HAPTICS_ENABLED] = enabled }
  }

  suspend fun setPermissionOnboardingDismissed(dismissed: Boolean) {
    context.bluePulseDataStore.edit { it[Keys.ONBOARDING_DISMISSED] = dismissed }
  }
}
