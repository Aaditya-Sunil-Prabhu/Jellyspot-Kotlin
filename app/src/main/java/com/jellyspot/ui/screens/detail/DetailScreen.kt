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
import com.jellyspot.ui.screens.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    type: String, // album, artist, playlist
    id: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allTracks = uiState.queue.ifEmpty { 
        // Fallback: This is mainly a UI mock since we don't have a separate DetailViewModel yet
        // In a real app, we'd fetch specific tracks by ID.
        // For now, filtering current tracks in memory or showing placeholder if empty.
        // TODO: Inject LibraryViewModel or separate DetailViewModel
        emptyList() 
    }
    
    // Placeholder logic for demo: Filter tracks based on type/id from the global queue or repository
    // Since we don't have easy access to repository here without VM, we'll use a placeholder UI
    // In a real implementation, ViewModel should load data.
    
    // TEMPORARY: Just show a generic detail page
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(id) }, // Display ID/Name as title
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    when(type) {
                        "artist" -> Icons.Default.Person
                        "album" -> Icons.Default.Album
                        else -> Icons.Default.QueueMusic
                    },
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "$type: $id",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Feature coming soon",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
