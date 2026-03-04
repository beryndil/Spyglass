package dev.spyglass.android.connect.chestfinder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.SearchHit
import dev.spyglass.android.core.ui.ItemTextures
import dev.spyglass.android.core.ui.SpyglassIconImage

/**
 * Search bar + results list: item icon, name, count, container type, coordinates.
 */
@Composable
fun ChestFinderScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Chest Finder", style = MaterialTheme.typography.titleMedium)
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

    Column(modifier = Modifier.fillMaxWidth()) {
        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { finderState.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search items (e.g., diamond, iron)") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { finderState.clear() }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(8.dp))

        val hits = results?.results ?: emptyList()

        if (query.isNotBlank() && hits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No items found matching \"$query\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                hits.take(15).forEach { hit ->
                    SearchHitCard(
                        hit = hit,
                        playerX = playerData?.posX ?: 0.0,
                        playerZ = playerData?.posZ ?: 0.0,
                    )
                }
                if (hits.size > 15) {
                    Text(
                        "Showing 15 of ${hits.size} results — refine your search",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHitCard(hit: SearchHit, playerX: Double, playerZ: Double) {
    val icon = ItemTextures.get(hit.itemId)
    val displayName = hit.itemId.replace("_", " ").replaceFirstChar { it.uppercase() }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    Spacer(Modifier.height(2.dp))
                }

                if (hit.locations.size > 5) {
                    Text(
                        "...and ${hit.locations.size - 5} more locations",
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
