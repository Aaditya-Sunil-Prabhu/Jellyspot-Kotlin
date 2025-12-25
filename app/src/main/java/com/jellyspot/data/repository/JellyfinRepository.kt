package com.jellyspot.data.repository

import android.content.Context
import com.jellyspot.data.local.entities.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Jellyfin server operations.
 */
@Singleton
class JellyfinRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private var apiClient: ApiClient? = null
    private var userId: UUID? = null

    private val jellyfin = Jellyfin {
        clientInfo = org.jellyfin.sdk.model.ClientInfo(
            name = "Jellyspot",
            version = "1.0.0"
        )
        deviceInfo = org.jellyfin.sdk.model.DeviceInfo(
            id = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ),
            name = android.os.Build.MODEL
        )
    }

    /**
     * Check if authenticated.
     */
    suspend fun isAuthenticated(): Boolean {
        return apiClient != null && userId != null
    }

    /**
     * Login to Jellyfin server.
     */
    suspend fun login(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Unit> {
        return try {
            val client = jellyfin.createApi(baseUrl = serverUrl)
            
            val authResult = client.userApi.authenticateUserByName(
                data = AuthenticateUserByName(
                    username = username,
                    pw = password
                )
            )
            
            val user = authResult.content
            val accessToken = user.accessToken ?: return Result.failure(Exception("No access token"))
            val userIdStr = user.user?.id?.toString() ?: return Result.failure(Exception("No user ID"))
            
            // Save credentials
            settingsRepository.setJellyfinServerUrl(serverUrl)
            settingsRepository.setJellyfinToken(accessToken)
            settingsRepository.setJellyfinUserId(userIdStr)
            
            // Create authenticated client
            apiClient = jellyfin.createApi(
                baseUrl = serverUrl,
                accessToken = accessToken
            )
            userId = user.user?.id
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initialize API client from stored credentials.
     */
    suspend fun initializeFromStorage(): Boolean {
        return try {
            val serverUrl = settingsRepository.jellyfinServerUrl.first() ?: return false
            val token = settingsRepository.jellyfinToken.first() ?: return false
            val userIdStr = settingsRepository.jellyfinUserId.first() ?: return false
            
            apiClient = jellyfin.createApi(
                baseUrl = serverUrl,
                accessToken = token
            )
            userId = UUID.fromString(userIdStr)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Logout and clear credentials.
     */
    suspend fun logout() {
        apiClient = null
        userId = null
        settingsRepository.clearJellyfinAuth()
    }

    /**
     * Get latest music items.
     */
    suspend fun getLatestMusic(limit: Int = 20): List<TrackEntity> {
        val client = apiClient ?: return emptyList()
        val uid = userId ?: return emptyList()
        
        return try {
            val response = client.itemsApi.getItems(
                userId = uid,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                limit = limit,
                sortBy = listOf(ItemSortBy.DATE_CREATED),
                sortOrder = listOf(SortOrder.DESCENDING),
                recursive = true,
                fields = listOf(ItemFields.PATH, ItemFields.MEDIA_SOURCES)
            )
            
            response.content.items?.mapNotNull { item ->
                TrackEntity(
                    id = "jellyfin_${item.id}",
                    name = item.name ?: "Unknown",
                    artist = item.albumArtist ?: item.artists?.firstOrNull() ?: "Unknown",
                    album = item.album ?: "Unknown",
                    albumId = item.albumId?.toString(),
                    artistId = item.artistItems?.firstOrNull()?.id?.toString(),
                    durationMs = (item.runTimeTicks ?: 0) / 10000,
                    streamUrl = getStreamUrl(item.id.toString()),
                    imageUrl = getImageUrl(item.id.toString()),
                    source = "jellyfin",
                    jellyfinItemId = item.id.toString()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Search for tracks.
     */
    suspend fun search(query: String, limit: Int = 50): List<TrackEntity> {
        val client = apiClient ?: return emptyList()
        val uid = userId ?: return emptyList()
        
        return try {
            val response = client.itemsApi.getItems(
                userId = uid,
                searchTerm = query,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                limit = limit,
                recursive = true,
                fields = listOf(ItemFields.PATH, ItemFields.MEDIA_SOURCES)
            )
            
            response.content.items?.mapNotNull { item ->
                TrackEntity(
                    id = "jellyfin_${item.id}",
                    name = item.name ?: "Unknown",
                    artist = item.albumArtist ?: item.artists?.firstOrNull() ?: "Unknown",
                    album = item.album ?: "Unknown",
                    albumId = item.albumId?.toString(),
                    artistId = item.artistItems?.firstOrNull()?.id?.toString(),
                    durationMs = (item.runTimeTicks ?: 0) / 10000,
                    streamUrl = getStreamUrl(item.id.toString()),
                    imageUrl = getImageUrl(item.id.toString()),
                    source = "jellyfin",
                    jellyfinItemId = item.id.toString()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get all music items.
     */
    suspend fun getAllMusic(limit: Int = 500): List<TrackEntity> {
        val client = apiClient ?: return emptyList()
        val uid = userId ?: return emptyList()
        
        return try {
            val response = client.itemsApi.getItems(
                userId = uid,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                limit = limit,
                sortBy = listOf(ItemSortBy.SORT_NAME),
                sortOrder = listOf(SortOrder.ASCENDING),
                recursive = true,
                fields = listOf(ItemFields.PATH, ItemFields.MEDIA_SOURCES)
            )
            
            response.content.items?.mapNotNull { item ->
                TrackEntity(
                    id = "jellyfin_${item.id}",
                    name = item.name ?: "Unknown",
                    artist = item.albumArtist ?: item.artists?.firstOrNull() ?: "Unknown",
                    album = item.album ?: "Unknown",
                    albumId = item.albumId?.toString(),
                    artistId = item.artistItems?.firstOrNull()?.id?.toString(),
                    durationMs = (item.runTimeTicks ?: 0) / 10000,
                    streamUrl = getStreamUrl(item.id.toString()),
                    imageUrl = getImageUrl(item.id.toString()),
                    source = "jellyfin",
                    jellyfinItemId = item.id.toString()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get stream URL for a track.
     */
    fun getStreamUrl(itemId: String): String? {
        val baseUrl = runCatching { 
            kotlinx.coroutines.runBlocking { settingsRepository.jellyfinServerUrl.first() }
        }.getOrNull() ?: return null
        
        return "$baseUrl/Audio/$itemId/universal?api_key=${getToken()}"
    }

    /**
     * Get image URL for an item.
     */
    fun getImageUrl(itemId: String, imageType: String = "Primary"): String? {
        val baseUrl = runCatching {
            kotlinx.coroutines.runBlocking { settingsRepository.jellyfinServerUrl.first() }
        }.getOrNull() ?: return null
        
        return "$baseUrl/Items/$itemId/Images/$imageType"
    }

    private fun getToken(): String? {
        return runCatching {
            kotlinx.coroutines.runBlocking { settingsRepository.jellyfinToken.first() }
        }.getOrNull()
    }
}
