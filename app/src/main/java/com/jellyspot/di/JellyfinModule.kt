package com.jellyspot.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.android.androidDevice
import org.jellyfin.sdk.createJellyfin
import javax.inject.Singleton

/**
 * Hilt module for Jellyfin SDK.
 */
@Module
@InstallIn(SingletonComponent::class)
object JellyfinModule {

    @Provides
    @Singleton
    fun provideJellyfin(@ApplicationContext context: Context): Jellyfin {
        return createJellyfin {
            clientInfo = org.jellyfin.sdk.model.ClientInfo(
                name = "Jellyspot",
                version = "1.0.0"
            )
            deviceInfo = androidDevice(context)
        }
    }
}
