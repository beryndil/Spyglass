package dev.spyglass.android.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

private val BROWSE_TAB_NAMES = listOf(
    "Blocks", "Items", "Recipes", "Mobs", "Trades",
    "Biomes", "Structures", "Enchants", "Potions",
)

private val TOOL_TAB_NAMES = listOf(
    "Todo", "Shopping", "Enchanting", "Fill", "Shapes", "Maze", "Storage", "Smelt", "Nether", "Reference", "Game Clock",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onCalcTab: (Int) -> Unit = {},
    vm: SettingsViewModel = viewModel(),
) {
    val defaultBrowseTab    by vm.defaultBrowseTab.collectAsState()
    val defaultToolTab      by vm.defaultToolTab.collectAsState()
    val showTipOfDay        by vm.showTipOfDay.collectAsState()
    val showFavoritesOnHome by vm.showFavoritesOnHome.collectAsState()
    val playerUsername      by vm.playerUsername.collectAsState()
    val playerUuid          by vm.playerUuid.collectAsState()
    val gameClockEnabled    by vm.gameClockEnabled.collectAsState()
    val allFavorites        by vm.allFavorites.collectAsState()
    val backgroundTheme     by vm.backgroundTheme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionHeader("Settings")

        // ── Theme ───────────────────────────────────────────────────────
        SectionHeader("Theme")
        ResultCard {
            Text(
                "Background",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ThemeOrder.forEach { key ->
                    val info = ThemeInfoMap[key] ?: return@forEach
                    val isSelected = backgroundTheme == key
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(info.background, CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = borderColor,
                                shape = CircleShape,
                            )
                            .clickable { vm.setBackgroundTheme(key) },
                    )
                }
            }
            Text(
                ThemeInfoMap[backgroundTheme]?.label ?: "Obsidian",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        // ── Player Name ───────────────────────────────────────────────
        SectionHeader("Player Name")
        ResultCard {
            if (playerUsername.isNotBlank()) {
                StatRow("Username", playerUsername)
                if (playerUuid.isNotBlank()) {
                    StatRow("UUID", playerUuid)
                }
                TextButton(
                    onClick = vm::clearPlayerUsername,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Clear Username", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    "No username set. You\u2019ll be asked on next launch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // ── Default Browse Tab ──────────────────────────────────────────
        SectionHeader("Default Browse Tab")
        ResultCard {
            Text(
                "Which tab opens first when you tap Browse",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BROWSE_TAB_NAMES.forEachIndexed { i, name ->
                    FilterChip(
                        selected = defaultBrowseTab == i,
                        onClick = { vm.setDefaultBrowseTab(i) },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        // ── Default Tool Tab ────────────────────────────────────────────
        SectionHeader("Default Tool Tab")
        ResultCard {
            Text(
                "Which tab opens first when you tap Tools",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TOOL_TAB_NAMES.forEachIndexed { i, name ->
                    FilterChip(
                        selected = defaultToolTab == i,
                        onClick = { vm.setDefaultToolTab(i) },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        // ── Toggles ─────────────────────────────────────────────────────
        SectionHeader("Display")
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Tip of the Day", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("Daily Minecraft tip on the Home screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = showTipOfDay,
                    onCheckedChange = vm::setShowTipOfDay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
            SpyglassDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Favorites on Home", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("Display your favorited items on the Home page", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = showFavoritesOnHome,
                    onCheckedChange = vm::setShowFavoritesOnHome,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        }

        // ── Game Clock ─────────────────────────────────────────────────────
        SectionHeader("Game Clock")
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Game Clock", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("Show mini clock in the top bar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = gameClockEnabled,
                    onCheckedChange = vm::setGameClockEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
            SpyglassDivider()
            Text(
                "Configure Game Clock \u2192",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onCalcTab(10) },
            )
        }

        // ── Favorites Management ────────────────────────────────────────
        SectionHeader("Favorites")
        ResultCard {
            if (allFavorites.isEmpty()) {
                Text(
                    "No favorites yet. Star items in the Browse tabs to add them here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                Text(
                    "${allFavorites.size} favorite(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                allFavorites.forEach { fav ->
                    Text(
                        "\u2605  ${fav.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SpyglassDivider()
                TextButton(onClick = vm::clearAllFavorites) {
                    Text("Clear All Favorites", color = Red400)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
