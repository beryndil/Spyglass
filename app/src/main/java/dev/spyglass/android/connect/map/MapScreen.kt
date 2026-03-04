package dev.spyglass.android.connect.map

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.MapTile
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.StructureLocation
import dev.spyglass.android.connect.client.ConnectionState

/**
 * Canvas-based overhead map: receives base64 PNG tiles from desktop,
 * supports pinch-to-zoom, pan, dimension switcher.
 */
@Composable
fun MapScreen(
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
            Text("World Map", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }

        MapContent(viewModel = viewModel)
    }
}

@Composable
fun MapContent(viewModel: ConnectViewModel) {
    val mapState = remember { MapState(viewModel) }
    val mapData by mapState.mapData.collectAsStateWithLifecycle()
    val structures by viewModel.structures.collectAsStateWithLifecycle()
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected

    // Request initial data only when connected
    LaunchedEffect(isConnected) {
        if (isConnected) {
            mapState.requestAroundPlayer()
            viewModel.requestStructures()
        }
    }

    if (!isConnected && lastUpdated != null) {
        OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
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
                    "overworld" -> "OW"
                    "the_nether" -> "N"
                    "the_end" -> "E"
                    else -> dim
                }
                FilterChip(
                    selected = selected,
                    onClick = { if (isConnected) mapState.switchDimension(dim) },
                    enabled = isConnected,
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
                Icon(Icons.Filled.MyLocation, contentDescription = "Center on player")
            }
        }

        // Map canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 16f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
        ) {
            val tiles = mapData?.tiles ?: emptyList()
            val px = mapData?.playerX ?: 0.0
            val pz = mapData?.playerZ ?: 0.0

            Canvas(modifier = Modifier.fillMaxSize()) {
                val tileSize = 16f * scale

                tiles.forEach { tile ->
                    drawMapTile(tile, px, pz, tileSize, offsetX, offsetY)
                }

                // Draw player marker
                val playerScreenX = size.width / 2 + offsetX
                val playerScreenY = size.height / 2 + offsetY
                drawCircle(
                    color = Color.Red,
                    radius = 6f,
                    center = Offset(playerScreenX, playerScreenY),
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(playerScreenX, playerScreenY),
                )

                // Draw structure markers
                structures.filter { it.dimension == mapState.currentDimension }.forEach { structure ->
                    val sx = size.width / 2 + ((structure.x - px.toFloat()) * scale / 16f * tileSize / scale) + offsetX
                    val sy = size.height / 2 + ((structure.z - pz.toFloat()) * scale / 16f * tileSize / scale) + offsetY
                    drawCircle(
                        color = Color.Yellow,
                        radius = 5f,
                        center = Offset(sx, sy),
                    )
                }
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

private fun DrawScope.drawMapTile(
    tile: MapTile,
    playerX: Double,
    playerZ: Double,
    tileSize: Float,
    offsetX: Float,
    offsetY: Float,
) {
    try {
        val bytes = Base64.decode(tile.imageBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return

        val relX = (tile.chunkX * 16 - playerX).toFloat()
        val relZ = (tile.chunkZ * 16 - playerZ).toFloat()

        val screenX = size.width / 2 + relX / 16 * tileSize + offsetX
        val screenY = size.height / 2 + relZ / 16 * tileSize + offsetY

        drawImage(
            image = bitmap.asImageBitmap(),
            dstOffset = IntOffset(screenX.toInt(), screenY.toInt()),
            dstSize = IntSize(tileSize.toInt(), tileSize.toInt()),
        )
    } catch (_: Exception) {
        // Skip corrupt tiles
    }
}
