package dev.spyglass.android.changelog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

private data class VersionData(val version: String, val date: String, val highlights: List<String>)

private val VERSIONS = listOf(
    VersionData(
        version = "v1.0-alpha.2",
        date = "February 2026",
        highlights = listOf(
            "15 background color themes — dark, mid-tone, and light options",
            "Theme picker in Settings with tappable color swatches",
            "Improved contrast for gold accent and muted text on light themes",
            "Expanded item data with armor stats, food stats, and attack info",
        ),
    ),
    VersionData(
        version = "v1.0-alpha.1",
        date = "February 2026",
        highlights = listOf(
            "930+ blocks and 360 items with full detail cards",
            "9 Browse tabs: Blocks, Items, Recipes, Mobs, Trades, Biomes, Structures, Enchantments, Potions",
            "7 Tools: Block Fill, Smelting, Storage, Nether Portal, Shapes, Enchanting, Quick Reference",
            "Enchant optimizer — cheapest XP order for combining books on the anvil",
            "Cross-tab links — tap any item, mob, biome, or structure to jump to its detail page",
            "Global search across all data types",
            "Favorites system — star any item and pin it to the top of Browse tabs",
            "Settings — default tabs, tip of the day toggle, favorites on home page",
            "Changelog and Feedback screens",
        ),
    ),
)

@Composable
fun ChangelogScreen(onBack: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "back") {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionHeader(stringResource(R.string.changelog_title))
        }

        items(VERSIONS, key = { it.version }) { entry ->
            VersionEntry(
                version = entry.version,
                date = entry.date,
                highlights = entry.highlights,
            )
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
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
