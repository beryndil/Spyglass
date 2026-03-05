package dev.spyglass.android.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import dev.spyglass.android.calculators.clock.ClockEngine
import dev.spyglass.android.calculators.clock.eventColor
import dev.spyglass.android.browse.BrowseScreen
import dev.spyglass.android.browse.search.SearchScreen
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.home.HomeScreen
import dev.spyglass.android.changelog.ChangelogScreen
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.QrScannerScreen
import dev.spyglass.android.connect.character.CharacterScreen
import dev.spyglass.android.connect.inventory.InventoryScreen
import dev.spyglass.android.connect.inventory.EnderChestScreen
import dev.spyglass.android.connect.chestfinder.ChestFinderScreen
import dev.spyglass.android.connect.map.MapScreen
import dev.spyglass.android.connect.statistics.StatisticsScreen
import dev.spyglass.android.connect.advancements.AdvancementsScreen
import dev.spyglass.android.disclaimer.DisclaimerScreen
import dev.spyglass.android.feedback.FeedbackScreen
import dev.spyglass.android.license.LicenseScreen
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.SettingsScreen
import dev.spyglass.android.settings.dataStore
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

sealed class TopDest(val route: String, val labelResId: Int, val icon: SpyglassIcon) {
    data object Home        : TopDest("home",        R.string.nav_home,   PixelIcons.Blocks)
    data object Browse      : TopDest("browse",      R.string.nav_browse, PixelIcons.Browse)
    data object Calculators : TopDest("calculators",  R.string.nav_tools,  PixelIcons.Anvil)
    data object Search      : TopDest("search",      R.string.nav_search, PixelIcons.Search)
}

val TOP_DESTINATIONS = listOf(TopDest.Home, TopDest.Browse, TopDest.Calculators, TopDest.Search)

private val SUB_ROUTES = setOf(
    "about", "settings", "changelog", "feedback", "license", "disclaimer",
    "connect_scan", "connect_character", "connect_inventory",
    "connect_enderchest", "connect_chestfinder", "connect_map",
    "connect_statistics", "connect_advancements",
)

/** Pending navigation target from Search -> Browse */
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
    val connectViewModel: ConnectViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBars = currentRoute !in SUB_ROUTES

    fun navigateTo(route: String) {
        try {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } catch (_: IllegalStateException) {
            // Back stack entry may not exist yet during rapid navigation — safe to ignore
        }
    }

    Scaffold(
        topBar    = { SpyglassTopBar(navController, onClockTap = {
            pendingCalcTab = 9
            navigateTo(TopDest.Calculators.route)
        }) },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                if (showBars) {
                    BottomNavBar(navController)
                }
                AdBanner()
            }
        },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = TopDest.Home.route,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(TopDest.Home.route) {
                HomeScreen(
                    onBrowseTarget = { target ->
                        pendingTarget = target
                        navigateTo(TopDest.Browse.route)
                    },
                    onCalcTab = { tab ->
                        pendingCalcTab = tab
                        navigateTo(TopDest.Calculators.route)
                    },
                    onSearch = { navigateTo(TopDest.Search.route) },
                    connectViewModel = connectViewModel,
                    onScanQr = {
                        navController.navigate("connect_scan") { launchSingleTop = true }
                    },
                    onConnectNav = { route ->
                        navController.navigate(route) { launchSingleTop = true }
                    },
                )
            }
            composable(TopDest.Calculators.route) {
                val calcTab = pendingCalcTab
                pendingCalcTab = null
                CalculatorsScreen(
                    initialTab = calcTab,
                    onBrowseTarget = { target ->
                        pendingTarget = target
                        navigateTo(TopDest.Browse.route)
                    },
                )
            }
            composable(TopDest.Browse.route) {
                val target = pendingTarget
                pendingTarget = null
                BrowseScreen(
                    initialTarget = target,
                    onCalcTab = { tab ->
                        pendingCalcTab = tab
                        navigateTo(TopDest.Calculators.route)
                    },
                )
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
            composable("about") {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onLicense = {
                        navController.navigate("license") { launchSingleTop = true }
                    },
                    onDisclaimer = {
                        navController.navigate("disclaimer") { launchSingleTop = true }
                    },
                )
            }
            composable("license") { LicenseScreen(onBack = { navController.popBackStack() }) }
            composable("disclaimer") { DisclaimerScreen(onBack = { navController.popBackStack() }) }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onCalcTab = { tab ->
                        pendingCalcTab = tab
                        navController.popBackStack()
                        navigateTo(TopDest.Calculators.route)
                    },
                    onAbout = {
                        navController.navigate("about") { launchSingleTop = true }
                    },
                    onFeedback = {
                        navController.navigate("feedback") { launchSingleTop = true }
                    },
                )
            }
            composable("changelog") { ChangelogScreen(onBack = { navController.popBackStack() }) }
            composable("feedback") { FeedbackScreen(onBack = { navController.popBackStack() }) }

            // ── Spyglass Connect — QR scanner (full-screen) ──
            composable("connect_scan") {
                QrScannerScreen(
                    onPairingDataScanned = { pairingData ->
                        connectViewModel.connectFromQr(pairingData)
                        if (navController.currentDestination?.route == "connect_scan") {
                            navController.popBackStack()
                        }
                    },
                    onBack = {
                        if (navController.currentDestination?.route == "connect_scan") {
                            navController.popBackStack()
                        }
                    },
                )
            }

            // ── Spyglass Connect — sub-screens ──
            composable("connect_character") {
                CharacterScreen(
                    viewModel = connectViewModel,
                    onBack = { navController.popBackStack() },
                    onBrowseTarget = { target ->
                        pendingTarget = target
                        navigateTo(TopDest.Browse.route)
                    },
                )
            }
            composable("connect_inventory") {
                InventoryScreen(
                    viewModel = connectViewModel,
                    onBack = { navController.popBackStack() },
                    onBrowseTarget = { target ->
                        pendingTarget = target
                        navigateTo(TopDest.Browse.route)
                    },
                )
            }
            composable("connect_enderchest") {
                EnderChestScreen(
                    viewModel = connectViewModel,
                    onBack = { navController.popBackStack() },
                    onBrowseTarget = { target ->
                        pendingTarget = target
                        navigateTo(TopDest.Browse.route)
                    },
                )
            }
            composable("connect_chestfinder") {
                ChestFinderScreen(
                    viewModel = connectViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("connect_map") {
                MapScreen(
                    viewModel = connectViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("connect_statistics") {
                StatisticsScreen(
                    viewModel = connectViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("connect_advancements") {
                AdvancementsScreen(
                    viewModel = connectViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

// -- Top bar --

@Composable
private fun SpyglassTopBar(navController: NavHostController, onClockTap: () -> Unit = {}) {
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
                text  = stringResource(R.string.top_bar_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            SpyglassIconImage(
                icon = SpyglassIcon.Drawable(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified,
            )
        }

        MiniGameClock(onTap = onClockTap)
        Spacer(Modifier.width(8.dp))

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                SpyglassIconImage(
                    icon = PixelIcons.Menu,
                    contentDescription = stringResource(R.string.menu),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.changelog)) },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("changelog") {
                            launchSingleTop = true
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feedback)) },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("feedback") {
                            launchSingleTop = true
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about)) },
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

// -- Bottom nav --

@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest   = backStackEntry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
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
                icon  = { SpyglassIconImage(dest.icon, contentDescription = stringResource(dest.labelResId), modifier = Modifier.size(24.dp)) },
                label = { Text(stringResource(dest.labelResId)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    unselectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                ),
            )
        }
    }
}

// -- Mini game clock --

private data class ClockPrefs(
    val enabled: Boolean = false,
    val syncTick: Long = -1L,
    val syncTimeMs: Long = 0L,
    val eventsJson: String? = null,
)

@Composable
private fun MiniGameClock(onTap: () -> Unit = {}) {
    val context = LocalContext.current
    val store = context.dataStore

    // Single DataStore collector — replaces 4 separate flows
    val prefs by remember {
        store.data.map { p ->
            ClockPrefs(
                enabled = p[PreferenceKeys.GAME_CLOCK_ENABLED] ?: false,
                syncTick = p[PreferenceKeys.CLOCK_TICK_OFFSET] ?: -1L,
                syncTimeMs = p[PreferenceKeys.CLOCK_SYNC_TIME_MS] ?: 0L,
                eventsJson = p[PreferenceKeys.CLOCK_ACTIVE_EVENTS],
            )
        }
    }.collectAsStateWithLifecycle(initialValue = ClockPrefs())

    if (!prefs.enabled || prefs.syncTick < 0) return

    val events = remember(prefs.eventsJson) {
        prefs.eventsJson?.let { json ->
            ClockEngine.deserializeEvents(json).sortedBy { it.tick }
        } ?: ClockEngine.PREDEFINED_EVENTS
            .filter { it.predefinedId in ClockEngine.DEFAULT_EVENT_IDS }
            .sortedBy { it.tick }
    }

    if (events.isEmpty()) return

    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(prefs.syncTick, prefs.syncTimeMs) {
        while (true) {
            tick = ClockEngine.currentTick(prefs.syncTick, prefs.syncTimeMs)
            delay(1000)
        }
    }

    val currentEvt = ClockEngine.currentEvent(tick, events)
    val nextPair = ClockEngine.nextEvent(tick, events)

    if (currentEvt == null || nextPair == null) return

    val (nextEvt, ticksAway) = nextPair
    val timeColor = if (ticksAway <= ClockEngine.COUNTDOWN_PREVIEW_TICKS) {
        eventColor(nextEvt.color)
    } else {
        eventColor(currentEvt.color)
    }

    val time = ClockEngine.formatTime(tick)
    val countdown = ClockEngine.formatCountdown(ticksAway)

    Text(
        text = "$time \u2022 $countdown",
        style = MaterialTheme.typography.labelSmall,
        color = timeColor,
        maxLines = 1,
        modifier = Modifier.clickable { onTap() },
    )
}
