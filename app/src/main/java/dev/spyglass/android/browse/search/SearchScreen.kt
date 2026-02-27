package dev.spyglass.android.browse.search

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

data class SearchResult(val type: String, val id: String, val name: String, val detail: String = "")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<SearchResult>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.length < 2) return@flatMapLatest flowOf(emptyList())
            combine(
                listOf(
                    repo.searchBlocks(q),
                    repo.searchRecipes(q),
                    repo.searchMobs(q),
                    repo.searchBiomes(q),
                    repo.searchEnchants(q),
                    repo.searchPotions(q),
                    repo.searchTrades(q),
                    repo.searchStructures(q),
                    repo.searchItems(q),
                )
            ) { results ->
                val blocks     = (results[0] as List<*>)
                val recipes    = (results[1] as List<*>)
                val mobs       = (results[2] as List<*>)
                val biomes     = (results[3] as List<*>)
                val enchants   = (results[4] as List<*>)
                val potions    = (results[5] as List<*>)
                val trades     = (results[6] as List<*>)
                val structures = (results[7] as List<*>)
                val items      = (results[8] as List<*>)
                buildList {
                    addAll(blocks.take(5).map   { it as dev.spyglass.android.data.db.entities.BlockEntity;     SearchResult("Block",       it.id, it.name, it.category) })
                    addAll(recipes.take(5).map  { it as dev.spyglass.android.data.db.entities.RecipeEntity;    SearchResult("Recipe",      it.outputItem, it.outputItem.substringAfterLast(':').replace('_', ' ').replaceFirstChar { c -> c.uppercase() }, it.type.replace('_', ' ')) })
                    addAll(mobs.take(5).map     { it as dev.spyglass.android.data.db.entities.MobEntity;       SearchResult("Mob",         it.id, it.name, it.category) })
                    addAll(biomes.take(5).map   { it as dev.spyglass.android.data.db.entities.BiomeEntity;     SearchResult("Biome",       it.id, it.name, it.category) })
                    addAll(enchants.take(5).map { it as dev.spyglass.android.data.db.entities.EnchantEntity;   SearchResult("Enchantment", it.id, it.name, it.target) })
                    addAll(potions.take(5).map  { it as dev.spyglass.android.data.db.entities.PotionEntity;    SearchResult("Potion",      it.id, it.name, it.category) })
                    addAll(trades.take(5).map   { it as dev.spyglass.android.data.db.entities.TradeEntity;     SearchResult("Trade",       it.profession, "${it.sellItem.replace('_', ' ')} (${it.levelName})", it.profession) })
                    addAll(structures.take(5).map { it as dev.spyglass.android.data.db.entities.StructureEntity; SearchResult("Structure",  it.id, it.name, it.dimension) })
                    addAll(items.take(5).map    { it as dev.spyglass.android.data.db.entities.ItemEntity;      SearchResult("Item",        it.id, it.name, it.category) })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
}

/** Maps search result type -> browse tab index */
fun browseTabForType(type: String): Int = when (type) {
    "Block"       -> 0
    "Item"        -> 1
    "Recipe"      -> 2
    "Mob"         -> 3
    "Trade"       -> 4
    "Biome"       -> 5
    "Structure"   -> 6
    "Enchantment" -> 7
    "Potion"      -> 8
    else          -> 0
}

private fun typeIcon(type: String): SpyglassIcon = when (type) {
    "Block"       -> PixelIcons.Blocks
    "Recipe"      -> PixelIcons.Crafting
    "Mob"         -> PixelIcons.Mob
    "Biome"       -> PixelIcons.Biome
    "Enchantment" -> PixelIcons.Enchant
    "Potion"      -> PixelIcons.Potion
    "Trade"       -> PixelIcons.Trade
    "Structure"   -> PixelIcons.Structure
    "Item"        -> PixelIcons.Item
    else          -> PixelIcons.Search
}

private fun typeColor(type: String) = when (type) {
    "Block"       -> Stone300
    "Recipe"      -> Gold
    "Mob"         -> NetherRed
    "Biome"       -> Emerald
    "Enchantment" -> EnderPurple
    "Potion"      -> PotionBlue
    "Trade"       -> Emerald
    "Structure"   -> Gold
    "Item"        -> Gold
    else          -> Stone500
}

@Composable
fun SearchScreen(
    onResultTap: (tab: Int, id: String) -> Unit = { _, _ -> },
    vm: SearchViewModel = viewModel(),
) {
    val query   by vm.query.collectAsState()
    val results by vm.results.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search all Minecraft data\u2026", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        if (query.length < 2 && results.isEmpty()) {
            EmptyState(
                icon     = PixelIcons.Search,
                title    = "Search everything",
                subtitle = "Type at least 2 characters to search blocks, mobs, biomes, structures, items, recipes, trades, and more.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results) { r ->
                    BrowseListItem(
                        headline    = r.name,
                        supporting  = r.id,
                        leadingIcon = typeIcon(r.type),
                        leadingIconTint = typeColor(r.type),
                        modifier    = Modifier.clickable { onResultTap(browseTabForType(r.type), r.id) },
                        trailing    = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                CategoryBadge(label = r.type, color = typeColor(r.type))
                                if (r.detail.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(r.detail, style = MaterialTheme.typography.bodySmall, color = Stone500)
                                }
                            }
                        },
                    )
                }
                if (results.isEmpty() && query.length >= 2) {
                    item {
                        EmptyState(
                            icon     = PixelIcons.SearchOff,
                            title    = "No results",
                            subtitle = "No matches for \"$query\"",
                        )
                    }
                }
            }
        }
    }
}
