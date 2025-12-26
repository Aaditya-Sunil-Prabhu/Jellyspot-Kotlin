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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
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
import androidx.compose.ui.geometry.Offset
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
import com.jellyspot.ui.components.EqualizerIndicator
import com.jellyspot.ui.components.SongOption
import com.jellyspot.ui.components.SongOptionsSheet
import com.jellyspot.ui.theme.DynamicTheme
import com.jellyspot.ui.theme.rememberDynamicColorFromUrl
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val track = uiState.currentTrack
    val scrollState = rememberScrollState()
    
    // Back Handler: Close player if expanded or queue is open
    BackHandler(enabled = isExpanded || uiState.showQueue) {
        if (uiState.showQueue) {
            viewModel.toggleQueue()
        } else {
            onDismiss()
        }
    }
    
    // Song options sheet state
    var showOptionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // Slider state for smooth seeking
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    
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
                                Spacer(modifier = Modifier.height(4.dp)) // Increased gap
                                Text(
                                    "\"${track?.album ?: "Unknown"}\" Radio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f), // Faded subtext
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Scrolled State: Song Title - Artist (Row Layout)
                            // Scrolled State: Song Title - Artist (Row Layout)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(headerAlpha)
                                    .padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Title and Artist
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            track?.name ?: "Unknown",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                        Text(
                                            track?.artist ?: "Unknown Artist",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    }
                                    
                                    // Right: Controls
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                         // Moved Like/Pause to right as requested
                                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                                            Icon(
                                                if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = "Favorite",
                                                tint = if (uiState.isFavorite) Color.Red else Color.White
                                            )
                                        }
                                        IconButton(onClick = { viewModel.togglePlayPause() }) {
                                            Icon(
                                                if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White
                                            )
                                        }
                                        IconButton(onClick = { showOptionsSheet = true }) {
                                            Icon(
                                                Icons.Default.MoreVert, 
                                                contentDescription = "More",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                                
                                // Progress bar extended full width
                                if (uiState.durationMs > 0) {
                                    LinearProgressIndicator(
                                        progress = { (uiState.positionMs.toFloat() / uiState.durationMs).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .height(2.dp),
                                        trackColor = Color.White.copy(alpha = 0.3f),
                                        color = Color.White
                                    )
                                }
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
                        // Options menu moved inside scrolled row for consistency or kept here -> User asked to move Like/Pause next to 3 dots.
                        // We put 3 dots in the Row above, so we can remove this Action slot or keep it empty for initial state?
                        // The user said: "push the like and pause button to the end next to the 3 dots menu" in header player.
                        // And "Header Player: Extend seek bar". 
                        
                        // If header is visible (scrolled), we show the Row.
                        // If not (initial), we show the centralized NOW PLAYING.
                        // The standard 3 dots top right is usually for the main player actions.
                        
                        // We'll keep the top-right 3 dots for the Initial State (when not scrolled).
                        // When scrolled, the Row takes over title slot.
                         if (headerAlpha < 0.5f) {
                            IconButton(onClick = { showOptionsSheet = true }) {
                                Icon(
                                    Icons.Default.MoreVert, 
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Main Player Content (Below Queue)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Main player content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Album Art
                        // Horizontal Drag for Carousel (Next/Prev)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount < -50) {
                                            viewModel.skipNext()
                                        } else if (dragAmount > 50) {
                                            viewModel.skipPrevious()
                                        }
                                    }
                                }
                        ) {
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
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (currentTrack?.imageUrl != null) {
                                        AsyncImage(
                                            model = currentTrack.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
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
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = track?.artist ?: "—",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
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
                                value = if (isDraggingSlider) sliderPosition else if (uiState.durationMs > 0) 
                                    uiState.positionMs.toFloat() / uiState.durationMs 
                                else 0f,
                                onValueChange = { value ->
                                    isDraggingSlider = true
                                    sliderPosition = value
                                },
                                onValueChangeFinished = {
                                    viewModel.seekTo((sliderPosition * uiState.durationMs).toLong())
                                    isDraggingSlider = false
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

                
                // Queue Overlay
                AnimatedVisibility(
                    visible = uiState.showQueue,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    QueueView(
                        queue = uiState.queue,
                        currentTrack = track,
                        isPlaying = uiState.isPlaying,
                        onTrackClick = { index -> viewModel.playFromQueue(index) },
                        onRemoveClick = { index -> viewModel.removeFromQueue(index) },
                        onMoveTrack = { from, to -> viewModel.moveTrack(from, to) },
                        onClose = { viewModel.toggleQueue() },
                        modifier = Modifier.fillMaxSize()
                    )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueView(
    queue: List<TrackEntity>,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    onTrackClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var isLocked by remember { mutableStateOf(true) }
    
    // Drag state
    val density = LocalDensity.current
    val itemHeight = 72.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }
    
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Auto-scroll to current track
    LaunchedEffect(currentTrack) {
        if (currentTrack != null) {
            val index = queue.indexOfFirst { it.id == currentTrack.id }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Queue Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Playing Queue",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${queue.size} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Lock Toggle
                IconButton(onClick = { isLocked = !isLocked }) {
                    Icon(
                        if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isLocked) "Unlock Queue" else "Lock Queue",
                        tint = if (isLocked) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                
                // Close Button
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Queue", tint = Color.White)
                }
            }
        }
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(queue, key = { _, track -> track.id }) { index, track ->
                val isCurrentTrack = track.id == currentTrack?.id
                val isDragging = index == draggedIndex
                
                // Swipe to dismiss setup
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (!isLocked && (it == SwipeToDismissBoxValue.EndToStart)) {
                            onRemoveClick(index)
                            true
                        } else false
                    }
                )

                if (isLocked) {
                    QueueItemContent(
                        track = track,
                        isCurrent = isCurrentTrack,
                        isDragging = isDragging,
                        dragOffsetY = dragOffsetY,
                        isLocked = true,
                        onDragStart = {},
                        onDrag = { _, _ -> },
                        onDragEnd = {},
                        onTrackClick = { onTrackClick(index) }
                    )
                } else {
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        gesturesEnabled = !isLocked,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red.copy(alpha = 0.8f))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        },
                        content = {
                            QueueItemContent(
                                track = track,
                                isCurrent = isCurrentTrack,
                                isDragging = isDragging,
                                dragOffsetY = dragOffsetY,
                                isLocked = false,
                                onDragStart = { 
                                    draggedIndex = index 
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount -> 
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                },
                                onDragEnd = {
                                    val moveBy = (dragOffsetY / itemHeightPx).roundToInt()
                                    val targetIndex = (draggedIndex + moveBy).coerceIn(0, queue.size - 1)
                                    if (targetIndex != draggedIndex) {
                                        onMoveTrack(draggedIndex, targetIndex)
                                    }
                                    draggedIndex = -1
                                    dragOffsetY = 0f
                                },
                                onTrackClick = { onTrackClick(index) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemContent(
    track: TrackEntity,
    isCurrent: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    isLocked: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTrackClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp) // Fixed height for calculation
            .clickable(onClick = onTrackClick)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                zIndex = if (isDragging) 1f else 0f
                alpha = if (isDragging) 0.9f else 1f
            },
        colors = ListItemDefaults.colors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
        ),
        headlineContent = { 
            Text(
                track.name, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White
            ) 
        },
        supportingContent = { 
            Text(
                track.artist, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.7f)
            ) 
        },
        leadingContent = {
            if (track.imageUrl != null) {
                AsyncImage(
                    model = track.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                 Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                    }
                }
            }
        },
        trailingContent = {
            if (!isLocked) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd
                        )
                    }
                )
            } else if (isCurrent) {
                 Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}


private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
