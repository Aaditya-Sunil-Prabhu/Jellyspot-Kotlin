package com.jellyspot.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack

    // Gradient background based on theme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Now Playing", style = MaterialTheme.typography.labelSmall)
                            Text(
                                track?.album ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleQueue() }) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (uiState.showQueue) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { /* More options */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (uiState.showQueue) {
                // Queue view
                QueueView(
                    queue = uiState.queue,
                    currentTrack = track,
                    onTrackClick = { viewModel.playFromQueue(it) },
                    onRemoveClick = { viewModel.removeFromQueue(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                // Player view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Album Art
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp
                    ) {
                        if (track?.imageUrl != null) {
                            AsyncImage(
                                model = track.imageUrl,
                                contentDescription = "Album art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Track info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = track?.name ?: "No track playing",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track?.artist ?: "—",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = if (uiState.durationMs > 0) 
                                uiState.positionMs.toFloat() / uiState.durationMs 
                            else 0f,
                            onValueChange = { value ->
                                viewModel.seekTo((value * uiState.durationMs).toLong())
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatTime(uiState.positionMs),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                formatTime(uiState.durationMs),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (uiState.shuffleEnabled) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Previous
                        IconButton(
                            onClick = { viewModel.skipPrevious() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Play/Pause
                        FilledIconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next
                        IconButton(
                            onClick = { viewModel.skipNext() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Repeat
                        IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                            Icon(
                                when (uiState.repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (uiState.repeatMode != Player.REPEAT_MODE_OFF) 
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.toggleLyrics() }) {
                            Icon(Icons.Default.Lyrics, contentDescription = "Lyrics")
                        }
                        IconButton(onClick = { /* Share */ }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun QueueView(
    queue: List<com.jellyspot.data.local.entities.TrackEntity>,
    currentTrack: com.jellyspot.data.local.entities.TrackEntity?,
    onTrackClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                "Queue (${queue.size} tracks)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        itemsIndexed(queue, key = { _, track -> track.id }) { index, track ->
            val isCurrentTrack = track.id == currentTrack?.id
            ListItem(
                modifier = Modifier
                    .clickable { onTrackClick(index) }
                    .then(
                        if (isCurrentTrack) Modifier.background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        ) else Modifier
                    ),
                headlineContent = {
                    Text(
                        track.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrentTrack) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = {
                    if (isCurrentTrack) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingContent = {
                    IconButton(onClick = { onRemoveClick(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
