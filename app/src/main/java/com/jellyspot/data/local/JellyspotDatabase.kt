package com.jellyspot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jellyspot.data.local.dao.DownloadDao
import com.jellyspot.data.local.dao.PlaylistDao
import com.jellyspot.data.local.dao.TrackDao
import com.jellyspot.data.local.entities.*

/**
 * Room database for Jellyspot.
 */
@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        DownloadEntity::class,
        AlbumEntity::class,
        ArtistEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JellyspotDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao
}
