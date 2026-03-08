package dev.spyglass.android.browse.crafting

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.core.ui.rememberHapticConfirm
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.data.ItemTags
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private val RECIPE_TYPES = listOf("all", "shaped", "shapeless", "smelting", "food", "smithing", "stonecutting", "loom")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CraftingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _type = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val type: StateFlow<String> = _type.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val recipes: StateFlow<List<RecipeEntity>> = combine(_query.debounce(200), _type) { q, t ->
        when (t) {
            "food" -> repo.recipesByItemCategory("food").map { list ->
                if (q.isBlank()) list else list.filter { it.outputItem.contains(q, ignoreCase = true) }
            }
            "smelting" -> {
                val foodIds = repo.recipesByItemCategory("food").map { list -> list.map { it.outputItem }.toSet() }
                combine(repo.searchRecipes(q), foodIds) { list, foods ->
                    list.filter { it.type.contains("smelting") && it.outputItem !in foods }
                }
            }
            "all" -> repo.searchRecipes(q)
            else -> repo.searchRecipes(q).map { list -> list.filter { it.type.contains(t) } }
        }
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<Map<String, RecipeEntity>> = repo.searchRecipes("")
        .map { list -> list.associateBy { it.outputItem } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteRecipes: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("recipe")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "recipe", displayName = displayName))
        }
    }

    fun setQuery(q: String) { _query.value = q }
    fun setType(t: String) { _type.value = t }
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
    val query       by vm.query.collectAsStateWithLifecycle()
    val type        by vm.type.collectAsStateWithLifecycle()
    val recipes     by vm.recipes.collectAsStateWithLifecycle()
    val expandedIds by vm.expandedIds.collectAsStateWithLifecycle()
    val allRecipes  by vm.allRecipes.collectAsStateWithLifecycle()
    val favoriteIds     by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoriteRecipes by vm.favoriteRecipes.collectAsStateWithLifecycle()
    val listState   = rememberLazyListState()
    val hapticConfirm = rememberHapticConfirm()
    val hapticClick = rememberHapticClick()

    // Deduplicate recipes that differ only by tag-member variants
    val dedupedRecipes = remember(recipes) {
        recipes.distinctBy { r ->
            val normalized = parseIngredientCounts(r).map { (id, count) ->
                (ItemTags.tagForIngredient(id, r.outputItem) ?: id) to count
            }.sortedBy { it.first }
            Triple(r.outputItem, r.type, normalized)
        }
    }

    // Auto-expand and scroll to target recipe from cross-reference
    LaunchedEffect(targetRecipeId) {
        if (targetRecipeId != null) {
            vm.setQuery("")
            vm.setType("all")
            snapshotFlow { dedupedRecipes }
                .first { it.isNotEmpty() }
            val idx = dedupedRecipes.indexOfFirst { it.id == targetRecipeId || it.outputItem == targetRecipeId }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1)
                vm.expandRecipe(targetRecipeId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search recipes\u2026", color = MaterialTheme.colorScheme.secondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        // Type filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(RECIPE_TYPES, key = { it }) { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { hapticClick(); vm.setType(t) },
                    label = {
                        Text(
                            if (t == "all") stringResource(R.string.all) else t.replaceFirstChar { it.uppercase() },
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
                    icon = PixelIcons.Crafting,
                    title = "Recipes",
                    description = "Crafting recipes for every craftable item",
                    stat = "${dedupedRecipes.size} recipes",
                )
            }
            if (favoriteRecipes.isNotEmpty()) {
                item(key = "fav_header") {
                    SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
                }
                items(favoriteRecipes, key = { "fav_${it.id}" }) { fav ->
                    val isFav = fav.id in favoriteIds
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        leadingIcon = ItemTextures.get(fav.id) ?: PixelIcons.Crafting,
                        modifier = Modifier.clickable { vm.toggleExpanded(fav.id) },
                        trailing = {
                            IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
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
            items(dedupedRecipes, key = { it.id }) { r ->
                val isExpanded = r.id in expandedIds
                Column {
                    BrowseListItem(
                        headline    = "${r.outputCount}\u00D7 ${r.outputItem.substringAfterLast(':').replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                        supporting  = r.type.replace('_', ' '),
                        leadingIcon = ItemTextures.get(r.outputItem) ?: PixelIcons.Crafting,
                        modifier    = Modifier.clickable { vm.toggleExpanded(r.id) },
                        trailing    = {
                            val isFav = r.id in favoriteIds
                            IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(r.id, r.outputItem.substringAfterLast(':').replace('_', ' ').replaceFirstChar { it.uppercase() }) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = stringResource(R.string.favorite),
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                    val reduceMotion = LocalReduceAnimations.current
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = if (reduceMotion) expandVertically(snap()) else expandVertically(),
                        exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(),
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
    val recipesForItem   by vm.recipesForItem(outputItem).collectAsStateWithLifecycle(initialValue = emptyList())
    val recipesUsingItem by vm.recipesUsingItem(outputItem).collectAsStateWithLifecycle(initialValue = emptyList())

    ItemDetailPager(
        itemId = outputItem,
        recipesForItem = recipesForItem,
        recipesUsingItem = recipesUsingItem,
        allRecipes = allRecipes,
        onItemTap = onItemTap,
        onBiomeTap = onBiomeTap,
    )
}

