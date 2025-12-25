package com.jellyspot.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.jellyspot.data.local.entities.TrackEntity
import com.jellyspot.data.repository.LocalMusicRepository
import com.jellyspot.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the player screen.
 */
data class PlayerUiState(
    val currentTrack: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<TrackEntity> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isFavorite: Boolean = false,
    val showQueue: Boolean = false,
    val showLyrics: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val localMusicRepository: LocalMusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        // Collect player state
        viewModelScope.launch {
            combine(
                playerManager.currentTrack,
                playerManager.isPlaying,
                playerManager.durationMs,
                playerManager.queue,
                playerManager.shuffleEnabled,
                playerManager.repeatMode
            ) { values ->
                val track = values[0] as? TrackEntity
                val isPlaying = values[1] as Boolean
                val duration = values[2] as Long
                val queue = values[3] as List<*>
                val shuffle = values[4] as Boolean
                val repeat = values[5] as Int
                
                _uiState.update { state ->
                    state.copy(
                        currentTrack = track,
                        isPlaying = isPlaying,
                        durationMs = duration,
                        queue = queue.filterIsInstance<TrackEntity>(),
                        shuffleEnabled = shuffle,
                        repeatMode = repeat
                    )
                }
            }.collect()
        }

        // Update position periodically
        viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(positionMs = playerManager.getCurrentPosition()) }
                delay(500)
            }
        }

        // Check favorite status when track changes
        viewModelScope.launch {
            playerManager.currentTrack.collect { track ->
                if (track != null) {
                    val trackFromDb = localMusicRepository.getAllTracks().first()
                        .find { it.id == track.id }
                    _uiState.update { it.copy(isFavorite = trackFromDb?.isFavorite == true) }
                }
            }
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    
    fun skipNext() = playerManager.skipNext()
    
    fun skipPrevious() = playerManager.skipPrevious()
    
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    
    fun toggleShuffle() = playerManager.toggleShuffle()
    
    fun cycleRepeatMode() = playerManager.cycleRepeatMode()

    fun toggleFavorite() {
        viewModelScope.launch {
            _uiState.value.currentTrack?.let { track ->
                localMusicRepository.toggleFavorite(track.id)
                _uiState.update { it.copy(isFavorite = !it.isFavorite) }
            }
        }
    }

    fun toggleQueue() {
        _uiState.update { it.copy(showQueue = !it.showQueue) }
    }

    fun toggleLyrics() {
        _uiState.update { it.copy(showLyrics = !it.showLyrics) }
    }

    fun removeFromQueue(index: Int) {
        playerManager.removeFromQueue(index)
    }

    fun playFromQueue(index: Int) {
        val queue = _uiState.value.queue
        if (index < queue.size) {
            playerManager.playTracks(queue, index)
        }
    }
}
