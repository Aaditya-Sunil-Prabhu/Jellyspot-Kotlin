 package com.jellyspot.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EqualizerIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isAnimating: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Bar 1
        EqualizerBar(
            color = color,
            isAnimating = isAnimating,
            durationMillis = 600,
            delayMillis = 0,
            minHeight = 4.dp,
            maxHeight = 12.dp
        )
        // Bar 2
        EqualizerBar(
            color = color,
            isAnimating = isAnimating,
            durationMillis = 500,
            delayMillis = 100,
            minHeight = 6.dp,
            maxHeight = 16.dp
        )
        // Bar 3
        EqualizerBar(
            color = color,
            isAnimating = isAnimating,
            durationMillis = 700,
            delayMillis = 200,
            minHeight = 5.dp,
            maxHeight = 10.dp
        )
    }
}

@Composable
private fun EqualizerBar(
    color: Color,
    isAnimating: Boolean,
    durationMillis: Int,
    delayMillis: Int,
    minHeight: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_transition")
    
    // Animate height if isAnimating is true, otherwise static
    val height by if (isAnimating) {
        infiniteTransition.animateValue(
            initialValue = minHeight,
            targetValue = maxHeight,
            typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, delayMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq_height"
        )
    } else {
        remember { mutableStateOf(minHeight) }
    }

    Box(
        modifier = Modifier
            .width(3.dp)
            .height(height)
            .background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
    )
}
