package dev.spyglass.android.browse.blocks

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.BlockEntity
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BlocksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val blocks: StateFlow<List<BlockEntity>> = _query
        .debounce(200)
        .flatMapLatest { repo.searchBlocks(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Build a map of all recipes keyed by outputItem for chain calculation
    val allRecipes: StateFlow<Map<String, RecipeEntity>> = repo.searchRecipes("")
        .map { list -> list.associateBy { it.outputItem } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setQuery(q: String) { _query.value = q }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandBlock(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun recipesForItem(itemId: String) = repo.recipesForItem(itemId)
    fun recipesUsingItem(itemId: String) = repo.recipesUsingIngredient(itemId)
}

@Composable
fun BlocksScreen(
    targetBlockId: String? = null,
    onItemTap: (String) -> Unit = {},
    onBiomeTap: (String) -> Unit = {},
    vm: BlocksViewModel = viewModel(),
) {
    val query       by vm.query.collectAsState()
    val blocks      by vm.blocks.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val allRecipes  by vm.allRecipes.collectAsState()
    val listState   = rememberLazyListState()

    // Auto-expand and scroll to target block from cross-reference
    LaunchedEffect(targetBlockId, blocks) {
        if (targetBlockId != null && blocks.isNotEmpty()) {
            vm.expandBlock(targetBlockId)
            val idx = blocks.indexOfFirst { it.id == targetBlockId }
            if (idx >= 0) listState.animateScrollToItem(idx + 1) // +1 for intro header
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value         = query,
            onValueChange = vm::setQuery,
            placeholder   = { Text("Search blocks…", color = Stone500) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold, unfocusedBorderColor = Stone700,
                cursorColor = Gold,
            ),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Blocks,
                    title = "Blocks",
                    description = "Every placeable block with hardness and tool info",
                    stat = "${blocks.size} blocks",
                )
            }
            items(blocks, key = { it.id }) { b ->
                val isExpanded = b.id in expandedIds
                Column {
                    BrowseListItem(
                        headline    = b.name,
                        supporting  = b.id,
                        leadingIcon = ItemTextures.get(b.id) ?: PixelIcons.Blocks,
                        modifier    = Modifier.clickable { vm.toggleExpanded(b.id) },
                        trailing    = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                if (b.toolRequired.isNotBlank())
                                    Text(b.toolRequired, style = MaterialTheme.typography.bodySmall, color = Stone300)
                                Text("hardness ${b.hardness}", style = MaterialTheme.typography.bodySmall, color = Stone500)
                            }
                        },
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        BlockDetailContent(b.id, allRecipes, vm, onItemTap, onBiomeTap)
                    }
                }
            }
            if (blocks.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No blocks found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}

@Composable
private fun BlockDetailContent(
    blockId: String,
    allRecipes: Map<String, RecipeEntity>,
    vm: BlocksViewModel,
    onItemTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
) {
    val recipesForItem  by vm.recipesForItem(blockId).collectAsState(initial = emptyList())
    val recipesUsingItem by vm.recipesUsingItem(blockId).collectAsState(initial = emptyList())

    ItemDetailPager(
        itemId = blockId,
        recipesForItem = recipesForItem,
        recipesUsingItem = recipesUsingItem,
        allRecipes = allRecipes,
        onItemTap = onItemTap,
        onBiomeTap = onBiomeTap,
    )
}
