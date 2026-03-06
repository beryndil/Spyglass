package dev.spyglass.android.browse.search

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.*
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

data class SearchResult(val type: String, val id: String, val name: String, val detail: String = "")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val hideUnobtainable: StateFlow<Boolean> = app.dataStore.data
        .map { it[PreferenceKeys.HIDE_UNOBTAINABLE_BLOCKS] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val results: StateFlow<List<SearchResult>> = combine(_query.debounce(300), hideUnobtainable) { q, hide -> q to hide }
        .mapLatest { (q, hide) ->
            if (q.length < 2) return@mapLatest emptyList()
            searchAll(q, hide)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun searchAll(q: String, hideUnobtainable: Boolean): List<SearchResult> = coroutineScope {
        val blocks       = async { repo.searchBlocksOnce(q) }
        val recipes      = async { repo.searchRecipesOnce(q) }
        val mobs         = async { repo.searchMobsOnce(q) }
        val biomes       = async { repo.searchBiomesOnce(q) }
        val enchants     = async { repo.searchEnchantsOnce(q) }
        val potions      = async { repo.searchPotionsOnce(q) }
        val trades       = async { repo.searchTradesOnce(q) }
        val structures   = async { repo.searchStructuresOnce(q) }
        val items        = async { repo.searchItemsOnce(q) }
        val advancements = async { repo.searchAdvancementsOnce(q) }
        val commands     = async { repo.searchCommandsOnce(q) }

        buildList {
            val blockList = blocks.await().let { list ->
                if (hideUnobtainable) list.filter { it.isObtainable } else list
            }
            addAll(blockList.map { SearchResult("Block", it.id, it.name, it.category) })
            addAll(recipes.await().map { SearchResult("Recipe", it.outputItem, it.outputItem.substringAfterLast(':').replace('_', ' ').replaceFirstChar { c -> c.uppercase() }, it.type.replace('_', ' ')) })
            addAll(mobs.await().map { SearchResult("Mob", it.id, it.name, it.category) })
            addAll(biomes.await().map { SearchResult("Biome", it.id, it.name, it.category) })
            addAll(enchants.await().map { SearchResult("Enchantment", it.id, it.name, it.target) })
            addAll(potions.await().map { SearchResult("Potion", it.id, it.name, it.category) })
            addAll(trades.await().map { SearchResult("Trade", it.profession, "${it.sellItem.replace('_', ' ')} (${it.levelName})", it.profession) })
            addAll(structures.await().map { SearchResult("Structure", it.id, it.name, it.dimension) })
            addAll(items.await().map { SearchResult("Item", it.id, it.name, it.category) })
            addAll(advancements.await().map { SearchResult("Advancement", it.id, it.name, it.category) })
            addAll(commands.await().map { SearchResult("Command", it.id, it.name, it.category) })
        }
    }

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
    "Advancement" -> 9
    "Command"     -> 10
    else          -> 0
}

private fun typeIcon(type: String, id: String): SpyglassIcon = when (type) {
    "Block"       -> ItemTextures.get(id) ?: PixelIcons.Blocks
    "Item"        -> ItemTextures.get(id) ?: PixelIcons.Item
    "Recipe"      -> ItemTextures.get(id) ?: PixelIcons.Crafting
    "Mob"         -> MobTextures.get(id) ?: PixelIcons.Mob
    "Biome"       -> BiomeTextures.get(id) ?: PixelIcons.Biome
    "Enchantment" -> EnchantTextures.get(id) ?: PixelIcons.Enchant
    "Potion"      -> PotionTextures.get(id) ?: PixelIcons.Potion
    "Trade"       -> PixelIcons.Trade
    "Structure"   -> StructureTextures.get(id) ?: PixelIcons.Structure
    "Advancement" -> PixelIcons.Advancement
    "Command"     -> PixelIcons.Command
    else          -> PixelIcons.Search
}

@Composable
private fun typeColor(type: String) = when (type) {
    "Block"       -> MaterialTheme.colorScheme.onSurfaceVariant
    "Recipe"      -> MaterialTheme.colorScheme.primary
    "Mob"         -> NetherRed
    "Biome"       -> Emerald
    "Enchantment" -> EnderPurple
    "Potion"      -> PotionBlue
    "Trade"       -> Emerald
    "Structure"   -> MaterialTheme.colorScheme.primary
    "Item"        -> MaterialTheme.colorScheme.primary
    "Advancement" -> Emerald
    "Command"     -> PotionBlue
    else          -> MaterialTheme.colorScheme.secondary
}

@Composable
fun SearchScreen(
    onResultTap: (tab: Int, id: String) -> Unit = { _, _ -> },
    vm: SearchViewModel = viewModel(),
) {
    val query   by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text(stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.secondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        if (query.length < 2 && results.isEmpty()) {
            EmptyState(
                icon     = PixelIcons.Search,
                title    = stringResource(R.string.search_empty_title),
                subtitle = stringResource(R.string.search_empty_subtitle),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(results, key = { index, it -> "${it.type}_${index}_${it.id}" }) { _, r ->
                    val icon = typeIcon(r.type, r.id)
                    BrowseListItem(
                        headline    = r.name,
                        supporting  = "",
                        leadingIcon = icon,
                        leadingIconTint = if (icon is SpyglassIcon.Drawable) androidx.compose.ui.graphics.Color.Unspecified else typeColor(r.type),
                        modifier    = Modifier.clickable { onResultTap(browseTabForType(r.type), r.id) },
                        trailing    = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                CategoryBadge(label = r.type, color = typeColor(r.type))
                                if (r.detail.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(r.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        },
                    )
                }
                if (results.isEmpty() && query.length >= 2) {
                    item {
                        EmptyState(
                            icon     = PixelIcons.SearchOff,
                            title    = stringResource(R.string.search_no_results),
                            subtitle = stringResource(R.string.search_no_matches, query),
                        )
                    }
                }
            }
        }
    }
}
