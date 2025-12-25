package com.jellyspot.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.jellyspot.data.local.entities.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages media playback state and MediaController connection.
 */
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaController: MediaController? = null

    // Playback state
    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val queue: StateFlow<List<TrackEntity>> = _queue.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private var trackMap = mutableMapOf<String, TrackEntity>()

    /**
     * Initialize the MediaController connection.
     */
    fun initialize() {
        scope.launch {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, JellyspotMediaService::class.java)
            )
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            
            controllerFuture.addListener({
                try {
                    mediaController = controllerFuture.get()
                    mediaController?.addListener(playerListener)
                } catch (e: Exception) {
                    // Handle error
                }
            }, MoreExecutors.directExecutor())
        }
    }

    /**
     * Play a single track.
     */
    fun playTrack(track: TrackEntity) {
        playTracks(listOf(track), 0)
    }

    /**
     * Play a list of tracks starting from specified index.
     */
    fun playTracks(tracks: List<TrackEntity>, startIndex: Int = 0) {
        scope.launch {
            val controller = mediaController ?: return@launch
            
            // Update track map for metadata lookup
            trackMap.clear()
            tracks.forEach { trackMap[it.id] = it }
            
            // Convert to MediaItems
            val mediaItems = tracks.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(track.streamUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.name)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .setArtworkUri(track.imageUrl?.let { android.net.Uri.parse(it) })
                            .build()
                    )
                    .build()
            }
            
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
            
            _queue.value = tracks
        }
    }

    /**
     * Add track to play next.
     */
    fun addToQueueNext(track: TrackEntity) {
        scope.launch {
            val controller = mediaController ?: return@launch
            val currentIndex = controller.currentMediaItemIndex
            
            trackMap[track.id] = track
            
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.name)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
            
            controller.addMediaItem(currentIndex + 1, mediaItem)
            
            val updatedQueue = _queue.value.toMutableList()
            updatedQueue.add(currentIndex + 1, track)
            _queue.value = updatedQueue
        }
    }

    /**
     * Add track to end of queue.
     */
    fun addToQueueEnd(track: TrackEntity) {
        scope.launch {
            val controller = mediaController ?: return@launch
            
            trackMap[track.id] = track
            
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.name)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
            
            controller.addMediaItem(mediaItem)
            
            val updatedQueue = _queue.value.toMutableList()
            updatedQueue.add(track)
            _queue.value = updatedQueue
        }
    }

    /**
     * Toggle play/pause.
     */
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    /**
     * Skip to next track.
     */
    fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    /**
     * Skip to previous track.
     */
    fun skipPrevious() {
        mediaController?.let { controller ->
            if (controller.currentPosition > 3000) {
                controller.seekTo(0)
            } else {
                controller.seekToPreviousMediaItem()
            }
        }
    }

    /**
     * Seek to position.
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    /**
     * Toggle shuffle mode.
     */
    fun toggleShuffle() {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
            _shuffleEnabled.value = controller.shuffleModeEnabled
        }
    }

    /**
     * Cycle repeat mode.
     */
    fun cycleRepeatMode() {
        mediaController?.let { controller ->
            val newMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = newMode
            _repeatMode.value = newMode
        }
    }

    /**
     * Remove track from queue.
     */
    fun removeFromQueue(index: Int) {
        scope.launch {
            val controller = mediaController ?: return@launch
            controller.removeMediaItem(index)
            
            val updatedQueue = _queue.value.toMutableList()
            if (index < updatedQueue.size) {
                updatedQueue.removeAt(index)
                _queue.value = updatedQueue
            }
        }
    }

    /**
     * Clear the queue.
     */
    fun clearQueue() {
        mediaController?.clearMediaItems()
        _queue.value = emptyList()
        _currentTrack.value = null
    }

    /**
     * Get current playback position.
     */
    fun getCurrentPosition(): Long = mediaController?.currentPosition ?: 0L

    /**
     * Release resources.
     */
    fun release() {
        mediaController?.release()
        mediaController = null
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.let { mediaId ->
                _currentTrack.value = trackMap[mediaId]
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            mediaController?.let { controller ->
                _durationMs.value = controller.duration.coerceAtLeast(0)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _positionMs.value = newPosition.positionMs
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }
    }
}
