package com.jellyspot.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.jellyspot.data.local.entities.TrackEntity
import com.jellyspot.ui.components.SongOption
import com.jellyspot.ui.components.SongOptionsSheet
import kotlinx.coroutines.delay
import java.util.Calendar

// Get greeting based on time of day
private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        hour in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

// Get quirky subtitle based on time
private fun getQuirkySubtitle(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val quotes = when {
        hour in 5..11 -> listOf("Rise and shine! ☀️", "Coffee first, music second. ☕", "Morning vibes loading... 🔋")
        hour in 12..16 -> listOf("Keep the momentum going. 🚀", "Focus mode: ON. 🎧", "Afternoon jams incoming.")
        hour in 17..20 -> listOf("Unwind time. 🍷", "Relax and listen. 🛋️", "Evening chill session.")
        else -> listOf("Late night vibes. 🌙", "The world is quiet. 🤫", "Just you and the music.")
    }
    return quotes.random()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Song options bottom sheet state
    var selectedTrackForMenu by remember { mutableStateOf<TrackEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Typing animation for greeting
    var displayedGreeting by remember { mutableStateOf("") }
    val fullGreeting = remember { getGreeting() }
    val quirkySubtitle = remember { getQuirkySubtitle() }
    
    LaunchedEffect(Unit) {
        fullGreeting.forEachIndexed { index, _ ->
            delay(80)
            displayedGreeting = fullGreeting.substring(0, index + 1)
        }
    }
    
    // Animation visibility
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            delay(200)
            showContent = true
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header with greeting
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = displayedGreeting,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            // Reserve space for subtitle to prevent layout shift
                            Box(modifier = Modifier.height(24.dp)) {
                                AnimatedVisibility(
                                    visible = displayedGreeting == fullGreeting,
                                    enter = fadeIn()
                                ) {
                                    Text(
                                        text = quirkySubtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        SourceToggleChip(
                            currentSource = uiState.dataSource,
                            onSourceChange = { viewModel.setDataSource(it) }
                        )
                    }
                }
            }
            
            // Error state
            if (uiState.errorMessage != null) {
                item {
                    ErrorCard(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
            }
            
            // Empty state
            if (uiState.sections.isEmpty() && !uiState.isLoading && uiState.errorMessage == null) {
                item {
                    EmptyStateCard(onScanLibrary = { viewModel.scanLocalLibrary() })
                }
            }
            
            // Sections
            items(uiState.sections) { section ->
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                        initialOffsetY = { 50 },
                        animationSpec = tween(500)
                    )
                ) {
                    when (section.type) {
                        SectionType.ARTISTS -> ArtistsSection(
                            title = section.title,
                            artists = section.artists,
                            onArtistClick = { artist ->
                                onNavigateToDetail("artist", artist.id)
                            }
                        )
                        else -> TrackSection(
                            section = section,
                            onTrackClick = { track ->
                                // Just play the track - mini player will show automatically
                                viewModel.playTrack(track, section.tracks)
                            },
                            onMenuClick = { track -> selectedTrackForMenu = track }
                        )
                    }
                }
            }
        }
    }
    
    // Song options bottom sheet
    selectedTrackForMenu?.let { track ->
        SongOptionsSheet(
            track = track,
            sheetState = sheetState,
            onDismiss = { selectedTrackForMenu = null },
            onOptionClick = { option ->
                // TODO: Implement option actions
                selectedTrackForMenu = null
            }
        )
    }
}

@Composable
private fun SourceToggleChip(
    currentSource: String,
    onSourceChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (currentSource) {
                    "jellyfin" -> Icons.Default.Cloud
                    else -> Icons.Default.PhoneAndroid
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (currentSource == "jellyfin") "Jellyfin" else "Local",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Local Music") },
                onClick = { onSourceChange("local"); expanded = false },
                leadingIcon = { Icon(Icons.Default.PhoneAndroid, null) }
            )
            DropdownMenuItem(
                text = { Text("Jellyfin") },
                onClick = { onSourceChange("jellyfin"); expanded = false },
                leadingIcon = { Icon(Icons.Default.Cloud, null) }
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyStateCard(onScanLibrary: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No music found", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Scan your device for music", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp))
            FilledTonalButton(onClick = onScanLibrary) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Library")
            }
        }
    }
}

// ==================== ARTISTS SECTION ====================
@Composable
private fun ArtistsSection(
    title: String,
    artists: List<ArtistItem>,
    onArtistClick: (ArtistItem) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists) { artist ->
                ArtistCircle(artist = artist, onClick = { onArtistClick(artist) })
            }
        }
    }
}

@Composable
private fun ArtistCircle(artist: ArtistItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(90.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
                if (artist.imageUrl != null) {
                    AsyncImage(
                        model = artist.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ==================== TRACK SECTIONS ====================
@Composable
private fun TrackSection(
    section: HomeSection,
    onTrackClick: (TrackEntity) -> Unit,
    onMenuClick: (TrackEntity) -> Unit = {}
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        when (section.type) {
            SectionType.QUICK_PICKS -> {
                // Horizontal scrollable list (4 items visible)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(section.tracks) { index, track ->
                        val nextTrack = section.tracks.getOrNull(index + 1)
                        QuickPicksListItem(
                            track = track,
                            nextTrack = nextTrack,
                            onClick = { onTrackClick(track) },
                            onMenuClick = { onMenuClick(track) }
                        )
                    }
                }
            }
            SectionType.HORIZONTAL -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(section.tracks) { track ->
                        TrackCard(track = track, onClick = { onTrackClick(track) }, modifier = Modifier.width(120.dp))
                    }
                }
            }
            SectionType.GRID -> {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(380.dp)
                ) {
                    items(section.tracks) { track ->
                        TrackCard(track = track, onClick = { onTrackClick(track) }, modifier = Modifier.width(120.dp))
                    }
                }
            }
            SectionType.LARGE -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(section.tracks) { track ->
                        LargeTrackCard(track = track, onClick = { onTrackClick(track) })
                    }
                }
            }
            else -> {} // ARTISTS handled separately
        }
    }
}

/**
 * Quick Picks list item with thumbnail, title+artist, next preview, and menu.
 * Fixed width for horizontal scrolling (shows ~4 items at a time).
 */
@Composable
private fun QuickPicksListItem(
    track: TrackEntity,
    nextTrack: TrackEntity?,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp) // Fixed width for horizontal scroll
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main thumbnail
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (track.imageUrl != null) {
                    AsyncImage(
                        model = track.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Title and artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
            
            // Next track preview (small thumbnail on right)
            if (nextTrack != null) {
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    if (nextTrack.imageUrl != null) {
                        AsyncImage(
                            model = nextTrack.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
            
            // 3-dot menu
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackCard(track: TrackEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Surface(
            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Box {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
                if (track.imageUrl != null) {
                    AsyncImage(
                        model = track.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LargeTrackCard(track: TrackEntity, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.width(300.dp), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (track.imageUrl != null) {
                        AsyncImage(model = track.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = track.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledIconButton(onClick = onClick, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
