package com.jellyspot.data.local.dao

import androidx.room.*
import com.jellyspot.data.local.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for download queue operations.
 */
@Dao
interface DownloadDao {
    
    @Query("SELECT * FROM downloads ORDER BY addedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY addedAt ASC")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE status = 'pending' ORDER BY addedAt ASC LIMIT 1")
    suspend fun getNextPendingDownload(): DownloadEntity?
    
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?
    
    @Query("SELECT * FROM downloads WHERE trackId = :trackId")
    suspend fun getDownloadByTrackId(trackId: String): DownloadEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)
    
    @Update
    suspend fun updateDownload(download: DownloadEntity)
    
    @Delete
    suspend fun deleteDownload(download: DownloadEntity)
    
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)
    
    @Query("DELETE FROM downloads WHERE status = 'completed'")
    suspend fun clearCompletedDownloads()
    
    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
    
    @Query("UPDATE downloads SET progress = :progress, downloadedBytes = :downloadedBytes WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, downloadedBytes: Long)
    
    @Query("UPDATE downloads SET status = 'failed', errorMessage = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String)
    
    @Query("UPDATE downloads SET status = 'completed', localPath = :localPath, completedAt = :completedAt, progress = 1.0 WHERE id = :id")
    suspend fun markCompleted(id: String, localPath: String, completedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE downloads SET status = 'cancelled' WHERE status = 'pending'")
    suspend fun cancelAllPending()
    
    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'downloading'")
    suspend fun getActiveDownloadCount(): Int
}
