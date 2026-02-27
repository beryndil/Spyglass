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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.MobEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

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

    val mobs: StateFlow<List<MobEntity>> = combine(_query.debounce(200), _category) { q, cat ->
        if (cat == "all") repo.searchMobs(q) else repo.mobsByCategory(cat)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandMob(id: String) {
        _expandedIds.value = _expandedIds.value + id
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
    vm: MobsViewModel = viewModel(),
) {
    val query      by vm.query.collectAsState()
    val category   by vm.category.collectAsState()
    val mobs       by vm.mobs.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val listState  = rememberLazyListState()

    val categories = listOf("all", "hostile", "neutral", "passive", "boss")

    // Auto-expand and scroll to target mob from cross-reference
    LaunchedEffect(targetMobId, mobs) {
        if (targetMobId != null && mobs.isNotEmpty()) {
            vm.expandMob(targetMobId)
            val idx = mobs.indexOfFirst { it.id == targetMobId }
            if (idx >= 0) listState.animateScrollToItem(idx + 1) // +1 for intro header
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search mobs…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { c ->
                FilterChip(selected = category == c, onClick = { vm.setCategory(c) },
                    label = { Text(c.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
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
            items(mobs, key = { it.id }) { m ->
                val isExpanded = m.id in expandedIds
                val categoryColor = when (m.category) {
                    "hostile"  -> NetherRed
                    "neutral"  -> Gold
                    "passive"  -> Emerald
                    "boss"     -> EnderPurple
                    else       -> Stone500
                }
                Column {
                    BrowseListItem(
                        headline    = m.name,
                        supporting  = m.id,
                        leadingIcon = MobTextures.get(m.id) ?: PixelIcons.Mob,
                        modifier    = Modifier.clickable { vm.toggleExpanded(m.id) },
                        trailing    = {
                            Column(horizontalAlignment = Alignment.End) {
                                CategoryBadge(label = m.category, color = categoryColor)
                                Spacer(Modifier.height(2.dp))
                                Text("HP ${m.health}", style = MaterialTheme.typography.bodySmall, color = Stone500)
                            }
                        },
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        MobDetailCard(m, onNavigateToBiome, onNavigateToStructure, onItemTap)
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
private fun MobDetailCard(mob: MobEntity, onBiomeTap: (String) -> Unit, onStructureTap: (String) -> Unit, onItemTap: (String) -> Unit) {
    val drops  = parseDrops(mob.dropsJson)
    val biomes = parseBiomes(mob.spawnBiomesJson)

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        // Description
        if (mob.description.isNotEmpty()) {
            Text(mob.description, style = MaterialTheme.typography.bodyMedium, color = Stone300)
        }

        // Stats
        StatRow("Health", "${mob.health} HP")
        StatRow("XP Drop", mob.xpDrop)
        if (mob.isFireImmune) StatRow("Fire Immune", "Yes")

        // Drops
        if (drops.isNotEmpty()) {
            SpyglassDivider()
            Text("Drops", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                drops.forEach { drop ->
                    AssistChip(
                        onClick = { onItemTap(drop) },
                        label = { Text(formatId(drop), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Stone300,
                            containerColor = Stone300.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Spawn biomes
        if (biomes.isNotEmpty()) {
            SpyglassDivider()
            Text("Found in", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                biomes.forEach { biomeId ->
                    if (biomeId in STRUCTURE_IDS) {
                        AssistChip(
                            onClick = { onStructureTap(biomeId) },
                            label = { Text(formatId(biomeId), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Gold,
                                containerColor = Gold.copy(alpha = 0.12f),
                            ),
                            border = null,
                        )
                    } else if (biomeId in SPECIAL_LOCATIONS) {
                        CategoryBadge(label = formatId(biomeId), color = Stone500)
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
    }
}
