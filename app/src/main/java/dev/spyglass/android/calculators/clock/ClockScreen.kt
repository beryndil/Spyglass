package dev.spyglass.android.calculators.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun ClockScreen(vm: ClockViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tickInput by remember { mutableStateOf("") }
    var showF3Help by remember { mutableStateOf(false) }
    var showQuickHelp by remember { mutableStateOf(false) }
    var showCountdownHelp by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Clock,
            title = "Game Clock",
            description = "Track Minecraft\u2019s day/night cycle and upcoming events.",
        )

        // ── Sync Controls ──
        SectionHeader("Sync", icon = PixelIcons.Clock)
        InputCard {
            if (!state.synced) {
                Text(
                    "Not synced \u2014 choose a method below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // F3 Sync
                Text(
                    "F3 SYNC (MOST ACCURATE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Enter the tick value from the F3 debug screen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SpyglassTextField(
                        value = tickInput,
                        onValueChange = { tickInput = it.filter { c -> c.isDigit() } },
                        label = "Day Time tick (0\u201323999)",
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            vm.syncFromF3(tickInput)
                            tickInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        enabled = tickInput.isNotBlank(),
                    ) {
                        Text("Sync")
                    }
                }

                SpyglassDivider()

                // Manual Sync
                Text(
                    "QUICK SYNC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Tap when you see the event in-game:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Sunrise", "Noon", "Sunset").forEach { event ->
                        OutlinedButton(
                            onClick = { vm.syncManual(event) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(event, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                StatRow("Synced via", if (state.syncMethod == "f3") "F3 Debug" else "Quick Sync")
                StatRow("Game time", state.timeString)
                TextButton(
                    onClick = vm::resetSync,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Stop Clock", color = Red400, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (state.synced) {
            // ── Clock Display ──
            SectionHeader("Current Time")
            ResultCard {
                Text(
                    state.timeString,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(4.dp))
                // Day progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outline),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(state.dayProgress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Day ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "${state.displayedDay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showDayDialog = true }
                            .padding(horizontal = 4.dp),
                    )
                    Text(
                        "  \u2022  Tick ${state.currentTick}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // ── Your Events ──
            SectionHeader("Your Events")
            ResultCard {
                if (state.activeEvents.isEmpty()) {
                    Text(
                        "No events configured. Tap below to add events.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    state.activeEvents.forEachIndexed { index, event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(eventColor(event.color)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    event.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    ClockEngine.formatTime(event.tick),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    "\u2715",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { vm.removeEvent(event) }
                                        .padding(4.dp),
                                )
                            }
                        }
                        if (index < state.activeEvents.lastIndex) {
                            SpyglassDivider()
                        }
                    }
                }
                SpyglassDivider()
                TextButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add Event \u2192", color = MaterialTheme.colorScheme.primary)
                }
            }

            // ── Event Timeline ──
            SectionHeader("Upcoming Events")
            ResultCard {
                if (state.events.isEmpty()) {
                    Text(
                        "No events configured.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    state.events.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(eventColor(event.color)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    event.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                event.countdown,
                                style = MaterialTheme.typography.bodyMedium,
                                color = eventColor(event.color),
                            )
                        }
                        if (event != state.events.last()) {
                            SpyglassDivider()
                        }
                    }
                }
            }
        }

        // ── Help ──
        SectionHeader("How to Sync")
        ResultCard {
            ExpandableHelp(
                title = "Using F3 (PC)",
                expanded = showF3Help,
                onToggle = { showF3Help = !showF3Help },
            ) {
                Text("1. Press F3 in Minecraft to open the debug screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("2. Find the \u201cDay Time\u201d value on the left side", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("3. Enter the tick number (0\u201323999) in the field above", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("4. Tap \u201cSync\u201d \u2014 the clock will track from that point", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpyglassDivider()
            ExpandableHelp(
                title = "Quick Sync",
                expanded = showQuickHelp,
                onToggle = { showQuickHelp = !showQuickHelp },
            ) {
                Text("Tap the Sunrise, Noon, or Sunset button the moment you see that event happen in-game.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("\u2022 Sunrise \u2014 when the sun first appears on the horizon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("\u2022 Noon \u2014 when the sun is directly overhead", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("\u2022 Sunset \u2014 when the sun starts to dip below the horizon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpyglassDivider()
            ExpandableHelp(
                title = "Reading Countdowns",
                expanded = showCountdownHelp,
                onToggle = { showCountdownHelp = !showCountdownHelp },
            ) {
                Text("The event timeline shows how long until each event occurs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("1 Minecraft day = 20 real minutes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("1 Minecraft hour \u2248 50 real seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Events are color-coded: green (safe), gold (warning), red (danger).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // ── Add Event Dialog ──
    if (showAddDialog) {
        AddEventDialog(
            activeEvents = state.activeEvents,
            onAddPredefined = { vm.addPredefinedEvent(it) },
            onAddCustom = { name, tick, color -> vm.addCustomEvent(name, tick, color) },
            onDismiss = { showAddDialog = false },
        )
    }

    // ── Set Day Dialog ──
    if (showDayDialog) {
        SetDayDialog(
            currentDay = state.displayedDay,
            onSetDay = { vm.setDay(it); showDayDialog = false },
            onDismiss = { showDayDialog = false },
        )
    }
}

@Composable
private fun AddEventDialog(
    activeEvents: List<ClockEngine.GameEvent>,
    onAddPredefined: (String) -> Unit,
    onAddCustom: (String, Long, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var customName by remember { mutableStateOf("") }
    var customTick by remember { mutableStateOf("") }
    var customColor by remember { mutableStateOf("green") }

    val activeIds = activeEvents.mapNotNull { it.predefinedId }.toSet()
    val availablePredefined = ClockEngine.PREDEFINED_EVENTS.filter { it.predefinedId !in activeIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Add Event", color = MaterialTheme.colorScheme.primary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Tab selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Predefined") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Custom") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }

                if (selectedTab == 0) {
                    // Predefined events
                    if (availablePredefined.isEmpty()) {
                        Text(
                            "All predefined events are already added.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            availablePredefined.forEach { event ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onAddPredefined(event.predefinedId!!)
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(eventColor(event.color)),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            event.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            ClockEngine.formatTime(event.tick),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        Text(
                                            "+",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Custom event form
                    SpyglassTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = "Event name",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SpyglassTextField(
                        value = customTick,
                        onValueChange = { customTick = it.filter { c -> c.isDigit() } },
                        label = "Tick (0\u201323999)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "COLOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("green" to "Green", "gold" to "Gold", "red" to "Red").forEach { (value, label) ->
                            FilterChip(
                                selected = customColor == value,
                                onClick = { customColor = value },
                                label = { Text(label) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(eventColor(value)),
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = eventColor(value).copy(alpha = 0.2f),
                                    selectedLabelColor = eventColor(value),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val tick = customTick.toLongOrNull() ?: return@Button
                            onAddCustom(customName, tick, customColor)
                            customName = ""
                            customTick = ""
                            customColor = "green"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        enabled = customName.isNotBlank() && customTick.isNotBlank() &&
                            (customTick.toLongOrNull() ?: -1L) in 0 until ClockEngine.TICKS_PER_DAY,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.add))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun SetDayDialog(
    currentDay: Long,
    onSetDay: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var dayInput by remember { mutableStateOf(currentDay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Set Day", color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter the current in-game day from the F3 debug screen (Day line). You can update this any time after a server restart or crash.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpyglassTextField(
                    value = dayInput,
                    onValueChange = { dayInput = it.filter { c -> c.isDigit() } },
                    label = "Day number",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val day = dayInput.toLongOrNull() ?: return@TextButton
                    onSetDay(day)
                },
                enabled = dayInput.isNotBlank() && dayInput.toLongOrNull() != null,
            ) {
                Text("Set", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.secondary)
            }
        },
    )
}

@Composable
private fun ExpandableHelp(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (expanded) "\u25B2" else "\u25BC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content,
            )
        }
    }
}

@Composable
internal fun eventColor(color: String) = when (color) {
    "green" -> Green400
    "gold" -> MaterialTheme.colorScheme.primary
    "red" -> Red400
    else -> MaterialTheme.colorScheme.secondary
}
