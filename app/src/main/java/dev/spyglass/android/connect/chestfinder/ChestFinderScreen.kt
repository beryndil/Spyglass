package dev.spyglass.android.connect.chestfinder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ChestDiamondLoader
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.ContainerInfo
import dev.spyglass.android.connect.SearchHit
import dev.spyglass.android.connect.inventory.InventorySlotView
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.ItemTextures
import dev.spyglass.android.core.ui.SpyglassIconImage

/**
 * Storage: search bar + results, then a full browse list of all containers
 * with their items displayed like inventory slots.
 */
@Composable
fun ChestFinderScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.setActiveScreen("chestfinder")
        onDispose { viewModel.setActiveScreen(null) }
    }

    // Request chest contents when screen opens (if connected)
    LaunchedEffect(connectionState.isConnected) {
        if (connectionState.isConnected) {
            viewModel.requestChests()
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
            Text(stringResource(R.string.connect_storage), style = MaterialTheme.typography.titleMedium)
        }

        ChestFinderContent(viewModel = viewModel)
    }
}

@Composable
fun ChestFinderContent(viewModel: ConnectViewModel) {
    val scope = rememberCoroutineScope()
    val finderState = remember { ChestFinderState(viewModel, scope) }
    val query by finderState.query.collectAsStateWithLifecycle()
    val results by finderState.results.collectAsStateWithLifecycle()
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val chestContents by viewModel.chestContents.collectAsStateWithLifecycle()

    val hits = results?.results ?: emptyList()
    val containers = chestContents?.containers ?: emptyList()
    val isLoading = chestContents == null
    val isSearching = query.isNotBlank()

    // Local fallback search through loaded containers when server search returns empty
    val localMatches = remember(containers, query, hits) {
        if (!isSearching || hits.isNotEmpty() || containers.isEmpty()) emptyList()
        else {
            val q = query.lowercase()
            containers.filter { c ->
                c.items.any { it.id.lowercase().contains(q) } ||
                    c.customName?.lowercase()?.contains(q) == true
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // Search bar
        item(key = "search_bar") {
            OutlinedTextField(
                value = query,
                onValueChange = { finderState.onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.connect_search_items_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { finderState.clear() }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.connect_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        // Search results — try server hits first, fall back to local container filter
        if (isSearching) {
            if (hits.isNotEmpty()) {
                items(hits.take(15), key = { "hit_${it.itemId}" }) { hit ->
                    SearchHitCard(
                        hit = hit,
                        playerX = playerData?.posX ?: 0.0,
                        playerZ = playerData?.posZ ?: 0.0,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (hits.size > 15) {
                    item(key = "more_results") {
                        Text(
                            stringResource(R.string.connect_showing_results, hits.size),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (localMatches.isNotEmpty()) {
                item(key = "local_match_header") {
                    Text(
                        stringResource(R.string.connect_container_count, localMatches.size),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(localMatches, key = { "local_${it.type}_${it.x}_${it.y}_${it.z}" }) { container ->
                    ContainerCard(
                        container = container,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            } else {
                item(key = "no_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.connect_no_items_found, query),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // All containers browse section
        if (!isSearching) {
            if (isLoading) {
                item(key = "loading") {
                    ChestDiamondLoader(
                        statusText = stringResource(R.string.connect_anim_scanning_chests),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    )
                }
            } else if (containers.isEmpty()) {
                item(key = "no_containers") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.connect_no_containers),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item(key = "container_count") {
                    Text(
                        stringResource(R.string.connect_container_count, containers.size),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                items(containers, key = { "${it.type}_${it.x}_${it.y}_${it.z}" }) { container ->
                    ContainerCard(
                        container = container,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContainerCard(container: ContainerInfo, modifier: Modifier = Modifier) {
    val containerType = container.type.replace("_", " ").replaceFirstChar { it.uppercase() }
    val label = container.customName ?: containerType
    val coords = "${container.x}, ${container.y}, ${container.z}"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Container header: name + coords
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (container.customName != null) {
                        Text(
                            containerType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    coords,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (container.items.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                // Items grid — 9 columns like inventory
                val columns = 9
                val rows = (container.items.size + columns - 1) / columns
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            for (col in 0 until columns) {
                                val index = row * columns + col
                                if (index < container.items.size) {
                                    InventorySlotView(
                                        item = container.items[index],
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHitCard(
    hit: SearchHit,
    playerX: Double,
    playerZ: Double,
    modifier: Modifier = Modifier,
) {
    val icon = ItemTextures.get(hit.itemId)
    val displayName = hit.itemId.replace("_", " ").replaceFirstChar { it.uppercase() }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Item header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (icon != null) {
                    SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = Color.Unspecified)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "x${hit.totalCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Location list
            if (hit.locations.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                hit.locations.take(5).forEach { container ->
                    val containerType = container.type.replace("_", " ").replaceFirstChar { it.uppercase() }
                    val label = container.customName ?: containerType
                    val count = container.items.sumOf { it.count }
                    val dist = distance(playerX, playerZ, container.x.toDouble(), container.z.toDouble())

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "$label (x$count)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${container.x}, ${container.y}, ${container.z}  (${dist}m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (hit.locations.size > 5) {
                    Text(
                        stringResource(R.string.connect_and_more_locations, hit.locations.size - 5),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun distance(x1: Double, z1: Double, x2: Double, z2: Double): Int {
    val dx = x1 - x2
    val dz = z1 - z2
    return kotlin.math.sqrt(dx * dx + dz * dz).toInt()
}
