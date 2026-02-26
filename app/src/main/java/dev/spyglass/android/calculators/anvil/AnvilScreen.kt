package dev.spyglass.android.calculators.anvil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

private val WEAPONS = listOf(ItemType.SWORD, ItemType.BOW, ItemType.CROSSBOW, ItemType.TRIDENT, ItemType.MACE)
private val TOOL_TYPES = listOf(ItemType.PICKAXE, ItemType.AXE, ItemType.SHOVEL, ItemType.HOE, ItemType.FISHING_ROD)
private val ARMOR_TYPES = listOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)

private fun ItemType.displayName(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnvilScreen(vm: AnvilViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val available = vm.enchantsForCurrentItem()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("Enchanting", icon = PixelIcons.Anvil)

        InputCard {
            // Item selector — grouped by category
            Text("Weapons", style = MaterialTheme.typography.bodySmall, color = Stone500)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WEAPONS.forEach { t ->
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Tools", style = MaterialTheme.typography.bodySmall, color = Stone500)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TOOL_TYPES.forEach { t ->
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Armor", style = MaterialTheme.typography.bodySmall, color = Stone500)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ARMOR_TYPES.forEach { t ->
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        // Enchantment picker
        InputCard {
            Text("Enchantments", style = MaterialTheme.typography.bodySmall, color = Stone500)
            available.forEach { e ->
                val picked = s.pickedEnchants.find { it.enchant.id == e.id }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FilterChip(
                        selected = picked != null,
                        onClick  = { vm.toggleEnchant(e) },
                        label    = { Text(e.name) },
                        modifier = Modifier.weight(1f),
                    )
                    if (picked != null && e.maxLevel > 1) {
                        Spacer(Modifier.width(8.dp))
                        Row {
                            (1..e.maxLevel).forEach { lvl ->
                                IconButton(onClick = { vm.setEnchantLevel(e.id, lvl) }, modifier = Modifier.size(32.dp)) {
                                    val numeral = listOf("I","II","III","IV","V")[lvl - 1]
                                    Text(numeral, style = MaterialTheme.typography.labelSmall,
                                        color = if (picked.level == lvl) Gold else Stone500)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Incompatibility warnings
        if (s.warnings.isNotEmpty()) {
            ResultCard {
                s.warnings.forEach { warning ->
                    Text("⚠ $warning", style = MaterialTheme.typography.bodySmall, color = Red400)
                }
            }
        }

        // Results
        if (s.steps.isNotEmpty()) {
            ResultCard {
                s.steps.forEachIndexed { i, step ->
                    if (i > 0) SpyglassDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(step.desc, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            "${step.cost} XP",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (step.tooExpensive) Red400 else Gold,
                        )
                    }
                    if (step.tooExpensive) Text("Too Expensive!", style = MaterialTheme.typography.bodySmall, color = Red400)
                }
                SpyglassDivider()
                StatRow("Total XP", "${s.totalCost} levels")
            }
        }
    }
}
