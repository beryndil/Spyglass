package dev.spyglass.android.core.module

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.Emerald
import dev.spyglass.android.core.ui.ItemTextures
import dev.spyglass.android.core.ui.LocalSurfaceCard
import dev.spyglass.android.core.ui.PixelIcons
import dev.spyglass.android.core.ui.ResultCard
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.SpyglassDivider
import dev.spyglass.android.core.ui.SpyglassIcon
import dev.spyglass.android.core.ui.SpyglassIconImage
import dev.spyglass.android.data.db.entities.TodoEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Tools module — owns all 19 calculator screens, todo, shopping lists,
 * notes, and waypoints.
 */
object ToolsModule : SpyglassModule {

    override val id = "tools"
    override val name = "Tools & Calculators"
    override val icon: SpyglassIcon = PixelIcons.Anvil
    override val priority = 20
    override val canDisable = true

    // ── Home sections ───────────────────────────────────────────────────────

    override fun homeSections(): List<HomeSection> = listOf(
        HomeSection("todo", 20) { scope -> TodoPreviewSection(scope) },
        HomeSection("tools", 40) { scope -> QuickToolsSection(scope) },
    )

    // ── Settings sections ───────────────────────────────────────────────────

    override fun settingsSections(): List<SettingsSection> = listOf(
        SettingsSection("tool_tab", "Default Tool Tab", 25) { ToolTabContent() },
        SettingsSection("clock", "Game Clock", 30) { scope -> GameClockContent(scope) },
    )

    // ── Nav routes ──────────────────────────────────────────────────────────

    override fun navRoutes(): List<ModuleRoute> = emptyList()

    override fun bottomNavItems(): List<BottomNavItem> = listOf(
        BottomNavItem("calculators", R.string.nav_tools, PixelIcons.Anvil, 20),
    )

    override fun searchProvider(): SearchProvider? = null

    // ── Home composables ────────────────────────────────────────────────────

    @Composable
    private fun TodoPreviewSection(scope: HomeSectionScope) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val repo by androidx.compose.runtime.produceState<GameDataRepository?>(null) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { GameDataRepository.get(context) }
        }
        val todoPreview by androidx.compose.runtime.produceState(emptyList<TodoEntity>(), repo) {
            repo?.incompleteTodosPreview(3)?.collect { value = it } ?: return@produceState
        }
        val todoCount by androidx.compose.runtime.produceState(0, repo) {
            repo?.incompleteTodoCount()?.collect { value = it } ?: return@produceState
        }

        SectionHeader(stringResource(R.string.home_todo), icon = PixelIcons.Todo)
        Spacer(Modifier.height(8.dp))
        if (todoCount > 0) {
            ResultCard {
                todoPreview.forEach { todo ->
                    TodoRow(
                        todo = todo,
                        onToggle = {
                            repo?.let { r -> coroutineScope.launch { r.toggleTodoCompleted(todo.id, !todo.completed) } }
                        },
                    )
                }
                if (todoCount > 3) {
                    SpyglassDivider()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.navigateToCalcTab(0) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (todoCount > 3) stringResource(R.string.home_todo_view_all, todoCount) else stringResource(R.string.home_todo_edit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            ResultCard(
                modifier = Modifier.clickable { scope.navigateToCalcTab(0) },
            ) {
                Text(
                    stringResource(R.string.home_todo_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.home_todo_open),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    @Composable
    private fun QuickToolsSection(scope: HomeSectionScope) {
        val context = LocalContext.current
        val showExperimental by remember {
            context.dataStore.data.map { it[PreferenceKeys.SHOW_EXPERIMENTAL] ?: false }
        }.collectAsStateWithLifecycle(initialValue = true)
        val calcLinks = remember(showExperimental) {
            if (showExperimental) ALL_CALC_LINKS else ALL_CALC_LINKS.filter { !it.experimental }
        }
        SectionHeader(stringResource(R.string.home_tools), icon = PixelIcons.Anvil)
        Spacer(Modifier.height(8.dp))
        QuickLinkGrid(calcLinks.map { it.link }) { index -> scope.navigateToCalcTab(calcLinks[index].tabIndex) }
    }

    // ── Settings composables ────────────────────────────────────────────────

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ToolTabContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val defaultToolTab by remember {
            context.dataStore.data.map { it[PreferenceKeys.DEFAULT_TOOL_TAB] ?: 0 }
        }.collectAsStateWithLifecycle(initialValue = 0)

        SectionHeader(stringResource(R.string.settings_default_tool_tab))
        ResultCard {
            Text(
                stringResource(R.string.settings_tool_tab_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                toolTabNames().forEachIndexed { i, name ->
                    FilterChip(
                        selected = defaultToolTab == i,
                        onClick = { scope.launch { context.dataStore.edit { it[PreferenceKeys.DEFAULT_TOOL_TAB] = i } } },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }

    @Composable
    private fun GameClockContent(scope: SettingsSectionScope) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val gameClockEnabled by remember {
            context.dataStore.data.map { it[PreferenceKeys.GAME_CLOCK_ENABLED] ?: false }
        }.collectAsStateWithLifecycle(initialValue = false)

        SectionHeader(stringResource(R.string.settings_game_clock))
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_game_clock), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.settings_game_clock_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = gameClockEnabled,
                    onCheckedChange = { coroutineScope.launch { context.dataStore.edit { it[PreferenceKeys.GAME_CLOCK_ENABLED] = !gameClockEnabled } } },
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
                stringResource(R.string.settings_configure_clock),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { scope.navigateToCalcTab(9) },
            )
        }
    }

    // ── Shared helpers ──────────────────────────────────────────────────────

    private data class QuickLink(
        val icon: SpyglassIcon,
        val label: String,
        val iconTint: Color = Color.Unspecified,
    )

    private data class CalcLink(val link: QuickLink, val tabIndex: Int, val experimental: Boolean = false)

    private val ALL_CALC_LINKS = listOf(
        CalcLink(QuickLink(PixelIcons.Todo, "Todo List"), 0),
        CalcLink(QuickLink(PixelIcons.Storage, "Shopping Lists"), 1),
        CalcLink(QuickLink(PixelIcons.Bookmark, "Notes"), 11),
        CalcLink(QuickLink(PixelIcons.Waypoints, "Waypoints", Emerald), 12),
        CalcLink(QuickLink(PixelIcons.Fill, "Block Fill"), 3),
        CalcLink(QuickLink(PixelIcons.Shapes, "Shapes"), 4),
        CalcLink(QuickLink(PixelIcons.Maze, "Maze Maker"), 5),
        CalcLink(QuickLink(PixelIcons.Storage, "Storage"), 6),
        CalcLink(QuickLink(PixelIcons.Anvil, "Enchanting"), 2),
        CalcLink(QuickLink(PixelIcons.Smelt, "Smelting"), 7),
        CalcLink(QuickLink(PixelIcons.Enchant, "Librarian Guide"), 14, experimental = true),
        CalcLink(QuickLink(PixelIcons.Torch, "Light Spacing"), 10),
        CalcLink(QuickLink(PixelIcons.Nether, "Nether Portal"), 8),
        CalcLink(QuickLink(PixelIcons.Clock, "Game Clock"), 9),
    )

    @Composable
    private fun toolTabNames() = listOf(
        stringResource(R.string.calc_tab_todo), stringResource(R.string.calc_tab_shopping),
        stringResource(R.string.calc_tab_enchanting), stringResource(R.string.calc_tab_fill),
        stringResource(R.string.calc_tab_shapes), stringResource(R.string.calc_tab_maze),
        stringResource(R.string.calc_tab_storage), stringResource(R.string.calc_tab_smelt),
        stringResource(R.string.calc_tab_nether), stringResource(R.string.calc_tab_game_clock),
        stringResource(R.string.calc_tab_light), stringResource(R.string.calc_tab_notes),
        stringResource(R.string.calc_tab_waypoints), stringResource(R.string.calc_tab_redstone),
        stringResource(R.string.calc_tab_librarian), stringResource(R.string.calc_tab_food),
        stringResource(R.string.calc_tab_banners), stringResource(R.string.calc_tab_trims),
        stringResource(R.string.calc_tab_loot),
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

    @Composable
    private fun TodoRow(todo: TodoEntity, onToggle: () -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 4.dp),
        ) {
            Checkbox(
                checked = todo.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.secondary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            if (todo.itemId != null) {
                val tex = ItemTextures.get(todo.itemId)
                if (tex != null) {
                    SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
            }
            Text(
                todo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (todo.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
