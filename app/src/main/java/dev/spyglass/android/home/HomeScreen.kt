package dev.spyglass.android.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.R
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.TodoEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Home preferences (single DataStore collector) ───────────────────────────

private data class HomePrefs(
    val showTipOfDay: Boolean = true,
    val showFavoritesOnHome: Boolean = false,
    val showExperimental: Boolean = true,
    val minecraftEdition: String = "java",
    val loaded: Boolean = false,
)

// ── Home screen ─────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onBrowseTarget: (dev.spyglass.android.navigation.BrowseTarget) -> Unit,
    onCalcTab: (Int) -> Unit,
    onSearch: () -> Unit,
    connectViewModel: ConnectViewModel? = null,
    onScanQr: () -> Unit = {},
    onConnectNav: (String) -> Unit = {},
    scrollToTopTrigger: Int = 0,
) {
    val onBrowseTab: (Int) -> Unit = { tab -> onBrowseTarget(dev.spyglass.android.navigation.BrowseTarget(tab, "")) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Single DataStore collector
    val prefs by remember {
        context.dataStore.data.map { p ->
            HomePrefs(
                showTipOfDay = p[PreferenceKeys.SHOW_TIP_OF_DAY] ?: true,
                showFavoritesOnHome = p[PreferenceKeys.SHOW_FAVORITES_ON_HOME] ?: false,
                showExperimental = p[PreferenceKeys.SHOW_EXPERIMENTAL] ?: false,
                minecraftEdition = p[PreferenceKeys.MINECRAFT_EDITION] ?: "java",
                loaded = true,
            )
        }
    }.collectAsStateWithLifecycle(initialValue = HomePrefs())

    // Load and filter tips by edition (async to avoid main thread I/O)
    val filteredTips by produceState(emptyList<Tip>(), prefs.minecraftEdition) {
        val allTips = TipsLoader.load(context)
        val edition = prefs.minecraftEdition
        value = allTips.filter { it.edition == "both" || it.edition == edition }
    }
    val tipStartIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }

    // Async repo access — avoids blocking main thread if database isn't ready yet
    val repo by produceState<GameDataRepository?>(null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            GameDataRepository.get(context)
        }
    }
    val favorites by produceState(emptyList<FavoriteEntity>(), repo) {
        repo?.allFavorites()?.collect { value = it } ?: return@produceState
    }
    val todoPreview by produceState(emptyList<TodoEntity>(), repo) {
        repo?.incompleteTodosPreview(3)?.collect { value = it } ?: return@produceState
    }
    val todoCount by produceState(0, repo) {
        repo?.incompleteTodoCount()?.collect { value = it } ?: return@produceState
    }

    val listState = rememberLazyListState()
    val reduceMotion = LocalReduceAnimations.current

    // Scroll to top when tab is re-tapped
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            if (reduceMotion) listState.scrollToItem(0)
            else listState.animateScrollToItem(0)
        }
    }

    // LazyColumn — only visible sections compose on first frame
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── A. Header / Branding ──
        item(key = "header") {
            HomeHeader()
        }

        // ── Search ──
        item(key = "search") {
            HomeSearchBar(onSearch = onSearch)
        }

        // ── Connect hub ──
        if (connectViewModel != null) {
            item(key = "connect") {
                HomeConnectSection(
                    connectViewModel = connectViewModel,
                    onScanQr = onScanQr,
                    onBrowseTarget = onBrowseTarget,
                    onConnectNav = onConnectNav,
                    onCalcTab = onCalcTab,
                )
            }
        }

        // ── B. Todo list ──
        item(key = "todo") {
            HomeTodoSection(
                todoCount = todoCount,
                todoPreview = todoPreview,
                onCalcTab = onCalcTab,
                onToggle = { todoId, completed ->
                    repo?.let { r -> scope.launch { r.toggleTodoCompleted(todoId, completed) } }
                },
            )
        }

        // ── B2. Favorites on Home ──
        if (prefs.showFavoritesOnHome && favorites.isNotEmpty()) {
            item(key = "favorites") {
                HomeFavoritesSection(
                    favorites = favorites,
                    onBrowseTab = onBrowseTab,
                )
            }
        }

        // ── C. Quick Access — Tools ──
        item(key = "tools") {
            val allCalc = allCalcLinks()
            val calcLinks = remember(prefs.showExperimental, allCalc) {
                if (prefs.showExperimental) allCalc else allCalc.filter { !it.experimental }
            }
            SectionHeader(stringResource(R.string.home_tools), icon = PixelIcons.Anvil)
            Spacer(Modifier.height(8.dp))
            QuickLinkGrid(calcLinks.map { it.link }) { index -> onCalcTab(calcLinks[index].tabIndex) }
        }

        // ── D. Tip of the Day ──
        if (prefs.showTipOfDay && filteredTips.isNotEmpty()) {
            item(key = "tip") {
                HomeTipSection(
                    tips = filteredTips,
                    startIndex = tipStartIndex % filteredTips.size,
                    repo = repo,
                    onDisable = {
                        scope.launch {
                            context.dataStore.edit { it[PreferenceKeys.SHOW_TIP_OF_DAY] = false }
                        }
                    },
                )
            }
        }

        // ── E. Quick Access — Browse ──
        item(key = "browse") {
            HomeBrowseSection(onBrowseTab = onBrowseTab)
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(8.dp))
        }
    }
}
