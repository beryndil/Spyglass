package dev.spyglass.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import androidx.datastore.preferences.core.edit
import coil.compose.AsyncImage
import dev.spyglass.android.core.net.MojangApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

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
    QuickLink(PixelIcons.Torch,     "Light Spacing")          to 10,
    // ── Crafting & Resources ──
    QuickLink(PixelIcons.Anvil,     "Enchanting")             to 2,
    QuickLink(PixelIcons.Smelt,     "Smelting")               to 7,
    QuickLink(PixelIcons.Storage,   "Storage")                to 6,
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
    val playerUsername: String = "",
    val playerUuid: String = "",
    val dismissUsernameDialog: Boolean = false,
    val loaded: Boolean = false,
)

// ── Home screen ─────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onBrowseTab: (Int) -> Unit,
    onCalcTab: (Int) -> Unit,
    onSearch: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tips = stringArrayResource(R.array.tips)
    val tipIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % tips.size }

    // Single DataStore collector — replaces 6 separate flows
    val prefs by remember {
        context.dataStore.data.map { p ->
            HomePrefs(
                showTipOfDay = p[PreferenceKeys.SHOW_TIP_OF_DAY] ?: true,
                showFavoritesOnHome = p[PreferenceKeys.SHOW_FAVORITES_ON_HOME] ?: false,
                playerUsername = p[PreferenceKeys.PLAYER_USERNAME] ?: "",
                playerUuid = p[PreferenceKeys.PLAYER_UUID] ?: "",
                dismissUsernameDialog = p[PreferenceKeys.DISMISS_USERNAME_DIALOG] ?: false,
                loaded = true,
            )
        }
    }.collectAsState(initial = HomePrefs())

    // Async repo access — avoids blocking main thread if database isn't ready yet
    val repo by produceState<GameDataRepository?>(null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            GameDataRepository.get(context)
        }
    }
    val favorites by produceState(emptyList<FavoriteEntity>(), repo) {
        repo?.allFavorites()?.collect { value = it }
    }
    val blockCount by produceState(0, repo) {
        repo?.blockCountFlow()?.collect { value = it }
    }
    val itemCount by produceState(0, repo) {
        repo?.itemCountFlow()?.collect { value = it }
    }
    val todoPreview by produceState(emptyList<TodoEntity>(), repo) {
        repo?.incompleteTodosPreview(3)?.collect { value = it }
    }
    val todoCount by produceState(0, repo) {
        repo?.incompleteTodoCount()?.collect { value = it }
    }

    // If username is set but UUID is missing, fetch it now (upgrade path for existing users)
    LaunchedEffect(prefs.loaded, prefs.playerUsername, prefs.playerUuid) {
        if (prefs.loaded && prefs.playerUsername.isNotBlank() && prefs.playerUuid.isBlank()) {
            val uuid = MojangApi.fetchUuid(prefs.playerUsername)
            if (uuid != null) {
                context.dataStore.edit { it[PreferenceKeys.PLAYER_UUID] = uuid }
            }
        }
    }

    // Username dialog state — only evaluate after DataStore has loaded
    var showUsernameDialog by remember { mutableStateOf(false) }
    LaunchedEffect(prefs.loaded, prefs.playerUsername, prefs.dismissUsernameDialog) {
        showUsernameDialog = prefs.loaded && prefs.playerUsername.isBlank() && !prefs.dismissUsernameDialog
    }

    if (showUsernameDialog) {
        UsernameDialog(
            onSave = { name ->
                scope.launch {
                    context.dataStore.edit { it[PreferenceKeys.PLAYER_USERNAME] = name }
                    val uuid = MojangApi.fetchUuid(name)
                    if (uuid != null) {
                        context.dataStore.edit { it[PreferenceKeys.PLAYER_UUID] = uuid }
                    }
                }
                showUsernameDialog = false
            },
            onLater = { showUsernameDialog = false },
            onDontAskAgain = {
                scope.launch { context.dataStore.edit { it[PreferenceKeys.DISMISS_USERNAME_DIALOG] = true } }
                showUsernameDialog = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── A. Header / Branding ──
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val skinModel = if (prefs.playerUuid.isNotBlank()) MojangApi.skinUrl(prefs.playerUuid) else null
            if (skinModel != null) {
                AsyncImage(
                    model = skinModel,
                    contentDescription = "${prefs.playerUsername} skin",
                    modifier = Modifier.height(140.dp),
                )
            } else {
                SpyglassIconImage(
                    SpyglassIcon.Drawable(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(144.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (prefs.playerUsername.isNotBlank()) stringResource(R.string.home_welcome_name, prefs.playerUsername) else stringResource(R.string.home_welcome),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.home_minecraft_version), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }

        // ── Search ──
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

        // ── B. Todo list ──
        if (todoCount > 0) {
            SectionHeader(stringResource(R.string.home_todo), icon = PixelIcons.Todo)
            ResultCard {
                todoPreview.forEach { todo ->
                    HomeTodoRow(
                        todo = todo,
                        onToggle = { repo?.let { r -> scope.launch { r.toggleTodoCompleted(todo.id, !todo.completed) } } },
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
            SectionHeader(stringResource(R.string.home_todo), icon = PixelIcons.Todo)
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

        // ── B2. Favorites on Home ──
        if (prefs.showFavoritesOnHome && favorites.isNotEmpty()) {
            SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
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

        // ── C. Quick Access — Tools ──
        SectionHeader(stringResource(R.string.home_tools), icon = SpyglassIcon.Drawable(R.drawable.item_diamond_pickaxe))
        QuickLinkGrid(CALC_LINKS.map { it.first }) { index -> onCalcTab(CALC_LINKS[index].second) }

        // ── D. Tip of the Day ──
        if (prefs.showTipOfDay) {
            ResultCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_did_you_know), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    tips[tipIndex],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── E. Quick Access — Browse ──
        SectionHeader(stringResource(R.string.home_browse), icon = PixelIcons.Browse)
        browseLinks().let { links ->
            QuickLinkGrid(links.map { it.first }) { index -> onBrowseTab(links[index].second) }
        }

        // ── F. What's New ──
        SectionHeader(stringResource(R.string.home_whats_new))
        ResultCard {
            WhatsNewItem(stringResource(R.string.home_whats_new_blocks, blockCount, itemCount), stringResource(R.string.home_whats_new_blocks_desc))
            SpyglassDivider()
            WhatsNewItem(stringResource(R.string.home_whats_new_enchant), stringResource(R.string.home_whats_new_enchant_desc))
            SpyglassDivider()
            WhatsNewItem(stringResource(R.string.home_whats_new_links), stringResource(R.string.home_whats_new_links_desc))
            SpyglassDivider()
            WhatsNewItem(stringResource(R.string.home_whats_new_search), stringResource(R.string.home_whats_new_search_desc))
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Quick link grid — 2 columns ─────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLinkGrid(links: List<QuickLink>, onTap: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2,
    ) {
        links.forEachIndexed { index, link ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(LocalSurfaceCard.current, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { onTap(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpyglassIconImage(link.icon, contentDescription = null, tint = if (link.iconTint == Color.Unspecified) MaterialTheme.colorScheme.primary else link.iconTint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(link.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        // If odd number of links, add spacer to fill the last row
        if (links.size % 2 != 0) {
            Spacer(Modifier.weight(1f))
        }
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

@Composable
private fun WhatsNewItem(title: String, desc: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}
