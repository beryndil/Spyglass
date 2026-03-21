package dev.spyglass.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

// ── Quick link data ─────────────────────────────────────────────────────────

internal data class QuickLink(
    val icon: SpyglassIcon,
    val label: String,
    val iconTint: Color = Color.Unspecified,
)

// Pair: QuickLink to browse-tab index
@Composable
internal fun browseLinks() = listOf(
    // ── Core Content ──
    QuickLink(PixelIcons.Blocks,    stringResource(R.string.home_link_blocks),       MaterialTheme.colorScheme.onSurfaceVariant) to 0,
    QuickLink(PixelIcons.Item,      stringResource(R.string.home_link_items),        MaterialTheme.colorScheme.primary)           to 1,
    QuickLink(PixelIcons.Crafting,  stringResource(R.string.home_link_recipes),      MaterialTheme.colorScheme.primary)           to 2,
    // ── World & Entities ──
    QuickLink(PixelIcons.Mob,       stringResource(R.string.home_link_mobs),         NetherRed)                                   to 3,
    QuickLink(PixelIcons.Biome,     stringResource(R.string.home_link_biomes),       Emerald)                                     to 5,
    QuickLink(PixelIcons.Structure, stringResource(R.string.home_link_structures),   MaterialTheme.colorScheme.primary)           to 6,
    // ── Game Mechanics ──
    QuickLink(PixelIcons.Trade,     stringResource(R.string.home_link_trades),       Emerald)                                     to 4,
    QuickLink(PixelIcons.Enchant,   stringResource(R.string.home_link_enchants),     EnderPurple)                                 to 7,
    QuickLink(PixelIcons.Potion,    stringResource(R.string.home_link_potions),      PotionBlue)                                  to 8,
    // ── Progress & Info ──
    QuickLink(PixelIcons.Command,   stringResource(R.string.home_link_commands),     PotionBlue)                                  to 9,
    QuickLink(PixelIcons.Bookmark,  stringResource(R.string.home_link_reference),    MaterialTheme.colorScheme.primary)           to 10,
    QuickLink(PixelIcons.Clock,     stringResource(R.string.home_link_versions),     MaterialTheme.colorScheme.secondary)         to 11,
)

// Pair: QuickLink to calculator-tab index (allows visual reordering independent of tab order)
internal data class CalcLink(
    val link: QuickLink,
    val tabIndex: Int,
    val experimental: Boolean = false,
)

@Composable
internal fun allCalcLinks() = listOf(
    // ── Planning & Organization ──
    CalcLink(QuickLink(PixelIcons.Todo,      stringResource(R.string.home_link_todo_list)),              0),
    CalcLink(QuickLink(PixelIcons.Storage,   stringResource(R.string.home_link_shopping_lists)),         1),
    CalcLink(QuickLink(PixelIcons.Bookmark,  stringResource(R.string.home_link_notes)),                  2),
    CalcLink(QuickLink(PixelIcons.Waypoints, stringResource(R.string.home_link_waypoints),    Emerald), 3),
    CalcLink(QuickLink(PixelIcons.Advancement, stringResource(R.string.home_link_advancements), Emerald), 4),
    // ── Building & Design ──
    CalcLink(QuickLink(PixelIcons.Fill,      stringResource(R.string.home_link_block_fill)),             6),
    CalcLink(QuickLink(PixelIcons.Shapes,    stringResource(R.string.home_link_shapes)),                 7),
    CalcLink(QuickLink(PixelIcons.Maze,      stringResource(R.string.home_link_maze_maker)),             8),
    CalcLink(QuickLink(PixelIcons.Storage,   stringResource(R.string.home_link_storage)),                9),
    // ── Crafting & Resources ──
    CalcLink(QuickLink(PixelIcons.Anvil,     stringResource(R.string.home_link_enchanting)),             5),
    CalcLink(QuickLink(PixelIcons.Smelt,     stringResource(R.string.home_link_smelting)),               10),
    // ── Reference ──
    CalcLink(QuickLink(PixelIcons.Enchant,   stringResource(R.string.home_link_librarian_guide)),       15, experimental = true),
    CalcLink(QuickLink(PixelIcons.Torch,     stringResource(R.string.home_link_light_spacing)),          13),
    // ── World & Navigation ──
    CalcLink(QuickLink(PixelIcons.Nether,    stringResource(R.string.home_link_nether_portal)),          11),
    CalcLink(QuickLink(PixelIcons.Clock,     stringResource(R.string.home_link_game_clock)),             12),
)

// ── Quick link grid — 2 columns ─────────────────────────────────────────────

@Composable
internal fun QuickLinkGrid(links: List<QuickLink>, onTap: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Left column: even indices (0, 2, 4, ...)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            links.forEachIndexed { i, link ->
                if (i % 2 == 0) QuickLinkCard(link) { onTap(i) }
            }
        }
        // Right column: odd indices (1, 3, 5, ...)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            links.forEachIndexed { i, link ->
                if (i % 2 != 0) QuickLinkCard(link) { onTap(i) }
            }
        }
    }
}

@Composable
private fun QuickLinkCard(link: QuickLink, onClick: () -> Unit) {
    val hapticClick = rememberHapticClick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalSurfaceCard.current, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { hapticClick(); onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpyglassIconImage(link.icon, contentDescription = null, tint = if (link.iconTint == Color.Unspecified) MaterialTheme.colorScheme.primary else link.iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(link.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}
