package com.jellyspot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jellyspot.data.local.entities.TrackEntity

@Composable
fun TracksList(
    tracks: List<TrackEntity>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (TrackEntity) -> Unit,
    onFavoriteClick: (TrackEntity) -> Unit,
    isFavorite: (TrackEntity) -> Boolean,
    onMenuClick: (TrackEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector = Icons.Default.MusicNote,
    emptyTitle: String = "No songs found",
    emptySubtitle: String = "Tap refresh to scan your device for music"
) {
    if (tracks.isEmpty()) {
        EmptyState(
            icon = emptyIcon,
            title = emptyTitle,
            subtitle = emptySubtitle
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                TrackItem(
                    track = track,
                    isCurrentTrack = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    onClick = { onTrackClick(track) },
                    onFavoriteClick = { onFavoriteClick(track) },
                    isFavorite = isFavorite(track),
                    onMenuClick = { onMenuClick(track) }
                )
            }
        }
    }
}

@Composable
fun TrackItem(
    track: TrackEntity,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean,
    onMenuClick: () -> Unit = {}
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(if (isCurrentTrack) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)) else Modifier),
        headlineContent = {
            Text(
                track.name, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
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
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                    if (track.imageUrl != null) {
                        AsyncImage(
                            model = track.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Equalizer Overlay for current track
                    if (isCurrentTrack) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            EqualizerIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                isAnimating = isPlaying
                            )
                        }
                    }
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
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
