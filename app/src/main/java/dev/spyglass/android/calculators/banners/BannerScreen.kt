package dev.spyglass.android.calculators.banners

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

// ── Reference data (kept for collapsible reference section) ─────────────────

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

// ── Main screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BannerScreen(vm: BannerDesignerViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var showReference by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Blocks,
            title = "Banner Designer",
            description = "Design custom banners like a Loom. Pick a base color, add up to 6 pattern layers, and preview your design live.",
        )

        // ── Base color selector ─────────────────────────────────────────
        SectionHeader("Base Color")
        InputCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DyeColor.entries.forEach { dye ->
                    ColorSwatch(
                        color = dye.color,
                        selected = s.baseColor == dye,
                        onClick = { vm.setBaseColor(dye) },
                        label = dye.displayName,
                    )
                }
            }
        }

        // ── Live banner preview ─────────────────────────────────────────
        SectionHeader("Preview")
        ResultCard(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BannerPreview(
                    baseColor = s.baseColor,
                    layers = s.layers,
                    widthDp = 120.dp,
                    heightDp = 200.dp,
                )
            }
            Text(
                "${s.layers.size}/6 layers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        // ── Add pattern layer ───────────────────────────────────────────
        if (s.layers.size < 6) {
            SectionHeader("Add Pattern")
            InputCard {
                // Category filter chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PATTERN_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = s.selectedCategory == cat,
                            onClick = { vm.setSelectedCategory(cat) },
                            label = {
                                Text(
                                    cat.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }

                // Pattern thumbnails
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BannerPattern.entries
                        .filter { it.category == s.selectedCategory && it != BannerPattern.BASE }
                        .forEach { pattern ->
                            val isSelected = s.selectedPattern == pattern
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { vm.setSelectedPattern(pattern) }
                                    .padding(2.dp),
                            ) {
                                BannerPreview(
                                    baseColor = DyeColor.WHITE,
                                    layers = listOf(BannerLayer(pattern, DyeColor.BLACK)),
                                    widthDp = 28.dp,
                                    heightDp = 48.dp,
                                    showPole = false,
                                )
                            }
                        }
                }

                Text(
                    s.selectedPattern.displayName + if (s.selectedPattern.requiresItem) " (requires item)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Layer color picker
                Text("Layer Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DyeColor.entries.forEach { dye ->
                        ColorSwatch(
                            color = dye.color,
                            selected = s.selectedLayerColor == dye,
                            onClick = { vm.setSelectedLayerColor(dye) },
                            label = dye.displayName,
                        )
                    }
                }

                // Add button
                Button(
                    onClick = { vm.addLayer() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Layer")
                }
            }
        }

        // ── Layer stack ─────────────────────────────────────────────────
        if (s.layers.isNotEmpty()) {
            SectionHeader("Layer Stack")
            ResultCard {
                s.layers.forEachIndexed { index, layer ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(16.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(layer.color.color, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                        Text(
                            layer.pattern.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.moveLayerUp(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up",
                                tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { vm.moveLayerDown(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down",
                                tint = if (index < s.layers.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { vm.removeLayer(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < s.layers.lastIndex) SpyglassDivider()
                }

                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { vm.clearDesign() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear All")
                }
            }
        }

        // ── Materials summary ───────────────────────────────────────────
        if (s.layers.isNotEmpty()) {
            SectionHeader("Materials Needed")
            ResultCard {
                StatRow("Banner", "1x ${s.baseColor.displayName} Banner")

                val dyeCounts = mutableMapOf<DyeColor, Int>()
                s.layers.forEach { layer -> dyeCounts[layer.color] = (dyeCounts[layer.color] ?: 0) + 1 }
                dyeCounts.forEach { (dye, count) ->
                    StatRow("${dye.displayName} Dye", "${count}x")
                }

                val specialItems = s.layers.filter { it.pattern.requiresItem }.map { it.pattern.itemName }.distinct()
                if (specialItems.isNotEmpty()) {
                    SpyglassDivider()
                    specialItems.forEach { item ->
                        StatRow(item, "1x (reusable)")
                    }
                }
            }
        }

        // ── Collapsible reference ───────────────────────────────────────
        OutlinedButton(
            onClick = { showReference = !showReference },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        ) {
            Text(if (showReference) "Hide Reference" else "Show Reference")
        }

        if (showReference) {
            PatternItemsSection()
            LoomPatternsSection()
            DyeColorsSection()
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Color swatch ────────────────────────────────────────────────────────────

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .background(color, RoundedCornerShape(4.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp),
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = label,
                tint = if (color == DyeColor.WHITE.color || color == DyeColor.YELLOW.color || color == DyeColor.LIME.color)
                    Color.Black else Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Reference sections (preserved from original) ───────────────────────────

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
                verticalAlignment = Alignment.CenterVertically,
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
            "These patterns are available in the Loom without any special banner pattern item.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
        )
    }

    val loomPatterns = BannerPattern.entries
        .filter { !it.requiresItem && it != BannerPattern.BASE }
        .groupBy { it.category }

    PATTERN_CATEGORIES.filter { it != "special" }.forEach { group ->
        val patterns = loomPatterns[group] ?: return@forEach
        ResultCard {
            Text(
                group.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            patterns.forEach { pattern ->
                Text(
                    "\u2022 ${pattern.displayName}",
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
            DyeColor.entries.forEach { dye ->
                CategoryBadge(label = dye.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
