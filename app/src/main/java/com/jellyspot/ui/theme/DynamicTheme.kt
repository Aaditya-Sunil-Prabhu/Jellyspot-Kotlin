package com.jellyspot.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dynamic theme utilities for extracting dominant color from album art.
 */
object DynamicTheme {
    
    /**
     * Default dark color when no album art is available.
     */
    val defaultDarkColor = Color(0xFF1A1A1A)
    
    /**
     * Extract dominant color from a bitmap.
     */
    suspend fun extractDominantColor(bitmap: Bitmap?): Color = withContext(Dispatchers.Default) {
        if (bitmap == null) return@withContext defaultDarkColor
        
        try {
            val palette = Palette.from(bitmap).generate()
            
            // Try to get the most vibrant/saturated color
            val dominantSwatch = palette.vibrantSwatch
                ?: palette.mutedSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.darkMutedSwatch
                ?: palette.dominantSwatch
            
            dominantSwatch?.rgb?.let { Color(it) } ?: defaultDarkColor
        } catch (e: Exception) {
            defaultDarkColor
        }
    }
    
    /**
     * Darken a color for use as background.
     */
    fun darkenColor(color: Color, factor: Float = 0.3f): Color {
        val argb = color.toArgb()
        val r = ((argb shr 16) and 0xFF) * factor
        val g = ((argb shr 8) and 0xFF) * factor
        val b = (argb and 0xFF) * factor
        return Color(r.toInt(), g.toInt(), b.toInt())
    }
}

/**
 * Composable state for dynamic theme colors.
 */
@Composable
fun rememberDynamicThemeState(): DynamicThemeState {
    return remember { DynamicThemeState() }
}

@Stable
class DynamicThemeState {
    var dominantColor by mutableStateOf(DynamicTheme.defaultDarkColor)
        private set
    
    fun updateColor(color: Color) {
        dominantColor = color
    }
}

/**
 * Animate color changes smoothly.
 */
@Composable
fun animateDynamicColor(targetColor: Color): Color {
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "dynamic_color"
    )
    return animatedColor
}

/**
 * CompositionLocal for dynamic player color.
 */
val LocalDynamicPlayerColor = compositionLocalOf { DynamicTheme.defaultDarkColor }
