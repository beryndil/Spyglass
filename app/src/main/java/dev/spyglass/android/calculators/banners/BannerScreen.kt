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
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
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
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Blocks,
            title = stringResource(R.string.banner_title),
            description = stringResource(R.string.banner_description),
        )

        // ── Base color selector ─────────────────────────────────────────
        SectionHeader(stringResource(R.string.banner_base_color))
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
        SectionHeader(stringResource(R.string.banner_preview))
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
                stringResource(R.string.banner_layers_count, s.layers.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        // ── Add pattern layer ───────────────────────────────────────────
        if (s.layers.size < 6) {
            SectionHeader(stringResource(R.string.banner_add_pattern))
            InputCard {
                // Category filter chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PATTERN_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = s.selectedCategory == cat,
                            onClick = { hapticClick(); vm.setSelectedCategory(cat) },
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
                                    .clickable { hapticClick(); vm.setSelectedPattern(pattern) }
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
                    s.selectedPattern.displayName + if (s.selectedPattern.requiresItem) stringResource(R.string.banner_requires_item) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Layer color picker
                Text(stringResource(R.string.banner_layer_color), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                    onClick = { hapticClick(); vm.addLayer() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.banner_add_layer))
                }
            }
        }

        // ── Layer stack ─────────────────────────────────────────────────
        if (s.layers.isNotEmpty()) {
            SectionHeader(stringResource(R.string.banner_layer_stack))
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
                        IconButton(onClick = { hapticClick(); vm.moveLayerUp(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.banner_move_up),
                                tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { hapticClick(); vm.moveLayerDown(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.banner_move_down),
                                tint = if (index < s.layers.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { hapticConfirm(); vm.removeLayer(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.banner_remove),
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < s.layers.lastIndex) SpyglassDivider()
                }

                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { hapticConfirm(); vm.clearDesign() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.banner_clear_all))
                }
            }
        }

        // ── Materials summary ───────────────────────────────────────────
        if (s.layers.isNotEmpty()) {
            SectionHeader(stringResource(R.string.banner_materials_needed))
            ResultCard {
                StatRow(stringResource(R.string.banner_banner_material), stringResource(R.string.banner_banner_val, s.baseColor.displayName))

                val dyeCounts = mutableMapOf<DyeColor, Int>()
                s.layers.forEach { layer -> dyeCounts[layer.color] = (dyeCounts[layer.color] ?: 0) + 1 }
                dyeCounts.forEach { (dye, count) ->
                    StatRow("${dye.displayName} Dye", stringResource(R.string.banner_dye_count, count))
                }

                val specialItems = s.layers.filter { it.pattern.requiresItem }.map { it.pattern.itemName }.distinct()
                if (specialItems.isNotEmpty()) {
                    SpyglassDivider()
                    specialItems.forEach { item ->
                        StatRow(item, stringResource(R.string.banner_reusable))
                    }
                }
            }
        }

        // ── Collapsible reference ───────────────────────────────────────
        OutlinedButton(
            onClick = { hapticClick(); showReference = !showReference },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        ) {
            Text(if (showReference) stringResource(R.string.banner_hide_reference) else stringResource(R.string.banner_show_reference))
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
    val hapticClick = rememberHapticClick()
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
            .clickable { hapticClick(); onClick() },
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
    SectionHeader(stringResource(R.string.banner_special_items))
    ResultCard {
        Text(
            stringResource(R.string.banner_special_items_desc),
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
            StatRow(stringResource(R.string.banner_obtained), item.obtainMethod)
            StatRow(stringResource(R.string.banner_source), item.source)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoomPatternsSection() {
    SectionHeader(stringResource(R.string.banner_loom_patterns))
    ResultCard {
        Text(
            stringResource(R.string.banner_loom_patterns_desc),
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
    SectionHeader(stringResource(R.string.banner_all_16_dye))
    ResultCard {
        Text(
            stringResource(R.string.banner_dye_desc),
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
        Text(stringResource(R.string.banner_how_to_use_loom), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.banner_loom_steps),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpyglassDivider()
        Text(stringResource(R.string.banner_tips), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.banner_tips_text),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
