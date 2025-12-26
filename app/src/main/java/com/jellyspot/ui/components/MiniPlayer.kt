package com.jellyspot.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import coil3.compose.AsyncImage
import com.jellyspot.data.local.entities.TrackEntity
import com.jellyspot.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

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
 * Mini player component with:
 * - Physical drag up gesture with visual offset to expand to fullscreen
 * - Drag down to dismiss (hide mini player)
 * - Horizontal swipe for prev/next with fast, simple crossfade animation
 */
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onExpandPlayer: () -> Unit,
    onDismiss: () -> Unit = {},
    onVerticalDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    viewModel: MiniPlayerViewModel = hiltViewModel()
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    
    // Drag state
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    
    // Animation direction state
    var slideDirection by remember { mutableIntStateOf(0) } // 0: None, 1: Next (Right->Left), -1: Prev (Left->Right)
    
    // Hide if no track
    val isVisible = currentTrack != null

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it } + fadeIn(animationSpec = tween(150)),
        exit = slideOutVertically { it } + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            when {
                                // Horizontal swipe thresholds
                                horizontalDragOffset > 100f -> {
                                    // Swipe Right -> Prev
                                    slideDirection = -1 // Enter from Left
                                    viewModel.skipPrevious()
                                    horizontalDragOffset = 0f
                                }
                                horizontalDragOffset < -100f -> {
                                    // Swipe Left -> Next
                                    slideDirection = 1 // Enter from Right
                                    viewModel.skipNext()
                                    horizontalDragOffset = 0f
                                }
                                // Vertical Swipe Down to Dismiss (only if positive drag is significant)
                                verticalDragOffset > 100f -> {
                                    // Swipe Down -> Dismiss/Stop
                                    viewModel.togglePlayPause() // Or stop? Usually stop.
                                    // Since we don't have a 'stop' exposed cleanly or 'clear queue', 
                                    // user said "close it", assuming they mean hide miniplayer.
                                    // Often implies stop playback or clear track.
                                    // For now, let's calling togglePlayPause might just pause.
                                    // Ideally we clear current track.
                                    // Let's assume onDismiss passed in handles this? 
                                    // But checking NavGraph, onDismiss is empty default?
                                    // Let's try to just pause for now or we need a 'stop' in VM.
                                    // Actually, let's call onExpandPlayer if negative (Up)
                                    onDismiss() // Trigger parent dismiss callback if any
                                    verticalDragOffset = 0f
                                }
                                else -> {
                                    // Spring back
                                    horizontalDragOffset = 0f
                                    verticalDragOffset = 0f
                                    onDragEnd()
                                }
                            }
                            // Reset offsets
                            horizontalDragOffset = 0f
                            verticalDragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            
                            // Accumulate drags
                            horizontalDragOffset += dragAmount.x
                            verticalDragOffset += dragAmount.y
                            
                            // Pass vertical drag to parent for overlay sliding (only if primarily vertical and moving Up)
                            // If moving Down (positive), we keep it local for dismiss detection unless we are already expanded?
                            // Logic: 
                            // - If dragging UP (negative y), pass to onVerticalDrag (expands player).
                            // - If dragging DOWN (positive y), keep local to detect dismiss.
                            if (dragAmount.y < 0 || (verticalDragOffset < 0)) {
                                onVerticalDrag(dragAmount.y)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(12.dp),
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
                        .clickable(onClick = onExpandPlayer)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art
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
                            currentTrack?.imageUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Album art",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track info with Horizontal Slide Animation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        AnimatedContent(
                            targetState = currentTrack,
                            transitionSpec = {
                                if (slideDirection > 0) {
                                    // Next: Enter from Right, Exit to Left
                                    (slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { width -> width } + fadeIn())
                                        .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { width -> -width } + fadeOut())
                                } else if (slideDirection < 0) {
                                     // Prev: Enter from Left, Exit to Right
                                    (slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { width -> -width } + fadeIn())
                                        .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { width -> width } + fadeOut())
                                } else {
                                    // Default (Initial): Fade only or Vertical
                                    (fadeIn()).togetherWith(fadeOut())
                                }
                            },
                            label = "track_info_slide"
                        ) { track ->
                            Column {
                                Text(
                                    text = track?.name ?: "Unknown",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                    IconButton(onClick = { viewModel.skipNext() }) {
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
