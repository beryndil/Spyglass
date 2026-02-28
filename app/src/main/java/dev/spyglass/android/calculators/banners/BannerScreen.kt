package dev.spyglass.android.calculators.banners

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.spyglass.android.core.ui.*

private data class PatternItem(
    val name: String,
    val rarity: String,
    val obtainMethod: String,
    val source: String,
)

private val PATTERN_ITEMS = listOf(
    PatternItem("Field Masoned", "Common", "Crafting", "Paper + Bricks"),
    PatternItem("Bordure Indented", "Common", "Crafting", "Paper + Vines"),
    PatternItem("Flower Charge", "Common", "Crafting", "Paper + Oxeye Daisy"),
    PatternItem("Globe", "Common", "Trading", "Master Cartographer (8 Emeralds)"),
    PatternItem("Creeper Charge", "Uncommon", "Crafting", "Paper + Creeper Head"),
    PatternItem("Snout", "Uncommon", "Loot", "Bastion Remnant (10.1% chance)"),
    PatternItem("Skull Charge", "Rare", "Crafting", "Paper + Wither Skeleton Skull"),
    PatternItem("Thing", "Rare", "Crafting", "Paper + Enchanted Golden Apple"),
    PatternItem("Flow", "Rare", "Loot", "Trial Chambers Ominous Vault (15%)"),
    PatternItem("Guster", "Rare", "Loot", "Trial Chambers Vault (4.2%)"),
)

private val LOOM_PATTERNS = listOf(
    "Base" to "basic",
    "Chief" to "basic",
    "Fess" to "basic",
    "Pale" to "basic",
    "Paly" to "basic",
    "Bend" to "diagonal",
    "Bend Sinister" to "diagonal",
    "Per Bend" to "diagonal",
    "Per Bend Sinister" to "diagonal",
    "Per Bend Inverted" to "diagonal",
    "Per Bend Inverted Sinister" to "diagonal",
    "Pale Dexter" to "vertical",
    "Pale Sinister" to "vertical",
    "Per Pale" to "vertical",
    "Per Pale Inverted" to "vertical",
    "Per Fess" to "horizontal",
    "Per Fess Inverted" to "horizontal",
    "Saltire" to "cross",
    "Cross" to "cross",
    "Chevron" to "cross",
    "Inverted Chevron" to "cross",
    "Base Dexter Canton" to "corner",
    "Base Sinister Canton" to "corner",
    "Chief Dexter Canton" to "corner",
    "Chief Sinister Canton" to "corner",
    "Roundel" to "decorative",
    "Lozenge" to "decorative",
    "Bordure" to "decorative",
    "Base Indented" to "decorative",
    "Chief Indented" to "decorative",
    "Gradient" to "decorative",
    "Base Gradient" to "decorative",
)

private val DYE_COLORS = listOf(
    "White", "Red", "Orange", "Yellow", "Lime", "Green",
    "Light Blue", "Cyan", "Blue", "Purple", "Magenta", "Pink",
    "Brown", "Gray", "Light Gray", "Black",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BannerScreen() {
    var selectedSection by remember { mutableStateOf("patterns") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Blocks,
            title = "Banner Patterns",
            description = "Reference for all banner patterns, special pattern items, and dye colors.",
        )

        // Section selector
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("patterns" to "Pattern Items", "loom" to "Loom Patterns", "colors" to "Dye Colors").forEach { (key, label) ->
                FilterChip(
                    selected = selectedSection == key,
                    onClick = { selectedSection = key },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        when (selectedSection) {
            "patterns" -> PatternItemsSection()
            "loom" -> LoomPatternsSection()
            "colors" -> DyeColorsSection()
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PatternItemsSection() {
    SectionHeader("Special Pattern Items")
    ResultCard {
        Text(
            "These items unlock special patterns in the Loom. They are not consumed when used.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
        )
    }

    PATTERN_ITEMS.forEach { item ->
        val rarityColor = when (item.rarity) {
            "Rare" -> MaterialTheme.colorScheme.primary
            "Uncommon" -> PotionBlue
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                CategoryBadge(label = item.rarity, color = rarityColor)
            }
            Spacer(Modifier.height(4.dp))
            StatRow("Obtained", item.obtainMethod)
            StatRow("Source", item.source)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoomPatternsSection() {
    SectionHeader("Loom Patterns (No Item Required)")
    ResultCard {
        Text(
            "These patterns are available in the Loom without any special banner pattern item. Select a dye color and apply to any banner.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
        )
    }

    val grouped = LOOM_PATTERNS.groupBy { it.second }
    val groupOrder = listOf("basic", "diagonal", "vertical", "horizontal", "cross", "corner", "decorative")

    groupOrder.forEach { group ->
        val patterns = grouped[group] ?: return@forEach
        ResultCard {
            Text(
                group.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            patterns.forEach { (name, _) ->
                Text(
                    "\u2022 $name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DyeColorsSection() {
    SectionHeader("All 16 Dye Colors")
    ResultCard {
        Text(
            "Any of these colors can be used in the Loom to apply patterns to banners.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
        )
        SpyglassDivider()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DYE_COLORS.forEach { color ->
                CategoryBadge(label = color, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SpyglassDivider()
        Text("HOW TO USE THE LOOM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "1. Place a banner in the top-left slot\n2. Place a dye in the top-right slot\n3. Optionally place a pattern item in the middle slot\n4. Select a pattern from the list\n5. Take the result from the output slot",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpyglassDivider()
        Text("TIPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "\u2022 Banners can have up to 6 pattern layers\n\u2022 Pattern items are NOT consumed when used\n\u2022 You can copy a banner by placing it + blank banner in a crafting grid\n\u2022 Banners can be placed on shields at a crafting table",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
