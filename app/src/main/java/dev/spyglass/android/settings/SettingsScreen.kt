package dev.spyglass.android.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

private val BROWSE_TAB_NAMES = listOf(
    "Blocks", "Items", "Recipes", "Mobs", "Trades",
    "Biomes", "Structures", "Enchants", "Potions",
)

private val TOOL_TAB_NAMES = listOf(
    "Todo", "Shopping", "Enchanting", "Fill", "Shapes", "Storage", "Smelt", "Nether", "Reference",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    vm: SettingsViewModel = viewModel(),
) {
    val defaultBrowseTab    by vm.defaultBrowseTab.collectAsState()
    val defaultToolTab      by vm.defaultToolTab.collectAsState()
    val showTipOfDay        by vm.showTipOfDay.collectAsState()
    val showFavoritesOnHome by vm.showFavoritesOnHome.collectAsState()
    val playerUsername      by vm.playerUsername.collectAsState()
    val allFavorites        by vm.allFavorites.collectAsState()

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
                tint = Stone300,
            )
        }
        SectionHeader("Settings")

        // ── Theme ───────────────────────────────────────────────────────
        SectionHeader("Theme")
        ResultCard {
            StatRow("Color Theme", "Dark")
            Text(
                "Dark mode only for now. More themes coming soon.",
                style = MaterialTheme.typography.bodySmall,
                color = Stone500,
            )
        }

        // ── Player Name ───────────────────────────────────────────────
        SectionHeader("Player Name")
        ResultCard {
            if (playerUsername.isNotBlank()) {
                StatRow("Username", playerUsername)
                TextButton(
                    onClick = vm::clearPlayerUsername,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Clear Username", color = Stone500, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    "No username set. You\u2019ll be asked on next launch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone500,
                )
            }
        }

        // ── Default Browse Tab ──────────────────────────────────────────
        SectionHeader("Default Browse Tab")
        ResultCard {
            Text(
                "Which tab opens first when you tap Browse",
                style = MaterialTheme.typography.bodySmall,
                color = Stone500,
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
                color = Stone500,
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
                    Text("Show Tip of the Day", style = MaterialTheme.typography.bodyLarge, color = Stone100)
                    Text("Daily Minecraft tip on the Home screen", style = MaterialTheme.typography.bodySmall, color = Stone500)
                }
                Switch(
                    checked = showTipOfDay,
                    onCheckedChange = vm::setShowTipOfDay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Gold,
                        checkedTrackColor = GoldDim,
                        uncheckedThumbColor = Stone500,
                        uncheckedTrackColor = Stone700,
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
                    Text("Show Favorites on Home", style = MaterialTheme.typography.bodyLarge, color = Stone100)
                    Text("Display your favorited items on the Home page", style = MaterialTheme.typography.bodySmall, color = Stone500)
                }
                Switch(
                    checked = showFavoritesOnHome,
                    onCheckedChange = vm::setShowFavoritesOnHome,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Gold,
                        checkedTrackColor = GoldDim,
                        uncheckedThumbColor = Stone500,
                        uncheckedTrackColor = Stone700,
                    ),
                )
            }
        }

        // ── Favorites Management ────────────────────────────────────────
        SectionHeader("Favorites")
        ResultCard {
            if (allFavorites.isEmpty()) {
                Text(
                    "No favorites yet. Star items in the Browse tabs to add them here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone500,
                )
            } else {
                Text(
                    "${allFavorites.size} favorite(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone300,
                )
                allFavorites.forEach { fav ->
                    Text(
                        "\u2605  ${fav.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Stone300,
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
