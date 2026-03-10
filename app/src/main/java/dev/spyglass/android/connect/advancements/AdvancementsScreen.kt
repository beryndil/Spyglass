package dev.spyglass.android.connect.advancements

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.connect.AdvancementStatus
import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.OfflineIndicator
import dev.spyglass.android.connect.PlayerAdvancementsPayload
import dev.spyglass.android.connect.client.ConnectionState
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/** Advancement metadata from bundled advancements.json. */
@Serializable
private data class AdvancementMeta(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String = "",
    val type: String = "task",
    val parent: String? = null,
)

/** Merged advancement with player status. */
private data class AdvancementNode(
    val meta: AdvancementMeta,
    val state: AdvState,
    val depth: Int = 0,
)

private enum class AdvState { COMPLETED, AVAILABLE, LOCKED }

private val TAB_ORDER = listOf("minecraft", "adventure", "nether", "end", "husbandry")
private val TAB_LABELS = mapOf(
    "minecraft" to "Minecraft",
    "adventure" to "Adventure",
    "nether" to "Nether",
    "end" to "The End",
    "husbandry" to "Husbandry",
)

@Composable
fun AdvancementsScreen(
    viewModel: ConnectViewModel,
    onBack: () -> Unit,
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val playerAdv by viewModel.playerAdvancements.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val isConnected = connectionState.isConnected
    val context = LocalContext.current

    DisposableEffect(Unit) {
        viewModel.setActiveScreen("advancements")
        onDispose { viewModel.setActiveScreen(null) }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            Timber.d("AdvancementsScreen: requesting advancements")
            viewModel.requestAdvancements()
            kotlinx.coroutines.delay(3000)
            if (viewModel.playerAdvancements.value == null) {
                viewModel.requestAdvancements()
            }
        }
    }

    // Load bundled advancement metadata
    val allMeta = remember { loadBundledAdvancements(context) }

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
            Text(stringResource(R.string.connect_advancements), style = MaterialTheme.typography.titleMedium)
        }

        if (!isConnected && lastUpdated != null) {
            OfflineIndicator(lastUpdated, modifier = Modifier.padding(horizontal = 16.dp))
        }

        AdvancementsContent(
            allMeta = allMeta,
            playerAdv = playerAdv,
            isOffline = !isConnected,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancementsContent(
    allMeta: List<AdvancementMeta>,
    playerAdv: PlayerAdvancementsPayload?,
    isOffline: Boolean,
) {
    if (playerAdv == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isOffline) {
                    Text(stringResource(R.string.connect_no_cached_advancements), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.connect_loading_advancements), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    // Build lookup from player data
    val playerMap = remember(playerAdv) {
        playerAdv.advancements.associateBy { it.id }
    }

    // Filter tabs
    val hapticClick = rememberHapticClick()
    var selectedTab by remember { mutableStateOf<String?>(null) }

    // Merge and build tree
    val nodes = remember(allMeta, playerMap, selectedTab) {
        val completedIds = playerMap.filter { it.value.done }.keys
        val metaById = allMeta.associateBy { it.id }

        fun advState(meta: AdvancementMeta): AdvState {
            if (meta.id in completedIds) return AdvState.COMPLETED
            val parentId = meta.parent
            if (parentId == null || parentId in completedIds) return AdvState.AVAILABLE
            return AdvState.LOCKED
        }

        // Filter by tab if selected
        val filtered = if (selectedTab != null) {
            allMeta.filter { it.category == selectedTab }
        } else {
            allMeta
        }

        // Build tree ordered by parent chain depth
        fun depth(meta: AdvancementMeta, visited: Set<String> = emptySet()): Int {
            val parentId = meta.parent ?: return 0
            if (parentId in visited) return 0
            val parentMeta = metaById[parentId] ?: return 0
            return 1 + depth(parentMeta, visited + meta.id)
        }

        filtered.map { meta ->
            AdvancementNode(
                meta = meta,
                state = advState(meta),
                depth = depth(meta),
            )
        }.sortedWith(compareBy({ TAB_ORDER.indexOf(it.meta.category).let { i -> if (i < 0) 99 else i } }, { it.depth }, { it.meta.name }))
    }

    // Count stats
    val completedCount = nodes.count { it.state == AdvState.COMPLETED }
    val totalCount = nodes.size

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // Progress summary
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.connect_completed_count, completedCount, totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        // Tab filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedTab == null,
                onClick = { hapticClick(); selectedTab = null },
                label = { Text(stringResource(R.string.all)) },
            )
            TAB_ORDER.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { hapticClick(); selectedTab = if (selectedTab == tab) null else tab },
                    label = { Text(TAB_LABELS[tab] ?: tab) },
                )
            }
        }

        // Group by category
        var lastCategory = ""
        nodes.forEach { node ->
            if (node.meta.category != lastCategory) {
                lastCategory = node.meta.category
                if (selectedTab == null) {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(title = TAB_LABELS[node.meta.category] ?: node.meta.category)
                }
            }
            AdvancementRow(node)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AdvancementRow(node: AdvancementNode) {
    val alpha = when (node.state) {
        AdvState.COMPLETED -> 0.5f
        AdvState.AVAILABLE -> 1.0f
        AdvState.LOCKED -> 0.35f
    }

    ResultCard(
        modifier = Modifier.padding(start = (node.depth * 12).dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Type badge
            val badgeColor = when (node.meta.type) {
                "challenge" -> EnderPurple.copy(alpha = alpha)
                "goal" -> Emerald.copy(alpha = alpha)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            }
            val badgeLabel = when (node.meta.type) {
                "challenge" -> "C"
                "goal" -> "G"
                else -> "T"
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(badgeColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(Modifier.width(10.dp))

            // Name + description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    node.meta.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (node.meta.description.isNotBlank()) {
                    Text(
                        node.meta.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Status indicator
            when (node.state) {
                AdvState.COMPLETED -> {
                    Text(
                        "\u2713",
                        style = MaterialTheme.typography.titleMedium,
                        color = Emerald.copy(alpha = 0.7f),
                    )
                }
                AdvState.AVAILABLE -> {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
                    )
                }
                AdvState.LOCKED -> {}
            }
        }
    }
}

private val advJson = Json { ignoreUnknownKeys = true }

private fun loadBundledAdvancements(context: Context): List<AdvancementMeta> {
    return try {
        val text = context.assets.open("minecraft/advancements.json").bufferedReader().readText()
        advJson.decodeFromString<List<AdvancementMeta>>(text)
    } catch (e: Exception) {
        Timber.w(e, "Failed to load bundled advancements")
        emptyList()
    }
}
