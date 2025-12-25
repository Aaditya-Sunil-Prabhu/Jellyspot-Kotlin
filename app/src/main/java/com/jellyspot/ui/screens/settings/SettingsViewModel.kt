package com.jellyspot.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jellyspot.data.repository.FolderInfo
import com.jellyspot.data.repository.LocalMusicRepository
import com.jellyspot.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val sourceMode: SourceMode = SourceMode.LOCAL,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val amoledMode: Boolean = false,
    val crossfadeDuration: Int = 0,
    val jellyfinServerUrl: String = "",
    val jellyfinUsername: String = "",
    val isJellyfinConnected: Boolean = false,
    // Folder management
    val folders: List<FolderInfo> = emptyList(),
    val selectedFolders: Set<String> = emptySet(),
    val showFoldersDialog: Boolean = false
)

enum class ThemeOption(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System default")
}

enum class SourceMode(val displayName: String) {
    LOCAL("Local only"),
    JELLYFIN("Jellyfin only"),
    BOTH("Both")
}

enum class AudioQuality(val displayName: String) {
    LOW("Low (128 kbps)"),
    MEDIUM("Medium (256 kbps)"),
    HIGH("High (320 kbps)"),
    LOSSLESS("Lossless")
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localMusicRepository: LocalMusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadFolders()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.theme,
                settingsRepository.sourceMode,
                settingsRepository.audioQuality,
                settingsRepository.amoledMode
            ) { theme, sourceMode, audioQuality, amoledMode ->
                _uiState.update { state ->
                    state.copy(
                        theme = ThemeOption.entries.find { it.name == theme } ?: ThemeOption.SYSTEM,
                        sourceMode = SourceMode.entries.find { it.name == sourceMode } ?: SourceMode.LOCAL,
                        audioQuality = AudioQuality.entries.find { it.name == audioQuality } ?: AudioQuality.HIGH,
                        amoledMode = amoledMode
                    )
                }
            }.collect()
        }
        
        viewModelScope.launch {
            settingsRepository.jellyfinServerUrl.collect { url ->
                _uiState.update { it.copy(jellyfinServerUrl = url ?: "") }
            }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            val folders = localMusicRepository.getAvailableFolders()
            val selected = settingsRepository.selectedFolderPaths.first()
            _uiState.update { it.copy(folders = folders, selectedFolders = selected) }
        }
    }

    fun setTheme(theme: ThemeOption) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme.name)
            _uiState.update { it.copy(theme = theme) }
        }
    }

    fun setSourceMode(mode: SourceMode) {
        viewModelScope.launch {
            settingsRepository.setSourceMode(mode.name)
            _uiState.update { it.copy(sourceMode = mode) }
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsRepository.setAudioQuality(quality.name)
            _uiState.update { it.copy(audioQuality = quality) }
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAmoledMode(enabled)
            _uiState.update { it.copy(amoledMode = enabled) }
        }
    }

    fun setCrossfadeDuration(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setCrossfadeDuration(seconds)
            _uiState.update { it.copy(crossfadeDuration = seconds) }
        }
    }

    fun showFoldersDialog() {
        _uiState.update { it.copy(showFoldersDialog = true) }
    }

    fun hideFoldersDialog() {
        _uiState.update { it.copy(showFoldersDialog = false) }
    }

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

    fun selectAllFolders() {
        viewModelScope.launch {
            settingsRepository.setSelectedFolderPaths(emptySet())
            _uiState.update { it.copy(selectedFolders = emptySet()) }
        }
    }
}

