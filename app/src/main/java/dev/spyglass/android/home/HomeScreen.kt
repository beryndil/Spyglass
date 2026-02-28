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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

// ── Minecraft tips / Did You Know ───────────────────────────────────────────

private val TIPS = listOf(
    "Foxes can pick up items with their mouths. Breed two foxes and the baby will trust you and bring you whatever it picks up.",
    "You can place torches on furnaces and crafting tables to save wall space in tight builds.",
    "Holding a shield while crouching blocks 100% of damage from explosions, including Creepers.",
    "Piglins will trade you items for gold ingots, but they attack immediately if you open a chest in the Nether near them.",
    "A single water source placed at the center of a 9x9 farm hydrates every block in range.",
    "Putting a campfire under a beehive lets you harvest honey without angering the bees.",
    "Fortune III on a hoe gives you more seeds and other drops from crops.",
    "You can use a Name Tag to name a mob 'Dinnerbone' or 'Grumm' to flip it upside down.",
    "Scaffolding is the fastest way to ascend and descend tall builds - just hold jump or sneak.",
    "Soul Speed boots let you walk faster on soul sand and soul soil but they take durability damage.",
    "Cats scare away Creepers and Phantoms, making them the best pet for base defense.",
    "A Smithing Table with a Netherite Upgrade template converts diamond gear without losing enchantments.",
    "Efficiency V on a pickaxe with a Haste II beacon mines stone almost instantly.",
    "Zombies and Skeletons burn in sunlight, but not if they're wearing a helmet.",
    "Sweet berries slow down all mobs that walk through them, including the Warden.",
    "Tridents with Riptide let you fly through rain - combine with Elytra for long-distance travel.",
    "Shulker boxes keep their contents when broken, making them portable storage.",
    "You can cure a Zombie Villager by throwing a Splash Potion of Weakness and feeding it a Golden Apple.",
    "Composters turn excess crops and plant matter into bone meal for more farming.",
    "Silk Touch on a pickaxe lets you pick up spawners... just kidding. But it does work on glass and ice.",
    "A Redstone comparator can read the fill level of containers like chests and barrels.",
    "Endermen can't teleport if they're in a boat or minecart.",
    "Dolphins give you a speed boost in water if you swim near them.",
    "The Warden is not meant to be fought - it has 500 HP and deals massive damage. Sneak past it.",
    "You can put banners on shields at a crafting table to display your custom design.",
    "Mending repairs your gear using XP orbs, making it the most valuable enchantment for tools.",
    "Respawn anchors work in the Nether like beds work in the Overworld. Beds explode in the Nether.",
    "Placing a block of ice under soul soil creates a 'blue ice highway' for ultra-fast boat travel.",
    "A Looting III sword gives more drops from mobs, including rare drops like Wither Skeleton skulls.",
    "Copper blocks oxidize over time, changing color from orange to teal. Wax them with honeycomb to freeze the look.",
)

// ── Quick link data ─────────────────────────────────────────────────────────

private data class QuickLink(
    val icon: SpyglassIcon,
    val label: String,
    val iconTint: Color = Gold,
)

private val BROWSE_LINKS = listOf(
    QuickLink(PixelIcons.Blocks,    "Blocks",       Stone300),
    QuickLink(PixelIcons.Item,      "Items",        Gold),
    QuickLink(PixelIcons.Crafting,  "Recipes",      Gold),
    QuickLink(PixelIcons.Mob,       "Mobs",         NetherRed),
    QuickLink(PixelIcons.Trade,     "Trades",       Emerald),
    QuickLink(PixelIcons.Biome,     "Biomes",       Emerald),
    QuickLink(PixelIcons.Structure, "Structures",   Gold),
    QuickLink(PixelIcons.Enchant,   "Enchants",     EnderPurple),
    QuickLink(PixelIcons.Potion,    "Potions",      PotionBlue),
)

private val CALC_LINKS = listOf(
    QuickLink(PixelIcons.Todo,     "Todo List"),
    QuickLink(PixelIcons.Storage,  "Shopping Lists"),
    QuickLink(PixelIcons.Anvil,    "Enchanting"),
    QuickLink(PixelIcons.Fill,     "Block Fill"),
    QuickLink(PixelIcons.Shapes,   "Shapes"),
    QuickLink(PixelIcons.Maze,     "Maze Maker"),
    QuickLink(PixelIcons.Storage,  "Storage"),
    QuickLink(PixelIcons.Smelt,    "Smelting"),
    QuickLink(PixelIcons.Nether,   "Nether Portal"),
    QuickLink(PixelIcons.Bookmark, "Reference"),
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
    "potion"    -> 8
    else        -> 0
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

// ── Home screen ─────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onBrowseTab: (Int) -> Unit,
    onCalcTab: (Int) -> Unit,
    onSearch: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tipIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % TIPS.size }

    val showTipOfDay by remember {
        context.dataStore.data.map { it[PreferenceKeys.SHOW_TIP_OF_DAY] ?: true }
    }.collectAsState(initial = true)

    val showFavoritesOnHome by remember {
        context.dataStore.data.map { it[PreferenceKeys.SHOW_FAVORITES_ON_HOME] ?: false }
    }.collectAsState(initial = false)

    // Use a sentinel to know when DataStore has actually loaded
    val prefsLoaded by remember {
        context.dataStore.data.map { true }
    }.collectAsState(initial = false)

    val playerUsername by remember {
        context.dataStore.data.map { it[PreferenceKeys.PLAYER_USERNAME] ?: "" }
    }.collectAsState(initial = "")

    val dismissUsernameDialog by remember {
        context.dataStore.data.map { it[PreferenceKeys.DISMISS_USERNAME_DIALOG] ?: false }
    }.collectAsState(initial = false)

    val repo = remember { GameDataRepository.get(context) }
    val favorites by repo.allFavorites().collectAsState(initial = emptyList())
    val blockCount by repo.blockCountFlow().collectAsState(initial = 0)
    val itemCount by repo.itemCountFlow().collectAsState(initial = 0)
    val todoPreview by repo.incompleteTodosPreview(3).collectAsState(initial = emptyList())
    val todoCount by repo.incompleteTodoCount().collectAsState(initial = 0)

    // Username dialog state — only evaluate after DataStore has loaded
    var showUsernameDialog by remember { mutableStateOf(false) }
    LaunchedEffect(prefsLoaded, playerUsername, dismissUsernameDialog) {
        showUsernameDialog = prefsLoaded && playerUsername.isBlank() && !dismissUsernameDialog
    }

    if (showUsernameDialog) {
        UsernameDialog(
            onSave = { name ->
                scope.launch { context.dataStore.edit { it[PreferenceKeys.PLAYER_USERNAME] = name } }
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
        TabIntroHeader(
            icon = SpyglassIcon.Drawable(dev.spyglass.android.R.drawable.ic_launcher_foreground),
            title = if (playerUsername.isNotBlank()) "Welcome $playerUsername to Spyglass" else "Welcome to Spyglass",
            description = "Your Minecraft companion for crafting, building, and exploring",
            stat = "Minecraft Java 1.21.4",
            iconTint = Color.Unspecified,
            iconSize = 144.dp,
        )

        // ── B. Todo list ──
        if (todoCount > 0) {
            SectionHeader("Todo", icon = PixelIcons.Todo)
            ResultCard {
                todoPreview.forEach { todo ->
                    HomeTodoRow(
                        todo = todo,
                        onToggle = { scope.launch { repo.toggleTodoCompleted(todo.id, !todo.completed) } },
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
                        if (todoCount > 3) "View all $todoCount tasks" else "Edit Todo List",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold,
                    )
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            SectionHeader("Todo", icon = PixelIcons.Todo)
            ResultCard(
                modifier = Modifier.clickable { onCalcTab(0) },
            ) {
                Text(
                    "What do you have planned for today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone300,
                )
                Text(
                    "Tap to open your Todo list \u2192",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gold,
                )
            }
        }

        // ── B2. Favorites on Home ──
        if (showFavoritesOnHome && favorites.isNotEmpty()) {
            SectionHeader("Favorites", icon = PixelIcons.Bookmark)
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

        // ── C. Quick Access — Browse ──
        SectionHeader("Browse", icon = PixelIcons.Browse)
        QuickLinkGrid(BROWSE_LINKS) { index -> onBrowseTab(index) }

        // ── D. Quick Access — Tools ──
        SectionHeader("Tools", icon = SpyglassIcon.Drawable(dev.spyglass.android.R.drawable.item_diamond_pickaxe))
        QuickLinkGrid(CALC_LINKS) { index -> onCalcTab(index) }

        // ── E. Tip of the Day ──
        if (showTipOfDay) {
            ResultCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DID YOU KNOW?", style = MaterialTheme.typography.labelSmall, color = Gold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    TIPS[tipIndex],
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone300,
                )
            }
        }

        // ── F. What's New ──
        SectionHeader("What's New")
        ResultCard {
            WhatsNewItem("$blockCount blocks & $itemCount items", "Full database with categories, durability, and recipes")
            SpyglassDivider()
            WhatsNewItem("Enchant optimizer", "Calculate the cheapest XP order for combining books on the anvil")
            SpyglassDivider()
            WhatsNewItem("Cross-tab links", "Tap any item, mob, biome, or structure to jump to its detail page")
            SpyglassDivider()
            WhatsNewItem("Search everything", "Global search across blocks, mobs, items, recipes, and more")
        }

        // ── F. Search CTA ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(8.dp))
                .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable { onSearch() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            SpyglassIconImage(PixelIcons.Search, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Search everything", style = MaterialTheme.typography.titleMedium, color = Gold)
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
                    .background(SurfaceCard, RoundedCornerShape(8.dp))
                    .border(1.dp, Stone700, RoundedCornerShape(8.dp))
                    .clickable { onTap(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpyglassIconImage(link.icon, contentDescription = null, tint = link.iconTint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(link.label, style = MaterialTheme.typography.bodyMedium, color = Stone100)
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
                checkedColor = Gold,
                uncheckedColor = Stone500,
                checkmarkColor = Background,
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
            color = if (todo.completed) Stone500 else Stone100,
            textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WhatsNewItem(title: String, desc: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Stone100)
        Spacer(Modifier.height(2.dp))
        Text(desc, style = MaterialTheme.typography.bodySmall, color = Stone500)
    }
}
