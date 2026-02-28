package dev.spyglass.android.calculators.loot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.spyglass.android.core.ui.*

private data class StructureLoot(
    val name: String,
    val dimension: String,
    val exclusiveItems: List<String>,
    val notableItems: List<String>,
)

private val STRUCTURE_LOOT = listOf(
    StructureLoot(
        "Trial Chambers", "overworld",
        listOf("Heavy Core (Mace crafting)", "Trial Keys", "Ominous Trial Keys", "Ominous Bottles", "Breeze Rods", "Wind Charges"),
        listOf("Flow Armor Trim (22.5%)", "Bolt Armor Trim (6.3%)", "Density/Breach/Wind Burst enchanted books"),
    ),
    StructureLoot(
        "Ancient City", "overworld",
        listOf("Echo Shards", "Disc Fragments (music disc 5)", "Swift Sneak enchanted books"),
        listOf("Silence Armor Trim (1.2% - rarest)", "Ward Armor Trim (5%)", "Sculk catalysts/sensors", "Enchanted diamond gear"),
    ),
    StructureLoot(
        "Bastion Remnant", "nether",
        listOf("Pigstep Music Disc", "Snout Banner Pattern", "Netherite Upgrade Templates"),
        listOf("Snout Armor Trim (8-10%)", "Ancient Debris", "Gold blocks/ingots", "Enchanted diamond gear"),
    ),
    StructureLoot(
        "End City", "end",
        listOf("Elytra (ONLY source - end ships)"),
        listOf("Spire Armor Trim (6.7%)", "Shulker Shells", "Enchanted diamond tools/armor", "Diamond horse armor"),
    ),
    StructureLoot(
        "Woodland Mansion", "overworld",
        listOf("Totem of Undying (from Evokers)"),
        listOf("Vex Armor Trim (50%)", "Diamond blocks (secret rooms)", "Enchanted books", "Dark oak logs"),
    ),
    StructureLoot(
        "Stronghold", "overworld",
        listOf("Eyes of Ender (end portal)"),
        listOf("Eye Armor Trim (100% in library)", "Enchanted books", "Iron/diamond gear", "Golden apples"),
    ),
    StructureLoot(
        "Ocean Monument", "overworld",
        listOf("Sponges (wet, from ceilings)"),
        listOf("Tide Armor Trim (Elder Guardian 20%)", "Gold blocks (8 in treasure room)", "Prismarine/Sea lanterns"),
    ),
    StructureLoot(
        "Nether Fortress", "nether",
        listOf("Nether Wart (17.9%)", "Blaze Rods (from Blazes)"),
        listOf("Rib Armor Trim (6.7%)", "Saddles (33.3%)", "Golden horse armor (27.4%)", "Diamonds"),
    ),
    StructureLoot(
        "Desert Pyramid", "overworld",
        emptyList(),
        listOf("Dune Armor Trim (14.3%)", "Golden Apples (regular + enchanted)", "TNT trap", "Enchanted books", "Horse armor", "Diamonds/emeralds"),
    ),
    StructureLoot(
        "Jungle Pyramid", "overworld",
        emptyList(),
        listOf("Wild Armor Trim (33.3%)", "Enchanted books (level 30)", "Bamboo", "Diamonds/emeralds", "Horse armor"),
    ),
    StructureLoot(
        "Shipwreck", "overworld",
        listOf("Buried Treasure Maps"),
        listOf("Coast Armor Trim (16.7%)", "Iron ingots (97%)", "Diamonds/emeralds", "Enchanted books"),
    ),
    StructureLoot(
        "Trail Ruins", "overworld",
        listOf("Pottery Sherds (archaeology)"),
        listOf("Wayfinder Trim (8.3%)", "Raiser Trim (8.3%)", "Shaper Trim (8.3%)", "Host Trim (8.3%)"),
    ),
    StructureLoot(
        "Pillager Outpost", "overworld",
        listOf("Goat Horns"),
        listOf("Sentry Armor Trim (25%)", "Crossbow (50%)", "Bottles of Enchanting (61%)", "Dark oak logs"),
    ),
    StructureLoot(
        "Mineshaft", "overworld",
        emptyList(),
        listOf("Golden Apples", "Enchanted books", "Powered/Detector Rails", "Name tags", "Iron/gold ingots"),
    ),
)

private fun dimensionColor(dim: String): Color = when (dim) {
    "nether" -> NetherRed
    "end" -> EnderPurple
    else -> Emerald
}

private fun dimensionLabel(dim: String): String = when (dim) {
    "overworld" -> "Overworld"
    "nether" -> "Nether"
    "end" -> "The End"
    else -> dim.replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LootScreen() {
    var query by remember { mutableStateOf("") }
    var dimensionFilter by remember { mutableStateOf("all") }

    val filtered = remember(query, dimensionFilter) {
        STRUCTURE_LOOT.filter { loot ->
            (dimensionFilter == "all" || loot.dimension == dimensionFilter) &&
            (query.isBlank() || loot.name.contains(query, ignoreCase = true) ||
                loot.exclusiveItems.any { it.contains(query, ignoreCase = true) } ||
                loot.notableItems.any { it.contains(query, ignoreCase = true) })
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("Search loot or structures\u2026", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("all", "overworld", "nether", "end").forEach { dim ->
                FilterChip(
                    selected = dimensionFilter == dim,
                    onClick = { dimensionFilter = dim },
                    label = { Text(if (dim == "all") "All" else dimensionLabel(dim), style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Structure,
                    title = "Structure Loot",
                    description = "Notable and exclusive loot items found in each Minecraft structure. Find the right structure for what you need.",
                    stat = "${filtered.size} structures",
                )
            }

            items(filtered, key = { it.name }) { loot ->
                StructureLootCard(loot)
            }

            item {
                Spacer(Modifier.height(4.dp))
                ResultCard {
                    Text("RAREST EXCLUSIVE ITEMS", style = MaterialTheme.typography.labelSmall, color = Gold)
                    Spacer(Modifier.height(4.dp))
                    StatRow("Elytra", "End Cities only")
                    StatRow("Echo Shards", "Ancient Cities only")
                    StatRow("Heavy Core", "Trial Chambers only")
                    StatRow("Pigstep Disc", "Bastion Remnants only")
                    StatRow("Silence Trim", "Ancient Cities (1.2%)")
                    StatRow("Swift Sneak", "Ancient Cities only")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StructureLootCard(loot: StructureLoot) {
    val dimColor = dimensionColor(loot.dimension)

    ResultCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(loot.name, style = MaterialTheme.typography.titleMedium, color = Stone100)
            CategoryBadge(label = dimensionLabel(loot.dimension), color = dimColor)
        }

        if (loot.exclusiveItems.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("EXCLUSIVE / RARE", style = MaterialTheme.typography.labelSmall, color = Gold)
            Spacer(Modifier.height(4.dp))
            loot.exclusiveItems.forEach { item ->
                Text(
                    "\u2b50 $item",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold,
                )
            }
        }

        if (loot.notableItems.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("NOTABLE LOOT", style = MaterialTheme.typography.labelSmall, color = Stone500)
            Spacer(Modifier.height(4.dp))
            loot.notableItems.forEach { item ->
                Text(
                    "\u2022 $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone300,
                )
            }
        }
    }
}
