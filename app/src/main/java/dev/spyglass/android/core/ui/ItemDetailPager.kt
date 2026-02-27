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
import dev.spyglass.android.data.CompostData
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
    val tagId: String? = null,
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
    val tagMap = mutableMapOf<String, String?>()       // key -> tagId if tag-collapsed
    val order = mutableListOf<String>()               // insertion order for display

    fun trace(itemId: String, needed: Long, ancestors: Set<String>, outputContext: String?) {
        if (itemId in ancestors || needed <= 0) return // cycle detection only

        // Tag collapsing: only treat as tag if the parent recipe is truly tag-based
        val tag = if (outputContext != null) {
            ItemTags.tagForIngredient(itemId, outputContext)
        } else null
        val key = tag ?: itemId

        val recipe = recipes[itemId]
        if (recipe == null || recipe.type == "found") {
            quantities[key] = (quantities[key] ?: 0L) + needed
            if (key !in recipeMap) {
                recipeMap[key] = null
                biomeMap[key] = BiomeResourceMap.biomesForItem(itemId)
                tagMap[key] = tag
                order.add(key)
            }
            return
        }

        val outputCount = recipe.outputCount.coerceAtLeast(1)
        val craftsNeeded = ceil(needed.toDouble() / outputCount).toLong()

        quantities[key] = (quantities[key] ?: 0L) + needed
        crafts[key] = (crafts[key] ?: 0L) + craftsNeeded
        if (key !in recipeMap) {
            recipeMap[key] = recipe
            tagMap[key] = tag
            order.add(key)
        }

        val ingredients = parseIngredientCounts(recipe)
        for ((ingredientId, countPerCraft) in ingredients) {
            // If this item was tag-collapsed, propagate the grandparent context
            // so sub-ingredients also resolve as tags (e.g. stick → any planks → any logs)
            val nextContext = if (tag != null) outputContext else itemId
            trace(ingredientId, craftsNeeded * countPerCraft, ancestors + itemId, nextContext)
        }
    }

    trace(targetItem, targetCount, emptySet(), null)

    return order.map { key ->
        ChainStep(
            itemId = key,
            quantity = quantities[key] ?: 0L,
            craftsNeeded = crafts[key] ?: 0L,
            recipe = recipeMap[key],
            biomes = biomeMap[key] ?: emptyList(),
            tagId = tagMap[key],
        )
    }
}

data class CraftingPlanStep(
    val itemId: String,
    val quantity: Long,
    val craftsNeeded: Long,
    val recipe: RecipeEntity?,
    val depth: Int,
    val tagId: String? = null,
)

fun consolidateCraftingPlan(
    items: List<Pair<String, Int>>,
    recipes: Map<String, RecipeEntity>,
): List<CraftingPlanStep> {
    val totals = mutableMapOf<String, Long>()
    val craftTotals = mutableMapOf<String, Long>()
    val recipeForItem = mutableMapOf<String, RecipeEntity?>()
    val tagForKey = mutableMapOf<String, String?>()

    for ((itemId, qty) in items) {
        val chain = calculateChain(itemId, qty.toLong(), recipes)
        for (step in chain) {
            totals[step.itemId] = (totals[step.itemId] ?: 0L) + step.quantity
            craftTotals[step.itemId] = (craftTotals[step.itemId] ?: 0L) + step.craftsNeeded
            recipeForItem.putIfAbsent(step.itemId, step.recipe)
            tagForKey.putIfAbsent(step.itemId, step.tagId)
        }
    }

    val depthCache = mutableMapOf<String, Int>()
    fun itemDepth(id: String, visiting: Set<String> = emptySet()): Int {
        depthCache[id]?.let { return it }
        if (id in visiting) return 0
        // For tags, resolve to a representative member for depth calculation
        val resolvedId = if (id.startsWith("#")) {
            ItemTags.membersOfTag(id).firstOrNull { it in recipes } ?: id
        } else id
        val recipe = recipes[resolvedId]
        if (recipe == null || recipe.type == "found") return 0.also { depthCache[id] = it }
        val ingredients = parseIngredientCounts(recipe)
        val maxChild = ingredients.keys.maxOfOrNull {
            val childKey = ItemTags.tagForIngredient(it, resolvedId) ?: it
            itemDepth(childKey, visiting + id)
        } ?: 0
        return (maxChild + 1).also { depthCache[id] = it }
    }

    return totals.entries
        .map { (id, qty) ->
            CraftingPlanStep(
                itemId = id,
                quantity = qty,
                craftsNeeded = craftTotals[id] ?: 0L,
                recipe = recipeForItem[id],
                depth = itemDepth(id),
                tagId = tagForKey[id],
            )
        }
        .sortedWith(compareBy({ it.depth }, { -it.quantity }))
}

internal fun parseIngredientCounts(recipe: RecipeEntity): Map<String, Int> {
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
    id.substringAfterLast(':').split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

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
            val usesCount = recipesUsingItem.size + if (CompostData.chanceFor(itemId) != null) 1 else 0
            listOf("Recipe", "Uses ($usesCount)").forEachIndexed { i, label ->
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
                1 -> UsesPage(itemId, recipesUsingItem, onItemTap)
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
            // Deduplicate recipes that differ only by tag-member variants
            // (e.g. 9 charcoal smelting recipes → 1 with rotating log icon)
            val deduped = recipes.distinctBy { recipe ->
                val normalized = parseIngredientCounts(recipe).map { (id, count) ->
                    (ItemTags.tagForIngredient(id, recipe.outputItem) ?: id) to count
                }.sortedBy { it.first }
                recipe.type to normalized
            }
            deduped.forEach { recipe ->
                // Crafting grid visualization
                if (recipe.type.contains("shaped")) {
                    CraftingGridView(recipe, onItemTap)
                }

                // Ingredients list
                val ingredients = parseIngredientCounts(recipe)
                if (ingredients.isNotEmpty()) {
                    // Merge tag members into single entries (only when recipe is tag-based)
                    val merged = mutableMapOf<String, Int>()
                    val tagIds = mutableMapOf<String, String?>()
                    ingredients.forEach { (id, count) ->
                        val tag = ItemTags.tagForIngredient(id, recipe.outputItem)
                        val key = tag ?: id
                        merged[key] = (merged[key] ?: 0) + count
                        tagIds.putIfAbsent(key, tag)
                    }
                    Text("Ingredients", style = MaterialTheme.typography.labelSmall, color = Gold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        merged.forEach { (key, count) ->
                            val tag = tagIds[key]
                            val name = if (tag != null) formatTagName(tag) else formatItemName(key)
                            AssistChip(
                                onClick = { if (tag == null) onItemTap(key) },
                                leadingIcon = if (tag != null) { {
                                    RotatingTagIcon(tag, modifier = Modifier.size(16.dp))
                                } } else null,
                                label = { Text("$count× $name", style = MaterialTheme.typography.labelSmall) },
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
    itemId: String,
    recipesUsingItem: List<RecipeEntity>,
    onItemTap: (String) -> Unit,
) {
    val compostChance = CompostData.chanceFor(itemId)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Composting info
        if (compostChance != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Emerald.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .border(0.5.dp, Emerald.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val composterIcon = BlockTextures.get("composter")
                if (composterIcon != null) {
                    SpyglassIconImage(composterIcon, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Compostable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Emerald,
                    )
                    Text(
                        "$compostChance% chance per item",
                        style = MaterialTheme.typography.bodySmall,
                        color = Stone500,
                    )
                }
            }
        }

        // Recipe uses
        if (recipesUsingItem.isEmpty() && compostChance == null) {
            Text(
                "No known uses",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone500,
            )
        }
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

// ── Chain step icon — rotating for tags, static for regular items ────

@Composable
private fun ChainStepIcon(step: ChainStep, modifier: Modifier = Modifier) {
    if (step.tagId != null) {
        RotatingTagIcon(step.tagId, modifier = modifier)
    } else {
        val tex = ItemTextures.get(step.itemId)
        if (tex != null) {
            SpyglassIconImage(tex, contentDescription = null, modifier = modifier)
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
                                    .clickable { if (step.tagId == null) onItemTap(step.itemId) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ChainStepIcon(step, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    val name = if (step.tagId != null) formatTagName(step.tagId) else formatItemName(step.itemId)
                                    Text(
                                        "$name × ${step.quantity}",
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
                                    .clickable { if (step.tagId == null) onItemTap(step.itemId) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ChainStepIcon(step, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    val name = if (step.tagId != null) formatTagName(step.tagId) else formatItemName(step.itemId)
                                    Text(
                                        "$name × ${step.quantity}",
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
