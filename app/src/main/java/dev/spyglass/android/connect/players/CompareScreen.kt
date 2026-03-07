package dev.spyglass.android.connect.players

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.PlayerData
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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Compare Players", style = MaterialTheme.typography.titleMedium)
        }

        if (playerData == null || compareData == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isConnected) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading comparison data...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Connect to compare players", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    p1.playerName ?: "Player 1",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    p2.playerName ?: "Player 2",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }

            CompareRow("Health", "${p1.health.toInt()} / 20", "${p2.health.toInt()} / 20",
                p1.health > p2.health, p2.health > p1.health)
            CompareRow("Food", "${p1.foodLevel} / 20", "${p2.foodLevel} / 20",
                p1.foodLevel > p2.foodLevel, p2.foodLevel > p1.foodLevel)
            CompareRow("XP Level", "${p1.xpLevel}", "${p2.xpLevel}",
                p1.xpLevel > p2.xpLevel, p2.xpLevel > p1.xpLevel)

            SpyglassDivider()

            CompareRow("Dimension",
                p1.dimension.replace("_", " ").replaceFirstChar { it.uppercase() },
                p2.dimension.replace("_", " ").replaceFirstChar { it.uppercase() })
            CompareRow("Position",
                "${p1.posX.toInt()}, ${p1.posY.toInt()}, ${p1.posZ.toInt()}",
                "${p2.posX.toInt()}, ${p2.posY.toInt()}, ${p2.posZ.toInt()}")

            SpyglassDivider()

            CompareRow("Inventory",
                "${p1.inventory.sumOf { it.count }} items",
                "${p2.inventory.sumOf { it.count }} items")
            CompareRow("Armor",
                "${p1.armor.size} pieces",
                "${p2.armor.size} pieces")
            CompareRow("Ender Chest",
                "${p1.enderChest.sumOf { it.count }} items",
                "${p2.enderChest.sumOf { it.count }} items")
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
