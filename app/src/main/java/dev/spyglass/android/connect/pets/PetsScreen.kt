package dev.spyglass.android.connect.pets

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.PetData
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.client.ConnectionState
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

private val COLLAR_COLORS = mapOf(
    0 to "White", 1 to "Orange", 2 to "Magenta", 3 to "Light Blue",
    4 to "Yellow", 5 to "Lime", 6 to "Pink", 7 to "Gray",
    8 to "Light Gray", 9 to "Cyan", 10 to "Purple", 11 to "Blue",
    12 to "Brown", 13 to "Green", 14 to "Red", 15 to "Black",
)

@Composable
fun PetsScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    val pets by viewModel.pets.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected

    DisposableEffect(Unit) {
        viewModel.setActiveScreen("pets")
        onDispose { viewModel.setActiveScreen(null) }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) viewModel.requestPets()
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
            Text(stringResource(R.string.connect_pets), style = MaterialTheme.typography.titleMedium)
        }

        if (!isConnected && lastUpdated != null) {
            OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (pets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isConnected) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.connect_scanning_tamed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(stringResource(R.string.connect_no_tamed_mobs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return
        }

        val grouped = pets.groupBy { it.entityType }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            grouped.forEach { (type, typePets) ->
                Text(
                    formatEntityType(type) + " (${typePets.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                typePets.forEach { pet ->
                    PetCard(pet)
                }
            }
        }
    }
}

@Composable
private fun PetCard(pet: PetData) {
    ResultCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpyglassIconImage(
                MobTextures.get(pet.entityType) ?: PixelIcons.Mob,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pet.customName ?: formatEntityType(pet.entityType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Health bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val maxH = if (pet.maxHealth > 0) pet.maxHealth else 20f
                    Text(
                        "\u2764 ${pet.health.toInt()}/${maxH.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (pet.ownerName != null) {
                        Text(
                            stringResource(R.string.connect_owner_label, pet.ownerName!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "${pet.dimension.replace("_", " ").replaceFirstChar { it.uppercase() }} · ${pet.posX.toInt()}, ${pet.posY.toInt()}, ${pet.posZ.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Extra details
                val details = buildList {
                    if (pet.collarColor >= 0) add(stringResource(R.string.connect_collar, COLLAR_COLORS[pet.collarColor] ?: stringResource(R.string.connect_unknown)))
                    if (pet.catVariant != null) add(stringResource(R.string.connect_variant, pet.catVariant!!.replace("_", " ").replaceFirstChar { it.uppercase() }))
                    if (pet.horseSpeed > 0) add(stringResource(R.string.connect_speed_format, "%.2f".format(pet.horseSpeed * 43.17)))
                    if (pet.horseJump > 0) add(stringResource(R.string.connect_jump_format, "%.1f".format(pet.horseJump * 5.0)))
                }
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun formatEntityType(type: String): String =
    type.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
