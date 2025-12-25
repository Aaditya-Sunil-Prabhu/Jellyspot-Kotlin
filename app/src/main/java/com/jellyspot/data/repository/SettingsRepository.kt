package com.jellyspot.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for app settings using DataStore.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // Source settings
        val SOURCE_MODE = stringPreferencesKey("source_mode") // jellyfin, local, both
        val DATA_SOURCE = stringPreferencesKey("data_source") // jellyfin, local, youtube
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        
        // Audio settings
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality") // lossless, high, low, auto
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        
        // Appearance settings
        val ADAPTIVE_BACKGROUND = booleanPreferencesKey("adaptive_background")
        val BACKGROUND_TYPE = stringPreferencesKey("background_type") // off, blurred, blurhash
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val SHOW_TECHNICAL_DETAILS = booleanPreferencesKey("show_technical_details")
        
        // Download settings
        val DOWNLOAD_PATH = stringPreferencesKey("download_path")
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        
        // Jellyfin settings
        val JELLYFIN_SERVER_URL = stringPreferencesKey("jellyfin_server_url")
        val JELLYFIN_USER_ID = stringPreferencesKey("jellyfin_user_id")
        val JELLYFIN_TOKEN = stringPreferencesKey("jellyfin_token")
        val JELLYFIN_DEVICE_ID = stringPreferencesKey("jellyfin_device_id")
        val SELECTED_JELLYFIN_LIBRARIES = stringSetPreferencesKey("selected_jellyfin_libraries")
        
        // Local profile
        val LOCAL_PROFILE_NAME = stringPreferencesKey("local_profile_name")
        val LOCAL_PROFILE_IMAGE = stringPreferencesKey("local_profile_image")
        val SELECTED_FOLDER_PATHS = stringSetPreferencesKey("selected_folder_paths")
    }

    // Source Mode
    val sourceMode: Flow<String> = dataStore.data.map { it[SOURCE_MODE] ?: "both" }
    suspend fun setSourceMode(mode: String) = dataStore.edit { it[SOURCE_MODE] = mode }

    val dataSource: Flow<String> = dataStore.data.map { it[DATA_SOURCE] ?: "local" }
    suspend fun setDataSource(source: String) = dataStore.edit { it[DATA_SOURCE] = source }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }
    suspend fun setOnboardingComplete(complete: Boolean) = dataStore.edit { it[ONBOARDING_COMPLETE] = complete }

    // Audio Settings
    val audioQuality: Flow<String> = dataStore.data.map { it[AUDIO_QUALITY] ?: "lossless" }
    suspend fun setAudioQuality(quality: String) = dataStore.edit { it[AUDIO_QUALITY] = quality }

    val crossfadeEnabled: Flow<Boolean> = dataStore.data.map { it[CROSSFADE_ENABLED] ?: false }
    suspend fun setCrossfadeEnabled(enabled: Boolean) = dataStore.edit { it[CROSSFADE_ENABLED] = enabled }

    val crossfadeDuration: Flow<Int> = dataStore.data.map { it[CROSSFADE_DURATION] ?: 5 }
    suspend fun setCrossfadeDuration(duration: Int) = dataStore.edit { it[CROSSFADE_DURATION] = duration }

    // Appearance Settings
    val adaptiveBackground: Flow<Boolean> = dataStore.data.map { it[ADAPTIVE_BACKGROUND] ?: true }
    suspend fun setAdaptiveBackground(enabled: Boolean) = dataStore.edit { it[ADAPTIVE_BACKGROUND] = enabled }

    val backgroundType: Flow<String> = dataStore.data.map { it[BACKGROUND_TYPE] ?: "blurred" }
    suspend fun setBackgroundType(type: String) = dataStore.edit { it[BACKGROUND_TYPE] = type }

    val amoledMode: Flow<Boolean> = dataStore.data.map { it[AMOLED_MODE] ?: false }
    suspend fun setAmoledMode(enabled: Boolean) = dataStore.edit { it[AMOLED_MODE] = enabled }

    val showTechnicalDetails: Flow<Boolean> = dataStore.data.map { it[SHOW_TECHNICAL_DETAILS] ?: false }
    suspend fun setShowTechnicalDetails(enabled: Boolean) = dataStore.edit { it[SHOW_TECHNICAL_DETAILS] = enabled }

    // Download Settings
    val downloadPath: Flow<String?> = dataStore.data.map { it[DOWNLOAD_PATH] }
    suspend fun setDownloadPath(path: String?) = dataStore.edit { 
        if (path != null) it[DOWNLOAD_PATH] = path else it.remove(DOWNLOAD_PATH)
    }

    val maxConcurrentDownloads: Flow<Int> = dataStore.data.map { it[MAX_CONCURRENT_DOWNLOADS] ?: 1 }
    suspend fun setMaxConcurrentDownloads(count: Int) = dataStore.edit { it[MAX_CONCURRENT_DOWNLOADS] = count }

    val wifiOnlyDownloads: Flow<Boolean> = dataStore.data.map { it[WIFI_ONLY_DOWNLOADS] ?: false }
    suspend fun setWifiOnlyDownloads(enabled: Boolean) = dataStore.edit { it[WIFI_ONLY_DOWNLOADS] = enabled }

    // Jellyfin Settings
    val jellyfinServerUrl: Flow<String?> = dataStore.data.map { it[JELLYFIN_SERVER_URL] }
    suspend fun setJellyfinServerUrl(url: String?) = dataStore.edit {
        if (url != null) it[JELLYFIN_SERVER_URL] = url else it.remove(JELLYFIN_SERVER_URL)
    }

    val jellyfinToken: Flow<String?> = dataStore.data.map { it[JELLYFIN_TOKEN] }
    suspend fun setJellyfinToken(token: String?) = dataStore.edit {
        if (token != null) it[JELLYFIN_TOKEN] = token else it.remove(JELLYFIN_TOKEN)
    }

    val jellyfinUserId: Flow<String?> = dataStore.data.map { it[JELLYFIN_USER_ID] }
    suspend fun setJellyfinUserId(id: String?) = dataStore.edit {
        if (id != null) it[JELLYFIN_USER_ID] = id else it.remove(JELLYFIN_USER_ID)
    }

    // Local Profile
    val localProfileName: Flow<String> = dataStore.data.map { it[LOCAL_PROFILE_NAME] ?: "User" }
    suspend fun setLocalProfileName(name: String) = dataStore.edit { it[LOCAL_PROFILE_NAME] = name }

    val selectedFolderPaths: Flow<Set<String>> = dataStore.data.map { it[SELECTED_FOLDER_PATHS] ?: emptySet() }
    suspend fun setSelectedFolderPaths(paths: Set<String>) = dataStore.edit { it[SELECTED_FOLDER_PATHS] = paths }

    // Clear Jellyfin auth
    suspend fun clearJellyfinAuth() = dataStore.edit {
        it.remove(JELLYFIN_SERVER_URL)
        it.remove(JELLYFIN_USER_ID)
        it.remove(JELLYFIN_TOKEN)
    }
}
