package dev.spyglass.android.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.spyglass.android.about.AboutScreen
import dev.spyglass.android.calculators.CalculatorsScreen
import dev.spyglass.android.browse.BrowseScreen
import dev.spyglass.android.browse.search.SearchScreen
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.home.HomeScreen
import dev.spyglass.android.settings.SettingsScreen

sealed class TopDest(val route: String, val label: String, val icon: SpyglassIcon) {
    data object Home        : TopDest("home",        "Home",        PixelIcons.Bookmark)
    data object Browse      : TopDest("browse",      "Browse",      PixelIcons.Browse)
    data object Calculators : TopDest("calculators", "Calculators", PixelIcons.Calculator)
    data object Search      : TopDest("search",      "Search",      PixelIcons.Search)
}

val TOP_DESTINATIONS = listOf(TopDest.Home, TopDest.Browse, TopDest.Calculators, TopDest.Search)

private val SUB_ROUTES = setOf("about", "settings")

/** Pending navigation target from Search → Browse */
data class BrowseTarget(val tab: Int, val id: String)

@Composable
fun AppNavGraph() {
    val context = LocalContext.current
    val navController = remember {
        NavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
        }
    }
    var pendingTarget by remember { mutableStateOf<BrowseTarget?>(null) }
    var pendingCalcTab by remember { mutableStateOf<Int?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBars = currentRoute !in SUB_ROUTES

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        topBar    = { SpyglassTopBar(navController) },
        bottomBar = { if (showBars) BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = TopDest.Home.route,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(TopDest.Home.route) {
                HomeScreen(
                    onBrowseTab = { tab ->
                        pendingTarget = BrowseTarget(tab, "")
                        navigateTo(TopDest.Browse.route)
                    },
                    onCalcTab = { tab ->
                        pendingCalcTab = tab
                        navigateTo(TopDest.Calculators.route)
                    },
                    onSearch = { navigateTo(TopDest.Search.route) },
                )
            }
            composable(TopDest.Calculators.route) {
                val calcTab = pendingCalcTab
                pendingCalcTab = null
                CalculatorsScreen(initialTab = calcTab)
            }
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
            composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun SpyglassTopBar(navController: NavHostController) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "SPYGLASS",
                style = MaterialTheme.typography.labelSmall,
                color = Gold,
            )
            Spacer(Modifier.width(6.dp))
            SpyglassIconImage(
                icon = SpyglassIcon.Drawable(dev.spyglass.android.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified,
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                SpyglassIconImage(
                    icon = PixelIcons.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("about") {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

// ── Bottom nav ───────────────────────────────────────────────────────────────

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
