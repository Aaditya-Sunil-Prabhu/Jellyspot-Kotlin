package com.jellyspot.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jellyspot.data.local.entities.PlaylistEntity
import com.jellyspot.data.local.entities.TrackEntity
import com.jellyspot.data.repository.FolderInfo
import com.jellyspot.data.repository.LocalMusicRepository
import com.jellyspot.data.repository.PlaylistRepository
import com.jellyspot.data.repository.SettingsRepository
import com.jellyspot.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the library screen.
 */
data class LibraryUiState(
    val tracks: List<TrackEntity> = emptyList(),
    val favoriteTracks: List<TrackEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val folders: List<FolderInfo> = emptyList(),
    val selectedFolders: Set<String> = emptySet(),
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val selectedTab: LibraryTab = LibraryTab.SONGS,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC
)

enum class LibraryTab {
    SONGS, ALBUMS, ARTISTS, PLAYLISTS
}

enum class SortOrder {
    NAME_ASC, NAME_DESC, ARTIST_ASC, ALBUM_ASC, RECENTLY_ADDED, MOST_PLAYED
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val localMusicRepository: LocalMusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibrary()
        playerManager.initialize()
    }

    private fun loadLibrary() {
        // Load tracks
        viewModelScope.launch {
            localMusicRepository.getFilteredTracks().collect { tracks ->
                _uiState.update { it.copy(tracks = sortTracks(tracks, it.sortOrder)) }
            }
        }

        // Load favorites
        viewModelScope.launch {
            localMusicRepository.getFavoriteTracks().collect { favorites ->
                _uiState.update { it.copy(favoriteTracks = favorites) }
            }
        }

        // Load playlists
        viewModelScope.launch {
            playlistRepository.getLocalPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }

        // Load folders and selected folders
        viewModelScope.launch {
            val folders = localMusicRepository.getAvailableFolders()
            val selected = settingsRepository.selectedFolderPaths.first()
            _uiState.update { it.copy(folders = folders, selectedFolders = selected) }
        }
    }

    /**
     * Refresh the library by scanning for new tracks.
     */
    fun refreshLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0f) }
            
            localMusicRepository.scanLibrary { current, total ->
                val progress = if (total > 0) current.toFloat() / total else 0f
                _uiState.update { it.copy(scanProgress = progress) }
            }
            
            // Reload folders after scan
            val folders = localMusicRepository.getAvailableFolders()
            _uiState.update { it.copy(isScanning = false, folders = folders) }
        }
    }

    /**
     * Select a tab.
     */
    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * Play a track (and set queue to all tracks).
     */
    fun playTrack(track: TrackEntity) {
        val tracks = _uiState.value.tracks
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerManager.playTracks(tracks, index)
    }

    /**
     * Set sort order.
     */
    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            state.copy(
                sortOrder = order,
                tracks = sortTracks(state.tracks, order)
            )
        }
    }

    /**
     * Toggle folder selection.
     */
    fun toggleFolderSelection(folderPath: String) {
        viewModelScope.launch {
            val current = _uiState.value.selectedFolders.toMutableSet()
            if (current.contains(folderPath)) {
                current.remove(folderPath)
            } else {
                current.add(folderPath)
            }
            settingsRepository.setSelectedFolderPaths(current)
            _uiState.update { it.copy(selectedFolders = current) }
        }
    }

    /**
     * Select all folders.
     */
    fun selectAllFolders() {
        viewModelScope.launch {
            settingsRepository.setSelectedFolderPaths(emptySet())
            _uiState.update { it.copy(selectedFolders = emptySet()) }
        }
    }

    /**
     * Toggle favorite for a track.
     */
    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            localMusicRepository.toggleFavorite(trackId)
        }
    }

    /**
     * Create a new playlist.
     */
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    /**
     * Delete a playlist.
     */
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    /**
     * Rename a playlist.
     */
    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, newName)
        }
    }

    /**
     * Add track to playlist.
     */
    fun addToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    /**
     * Delete a track from device.
     */
    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            localMusicRepository.deleteTrack(track)
        }
    }

    private fun sortTracks(tracks: List<TrackEntity>, order: SortOrder): List<TrackEntity> {
        return when (order) {
            SortOrder.NAME_ASC -> tracks.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> tracks.sortedByDescending { it.name.lowercase() }
            SortOrder.ARTIST_ASC -> tracks.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM_ASC -> tracks.sortedBy { it.album.lowercase() }
            SortOrder.RECENTLY_ADDED -> tracks.sortedByDescending { it.createdAt }
            SortOrder.MOST_PLAYED -> tracks.sortedByDescending { it.playCount }
        }
    }
}
