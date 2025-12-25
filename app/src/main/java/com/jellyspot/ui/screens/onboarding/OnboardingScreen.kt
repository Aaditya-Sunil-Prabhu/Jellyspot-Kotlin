package com.jellyspot.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Jellyspot", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Choose how you want to listen to music", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), onClick = { onComplete() }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Local Music Only", style = MaterialTheme.typography.titleMedium)
                Text("Play music stored on your device", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), onClick = { onComplete() }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Jellyfin Server", style = MaterialTheme.typography.titleMedium)
                Text("Stream from your Jellyfin media server", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), onClick = { onComplete() }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Both Sources", style = MaterialTheme.typography.titleMedium)
                Text("Access local music and Jellyfin", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
