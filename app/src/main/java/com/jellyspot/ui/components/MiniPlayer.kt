package com.jellyspot.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.jellyspot.data.local.entities.TrackEntity
import com.jellyspot.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mini player ViewModel for state management.
 */
@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {
    val currentTrack: StateFlow<TrackEntity?> = playerManager.currentTrack
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val positionMs: StateFlow<Long> = playerManager.positionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    
    fun togglePlayPause() = playerManager.togglePlayPause()
    fun skipNext() = playerManager.skipNext()
    fun skipPrevious() = playerManager.skipPrevious()
}

/**
 * Swipe direction for animation control.
 */
private enum class SwipeDirection {
    NONE, LEFT, RIGHT
}

/**
 * Mini player component shown at the bottom of main screens.
 * Features:
 * - Swipe left for next song (text slides right to left)
 * - Swipe right for previous song (text slides left to right)
 * - Swipe/drag up to expand to fullscreen player
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onExpandPlayer: () -> Unit,
    viewModel: MiniPlayerViewModel = hiltViewModel()
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    
    // Swipe tracking for animations
    var horizontalSwipeOffset by remember { mutableFloatStateOf(0f) }
    var verticalSwipeOffset by remember { mutableFloatStateOf(0f) }
    var lastSwipeDirection by remember { mutableStateOf(SwipeDirection.NONE) }
    
    // Track change counter to trigger animation
    var trackChangeKey by remember { mutableIntStateOf(0) }
    
    // Remember previous track to detect changes via swipe
    var previousTrackId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(currentTrack?.id) {
        if (previousTrackId != null && currentTrack?.id != previousTrackId) {
            // Track changed, increment key for animation
            trackChangeKey++
        }
        previousTrackId = currentTrack?.id
    }
    
    // Alpha for drag-up gesture
    val dragAlpha = (1f - (-verticalSwipeOffset / 300f).coerceIn(0f, 1f))

    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .alpha(dragAlpha)
                // Vertical drag to expand player
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (verticalSwipeOffset < -100) {
                                // Dragged up enough - expand player
                                onExpandPlayer()
                            }
                            verticalSwipeOffset = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            verticalSwipeOffset += dragAmount
                        }
                    )
                }
                // Horizontal swipe for prev/next
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                horizontalSwipeOffset > 80 -> {
                                    // Swipe right = previous song
                                    lastSwipeDirection = SwipeDirection.RIGHT
                                    viewModel.skipPrevious()
                                }
                                horizontalSwipeOffset < -80 -> {
                                    // Swipe left = next song
                                    lastSwipeDirection = SwipeDirection.LEFT
                                    viewModel.skipNext()
                                }
                            }
                            horizontalSwipeOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalSwipeOffset += dragAmount
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column {
                // Progress bar at top
                if (durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandPlayer() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art with animated content
                    AnimatedContent(
                        targetState = currentTrack,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200))
                                .togetherWith(fadeOut(animationSpec = tween(200)))
                        },
                        label = "mini_player_art"
                    ) { track ->
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                )
                                if (track?.imageUrl != null) {
                                    AsyncImage(
                                        model = track.imageUrl,
                                        contentDescription = "Album art",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track info with directional animation
                    AnimatedContent(
                        targetState = Pair(currentTrack, trackChangeKey),
                        transitionSpec = {
                            // Determine slide direction based on last swipe
                            val slideDirection = when (lastSwipeDirection) {
                                SwipeDirection.RIGHT -> {
                                    // Previous song: text slides from left
                                    slideInHorizontally { -it } + fadeIn() togetherWith
                                        slideOutHorizontally { it } + fadeOut()
                                }
                                SwipeDirection.LEFT -> {
                                    // Next song: text slides from right
                                    slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it } + fadeOut()
                                }
                                SwipeDirection.NONE -> {
                                    // Default: simple fade
                                    fadeIn() togetherWith fadeOut()
                                }
                            }
                            slideDirection.using(SizeTransform(clip = false))
                        },
                        label = "mini_player_info",
                        modifier = Modifier.weight(1f)
                    ) { (track, _) ->
                        Column {
                            Text(
                                text = track?.name ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(
                                    iterations = Int.MAX_VALUE
                                )
                            )
                            Text(
                                text = track?.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Play/Pause button
                    IconButton(onClick = { viewModel.togglePlayPause() }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next button
                    IconButton(onClick = { 
                        lastSwipeDirection = SwipeDirection.LEFT
                        viewModel.skipNext() 
                    }) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }
    }
}
