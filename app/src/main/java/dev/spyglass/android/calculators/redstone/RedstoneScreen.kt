package dev.spyglass.android.calculators.redstone

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import kotlin.math.ceil
import kotlin.math.floor

private data class Container(
    val name: String,
    val slots: Int,
    val stackable: Boolean = true, // false for brewing stand, jukebox, etc.
)

private val CONTAINERS = listOf(
    Container("Chest (Single)", 27),
    Container("Chest (Double)", 54),
    Container("Barrel", 27),
    Container("Shulker Box", 27),
    Container("Hopper", 5),
    Container("Dispenser", 9),
    Container("Dropper", 9),
    Container("Furnace", 3),
    Container("Blast Furnace", 3),
    Container("Smoker", 3),
    Container("Brewing Stand", 5, stackable = false),
    Container("Crafter", 9),
    Container("Decorated Pot", 1),
    Container("Chiseled Bookshelf", 6, stackable = false),
)

/**
 * Calculates the comparator signal strength for a container with a given fill level.
 * Formula: signal = floor(1 + (filledSlots / totalSlots) * 14)
 * where filledSlots = sum of (itemCount / maxStackSize) for each slot
 * Signal is 0 when empty, 1 when any item is present, 15 when full.
 */
private fun signalStrength(filledSlotFraction: Double): Int {
    if (filledSlotFraction <= 0.0) return 0
    return floor(1.0 + filledSlotFraction * 14.0).toInt().coerceIn(0, 15)
}

/**
 * Calculates the fraction of a slot filled given itemCount / stackSize
 */
private fun fillFraction(totalItems: Int, stackSize: Int, slots: Int): Double {
    if (slots <= 0 || stackSize <= 0) return 0.0
    val maxCapacity = slots * stackSize
    return totalItems.toDouble() / maxCapacity.toDouble()
}

/**
 * Given a target signal strength and container, calculate how many items (of a given stack size) are needed.
 */
private fun itemsForSignal(targetSignal: Int, slots: Int, stackSize: Int): Int {
    if (targetSignal <= 0) return 0
    if (targetSignal >= 15) return slots * stackSize
    // signal = floor(1 + fraction * 14)
    // targetSignal = 1 + fraction * 14  => fraction = (targetSignal - 1) / 14
    val minFraction = (targetSignal - 1).toDouble() / 14.0
    val totalCapacity = slots * stackSize
    return ceil(minFraction * totalCapacity).toInt().coerceAtLeast(1)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RedstoneScreen() {
    var selectedContainer by remember { mutableStateOf(CONTAINERS[0]) }
    var showContainerPicker by remember { mutableStateOf(false) }
    var itemCountStr by remember { mutableStateOf("") }
    var stackSizeStr by remember { mutableStateOf("64") }
    var targetSignalStr by remember { mutableStateOf("") }

    val itemCount = itemCountStr.toIntOrNull() ?: 0
    val stackSize = stackSizeStr.toIntOrNull()?.coerceIn(1, 64) ?: 64
    val fraction = fillFraction(itemCount, stackSize, selectedContainer.slots)
    val currentSignal = signalStrength(fraction)

    val targetSignal = targetSignalStr.toIntOrNull()?.coerceIn(0, 15)
    val itemsNeeded = if (targetSignal != null) itemsForSignal(targetSignal, selectedContainer.slots, stackSize) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Blocks,
            title = stringResource(R.string.redstone_title),
            description = stringResource(R.string.redstone_description),
        )

        // ── Container Selector ──
        SectionHeader(stringResource(R.string.redstone_container))
        InputCard {
            Text(stringResource(R.string.redstone_select_container), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            OutlinedButton(
                onClick = { showContainerPicker = !showContainerPicker },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.redstone_container_slots, selectedContainer.name, selectedContainer.slots))
            }
            if (showContainerPicker) {
                CONTAINERS.forEach { container ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = {
                            selectedContainer = container
                            showContainerPicker = false
                        }) {
                            Text(
                                container.name,
                                color = if (container == selectedContainer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            stringResource(R.string.redstone_slots_label, container.slots),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        // ── Signal Calculator ──
        SectionHeader(stringResource(R.string.redstone_calc_signal))
        InputCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = itemCountStr,
                    onValueChange = { itemCountStr = it },
                    label = { Text(stringResource(R.string.redstone_item_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = stackSizeStr,
                    onValueChange = { stackSizeStr = it },
                    label = { Text(stringResource(R.string.redstone_stack_size)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.redstone_stack_hint),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }

        ResultCard {
            StatRow(stringResource(R.string.redstone_fill_level), "${(fraction * 100).toInt()}%")
            StatRow(stringResource(R.string.redstone_signal_strength), stringResource(R.string.redstone_signal_out_of, currentSignal))
            StatRow(stringResource(R.string.redstone_max_capacity), stringResource(R.string.redstone_max_capacity_val, selectedContainer.slots * stackSize))
            if (itemCount > 0) {
                StatRow(stringResource(R.string.redstone_slots_used), stringResource(R.string.redstone_slots_used_val, ceil(itemCount.toDouble() / stackSize).toInt(), selectedContainer.slots))
            }
        }

        // ── Reverse Calculator ──
        SectionHeader(stringResource(R.string.redstone_items_needed_header))
        InputCard {
            OutlinedTextField(
                value = targetSignalStr,
                onValueChange = { targetSignalStr = it },
                label = { Text(stringResource(R.string.redstone_target_signal)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (itemsNeeded != null && targetSignal != null) {
            ResultCard {
                StatRow(stringResource(R.string.redstone_target_signal_label), "$targetSignal")
                StatRow(stringResource(R.string.redstone_items_needed), stringResource(R.string.redstone_items_needed_val, itemsNeeded, stackSize))
                if (targetSignal > 0) {
                    val maxForSignal = if (targetSignal >= 15) selectedContainer.slots * stackSize
                    else {
                        val maxFraction = targetSignal.toDouble() / 14.0
                        floor(maxFraction * selectedContainer.slots * stackSize).toInt()
                    }
                    StatRow(stringResource(R.string.redstone_max_items_at_signal, targetSignal), "$maxForSignal")
                }
            }
        }

        // ── Signal Reference Table ──
        SectionHeader(stringResource(R.string.redstone_signal_reference))
        ResultCard {
            Text(stringResource(R.string.redstone_signal_table_for, selectedContainer.name.uppercase()),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            for (sig in 0..15) {
                val items = itemsForSignal(sig, selectedContainer.slots, stackSize)
                val maxItems = if (sig >= 15) selectedContainer.slots * stackSize
                else {
                    val maxFrac = sig.toDouble() / 14.0
                    floor(maxFrac * selectedContainer.slots * stackSize).toInt()
                }
                val range = if (sig == 0) "0" else if (sig == 15) "$items+" else "$items - $maxItems"
                StatRow(stringResource(R.string.redstone_signal_n, sig), stringResource(R.string.redstone_items_suffix, range))
            }
        }

        // ── Special Blocks ──
        SectionHeader(stringResource(R.string.redstone_special_sources))
        ResultCard {
            Text(stringResource(R.string.redstone_non_container_sources), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            StatRow(stringResource(R.string.redstone_cake), stringResource(R.string.redstone_cake_val))
            StatRow(stringResource(R.string.redstone_composter), stringResource(R.string.redstone_composter_val))
            StatRow(stringResource(R.string.redstone_cauldron), stringResource(R.string.redstone_cauldron_val))
            StatRow(stringResource(R.string.redstone_beehive), stringResource(R.string.redstone_beehive_val))
            StatRow(stringResource(R.string.redstone_lectern), stringResource(R.string.redstone_lectern_val))
            StatRow(stringResource(R.string.redstone_respawn_anchor), stringResource(R.string.redstone_respawn_anchor_val))
            StatRow(stringResource(R.string.redstone_sculk_sensor), stringResource(R.string.redstone_sculk_sensor_val))
            StatRow(stringResource(R.string.redstone_end_portal_frame), stringResource(R.string.redstone_end_portal_frame_val))
            StatRow(stringResource(R.string.redstone_jukebox), stringResource(R.string.redstone_jukebox_val))
            StatRow(stringResource(R.string.redstone_command_block), stringResource(R.string.redstone_command_block_val))
            StatRow(stringResource(R.string.redstone_item_frame), stringResource(R.string.redstone_item_frame_val))
            SpyglassDivider()
            Text(stringResource(R.string.redstone_jukebox_disc_signals), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            StatRow("13", "Signal 1")
            StatRow("Cat", "Signal 2")
            StatRow("Blocks", "Signal 3")
            StatRow("Chirp", "Signal 4")
            StatRow("Creator", "Signal 5")
            StatRow("Far", "Signal 6")
            StatRow("Mall", "Signal 7")
            StatRow("Mellohi", "Signal 8")
            StatRow("Stal", "Signal 9")
            StatRow("Strad", "Signal 10")
            StatRow("Ward", "Signal 11")
            StatRow("11", "Signal 12")
            StatRow("Wait", "Signal 13")
            StatRow("Pigstep", "Signal 14")
            StatRow("Relic / Precipice / Creator (Music Box)", "Signal 15")
        }

        // ── Comparator Tips ──
        SectionHeader(stringResource(R.string.redstone_comparator_tips))
        ResultCard {
            Text(stringResource(R.string.redstone_how_comparators_work), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.redstone_comparator_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(stringResource(R.string.redstone_compare_mode_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.redstone_compare_mode_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(stringResource(R.string.redstone_subtract_mode_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.redstone_subtract_mode_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(stringResource(R.string.redstone_signal_formula), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.redstone_signal_formula_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
