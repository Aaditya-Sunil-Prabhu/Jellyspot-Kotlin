package com.jellyspot.data.repository

import com.jellyspot.data.local.entities.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.model.api.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Jellyfin server operations.
 */
@Singleton
class JellyfinRepository @Inject constructor(
    private val jellyfin: Jellyfin,
    private val settingsRepository: SettingsRepository
) {
    private var apiClient: ApiClient? = null
    private var userId: UUID? = null

    /**
     * Check if authenticated.
     */
    suspend fun isAuthenticated(): Boolean {
        val serverUrl = settingsRepository.jellyfinServerUrl.first()
        val token = settingsRepository.jellyfinToken.first()
        return serverUrl != null && token != null
    }

    /**
     * Login to Jellyfin server.
     */
    suspend fun login(
        serverUrl: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Create API client
            val api = jellyfin.createApi(serverUrl)
            
            // Authenticate
            val authResult by api.userApi.authenticateUserByName(
                authenticateUserByName = AuthenticateUserByName(
                    username = username,
                    pw = password
                )
            )

            // Store credentials
            val user = authResult.user ?: return@withContext Result.failure(Exception("No user returned"))
            val token = authResult.accessToken ?: return@withContext Result.failure(Exception("No token returned"))
            
            settingsRepository.setJellyfinServerUrl(serverUrl)
            settingsRepository.setJellyfinToken(token)
            settingsRepository.setJellyfinUserId(user.id.toString())
            
            // Update client with token
            apiClient = jellyfin.createApi(
                baseUrl = serverUrl,
                accessToken = token
            )
            userId = user.id
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initialize API client from stored credentials.
     */
    suspend fun initializeFromStorage(): Boolean = withContext(Dispatchers.IO) {
        val serverUrl = settingsRepository.jellyfinServerUrl.first() ?: return@withContext false
        val token = settingsRepository.jellyfinToken.first() ?: return@withContext false
        val userIdStr = settingsRepository.jellyfinUserId.first() ?: return@withContext false
        
        try {
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
    suspend fun getLatestMusic(limit: Int = 20): List<TrackEntity> = withContext(Dispatchers.IO) {
        val api = apiClient ?: return@withContext emptyList()
        val uid = userId ?: return@withContext emptyList()
        
        try {
            val result by api.userLibraryApi.getLatestMedia(
                userId = uid,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                limit = limit
            )
            result.mapNotNull { mapToTrack(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get resume items (continue listening).
     */
    suspend fun getResumeItems(limit: Int = 20): List<TrackEntity> = withContext(Dispatchers.IO) {
        val api = apiClient ?: return@withContext emptyList()
        val uid = userId ?: return@withContext emptyList()
        
        try {
            val result by api.itemsApi.getResumeItems(
                userId = uid,
                mediaTypes = listOf(MediaType.AUDIO),
                limit = limit
            )
            result.items?.mapNotNull { mapToTrack(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Search for tracks.
     */
    suspend fun search(query: String, limit: Int = 50): List<TrackEntity> = withContext(Dispatchers.IO) {
        val api = apiClient ?: return@withContext emptyList()
        val uid = userId ?: return@withContext emptyList()
        
        try {
            val result by api.itemsApi.getItems(
                userId = uid,
                searchTerm = query,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                recursive = true,
                limit = limit,
                sortBy = listOf(ItemSortBy.SORT_NAME)
            )
            result.items?.mapNotNull { mapToTrack(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get all music items.
     */
    suspend fun getAllMusic(limit: Int = 500): List<TrackEntity> = withContext(Dispatchers.IO) {
        val api = apiClient ?: return@withContext emptyList()
        val uid = userId ?: return@withContext emptyList()
        
        try {
            val result by api.itemsApi.getItems(
                userId = uid,
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                recursive = true,
                limit = limit,
                sortBy = listOf(ItemSortBy.SORT_NAME)
            )
            result.items?.mapNotNull { mapToTrack(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get tracks by album.
     */
    suspend fun getAlbumTracks(albumId: String): List<TrackEntity> = withContext(Dispatchers.IO) {
        val api = apiClient ?: return@withContext emptyList()
        val uid = userId ?: return@withContext emptyList()
        
        try {
            val result by api.itemsApi.getItems(
                userId = uid,
                parentId = UUID.fromString(albumId),
                includeItemTypes = listOf(BaseItemKind.AUDIO),
                sortBy = listOf(ItemSortBy.INDEX_NUMBER)
            )
            result.items?.mapNotNull { mapToTrack(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get stream URL for a track.
     */
    suspend fun getStreamUrl(trackId: String): String? {
        val api = apiClient ?: return null
        val serverUrl = settingsRepository.jellyfinServerUrl.first() ?: return null
        val token = settingsRepository.jellyfinToken.first() ?: return null
        val uid = userId ?: return null
        
        return "$serverUrl/Audio/$trackId/stream?static=true&UserId=$uid&api_key=$token&Container=opus,mp3,aac&AudioCodec=opus,mp3,aac"
    }

    /**
     * Get lyrics for a track.
     */
    suspend fun getLyrics(trackId: String): String? = withContext(Dispatchers.IO) {
        val api = apiClient ?: return@withContext null
        
        try {
            val result by api.audioApi.getLyrics(UUID.fromString(trackId))
            result.lyrics?.joinToString("\n") { lyric ->
                lyric.text ?: ""
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Report playback started.
     */
    suspend fun reportPlaybackStart(trackId: String) = withContext(Dispatchers.IO) {
        val api = apiClient ?: return@withContext
        try {
            api.sessionApi.reportPlaybackStart(
                PlaybackStartInfo(
                    itemId = UUID.fromString(trackId),
                    canSeek = true,
                    isMuted = false,
                    isPaused = false
                )
            )
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    /**
     * Report playback stopped.
     */
    suspend fun reportPlaybackStopped(trackId: String, positionMs: Long) = withContext(Dispatchers.IO) {
        val api = apiClient ?: return@withContext
        try {
            api.sessionApi.reportPlaybackStopped(
                PlaybackStopInfo(
                    itemId = UUID.fromString(trackId),
                    positionTicks = positionMs * 10000
                )
            )
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    /**
     * Get image URL for an item.
     */
    fun getImageUrl(itemId: String, imageType: String = "Primary"): String? {
        val serverUrl = runCatching { 
            kotlinx.coroutines.runBlocking { settingsRepository.jellyfinServerUrl.first() }
        }.getOrNull() ?: return null
        return "$serverUrl/Items/$itemId/Images/$imageType"
    }

    /**
     * Map Jellyfin item to TrackEntity.
     */
    private fun mapToTrack(item: BaseItemDto): TrackEntity? {
        val id = item.id?.toString() ?: return null
        return TrackEntity(
            id = "jellyfin_$id",
            name = item.name ?: "Unknown",
            artist = item.albumArtist ?: item.artists?.firstOrNull() ?: "Unknown Artist",
            album = item.album ?: "Unknown Album",
            albumId = item.albumId?.toString()?.let { "jellyfin_$it" },
            artistId = item.artistItems?.firstOrNull()?.id?.toString()?.let { "jellyfin_$it" },
            durationMillis = (item.runTimeTicks ?: 0) / 10000,
            streamUrl = "", // Will be resolved when playing
            imageUrl = getImageUrl(id),
            source = "jellyfin",
            bitrate = item.mediaSources?.firstOrNull()?.bitrate,
            codec = item.mediaSources?.firstOrNull()?.container,
            trackNumber = item.indexNumber
        )
    }
}
