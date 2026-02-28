package dev.spyglass.android.calculators.anvil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

private val WEAPONS = listOf(ItemType.SWORD, ItemType.BOW, ItemType.CROSSBOW, ItemType.TRIDENT, ItemType.MACE)
private val TOOL_TYPES = listOf(ItemType.PICKAXE, ItemType.AXE, ItemType.SHOVEL, ItemType.HOE, ItemType.FISHING_ROD)
private val ARMOR_TYPES = listOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)

private fun ItemType.displayName(): String =
    name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun ItemType.textureId(): String = when (this) {
    ItemType.SWORD       -> "diamond_sword"
    ItemType.BOW         -> "bow"
    ItemType.CROSSBOW    -> "crossbow"
    ItemType.TRIDENT     -> "trident"
    ItemType.MACE        -> "mace"
    ItemType.PICKAXE     -> "diamond_pickaxe"
    ItemType.AXE         -> "diamond_axe"
    ItemType.SHOVEL      -> "diamond_shovel"
    ItemType.HOE         -> "diamond_hoe"
    ItemType.FISHING_ROD -> "fishing_rod"
    ItemType.HELMET      -> "diamond_helmet"
    ItemType.CHESTPLATE  -> "diamond_chestplate"
    ItemType.LEGGINGS    -> "diamond_leggings"
    ItemType.BOOTS       -> "diamond_boots"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnvilScreen(vm: AnvilViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val warning by vm.warningMessage.collectAsState()
    val available = vm.enchantsForCurrentItem()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(warning) {
        if (warning != null) {
            snackbarHostState.showSnackbar(warning!!, duration = SnackbarDuration.Short)
            vm.clearWarning()
        }
    }

    Box {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Enchanting", icon = PixelIcons.Anvil)

        InputCard {
            // Item selector — grouped by category
            Text("Weapons", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WEAPONS.forEach { t ->
                    val icon = ItemTextures.get(t.textureId())
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (icon != null) { { SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Tools", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TOOL_TYPES.forEach { t ->
                    val icon = ItemTextures.get(t.textureId())
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (icon != null) { { SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Armor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ARMOR_TYPES.forEach { t ->
                    val icon = ItemTextures.get(t.textureId())
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (icon != null) { { SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                    )
                }
            }
        }

        // Enchantment picker
        InputCard {
            Text("Enchantments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            available.forEach { e ->
                val picked = s.pickedEnchants.find { it.enchant.id == e.id }
                val incompatible = picked == null && vm.isIncompatible(e)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FilterChip(
                        selected = picked != null,
                        onClick  = { vm.toggleEnchant(e) },
                        enabled  = !incompatible,
                        label    = {
                            Text(
                                e.name,
                                color = if (incompatible) MaterialTheme.colorScheme.outline else Color.Unspecified,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (picked != null && e.maxLevel > 1) {
                        Spacer(Modifier.width(8.dp))
                        Row {
                            (1..e.maxLevel).forEach { lvl ->
                                IconButton(onClick = { vm.setEnchantLevel(e.id, lvl) }, modifier = Modifier.size(32.dp)) {
                                    val numeral = listOf("I","II","III","IV","V")[lvl - 1]
                                    Text(numeral, style = MaterialTheme.typography.labelSmall,
                                        color = if (picked.level == lvl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Results
        if (s.steps.isNotEmpty()) {
            ResultCard {
                Text("Optimal Order", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                s.steps.forEachIndexed { i, step ->
                    if (i > 0) SpyglassDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Step ${i + 1}: ${step.desc}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${step.cost} lvl",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (step.tooExpensive) Red400 else MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (step.tooExpensive) Text("Too Expensive!", style = MaterialTheme.typography.bodySmall, color = Red400)
                }
                SpyglassDivider()
                StatRow("Total XP", "${s.totalCost} levels")
            }
        }

        Text(
            "Pick an item and select the enchantments you want. This tool calculates the cheapest XP order to combine them on an anvil, avoiding the \"Too Expensive\" cap. Tap enchantment levels (I\u2013V) to adjust.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
    )
    }
}
