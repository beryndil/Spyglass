package dev.spyglass.android.changelog

import android.app.Application
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

private data class VersionData(
    val version: String,
    val entityId: String,
    val date: String,
    val highlights: List<String>,
)

private val VERSIONS = listOf(
    VersionData(
        version = "v1.0-alpha.7",
        entityId = "a7",
        date = "March 2026",
        highlights = listOf(
            "Modular architecture — Connect, Browse, and Tools are now independent modules that can be toggled on/off in Settings",
            "Cold start optimization — MobileAds, Firebase, and DataStore reads moved off main thread for faster launch",
            "Rebranded to Spyglass Connect throughout the app",
            "All links updated to hardknocks.university",
            "Developer page linked from About screen",
        ),
    ),
    VersionData(
        version = "v1.0-alpha.6",
        entityId = "a6",
        date = "March 2026",
        highlights = listOf(
            "Spyglass Connect: Player Statistics — view lifetime stats pulled from your Minecraft save (blocks mined, mobs killed, distance walked, play time, and more)",
            "Spyglass Connect: Advancements Roadmap — interactive tree showing completed, available, and locked advancements across all 5 tabs",
            "Statistics auto-format distance (cm to blocks/km), time (ticks to hours/minutes), and large numbers with commas",
            "Advancements merge bundled metadata with live save data — filter by tab, see progress bar, and track what's next",
            "Offline caching for both Statistics and Advancements — viewable even when disconnected from PC",
        ),
    ),
    VersionData(
        version = "v1.0-alpha.5",
        entityId = "a5",
        date = "March 2026",
        highlights = listOf(
            "News feed on the home screen — synced from Spyglass-Data, supports Markdown and images",
            "Updatable textures — bundled icons can be updated via sync without rebuilding the APK",
            "Versioning now includes hour and minute (YYMMDDHHMM format)",
            "Images version displayed on the About screen",
            "Texture lookup uses downloaded overrides with bundled drawable fallback",
        ),
    ),
    VersionData(
        version = "v1.0-alpha.4",
        entityId = "a4",
        date = "March 2026",
        highlights = listOf(
            "Remote data sync — app checks GitHub for data updates without needing an app update",
            "Data version displayed in About screen for easy verification",
            "Removed player username/skin feature for a cleaner home screen",
        ),
    ),
    VersionData(
        version = "v1.0-alpha.3",
        entityId = "a3",
        date = "February 2026",
        highlights = listOf(
            "Major data accuracy pass across all 11 categories",
            "1,167 blocks with corrected properties and missing entries added",
            "550+ entries added or corrected across mobs, biomes, items, trades, and more",
            "5 new shapes: Wall, Arch, Ellipsoid, Arc Wall, Spiral",
            "Smelting XP values for all 78 smelting recipes",
            "Block glance text, ore Y-levels, and sort/filter UI for browse tabs",
            "Linkified entity descriptions with cross-tab navigation",
            "Search moved to top of home page",
            "Reference tab added to Browse",
            "Game clock: editable day counter and color fixes",
            "Baseline profiles and Trace instrumentation for faster startup",
            "ANR fix: database init moved off main thread",
            "Startup performance: HomeScreen converted to LazyColumn",
        ),
    ),
    VersionData(
        version = "v1.0-alpha.2",
        entityId = "a2",
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
        entityId = "a1",
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
    val app = LocalContext.current.applicationContext as Application
    val repo by produceState<GameDataRepository?>(null) {
        value = withContext(Dispatchers.IO) {
            GameDataRepository.get(app)
        }
    }
    val txMap by remember(repo) {
        repo?.let { translationMapFlow(app.dataStore, it, "changelog") }
            ?: flowOf(emptyMap())
    }.collectAsState(initial = emptyMap())

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
            val tx = txMap[entry.entityId]
            VersionEntry(
                version = entry.version,
                date = tx?.get("date") ?: entry.date,
                highlights = entry.highlights.mapIndexed { i, fallback ->
                    tx?.get("h${i + 1}") ?: fallback
                },
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
