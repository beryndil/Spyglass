package dev.spyglass.android.connect.waypoints

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.R
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.PlayerData
import dev.spyglass.android.connect.client.ConnectionState
import dev.spyglass.android.core.ui.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

private fun waypointColor(color: String, primary: Color): Color = when (color) {
    "gold" -> primary
    "green" -> Emerald
    "red" -> NetherRed
    "blue" -> PotionBlue
    "purple" -> EnderPurple
    else -> primary
}

@Composable
private fun dimensionLabel(dim: String): String = when (dim) {
    "overworld" -> stringResource(R.string.dimension_overworld)
    "the_nether" -> stringResource(R.string.dimension_nether)
    "the_end" -> stringResource(R.string.dimension_the_end)
    else -> dim.replaceFirstChar { it.uppercase() }
}

@Composable
private fun categoryLabel(cat: String): String = when (cat) {
    "base" -> stringResource(R.string.waypoint_category_base)
    "farm" -> stringResource(R.string.waypoint_category_farm)
    "portal" -> stringResource(R.string.waypoint_category_portal)
    "spawner" -> stringResource(R.string.waypoint_category_spawner)
    "village" -> stringResource(R.string.waypoint_category_village)
    "monument" -> stringResource(R.string.waypoint_category_monument)
    "other" -> stringResource(R.string.waypoint_category_other)
    else -> cat.replaceFirstChar { it.uppercase() }
}

/** Calculate 3D distance between two points, applying nether scaling when needed. */
private fun distanceBetween(
    x1: Int, y1: Int, z1: Int, dim1: String,
    x2: Int, y2: Int, z2: Int, dim2: String,
): Int? {
    // End is incomparable with other dimensions
    if (dim1 == "the_end" != (dim2 == "the_end")) return null

    // Scale coordinates to overworld equivalents for comparison
    fun toOverworld(x: Int, z: Int, dim: String): Pair<Int, Int> = when (dim) {
        "the_nether" -> x * 8 to z * 8
        else -> x to z
    }

    val (ox1, oz1) = toOverworld(x1, z1, dim1)
    val (ox2, oz2) = toOverworld(x2, z2, dim2)
    val dx = (ox2 - ox1).toDouble()
    val dy = (y2 - y1).toDouble()
    val dz = (oz2 - oz1).toDouble()
    return sqrt(dx * dx + dy * dy + dz * dz).roundToInt()
}

private fun distanceFromPlayer(wp: ConnectWaypoint, player: PlayerData): Int? {
    return distanceBetween(
        wp.x, wp.y, wp.z, wp.dimension,
        player.posX.toInt(), player.posY.toInt(), player.posZ.toInt(), player.dimension,
    )
}

private fun formatDistance(dist: Int?): String = when {
    dist == null -> "Different dimension"
    dist >= 1000 -> "${dist / 1000}.${(dist % 1000) / 100}k blocks"
    else -> "$dist blocks"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectWaypointsScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    val waypoints by viewModel.connectWaypoints.collectAsStateWithLifecycle()
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected
    val primary = MaterialTheme.colorScheme.primary

    var dimensionFilter by remember { mutableStateOf("all") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingWaypoint by remember { mutableStateOf<ConnectWaypoint?>(null) }
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    DisposableEffect(Unit) {
        viewModel.setActiveScreen("waypoints")
        onDispose { viewModel.setActiveScreen(null) }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) viewModel.requestPlayerData()
    }

    // Safety net: if playerData exists but no auto waypoints, force refresh
    LaunchedEffect(playerData, waypoints) {
        if (playerData != null && waypoints.none { it.source == ConnectWaypoint.SOURCE_AUTO }) {
            viewModel.refreshWaypoints()
        }
    }

    val filtered = remember(waypoints, dimensionFilter) {
        val list = if (dimensionFilter == "all") waypoints
        else waypoints.filter { it.dimension == dimensionFilter }

        val auto = list.filter { it.source == ConnectWaypoint.SOURCE_AUTO }
        val custom = list.filter { it.source == ConnectWaypoint.SOURCE_CUSTOM }
        auto to custom
    }
    val (autoWaypoints, customWaypoints) = filtered

    val sortedCustom = remember(customWaypoints, playerData) {
        val pd = playerData
        if (pd != null) {
            customWaypoints.sortedBy { distanceFromPlayer(it, pd) ?: Int.MAX_VALUE }
        } else {
            customWaypoints
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.connect_waypoints), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(
                onClick = { hapticClick(); showCreateDialog = true },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.connect_new_waypoint_desc), modifier = Modifier.size(18.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Offline indicator
            if (!isConnected) {
                item { OfflineIndicator(lastUpdated) }
            }

            // Dimension filter chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = dimensionFilter == "all",
                        onClick = { hapticClick(); dimensionFilter = "all" },
                        label = { Text(stringResource(R.string.all), style = MaterialTheme.typography.labelSmall) },
                    )
                    ConnectWaypoint.DIMENSIONS.forEach { dim ->
                        FilterChip(
                            selected = dimensionFilter == dim,
                            onClick = { hapticClick(); dimensionFilter = dim },
                            label = { Text(dimensionLabel(dim), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            // Auto waypoints section
            if (autoWaypoints.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.connect_live_locations),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(autoWaypoints, key = { it.id }) { wp ->
                    WaypointCard(
                        wp = wp,
                        playerData = playerData,
                        allWaypoints = waypoints,
                        isExpanded = wp.id in expandedIds,
                        onToggle = {
                            expandedIds = if (wp.id in expandedIds) expandedIds - wp.id else expandedIds + wp.id
                        },
                        onEdit = null,
                        onDelete = null,
                        primary = primary,
                    )
                }
            }

            // Custom waypoints section
            if (customWaypoints.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.connect_custom_waypoints),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(sortedCustom, key = { it.id }) { wp ->
                    WaypointCard(
                        wp = wp,
                        playerData = playerData,
                        allWaypoints = waypoints,
                        isExpanded = wp.id in expandedIds,
                        onToggle = {
                            expandedIds = if (wp.id in expandedIds) expandedIds - wp.id else expandedIds + wp.id
                        },
                        onEdit = { editingWaypoint = wp },
                        onDelete = { hapticConfirm(); viewModel.deleteConnectWaypoint(wp.id) },
                        primary = primary,
                    )
                }
            }

            // Empty state
            if (autoWaypoints.isEmpty() && customWaypoints.isEmpty()) {
                item {
                    EmptyState(
                        icon = PixelIcons.Waypoints,
                        title = if (dimensionFilter != "all") stringResource(R.string.connect_no_waypoints_dimension) else stringResource(R.string.connect_no_waypoints_yet),
                        subtitle = if (!isConnected) stringResource(R.string.connect_connect_to_autodetect) else stringResource(R.string.connect_locations_will_appear),
                    )
                }
            }

            // Add at current position button (when connected)
            if (playerData != null) {
                item {
                    OutlinedButton(
                        onClick = { hapticClick(); showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.connect_add_waypoint_at_position))
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showCreateDialog) {
        WaypointDialog(
            title = stringResource(R.string.connect_new_waypoint_title),
            initialX = playerData?.posX?.toInt()?.toString() ?: "",
            initialY = playerData?.posY?.toInt()?.toString() ?: "",
            initialZ = playerData?.posZ?.toInt()?.toString() ?: "",
            initialDimension = playerData?.dimension ?: "overworld",
            onDismiss = { showCreateDialog = false },
            onSave = { name, x, y, z, dim, cat, color, notes ->
                viewModel.createConnectWaypoint(
                    ConnectWaypoint(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name, x = x, y = y, z = z,
                        dimension = dim, category = cat, color = color, notes = notes,
                    ),
                )
                showCreateDialog = false
            },
        )
    }

    if (editingWaypoint != null) {
        val wp = editingWaypoint!!
        WaypointDialog(
            title = stringResource(R.string.connect_edit_waypoint_title),
            initialName = wp.name,
            initialX = wp.x.toString(),
            initialY = wp.y.toString(),
            initialZ = wp.z.toString(),
            initialDimension = wp.dimension,
            initialCategory = wp.category,
            initialColor = wp.color,
            initialNotes = wp.notes,
            onDismiss = { editingWaypoint = null },
            onSave = { name, x, y, z, dim, cat, color, notes ->
                viewModel.updateConnectWaypoint(
                    wp.copy(name = name, x = x, y = y, z = z, dimension = dim, category = cat, color = color, notes = notes),
                )
                editingWaypoint = null
            },
        )
    }
}

@Composable
private fun WaypointCard(
    wp: ConnectWaypoint,
    playerData: PlayerData?,
    allWaypoints: List<ConnectWaypoint>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    primary: Color,
) {
    val hapticClick = rememberHapticClick()
    val wpColor = waypointColor(wp.color, primary)
    val dist = playerData?.let { distanceFromPlayer(wp, it) }

    Column {
        BrowseListItem(
            headline = wp.name,
            supporting = "${wp.x}, ${wp.y}, ${wp.z}",
            supportingMaxLines = 1,
            leadingIcon = PixelIcons.Waypoints,
            leadingIconTint = wpColor,
            modifier = Modifier.clickable { hapticClick(); onToggle() },
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    if (playerData != null) {
                        Text(
                            formatDistance(dist),
                            style = MaterialTheme.typography.bodySmall,
                            color = wpColor,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    CategoryBadge(label = categoryLabel(wp.category), color = wpColor)
                }
            },
        )

        val reduceMotion = LocalReduceAnimations.current
        AnimatedVisibility(
            visible = isExpanded,
            enter = if (reduceMotion) expandVertically(snap()) else expandVertically(),
            exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(),
        ) {
            WaypointDetail(
                wp = wp,
                playerData = playerData,
                allWaypoints = allWaypoints,
                onEdit = onEdit,
                onDelete = onDelete,
                primary = primary,
            )
        }
    }
}

@Composable
private fun WaypointDetail(
    wp: ConnectWaypoint,
    playerData: PlayerData?,
    allWaypoints: List<ConnectWaypoint>,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    primary: Color,
) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    var confirmDelete by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val wpColor = waypointColor(wp.color, primary)
    val context = LocalContext.current

    // Nether coordinate conversion
    val hasConversion = wp.dimension == "overworld" || wp.dimension == "the_nether"
    val convertedDim = if (wp.dimension == "overworld") "the_nether" else "overworld"
    val convertedX = if (wp.dimension == "overworld") wp.x / 8 else wp.x * 8
    val convertedZ = if (wp.dimension == "overworld") wp.z / 8 else wp.z * 8

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        Text(stringResource(R.string.connect_coordinates), style = MaterialTheme.typography.labelSmall, color = primary)
        Text(
            "X: ${wp.x}   Y: ${wp.y}   Z: ${wp.z}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = wpColor,
        )

        // Copy /tp command
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    hapticClick()
                    val cmd = "/tp @s ${wp.x} ${wp.y} ${wp.z}"
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager ?: return@clickable
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("TP Command", cmd))
                    copied = true
                }
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "/tp @s ${wp.x} ${wp.y} ${wp.z}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (copied) stringResource(R.string.copied) else stringResource(R.string.tap_to_copy),
                style = MaterialTheme.typography.labelSmall,
                color = if (copied) Emerald else MaterialTheme.colorScheme.secondary,
            )
        }

        SpyglassDivider()
        StatRow("Dimension", dimensionLabel(wp.dimension))
        StatRow("Category", categoryLabel(wp.category))

        // Nether/Overworld coordinate conversion
        if (hasConversion) {
            SpyglassDivider()
            Text(
                "${dimensionLabel(convertedDim).uppercase()} COORDS",
                style = MaterialTheme.typography.labelSmall,
                color = if (convertedDim == "the_nether") NetherRed else Emerald,
            )
            Text(
                "X: $convertedX   Y: ${wp.y}   Z: $convertedZ",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (convertedDim == "the_nether") NetherRed else Emerald,
            )
        }

        // Distance to other waypoints
        val otherWaypoints = allWaypoints.filter { it.id != wp.id }
        if (otherWaypoints.isNotEmpty()) {
            SpyglassDivider()
            Text(stringResource(R.string.connect_distances), style = MaterialTheme.typography.labelSmall, color = primary)
            otherWaypoints.forEach { other ->
                val dist = distanceBetween(wp.x, wp.y, wp.z, wp.dimension, other.x, other.y, other.z, other.dimension)
                StatRow(other.name, formatDistance(dist))
            }
        }

        if (wp.notes.isNotBlank()) {
            SpyglassDivider()
            Text(stringResource(R.string.connect_notes), style = MaterialTheme.typography.labelSmall, color = primary)
            Text(wp.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (wp.source == ConnectWaypoint.SOURCE_AUTO) {
            SpyglassDivider()
            Text(
                stringResource(R.string.connect_auto_detected),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (onEdit != null || onDelete != null) {
            SpyglassDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onEdit != null) {
                    TextButton(onClick = { hapticClick(); onEdit() }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.edit))
                    }
                }
                if (onDelete != null) {
                    if (!confirmDelete) {
                        TextButton(
                            onClick = { hapticConfirm(); confirmDelete = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Red400),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.delete))
                        }
                    } else {
                        TextButton(
                            onClick = { hapticConfirm(); onDelete() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Red400),
                        ) { Text(stringResource(R.string.confirm_delete)) }
                        TextButton(onClick = { hapticClick(); confirmDelete = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaypointDialog(
    title: String,
    initialName: String = "",
    initialX: String = "",
    initialY: String = "",
    initialZ: String = "",
    initialDimension: String = "overworld",
    initialCategory: String = "base",
    initialColor: String = "gold",
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, x: Int, y: Int, z: Int, dimension: String, category: String, color: String, notes: String) -> Unit,
) {
    val hapticClick = rememberHapticClick()
    val primary = MaterialTheme.colorScheme.primary
    var name by remember { mutableStateOf(initialName) }
    var x by remember { mutableStateOf(initialX) }
    var y by remember { mutableStateOf(initialY) }
    var z by remember { mutableStateOf(initialZ) }
    var dimension by remember { mutableStateOf(initialDimension) }
    var category by remember { mutableStateOf(initialCategory) }
    var color by remember { mutableStateOf(initialColor) }
    var notes by remember { mutableStateOf(initialNotes) }

    val canSave = name.isNotBlank() && x.toIntOrNull() != null && y.toIntOrNull() != null && z.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.connect_waypoint_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, cursorColor = primary),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = x, onValueChange = { x = it },
                        label = { Text(stringResource(R.string.waypoint_coord_x)) }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, cursorColor = primary),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = y, onValueChange = { y = it },
                        label = { Text(stringResource(R.string.waypoint_coord_y)) }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, cursorColor = primary),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = z, onValueChange = { z = it },
                        label = { Text(stringResource(R.string.waypoint_coord_z)) }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, cursorColor = primary),
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(stringResource(R.string.dimension), style = MaterialTheme.typography.labelSmall, color = primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConnectWaypoint.DIMENSIONS.forEach { dim ->
                        FilterChip(
                            selected = dimension == dim,
                            onClick = { hapticClick(); dimension = dim },
                            label = { Text(dimensionLabel(dim), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Text(stringResource(R.string.category), style = MaterialTheme.typography.labelSmall, color = primary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ConnectWaypoint.CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { hapticClick(); category = cat },
                            label = { Text(categoryLabel(cat), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Text(stringResource(R.string.connect_waypoint_color), style = MaterialTheme.typography.labelSmall, color = primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConnectWaypoint.COLORS.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(if (color == c) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .background(waypointColor(c, primary))
                                .clickable { hapticClick(); color = c },
                        ) {
                            if (color == c) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .align(Alignment.Center),
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.connect_waypoint_notes)) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primary, cursorColor = primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { hapticClick(); onSave(name.trim(), x.toInt(), y.toInt(), z.toInt(), dimension, category, color, notes.trim()) },
                enabled = canSave,
            ) { Text(stringResource(R.string.save), color = if (canSave) primary else MaterialTheme.colorScheme.secondary) }
        },
        dismissButton = {
            TextButton(onClick = { hapticClick(); onDismiss() }) { Text(stringResource(R.string.cancel)) }
        },
    )
}
