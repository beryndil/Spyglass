package dev.spyglass.android.calculators.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

private data class FoodItem(
    val name: String,
    val hunger: Int,            // hunger points restored (half shanks)
    val saturation: Float,      // saturation points restored
    val category: String,       // crop, meat, fish, crafted, other
    val effect: String = "",    // special effects
    val stackSize: Int = 64,
)

// All MC Java 1.21.4 food items with accurate hunger + saturation values
private val ALL_FOODS = listOf(
    // Crops & Plants
    FoodItem("Apple", 4, 2.4f, "crop"),
    FoodItem("Golden Apple", 4, 9.6f, "crafted", "Absorption I (2:00), Regeneration II (0:05)"),
    FoodItem("Enchanted Golden Apple", 4, 9.6f, "other", "Absorption IV (2:00), Regeneration II (0:20), Resistance (5:00), Fire Resistance (5:00)", stackSize = 64),
    FoodItem("Melon Slice", 2, 1.2f, "crop"),
    FoodItem("Sweet Berries", 2, 0.4f, "crop"),
    FoodItem("Glow Berries", 2, 0.4f, "crop"),
    FoodItem("Chorus Fruit", 4, 2.4f, "other", "Random teleportation"),
    FoodItem("Carrot", 3, 3.6f, "crop"),
    FoodItem("Golden Carrot", 6, 14.4f, "crafted"),
    FoodItem("Potato", 1, 0.6f, "crop"),
    FoodItem("Baked Potato", 5, 6.0f, "crafted"),
    FoodItem("Poisonous Potato", 2, 1.2f, "crop", "60% chance Poison (0:05)"),
    FoodItem("Beetroot", 1, 1.2f, "crop"),
    FoodItem("Dried Kelp", 1, 0.6f, "crop"),

    // Raw Meat
    FoodItem("Raw Beef", 3, 1.8f, "meat"),
    FoodItem("Raw Porkchop", 3, 1.8f, "meat"),
    FoodItem("Raw Chicken", 2, 1.2f, "meat", "30% chance Hunger (0:30)"),
    FoodItem("Raw Mutton", 2, 1.2f, "meat"),
    FoodItem("Raw Rabbit", 3, 1.8f, "meat"),

    // Cooked Meat
    FoodItem("Steak", 8, 12.8f, "meat"),
    FoodItem("Cooked Porkchop", 8, 12.8f, "meat"),
    FoodItem("Cooked Chicken", 6, 7.2f, "meat"),
    FoodItem("Cooked Mutton", 6, 9.6f, "meat"),
    FoodItem("Cooked Rabbit", 5, 6.0f, "meat"),

    // Fish
    FoodItem("Raw Cod", 2, 0.4f, "fish"),
    FoodItem("Raw Salmon", 2, 0.4f, "fish"),
    FoodItem("Cooked Cod", 5, 6.0f, "fish"),
    FoodItem("Cooked Salmon", 6, 9.6f, "fish"),
    FoodItem("Tropical Fish", 1, 0.2f, "fish"),
    FoodItem("Pufferfish", 1, 0.2f, "fish", "Hunger III (0:15), Nausea (0:15), Poison II (1:00)"),

    // Crafted Foods
    FoodItem("Bread", 5, 6.0f, "crafted"),
    FoodItem("Cookie", 2, 0.4f, "crafted"),
    FoodItem("Pumpkin Pie", 8, 4.8f, "crafted"),
    FoodItem("Cake (per slice)", 2, 0.4f, "crafted", "7 slices total = 14 hunger, 2.8 saturation", stackSize = 1),
    FoodItem("Mushroom Stew", 6, 7.2f, "crafted", stackSize = 1),
    FoodItem("Beetroot Soup", 6, 7.2f, "crafted", stackSize = 1),
    FoodItem("Rabbit Stew", 10, 12.0f, "crafted", stackSize = 1),
    FoodItem("Suspicious Stew", 6, 7.2f, "crafted", "Effect depends on flower used", stackSize = 1),

    // Other
    FoodItem("Rotten Flesh", 4, 0.8f, "other", "80% chance Hunger (0:30)"),
    FoodItem("Spider Eye", 2, 3.2f, "other", "Poison (0:05)"),
    FoodItem("Honey Bottle", 6, 1.2f, "other", "Clears Poison effect", stackSize = 16),
    FoodItem("Milk Bucket", 0, 0.0f, "other", "Clears all status effects", stackSize = 1),
)

private enum class SortMode(val label: String) {
    NAME("Name"),
    HUNGER("Hunger"),
    SATURATION("Saturation"),
    EFFICIENCY("Efficiency"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodScreen() {
    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("all") }
    var sortMode by remember { mutableStateOf(SortMode.SATURATION) }

    val categories = listOf("all", "crop", "meat", "fish", "crafted", "other")

    val filteredFoods = remember(query, categoryFilter, sortMode) {
        ALL_FOODS
            .filter { food ->
                (categoryFilter == "all" || food.category == categoryFilter) &&
                (query.isBlank() || food.name.contains(query, ignoreCase = true))
            }
            .sortedByDescending { food ->
                when (sortMode) {
                    SortMode.NAME -> 0f // sorted alphabetically below
                    SortMode.HUNGER -> food.hunger.toFloat()
                    SortMode.SATURATION -> food.saturation
                    SortMode.EFFICIENCY -> if (food.hunger > 0) food.saturation / food.hunger else 0f
                }
            }
            .let { list ->
                if (sortMode == SortMode.NAME) list.sortedBy { it.name }
                else list
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("Search food\u2026", color = MaterialTheme.colorScheme.secondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        // Category filters
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = { categoryFilter = cat },
                    label = { Text(if (cat == "all") stringResource(R.string.all) else cat.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // Sort options
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Sort:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.align(Alignment.CenterVertically))
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = sortMode == mode,
                    onClick = { sortMode = mode },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Item,
                    title = "Food & Saturation",
                    description = "Complete food reference with hunger, saturation, and special effects. Sort by saturation to find the best foods.",
                    stat = "${filteredFoods.size} foods",
                )
            }

            items(filteredFoods, key = { it.name }) { food ->
                FoodItemCard(food)
            }

            item {
                Spacer(Modifier.height(4.dp))
                ResultCard {
                    Text("HOW SATURATION WORKS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Saturation is a hidden stat that determines how long before hunger starts decreasing. Higher saturation = longer before you get hungry again. Your saturation cannot exceed your current hunger level.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SpyglassDivider()
                    Text("BEST FOODS BY CATEGORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    StatRow("Best Overall", "Golden Carrot (14.4 sat)")
                    StatRow("Best Cooked Meat", "Steak / Porkchop (12.8 sat)")
                    StatRow("Best Fish", "Cooked Salmon (9.6 sat)")
                    StatRow("Best Crop", "Baked Potato (6.0 sat)")
                    StatRow("Best Stew", "Rabbit Stew (12.0 sat)")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FoodItemCard(food: FoodItem) {
    val satColor = when {
        food.saturation >= 10f -> Emerald
        food.saturation >= 5f -> MaterialTheme.colorScheme.primary
        food.saturation >= 2f -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.secondary
    }

    BrowseListItem(
        headline = food.name,
        supporting = buildString {
            append("${food.hunger / 2.0} shanks")
            if (food.effect.isNotBlank()) append(" \u2022 ${food.effect}")
        },
        supportingMaxLines = 2,
        leadingIcon = PixelIcons.Item,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${"%.1f".format(food.saturation)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = satColor,
                )
                Text(
                    "saturation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        },
    )
}
