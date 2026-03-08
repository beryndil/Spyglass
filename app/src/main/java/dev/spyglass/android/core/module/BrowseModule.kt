package dev.spyglass.android.core.module

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.BiomeTextures
import dev.spyglass.android.core.ui.BrowseListItem
import dev.spyglass.android.core.ui.Emerald
import dev.spyglass.android.core.ui.EnchantTextures
import dev.spyglass.android.core.ui.EnderPurple
import dev.spyglass.android.core.ui.ItemTextures
import dev.spyglass.android.core.ui.LocalSurfaceCard
import dev.spyglass.android.core.ui.MobTextures
import dev.spyglass.android.core.ui.NetherRed
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.PotionBlue
import dev.spyglass.android.core.ui.PotionTextures
import dev.spyglass.android.core.ui.Red400
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.SpyglassIcon
import dev.spyglass.android.core.ui.SpyglassIconImage
import dev.spyglass.android.core.ui.StructureTextures
import dev.spyglass.android.core.ui.VersionCard
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.MinecraftUpdates
import dev.spyglass.android.settings.MinecraftVersions
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Database module — owns Room database, DAOs, entities, seeder, sync,
 * repository, all Browse screens, and favorites.
 */
object BrowseModule : SpyglassModule {

    override val id = "browse"
    override val name = "Browse"
    override val icon: SpyglassIcon = PixelIcons.Browse
    override val priority = 10
    override val canDisable = true

    // ── Home sections ───────────────────────────────────────────────────────

    override fun homeSections(): List<HomeSection> = listOf(
        HomeSection("favorites", 30) { scope -> FavoritesSection(scope) },
        HomeSection("browse", 70) { scope -> BrowseGridSection(scope) },
    )

    // ── Settings sections ───────────────────────────────────────────────────

    override fun settingsSections(): List<SettingsSection> = listOf(
        SettingsSection("game_version", "Game Version", 15) { GameVersionContent() },
        SettingsSection("browse_settings", "Browse", 20) { BrowseSettingsContent() },
    )

    // ── Nav routes ──────────────────────────────────────────────────────────

    override fun navRoutes(): List<ModuleRoute> = emptyList()

    override fun bottomNavItems(): List<BottomNavItem> = listOf(
        BottomNavItem("browse", R.string.nav_browse, PixelIcons.Browse, 10),
    )

    override fun searchProvider(): SearchProvider = DatabaseSearchProvider

    override suspend fun onInit(context: Context) {
        // Database warm-up and seeding handled by SpyglassApp
    }

    // ── Home composables ────────────────────────────────────────────────────

    @Composable
    private fun FavoritesSection(scope: HomeSectionScope) {
        val context = LocalContext.current

        val showFavoritesOnHome by remember {
            context.dataStore.data.map { it[PreferenceKeys.SHOW_FAVORITES_ON_HOME] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        val repo by androidx.compose.runtime.produceState<GameDataRepository?>(null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { GameDataRepository.get(context) }
        }
        val favorites by androidx.compose.runtime.produceState(emptyList<FavoriteEntity>(), repo) {
            repo?.allFavorites()?.collect { value = it } ?: return@produceState
        }

        if (!showFavoritesOnHome || favorites.isEmpty()) return

        SectionHeader(stringResource(R.string.favorites), icon = PixelIcons.Bookmark)
        Spacer(Modifier.height(8.dp))
        favorites.forEach { fav ->
            BrowseListItem(
                headline = fav.displayName,
                supporting = fav.type,
                leadingIcon = iconForFavorite(fav.type, fav.id),
                modifier = Modifier.clickable {
                    scope.navigateToBrowseTab(browseTabForType(fav.type))
                },
            )
        }
    }

    @Composable
    private fun BrowseGridSection(scope: HomeSectionScope) {
        SectionHeader(stringResource(R.string.home_browse), icon = PixelIcons.Browse)
        Spacer(Modifier.height(8.dp))
        val links = browseLinks()
        QuickLinkGrid(links.map { it.first }) { index -> scope.navigateToBrowseTab(links[index].second) }
    }

    // ── Settings composables ────────────────────────────────────────────────

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun GameVersionContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val minecraftEdition by remember {
            context.dataStore.data.map { it[PreferenceKeys.MINECRAFT_EDITION] ?: "java" }
        }.collectAsStateWithLifecycle(initialValue = "java")

        val minecraftVersion by remember {
            context.dataStore.data.map { it[PreferenceKeys.MINECRAFT_VERSION] ?: "" }
        }.collectAsStateWithLifecycle(initialValue = "")

        val versionFilterMode by remember {
            context.dataStore.data.map { it[PreferenceKeys.VERSION_FILTER_MODE] ?: "show_all" }
        }.collectAsStateWithLifecycle(initialValue = "show_all")

        var versionExpanded by remember { mutableStateOf(false) }

        SectionHeader("Game Version")
        ResultCard {
            Text(
                "Filter content by Minecraft edition and version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            Text("Edition", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            dev.spyglass.android.core.ui.TogglePill(
                options = listOf("Java", "Bedrock"),
                selected = if (minecraftEdition == "bedrock") 1 else 0,
                onSelect = { selected -> scope.launch { context.dataStore.edit { prefs -> prefs[PreferenceKeys.MINECRAFT_EDITION] = if (selected == 1) "bedrock" else "java" } } },
            )

            val versions = if (minecraftEdition == "bedrock") MinecraftVersions.BEDROCK_VERSIONS else MinecraftVersions.JAVA_VERSIONS
            val displayVersion = minecraftVersion.ifBlank { "Latest" }
            Text("Version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Box {
                OutlinedButton(onClick = { versionExpanded = true }) {
                    Text(displayVersion, color = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(expanded = versionExpanded, onDismissRequest = { versionExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Latest", color = if (minecraftVersion.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                        onClick = { scope.launch { context.dataStore.edit { it[PreferenceKeys.MINECRAFT_VERSION] = "" } }; versionExpanded = false },
                    )
                    versions.reversed().forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v, color = if (minecraftVersion == v) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                            onClick = { scope.launch { context.dataStore.edit { it[PreferenceKeys.MINECRAFT_VERSION] = v } }; versionExpanded = false },
                        )
                    }
                }
            }

            Text("Filter Mode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val modes = listOf("show_all" to "Show All", "highlight" to "Highlight Unavailable", "hide" to "Hide Unavailable")
                modes.forEach { (key, label) ->
                    FilterChip(
                        selected = versionFilterMode == key,
                        onClick = { scope.launch { context.dataStore.edit { it[PreferenceKeys.VERSION_FILTER_MODE] = key } } },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        val selectedVersion = minecraftVersion.ifBlank { MinecraftVersions.JAVA_VERSIONS.last() }
        val updateInfo = remember(selectedVersion) { MinecraftUpdates.forVersion(selectedVersion) }
        if (updateInfo != null) {
            Spacer(Modifier.height(4.dp))
            VersionCard(
                version = updateInfo.version,
                name = updateInfo.name,
                releaseDate = updateInfo.releaseDate,
                accentColor = updateInfo.color,
                icon = updateInfo.icon,
                changelog = updateInfo.changelog,
            )
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun BrowseSettingsContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val defaultBrowseTab by remember {
            context.dataStore.data.map { it[PreferenceKeys.DEFAULT_BROWSE_TAB] ?: 0 }
        }.collectAsStateWithLifecycle(initialValue = 0)

        val repo by androidx.compose.runtime.produceState<GameDataRepository?>(null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { GameDataRepository.get(context) }
        }
        val allFavorites by androidx.compose.runtime.produceState(emptyList<FavoriteEntity>(), repo) {
            repo?.allFavorites()?.collect { value = it } ?: return@produceState
        }

        SectionHeader("Browse")
        ResultCard {
            // Default browse tab
            Text(
                stringResource(R.string.settings_browse_tab_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                browseTabNames().forEachIndexed { i, name ->
                    FilterChip(
                        selected = defaultBrowseTab == i,
                        onClick = { scope.launch { context.dataStore.edit { it[PreferenceKeys.DEFAULT_BROWSE_TAB] = i } } },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            SpyglassDivider()

            // Favorites
            if (allFavorites.isEmpty()) {
                Text(
                    stringResource(R.string.settings_no_favorites),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                Text(
                    stringResource(R.string.settings_favorites_count, allFavorites.size),
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
                TextButton(onClick = {
                    scope.launch { repo?.deleteAllFavorites() }
                }) {
                    Text(stringResource(R.string.settings_clear_all_favorites), color = Red400)
                }
            }
        }
    }

    // ── Search provider ─────────────────────────────────────────────────────

    private object DatabaseSearchProvider : SearchProvider {
        override suspend fun search(query: String): List<ModuleSearchResult> = coroutineScope {
            if (query.length < 2) return@coroutineScope emptyList()
            // This delegates to the existing SearchViewModel logic
            emptyList() // Search is handled by the existing SearchScreen composable directly
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private data class QuickLink(
        val icon: SpyglassIcon,
        val label: String,
        val iconTint: Color = Color.Unspecified,
    )

    @Composable
    private fun browseLinks() = listOf(
        QuickLink(PixelIcons.Blocks, "Blocks", MaterialTheme.colorScheme.onSurfaceVariant) to 0,
        QuickLink(PixelIcons.Item, "Items", MaterialTheme.colorScheme.primary) to 1,
        QuickLink(PixelIcons.Crafting, "Recipes", MaterialTheme.colorScheme.primary) to 2,
        QuickLink(PixelIcons.Mob, "Mobs", NetherRed) to 3,
        QuickLink(PixelIcons.Biome, "Biomes", Emerald) to 5,
        QuickLink(PixelIcons.Structure, "Structures", MaterialTheme.colorScheme.primary) to 6,
        QuickLink(PixelIcons.Trade, "Trades", Emerald) to 4,
        QuickLink(PixelIcons.Enchant, "Enchants", EnderPurple) to 7,
        QuickLink(PixelIcons.Potion, "Potions", PotionBlue) to 8,
        QuickLink(PixelIcons.Advancement, "Advancements", Emerald) to 9,
        QuickLink(PixelIcons.Command, "Commands", PotionBlue) to 10,
        QuickLink(PixelIcons.Bookmark, "Reference", MaterialTheme.colorScheme.primary) to 11,
        QuickLink(PixelIcons.Clock, "Versions", MaterialTheme.colorScheme.secondary) to 12,
    )

    @Composable
    private fun browseTabNames() = listOf(
        stringResource(R.string.browse_tab_blocks), stringResource(R.string.browse_tab_items),
        stringResource(R.string.browse_tab_recipes), stringResource(R.string.browse_tab_mobs),
        stringResource(R.string.browse_tab_trades), stringResource(R.string.browse_tab_biomes),
        stringResource(R.string.browse_tab_structures), stringResource(R.string.browse_tab_enchants),
        stringResource(R.string.browse_tab_potions), stringResource(R.string.browse_tab_advancements),
        stringResource(R.string.browse_tab_commands), stringResource(R.string.browse_tab_reference),
        stringResource(R.string.browse_tab_versions),
    )

    @Composable
    private fun QuickLinkGrid(links: List<QuickLink>, onTap: (Int) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                links.forEachIndexed { i, link -> if (i % 2 == 0) QuickLinkCard(link) { onTap(i) } }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                links.forEachIndexed { i, link -> if (i % 2 != 0) QuickLinkCard(link) { onTap(i) } }
            }
        }
    }

    @Composable
    private fun QuickLinkCard(link: QuickLink, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalSurfaceCard.current, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpyglassIconImage(link.icon, contentDescription = null, tint = if (link.iconTint == Color.Unspecified) MaterialTheme.colorScheme.primary else link.iconTint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(link.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    private fun browseTabForType(type: String): Int = when (type) {
        "block" -> 0; "item" -> 1; "recipe" -> 2; "mob" -> 3; "trade" -> 4
        "biome" -> 5; "structure" -> 6; "enchant" -> 7; "potion" -> 8
        "advancement" -> 9; "command" -> 10; else -> 0
    }

    private fun iconForFavorite(type: String, id: String): SpyglassIcon = when (type) {
        "block" -> ItemTextures.get(id) ?: PixelIcons.Blocks
        "item" -> ItemTextures.get(id) ?: PixelIcons.Item
        "recipe" -> ItemTextures.get(id) ?: PixelIcons.Crafting
        "mob" -> MobTextures.get(id) ?: PixelIcons.Mob
        "trade" -> PixelIcons.Trade
        "biome" -> BiomeTextures.get(id) ?: PixelIcons.Biome
        "structure" -> StructureTextures.get(id) ?: PixelIcons.Structure
        "enchant" -> EnchantTextures.get(id) ?: PixelIcons.Enchant
        "potion" -> PotionTextures.get(id) ?: PixelIcons.Potion
        else -> PixelIcons.Bookmark
    }
}
