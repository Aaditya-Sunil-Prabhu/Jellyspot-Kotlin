package com.jellyspot.data.repository

import com.jellyspot.data.local.dao.PlaylistDao
import com.jellyspot.data.local.dao.TrackDao
import com.jellyspot.data.local.entities.PlaylistEntity
import com.jellyspot.data.local.entities.PlaylistTrackCrossRef
import com.jellyspot.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for playlist operations.
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao
) {
    /**
     * Get all playlists.
     */
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    /**
     * Get local playlists only.
     */
    fun getLocalPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getPlaylistsBySource("local")

    /**
     * Get a playlist by ID.
     */
    suspend fun getPlaylistById(id: String): PlaylistEntity? = playlistDao.getPlaylistById(id)

    /**
     * Get tracks in a playlist.
     */
    fun getPlaylistTracks(playlistId: String): Flow<List<TrackEntity>> = 
        playlistDao.getPlaylistTracks(playlistId)

    /**
     * Get track count for a playlist.
     */
    suspend fun getPlaylistTrackCount(playlistId: String): Int = 
        playlistDao.getPlaylistTrackCount(playlistId)

    /**
     * Create a new local playlist.
     */
    suspend fun createPlaylist(name: String, description: String? = null): PlaylistEntity {
        val playlist = PlaylistEntity(
            id = "playlist_${UUID.randomUUID()}",
            name = name,
            description = description,
            source = "local",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(playlist)
        return playlist
    }

    /**
     * Rename a playlist.
     */
    suspend fun renamePlaylist(playlistId: String, newName: String) {
        val playlist = playlistDao.getPlaylistById(playlistId)
        playlist?.let {
            playlistDao.updatePlaylist(
                it.copy(name = newName, updatedAt = System.currentTimeMillis())
            )
        }
    }

    /**
     * Update playlist description.
     */
    suspend fun updatePlaylistDescription(playlistId: String, description: String?) {
        val playlist = playlistDao.getPlaylistById(playlistId)
        playlist?.let {
            playlistDao.updatePlaylist(
                it.copy(description = description, updatedAt = System.currentTimeMillis())
            )
        }
    }

    /**
     * Delete a playlist.
     */
    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.clearPlaylist(playlistId) // Remove all tracks first
        playlistDao.deletePlaylistById(playlistId)
    }

    /**
     * Add a track to a playlist.
     */
    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        val maxPosition = playlistDao.getMaxPosition(playlistId) ?: -1
        val crossRef = PlaylistTrackCrossRef(
            playlistId = playlistId,
            trackId = trackId,
            position = maxPosition + 1,
            addedAt = System.currentTimeMillis()
        )
        playlistDao.addTrackToPlaylist(crossRef)
        
        // Update playlist timestamp
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Add multiple tracks to a playlist.
     */
    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) {
        var position = (playlistDao.getMaxPosition(playlistId) ?: -1) + 1
        val now = System.currentTimeMillis()
        
        trackIds.forEach { trackId ->
            val crossRef = PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = position++,
                addedAt = now
            )
            playlistDao.addTrackToPlaylist(crossRef)
        }
        
        // Update playlist timestamp
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Remove a track from a playlist.
     */
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        
        // Update playlist timestamp
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Reorder tracks in a playlist.
     */
    suspend fun reorderPlaylistTrack(playlistId: String, trackId: String, newPosition: Int) {
        playlistDao.updateTrackPosition(playlistId, trackId, newPosition)
    }

    /**
     * Clear all tracks from a playlist.
     */
    suspend fun clearPlaylist(playlistId: String) {
        playlistDao.clearPlaylist(playlistId)
    }

    /**
     * Update playlist image from first track.
     */
    suspend fun updatePlaylistImage(playlistId: String) {
        // Get first track's image as playlist image
        // This would be expanded in a full implementation
    }
}
