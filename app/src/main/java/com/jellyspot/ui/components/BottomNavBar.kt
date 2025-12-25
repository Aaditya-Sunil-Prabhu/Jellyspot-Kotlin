package com.jellyspot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bottom navigation bar for main screens.
 */
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { onNavigate("home") }
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentRoute == "library") Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                    contentDescription = "Library"
                )
            },
            label = { Text("Library") },
            selected = currentRoute == "library",
            onClick = { onNavigate("library") }
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentRoute == "search") Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "Search"
                )
            },
            label = { Text("Search") },
            selected = currentRoute == "search",
            onClick = { onNavigate("search") }
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = { onNavigate("settings") }
        )
    }
}
