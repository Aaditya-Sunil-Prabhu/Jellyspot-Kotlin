package com.jellyspot.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.jellyspot.data.local.entities.TrackEntity
import com.jellyspot.ui.components.EqualizerIndicator
import com.jellyspot.ui.components.TracksList
import com.jellyspot.ui.screens.library.LibraryViewModel
import com.jellyspot.ui.screens.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    type: String, // album, artist, playlist
    id: String,
    onNavigateBack: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val playerUiState by playerViewModel.uiState.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    
    // Filter tracks based on type and id
    val tracks = remember(type, id, libraryUiState.tracks) {
        when (type) {
            "album" -> libraryUiState.tracks.filter { it.albumId.toString() == id || it.album == id }
            "artist" -> libraryUiState.tracks.filter { it.artistId.toString() == id || it.artist == id }
            else -> emptyList() // Playlist logic requires separate fetching or improved state
        }
    }

    // Derivative info for header
    val headerTitle = remember(type, id, tracks) {
        when (type) {
            "album" -> tracks.firstOrNull()?.album ?: id
            "artist" -> tracks.firstOrNull()?.artist ?: id
            "playlist" -> "Playlist" // Placeholder
            else -> id
        }
    }
    
    val headerSubtitle = remember(type, tracks) {
        when (type) {
            "album" -> tracks.firstOrNull()?.artist ?: "Unknown Artist"
            "artist" -> "${tracks.size} Songs"
            else -> ""
        }
    }

    val headerImage = tracks.firstOrNull()?.imageUrl

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (type == "playlist") id else "") }, // Only show title in bar for playlists or if scrolled
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    if (headerImage != null) {
                        AsyncImage(
                            model = headerImage,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (type) {
                                    "artist" -> Icons.Default.Person
                                    else -> Icons.Default.Album
                                },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Text Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = headerSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Action Buttons (Play/Shuffle)
                    Row {
                        Button(
                            onClick = { playerViewModel.playAll(tracks, shuffle = false) },
                            enabled = tracks.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { playerViewModel.playAll(tracks, shuffle = true) },
                            enabled = tracks.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Tracks List
            TracksList(
                tracks = tracks,
                currentTrackId = playerUiState.currentTrack?.id,
                isPlaying = playerUiState.isPlaying,
                onTrackClick = { track -> playerViewModel.playAll(tracks.dropWhile { it.id != track.id }) }, // Play from this track onwards? Or just play track? 
                // Better: Play the whole filtered list, starting at this index
                // We need to find index in 'tracks'
                // viewModel.playTracks(tracks, index)
                // Existing playTrack just plays one? No, usually sets context.
                // Let's us playAll with index if possible or just playTrack for now (which might just queue one or Context).
                // Re-using Library logic: viewModel.playTrack(track) usually plays it.
                // But for detailed context (Album), we want to play the Album.
                // Let's implement onTrackClick to play the Album context starting at this song.
                
                // Since PlayerViewModel.playAll takes a list, we can pass the whole list and find index.
                // But playAll puts them all in queue.
                // We need `playAll(tracks, startIndex)`
                // Currently `playAll` logic in VM: `playerManager.playTracks(tracksToPlay, 0)`
                // I should overload `playAll` or modify it to accept startIndex. 
                // For now, let's just make onTrackClick play the list starting from that track's index.
                onTrackClick = { track -> 
                    val index = tracks.indexOfFirst { it.id == track.id }
                    if (index >= 0) {
                         // We don't have playTracks(list, startIndex) in VM yet (publicly). 
                         // But we have `playFromQueue(index)`.
                         // If we replace the queue with `tracks` then `playFromQueue(index)` works.
                         // So: set queue, then play.
                         // PlayerManager.playTracks(tracks, index) does exactly this.
                         // I can access playerManager? No, it's private in VM.
                         // I added `playAll(tracks, shuffle)` which calls `playTracks(..., 0)`.
                         // I should update `playAll` to take `startIndex`.
                         playerViewModel.playAll(tracks, startIndex = index) 
                    }
                },
                onFavoriteClick = { playerViewModel.toggleFavorite() }, // This toggles *current* track favorite in VM. We need to toggle *specific* track.
                // PlayerViewModel.toggleFavorite() uses currentTrack.
                // LibraryViewModel.toggleFavorite(id) exists.
                // So use LibraryViewModel.
                onFavoriteClick = { track -> libraryViewModel.toggleFavorite(track.id) },
                isFavorite = { track -> libraryUiState.favoriteTracks.any { it.id == track.id } }
            )
        }
    }
}
