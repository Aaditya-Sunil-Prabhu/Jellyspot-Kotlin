package com.jellyspot.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jellyspot.data.local.entities.TrackEntity
import com.jellyspot.data.repository.JellyfinRepository
import com.jellyspot.data.repository.LocalMusicRepository
import com.jellyspot.data.repository.SettingsRepository
import com.jellyspot.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home section data.
 */
data class HomeSection(
    val title: String,
    val tracks: List<TrackEntity>,
    val type: SectionType = SectionType.HORIZONTAL
)

enum class SectionType {
    HORIZONTAL, GRID, LARGE
}

/**
 * UI state for home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val sections: List<HomeSection> = emptyList(),
    val sourceMode: String = "local",
    val dataSource: String = "local",
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val jellyfinRepository: JellyfinRepository,
    private val localMusicRepository: LocalMusicRepository,
    private val settingsRepository: SettingsRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Initialize player
        playerManager.initialize()
        
        // Watch source mode changes
        viewModelScope.launch {
            combine(
                settingsRepository.sourceMode,
                settingsRepository.dataSource
            ) { sourceMode, dataSource ->
                _uiState.update { it.copy(sourceMode = sourceMode, dataSource = dataSource) }
                loadHomeFeed(sourceMode, dataSource)
            }.collect()
        }
    }

    private suspend fun loadHomeFeed(sourceMode: String, dataSource: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        val sections = mutableListOf<HomeSection>()
        
        try {
            when (dataSource) {
                "jellyfin" -> {
                    if (jellyfinRepository.initializeFromStorage()) {
                        // Recently Added
                        val latest = jellyfinRepository.getLatestMusic(20)
                        if (latest.isNotEmpty()) {
                            sections.add(HomeSection("Recently Added", latest, SectionType.HORIZONTAL))
                        }
                        
                        // All Music
                        val allMusic = jellyfinRepository.getAllMusic(30)
                        if (allMusic.isNotEmpty()) {
                            sections.add(HomeSection("Your Music", allMusic, SectionType.GRID))
                        }
                    } else {
                        _uiState.update { it.copy(errorMessage = "Not logged in to Jellyfin") }
                    }
                }
                
                "local" -> {
                    val tracks = localMusicRepository.getFilteredTracks().first()
                    
                    if (tracks.isEmpty()) {
                        // No tracks - prompt scan
                        sections.add(HomeSection("Getting Started", emptyList()))
                    } else {
                        // Recently Played
                        val recentlyPlayed = tracks.filter { it.lastPlayedAt != null }
                            .sortedByDescending { it.lastPlayedAt }
                            .take(20)
                        if (recentlyPlayed.isNotEmpty()) {
                            sections.add(HomeSection("Recently Played", recentlyPlayed, SectionType.HORIZONTAL))
                        }
                        
                        // Most Played
                        val mostPlayed = tracks.filter { it.playCount > 0 }
                            .sortedByDescending { it.playCount }
                            .take(20)
                        if (mostPlayed.isNotEmpty()) {
                            sections.add(HomeSection("Most Played", mostPlayed, SectionType.HORIZONTAL))
                        }
                        
                        // Favorites
                        val favorites = tracks.filter { it.isFavorite }.take(20)
                        if (favorites.isNotEmpty()) {
                            sections.add(HomeSection("Favorites", favorites, SectionType.HORIZONTAL))
                        }
                        
                        // All Songs (shuffled sample)
                        val shuffled = tracks.shuffled().take(30)
                        sections.add(HomeSection("Shuffle All", shuffled, SectionType.GRID))
                    }
                }
            }
            
            _uiState.update { it.copy(isLoading = false, sections = sections) }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load") 
            }
        }
    }

    /**
     * Play a track and set queue.
     */
    fun playTrack(track: TrackEntity, sectionTracks: List<TrackEntity>) {
        val index = sectionTracks.indexOfFirst { it.id == track.id }
        playerManager.playTracks(sectionTracks, if (index >= 0) index else 0)
    }

    /**
     * Toggle favorite.
     */
    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            localMusicRepository.toggleFavorite(trackId)
        }
    }

    /**
     * Refresh home feed.
     */
    fun refresh() {
        viewModelScope.launch {
            loadHomeFeed(_uiState.value.sourceMode, _uiState.value.dataSource)
        }
    }

    /**
     * Scan local library.
     */
    fun scanLocalLibrary() {
        viewModelScope.launch {
            localMusicRepository.scanLibrary()
            refresh()
        }
    }

    /**
     * Switch data source.
     */
    fun setDataSource(source: String) {
        viewModelScope.launch {
            settingsRepository.setDataSource(source)
        }
    }
}
