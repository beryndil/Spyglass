package dev.spyglass.android.browse.biomes

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.BiomeResourceMap
import dev.spyglass.android.data.db.entities.BiomeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

// Mob IDs known to exist in the mobs table
private val KNOWN_MOB_IDS = setOf(
    "zombie", "skeleton", "creeper", "spider", "cave_spider", "enderman", "witch",
    "slime", "ghast", "zombie_pigman", "blaze", "magma_cube", "wither_skeleton",
    "endermite", "silverfish", "guardian", "elder_guardian", "shulker", "evoker",
    "vindicator", "pillager", "ravager", "vex", "husk", "stray", "drowned",
    "phantom", "piglin", "hoglin", "zoglin", "piglin_brute", "strider",
    "cow", "pig", "sheep", "chicken", "rabbit", "fox", "wolf", "ocelot", "cat",
    "parrot", "horse", "donkey", "mule", "llama", "polar_bear", "panda", "bee",
    "goat", "axolotl", "glow_squid", "squid", "dolphin", "pufferfish", "turtle", "tropical_fish",
    "mooshroom", "frog", "allay", "sniffer", "camel", "armadillo", "breeze",
    "bogged", "creaking", "warden", "ender_dragon", "wither", "villager",
    "wandering_trader", "bat", "iron_golem", "snow_golem",
)

private val BIOME_CATEGORIES = listOf("all", "forest", "ocean", "desert", "mountain", "cave", "nether", "end")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BiomesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val biomes: StateFlow<List<BiomeEntity>> = combine(_query.debounce(200), _category) { q, cat ->
        repo.searchBiomes(q).map { list ->
            if (cat == "all") list else list.filter { it.category.equals(cat, ignoreCase = true) }
        }
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandBiome(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }
}

private fun parseMobs(mobsJson: String): List<String> {
    val trimmed = mobsJson.trim()
    if (trimmed.isBlank() || trimmed == "[]") return emptyList()
    return trimmed.removeSurrounding("[", "]")
        .replace("\"", "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun parseStructures(structures: String): List<String> {
    if (structures.isBlank()) return emptyList()
    return structures.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

private fun formatId(id: String): String =
    id.replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun parseBiomeColor(hex: String): Color? =
    if (hex.isNotEmpty()) runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() else null

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BiomesScreen(
    targetBiomeId: String? = null,
    onNavigateToMob: (mobId: String) -> Unit = {},
    onNavigateToStructure: (structureId: String) -> Unit = {},
    onItemTap: (String) -> Unit = {},
    vm: BiomesViewModel = viewModel(),
) {
    val query       by vm.query.collectAsState()
    val category    by vm.category.collectAsState()
    val biomes      by vm.biomes.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val listState   = rememberLazyListState()

    // Auto-expand and scroll to target biome from cross-reference
    LaunchedEffect(targetBiomeId, biomes) {
        if (targetBiomeId != null && biomes.isNotEmpty()) {
            vm.expandBiome(targetBiomeId)
            val idx = biomes.indexOfFirst { it.id == targetBiomeId }
            if (idx >= 0) listState.animateScrollToItem(idx + 1) // +1 for intro header
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search biomes\u2026", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        // Category filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(BIOME_CATEGORIES) { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { vm.setCategory(c) },
                    label = {
                        Text(
                            c.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Biome,
                    title = "Biomes",
                    description = "Every Minecraft biome with temperature, mobs, structures, and map colors",
                    stat = "${biomes.size} biomes",
                )
            }
            items(biomes, key = { it.id }) { b ->
                val isExpanded = b.id in expandedIds
                Column {
                    BiomeListItem(b, onClick = { vm.toggleExpanded(b.id) })
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        BiomeDetailCard(b, onNavigateToMob, onNavigateToStructure, onItemTap)
                    }
                }
            }
            if (biomes.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No biomes found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}

@Composable
private fun BiomeListItem(b: BiomeEntity, onClick: () -> Unit) {
    val biomeColor = parseBiomeColor(b.color)

    BrowseListItem(
        headline    = b.name,
        supporting  = b.id,
        leadingIcon = BiomeTextures.get(b.id) ?: PixelIcons.Biome,
        modifier    = Modifier.clickable { onClick() },
        trailing    = {
            Column(horizontalAlignment = Alignment.End) {
                CategoryBadge(label = b.category.ifEmpty { "unknown" }, color = Emerald)
                Spacer(Modifier.height(2.dp))
                Text("${b.temperature}\u00B0  ${b.precipitation}", style = MaterialTheme.typography.bodySmall, color = Stone500)
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BiomeDetailCard(
    biome: BiomeEntity,
    onMobTap: (String) -> Unit,
    onStructureTap: (String) -> Unit,
    onItemTap: (String) -> Unit,
) {
    val bgColor   = parseBiomeColor(biome.color) ?: SurfaceDark
    val isLight   = (0.299 * bgColor.red + 0.587 * bgColor.green + 0.114 * bgColor.blue) > 0.5
    val textColor    = if (isLight) Background else Stone100
    val subtextColor = if (isLight) Stone700 else Stone300
    val labelColor   = if (isLight) Background.copy(alpha = 0.7f) else Gold

    val mobs       = parseMobs(biome.mobsJson)
    val structures = parseStructures(biome.structures)
    val resources  = BiomeResourceMap.itemsForBiome(biome.id)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Temperature", style = MaterialTheme.typography.bodyMedium, color = subtextColor)
            Text("${biome.temperature}\u00B0", style = MaterialTheme.typography.bodyLarge, color = textColor)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Precipitation", style = MaterialTheme.typography.bodyMedium, color = subtextColor)
            Text(biome.precipitation.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, color = textColor)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Category", style = MaterialTheme.typography.bodyMedium, color = subtextColor)
            Text(biome.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, color = textColor)
        }

        // Features
        if (biome.features.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text("Features", style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(biome.features, style = MaterialTheme.typography.bodySmall, color = subtextColor)
        }

        // Resources (from BiomeResourceMap)
        if (resources.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text("Resources", style = MaterialTheme.typography.labelSmall, color = labelColor)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                resources.forEach { itemId ->
                    AssistChip(
                        onClick = { onItemTap(itemId) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val tex = ItemTextures.get(itemId)
                                if (tex != null) {
                                    SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(formatId(itemId), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = textColor,
                            containerColor = textColor.copy(alpha = 0.15f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Structures
        if (structures.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text("Structures", style = MaterialTheme.typography.labelSmall, color = labelColor)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                structures.forEach { s ->
                    AssistChip(
                        onClick = { onStructureTap(s) },
                        label = { Text(formatId(s), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = textColor,
                            containerColor = textColor.copy(alpha = 0.15f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Mobs
        if (mobs.isNotEmpty()) {
            HorizontalDivider(color = textColor.copy(alpha = 0.2f), thickness = 0.5.dp)
            Text("Mobs found here", style = MaterialTheme.typography.labelSmall, color = labelColor)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mobs.forEach { mobId ->
                    if (mobId in KNOWN_MOB_IDS) {
                        AssistChip(
                            onClick = { onMobTap(mobId) },
                            label = { Text(formatId(mobId), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = textColor,
                                containerColor = textColor.copy(alpha = 0.15f),
                            ),
                            border = null,
                        )
                    }
                    // Skip unknown mob IDs gracefully
                }
            }
        }
    }
}
