package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "screenshot_preferences")

data class UserPreferences(
    val autoScanNew: Boolean,
    val scanFrequencyMinutes: Int,
    val enableAiAnalysis: Boolean,
    val customApiKey: String,
    val aiProvider: String,
    val themeMode: String,
    val hasCompletedFirstScan: Boolean,
    val lastUpdateCheckTime: Long = 0L,
    val autoCheckUpdateFrequencyDays: Int = 7,
    val customUpdateUrl: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val AUTO_SCAN_NEW = booleanPreferencesKey("auto_scan_new")
        val SCAN_FREQUENCY = intPreferencesKey("scan_frequency_minutes")
        val ENABLE_AI = booleanPreferencesKey("enable_ai_analysis")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAS_COMPLETED_FIRST_SCAN = booleanPreferencesKey("has_completed_first_scan")
        val LAST_UPDATE_CHECK_TIME = longPreferencesKey("last_update_check_time")
        val AUTO_CHECK_UPDATE_FREQUENCY_DAYS = intPreferencesKey("auto_check_update_frequency_days")
        val CUSTOM_UPDATE_URL = stringPreferencesKey("custom_update_url")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            autoScanNew = preferences[PreferencesKeys.AUTO_SCAN_NEW] ?: true,
            scanFrequencyMinutes = preferences[PreferencesKeys.SCAN_FREQUENCY] ?: 60,
            enableAiAnalysis = preferences[PreferencesKeys.ENABLE_AI] ?: false,
            customApiKey = preferences[PreferencesKeys.CUSTOM_API_KEY] ?: "",
            aiProvider = preferences[PreferencesKeys.AI_PROVIDER] ?: "gemini",
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "system",
            hasCompletedFirstScan = preferences[PreferencesKeys.HAS_COMPLETED_FIRST_SCAN] ?: false,
            lastUpdateCheckTime = preferences[PreferencesKeys.LAST_UPDATE_CHECK_TIME] ?: 0L,
            autoCheckUpdateFrequencyDays = preferences[PreferencesKeys.AUTO_CHECK_UPDATE_FREQUENCY_DAYS] ?: 7,
            customUpdateUrl = preferences[PreferencesKeys.CUSTOM_UPDATE_URL] ?: ""
        )
    }

    suspend fun setLastUpdateCheckTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_UPDATE_CHECK_TIME] = timestamp
        }
    }

    suspend fun setAutoCheckUpdateFrequencyDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CHECK_UPDATE_FREQUENCY_DAYS] = days
        }
    }

    suspend fun setCustomUpdateUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_UPDATE_URL] = url
        }
    }

    suspend fun setAutoScanNew(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCAN_NEW] = enabled
        }
    }

    suspend fun setScanFrequencyMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCAN_FREQUENCY] = minutes
        }
    }

    suspend fun setEnableAiAnalysis(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_AI] = enabled
        }
    }

    suspend fun setCustomApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_API_KEY] = apiKey
        }
    }

    suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_PROVIDER] = provider
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setHasCompletedFirstScan(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_FIRST_SCAN] = completed
        }
    }
}
