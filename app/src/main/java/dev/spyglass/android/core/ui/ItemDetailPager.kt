package dev.spyglass.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spyglass.android.data.BiomeResourceMap
import dev.spyglass.android.data.ItemTags
import dev.spyglass.android.data.db.entities.RecipeEntity
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlin.math.ceil

// ── Data model for chain calculation ────────────────────────────────────────

data class ChainStep(
    val itemId: String,
    val quantity: Long,
    val craftsNeeded: Long,
    val recipe: RecipeEntity?,
    val biomes: List<String> = emptyList(),
)

// ── Chain calculator algorithm ──────────────────────────────────────────────

fun calculateChain(
    targetItem: String,
    targetCount: Long,
    recipes: Map<String, RecipeEntity>,
): List<ChainStep> {
    // Accumulate total quantities needed per item across all branches
    val quantities = mutableMapOf<String, Long>()    // total items needed
    val crafts = mutableMapOf<String, Long>()         // total crafts needed
    val recipeMap = mutableMapOf<String, RecipeEntity?>()
    val biomeMap = mutableMapOf<String, List<String>>()
    val order = mutableListOf<String>()               // insertion order for display

    fun trace(itemId: String, needed: Long, ancestors: Set<String>) {
        if (itemId in ancestors || needed <= 0) return // cycle detection only

        val recipe = recipes[itemId]
        if (recipe == null || recipe.type == "found") {
            quantities[itemId] = (quantities[itemId] ?: 0L) + needed
            if (itemId !in recipeMap) {
                recipeMap[itemId] = null
                biomeMap[itemId] = BiomeResourceMap.biomesForItem(itemId)
                order.add(itemId)
            }
            return
        }

        val outputCount = recipe.outputCount.coerceAtLeast(1)
        val craftsNeeded = ceil(needed.toDouble() / outputCount).toLong()

        quantities[itemId] = (quantities[itemId] ?: 0L) + needed
        crafts[itemId] = (crafts[itemId] ?: 0L) + craftsNeeded
        if (itemId !in recipeMap) {
            recipeMap[itemId] = recipe
            order.add(itemId)
        }

        val ingredients = parseIngredientCounts(recipe)
        for ((ingredientId, countPerCraft) in ingredients) {
            trace(ingredientId, craftsNeeded * countPerCraft, ancestors + itemId)
        }
    }

    trace(targetItem, targetCount, emptySet())

    return order.map { itemId ->
        ChainStep(
            itemId = itemId,
            quantity = quantities[itemId] ?: 0L,
            craftsNeeded = crafts[itemId] ?: 0L,
            recipe = recipeMap[itemId],
            biomes = biomeMap[itemId] ?: emptyList(),
        )
    }
}

private fun parseIngredientCounts(recipe: RecipeEntity): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    runCatching {
        val json = Json.parseToJsonElement(recipe.ingredientsJson)
        when {
            recipe.type.contains("shaped") -> {
                // 2D array for shaped recipes
                json.jsonArray.forEach { row ->
                    if (row is JsonArray) {
                        row.forEach { cell ->
                            val id = cell.jsonPrimitive.contentOrNull
                            if (!id.isNullOrBlank()) counts[id] = (counts[id] ?: 0) + 1
                        }
                    }
                }
            }
            else -> {
                // flat array for shapeless/smelting
                json.jsonArray.forEach { elem ->
                    val id = elem.jsonPrimitive.contentOrNull
                    if (!id.isNullOrBlank()) counts[id] = (counts[id] ?: 0) + 1
                }
            }
        }
    }
    return counts
}

private fun formatItemName(id: String): String =
    id.substringAfterLast(':').replace('_', ' ').replaceFirstChar { it.uppercase() }

// ── Main pager composable ────────────────────────────────────────────────

@Composable
fun ItemDetailPager(
    itemId: String,
    recipesForItem: List<RecipeEntity>,
    recipesUsingItem: List<RecipeEntity>,
    allRecipes: Map<String, RecipeEntity>,
    onItemTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .border(1.dp, Stone700, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        // Tab indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceMid, RoundedCornerShape(6.dp))
                .padding(2.dp),
        ) {
            listOf("Recipe", "Uses (${recipesUsingItem.size})").forEachIndexed { i, label ->
                val isSelected = pagerState.currentPage == i
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Gold else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(4.dp),
                        )
                        .height(32.dp),
                ) {
                    TextButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Background else Stone300,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Pager
        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> RecipePage(itemId, recipesForItem, allRecipes, onItemTap, onBiomeTap)
                1 -> UsesPage(recipesUsingItem, onItemTap)
            }
        }
    }
}

// ── Recipe page ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipePage(
    itemId: String,
    recipes: List<RecipeEntity>,
    allRecipes: Map<String, RecipeEntity>,
    onItemTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (recipes.isEmpty()) {
            // No recipe — show biome sources
            val biomes = BiomeResourceMap.biomesForItem(itemId)
            Text("No crafting recipe", style = MaterialTheme.typography.bodyMedium, color = Stone500)
            if (biomes.isNotEmpty()) {
                Text("Found in", style = MaterialTheme.typography.labelSmall, color = Gold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    biomes.forEach { biomeId ->
                        AssistChip(
                            onClick = { onBiomeTap(biomeId) },
                            label = { Text(formatItemName(biomeId), style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Emerald,
                                containerColor = Emerald.copy(alpha = 0.12f),
                            ),
                            border = null,
                        )
                    }
                }
            }
        } else {
            recipes.forEach { recipe ->
                // Crafting grid visualization
                if (recipe.type.contains("shaped")) {
                    CraftingGridView(recipe, onItemTap)
                }

                // Ingredients list
                val ingredients = parseIngredientCounts(recipe)
                if (ingredients.isNotEmpty()) {
                    Text("Ingredients", style = MaterialTheme.typography.labelSmall, color = Gold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ingredients.forEach { (id, count) ->
                            val label = "$count× ${formatItemName(id)}"
                            AssistChip(
                                onClick = { onItemTap(id) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = Stone100,
                                    containerColor = Stone700.copy(alpha = 0.5f),
                                ),
                                border = null,
                            )
                        }
                    }
                }

                // Recipe type badge
                Text(
                    recipe.type.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone500,
                )

                SpyglassDivider()

                // Chain calculator
                ChainCalculatorSection(itemId, allRecipes, onItemTap, onBiomeTap)
            }
        }
    }
}

// ── Crafting grid visualization (delegates to shared TextureCraftingGrid) ───

@Composable
private fun CraftingGridView(recipe: RecipeEntity, onItemTap: (String) -> Unit) {
    TextureCraftingGrid(recipe = recipe, onItemTap = onItemTap)
}

// ── Uses page ────────────────────────────────────────────────────────

@Composable
private fun UsesPage(
    recipesUsingItem: List<RecipeEntity>,
    onItemTap: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (recipesUsingItem.isEmpty()) {
            Text(
                "No known uses as ingredient",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone500,
            )
        } else {
            recipesUsingItem.forEach { recipe ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(6.dp))
                        .border(0.5.dp, Stone700, RoundedCornerShape(6.dp))
                        .clickable { onItemTap(recipe.outputItem) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Output item icon
                    val outputTexture = ItemTextures.get(recipe.outputItem)
                    if (outputTexture != null) {
                        SpyglassIconImage(
                            outputTexture, contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${recipe.outputCount}× ${formatItemName(recipe.outputItem)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Stone100,
                        )
                        Text(
                            recipe.type.replace('_', ' '),
                            style = MaterialTheme.typography.bodySmall,
                            color = Stone500,
                        )
                    }
                    Text("→", color = Gold, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// ── Chain calculator section ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChainCalculatorSection(
    itemId: String,
    allRecipes: Map<String, RecipeEntity>,
    onItemTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
) {
    var quantityInput by remember { mutableStateOf("") }
    val quantity = quantityInput.toLongOrNull() ?: 0L

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Chain Calculator", style = MaterialTheme.typography.labelSmall, color = Gold)

        OutlinedTextField(
            value = quantityInput,
            onValueChange = { quantityInput = it.filter { c -> c.isDigit() } },
            label = { Text("How many?") },
            placeholder = { Text("e.g. 55", color = Stone500) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = Stone700,
                focusedLabelColor = Gold,
                unfocusedLabelColor = Stone500,
                cursorColor = Gold,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (quantity > 0) {
            val chain = remember(itemId, quantity) {
                calculateChain(itemId, quantity, allRecipes)
            }

            if (chain.isNotEmpty()) {
                // Split into two tiers: crafted ingredients vs raw materials
                val craftedSteps = chain.filter { it.recipe != null }
                val rawSteps = chain.filter { it.recipe == null }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background, RoundedCornerShape(6.dp))
                        .border(0.5.dp, Stone700, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Top tier — crafted ingredients
                    if (craftedSteps.isNotEmpty()) {
                        Text("Craft", style = MaterialTheme.typography.labelSmall, color = Gold)
                        craftedSteps.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemTap(step.itemId) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val tex = ItemTextures.get(step.itemId)
                                    if (tex != null) {
                                        SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        "${formatItemName(step.itemId)} × ${step.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Gold,
                                    )
                                }
                                Text(
                                    "${step.craftsNeeded} crafts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Stone500,
                                )
                            }
                        }
                    }

                    // Bottom tier — raw materials
                    if (rawSteps.isNotEmpty()) {
                        if (craftedSteps.isNotEmpty()) SpyglassDivider()
                        Text("Gather", style = MaterialTheme.typography.labelSmall, color = Emerald)
                        rawSteps.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemTap(step.itemId) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val tex = ItemTextures.get(step.itemId)
                                    if (tex != null) {
                                        SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        "${formatItemName(step.itemId)} × ${step.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Emerald,
                                    )
                                }
                                if (step.biomes.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        step.biomes.forEach { biomeId ->
                                            Text(
                                                formatItemName(biomeId),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Stone500,
                                                modifier = Modifier.clickable { onBiomeTap(biomeId) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
