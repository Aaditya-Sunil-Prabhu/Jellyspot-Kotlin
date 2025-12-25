package com.jellyspot.data.local.dao

import androidx.room.*
import com.jellyspot.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for track operations.
 */
@Dao
interface TrackDao {
    
    @Query("SELECT * FROM tracks ORDER BY name ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE source = :source ORDER BY name ASC")
    fun getTracksBySource(source: String): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE source = 'local' ORDER BY name ASC")
    fun getLocalTracks(): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?
    
    @Query("SELECT * FROM tracks WHERE albumId = :albumId ORDER BY trackNumber ASC")
    fun getTracksByAlbum(albumId: String): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE artistId = :artistId ORDER BY name ASC")
    fun getTracksByArtist(artistId: String): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE name LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 50")
    suspend fun searchTracks(query: String): List<TrackEntity>
    
    @Query("SELECT * FROM tracks ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 20): Flow<List<TrackEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)
    
    @Update
    suspend fun updateTrack(track: TrackEntity)
    
    @Delete
    suspend fun deleteTrack(track: TrackEntity)
    
    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)
    
    @Query("DELETE FROM tracks WHERE source = :source")
    suspend fun deleteTracksBySource(source: String)
    
    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)
    
    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM tracks WHERE source = 'local'")
    suspend fun getLocalTrackCount(): Int
}
