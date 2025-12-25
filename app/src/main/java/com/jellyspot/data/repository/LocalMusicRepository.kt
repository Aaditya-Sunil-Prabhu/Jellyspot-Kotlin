package com.jellyspot.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.jellyspot.data.local.dao.TrackDao
import com.jellyspot.data.local.entities.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for local music operations using MediaStore.
 */
@Singleton
class LocalMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val settingsRepository: SettingsRepository
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Collection URI for audio files.
     */
    private val audioCollection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    /**
     * Projection for audio queries.
     */
    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA, // File path
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.DATE_ADDED,
    )

    /**
     * Get all local tracks from database.
     */
    fun getAllTracks(): Flow<List<TrackEntity>> = trackDao.getLocalTracks()

    /**
     * Get favorite tracks.
     */
    fun getFavoriteTracks(): Flow<List<TrackEntity>> = trackDao.getFavoriteTracks()

    /**
     * Get tracks filtered by selected folders.
     */
    fun getFilteredTracks(): Flow<List<TrackEntity>> {
        return trackDao.getLocalTracks().map { tracks ->
            val selectedPaths = settingsRepository.selectedFolderPaths.first()
            if (selectedPaths.isEmpty()) {
                tracks // No filter = all tracks
            } else {
                tracks.filter { track ->
                    val trackFolder = getParentPath(track.localPath ?: "")
                    selectedPaths.any { selectedPath ->
                        trackFolder.startsWith(selectedPath)
                    }
                }
            }
        }
    }

    /**
     * Get unique folder paths from all tracks.
     */
    suspend fun getAvailableFolders(): List<FolderInfo> = withContext(Dispatchers.IO) {
        val tracks = trackDao.getLocalTracks().first()
        val folderMap = mutableMapOf<String, Int>()
        
        tracks.forEach { track ->
            val folderPath = getParentPath(track.localPath ?: "")
            if (folderPath.isNotEmpty()) {
                folderMap[folderPath] = (folderMap[folderPath] ?: 0) + 1
            }
        }
        
        folderMap.map { (path, count) ->
            FolderInfo(
                path = path,
                displayName = path.substringAfterLast("/"),
                trackCount = count
            )
        }.sortedBy { it.displayName.lowercase() }
    }

    /**
     * Scan device for audio files and update database.
     */
    suspend fun scanLibrary(
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): ScanResult = withContext(Dispatchers.IO) {
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        val newTracks = mutableListOf<TrackEntity>()
        var scannedCount = 0
        var totalCount = 0
        
        // First, get total count
        contentResolver.query(audioCollection, arrayOf(MediaStore.Audio.Media._ID), selection, null, null)?.use { cursor ->
            totalCount = cursor.count
        }
        
        contentResolver.query(
            audioCollection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val albumId = cursor.getLong(albumIdColumn)
                val artistId = cursor.getLong(artistIdColumn)
                val duration = cursor.getLong(durationColumn)
                val filePath = cursor.getString(dataColumn) ?: ""
                val fileSize = cursor.getLong(sizeColumn)
                val trackNumber = cursor.getInt(trackColumn)
                val mimeType = cursor.getString(mimeColumn) ?: ""
                
                // Build content URI for playback
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                // Build album art URI
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                // Extract codec from mime type
                val codec = when {
                    mimeType.contains("flac") -> "FLAC"
                    mimeType.contains("mp3") || mimeType.contains("mpeg") -> "MP3"
                    mimeType.contains("aac") || mimeType.contains("m4a") -> "AAC"
                    mimeType.contains("ogg") || mimeType.contains("vorbis") -> "OGG"
                    mimeType.contains("opus") -> "OPUS"
                    mimeType.contains("wav") -> "WAV"
                    else -> mimeType.substringAfter("/").uppercase()
                }
                
                val track = TrackEntity(
                    id = "local_$id",
                    name = title,
                    artist = artist,
                    album = album,
                    albumId = "local_album_$albumId",
                    artistId = "local_artist_$artistId",
                    durationMillis = duration,
                    streamUrl = contentUri.toString(),
                    localPath = filePath,
                    imageUrl = albumArtUri.toString(),
                    source = "local",
                    codec = codec,
                    container = filePath.substringAfterLast(".").uppercase(),
                    fileSize = fileSize,
                    trackNumber = trackNumber,
                    metadataEnriched = false
                )
                
                newTracks.add(track)
                scannedCount++
                
                // Report progress every 50 tracks
                if (scannedCount % 50 == 0) {
                    onProgress(scannedCount, totalCount)
                }
            }
        }
        
        // Update database
        val existingTracks = trackDao.getLocalTracks().first()
        val existingIds = existingTracks.map { it.id }.toSet()
        val newIds = newTracks.map { it.id }.toSet()
        
        // Preserve favorites when updating
        val favoritesMap = existingTracks.filter { it.isFavorite }.associate { it.id to true }
        
        val tracksToInsert = newTracks.map { track ->
            track.copy(isFavorite = favoritesMap[track.id] == true)
        }
        
        // Insert all scanned tracks
        trackDao.insertTracks(tracksToInsert)
        
        // Remove tracks that no longer exist
        val removedIds = existingIds - newIds
        removedIds.forEach { id ->
            trackDao.deleteTrackById(id)
        }
        
        onProgress(totalCount, totalCount)
        
        ScanResult(
            totalTracks = newTracks.size,
            newTracks = (newIds - existingIds).size,
            removedTracks = removedIds.size
        )
    }

    /**
     * Toggle favorite status for a track.
     */
    suspend fun toggleFavorite(trackId: String) {
        val track = trackDao.getTrackById(trackId)
        track?.let {
            trackDao.setFavorite(trackId, !it.isFavorite)
        }
    }

    /**
     * Search tracks by name, artist, or album.
     */
    suspend fun searchTracks(query: String): List<TrackEntity> {
        return trackDao.searchTracks(query)
    }

    /**
     * Delete a track from device storage.
     */
    suspend fun deleteTrack(track: TrackEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // For Q+, need MediaStore delete
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val id = track.id.removePrefix("local_").toLongOrNull() ?: return@withContext false
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val deleted = contentResolver.delete(uri, null, null) > 0
                if (deleted) {
                    trackDao.deleteTrackById(track.id)
                }
                deleted
            } else {
                // For older versions, delete file directly
                val file = java.io.File(track.localPath ?: return@withContext false)
                val deleted = file.delete()
                if (deleted) {
                    trackDao.deleteTrackById(track.id)
                }
                deleted
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get track count.
     */
    suspend fun getTrackCount(): Int = trackDao.getLocalTrackCount()

    private fun getParentPath(path: String): String {
        return path.substringBeforeLast("/")
    }
}

/**
 * Folder information for folder selection.
 */
data class FolderInfo(
    val path: String,
    val displayName: String,
    val trackCount: Int
)

/**
 * Result of a library scan.
 */
data class ScanResult(
    val totalTracks: Int,
    val newTracks: Int,
    val removedTracks: Int
)
