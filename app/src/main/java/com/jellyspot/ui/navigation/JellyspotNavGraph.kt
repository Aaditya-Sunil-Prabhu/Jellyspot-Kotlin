package com.jellyspot.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jellyspot.ui.components.BottomNavBar
import com.jellyspot.ui.components.MiniPlayer

import com.jellyspot.ui.screens.auth.JellyfinLoginScreen

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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    // Mini Player above bottom nav
                    MiniPlayer(
                        onExpandPlayer = { navController.navigate(Routes.PLAYER) }
                    )
                    // Bottom navigation
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
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200))
            }
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
                        navController.navigate(Routes.PLAYER)
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
                        navController.navigate(Routes.PLAYER)
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
                        navController.navigate(Routes.PLAYER)
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

            // Full-screen Player
            composable(
                Routes.PLAYER,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(300)
                    )
                }
            ) {
                PlayerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

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
}
