package dev.spyglass.android.browse.crafting

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CraftingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val recipes: StateFlow<List<RecipeEntity>> = _query.debounce(200)
        .flatMapLatest { repo.searchRecipes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<Map<String, RecipeEntity>> = repo.searchRecipes("")
        .map { list -> list.associateBy { it.outputItem } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setQuery(q: String) { _query.value = q }
    fun toggleExpanded(id: String) {
        _expandedIds.value = _expandedIds.value.let { if (id in it) it - id else it + id }
    }
    fun expandRecipe(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun recipesForItem(itemId: String) = repo.recipesForItem(itemId)
    fun recipesUsingItem(itemId: String) = repo.recipesUsingIngredient(itemId)
}

@Composable
fun CraftingScreen(
    targetRecipeId: String? = null,
    onItemTap: (String) -> Unit = {},
    onBiomeTap: (String) -> Unit = {},
    vm: CraftingViewModel = viewModel(),
) {
    val query       by vm.query.collectAsState()
    val recipes     by vm.recipes.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val allRecipes  by vm.allRecipes.collectAsState()
    val listState   = rememberLazyListState()

    // Auto-expand and scroll to target recipe from cross-reference
    LaunchedEffect(targetRecipeId, recipes) {
        if (targetRecipeId != null && recipes.isNotEmpty()) {
            vm.expandRecipe(targetRecipeId)
            val idx = recipes.indexOfFirst { it.id == targetRecipeId || it.outputItem == targetRecipeId }
            if (idx >= 0) listState.animateScrollToItem(idx + 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search recipes…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Crafting,
                    title = "Recipes",
                    description = "Crafting recipes for every craftable item",
                    stat = "${recipes.size} recipes",
                )
            }
            items(recipes, key = { it.id }) { r ->
                val isExpanded = r.id in expandedIds
                Column {
                    BrowseListItem(
                        headline    = "${r.outputCount}× ${r.outputItem.substringAfterLast(':').replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                        supporting  = r.type.replace('_', ' '),
                        leadingIcon = ItemTextures.get(r.outputItem) ?: PixelIcons.Crafting,
                        modifier    = Modifier.clickable { vm.toggleExpanded(r.id) },
                        trailing    = {
                            RecipeGridPreview(r)
                        },
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        RecipeDetailContent(r.outputItem, allRecipes, vm, onItemTap, onBiomeTap)
                    }
                }
            }
            if (recipes.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No recipes found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}

@Composable
private fun RecipeDetailContent(
    outputItem: String,
    allRecipes: Map<String, RecipeEntity>,
    vm: CraftingViewModel,
    onItemTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
) {
    val recipesForItem   by vm.recipesForItem(outputItem).collectAsState(initial = emptyList())
    val recipesUsingItem by vm.recipesUsingItem(outputItem).collectAsState(initial = emptyList())

    ItemDetailPager(
        itemId = outputItem,
        recipesForItem = recipesForItem,
        recipesUsingItem = recipesUsingItem,
        allRecipes = allRecipes,
        onItemTap = onItemTap,
        onBiomeTap = onBiomeTap,
    )
}

@Composable
private fun RecipeGridPreview(r: RecipeEntity) {
    if (r.type.contains("shaped") && r.ingredientsJson.isNotBlank()) {
        val cells = runCatching {
            Json.parseToJsonElement(r.ingredientsJson).jsonArray.map {
                if (it is JsonNull) null else it.jsonPrimitive.contentOrNull?.substringAfterLast(':')?.take(2)?.uppercase()
            }
        }.getOrElse { emptyList() }

        if (cells.size == 9) {
            Column(modifier = Modifier.size(72.dp)) {
                for (row in 0..2) {
                    Row {
                        for (col in 0..2) {
                            val cell = cells.getOrNull(row * 3 + col)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(24.dp)
                                    .background(if (cell != null) SurfaceMid else Background, RoundedCornerShape(2.dp))
                                    .border(0.5.dp, Stone700, RoundedCornerShape(2.dp)),
                            ) {
                                if (cell != null) Text(cell, fontSize = 7.sp, color = Stone300, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}
