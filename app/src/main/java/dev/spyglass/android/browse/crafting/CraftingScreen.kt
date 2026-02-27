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

private val RECIPE_TYPES = listOf("all", "shaped", "shapeless", "smelting", "smithing")

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
        repo.searchRecipes(q).map { list ->
            if (t == "all") list else list.filter { it.type.contains(t) }
        }
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<Map<String, RecipeEntity>> = repo.searchRecipes("")
        .map { list -> list.associateBy { it.outputItem } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
    val query       by vm.query.collectAsState()
    val type        by vm.type.collectAsState()
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
            placeholder = { Text("Search recipes\u2026", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        // Type filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(RECIPE_TYPES) { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { vm.setType(t) },
                    label = {
                        Text(
                            t.replaceFirstChar { it.uppercase() },
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
                    stat = "${recipes.size} recipes",
                )
            }
            items(recipes, key = { it.id }) { r ->
                val isExpanded = r.id in expandedIds
                Column {
                    BrowseListItem(
                        headline    = "${r.outputCount}\u00D7 ${r.outputItem.substringAfterLast(':').replace('_', ' ').replaceFirstChar { it.uppercase() }}",
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
        // Parse 2D array for shaped recipes — keep full item IDs for texture lookup
        val parsed = runCatching {
            Json.parseToJsonElement(r.ingredientsJson).jsonArray
        }.getOrElse { return }

        val origRows = parsed.size
        val origCols = runCatching {
            val first = parsed.firstOrNull()
            if (first is JsonArray) first.size else 1
        }.getOrDefault(1)

        val origCells = parsed.flatMap { row ->
            if (row is JsonArray) row.map { it.jsonPrimitive.contentOrNull }
            else listOf(row.jsonPrimitive.contentOrNull)
        }

        // Always render as 3×3 grid for shaped recipes
        val cells = (0 until 9).map { idx ->
            val r2 = idx / 3
            val c2 = idx % 3
            if (r2 < origRows && c2 < origCols) origCells.getOrNull(r2 * origCols + c2) else null
        }

        Column(modifier = Modifier.size(72.dp)) {
            for (row in 0..2) {
                Row {
                    for (col in 0..2) {
                        val cell = cells.getOrNull(row * 3 + col)
                        val texture = if (!cell.isNullOrBlank()) ItemTextures.get(cell) else null
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(24.dp)
                                .background(if (!cell.isNullOrBlank()) SurfaceMid else Background, RoundedCornerShape(2.dp))
                                .border(0.5.dp, Stone700, RoundedCornerShape(2.dp)),
                        ) {
                            if (texture != null) {
                                SpyglassIconImage(texture, contentDescription = null, modifier = Modifier.size(18.dp))
                            } else if (!cell.isNullOrBlank()) {
                                Text(cell.take(2).uppercase(), fontSize = 7.sp, color = Stone300, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    } else if (r.type.contains("smelting") && r.ingredientsJson.isNotBlank()) {
        // Smelting preview: input → output
        val inputId = runCatching {
            val arr = Json.parseToJsonElement(r.ingredientsJson).jsonArray
            arr.firstOrNull()?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        val inputTexture = inputId?.let { ItemTextures.get(it) }
        val outputTexture = ItemTextures.get(r.outputItem)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
                    .background(SurfaceMid, RoundedCornerShape(2.dp))
                    .border(0.5.dp, Stone700, RoundedCornerShape(2.dp)),
            ) {
                if (inputTexture != null) {
                    SpyglassIconImage(inputTexture, contentDescription = null, modifier = Modifier.size(18.dp))
                } else if (inputId != null) {
                    Text(inputId.take(2).uppercase(), fontSize = 7.sp, color = Stone300, textAlign = TextAlign.Center)
                }
            }
            Text("\u2192", color = Gold, style = MaterialTheme.typography.bodySmall)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
                    .background(SurfaceMid, RoundedCornerShape(2.dp))
                    .border(0.5.dp, Stone700, RoundedCornerShape(2.dp)),
            ) {
                if (outputTexture != null) {
                    SpyglassIconImage(outputTexture, contentDescription = null, modifier = Modifier.size(18.dp))
                } else {
                    Text(r.outputItem.take(2).uppercase(), fontSize = 7.sp, color = Stone300, textAlign = TextAlign.Center)
                }
            }
        }
    } else if (r.type.contains("shapeless") && r.ingredientsJson.isNotBlank()) {
        // Shapeless: show ingredients with textures
        val cellIds = runCatching {
            Json.parseToJsonElement(r.ingredientsJson).jsonArray.map {
                it.jsonPrimitive.contentOrNull
            }
        }.getOrElse { emptyList() }

        if (cellIds.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                cellIds.take(4).forEach { cellId ->
                    val texture = cellId?.let { ItemTextures.get(it) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(24.dp)
                            .background(if (cellId != null) SurfaceMid else Background, RoundedCornerShape(2.dp))
                            .border(0.5.dp, Stone700, RoundedCornerShape(2.dp)),
                    ) {
                        if (texture != null) {
                            SpyglassIconImage(texture, contentDescription = null, modifier = Modifier.size(18.dp))
                        } else if (cellId != null) {
                            Text(cellId.take(2).uppercase(), fontSize = 7.sp, color = Stone300, textAlign = TextAlign.Center)
                        }
                    }
                }
                if (cellIds.size > 4) {
                    Text("+${cellIds.size - 4}", fontSize = 7.sp, color = Stone500)
                }
            }
        }
    }
}
