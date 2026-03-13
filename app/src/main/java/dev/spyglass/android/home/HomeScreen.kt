package dev.spyglass.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.spyglass.android.BuildConfig
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.data.db.entities.TodoEntity
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.runtime.mutableIntStateOf
import androidx.datastore.preferences.core.edit
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dev.spyglass.android.connect.*
import dev.spyglass.android.connect.client.ConnectionState
import dev.spyglass.android.connect.client.connectionStatusText
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ── Quick link data ─────────────────────────────────────────────────────────

private data class QuickLink(
    val icon: SpyglassIcon,
    val label: String,
    val iconTint: Color = Color.Unspecified,
)

// Pair: QuickLink to browse-tab index
@Composable
private fun browseLinks() = listOf(
    // ── Core Content ──
    QuickLink(PixelIcons.Blocks,    stringResource(R.string.home_link_blocks),       MaterialTheme.colorScheme.onSurfaceVariant) to 0,
    QuickLink(PixelIcons.Item,      stringResource(R.string.home_link_items),        MaterialTheme.colorScheme.primary)           to 1,
    QuickLink(PixelIcons.Crafting,  stringResource(R.string.home_link_recipes),      MaterialTheme.colorScheme.primary)           to 2,
    // ── World & Entities ──
    QuickLink(PixelIcons.Mob,       stringResource(R.string.home_link_mobs),         NetherRed)                                   to 3,
    QuickLink(PixelIcons.Biome,     stringResource(R.string.home_link_biomes),       Emerald)                                     to 5,
    QuickLink(PixelIcons.Structure, stringResource(R.string.home_link_structures),   MaterialTheme.colorScheme.primary)           to 6,
    // ── Game Mechanics ──
    QuickLink(PixelIcons.Trade,     stringResource(R.string.home_link_trades),       Emerald)                                     to 4,
    QuickLink(PixelIcons.Enchant,   stringResource(R.string.home_link_enchants),     EnderPurple)                                 to 7,
    QuickLink(PixelIcons.Potion,    stringResource(R.string.home_link_potions),      PotionBlue)                                  to 8,
    // ── Progress & Info ──
    QuickLink(PixelIcons.Advancement, stringResource(R.string.home_link_advancements), Emerald)                                         to 9,
    QuickLink(PixelIcons.Command,   stringResource(R.string.home_link_commands),     PotionBlue)                                  to 10,
    QuickLink(PixelIcons.Bookmark,  stringResource(R.string.home_link_reference),    MaterialTheme.colorScheme.primary)           to 11,
    QuickLink(PixelIcons.Clock,     stringResource(R.string.home_link_versions),     MaterialTheme.colorScheme.secondary)         to 12,
)

// Pair: QuickLink to calculator-tab index (allows visual reordering independent of tab order)
private data class CalcLink(
    val link: QuickLink,
    val tabIndex: Int,
    val experimental: Boolean = false,
)

@Composable
private fun allCalcLinks() = listOf(
    // ── Planning & Organization ──
    CalcLink(QuickLink(PixelIcons.Todo,      stringResource(R.string.home_link_todo_list)),              0),
    CalcLink(QuickLink(PixelIcons.Storage,   stringResource(R.string.home_link_shopping_lists)),         1),
    CalcLink(QuickLink(PixelIcons.Bookmark,  stringResource(R.string.home_link_notes)),                  11),
    CalcLink(QuickLink(PixelIcons.Waypoints, stringResource(R.string.home_link_waypoints),    Emerald), 12),
    // ── Building & Design ──
    CalcLink(QuickLink(PixelIcons.Fill,      stringResource(R.string.home_link_block_fill)),             3),
    CalcLink(QuickLink(PixelIcons.Shapes,    stringResource(R.string.home_link_shapes)),                 4),
    CalcLink(QuickLink(PixelIcons.Maze,      stringResource(R.string.home_link_maze_maker)),             5),
    CalcLink(QuickLink(PixelIcons.Storage,   stringResource(R.string.home_link_storage)),                6),
    // ── Crafting & Resources ──
    CalcLink(QuickLink(PixelIcons.Anvil,     stringResource(R.string.home_link_enchanting)),             2),
    CalcLink(QuickLink(PixelIcons.Smelt,     stringResource(R.string.home_link_smelting)),               7),
    // ── Reference ──
    CalcLink(QuickLink(PixelIcons.Enchant,   stringResource(R.string.home_link_librarian_guide)),       14, experimental = true),
    CalcLink(QuickLink(PixelIcons.Torch,     stringResource(R.string.home_link_light_spacing)),          10),
    // ── World & Navigation ──
    CalcLink(QuickLink(PixelIcons.Nether,    stringResource(R.string.home_link_nether_portal)),          8),
    CalcLink(QuickLink(PixelIcons.Clock,     stringResource(R.string.home_link_game_clock)),             9),
)

// ── Browse tab index for favorite types ─────────────────────────────────────

private fun browseTabForType(type: String): Int = when (type) {
    "block"     -> 0
    "item"      -> 1
    "recipe"    -> 2
    "mob"       -> 3
    "trade"     -> 4
    "biome"     -> 5
    "structure" -> 6
    "enchant"   -> 7
    "potion"      -> 8
    "advancement" -> 9
    "command"     -> 10
    else          -> 0
}

private fun iconForFavorite(type: String, id: String): SpyglassIcon = when (type) {
    "block"     -> ItemTextures.get(id) ?: PixelIcons.Blocks
    "item"      -> ItemTextures.get(id) ?: PixelIcons.Item
    "recipe"    -> ItemTextures.get(id) ?: PixelIcons.Crafting
    "mob"       -> MobTextures.get(id) ?: PixelIcons.Mob
    "trade"     -> PixelIcons.Trade
    "biome"     -> BiomeTextures.get(id) ?: PixelIcons.Biome
    "structure" -> StructureTextures.get(id) ?: PixelIcons.Structure
    "enchant"   -> EnchantTextures.get(id) ?: PixelIcons.Enchant
    "potion"    -> PotionTextures.get(id) ?: PixelIcons.Potion
    else        -> PixelIcons.Bookmark
}

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

// ── Update check ────────────────────────────────────────────────────────────

private fun checkForUpdate(): Boolean? {
    return try {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("https://api.github.com/repos/beryndil/Spyglass/releases/latest")
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            // Parse tag_name from JSON (e.g. "FireHorse.0304.1430-a")
            val zodiacYears = mapOf(
                "WoodDragon" to 2024, "WoodSnake" to 2025,
                "FireHorse" to 2026, "FireGoat" to 2027,
                "EarthMonkey" to 2028, "EarthRooster" to 2029,
                "MetalDog" to 2030, "MetalPig" to 2031,
                "WaterRat" to 2032, "WaterOx" to 2033,
                "WoodTiger" to 2034, "WoodRabbit" to 2035,
            )
            val tagMatch = Regex(""""tag_name"\s*:\s*"([A-Za-z]+)\.(\d{2})(\d{2})\.(\d{2})(\d{2})-a"""")
                .find(body) ?: return null
            val year = (zodiacYears[tagMatch.groupValues[1]] ?: return null) - 2000
            val month = tagMatch.groupValues[2].toInt()
            val day = tagMatch.groupValues[3].toInt()
            val hour = tagMatch.groupValues[4].toInt()
            val minute = tagMatch.groupValues[5].toInt()
            val remoteCode = year * 10_000_000 + month * 1_000_000 +
                day * 10_000 + hour * 100 + minute
            remoteCode > BuildConfig.VERSION_CODE
        }
    } catch (e: Exception) {
        Timber.d(e, "Update check failed")
        null
    }
}

// ── Extracted section composables ───────────────────────────────────────────

@Composable
private fun HomeHeader() {
    val updateAvailable by produceState<Boolean?>(null) {
        value = kotlinx.coroutines.withContext(Dispatchers.IO) { checkForUpdate() }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpyglassIconImage(
            SpyglassIcon.Drawable(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(144.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_welcome),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (updateAvailable == true) {
            val uriHandler = LocalUriHandler.current
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_update_available),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFD32F2F),
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://play.google.com/store/apps/details?id=dev.spyglass.android")
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeSearchBar(onSearch: () -> Unit) {
    val hapticClick = rememberHapticClick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { hapticClick(); onSearch() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        SpyglassIconImage(PixelIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.search), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HomeTodoSection(
    todoCount: Int,
    todoPreview: List<TodoEntity>,
    onCalcTab: (Int) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
) {
    SectionHeader(stringResource(R.string.home_todo), icon = PixelIcons.Todo)
    Spacer(Modifier.height(8.dp))
    if (todoCount > 0) {
        ResultCard {
            todoPreview.forEach { todo ->
                HomeTodoRow(
                    todo = todo,
                    onToggle = { onToggle(todo.id, !todo.completed) },
                )
            }
            if (todoCount > 3) {
                SpyglassDivider()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCalcTab(0) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (todoCount > 3) stringResource(R.string.home_todo_view_all, todoCount) else stringResource(R.string.home_todo_edit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            }
        }
    } else {
        ResultCard(
            modifier = Modifier.clickable { onCalcTab(0) },
        ) {
            Text(
                stringResource(R.string.home_todo_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.home_todo_open),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun HomeFavoritesSection(
    favorites: List<FavoriteEntity>,
    onBrowseTab: (Int) -> Unit,
) {
    SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
    Spacer(Modifier.height(8.dp))
    favorites.forEach { fav ->
        BrowseListItem(
            headline = fav.displayName,
            supporting = fav.type,
            leadingIcon = iconForFavorite(fav.type, fav.id),
            modifier = Modifier.clickable {
                onBrowseTab(browseTabForType(fav.type))
            },
        )
    }
}

@Composable
private fun HomeTipSection(tips: List<Tip>, startIndex: Int, repo: GameDataRepository?, onDisable: () -> Unit) {
    var tipIndex by remember { mutableIntStateOf(startIndex) }
    var menuExpanded by remember { mutableStateOf(false) }
    val currentTip = tips[tipIndex]
    val tipText = if (repo != null) {
        translatedText(repo, "tip", currentTip.id.toString(), "text", currentTip.text)
    } else currentTip.text

    ResultCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_did_you_know), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_tip_options), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_tip_disable)) },
                        onClick = {
                            menuExpanded = false
                            onDisable()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tipText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { tipIndex = Math.floorMod(tipIndex - 1, tips.size) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.home_tip_previous), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { tipIndex = (tipIndex + 1) % tips.size },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.home_tip_next), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun HomeBrowseSection(onBrowseTab: (Int) -> Unit) {
    SectionHeader(stringResource(R.string.home_browse), icon = PixelIcons.Browse)
    Spacer(Modifier.height(8.dp))
    browseLinks().let { links ->
        QuickLinkGrid(links.map { it.first }) { index -> onBrowseTab(links[index].second) }
    }
}

// ── Quick link grid — 2 columns ─────────────────────────────────────────────

@Composable
private fun QuickLinkGrid(links: List<QuickLink>, onTap: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Left column: even indices (0, 2, 4, ...)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            links.forEachIndexed { i, link ->
                if (i % 2 == 0) QuickLinkCard(link) { onTap(i) }
            }
        }
        // Right column: odd indices (1, 3, 5, ...)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            links.forEachIndexed { i, link ->
                if (i % 2 != 0) QuickLinkCard(link) { onTap(i) }
            }
        }
    }
}

@Composable
private fun QuickLinkCard(link: QuickLink, onClick: () -> Unit) {
    val hapticClick = rememberHapticClick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalSurfaceCard.current, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { hapticClick(); onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpyglassIconImage(link.icon, contentDescription = null, tint = if (link.iconTint == Color.Unspecified) MaterialTheme.colorScheme.primary else link.iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(link.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

// ── Home todo row (interactive) ─────────────────────────────────────────────

@Composable
private fun HomeTodoRow(todo: TodoEntity, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
    ) {
        Checkbox(
            checked = todo.completed,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.secondary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        if (todo.itemId != null) {
            val tex = ItemTextures.get(todo.itemId)
            if (tex != null) {
                SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
        }
        Text(
            todo.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (todo.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Spyglass Connect hub ─────────────────────────────────────────────────────

@Composable
private fun connectLinks(
    playerSkin: android.graphics.Bitmap?,
    playerCount: Int = 1,
): List<Pair<QuickLink, String>> {
    val characterIcon: SpyglassIcon = if (playerSkin != null) {
        SpyglassIcon.BitmapIcon(playerSkin)
    } else {
        PixelIcons.Steve
    }
    return buildList {
        add(QuickLink(characterIcon,          stringResource(R.string.home_connect_link_character))    to "connect_character")
        add(QuickLink(PixelIcons.Backpack,    stringResource(R.string.home_connect_link_inventory))     to "connect_inventory")
        add(QuickLink(PixelIcons.EnderChest,  stringResource(R.string.home_connect_link_ender_chest))   to "connect_enderchest")
        add(QuickLink(PixelIcons.Storage,     stringResource(R.string.home_connect_link_storage))       to "connect_chestfinder")
        add(QuickLink(PixelIcons.Biome,       stringResource(R.string.home_connect_link_world_map))     to "connect_map")
        add(QuickLink(PixelIcons.Waypoints,   stringResource(R.string.home_connect_link_waypoints))     to "connect_waypoints")
        val wolfIcon = MobTextures.get("wolf") ?: PixelIcons.Mob
        add(QuickLink(wolfIcon,               stringResource(R.string.home_connect_link_pets))          to "connect_pets")
        if (playerCount > 1) {
            add(QuickLink(PixelIcons.Steve,   stringResource(R.string.home_connect_link_players))       to "connect_players")
        }
        add(QuickLink(PixelIcons.Anvil,       stringResource(R.string.home_connect_link_statistics))    to "connect_statistics")
        add(QuickLink(PixelIcons.Advancement, stringResource(R.string.home_connect_link_advancements))       to "tracker")
    }
}

@Composable
private fun HomeConnectSection(
    connectViewModel: ConnectViewModel,
    onScanQr: () -> Unit,
    onBrowseTarget: (dev.spyglass.android.navigation.BrowseTarget) -> Unit = {},
    onConnectNav: (String) -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
) {
    val state by connectViewModel.connectionState.collectAsStateWithLifecycle()
    val worlds by connectViewModel.worlds.collectAsStateWithLifecycle()
    val selectedWorld by connectViewModel.selectedWorld.collectAsStateWithLifecycle()
    val playerSkin by connectViewModel.playerSkin.collectAsStateWithLifecycle()
    val playerList by connectViewModel.playerList.collectAsStateWithLifecycle()
    val links = connectLinks(playerSkin, playerList.size)

    val hasCachedData = selectedWorld != null

    when {
        // ── Connected, no world selected ──
        state.isConnected && selectedWorld == null -> {
            SectionHeader(stringResource(R.string.home_connect_title), icon = PixelIcons.Waypoints)
            Spacer(Modifier.height(8.dp))
            ConnectWorldSelector(
                worlds = worlds,
                selectedWorld = null,
                onSelectWorld = {
                    connectViewModel.selectWorld(it)
                    connectViewModel.requestPlayerData()
                },
                onDisconnect = { connectViewModel.disconnect() },
            )
        }

        // ── Has cached data (any connection state) — data-first ──
        hasCachedData -> {
            QuickLinkGrid(links.map { it.first }) { index ->
                val route = links[index].second
                if (route == "tracker") {
                    onCalcTab(19)
                } else {
                    onConnectNav(route)
                }
            }
            Spacer(Modifier.height(6.dp))
            ConnectStatusLine(
                state = state,
                worlds = worlds,
                selectedWorld = selectedWorld,
                onSelectWorld = {
                    connectViewModel.selectWorld(it)
                    connectViewModel.requestPlayerData()
                },
                onDisconnect = { connectViewModel.disconnect() },
                onScanQr = onScanQr,
                onReconnect = { connectViewModel.tryReconnect() },
                onClearData = { connectViewModel.clearCachedData() },
            )
        }

        // ── No cached data (any state) — show pairing card or connecting spinner ──
        else -> {
            SectionHeader(stringResource(R.string.home_connect_title), icon = PixelIcons.Waypoints)
            Spacer(Modifier.height(8.dp))
            if (!state.isConnected && state !is ConnectionState.Disconnected && state !is ConnectionState.Error) {
                // Actively connecting/pairing — show spinner
                ResultCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            connectionStatusText(state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                ConnectDisconnectedCard(
                    state = state,
                    onScanQr = onScanQr,
                    onReconnect = { connectViewModel.tryReconnect() },
                    showReconnect = false,
                )
            }
        }
    }
}

@Composable
private fun ConnectDisconnectedCard(
    state: ConnectionState,
    onScanQr: () -> Unit,
    onReconnect: () -> Unit,
    showReconnect: Boolean = true,
) {
    ResultCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpyglassIconImage(
                PixelIcons.Waypoints,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_connect_stream),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.home_connect_wifi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onScanQr,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(stringResource(R.string.home_connect_scan_qr))
        }
        if (showReconnect) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onReconnect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_connect_reconnect_last))
            }
        }

        if (state is ConnectionState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                state.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFF44336),
            )
        }
    }
}

@Composable
private fun ConnectStatusLine(
    state: ConnectionState,
    worlds: List<WorldInfo>,
    selectedWorld: String?,
    onSelectWorld: (String) -> Unit,
    onDisconnect: () -> Unit,
    onScanQr: () -> Unit,
    onReconnect: () -> Unit,
    onClearData: () -> Unit = {},
) {
    val currentWorld = worlds.firstOrNull { it.folderName == selectedWorld }
    var worldMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val isInProgress = state is ConnectionState.Reconnecting ||
        state is ConnectionState.Connecting || state is ConnectionState.Pairing
    val isDisconnected = state is ConnectionState.Disconnected || state is ConnectionState.Error

    val connectedLabel = stringResource(R.string.home_connect_status_connected)
    val reconnectingLabel = stringResource(R.string.home_connect_status_reconnecting)
    val connectingLabel = stringResource(R.string.home_connect_status_connecting)
    val reconnectLabel = stringResource(R.string.home_connect_status_reconnect)

    val (statusColor, statusLabel) = when {
        state.isConnected -> Emerald to connectedLabel
        isInProgress -> Color(0xFFFFC107) to if (state is ConnectionState.Reconnecting) reconnectingLabel else connectingLabel
        else -> Color(0xFFF44336) to reconnectLabel
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(stringResource(R.string.home_connect_clear_data_title)) },
            text = { Text(stringResource(R.string.home_connect_clear_data_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataDialog = false
                    onClearData()
                }) { Text(stringResource(R.string.home_connect_clear_data_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Left side: globe icon + world name (clickable to switch worlds)
        if (currentWorld != null) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = if (state.isConnected && worlds.size > 1) {
                        Modifier.clickable { worldMenuExpanded = true }
                    } else {
                        Modifier
                    },
                ) {
                    SpyglassIconImage(
                        PixelIcons.Globe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        currentWorld.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // World switcher dropdown anchored to world name
                if (worldMenuExpanded && worlds.size > 1) {
                    DropdownMenu(
                        expanded = worldMenuExpanded,
                        onDismissRequest = { worldMenuExpanded = false },
                    ) {
                        worlds.forEach { world ->
                            DropdownMenuItem(
                                text = { Text(world.displayName) },
                                onClick = {
                                    worldMenuExpanded = false
                                    onSelectWorld(world.folderName)
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Right side: tappable status indicator with dropdown menu
        Box {
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                modifier = Modifier.clickable { statusMenuExpanded = true },
            )
            DropdownMenu(
                expanded = statusMenuExpanded,
                onDismissRequest = { statusMenuExpanded = false },
            ) {
                if (state.isConnected) {
                    if (worlds.size > 1) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_connect_switch_world)) },
                            onClick = {
                                statusMenuExpanded = false
                                worldMenuExpanded = true
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_connect_disconnect)) },
                        onClick = {
                            statusMenuExpanded = false
                            onDisconnect()
                        },
                    )
                } else if (isDisconnected) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_connect_status_reconnect)) },
                        onClick = {
                            statusMenuExpanded = false
                            onReconnect()
                        },
                    )
                } else if (isInProgress) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cancel)) },
                        onClick = {
                            statusMenuExpanded = false
                            onDisconnect()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_connect_scan_new_device)) },
                    onClick = {
                        statusMenuExpanded = false
                        onScanQr()
                    },
                )
                SpyglassDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_connect_clear_data_title), color = Color(0xFFF44336)) },
                    onClick = {
                        statusMenuExpanded = false
                        showClearDataDialog = true
                    },
                )
            }
        }
    }
}

@Composable
private fun ConnectWorldSelector(
    worlds: List<WorldInfo>,
    selectedWorld: String?,
    onSelectWorld: (String) -> Unit,
    onDisconnect: () -> Unit,
) {
    ResultCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.home_connect_select_world),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.home_connect_disconnect),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFF44336),
                modifier = Modifier.clickable { onDisconnect() },
            )
        }
        Spacer(Modifier.height(8.dp))
        worlds.forEach { world ->
            val isModded = world.isModded
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectWorld(world.folderName) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SpyglassIconImage(
                    PixelIcons.Globe,
                    contentDescription = null,
                    tint = if (isModded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        world.displayName + if (isModded) stringResource(R.string.home_connect_modded_suffix) else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isModded) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    )
                    Text(
                        stringResource(R.string.home_connect_game_mode_difficulty, world.gameMode.replaceFirstChar { it.uppercase() }, world.difficulty.replaceFirstChar { it.uppercase() }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (worlds.isEmpty()) {
            Text(
                stringResource(R.string.home_connect_no_worlds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}



