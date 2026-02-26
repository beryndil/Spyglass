package dev.spyglass.android.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.spyglass.android.calculators.CalculatorsScreen
import dev.spyglass.android.browse.BrowseScreen
import dev.spyglass.android.browse.search.SearchScreen
import dev.spyglass.android.core.ui.*

sealed class TopDest(val route: String, val label: String, val icon: SpyglassIcon) {
    data object Calculators : TopDest("calculators", "Calculators", PixelIcons.Calculator)
    data object Browse      : TopDest("browse",      "Browse",      PixelIcons.Browse)
    data object Search      : TopDest("search",      "Search",      PixelIcons.Search)
}

val TOP_DESTINATIONS = listOf(TopDest.Calculators, TopDest.Browse, TopDest.Search)

/** Pending navigation target from Search → Browse */
data class BrowseTarget(val tab: Int, val id: String)

@Composable
fun AppNavGraph() {
    val navController: NavHostController = rememberNavController()
    var pendingTarget by remember { mutableStateOf<BrowseTarget?>(null) }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = TopDest.Calculators.route,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(TopDest.Calculators.route) { CalculatorsScreen() }
            composable(TopDest.Browse.route) {
                val target = pendingTarget
                pendingTarget = null
                BrowseScreen(initialTarget = target)
            }
            composable(TopDest.Search.route) {
                SearchScreen(onResultTap = { tab, id ->
                    pendingTarget = BrowseTarget(tab, id)
                    navController.navigate(TopDest.Browse.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                })
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest   = backStackEntry?.destination

    NavigationBar(containerColor = Background) {
        TOP_DESTINATIONS.forEach { dest ->
            NavigationBarItem(
                selected = currentDest?.hierarchy?.any { it.route == dest.route } == true,
                onClick  = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { SpyglassIconImage(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Gold,
                    selectedTextColor   = Gold,
                    unselectedIconColor = Stone500,
                    unselectedTextColor = Stone500,
                    indicatorColor      = GoldDim.copy(alpha = 0.15f),
                ),
            )
        }
    }
}
