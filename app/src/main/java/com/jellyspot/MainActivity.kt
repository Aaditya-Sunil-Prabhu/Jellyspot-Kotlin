package com.jellyspot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jellyspot.ui.navigation.JellyspotNavGraph
import com.jellyspot.ui.theme.JellyspotTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Jellyspot.
 * Sets up edge-to-edge display, splash screen, and the Compose UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            JellyspotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JellyspotNavGraph()
                }
            }
        }
    }
}
