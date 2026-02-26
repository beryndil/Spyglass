package dev.spyglass.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.spyglass.android.core.ui.Background
import dev.spyglass.android.core.ui.Gold
import dev.spyglass.android.core.ui.GoldDim
import dev.spyglass.android.core.ui.Stone500

sealed class TopDest(val route: String, val label: String, val icon: ImageVector) {
    data object Calculators : TopDest("calculators", "Calculators", Icons.Default.Calculate)
    data object Browse      : TopDest("browse",      "Browse",      Icons.Default.Inventory2)
    data object Search      : TopDest("search",      "Search",      Icons.Default.Search)
}

val TOP_DESTINATIONS = listOf(TopDest.Calculators, TopDest.Browse, TopDest.Search)

@Composable
fun AppNavGraph() {
    val navController: NavHostController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = TopDest.Calculators.route,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(TopDest.Calculators.route) { CalculatorsScreen() }
            composable(TopDest.Browse.route)      { BrowseScreen() }
            composable(TopDest.Search.route)      { SearchScreen() }
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
                icon  = { Icon(dest.icon, contentDescription = dest.label) },
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
