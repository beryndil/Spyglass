package dev.spyglass.android.browse.search

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                repo.searchBlocks(q),
                repo.searchMobs(q),
                repo.searchBiomes(q),
                repo.searchEnchants(q),
                repo.searchPotions(q),
            ) { blocks, mobs, biomes, enchants, potions ->
                buildList {
                    addAll(blocks.take(5).map   { SearchResult("Block",       it.id, it.name, it.category) })
                    addAll(mobs.take(5).map      { SearchResult("Mob",         it.id, it.name, it.category) })
                    addAll(biomes.take(5).map    { SearchResult("Biome",       it.id, it.name, it.category) })
                    addAll(enchants.take(5).map  { SearchResult("Enchantment", it.id, it.name, it.target) })
                    addAll(potions.take(5).map   { SearchResult("Potion",      it.id, it.name, it.category) })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
}

private fun typeIcon(type: String): ImageVector = when (type) {
    "Block"       -> Icons.Default.ViewInAr
    "Mob"         -> Icons.Default.Pets
    "Biome"       -> Icons.Default.Terrain
    "Enchantment" -> Icons.Default.AutoFixHigh
    "Potion"      -> Icons.Default.Science
    else          -> Icons.Default.Search
}

private fun typeColor(type: String) = when (type) {
    "Block"       -> Stone300
    "Mob"         -> NetherRed
    "Biome"       -> Emerald
    "Enchantment" -> EnderPurple
    "Potion"      -> PotionBlue
    else          -> Stone500
}

@Composable
fun SearchScreen(vm: SearchViewModel = viewModel()) {
    val query   by vm.query.collectAsState()
    val results by vm.results.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search all Minecraft data…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        if (query.length < 2 && results.isEmpty()) {
            EmptyState(
                icon     = Icons.Default.Search,
                title    = "Search everything",
                subtitle = "Type at least 2 characters to search blocks, mobs, biomes, enchantments, and potions.",
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
                            icon     = Icons.Default.SearchOff,
                            title    = "No results",
                            subtitle = "No matches for \"$query\"",
                        )
                    }
                }
            }
        }
    }
}
