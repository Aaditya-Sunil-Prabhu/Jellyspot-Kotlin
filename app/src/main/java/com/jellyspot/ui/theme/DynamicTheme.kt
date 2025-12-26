package com.jellyspot.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
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
     * Load image and extract color using Coil3.
     */
    suspend fun extractColorFromUrl(
        context: Context,
        imageUrl: String?
    ): Color = withContext(Dispatchers.IO) {
        if (imageUrl == null) return@withContext defaultDarkColor
        
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Required for Palette
                .build()
            
            val imageLoader = ImageLoader(context)
            val result = imageLoader.execute(request)
            
            if (result is SuccessResult) {
                val bitmap = result.image.toBitmap()
                extractDominantColor(bitmap)
            } else {
                defaultDarkColor
            }
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
 * Composable state for dynamic theme colors extracted from image URL.
 */
@Composable
fun rememberDynamicColorFromUrl(imageUrl: String?): Color {
    val context = LocalContext.current
    var dominantColor by remember(imageUrl) { mutableStateOf(DynamicTheme.defaultDarkColor) }
    
    LaunchedEffect(imageUrl) {
        dominantColor = DynamicTheme.extractColorFromUrl(context, imageUrl)
    }
    
    return animateDynamicColor(dominantColor)
}

/**
 * CompositionLocal for dynamic player color.
 */
val LocalDynamicPlayerColor = compositionLocalOf { DynamicTheme.defaultDarkColor }
