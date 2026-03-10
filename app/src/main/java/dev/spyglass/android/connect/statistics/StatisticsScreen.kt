package dev.spyglass.android.connect.statistics

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.PlayerStatsPayload
import dev.spyglass.android.connect.StatCategory
import dev.spyglass.android.connect.client.ConnectionState
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import timber.log.Timber

// Stat categories where values are in centimeters (cm → blocks)
private val DISTANCE_STATS = setOf(
    "walk_one_cm", "sprint_one_cm", "crouch_one_cm", "swim_one_cm",
    "fall_one_cm", "climb_one_cm", "fly_one_cm", "walk_under_water_one_cm",
    "walk_on_water_one_cm", "boat_one_cm", "minecart_one_cm", "aviate_one_cm",
    "horse_one_cm", "pig_one_cm", "strider_one_cm",
)

// Stat categories where values are in ticks (20 ticks = 1 second)
private val TIME_STATS = setOf(
    "play_time", "total_world_time", "time_since_death", "time_since_rest",
    "sneak_time",
)

// Category display names
private val CATEGORY_NAMES = mapOf(
    "custom" to "General",
    "mined" to "Blocks Mined",
    "broken" to "Items Broken",
    "crafted" to "Items Crafted",
    "used" to "Items Used",
    "picked_up" to "Items Picked Up",
    "dropped" to "Items Dropped",
    "killed" to "Mobs Killed",
    "killed_by" to "Killed By",
)

@Composable
fun StatisticsScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val stats by viewModel.playerStats.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected

    DisposableEffect(Unit) {
        viewModel.setActiveScreen("statistics")
        onDispose { viewModel.setActiveScreen(null) }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            Timber.d("StatisticsScreen: requesting stats")
            viewModel.requestStats()
            kotlinx.coroutines.delay(3000)
            if (viewModel.playerStats.value == null) {
                viewModel.requestStats()
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.connect_player_statistics), style = MaterialTheme.typography.titleMedium)
        }

        if (!isConnected && lastUpdated != null) {
            OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
        }

        StatsContent(stats = stats, isOffline = !isConnected)
    }
}

@Composable
private fun StatsContent(stats: PlayerStatsPayload?, isOffline: Boolean) {
    if (stats == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isOffline) {
                    Text(stringResource(R.string.connect_no_cached_statistics), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.connect_loading_statistics), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    if (stats.categories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.connect_no_statistics), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stats.categories.forEach { category ->
            StatCategorySection(category)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCategorySection(category: StatCategory) {
    SectionHeader(title = CATEGORY_NAMES[category.category] ?: formatKey(category.category))

    ResultCard {
        category.entries.forEachIndexed { index, entry ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatKey(entry.key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatStatValue(entry.key, entry.value, category.category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Format a stat key: replace underscores with spaces, title case. */
private fun formatKey(key: String): String {
    return key.replace("_one_cm", "")
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

/** Format a stat value with appropriate units. */
private fun formatStatValue(key: String, value: Long, category: String): String {
    // Distance stats: convert cm to blocks (100cm = 1 block)
    if (key in DISTANCE_STATS || category == "custom" && key.endsWith("_one_cm")) {
        val blocks = value / 100.0
        return if (blocks >= 1000) {
            "%.1f km".format(blocks / 1000.0)
        } else {
            "%,.0f blocks".format(blocks)
        }
    }

    // Time stats: convert ticks to human-readable
    if (key in TIME_STATS) {
        return formatTicks(value)
    }

    // Damage stats: convert half-hearts to hearts
    if (key.startsWith("damage_")) {
        val hearts = value / 20.0 // Damage is in 0.05 HP units actually, but MC stores raw
        // MC damage stats are stored in tenths of half-hearts, so /10 for half-hearts
        return "%,d".format(value)
    }

    return "%,d".format(value)
}

/** Convert ticks to hours/minutes format. */
private fun formatTicks(ticks: Long): String {
    val totalSeconds = ticks / 20
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${totalSeconds}s"
    }
}
