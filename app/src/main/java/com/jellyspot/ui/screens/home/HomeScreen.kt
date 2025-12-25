package com.jellyspot.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.jellyspot.data.local.entities.TrackEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jellyspot") },
                actions = {
                    // Source selector
                    var showSourceMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSourceMenu = true }) {
                        Icon(
                            when (uiState.dataSource) {
                                "jellyfin" -> Icons.Default.Cloud
                                "local" -> Icons.Default.PhoneAndroid
                                else -> Icons.Default.MusicNote
                            },
                            contentDescription = "Source"
                        )
                    }
                    DropdownMenu(
                        expanded = showSourceMenu,
                        onDismissRequest = { showSourceMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Local Music") },
                            onClick = {
                                viewModel.setDataSource("local")
                                showSourceMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Jellyfin") },
                            onClick = {
                                viewModel.setDataSource("jellyfin")
                                showSourceMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Cloud, null) }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.errorMessage != null) {
                // Error state
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Error, null, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.errorMessage!!, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            } else if (uiState.sections.isEmpty() && !uiState.isLoading) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No music found", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Scan your device for music or connect to Jellyfin",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.scanLocalLibrary() }) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Library")
                    }
                }
            } else {
                // Content
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(uiState.sections) { section ->
                        HomeSection(
                            section = section,
                            onTrackClick = { track ->
                                viewModel.playTrack(track, section.tracks)
                                onNavigateToPlayer()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSection(
    section: HomeSection,
    onTrackClick: (TrackEntity) -> Unit
) {
    Column {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        when (section.type) {
            SectionType.HORIZONTAL -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(section.tracks) { track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(track) },
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }
            SectionType.GRID -> {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(section.tracks) { track ->
                        TrackCard(
                            track = track,
                            onClick = { onTrackClick(track) },
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }
            SectionType.LARGE -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(section.tracks) { track ->
                        LargeTrackCard(
                            track = track,
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: TrackEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
            tonalElevation = 2.dp
        ) {
            AsyncImage(
                model = track.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LargeTrackCard(
    track: TrackEntity,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(280.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
