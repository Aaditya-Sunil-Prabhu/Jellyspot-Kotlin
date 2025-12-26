package com.jellyspot.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jellyspot.ui.screens.home.HomeScreen
import com.jellyspot.ui.screens.library.LibraryScreen
import com.jellyspot.ui.screens.search.SearchScreen
import com.jellyspot.ui.screens.settings.SettingsScreen
import com.jellyspot.ui.screens.player.PlayerScreen
import com.jellyspot.ui.screens.onboarding.OnboardingScreen
import com.jellyspot.ui.screens.auth.JellyfinLoginScreen
import com.jellyspot.ui.components.BottomNavBar
import com.jellyspot.ui.components.MiniPlayer

/**
 * Navigation routes for the app.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val DETAIL = "detail/{type}/{id}"
    const val DOWNLOADS = "downloads"
    const val JELLYFIN_LOGIN = "jellyfin_login"
    
    fun detail(type: String, id: String) = "detail/$type/$id"
}

/**
 * Bottom navigation destinations.
 */
enum class BottomNavDestination(val route: String, val label: String, val icon: String) {
    HOME(Routes.HOME, "Home", "home"),
    LIBRARY(Routes.LIBRARY, "Library", "library_music"),
    SEARCH(Routes.SEARCH, "Search", "search"),
    SETTINGS(Routes.SETTINGS, "Settings", "settings")
}

/**
 * Main navigation graph for the app.
 */
@Composable
fun JellyspotNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes that should show bottom nav and mini player
    val showBottomBar = currentRoute in listOf(
        Routes.HOME,
        Routes.LIBRARY,
        Routes.SEARCH,
        Routes.SETTINGS,
        Routes.DOWNLOADS
    )

    // Persistent Player State
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeight.toPx() }
    
    val animatedPlayerOffset = remember { Animatable(screenHeightPx) }
    val scope = rememberCoroutineScope()
    
    // Drag Logic
    fun onDrag(delta: Float) {
        scope.launch {
            val newOffset = (animatedPlayerOffset.value + delta).coerceIn(0f, screenHeightPx)
            animatedPlayerOffset.snapTo(newOffset)
        }
    }

    fun onDragEnd() {
        val current = animatedPlayerOffset.value
        // User feedback: "drag down by just half it should automatically close"
        // Making it easier: if dragged down > 20% of screen, close it.
        // Current is offset from top (0 = Expanded, H = Collapsed).
        // If we overlap 20% (from top or bottom), we trigger the snap.
        // To CLOSE: if current > 0.2 * H
        // To OPEN: if current < 0.8 * H (dragged up 20% from bottom)
        val threshold = screenHeightPx * 0.2f
        val openThreshold = screenHeightPx * 0.8f
        
        val target = if (current < openThreshold) 0f else screenHeightPx
        scope.launch {
            animatedPlayerOffset.animateTo(
                targetValue = target,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        }
    }
    
    // MiniPlayer Transition (Opacity & Slide)
    // When progress = 1 (Collapsed): Alpha = 1, TranslationY = 0
    // When progress = 0 (Expanded): Alpha = 0, TranslationY = -50dp (Slide up)
    val playerProgress by remember {
        derivedStateOf {
            (animatedPlayerOffset.value / screenHeightPx).coerceIn(0f, 1f)
        }
    }
    
    val miniPlayerAlpha by remember { derivedStateOf { playerProgress } }
    val miniPlayerTranslationY by remember { 
        derivedStateOf { 
            // -100px when complete expanded (hidden), 0px when collapsed (visible)
            // "make it seem like it is sliding down and fading in"
            -100f * (1f - playerProgress) 
        } 
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content with Scaffold
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(
                        currentRoute = currentRoute ?: Routes.HOME,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                // Subtle horizontal slide animations for tab navigation
                // Simple fade transitions for better performance
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                popExitTransition = { fadeOut(animationSpec = tween(300)) }

            ) {
                // Onboarding
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onComplete = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                // Home
                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigateToDetail = { type, id ->
                            navController.navigate(Routes.detail(type, id))
                        },
                        onNavigateToPlayer = {
                            scope.launch { animatedPlayerOffset.animateTo(0f) }
                        }
                    )
                }

                // Library
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        onNavigateToDetail = { type, id ->
                            navController.navigate(Routes.detail(type, id))
                        },
                        onNavigateToPlayer = {
                            scope.launch { animatedPlayerOffset.animateTo(0f) }
                        }
                    )
                }

                // Search
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onNavigateToDetail = { type, id ->
                            navController.navigate(Routes.detail(type, id))
                        },
                        onNavigateToPlayer = {
                            scope.launch { animatedPlayerOffset.animateTo(0f) }
                        }
                    )
                }

                // Settings
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onNavigateToJellyfinLogin = {
                            navController.navigate(Routes.JELLYFIN_LOGIN)
                        }
                    )
                }

                // Jellyfin Login
                composable(Routes.JELLYFIN_LOGIN) {
                    JellyfinLoginScreen(
                        onLoginSuccess = { navController.popBackStack() },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Full-screen Player removed from NavHost (handled by Overlay)

                // Detail (Album/Artist/Playlist)
                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("id") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: ""
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    // TODO: DetailScreen
                }
            }
        }
        
        // Floating MiniPlayer (Overlay on top of Scaffold content)
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Gap above BottomNavBar (approx 80dp for navbar + 24dp gap)
                    .padding(bottom = 104.dp) 
                    .graphicsLayer {
                        alpha = miniPlayerAlpha
                        translationY = miniPlayerTranslationY
                    }
            ) {
                MiniPlayer(
                    onExpandPlayer = { scope.launch { animatedPlayerOffset.animateTo(0f) } },
                    onVerticalDrag = { delta -> onDrag(delta) },
                    onDragEnd = { onDragEnd() }
                )
            }
        }
        
        // Persistent Player Overlay
        // Always composed to maintain state (e.g. scroll position matches lyrics)
        // Moved off-screen when collapsed
        PlayerScreen(
            modifier = Modifier
                .offset { IntOffset(0, animatedPlayerOffset.value.roundToInt()) }
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { onDragEnd() },
                        onVerticalDrag = { _, dragAmount -> onDrag(dragAmount) }
                    )
                },
            onDismiss = { 
                scope.launch { 
                    animatedPlayerOffset.animateTo(screenHeightPx) 
                } 
            },
            onDrag = { delta -> onDrag(delta) } // Handle drag from nested scroll
        )
    }
}
