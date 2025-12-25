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
 * Artist data for home screen.
 */
data class ArtistItem(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val trackCount: Int = 0
)

/**
 * Home section data.
 */
data class HomeSection(
    val title: String,
    val tracks: List<TrackEntity> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val type: SectionType = SectionType.HORIZONTAL
)

enum class SectionType {
    HORIZONTAL,  // Scrollable row of track cards
    GRID,        // 2-row grid of track cards
    LARGE,       // Large cards with play button
    ARTISTS      // Circle avatars for artists
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
                        val latest = jellyfinRepository.getLatestMusic(20)
                        if (latest.isNotEmpty()) {
                            sections.add(HomeSection("Recently Added", latest, type = SectionType.HORIZONTAL))
                        }
                        val allMusic = jellyfinRepository.getAllMusic(30)
                        if (allMusic.isNotEmpty()) {
                            sections.add(HomeSection("Your Music", allMusic, type = SectionType.GRID))
                        }
                    } else {
                        _uiState.update { it.copy(errorMessage = "Not logged in to Jellyfin") }
                    }
                }
                
                "local" -> {
                    val tracks = localMusicRepository.getFilteredTracks().first()
                    
                    if (tracks.isEmpty()) {
                        // No tracks - show empty state
                        _uiState.update { it.copy(isLoading = false, sections = emptyList()) }
                        return
                    }
                    
                    // 1. ARTISTS SECTION (Circle avatars at top)
                    val artistMap = mutableMapOf<String, MutableList<TrackEntity>>()
                    tracks.forEach { track ->
                        val artistName = track.artist.ifBlank { "Unknown Artist" }
                        artistMap.getOrPut(artistName) { mutableListOf() }.add(track)
                    }
                    
                    val artists = artistMap.map { (name, trackList) ->
                        ArtistItem(
                            id = "artist_${name.lowercase().replace(" ", "_")}",
                            name = name,
                            imageUrl = trackList.firstOrNull()?.imageUrl,
                            trackCount = trackList.size
                        )
                    }.sortedByDescending { it.trackCount }.take(12)
                    
                    if (artists.isNotEmpty()) {
                        sections.add(HomeSection(
                            title = "Artists",
                            artists = artists,
                            type = SectionType.ARTISTS
                        ))
                    }
                    
                    // 2. QUICK PICKS (12 random + recently played songs)
                    val recentlyPlayed = tracks.filter { it.lastPlayedAt != null }
                        .sortedByDescending { it.lastPlayedAt }
                        .take(6)
                    val randomPicks = tracks.filter { it.lastPlayedAt == null }
                        .shuffled()
                        .take(12 - recentlyPlayed.size)
                    val quickPicks = (recentlyPlayed + randomPicks).take(12)
                    
                    if (quickPicks.isNotEmpty()) {
                        sections.add(HomeSection(
                            title = "Quick Picks",
                            tracks = quickPicks,
                            type = SectionType.HORIZONTAL
                        ))
                    }
                    
                    // 3. RECENTLY ADDED (sorted by creation date)
                    val recentlyAdded = tracks.sortedByDescending { it.createdAt }.take(12)
                    if (recentlyAdded.isNotEmpty()) {
                        sections.add(HomeSection(
                            title = "Recently Added",
                            tracks = recentlyAdded,
                            type = SectionType.HORIZONTAL
                        ))
                    }
                    
                    // 4. MOST PLAYED
                    val mostPlayed = tracks.filter { it.playCount > 0 }
                        .sortedByDescending { it.playCount }
                        .take(12)
                    if (mostPlayed.isNotEmpty()) {
                        sections.add(HomeSection(
                            title = "Most Played",
                            tracks = mostPlayed,
                            type = SectionType.HORIZONTAL
                        ))
                    }
                    
                    // 5. FAVORITES
                    val favorites = tracks.filter { it.isFavorite }.take(12)
                    if (favorites.isNotEmpty()) {
                        sections.add(HomeSection(
                            title = "Favorites",
                            tracks = favorites,
                            type = SectionType.LARGE
                        ))
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
