package dev.spyglass.android.calculators.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
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
            title = stringResource(R.string.clock_title),
            description = stringResource(R.string.clock_description),
        )

        // ── Sync Controls ──
        SectionHeader(stringResource(R.string.clock_sync), icon = PixelIcons.Clock)
        InputCard {
            if (!state.synced) {
                Text(
                    stringResource(R.string.clock_not_synced),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // F3 Sync
                Text(
                    stringResource(R.string.clock_f3_sync),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.clock_f3_desc),
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
                        label = stringResource(R.string.clock_day_time_tick),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            hapticClick()
                            vm.syncFromF3(tickInput)
                            tickInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        enabled = tickInput.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.clock_sync_btn))
                    }
                }

                SpyglassDivider()

                // Manual Sync
                Text(
                    stringResource(R.string.clock_quick_sync),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.clock_quick_sync_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                val syncEvents = listOf(
                    "Sunrise" to stringResource(R.string.clock_event_sunrise),
                    "Noon" to stringResource(R.string.clock_event_noon),
                    "Sunset" to stringResource(R.string.clock_event_sunset),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    syncEvents.forEach { (key, label) ->
                        OutlinedButton(
                            onClick = { hapticClick(); vm.syncManual(key) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                StatRow(stringResource(R.string.clock_synced_via), if (state.syncMethod == "f3") stringResource(R.string.clock_f3_debug) else stringResource(R.string.clock_quick_sync_label))
                StatRow(stringResource(R.string.clock_game_time), state.timeString)
                TextButton(
                    onClick = { hapticConfirm(); vm.resetSync() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text(stringResource(R.string.clock_stop), color = Red400, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (state.synced) {
            // ── Clock Display ──
            SectionHeader(stringResource(R.string.clock_current_time))
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
                        stringResource(R.string.clock_day_prefix),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "${state.displayedDay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { hapticClick(); showDayDialog = true }
                            .padding(horizontal = 4.dp),
                    )
                    Text(
                        stringResource(R.string.clock_tick_format, state.currentTick),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // ── Your Events ──
            SectionHeader(stringResource(R.string.clock_your_events))
            ResultCard {
                if (state.activeEvents.isEmpty()) {
                    Text(
                        stringResource(R.string.clock_no_events),
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
                                        .clickable { hapticConfirm(); vm.removeEvent(event) }
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
                    onClick = { hapticClick(); showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.clock_add_event_arrow), color = MaterialTheme.colorScheme.primary)
                }
            }

            // ── Event Timeline ──
            SectionHeader(stringResource(R.string.clock_upcoming_events))
            ResultCard {
                if (state.events.isEmpty()) {
                    Text(
                        stringResource(R.string.clock_no_events_configured),
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
        SectionHeader(stringResource(R.string.clock_how_to_sync))
        ResultCard {
            ExpandableHelp(
                title = stringResource(R.string.clock_using_f3),
                expanded = showF3Help,
                onToggle = { showF3Help = !showF3Help },
            ) {
                Text(stringResource(R.string.clock_f3_step_1), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.clock_f3_step_2), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.clock_f3_step_3), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.clock_f3_step_4), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpyglassDivider()
            ExpandableHelp(
                title = stringResource(R.string.clock_quick_sync_title),
                expanded = showQuickHelp,
                onToggle = { showQuickHelp = !showQuickHelp },
            ) {
                Text(stringResource(R.string.clock_quick_sync_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.clock_quick_sync_sunrise), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.clock_quick_sync_noon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.clock_quick_sync_sunset), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpyglassDivider()
            ExpandableHelp(
                title = stringResource(R.string.clock_reading_countdowns),
                expanded = showCountdownHelp,
                onToggle = { showCountdownHelp = !showCountdownHelp },
            ) {
                Text(stringResource(R.string.clock_countdown_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.clock_1_mc_day), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.clock_1_mc_hour), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.clock_color_coded), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val hapticClick = rememberHapticClick()
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
            Text(stringResource(R.string.clock_add_event_dialog), color = MaterialTheme.colorScheme.primary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Tab selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { hapticClick(); selectedTab = 0 },
                        label = { Text(stringResource(R.string.clock_predefined)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { hapticClick(); selectedTab = 1 },
                        label = { Text(stringResource(R.string.clock_custom)) },
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
                            stringResource(R.string.clock_all_predefined_added),
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
                                            hapticClick()
                                            event.predefinedId?.let(onAddPredefined) ?: return@clickable
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
                        label = stringResource(R.string.clock_event_name),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SpyglassTextField(
                        value = customTick,
                        onValueChange = { customTick = it.filter { c -> c.isDigit() } },
                        label = stringResource(R.string.clock_event_tick),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.clock_color_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    val colorChoices = listOf(
                        Triple("green", stringResource(R.string.clock_color_green), eventColor("green")),
                        Triple("gold", stringResource(R.string.clock_color_gold), eventColor("gold")),
                        Triple("red", stringResource(R.string.clock_color_red), eventColor("red")),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorChoices.forEach { (value, label, color) ->
                            FilterChip(
                                selected = customColor == value,
                                onClick = { hapticClick(); customColor = value },
                                label = { Text(label) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color),
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                    Button(
                        onClick = {
                            hapticClick()
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
            TextButton(onClick = { hapticClick(); onDismiss() }) {
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
    val hapticClick = rememberHapticClick()
    var dayInput by remember { mutableStateOf(currentDay.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.clock_set_day_dialog), color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.clock_set_day_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpyglassTextField(
                    value = dayInput,
                    onValueChange = { dayInput = it.filter { c -> c.isDigit() } },
                    label = stringResource(R.string.clock_day_number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    hapticClick()
                    val day = dayInput.toLongOrNull() ?: return@TextButton
                    onSetDay(day)
                },
                enabled = dayInput.isNotBlank() && dayInput.toLongOrNull() != null,
            ) {
                Text(stringResource(R.string.clock_set), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = { hapticClick(); onDismiss() }) {
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
    val hapticClick = rememberHapticClick()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { hapticClick(); onToggle() }
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
        val reduceMotion = LocalReduceAnimations.current
        AnimatedVisibility(visible = expanded, enter = if (reduceMotion) expandVertically(snap()) else expandVertically(), exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically()) {
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
