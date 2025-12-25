package com.jellyspot.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Sources") {
                    SettingsItem(icon = Icons.Default.Storage, title = "Source Mode", subtitle = "Configure music sources")
                }
            }
            item {
                SettingsSection(title = "Playback") {
                    SettingsItem(icon = Icons.Default.Equalizer, title = "Audio Quality", subtitle = "Lossless")
                    SettingsItem(icon = Icons.Default.Shuffle, title = "Crossfade", subtitle = "Off")
                }
            }
            item {
                SettingsSection(title = "Appearance") {
                    SettingsItem(icon = Icons.Default.Palette, title = "Theme", subtitle = "System default")
                    SettingsItem(icon = Icons.Default.DarkMode, title = "AMOLED Mode", subtitle = "Off")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp), content = content)
        }
    }
}

@Composable
private fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
    )
}
