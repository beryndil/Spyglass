package dev.spyglass.android.core.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.spyglass.android.R
import dev.spyglass.android.calculators.clock.ClockEngine
import dev.spyglass.android.calculators.clock.eventColor
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.core.module.BottomNavItem
import dev.spyglass.android.core.module.HomeSectionScope
import dev.spyglass.android.core.module.ModuleNavActions
import dev.spyglass.android.core.module.ModuleRegistry
import dev.spyglass.android.core.module.ModuleRoute
import dev.spyglass.android.core.module.SettingsSectionScope
import dev.spyglass.android.core.ui.AdBanner
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.SpyglassIcon
import dev.spyglass.android.core.ui.SpyglassIconImage
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.navigation.BrowseTarget
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

// ── Top-level destination ───────────────────────────────────────────────────

data class ShellDestination(
    val route: String,
    val labelResId: Int,
    val icon: SpyglassIcon,
    val priority: Int,
)

@Composable
fun ShellNavGraph() {
    val context = LocalContext.current

    val defaultStartupTab by remember {
        context.dataStore.data.map { it[PreferenceKeys.DEFAULT_STARTUP_TAB] ?: 0 }
    }.collectAsStateWithLifecycle(initialValue = 0)

    // Re-read enabled modules whenever a module is toggled
    val revision by ModuleRegistry.revision.collectAsStateWithLifecycle()

    val enabledModules by produceState(ModuleRegistry.modules, revision) {
        value = ModuleRegistry.enabledModules(context)
    }

    val moduleBottomNavItems = remember(enabledModules) {
        enabledModules.flatMap { it.bottomNavItems() }.sortedBy { it.priority }
    }

    // Routes include ALL modules so nav graph is stable (avoids crashes when
    // navigating to a route that was removed mid-session).
    val moduleRoutes = remember {
        ModuleRegistry.modules.flatMap { it.navRoutes() }
    }

    // Build destinations: Home is always first, then module-contributed tabs, then Search
    val homeItem = remember { ShellDestination("home", R.string.nav_home, PixelIcons.Blocks, 0) }
    val searchItem = remember { ShellDestination("search", R.string.nav_search, PixelIcons.Search, 100) }

    val destinations = remember(moduleBottomNavItems) {
        buildList {
            add(homeItem)
            moduleBottomNavItems.forEach { item ->
                add(ShellDestination(item.route, item.labelResId, item.icon, item.priority))
            }
            add(searchItem)
        }.sortedBy { it.priority }
    }

    val startDest = remember(defaultStartupTab, destinations) {
        destinations.getOrNull(defaultStartupTab)?.route ?: "home"
    }

    val navController = remember {
        NavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
        }
    }

    var pendingTarget by remember { mutableStateOf<BrowseTarget?>(null) }
    var pendingCalcTab by remember { mutableStateOf<Int?>(null) }
    val connectViewModel: ConnectViewModel = viewModel()

    // Scroll-to-top trigger — increments when user re-taps the current tab
    var scrollToTopTrigger by remember { mutableIntStateOf(0) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Sub-routes don't show top/bottom bars
    val subRoutes = remember(moduleRoutes) {
        moduleRoutes.map { it.route }.toSet()
    }
    val showBars = currentRoute !in subRoutes

    // ── Navigation helpers ──────────────────────────────────────────────────

    fun navigateToTop(route: String) {
        try {
            if (currentRoute == route) {
                scrollToTopTrigger++
                return
            }
            if (route == "home") scrollToTopTrigger++
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } catch (_: IllegalStateException) { }
    }

    fun navigateToSub(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    // ── Scope implementations ───────────────────────────────────────────────

    val homeSectionScope = object : HomeSectionScope {
        override fun navigateTo(route: String) = navigateToSub(route)
        override fun navigateToBrowseTab(tab: Int, id: String) {
            pendingTarget = BrowseTarget(tab, id)
            navigateToTop("browse")
        }
        override fun navigateToCalcTab(tab: Int) {
            pendingCalcTab = tab
            navigateToTop("calculators")
        }
        override fun navigateToSearch() = navigateToTop("search")
        override fun navigateToScanQr() = navigateToSub("connect_scan")
    }

    val settingsSectionScope = object : SettingsSectionScope {
        override fun navigateTo(route: String) = navigateToSub(route)
        override fun navigateToCalcTab(tab: Int) {
            pendingCalcTab = tab
            navController.popBackStack()
            navigateToTop("calculators")
        }
    }

    val moduleNavActions = object : ModuleNavActions {
        override fun navigateTo(route: String) = navigateToSub(route)
        override fun navigateBack() { navController.popBackStack() }
        override fun navigateToBrowseTab(tab: Int, id: String) {
            pendingTarget = BrowseTarget(tab, id)
            navigateToTop("browse")
        }
        override fun navigateToCalcTab(tab: Int) {
            pendingCalcTab = tab
            navigateToTop("calculators")
        }
    }

    // ── Scaffold ────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            ShellTopBar(navController, onClockTap = {
                pendingCalcTab = 9
                navigateToTop("calculators")
            })
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                if (showBars) {
                    ShellBottomNavBar(navController, destinations) { route ->
                        navigateToTop(route)
                    }
                }
                AdBanner()
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Home
            composable("home") {
                ShellHomeScreen(homeSectionScope, scrollToTopTrigger)
            }

            // Search — provided by DatabaseModule or fallback
            composable("search") {
                dev.spyglass.android.browse.search.SearchScreen(onResultTap = { tab, id ->
                    pendingTarget = BrowseTarget(tab, id)
                    navController.navigate("browse") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                })
            }

            // Browse — from DatabaseModule
            composable("browse") {
                val target = pendingTarget
                pendingTarget = null
                dev.spyglass.android.browse.BrowseScreen(
                    initialTarget = target,
                    onCalcTab = { tab ->
                        pendingCalcTab = tab
                        navigateToTop("calculators")
                    },
                )
            }

            // Calculators — from ToolsModule
            composable("calculators") {
                val calcTab = pendingCalcTab
                pendingCalcTab = null
                dev.spyglass.android.calculators.CalculatorsScreen(
                    initialTab = calcTab,
                    onBrowseTarget = { target ->
                        pendingTarget = target
                        navigateToTop("browse")
                    },
                )
            }

            // Settings — shell settings
            composable("settings") {
                ShellSettingsScreen(
                    scope = settingsSectionScope,
                    onBack = { navController.popBackStack() },
                )
            }

            // All module sub-routes
            moduleRoutes.forEach { moduleRoute ->
                composable(moduleRoute.route) { entry ->
                    moduleRoute.content(entry, moduleNavActions)
                }
            }
        }
    }
}

// ── Top bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ShellTopBar(navController: NavHostController, onClockTap: () -> Unit = {}) {
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
                text = stringResource(R.string.top_bar_title),
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
                        navController.navigate("settings") { launchSingleTop = true }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.changelog)) },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("changelog") { launchSingleTop = true }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feedback)) },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("feedback") { launchSingleTop = true }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.news)) },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("news") { launchSingleTop = true }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.help)) },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("help") { launchSingleTop = true }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about)) },
                    onClick = {
                        menuExpanded = false
                        navController.navigate("about") { launchSingleTop = true }
                    },
                )
            }
        }
    }
}

// ── Bottom nav ──────────────────────────────────────────────────────────────

@Composable
private fun ShellBottomNavBar(
    navController: NavHostController,
    destinations: List<ShellDestination>,
    onNavigate: (String) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val hapticClick = rememberHapticClick()
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        destinations.forEach { dest ->
            NavigationBarItem(
                selected = currentDest?.hierarchy?.any { it.route == dest.route } == true,
                onClick = { hapticClick(); onNavigate(dest.route) },
                icon = {
                    SpyglassIconImage(
                        dest.icon,
                        contentDescription = stringResource(dest.labelResId),
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = { Text(stringResource(dest.labelResId)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    unselectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                ),
            )
        }
    }
}

// ── Mini game clock ─────────────────────────────────────────────────────────

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
    androidx.compose.runtime.LaunchedEffect(prefs.syncTick, prefs.syncTimeMs) {
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
