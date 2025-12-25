package com.jellyspot.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jellyspot.data.local.entities.TrackEntity

/**
 * Song options bottom sheet - shows when user taps 3-dot menu on a track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    track: TrackEntity,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onOptionClick: (SongOption) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with track info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (track.imageUrl != null) {
                        AsyncImage(
                            model = track.imageUrl,
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Options list
            SongOption.entries.forEach { option ->
                OptionItem(
                    option = option,
                    onClick = { onOptionClick(option) }
                )
            }
        }
    }
}

@Composable
private fun OptionItem(
    option: SongOption,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(option.title) },
        leadingContent = {
            Icon(
                option.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

/**
 * Available options for song menu.
 * TODO: Add actual functionality for each option.
 */
enum class SongOption(val title: String, val icon: ImageVector) {
    LIKE("Like", Icons.Outlined.FavoriteBorder),
    DOWNLOAD("Download", Icons.Outlined.Download),
    ADD_TO_PLAYLIST("Add to a playlist", Icons.Default.PlaylistAdd),
    PLAY_NEXT("Play next", Icons.Default.PlayCircleOutline),
    ADD_TO_QUEUE("Add to queue", Icons.Default.QueueMusic),
    ARTISTS("Artists", Icons.Default.Person),
    ALBUM("Album", Icons.Default.Album),
    START_RADIO("Start radio", Icons.Default.Radio),
    LYRICS_PROVIDER("Main Lyrics Provider", Icons.Outlined.Lyrics),
    SLEEP_TIMER("Sleep Timer", Icons.Outlined.Timer),
    PLAYBACK_SPEED("Playback speed & pitch", Icons.Default.Speed),
    SHARE("Share", Icons.Default.Share)
}
