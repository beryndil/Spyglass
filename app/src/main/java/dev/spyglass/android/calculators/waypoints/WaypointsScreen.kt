package dev.spyglass.android.calculators.waypoints

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.WaypointEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val CATEGORIES = listOf("base", "farm", "portal", "spawner", "village", "monument", "other")
private val DIMENSIONS = listOf("overworld", "nether", "end")
private val COLORS = listOf("gold", "green", "red", "blue", "purple")

internal fun waypointColor(color: String): Color = when (color) {
    "gold" -> Gold
    "green" -> Emerald
    "red" -> NetherRed
    "blue" -> PotionBlue
    "purple" -> EnderPurple
    else -> Gold
}

private fun categoryLabel(cat: String): String = cat.replaceFirstChar { it.uppercase() }
private fun dimensionLabel(dim: String): String = when (dim) {
    "overworld" -> "Overworld"
    "nether" -> "Nether"
    "end" -> "The End"
    else -> dim.replaceFirstChar { it.uppercase() }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WaypointsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    val waypoints: StateFlow<List<WaypointEntity>> = combine(_query.debounce(200), _categoryFilter) { q, cat ->
        if (cat != "all") repo.waypointsByCategory(cat)
        else repo.searchWaypoints(q)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedIds: StateFlow<Set<Long>> = _expandedIds.asStateFlow()

    fun setQuery(q: String) { _query.value = q }
    fun setCategoryFilter(c: String) { _categoryFilter.value = c }
    fun toggleExpanded(id: Long) {
        val current = _expandedIds.value
        _expandedIds.value = if (id in current) current - id else current + id
    }

    fun createWaypoint(name: String, x: Int, y: Int, z: Int, dimension: String, category: String, color: String, notes: String) {
        viewModelScope.launch {
            repo.createWaypoint(WaypointEntity(
                name = name, x = x, y = y, z = z,
                dimension = dimension, category = category, color = color,
                notes = notes, createdAt = System.currentTimeMillis(),
            ))
        }
    }

    fun updateWaypoint(id: Long, name: String, x: Int, y: Int, z: Int, dimension: String, category: String, color: String, notes: String) {
        viewModelScope.launch { repo.updateWaypoint(id, name, x, y, z, dimension, category, color, notes) }
    }

    fun deleteWaypoint(id: Long) {
        viewModelScope.launch {
            repo.deleteWaypoint(id)
            _expandedIds.value = _expandedIds.value - id
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaypointsScreen(vm: WaypointsViewModel = viewModel()) {
    val query by vm.query.collectAsState()
    val categoryFilter by vm.categoryFilter.collectAsState()
    val waypoints by vm.waypoints.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingWaypoint by remember { mutableStateOf<WaypointEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query, onValueChange = vm::setQuery,
                placeholder = { Text("Search waypoints\u2026", color = Stone500) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Gold.copy(alpha = 0.15f), contentColor = Gold),
            ) {
                Icon(Icons.Default.Add, contentDescription = "New waypoint", modifier = Modifier.size(18.dp))
            }
        }

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = categoryFilter == "all",
                onClick = { vm.setCategoryFilter("all") },
                label = { Text("All", style = MaterialTheme.typography.labelSmall) },
            )
            CATEGORIES.forEach { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = { vm.setCategoryFilter(cat) },
                    label = { Text(categoryLabel(cat), style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Biome,
                    title = "Waypoints",
                    description = "Save and organize coordinates for your important Minecraft locations.",
                    stat = "${waypoints.size} waypoints",
                )
            }

            if (waypoints.isEmpty()) {
                item {
                    EmptyState(
                        icon = PixelIcons.Biome,
                        title = if (query.isNotBlank() || categoryFilter != "all") "No waypoints found" else "No waypoints saved",
                        subtitle = if (query.isNotBlank() || categoryFilter != "all") "Try a different search or filter" else "Tap + to save your first location",
                    )
                }
            }

            items(waypoints, key = { it.id }) { wp ->
                val isExpanded = wp.id in expandedIds
                val wpColor = waypointColor(wp.color)
                Column {
                    BrowseListItem(
                        headline = wp.name,
                        supporting = "${wp.x}, ${wp.y}, ${wp.z}",
                        supportingMaxLines = 1,
                        leadingIcon = PixelIcons.Biome,
                        leadingIconTint = wpColor,
                        modifier = Modifier.clickable { vm.toggleExpanded(wp.id) },
                        trailing = {
                            Column(horizontalAlignment = Alignment.End) {
                                CategoryBadge(label = categoryLabel(wp.category), color = wpColor)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    dimensionLabel(wp.dimension),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Stone500,
                                )
                            }
                        },
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        WaypointDetailCard(
                            wp = wp,
                            onEdit = { editingWaypoint = wp },
                            onDelete = { vm.deleteWaypoint(wp.id) },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showCreateDialog) {
        WaypointDialog(
            title = "New Waypoint",
            onDismiss = { showCreateDialog = false },
            onSave = { name, x, y, z, dim, cat, color, notes ->
                vm.createWaypoint(name, x, y, z, dim, cat, color, notes)
                showCreateDialog = false
            },
        )
    }

    if (editingWaypoint != null) {
        val wp = editingWaypoint!!
        WaypointDialog(
            title = "Edit Waypoint",
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
                vm.updateWaypoint(wp.id, name, x, y, z, dim, cat, color, notes)
                editingWaypoint = null
            },
        )
    }
}

@Composable
private fun WaypointDetailCard(
    wp: WaypointEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val wpColor = waypointColor(wp.color)

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            "COORDINATES",
            style = MaterialTheme.typography.labelSmall,
            color = Gold,
        )
        Text(
            "X: ${wp.x}   Y: ${wp.y}   Z: ${wp.z}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = wpColor,
        )
        SpyglassDivider()
        StatRow("Dimension", dimensionLabel(wp.dimension))
        StatRow("Category", categoryLabel(wp.category))
        if (wp.notes.isNotBlank()) {
            SpyglassDivider()
            Text(
                "NOTES",
                style = MaterialTheme.typography.labelSmall,
                color = Gold,
            )
            Text(wp.notes, style = MaterialTheme.typography.bodySmall, color = Stone300)
        }
        SpyglassDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Edit")
            }
            if (!confirmDelete) {
                TextButton(onClick = { confirmDelete = true }, colors = ButtonDefaults.textButtonColors(contentColor = Red400)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            } else {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Red400)) {
                    Text("Confirm Delete")
                }
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
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
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, cursorColor = Gold),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = x, onValueChange = { x = it },
                        label = { Text("X") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, cursorColor = Gold),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = y, onValueChange = { y = it },
                        label = { Text("Y") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, cursorColor = Gold),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = z, onValueChange = { z = it },
                        label = { Text("Z") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, cursorColor = Gold),
                        modifier = Modifier.weight(1f),
                    )
                }

                Text("Dimension", style = MaterialTheme.typography.labelSmall, color = Gold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DIMENSIONS.forEach { dim ->
                        FilterChip(
                            selected = dimension == dim,
                            onClick = { dimension = dim },
                            label = { Text(dimensionLabel(dim), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelSmall, color = Gold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(categoryLabel(cat), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelSmall, color = Gold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLORS.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(if (color == c) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .background(waypointColor(c))
                                .clickable { color = c },
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
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, cursorColor = Gold),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), x.toInt(), y.toInt(), z.toInt(), dimension, category, color, notes.trim()) },
                enabled = canSave,
            ) { Text("Save", color = if (canSave) Gold else Stone500) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
