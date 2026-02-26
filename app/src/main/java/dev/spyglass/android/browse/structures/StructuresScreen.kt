package dev.spyglass.android.browse.structures

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
import dev.spyglass.android.data.db.entities.StructureEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

private fun formatId(id: String): String =
    id.replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun parseCommaSeparated(csv: String): List<String> {
    if (csv.isBlank()) return emptyList()
    return csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class StructuresViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _dimension = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val dimension: StateFlow<String> = _dimension.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val structures: StateFlow<List<StructureEntity>> = combine(
        _query.debounce(200), _dimension
    ) { q, dim ->
        repo.searchStructures(q).map { list ->
            if (dim == "all") list else list.filter { it.dimension == dim }
        }
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setDimension(d: String) { _dimension.value = d }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandStructure(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StructuresScreen(
    targetStructureId: String? = null,
    onNavigateToMob: (mobId: String) -> Unit = {},
    onNavigateToBiome: (biomeId: String) -> Unit = {},
    onItemTap: (itemId: String) -> Unit = {},
    vm: StructuresViewModel = viewModel(),
) {
    val query        by vm.query.collectAsState()
    val dimension    by vm.dimension.collectAsState()
    val structures   by vm.structures.collectAsState()
    val expandedIds  by vm.expandedIds.collectAsState()
    val listState    = rememberLazyListState()

    val dimensions = listOf("all", "overworld", "nether", "end")

    // Auto-expand and scroll to target structure from cross-reference
    LaunchedEffect(targetStructureId, structures) {
        if (targetStructureId != null && structures.isNotEmpty()) {
            vm.expandStructure(targetStructureId)
            val idx = structures.indexOfFirst { it.id == targetStructureId }
            if (idx >= 0) listState.animateScrollToItem(idx + 1) // +1 for intro header
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search structures\u2026", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dimensions.forEach { d ->
                FilterChip(selected = dimension == d, onClick = { vm.setDimension(d) },
                    label = { Text(d.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Structure,
                    title = "Structures",
                    description = "Every Minecraft structure with biomes, mobs, loot, and how to find them",
                    stat = "${structures.size} structures",
                )
            }
            items(structures, key = { it.id }) { s ->
                val isExpanded = s.id in expandedIds
                Column {
                    StructureListItem(s, onClick = { vm.toggleExpanded(s.id) })
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        StructureDetailCard(s, onNavigateToMob, onNavigateToBiome, onItemTap)
                    }
                }
            }
            if (structures.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No structures found",
                    subtitle = "Try a different search or dimension filter",
                )
            }
        }
    }
}

@Composable
private fun StructureListItem(s: StructureEntity, onClick: () -> Unit) {
    val dimensionColor = when (s.dimension) {
        "nether" -> NetherRed
        "end"    -> EnderPurple
        else     -> Emerald
    }
    val difficultyColor = when (s.difficulty) {
        "hard"   -> NetherRed
        "medium" -> Gold
        else     -> Emerald
    }

    BrowseListItem(
        headline    = s.name,
        supporting  = s.id,
        leadingIcon = StructureTextures.get(s.id) ?: PixelIcons.Structure,
        modifier    = Modifier.clickable { onClick() },
        trailing    = {
            Column(horizontalAlignment = Alignment.End) {
                CategoryBadge(label = s.dimension, color = dimensionColor)
                if (s.difficulty.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    CategoryBadge(label = s.difficulty, color = difficultyColor)
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StructureDetailCard(
    structure: StructureEntity,
    onMobTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
    onItemTap: (String) -> Unit,
) {
    val biomes      = parseCommaSeparated(structure.biomes)
    val mobs        = parseCommaSeparated(structure.mobs)
    val loot        = parseCommaSeparated(structure.loot)
    val uniqueBlocks = parseCommaSeparated(structure.uniqueBlocks)

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        // Description
        if (structure.description.isNotEmpty()) {
            Text(structure.description, style = MaterialTheme.typography.bodyMedium, color = Stone300)
        }

        // How to Find
        if (structure.findMethod.isNotEmpty()) {
            SpyglassDivider()
            Text("How to Find", style = MaterialTheme.typography.labelSmall, color = Gold)
            Text(structure.findMethod, style = MaterialTheme.typography.bodyMedium, color = Stone300)
        }

        // Biomes
        if (biomes.isNotEmpty()) {
            SpyglassDivider()
            Text("Biomes", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                biomes.forEach { biomeId ->
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

        // Mobs
        if (mobs.isNotEmpty()) {
            SpyglassDivider()
            Text("Mobs", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mobs.forEach { mobId ->
                    AssistChip(
                        onClick = { onMobTap(mobId) },
                        label = { Text(formatId(mobId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = NetherRed,
                            containerColor = NetherRed.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Loot
        if (loot.isNotEmpty()) {
            SpyglassDivider()
            Text("Loot", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                loot.forEach { itemId ->
                    AssistChip(
                        onClick = { onItemTap(itemId) },
                        label = { Text(formatId(itemId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Gold,
                            containerColor = Gold.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }

        // Unique Blocks
        if (uniqueBlocks.isNotEmpty()) {
            SpyglassDivider()
            Text("Unique Blocks", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                uniqueBlocks.forEach { blockId ->
                    AssistChip(
                        onClick = { onItemTap(blockId) },
                        label = { Text(formatId(blockId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Stone300,
                            containerColor = Stone300.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }
    }
}
