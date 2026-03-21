package dev.spyglass.android.calculators.anvil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
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
    val s by vm.state.collectAsStateWithLifecycle()
    val warning by vm.warningMessage.collectAsStateWithLifecycle()
    val available = vm.enchantsForCurrentItem()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticClick = rememberHapticClick()

    LaunchedEffect(warning) {
        warning?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            vm.clearWarning()
        }
    }

    Box {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(stringResource(R.string.anvil_header), icon = PixelIcons.Anvil)

        // ── Item Selector ────────────────────────────────────────────────────
        InputCard {
            Text(stringResource(R.string.anvil_weapons), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WEAPONS.forEach { t ->
                    val icon = ItemTextures.get(t.textureId())
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { hapticClick(); vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (icon != null) { { SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.anvil_tools), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TOOL_TYPES.forEach { t ->
                    val icon = ItemTextures.get(t.textureId())
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { hapticClick(); vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (icon != null) { { SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.anvil_armor), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ARMOR_TYPES.forEach { t ->
                    val icon = ItemTextures.get(t.textureId())
                    FilterChip(
                        selected = s.selectedItem == t,
                        onClick  = { hapticClick(); vm.setItem(t) },
                        label    = { Text(t.displayName(), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (icon != null) { { SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                    )
                }
            }
        }

        // ── Enchantment Picker ───────────────────────────────────────────────
        InputCard {
            Text(stringResource(R.string.anvil_enchantments), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
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
                        onClick  = { hapticClick(); vm.toggleEnchant(e) },
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
                            for (lvl in 1..e.maxLevel) {
                                IconButton(onClick = { hapticClick(); vm.setEnchantLevel(e.id, lvl) }, modifier = Modifier.size(32.dp)) {
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

        // ── Results ──────────────────────────────────────────────────────────
        if (s.steps.isNotEmpty()) {
            ResultCard {
                Text(stringResource(R.string.anvil_optimal_order), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                s.steps.forEachIndexed { i, step ->
                    if (i > 0) SpyglassDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.anvil_step, i + 1, step.desc),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.anvil_lvl_cost, step.cost),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (step.tooExpensive) Red400 else MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (step.tooExpensive) Text(stringResource(R.string.anvil_too_expensive), style = MaterialTheme.typography.bodySmall, color = Red400)
                }
                SpyglassDivider()
                StatRow(stringResource(R.string.anvil_total_xp), stringResource(R.string.anvil_total_xp_val, s.totalCost))
            }
        }

        Text(
            stringResource(R.string.anvil_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        // ── How Costs Work ───────────────────────────────────────────────────
        var showInfo by remember { mutableStateOf(false) }
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { hapticClick(); showInfo = !showInfo },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.anvil_how_costs_work), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(if (showInfo) "\u25B2" else "\u25BC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            val reduceMotion = LocalReduceAnimations.current
            AnimatedVisibility(visible = showInfo, enter = if (reduceMotion) expandVertically(snap()) else expandVertically(), exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.anvil_prior_work_penalty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.anvil_prior_work_desc),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.anvil_too_expensive_title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.anvil_too_expensive_desc),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.anvil_why_order_matters), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.anvil_why_order_desc),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
    )
    }
}
