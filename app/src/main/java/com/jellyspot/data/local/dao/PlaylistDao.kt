package com.jellyspot.data.local.dao

import androidx.room.*
import com.jellyspot.data.local.entities.PlaylistEntity
import com.jellyspot.data.local.entities.PlaylistTrackCrossRef
import com.jellyspot.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for playlist operations.
 */
@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlists WHERE source = :source ORDER BY name ASC")
    fun getPlaylistsBySource(source: String): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)
    
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)
    
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
    
    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)
    
    // Playlist-Track relationship
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)
    
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)
    
    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
    """)
    fun getPlaylistTracks(playlistId: String): Flow<List<TrackEntity>>
    
    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getPlaylistTrackCount(playlistId: String): Int
    
    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?
    
    @Query("UPDATE playlist_tracks SET position = :newPosition WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updateTrackPosition(playlistId: String, trackId: String, newPosition: Int)
}
