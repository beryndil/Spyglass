package dev.spyglass.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import dev.spyglass.android.connect.*
import dev.spyglass.android.connect.client.ConnectionState
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ── Minecraft tips / Did You Know ───────────────────────────────────────────
// Tips are loaded from string-array resource for localization support

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
    QuickLink(PixelIcons.Blocks,    "Blocks",       MaterialTheme.colorScheme.onSurfaceVariant) to 0,
    QuickLink(PixelIcons.Item,      "Items",        MaterialTheme.colorScheme.primary)           to 1,
    QuickLink(PixelIcons.Crafting,  "Recipes",      MaterialTheme.colorScheme.primary)           to 2,
    // ── World & Entities ──
    QuickLink(PixelIcons.Mob,       "Mobs",         NetherRed)                                   to 3,
    QuickLink(PixelIcons.Biome,     "Biomes",       Emerald)                                     to 5,
    QuickLink(PixelIcons.Structure, "Structures",   MaterialTheme.colorScheme.primary)           to 6,
    // ── Game Mechanics ──
    QuickLink(PixelIcons.Trade,     "Trades",       Emerald)                                     to 4,
    QuickLink(PixelIcons.Enchant,   "Enchants",     EnderPurple)                                 to 7,
    QuickLink(PixelIcons.Potion,    "Potions",      PotionBlue)                                  to 8,
    // ── Progress & Info ──
    QuickLink(PixelIcons.Advancement, "Advancements", Emerald)                                   to 9,
    QuickLink(PixelIcons.Command,   "Commands",     PotionBlue)                                  to 10,
    QuickLink(PixelIcons.Bookmark,  "Reference",    MaterialTheme.colorScheme.primary)           to 11,
    QuickLink(PixelIcons.Clock,     "Versions",     MaterialTheme.colorScheme.secondary)         to 12,
)

// Pair: QuickLink to calculator-tab index (allows visual reordering independent of tab order)
private val CALC_LINKS = listOf(
    // ── Planning & Organization ──
    QuickLink(PixelIcons.Todo,      "Todo List")              to 0,
    QuickLink(PixelIcons.Storage,   "Shopping Lists")         to 1,
    QuickLink(PixelIcons.Bookmark,  "Notes")                  to 11,
    QuickLink(PixelIcons.Waypoints, "Waypoints",    Emerald)  to 12,
    // ── Building & Design ──
    QuickLink(PixelIcons.Fill,      "Block Fill")             to 3,
    QuickLink(PixelIcons.Shapes,    "Shapes")                 to 4,
    QuickLink(PixelIcons.Maze,      "Maze Maker")             to 5,
    QuickLink(PixelIcons.Storage,   "Storage")                to 6,
    // ── Crafting & Resources ──
    QuickLink(PixelIcons.Anvil,     "Enchanting")             to 2,
    QuickLink(PixelIcons.Smelt,     "Smelting")               to 7,
    // ── Reference ──
    QuickLink(PixelIcons.Enchant,   "Librarian Guide")        to 14,
    QuickLink(PixelIcons.Torch,     "Light Spacing")          to 10,
    // ── World & Navigation ──
    QuickLink(PixelIcons.Nether,    "Nether Portal")          to 8,
    QuickLink(PixelIcons.Clock,     "Game Clock")             to 9,
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
) {
    val onBrowseTab: (Int) -> Unit = { tab -> onBrowseTarget(dev.spyglass.android.navigation.BrowseTarget(tab, "")) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tips = stringArrayResource(R.array.tips)
    val tipIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % tips.size }

    // Single DataStore collector
    val prefs by remember {
        context.dataStore.data.map { p ->
            HomePrefs(
                showTipOfDay = p[PreferenceKeys.SHOW_TIP_OF_DAY] ?: true,
                showFavoritesOnHome = p[PreferenceKeys.SHOW_FAVORITES_ON_HOME] ?: false,
                loaded = true,
            )
        }
    }.collectAsStateWithLifecycle(initialValue = HomePrefs())

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

    // LazyColumn — only visible sections compose on first frame
    LazyColumn(
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
            SectionHeader(stringResource(R.string.home_tools), icon = PixelIcons.Anvil)
            Spacer(Modifier.height(8.dp))
            QuickLinkGrid(CALC_LINKS.map { it.first }) { index -> onCalcTab(CALC_LINKS[index].second) }
        }

        // ── D. Tip of the Day ──
        if (prefs.showTipOfDay) {
            item(key = "tip") {
                HomeTipSection(tip = tips[tipIndex])
            }
        }

        // ── E. Quick Access — Browse ──
        item(key = "browse") {
            HomeBrowseSection(onBrowseTab = onBrowseTab)
        }

        // ── F. News ──
        item(key = "news") {
            HomeNewsSection()
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
            .url("https://api.github.com/repos/Dev-VulX/Spyglass/releases/latest")
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            // Parse tag_name from JSON (e.g. "v2026.0304.1430")
            val tagMatch = Regex(""""tag_name"\s*:\s*"v?(\d{4})\.(\d{2})(\d{2})\.(\d{2})(\d{2})"""")
                .find(body) ?: return null
            val year = tagMatch.groupValues[1].toInt() - 2000
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
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_update_available),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFD32F2F),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onSearch() }
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
private fun HomeTipSection(tip: String) {
    ResultCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_did_you_know), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tip,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalSurfaceCard.current, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpyglassIconImage(link.icon, contentDescription = null, tint = if (link.iconTint == Color.Unspecified) MaterialTheme.colorScheme.primary else link.iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(link.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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

private val CONNECT_LINKS = listOf(
    QuickLink(PixelIcons.Steve,       "Character")    to "connect_character",
    QuickLink(PixelIcons.Item,        "Inventory")     to "connect_inventory",
    QuickLink(PixelIcons.Enchant,     "Ender Chest")   to "connect_enderchest",
    QuickLink(PixelIcons.Search,      "Chest Finder")  to "connect_chestfinder",
    QuickLink(PixelIcons.Biome,       "World Map")     to "connect_map",
    QuickLink(PixelIcons.Anvil,       "Statistics")    to "connect_statistics",
    QuickLink(PixelIcons.Advancement, "Advancements")  to "connect_advancements",
)

@Composable
private fun HomeConnectSection(
    connectViewModel: ConnectViewModel,
    onScanQr: () -> Unit,
    onBrowseTarget: (dev.spyglass.android.navigation.BrowseTarget) -> Unit = {},
    onConnectNav: (String) -> Unit = {},
) {
    val state by connectViewModel.connectionState.collectAsStateWithLifecycle()
    val worlds by connectViewModel.worlds.collectAsStateWithLifecycle()
    val selectedWorld by connectViewModel.selectedWorld.collectAsStateWithLifecycle()

    SectionHeader("Spyglass Connect", icon = PixelIcons.Waypoints)
    Spacer(Modifier.height(8.dp))

    val hasCachedData = selectedWorld != null

    when {
        // ── Disconnected / Error ──
        state is ConnectionState.Disconnected || state is ConnectionState.Error -> {
            ConnectDisconnectedCard(
                state = state,
                onScanQr = onScanQr,
                onReconnect = { connectViewModel.tryReconnect() },
            )
            // Show quick links for cached offline data
            if (hasCachedData) {
                Spacer(Modifier.height(8.dp))
                QuickLinkGrid(CONNECT_LINKS.map { it.first }) { index ->
                    onConnectNav(CONNECT_LINKS[index].second)
                }
            }
        }

        // ── Connecting / Pairing / Reconnecting ──
        !state.isConnected -> {
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
                        state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // ── Connected, no world selected ──
        state.isConnected && selectedWorld == null -> {
            ConnectStatusBar(state)
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

        // ── Connected, world selected — full hub ──
        state.isConnected -> {
            ConnectStatusBar(state)
            Spacer(Modifier.height(8.dp))
            ConnectWorldHeader(
                worlds = worlds,
                selectedWorld = selectedWorld,
                onSelectWorld = {
                    connectViewModel.selectWorld(it)
                    connectViewModel.requestPlayerData()
                },
                onDisconnect = { connectViewModel.disconnect() },
            )
            Spacer(Modifier.height(8.dp))
            QuickLinkGrid(CONNECT_LINKS.map { it.first }) { index ->
                onConnectNav(CONNECT_LINKS[index].second)
            }
        }
    }
}

@Composable
private fun ConnectDisconnectedCard(
    state: ConnectionState,
    onScanQr: () -> Unit,
    onReconnect: () -> Unit,
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
                    "Stream Minecraft world data from your PC",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Both devices on the same WiFi",
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
            Text("Scan QR Code")
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = onReconnect,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reconnect to Last Device")
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
private fun ConnectStatusBar(state: ConnectionState) {
    val deviceName = (state as? ConnectionState.Connected)?.deviceName ?: ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Emerald.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Emerald, CircleShape),
        )
        Text(
            "Connected" + if (deviceName.isNotEmpty()) " to $deviceName" else "",
            style = MaterialTheme.typography.labelSmall,
            color = Emerald,
            modifier = Modifier.weight(1f),
        )
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
                "Select a World",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Disconnect",
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
                        world.displayName + if (isModded) " (Modded)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isModded) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    )
                    Text(
                        "${world.gameMode.replaceFirstChar { it.uppercase() }} • ${world.difficulty.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (worlds.isEmpty()) {
            Text(
                "No worlds found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectWorldHeader(
    worlds: List<WorldInfo>,
    selectedWorld: String?,
    onSelectWorld: (String) -> Unit,
    onDisconnect: () -> Unit,
) {
    val currentWorld = worlds.firstOrNull { it.folderName == selectedWorld }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpyglassIconImage(PixelIcons.Globe, contentDescription = null, tint = Emerald, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f)) {
            Text(
                currentWorld?.displayName ?: "Unknown World",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { expanded = true },
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                worlds.forEach { world ->
                    DropdownMenuItem(
                        text = { Text(world.displayName) },
                        onClick = {
                            expanded = false
                            onSelectWorld(world.folderName)
                        },
                    )
                }
            }
        }
        Text(
            "Disconnect",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFF44336),
            modifier = Modifier.clickable { onDisconnect() },
        )
    }
}


