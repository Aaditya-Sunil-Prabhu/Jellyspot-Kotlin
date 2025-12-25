package com.jellyspot.ui.screens.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.jellyspot.data.local.entities.TrackEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    
    // Determine which permission to request based on Android version
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.refreshLibrary()
        }
    }
    
    // Check permission on launch
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, audioPermission
        ) == PermissionChecker.PERMISSION_GRANTED
        
        if (hasPermission) {
            viewModel.refreshLibrary()
        } else {
            permissionLauncher.launch(audioPermission)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    // Sort button
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.displayName()) },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (uiState.sortOrder == order) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Refresh button
                    IconButton(onClick = { 
                        if (hasPermission) {
                            viewModel.refreshLibrary()
                        } else {
                            permissionLauncher.launch(audioPermission)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == LibraryTab.PLAYLISTS) {
                FloatingActionButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create playlist")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission denied state
            if (!hasPermission) {
                PermissionDeniedState(
                    onRequestPermission = { permissionLauncher.launch(audioPermission) }
                )
            } else {
                // Scanning progress
                AnimatedVisibility(visible = uiState.isScanning) {
                    LinearProgressIndicator(
                        progress = { uiState.scanProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Tab row
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    edgePadding = 16.dp
                ) {
                    LibraryTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = { Text(tab.displayName()) }
                        )
                    }
                }

                // Content based on selected tab
                when (uiState.selectedTab) {
                    LibraryTab.SONGS -> TracksList(
                        tracks = uiState.tracks,
                        onTrackClick = { track ->
                            viewModel.playTrack(track)
                            onNavigateToPlayer()
                        },
                        onFavoriteClick = { viewModel.toggleFavorite(it.id) },
                        isFavorite = { track -> uiState.favoriteTracks.any { it.id == track.id } }
                    )
                    
                    LibraryTab.ALBUMS -> AlbumsList(uiState.tracks, onNavigateToDetail)
                    
                    LibraryTab.ARTISTS -> ArtistsList(uiState.tracks, onNavigateToDetail)
                    
                    LibraryTab.PLAYLISTS -> PlaylistsList(
                        playlists = uiState.playlists,
                        onPlaylistClick = { onNavigateToDetail("playlist", it.id) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it.id) }
                    )
                    
                    LibraryTab.FOLDERS -> FoldersList(
                        folders = uiState.folders,
                        selectedFolders = uiState.selectedFolders,
                        onFolderToggle = { viewModel.toggleFolderSelection(it) },
                        onSelectAll = { viewModel.selectAllFolders() }
                    )
                }
            }
        }
    }

    // Create playlist dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
private fun PermissionDeniedState(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MusicOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Jellyspot needs access to your music files to display your library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Permission")
        }
    }
}

@Composable
private fun TracksList(
    tracks: List<TrackEntity>,
    onTrackClick: (TrackEntity) -> Unit,
    onFavoriteClick: (TrackEntity) -> Unit,
    isFavorite: (TrackEntity) -> Boolean
) {
    if (tracks.isEmpty()) {
        EmptyState(
            icon = Icons.Default.MusicNote,
            title = "No songs found",
            subtitle = "Tap refresh to scan your device for music"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                TrackItem(
                    track = track,
                    onClick = { onTrackClick(track) },
                    onFavoriteClick = { onFavoriteClick(track) },
                    isFavorite = isFavorite(track)
                )
            }
        }
    }
}

@Composable
private fun TrackItem(
    track: TrackEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(track.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                "${track.artist} • ${track.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun AlbumsList(tracks: List<TrackEntity>, onNavigateToDetail: (String, String) -> Unit) {
    val albums = tracks.groupBy { it.albumId to it.album }.map { (key, albumTracks) ->
        Triple(key.first ?: "", key.second, albumTracks)
    }.distinctBy { it.first }

    if (albums.isEmpty()) {
        EmptyState(Icons.Default.Album, "No albums", "Albums from your music will appear here")
    } else {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(albums) { (albumId, albumName, albumTracks) ->
                ListItem(
                    modifier = Modifier.clickable { onNavigateToDetail("album", albumId) },
                    headlineContent = { Text(albumName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("${albumTracks.size} songs • ${albumTracks.firstOrNull()?.artist ?: ""}") },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Album, contentDescription = null)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ArtistsList(tracks: List<TrackEntity>, onNavigateToDetail: (String, String) -> Unit) {
    val artists = tracks.groupBy { it.artistId to it.artist }.map { (key, artistTracks) ->
        Triple(key.first ?: "", key.second, artistTracks)
    }.distinctBy { it.first }

    if (artists.isEmpty()) {
        EmptyState(Icons.Default.Person, "No artists", "Artists from your music will appear here")
    } else {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(artists) { (artistId, artistName, artistTracks) ->
                ListItem(
                    modifier = Modifier.clickable { onNavigateToDetail("artist", artistId) },
                    headlineContent = { Text(artistName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("${artistTracks.size} songs") },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(artistName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsList(
    playlists: List<com.jellyspot.data.local.entities.PlaylistEntity>,
    onPlaylistClick: (com.jellyspot.data.local.entities.PlaylistEntity) -> Unit,
    onDeletePlaylist: (com.jellyspot.data.local.entities.PlaylistEntity) -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyState(Icons.Default.PlaylistPlay, "No playlists", "Create a playlist to organize your music")
    } else {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(playlists) { playlist ->
                ListItem(
                    modifier = Modifier.clickable { onPlaylistClick(playlist) },
                    headlineContent = { Text(playlist.name) },
                    supportingContent = { Text(playlist.description ?: "Playlist") },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null)
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { onDeletePlaylist(playlist) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FoldersList(
    folders: List<com.jellyspot.data.repository.FolderInfo>,
    selectedFolders: Set<String>,
    onFolderToggle: (String) -> Unit,
    onSelectAll: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            ListItem(
                modifier = Modifier.clickable(onClick = onSelectAll),
                headlineContent = { Text("All Folders") },
                supportingContent = { Text("Show music from all folders") },
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
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun LibraryTab.displayName() = when (this) {
    LibraryTab.SONGS -> "Songs"
    LibraryTab.ALBUMS -> "Albums"
    LibraryTab.ARTISTS -> "Artists"
    LibraryTab.PLAYLISTS -> "Playlists"
    LibraryTab.FOLDERS -> "Folders"
}

private fun SortOrder.displayName() = when (this) {
    SortOrder.NAME_ASC -> "Name (A-Z)"
    SortOrder.NAME_DESC -> "Name (Z-A)"
    SortOrder.ARTIST_ASC -> "Artist"
    SortOrder.ALBUM_ASC -> "Album"
    SortOrder.RECENTLY_ADDED -> "Recently Added"
    SortOrder.MOST_PLAYED -> "Most Played"
}
