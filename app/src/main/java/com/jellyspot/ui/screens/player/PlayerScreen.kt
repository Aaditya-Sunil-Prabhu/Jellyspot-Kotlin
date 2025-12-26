package com.jellyspot.ui.screens.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.jellyspot.data.local.entities.TrackEntity
import com.jellyspot.ui.components.ArtistCard
import com.jellyspot.ui.components.LyricsSection
import com.jellyspot.ui.components.SongOption
import com.jellyspot.ui.components.SongOptionsSheet
import com.jellyspot.ui.theme.DynamicTheme
import com.jellyspot.ui.theme.rememberDynamicColorFromUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack
    val scrollState = rememberScrollState()
    
    // Song options sheet state
    var showOptionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // Dynamic theme color from album art
    val animatedColor = rememberDynamicColorFromUrl(track?.imageUrl)
    
    // Header transition based on scroll
    val headerAlpha by remember {
        derivedStateOf {
            (scrollState.value.toFloat() / 600f).coerceIn(0f, 1f)
        }
    }
    
    // Gradient background with dynamic color
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DynamicTheme.darkenColor(animatedColor, 0.6f),
                        DynamicTheme.darkenColor(animatedColor, 0.2f),
                        Color.Black
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Box(contentAlignment = Alignment.Center) {
                            // Initial State: NOW PLAYING
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(1f - headerAlpha)
                            ) {
                                Text(
                                    "NOW PLAYING",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    "\"${track?.album ?: "Unknown"}\" Radio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Scrolled State: Song Title - Artist
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(headerAlpha)
                            ) {
                                Text(
                                    track?.name ?: "Unknown",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    track?.artist ?: "Unknown Artist",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.KeyboardArrowDown, 
                                contentDescription = "Minimize",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showOptionsSheet = true }) {
                            Icon(
                                Icons.Default.MoreVert, 
                                contentDescription = "More",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            if (uiState.showQueue) {
                QueueView(
                    queue = uiState.queue,
                    currentTrack = track,
                    onTrackClick = { viewModel.playFromQueue(it) },
                    onRemoveClick = { viewModel.removeFromQueue(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                // Scrollable player content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                ) {
                    // Main player content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Album Art
                        AnimatedContent(
                            targetState = track,
                            transitionSpec = {
                                (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)))
                                    .togetherWith(fadeOut(tween(300)) + scaleOut(targetScale = 0.92f, animationSpec = tween(300)))
                            },
                            label = "album_art"
                        ) { currentTrack ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 24.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            ) {
                                if (currentTrack?.imageUrl != null) {
                                    AsyncImage(
                                        model = currentTrack.imageUrl,
                                        contentDescription = "Album art",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(100.dp),
                                            tint = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Track info with action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track?.name ?: "No track",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track?.artist ?: "—",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Add to playlist button
                            IconButton(onClick = { /* TODO */ }) {
                                Icon(
                                    Icons.Outlined.AddCircleOutline,
                                    contentDescription = "Add to playlist",
                                    tint = Color.White
                                )
                            }
                            
                            // Favorite button
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (uiState.isFavorite) Color.Red else Color.White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Progress slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = if (uiState.durationMs > 0) 
                                    uiState.positionMs.toFloat() / uiState.durationMs 
                                else 0f,
                                onValueChange = { value ->
                                    viewModel.seekTo((value * uiState.durationMs).toLong())
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    formatTime(uiState.positionMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    formatTime(uiState.durationMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Playback controls
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
                                    tint = if (uiState.shuffleEnabled) animatedColor else Color.White
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
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.White
                                )
                            }
                            
                            // Play/Pause (large circle)
                            Surface(
                                onClick = { viewModel.togglePlayPause() },
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(36.dp),
                                        tint = Color.Black
                                    )
                                }
                            }
                            
                            // Next
                            IconButton(
                                onClick = { viewModel.skipNext() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.White
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
                                        animatedColor else Color.White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Bottom action row (Info and Queue)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { /* TODO: Show track info */ }) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = "Track info",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = { viewModel.toggleQueue() }) {
                                Icon(
                                    Icons.Default.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = if (uiState.showQueue) animatedColor else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Lyrics Section
                    LyricsSection(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .heightIn(min = 400.dp),
                        backgroundColor = animatedColor,
                        onShowClick = { viewModel.toggleLyrics() }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Artist Card
                    ArtistCard(
                        artistName = track?.artist ?: "Unknown Artist",
                        artistImageUrl = null, // TODO: Get artist image
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Bottom padding
                }
            }
        }
    }
    
    // Song options bottom sheet
    if (showOptionsSheet && track != null) {
        SongOptionsSheet(
            track = track,
            sheetState = sheetState,
            onDismiss = { showOptionsSheet = false },
            onOptionClick = { option ->
                // TODO: Implement option actions
                showOptionsSheet = false
            }
        )
    }
}

@Composable
private fun QueueView(
    queue: List<TrackEntity>,
    currentTrack: TrackEntity?,
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
                color = Color.White,
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
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ) else Modifier
                    ),
                headlineContent = {
                    Text(
                        track.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrentTrack) Color.White else Color.White.copy(alpha = 0.8f)
                    )
                },
                supportingContent = {
                    Text(
                        track.artist, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                },
                leadingContent = {
                    if (isCurrentTrack) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    } else {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                },
                trailingContent = {
                    IconButton(onClick = { onRemoveClick(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
