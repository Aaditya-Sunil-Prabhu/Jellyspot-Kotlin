package com.jellyspot.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Track entity for Room database.
 * Represents a music track from any source (local, Jellyfin, YouTube).
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val albumId: String? = null,
    val artistId: String? = null,
    val genre: String? = null,
    val imageUrl: String? = null,
    val imageBlurHash: String? = null,
    val durationMillis: Long,
    val streamUrl: String,
    val localPath: String? = null,
    val source: String, // "local", "jellyfin", "youtube"
    val isFavorite: Boolean = false,
    val bitrate: Int? = null,
    val codec: String? = null,
    val container: String? = null,
    val sampleRate: Int? = null,
    val fileSize: Long? = null,
    val trackNumber: Int? = null,
    val lyrics: String? = null,
    val metadataEnriched: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0
)

/**
 * Playlist entity.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val source: String = "local", // "local", "jellyfin"
    val jellyfinId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Join table for playlist-track relationship.
 */
@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Download entity for tracking download queue.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val trackId: String,
    val name: String,
    val artist: String,
    val album: String? = null,
    val imageUrl: String? = null,
    val sourceUrl: String,
    val localPath: String? = null,
    val status: String, // "pending", "downloading", "completed", "failed", "cancelled"
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val errorMessage: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Album entity for cached album data.
 */
@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String? = null,
    val imageUrl: String? = null,
    val year: Int? = null,
    val trackCount: Int = 0,
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Artist entity for cached artist data.
 */
@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val bio: String? = null,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)
