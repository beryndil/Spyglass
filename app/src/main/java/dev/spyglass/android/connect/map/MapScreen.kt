package dev.spyglass.android.connect.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ConnectViewModel
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.ServerSyncNote

/**
 * Canvas-based overhead map: accumulates tiles like Minecraft's in-game map.
 * Tiles are fetched around the player's physical position, not the viewport.
 * Panning shows cached tiles only — new tiles load as the player moves.
 */
@Composable
fun MapScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.connect_world_map), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }

        MapContent(viewModel = viewModel)
    }
}

@Composable
fun MapContent(viewModel: ConnectViewModel) {
    val hapticClick = rememberHapticClick()
    val coroutineScope = rememberCoroutineScope()
    val mapState = remember { MapState(viewModel, coroutineScope) }

    // Release bitmaps and cancel jobs when leaving the map screen
    DisposableEffect(Unit) {
        onDispose { mapState.clearAll() }
    }
    val tileRevision by mapState.tileRevision.collectAsStateWithLifecycle()
    val isLoading by mapState.isLoading.collectAsStateWithLifecycle()
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val selectedWorld by viewModel.selectedWorld.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected
    val isServerWorld = selectedWorld?.startsWith("ptero_") == true

    // Seed from ViewModel's accumulated tiles (survives navigation), then collect live batches
    LaunchedEffect(Unit) {
        viewModel.getAllAccumulatedTiles()?.let { mapState.mergeTiles(it) }
        viewModel.mapTileBatch.collect { payload ->
            mapState.mergeTiles(payload)
        }
    }

    // Clear cache on world change
    LaunchedEffect(selectedWorld) {
        mapState.clearAll()
    }

    // Request initial tiles when connected
    LaunchedEffect(isConnected) {
        if (isConnected) {
            mapState.requestAroundPlayer()
        }
    }

    // Watch player position — request new tiles when the player moves to a new chunk
    LaunchedEffect(playerData?.posX, playerData?.posZ, playerData?.dimension) {
        val player = playerData ?: return@LaunchedEffect
        if (!isConnected) return@LaunchedEffect
        mapState.onPlayerMoved(player.posX, player.posZ, player.dimension)
    }

    if (!isConnected && lastUpdated != null) {
        OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
    } else if (isConnected && isServerWorld) {
        ServerSyncNote(modifier = Modifier.padding(horizontal = 16.dp))
    }

    var scale by remember { mutableFloatStateOf(4f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Dimension switcher + center button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            val dimensions = listOf("overworld", "the_nether", "the_end")
            dimensions.forEach { dim ->
                val selected = mapState.currentDimension == dim
                val label = when (dim) {
                    "overworld" -> "Overworld"
                    "the_nether" -> "Nether"
                    "the_end" -> "End"
                    else -> dim
                }
                FilterChip(
                    selected = selected,
                    onClick = {
                        hapticClick()
                        mapState.switchDimension(dim)
                    },
                    enabled = true,
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }

            IconButton(
                onClick = {
                    offsetX = 0f
                    offsetY = 0f
                    scale = 4f
                    if (isConnected) mapState.requestAroundPlayer()
                },
                enabled = isConnected,
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.connect_center_on_player))
            }
        }

        // Map canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 16f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
        ) {
            @Suppress("UNUSED_VARIABLE")
            val revision = tileRevision

            val cachedTiles = mapState.tileCache.getTilesForDimension(mapState.currentDimension)
            val playerPos = mapState.tileCache.getPlayerPosition(mapState.currentDimension)
            val px = playerPos?.first ?: playerData?.posX ?: 0.0
            val pz = playerPos?.second ?: playerData?.posZ ?: 0.0

            Canvas(modifier = Modifier.fillMaxSize()) {
                val tileSize = 16f * scale
                val canvasWidth = size.width
                val canvasHeight = size.height

                cachedTiles.values.forEach { cached ->
                    val tile = cached.tile
                    val bitmap = cached.bitmap ?: return@forEach

                    val relX = (tile.chunkX * 16 - px).toFloat()
                    val relZ = (tile.chunkZ * 16 - pz).toFloat()

                    val screenX = canvasWidth / 2 + relX / 16 * tileSize + offsetX
                    val screenY = canvasHeight / 2 + relZ / 16 * tileSize + offsetY

                    // Frustum culling
                    if (screenX + tileSize < 0 || screenX > canvasWidth ||
                        screenY + tileSize < 0 || screenY > canvasHeight
                    ) return@forEach

                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset(screenX.toInt(), screenY.toInt()),
                        dstSize = IntSize(tileSize.toInt(), tileSize.toInt()),
                    )
                }

                // Player marker
                val playerScreenX = canvasWidth / 2 + offsetX
                val playerScreenY = canvasHeight / 2 + offsetY
                drawCircle(color = Color.Red, radius = 6f, center = Offset(playerScreenX, playerScreenY))
                drawCircle(color = Color.White, radius = 4f, center = Offset(playerScreenX, playerScreenY))

            }

            // Loading indicators
            if (isLoading && cachedTiles.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            } else if (isLoading && cachedTiles.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Coordinates label
            Text(
                "X: ${playerData?.posX?.toInt() ?: 0}  Z: ${playerData?.posZ?.toInt() ?: 0}  Zoom: ${"%.1f".format(scale)}x",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}
