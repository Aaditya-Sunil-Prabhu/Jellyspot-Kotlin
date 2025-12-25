package com.jellyspot.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.jellyspot.data.local.JellyspotDatabase
import com.jellyspot.data.local.dao.TrackDao
import com.jellyspot.data.local.dao.PlaylistDao
import com.jellyspot.data.local.dao.DownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// DataStore extension
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jellyspot_settings")

/**
 * Hilt module providing app-wide singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JellyspotDatabase {
        return Room.databaseBuilder(
            context,
            JellyspotDatabase::class.java,
            "jellyspot.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTrackDao(database: JellyspotDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    fun providePlaylistDao(database: JellyspotDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideDownloadDao(database: JellyspotDatabase): DownloadDao {
        return database.downloadDao()
    }
}
