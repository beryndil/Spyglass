package dev.spyglass.android.connect.players

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ChestDiamondLoader
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.PlayerData
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun CompareScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val compareData by viewModel.comparePlayerData.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.connect_compare_players), style = MaterialTheme.typography.titleMedium)
        }

        if (playerData == null || compareData == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isConnected) {
                    ChestDiamondLoader()
                } else {
                    Text(stringResource(R.string.connect_connect_to_compare), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return
        }

        val p1 = playerData!!
        val p2 = compareData!!

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header row with player names
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    p1.playerName ?: stringResource(R.string.connect_player_1),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    p2.playerName ?: stringResource(R.string.connect_player_2),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }

            CompareRow(stringResource(R.string.connect_health), "${p1.health.toInt()} / 20", "${p2.health.toInt()} / 20",
                p1.health > p2.health, p2.health > p1.health)
            CompareRow(stringResource(R.string.connect_food), "${p1.foodLevel} / 20", "${p2.foodLevel} / 20",
                p1.foodLevel > p2.foodLevel, p2.foodLevel > p1.foodLevel)
            CompareRow(stringResource(R.string.connect_xp_level), "${p1.xpLevel}", "${p2.xpLevel}",
                p1.xpLevel > p2.xpLevel, p2.xpLevel > p1.xpLevel)

            SpyglassDivider()

            CompareRow(stringResource(R.string.connect_compare_dimension),
                p1.dimension.replace("_", " ").replaceFirstChar { it.uppercase() },
                p2.dimension.replace("_", " ").replaceFirstChar { it.uppercase() })
            CompareRow(stringResource(R.string.connect_compare_position),
                "${p1.posX.toInt()}, ${p1.posY.toInt()}, ${p1.posZ.toInt()}",
                "${p2.posX.toInt()}, ${p2.posY.toInt()}, ${p2.posZ.toInt()}")

            SpyglassDivider()

            CompareRow(stringResource(R.string.connect_compare_inventory),
                stringResource(R.string.connect_items_format, p1.inventory.sumOf { it.count }),
                stringResource(R.string.connect_items_format, p2.inventory.sumOf { it.count }))
            CompareRow(stringResource(R.string.connect_compare_armor),
                stringResource(R.string.connect_pieces_format, p1.armor.size),
                stringResource(R.string.connect_pieces_format, p2.armor.size))
            CompareRow(stringResource(R.string.connect_ender_chest),
                stringResource(R.string.connect_items_format, p1.enderChest.sumOf { it.count }),
                stringResource(R.string.connect_items_format, p2.enderChest.sumOf { it.count }))
        }
    }
}

@Composable
private fun CompareRow(
    label: String,
    left: String,
    right: String,
    leftWins: Boolean = false,
    rightWins: Boolean = false,
) {
    ResultCard {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                left,
                style = MaterialTheme.typography.bodyMedium,
                color = if (leftWins) Emerald else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                right,
                style = MaterialTheme.typography.bodyMedium,
                color = if (rightWins) Emerald else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
