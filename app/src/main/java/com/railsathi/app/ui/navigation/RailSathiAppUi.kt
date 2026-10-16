package com.railsathi.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.railsathi.app.ui.MainViewModel
import com.railsathi.app.ui.availability.AvailabilityRoute
import com.railsathi.app.ui.components.BannerTone
import com.railsathi.app.ui.components.InfoBanner
import com.railsathi.app.ui.favourites.FavouritesRoute
import com.railsathi.app.ui.history.HistoryRoute
import com.railsathi.app.ui.home.HomeRoute
import com.railsathi.app.ui.pnr.PnrRoute
import com.railsathi.app.ui.privacy.PrivacyScreen
import com.railsathi.app.ui.runningstatus.RunningStatusRoute

private data class TopLevelItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tag: String,
)

private val topLevelItems = listOf(
    TopLevelItem(Routes.HOME, "Home", Icons.Default.Home, "nav_home"),
    TopLevelItem(Routes.STATUS_PATTERN, "Status", Icons.Default.Place, "nav_status"),
    TopLevelItem(Routes.AVAILABILITY, "Seats", Icons.AutoMirrored.Filled.List, "nav_seats"),
    TopLevelItem(Routes.PNR, "PNR", Icons.Default.Info, "nav_pnr"),
    TopLevelItem(Routes.FAVOURITES, "Saved", Icons.Default.Favorite, "nav_saved"),
)

@Composable
fun RailSathiAppUi(mainViewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isOnline by mainViewModel.isOnline.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = topLevelItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val target = if (item.route == Routes.STATUS_PATTERN) Routes.status() else item.route
                                navController.navigate(target) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag(item.tag),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (!isOnline) {
                InfoBanner(
                    text = "You're offline. Saved data is shown where available.",
                    tone = BannerTone.WARNING,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (mainViewModel.isSampleMode) {
                InfoBanner(
                    text = "Sample data mode: nothing shown here is real railway data.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.weight(1f),
            ) {
                composable(Routes.HOME) {
                    HomeRoute(
                        onOpenStatus = { navController.navigate(Routes.status(it)) { launchSingleTop = true } },
                        onOpenAvailability = { navController.navigate(Routes.AVAILABILITY) { launchSingleTop = true } },
                        onOpenPnr = { navController.navigate(Routes.PNR) { launchSingleTop = true } },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) { launchSingleTop = true } },
                    )
                }
                composable(
                    route = Routes.STATUS_PATTERN,
                    arguments = listOf(
                        navArgument(Routes.STATUS_ARG) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    RunningStatusRoute(initialTrain = entry.arguments?.getString(Routes.STATUS_ARG))
                }
                composable(Routes.AVAILABILITY) { AvailabilityRoute() }
                composable(Routes.PNR) { PnrRoute() }
                composable(Routes.FAVOURITES) {
                    FavouritesRoute(
                        onOpenStatus = { navController.navigate(Routes.status(it)) { launchSingleTop = true } },
                        onOpenHistory = { navController.navigate(Routes.HISTORY) { launchSingleTop = true } },
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryRoute(
                        onBack = { navController.popBackStack() },
                        onOpenStatus = { navController.navigate(Routes.status(it)) { launchSingleTop = true } },
                    )
                }
                composable(Routes.PRIVACY) {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
