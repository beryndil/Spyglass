package dev.spyglass.android.changelog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.spyglass.android.core.ui.*

@Composable
fun ChangelogScreen(onBack: () -> Unit = {}) {
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
        SectionHeader("Changelog")

        VersionEntry(
            version = "v1.0-alpha.2",
            date = "February 2026",
            highlights = listOf(
                "15 background color themes — dark, mid-tone, and light options",
                "Theme picker in Settings with tappable color swatches",
                "Improved contrast for gold accent and muted text on light themes",
                "Expanded item data with armor stats, food stats, and attack info",
            ),
        )

        VersionEntry(
            version = "v1.0-alpha.1",
            date = "February 2026",
            highlights = listOf(
                "598 blocks and 352 items with full detail cards",
                "9 Browse tabs: Blocks, Items, Recipes, Mobs, Trades, Biomes, Structures, Enchantments, Potions",
                "7 Tools: Block Fill, Smelting, Storage, Nether Portal, Shapes, Enchanting, Quick Reference",
                "Enchant optimizer — cheapest XP order for combining books on the anvil",
                "Cross-tab links — tap any item, mob, biome, or structure to jump to its detail page",
                "Global search across all data types",
                "Favorites system — star any item and pin it to the top of Browse tabs",
                "Settings — default tabs, tip of the day toggle, favorites on home page",
                "Changelog and Feedback screens",
            ),
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VersionEntry(
    version: String,
    date: String,
    highlights: List<String>,
) {
    ResultCard {
        Text(version, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        highlights.forEach { item ->
            Text(
                text = "\u2022  $item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
