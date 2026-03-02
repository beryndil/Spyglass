package dev.spyglass.android.browse.mobs

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.MobEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Non-biome, non-structure spawn locations shown as plain badges
private val SPECIAL_LOCATIONS = setOf(
    "all_overworld", "slime_chunks", "caves", "raid", "bred", "summoned",
    "underground_ocean", "nether_overworld", "breeds_only", "extreme_hills",
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MobsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query    = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query:    StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    private val _sortKey = MutableStateFlow("name")
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()

    val mobs: StateFlow<List<MobEntity>> = combine(
        _query.debounce(200), _category, _sortKey
    ) { q, cat, sort -> Triple(q, cat, sort) }
    .flatMapLatest { (q, cat, sort) ->
        val flow = when (cat) {
            "all" -> repo.searchMobs(q)
            "breedable" -> repo.breedableMobs()
            else -> repo.mobsByCategory(cat)
        }
        flow.map { list ->
            when (sort) {
                "health" -> list.sortedByDescending { it.health.split("-").first().toFloatOrNull() ?: 0f }
                "xp" -> list.sortedByDescending { it.xpDrop.split("-").first().toFloatOrNull() ?: 0f }
                else -> list
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
    fun setSortKey(k: String) { _sortKey.value = k }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandMob(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteMobs: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("mob")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "mob", displayName = displayName))
        }
    }
}

private fun parseDrops(dropsJson: String): List<String> {
    val trimmed = dropsJson.trim()
    if (trimmed.isBlank() || trimmed == "[]") return emptyList()
    return trimmed.removeSurrounding("[", "]")
        .replace("\"", "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun parseBiomes(biomesJson: String): List<String> {
    val trimmed = biomesJson.trim()
    if (trimmed.isBlank() || trimmed == "[]") return emptyList()
    return trimmed.removeSurrounding("[", "]")
        .replace("\"", "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun formatId(id: String): String =
    id.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

// Structure IDs that should be clickable in mob spawn locations
private val STRUCTURE_IDS = setOf(
    "nether_fortress", "ocean_monument", "woodland_mansion", "pillager_outpost",
    "stronghold", "village", "swamp_hut", "bastion_remnant", "trial_chambers",
    "mineshaft", "dungeon", "ancient_city", "end_city",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MobsScreen(
    targetMobId: String? = null,
    onNavigateToBiome: (biomeId: String) -> Unit = {},
    onNavigateToStructure: (structureId: String) -> Unit = {},
    onItemTap: (String) -> Unit = {},
    onMobTap: (String) -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
    entityLinkIndex: EntityLinkIndex = EntityLinkIndex(emptyList()),
    vm: MobsViewModel = viewModel(),
) {
    val query       by vm.query.collectAsState()
    val category    by vm.category.collectAsState()
    val sortKey     by vm.sortKey.collectAsState()
    val mobs        by vm.mobs.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val favoriteMobs by vm.favoriteMobs.collectAsState()
    val listState   = rememberLazyListState()
    val mobSortOptions = remember { listOf(
        SortOption("Name A\u2192Z", "name"),
        SortOption("Health \u2193", "health"),
        SortOption("XP \u2193", "xp"),
    ) }

    val categories = listOf("all", "hostile", "neutral", "passive", "boss", "breedable")

    // Auto-expand and scroll to target mob from cross-reference
    LaunchedEffect(targetMobId) {
        if (targetMobId != null) {
            vm.setQuery("")
            vm.setCategory("all")
            snapshotFlow { mobs }
                .first { it.isNotEmpty() }
            val idx = mobs.indexOfFirst { it.id == targetMobId }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1) // +1 for intro header
                vm.expandMob(targetMobId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query, onValueChange = vm::setQuery,
                placeholder = { Text("Search mobs\u2026", color = MaterialTheme.colorScheme.secondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
            )
            SortButton(options = mobSortOptions, selectedKey = sortKey, onSelect = vm::setSortKey)
        }
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            items(categories) { c ->
                FilterChip(selected = category == c, onClick = { vm.setCategory(c) },
                    label = { Text(if (c == "all") stringResource(R.string.all) else c.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Mob,
                    title = "Mobs",
                    description = "Every Minecraft mob with health, drops, spawn biomes, and strategy tips",
                    stat = "${mobs.size} mobs",
                )
            }
            if (favoriteMobs.isNotEmpty()) {
                item(key = "fav_header") {
                    SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
                }
                items(favoriteMobs, key = { "fav_${it.id}" }) { fav ->
                    val isFav = fav.id in favoriteIds
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        leadingIcon = MobTextures.get(fav.id) ?: PixelIcons.Mob,
                        modifier = Modifier.clickable { vm.toggleExpanded(fav.id) },
                        trailing = {
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = stringResource(R.string.favorite),
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                }
                item(key = "fav_divider") { SpyglassDivider() }
            }
            items(mobs, key = { it.id }) { m ->
                val isExpanded = m.id in expandedIds
                val categoryColor = when (m.category) {
                    "hostile"  -> NetherRed
                    "neutral"  -> MaterialTheme.colorScheme.primary
                    "passive"  -> Emerald
                    "boss"     -> EnderPurple
                    else       -> MaterialTheme.colorScheme.secondary
                }
                Column {
                    BrowseListItem(
                        headline    = m.name,
                        supporting  = "",
                        leadingIcon = MobTextures.get(m.id) ?: PixelIcons.Mob,
                        modifier    = Modifier.clickable { vm.toggleExpanded(m.id) },
                        trailing    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    CategoryBadge(label = m.category, color = categoryColor)
                                    Spacer(Modifier.height(2.dp))
                                    Text("HP ${m.health}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(Modifier.width(6.dp))
                                val isFav = m.id in favoriteIds
                                IconButton(onClick = { vm.toggleFavorite(m.id, m.name) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = stringResource(R.string.favorite),
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        MobDetailCard(m, onNavigateToBiome, onNavigateToStructure, onItemTap, onMobTap, onCalcTab, entityLinkIndex)
                    }
                }
            }
            if (mobs.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No mobs found",
                    subtitle = "Try a different search or category",
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MobDetailCard(mob: MobEntity, onBiomeTap: (String) -> Unit, onStructureTap: (String) -> Unit, onItemTap: (String) -> Unit, onMobTap: (String) -> Unit, onCalcTab: (Int) -> Unit, entityLinkIndex: EntityLinkIndex) {
    val drops  = parseDrops(mob.dropsJson)
    val biomes = parseBiomes(mob.spawnBiomesJson)

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        MinecraftIdRow(mob.id)

        // Description
        if (mob.description.isNotEmpty()) {
            LinkedDescription(
                description = mob.description,
                linkIndex = entityLinkIndex,
                selfId = mob.id,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onBiomeTap = onBiomeTap,
                onStructureTap = onStructureTap,
            )
        }

        // Stats
        StatRow("Health", "${mob.health} HP")
        StatRow("XP Drop", mob.xpDrop)
        if (mob.isFireImmune) StatRow("Fire Immune", stringResource(R.string.yes))

        // Breeding
        if (mob.breeding.isNotEmpty()) {
            SpyglassDivider()
            Text("Breeding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mob.breeding.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { food ->
                    AssistChip(
                        onClick = { onItemTap(food) },
                        label = { Text(formatId(food), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            val icon = ItemTextures.get(food)
                            if (icon != null) SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color.Unspecified)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Emerald,
                            containerColor = Emerald.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Drops
        if (drops.isNotEmpty()) {
            SpyglassDivider()
            Text("Drops", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                drops.forEach { drop ->
                    AssistChip(
                        onClick = { onItemTap(drop) },
                        label = { Text(formatId(drop), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Spawn biomes
        if (biomes.isNotEmpty()) {
            SpyglassDivider()
            Text("Found in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                biomes.forEach { biomeId ->
                    if (biomeId in STRUCTURE_IDS) {
                        AssistChip(
                            onClick = { onStructureTap(biomeId) },
                            label = { Text(formatId(biomeId), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ),
                            border = null,
                        )
                    } else if (biomeId in SPECIAL_LOCATIONS) {
                        CategoryBadge(label = formatId(biomeId), color = MaterialTheme.colorScheme.secondary)
                    } else {
                        AssistChip(
                            onClick = { onBiomeTap(biomeId) },
                            label = { Text(formatId(biomeId), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Emerald,
                                containerColor = Emerald.copy(alpha = 0.12f),
                            ),
                            border = null,
                        )
                    }
                }
            }
        }

        // Cross-links to tool tabs
        val hasFoodDrops = drops.any { it in FOOD_DROP_IDS }
        val isHostile = mob.category == "hostile" || mob.category == "boss"
        if (hasFoodDrops || isHostile) {
            SpyglassDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasFoodDrops) {
                    Text(
                        "Food Guide \u2192",
                        style = MaterialTheme.typography.labelSmall,
                        color = PotionBlue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onCalcTab(15) },
                    )
                }
                if (isHostile) {
                    Text(
                        "Light Spacing \u2192",
                        style = MaterialTheme.typography.labelSmall,
                        color = PotionBlue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onCalcTab(10) },
                    )
                }
            }
        }
    }
}

private val FOOD_DROP_IDS = setOf(
    "beef", "raw_beef", "cooked_beef", "porkchop", "raw_porkchop", "cooked_porkchop",
    "chicken", "raw_chicken", "cooked_chicken", "mutton", "raw_mutton", "cooked_mutton",
    "rabbit", "raw_rabbit", "cooked_rabbit", "cod", "raw_cod", "cooked_cod",
    "salmon", "raw_salmon", "cooked_salmon", "rotten_flesh", "spider_eye",
)
