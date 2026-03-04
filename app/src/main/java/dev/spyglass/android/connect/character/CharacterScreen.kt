package dev.spyglass.android.connect.character

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.client.ConnectionState
import timber.log.Timber
import dev.spyglass.android.connect.PlayerData
import dev.spyglass.android.connect.gear.EnchantRecommendation
import dev.spyglass.android.connect.gear.GearAnalysis
import dev.spyglass.android.connect.gear.SlotAnalysis
import dev.spyglass.android.connect.gear.SlotType
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.navigation.BrowseTarget

private val UpgradeOrange = Color(0xFFFF9800)

@Composable
fun CharacterScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
    onBrowseTarget: (BrowseTarget) -> Unit = {},
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val playerData by viewModel.playerData.collectAsStateWithLifecycle()
    val playerSkin by viewModel.playerSkin.collectAsStateWithLifecycle()
    val playerBodySkin by viewModel.playerBodySkin.collectAsStateWithLifecycle()
    val playerName by viewModel.playerName.collectAsStateWithLifecycle()
    val gearAnalysis by viewModel.gearAnalysis.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected

    // Request fresh player data when screen opens (only if connected)
    LaunchedEffect(isConnected) {
        if (isConnected) {
            Timber.d("CharacterScreen: requesting player data (current=${playerData != null})")
            viewModel.requestPlayerData()
            // Retry once after delay if data still missing
            kotlinx.coroutines.delay(3000)
            if (viewModel.playerData.value == null) {
                Timber.d("CharacterScreen: retrying player data request")
                viewModel.requestPlayerData()
            }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Character", style = MaterialTheme.typography.titleMedium)
        }

        if (!isConnected && lastUpdated != null) {
            OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
        }

        CharacterContent(
            playerData = playerData,
            playerSkin = playerSkin,
            playerBodySkin = playerBodySkin,
            playerName = playerName,
            gearAnalysis = gearAnalysis,
            isOffline = !isConnected,
            onBrowseItem = { itemId -> onBrowseTarget(BrowseTarget(1, itemId)) },
            onBrowseEnchant = { enchantId -> onBrowseTarget(BrowseTarget(7, enchantId)) },
        )
    }
}

@Composable
private fun CharacterContent(
    playerData: PlayerData?,
    playerSkin: Bitmap?,
    playerBodySkin: Bitmap?,
    playerName: String?,
    gearAnalysis: GearAnalysis?,
    isOffline: Boolean = false,
    onBrowseItem: (String) -> Unit,
    onBrowseEnchant: (String) -> Unit,
) {
    if (playerData == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isOffline) {
                    Text("No cached player data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Loading player data...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Avatar + Info row ──
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Body render (dungeons pose)
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .widthIn(min = 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (playerBodySkin == null) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (playerBodySkin != null) {
                    Image(
                        bitmap = playerBodySkin.asImageBitmap(),
                        contentDescription = "Player body",
                        modifier = Modifier.fillMaxHeight(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    SpyglassIconImage(
                        PixelIcons.Mob,
                        contentDescription = "Player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            // Right side: IGN, UUID, then armor boxes
            Column(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // IGN
                Text("IGN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    playerName ?: "Unknown Player",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // UUID (tap to copy)
                Text("UUID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                val uuid = playerData.playerUuid ?: "—"
                val clipboardManager = LocalClipboardManager.current
                Text(
                    uuid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(uuid))
                    },
                )

                Spacer(Modifier.weight(1f))

                // Armor boxes — horizontal row
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val armorSlotTypes = listOf(SlotType.HEAD, SlotType.CHEST, SlotType.LEGS, SlotType.FEET)
                    armorSlotTypes.forEach { slotType ->
                        val slotAnalysis = gearAnalysis?.slots?.find { it.slotType == slotType }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val itemId = slotAnalysis?.item?.id
                            if (itemId != null) {
                                val tex = ItemTextures.get(itemId)
                                if (tex != null) {
                                    SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Dimension + coords ──
        Text(
            "${playerData.dimension.replace("_", " ").replaceFirstChar { it.uppercase() }} · ${playerData.posX.toInt()}, ${playerData.posY.toInt()}, ${playerData.posZ.toInt()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Stat cards ──
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn("Health", "${playerData.health.toInt()} / 20")
                StatColumn("Food", "${playerData.foodLevel} / 20")
                StatColumn("XP", "${playerData.xpLevel}")
            }
        }

        // ── Equipment Analysis ──
        Text(
            "Equipment Analysis",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        if (gearAnalysis != null) {
            gearAnalysis.slots.forEach { slotAnalysis ->
                GearSlotCard(
                    slotAnalysis = slotAnalysis,
                    onBrowseItem = onBrowseItem,
                    onBrowseEnchant = onBrowseEnchant,
                )
            }
        } else {
            ResultCard {
                Text(
                    "Analyzing gear...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun slotLabel(slotType: SlotType): String = when (slotType) {
    SlotType.HEAD -> "Head"
    SlotType.CHEST -> "Chest"
    SlotType.LEGS -> "Legs"
    SlotType.FEET -> "Feet"
    SlotType.MAIN_HAND -> "Main Hand"
    SlotType.OFF_HAND -> "Off Hand"
}

private fun formatItemName(id: String): String =
    id.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun romanNumeral(level: Int): String = when (level) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"
    else -> level.toString()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GearSlotCard(
    slotAnalysis: SlotAnalysis,
    onBrowseItem: (String) -> Unit,
    onBrowseEnchant: (String) -> Unit,
) {
    ResultCard {
        val item = slotAnalysis.item

        if (item != null) {
            // ── Has item ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBrowseItem(item.id) },
            ) {
                val tex = ItemTextures.get(item.id)
                if (tex != null) {
                    SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    item.customName ?: formatItemName(item.id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    slotLabel(slotAnalysis.slotType),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Current enchants (at max level) — green
            if (slotAnalysis.currentEnchants.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                slotAnalysis.currentEnchants.forEach { info ->
                    Text(
                        "${info.name} ${romanNumeral(info.level)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Emerald,
                        modifier = Modifier
                            .clickable { onBrowseEnchant(info.id) }
                            .padding(vertical = 1.dp),
                    )
                }
            }

            // Upgradeable enchants — orange
            if (slotAnalysis.upgradeableEnchants.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                slotAnalysis.upgradeableEnchants.forEach { info ->
                    Text(
                        "${info.name} ${romanNumeral(info.level)} \u2192 ${romanNumeral(info.maxLevel)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = UpgradeOrange,
                        modifier = Modifier
                            .clickable { onBrowseEnchant(info.id) }
                            .padding(vertical = 1.dp),
                    )
                }
            }

            // Missing enchants — grouped recommendations
            if (slotAnalysis.missingEnchants.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                EnchantRecommendations(
                    recommendations = slotAnalysis.missingEnchants,
                    onBrowseEnchant = onBrowseEnchant,
                )
            }

            // Tier upgrade
            if (slotAnalysis.tierUpgrade != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onBrowseItem(slotAnalysis.tierUpgrade.id) },
                ) {
                    SpyglassIconImage(
                        PixelIcons.Anvil,
                        contentDescription = null,
                        tint = PotionBlue,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Upgrade to ${slotAnalysis.tierUpgrade.name} \u2192",
                        style = MaterialTheme.typography.bodySmall,
                        color = PotionBlue,
                    )
                }
            }
        } else {
            // ── Empty slot ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    slotLabel(slotAnalysis.slotType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Empty",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (slotAnalysis.tierUpgrade != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onBrowseItem(slotAnalysis.tierUpgrade.id) },
                ) {
                    val tex = ItemTextures.get(slotAnalysis.tierUpgrade.id)
                    if (tex != null) {
                        SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        "Suggested: ${slotAnalysis.tierUpgrade.name} \u2192",
                        style = MaterialTheme.typography.bodySmall,
                        color = PotionBlue,
                    )
                }
            }

            // Enchant recommendations for the suggested item
            if (slotAnalysis.missingEnchants.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                EnchantRecommendations(
                    recommendations = slotAnalysis.missingEnchants,
                    onBrowseEnchant = onBrowseEnchant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnchantRecommendations(
    recommendations: List<EnchantRecommendation>,
    onBrowseEnchant: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        recommendations.forEach { rec ->
            val label = rec.enchants.joinToString(" / ") { e ->
                if (e.maxLevel > 1) "${e.name} ${romanNumeral(e.maxLevel)}" else e.name
            }
            Text(
                "+ $label",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(4.dp),
                    )
                    .clickable { onBrowseEnchant(rec.enchants.first().id) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
