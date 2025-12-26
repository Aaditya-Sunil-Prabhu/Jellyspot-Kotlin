package com.jellyspot.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.jellyspot.MainActivity
import com.jellyspot.R
import com.jellyspot.data.repository.LocalMusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Media playback foreground service using Media3.
 * Handles background playback and media session.
 */
@UnstableApi
@AndroidEntryPoint
class JellyspotMediaService : MediaSessionService() {

    @Inject
    lateinit var repository: LocalMusicRepository

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    
    // Service scope for coroutines
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Custom Commands
    companion object {
        const val COMMAND_LIKE_ACTION = "com.jellyspot.COMMAND_LIKE"
        const val COMMAND_TOGGLE_REPEAT = "com.jellyspot.COMMAND_TOGGLE_REPEAT"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Create ExoPlayer instance
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // Handle audio focus
            )
            .setHandleAudioBecomingNoisy(true) // Pause when headphones disconnected
            .build()
        
        player?.addListener(playerListener)

        // Create PendingIntent for notification click
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create MediaSession
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(MediaSessionCallback())
            .setSessionActivity(pendingIntent)
            .build()
            
        // Set Custom Notification Provider
        setMediaNotificationProvider(CustomNotificationProvider())
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            // Stop service if player is not playing
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player?.removeListener(playerListener)
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }
    
    private fun updateCustomLayout(isLiked: Boolean) {
        val likeIcon = if (isLiked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        val likeButton = CommandButton.Builder()
            .setDisplayName("Like")
            .setIconResId(likeIcon)
            .setSessionCommand(SessionCommand(COMMAND_LIKE_ACTION, Bundle.EMPTY))
            .build()
            
        mediaSession?.setCustomLayout(ImmutableList.of(likeButton))
    }
    
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Update Like button state when track changes
            mediaItem?.mediaId?.let { trackId ->
                serviceScope.launch {
                    val track = repository.getTrackById(trackId)
                    updateCustomLayout(track?.isFavorite == true)
                }
            } ?: updateCustomLayout(false)
        }
    }

    /**
     * Callback for handling media session requests.
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val likeCmd = SessionCommand(COMMAND_LIKE_ACTION, Bundle.EMPTY)
            val repeatCmd = SessionCommand(COMMAND_TOGGLE_REPEAT, Bundle.EMPTY)
            
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(likeCmd)
                .add(repeatCmd)
                .build()
                
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            // Resolve URIs for media items
            val resolvedItems = mediaItems.map { item ->
                item.buildUpon()
                    .setUri(item.requestMetadata.mediaUri ?: item.localConfiguration?.uri)
                    .build()
            }.toMutableList()
            return Futures.immediateFuture(resolvedItems)
        }
        
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                COMMAND_LIKE_ACTION -> {
                    val currentMediaId = player?.currentMediaItem?.mediaId
                    if (currentMediaId != null) {
                        serviceScope.launch {
                            repository.toggleFavorite(currentMediaId)
                            val track = repository.getTrackById(currentMediaId)
                            updateCustomLayout(track?.isFavorite == true)
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                COMMAND_TOGGLE_REPEAT -> {
                    player?.let {
                        val newMode = when (it.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        it.repeatMode = newMode
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
    
    /**
     * Custom Notification Provider to include Like and Repeat buttons.
     */
    private inner class CustomNotificationProvider : DefaultMediaNotificationProvider(this) {
        override fun getMediaButtons(
            session: MediaSession,
            playerCommands: Player.Commands,
            customLayout: ImmutableList<CommandButton>,
            showPauseButton: Boolean
        ): ImmutableList<CommandButton> {
            val builder = ImmutableList.builder<CommandButton>()
            
            // 1. Like (Custom)
            if (customLayout.isNotEmpty()) {
                builder.add(customLayout[0])
            }
            
            // 2. Previous
            if (playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS)) {
                builder.add(
                    CommandButton.Builder()
                        .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .setIconResId(R.drawable.ic_skip_previous)
                        .setDisplayName("Previous")
                        .build()
                )
            }
            
            // 3. Play/Pause
            if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
                if (showPauseButton) {
                     builder.add(
                        CommandButton.Builder()
                            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                            .setIconResId(R.drawable.ic_pause)
                            .setDisplayName("Pause")
                            .build()
                    )
                } else {
                    builder.add(
                        CommandButton.Builder()
                            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                            .setIconResId(R.drawable.ic_play_arrow)
                            .setDisplayName("Play")
                            .build()
                    )
                }
            }
            
            // 4. Next
            if (playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT)) {
                builder.add(
                    CommandButton.Builder()
                        .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                        .setIconResId(R.drawable.ic_skip_next)
                        .setDisplayName("Next")
                        .build()
                )
            }
            
            // 5. Repeat (Custom Toggle)
            val repeatMode = player?.repeatMode ?: Player.REPEAT_MODE_OFF
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                else -> R.drawable.ic_repeat
            }
            
            builder.add(
                CommandButton.Builder()
                    .setDisplayName("Repeat")
                    .setIconResId(repeatIcon)
                    .setSessionCommand(SessionCommand(COMMAND_TOGGLE_REPEAT, Bundle.EMPTY))
                    .build()
            )
            
            return builder.build()
        }
    }
}
