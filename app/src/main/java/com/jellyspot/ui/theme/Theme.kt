package com.jellyspot.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Dark color scheme (default for music apps)
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = JellyspotBackground,
    surface = JellyspotSurface,
    surfaceVariant = JellyspotSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = JellyspotError
)

// AMOLED dark color scheme
private val AmoledDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceVariant = AmoledSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = JellyspotError
)

// Light color scheme (for accessibility)
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Custom app theme settings
data class JellyspotThemeSettings(
    val isAmoledMode: Boolean = false,
    val useDynamicColor: Boolean = true,
    val adaptiveBackgroundColor: Color? = null
)

val LocalJellyspotThemeSettings = staticCompositionLocalOf { JellyspotThemeSettings() }

/**
 * Jellyspot app theme with Material 3.
 * Supports dynamic colors (Android 12+), AMOLED mode, and adaptive background.
 */
@Composable
fun JellyspotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoledMode: Boolean = false,
    useDynamicColor: Boolean = true,
    adaptiveBackgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color on Android 12+
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // AMOLED dark mode
        darkTheme && isAmoledMode -> AmoledDarkColorScheme
        // Standard dark mode
        darkTheme -> DarkColorScheme
        // Light mode
        else -> LightColorScheme
    }

    // Apply adaptive background color if provided
    val finalColorScheme = if (adaptiveBackgroundColor != null && darkTheme) {
        colorScheme.copy(
            surface = adaptiveBackgroundColor.copy(alpha = 0.15f),
            surfaceVariant = adaptiveBackgroundColor.copy(alpha = 0.1f)
        )
    } else {
        colorScheme
    }

    val themeSettings = JellyspotThemeSettings(
        isAmoledMode = isAmoledMode,
        useDynamicColor = useDynamicColor,
        adaptiveBackgroundColor = adaptiveBackgroundColor
    )

    CompositionLocalProvider(LocalJellyspotThemeSettings provides themeSettings) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            content = content
        )
    }
}
