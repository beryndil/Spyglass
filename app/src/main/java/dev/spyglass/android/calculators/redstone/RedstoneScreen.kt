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
            title = "Redstone Signal",
            description = "Calculate comparator signal strength from container fill levels. Plan exact item counts for redstone contraptions.",
        )

        // ── Container Selector ──
        SectionHeader("Container")
        InputCard {
            Text("SELECT CONTAINER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            OutlinedButton(
                onClick = { showContainerPicker = !showContainerPicker },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${selectedContainer.name} (${selectedContainer.slots} slots)")
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
                            "${container.slots} slots",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        // ── Signal Calculator ──
        SectionHeader("Calculate Signal from Items")
        InputCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = itemCountStr,
                    onValueChange = { itemCountStr = it },
                    label = { Text("Item Count") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = stackSizeStr,
                    onValueChange = { stackSizeStr = it },
                    label = { Text("Stack Size") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("Common: 64 (most items), 16 (eggs, ender pearls, snowballs), 1 (tools, armor)",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }

        ResultCard {
            StatRow("Fill Level", "${(fraction * 100).toInt()}%")
            StatRow("Signal Strength", "$currentSignal / 15")
            StatRow("Max Capacity", "${selectedContainer.slots * stackSize} items")
            if (itemCount > 0) {
                StatRow("Slots Used", "${ceil(itemCount.toDouble() / stackSize).toInt()} / ${selectedContainer.slots}")
            }
        }

        // ── Reverse Calculator ──
        SectionHeader("Items Needed for Target Signal")
        InputCard {
            OutlinedTextField(
                value = targetSignalStr,
                onValueChange = { targetSignalStr = it },
                label = { Text("Target Signal (0-15)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, cursorColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (itemsNeeded != null && targetSignal != null) {
            ResultCard {
                StatRow("Target Signal", "$targetSignal")
                StatRow("Items Needed", "$itemsNeeded (stack size $stackSize)")
                if (targetSignal > 0) {
                    val maxForSignal = if (targetSignal >= 15) selectedContainer.slots * stackSize
                    else {
                        val maxFraction = targetSignal.toDouble() / 14.0
                        floor(maxFraction * selectedContainer.slots * stackSize).toInt()
                    }
                    StatRow("Max Items at Signal $targetSignal", "$maxForSignal")
                }
            }
        }

        // ── Signal Reference Table ──
        SectionHeader("Signal Reference")
        ResultCard {
            Text("SIGNAL TABLE FOR ${selectedContainer.name.uppercase()}",
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
                StatRow("Signal $sig", "$range items")
            }
        }

        // ── Special Blocks ──
        SectionHeader("Special Comparator Sources")
        ResultCard {
            Text("NON-CONTAINER SOURCES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            StatRow("Cake", "Signal = 14 - (2 * slices eaten), max 14")
            StatRow("Composter", "Signal = compost level (0-8)")
            StatRow("Cauldron", "Signal = water level (0-3)")
            StatRow("Beehive / Bee Nest", "Signal = honey level (0-5)")
            StatRow("Lectern", "Signal = page number / total pages * 15")
            StatRow("Respawn Anchor", "Signal = charges (0-4)")
            StatRow("Sculk Sensor", "Signal based on vibration type (1-15)")
            StatRow("End Portal Frame", "Signal 15 if has eye, 0 if empty")
            StatRow("Jukebox", "Signal depends on disc (1-15)")
            StatRow("Command Block", "Signal = success count")
            StatRow("Item Frame", "Signal = rotation step (0-8)")
            SpyglassDivider()
            Text("JUKEBOX DISC SIGNALS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
        SectionHeader("Comparator Tips")
        ResultCard {
            Text("HOW COMPARATORS WORK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "A Redstone Comparator reads the fill level of a container behind it and outputs a signal strength from 0-15.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text("COMPARE MODE (front torch off)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Output = rear signal if rear >= side signal, otherwise 0.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text("SUBTRACT MODE (front torch on)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Output = rear signal - side signal (minimum 0). Useful for arithmetic circuits.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text("SIGNAL FORMULA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "signal = floor(1 + (sum of item fractions / total slots) * 14)\nwhere item fraction = count / max_stack_size for each slot\nSignal is 0 when empty, 15 when completely full.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
