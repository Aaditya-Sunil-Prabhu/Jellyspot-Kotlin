package com.jellyspot.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToJellyfinLogin: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Dialog states
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sources Section
            item {
                SettingsSection(title = "Sources") {
                    SettingsClickableItem(
                        icon = Icons.Default.Storage,
                        title = "Source Mode",
                        subtitle = uiState.sourceMode.displayName,
                        onClick = { showSourceDialog = true }
                    )
                    if (onNavigateToJellyfinLogin != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableItem(
                            icon = Icons.Default.Cloud,
                            title = "Jellyfin Server",
                            subtitle = if (uiState.jellyfinServerUrl.isNotEmpty()) 
                                           uiState.jellyfinServerUrl 
                                       else "Not connected",
                            onClick = onNavigateToJellyfinLogin
                        )
                    }
                }
            }
            
            // Library Section (Folders moved here)
            item {
                SettingsSection(title = "Library") {
                    SettingsClickableItem(
                        icon = Icons.Default.Folder,
                        title = "Manage Folders",
                        subtitle = if (uiState.selectedFolders.isEmpty()) 
                                       "All folders (${uiState.folders.size})" 
                                   else "${uiState.selectedFolders.size} folders selected",
                        onClick = { viewModel.showFoldersDialog() }
                    )
                }
            }
            
            // Playback Section
            item {
                SettingsSection(title = "Playback") {
                    SettingsClickableItem(
                        icon = Icons.Default.HighQuality,
                        title = "Audio Quality",
                        subtitle = uiState.audioQuality.displayName,
                        onClick = { showQualityDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsClickableItem(
                        icon = Icons.Default.SwapHoriz,
                        title = "Crossfade",
                        subtitle = if (uiState.crossfadeDuration > 0) "${uiState.crossfadeDuration}s" else "Off",
                        onClick = { /* TODO: Crossfade slider dialog */ }
                    )
                }
            }
            
            // Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    SettingsClickableItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = uiState.theme.displayName,
                        onClick = { showThemeDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        title = "AMOLED Mode",
                        subtitle = "Pure black background",
                        checked = uiState.amoledMode,
                        onCheckedChange = { viewModel.setAmoledMode(it) }
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "1.0.0 (Kotlin Rewrite)"
                    )
                }
            }
        }
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        SelectionDialog(
            title = "Theme",
            options = ThemeOption.entries.map { it.displayName },
            selectedIndex = ThemeOption.entries.indexOf(uiState.theme),
            onSelect = { index ->
                viewModel.setTheme(ThemeOption.entries[index])
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // Source Mode Dialog
    if (showSourceDialog) {
        SelectionDialog(
            title = "Source Mode",
            options = SourceMode.entries.map { it.displayName },
            selectedIndex = SourceMode.entries.indexOf(uiState.sourceMode),
            onSelect = { index ->
                viewModel.setSourceMode(SourceMode.entries[index])
                showSourceDialog = false
            },
            onDismiss = { showSourceDialog = false }
        )
    }
    
    // Audio Quality Dialog
    if (showQualityDialog) {
        SelectionDialog(
            title = "Audio Quality",
            options = AudioQuality.entries.map { it.displayName },
            selectedIndex = AudioQuality.entries.indexOf(uiState.audioQuality),
            onSelect = { index ->
                viewModel.setAudioQuality(AudioQuality.entries[index])
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }
    
    // Folders Dialog
    if (uiState.showFoldersDialog) {
        FoldersDialog(
            folders = uiState.folders,
            selectedFolders = uiState.selectedFolders,
            onFolderToggle = { viewModel.toggleFolderSelection(it) },
            onSelectAll = { viewModel.selectAllFolders() },
            onDismiss = { viewModel.hideFoldersDialog() }
        )
    }
}

@Composable
private fun FoldersDialog(
    folders: List<com.jellyspot.data.repository.FolderInfo>,
    selectedFolders: Set<String>,
    onFolderToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Folders") },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        modifier = Modifier.clickable(onClick = onSelectAll),
                        headlineContent = { Text("All Folders") },
                        supportingContent = { Text("Include music from all folders") },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                        trailingContent = {
                            RadioButton(
                                selected = selectedFolders.isEmpty(),
                                onClick = onSelectAll
                            )
                        }
                    )
                    HorizontalDivider()
                }
                items(folders) { folder ->
                    val isSelected = selectedFolders.isEmpty() || selectedFolders.contains(folder.path)
                    ListItem(
                        modifier = Modifier.clickable { onFolderToggle(folder.path) },
                        headlineContent = { Text(folder.displayName) },
                        supportingContent = { Text("${folder.trackCount} songs") },
                        leadingContent = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onFolderToggle(folder.path) }
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
    )
}

@Composable
private fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    ListItem(
                        modifier = Modifier.clickable { onSelect(index) },
                        headlineContent = { Text(option) },
                        leadingContent = {
                            RadioButton(
                                selected = index == selectedIndex,
                                onClick = { onSelect(index) }
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

