package dev.spyglass.android.connect.players

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ChestDiamondLoader
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.PlayerSummary
import dev.spyglass.android.connect.client.SkinManager
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import kotlinx.coroutines.launch

@Composable
fun PlayersScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
    onSelectPlayer: (String) -> Unit,
    onCompare: (String) -> Unit,
) {
    val playerList by viewModel.playerList.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val loadingStatus by viewModel.loadingStatus.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected
    val headCache = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(isConnected) {
        if (isConnected) viewModel.requestPlayerList()
    }

    LaunchedEffect(playerList) {
        playerList.forEach { player ->
            if (player.uuid !in headCache) {
                launch {
                    SkinManager.fetchSkin(player.uuid)?.let {
                        headCache[player.uuid] = it
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.connect_players), style = MaterialTheme.typography.titleMedium)
        }

        if (playerList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isConnected) {
                    ChestDiamondLoader(statusText = loadingStatus)
                } else {
                    Text(stringResource(R.string.connect_no_player_data_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            playerList.forEach { player ->
                PlayerCard(
                    player = player,
                    headBitmap = headCache[player.uuid],
                    onTap = { onSelectPlayer(player.uuid) },
                    onCompare = { onCompare(player.uuid) },
                    showCompare = playerList.size > 1,
                )
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: PlayerSummary,
    headBitmap: Bitmap?,
    onTap: () -> Unit,
    onCompare: () -> Unit,
    showCompare: Boolean,
) {
    ResultCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTap() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (headBitmap != null) {
                SpyglassIconImage(
                    SpyglassIcon.BitmapIcon(headBitmap),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                SpyglassIconImage(
                    PixelIcons.Steve,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        player.name ?: stringResource(R.string.connect_unknown),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (player.isOwner) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.connect_owner),
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "\u2764 ${player.health.toInt()}/20",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "\uD83C\uDF56 ${player.foodLevel}/20",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "XP ${player.xpLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${player.dimension.replace("_", " ").replaceFirstChar { it.uppercase() }} · ${player.posX.toInt()}, ${player.posY.toInt()}, ${player.posZ.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showCompare) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.connect_compare),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onCompare() },
            )
        }
    }
}
