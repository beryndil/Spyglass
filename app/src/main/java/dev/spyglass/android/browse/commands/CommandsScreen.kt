package dev.spyglass.android.browse.commands

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.CommandEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CommandsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val commands: StateFlow<List<CommandEntity>> = combine(_query.debounce(200), _category) { q, cat ->
        if (q.isBlank() && cat == "all") repo.searchCommands("")
        else if (cat != "all") repo.commandsByCategory(cat)
        else repo.searchCommands(q)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteCommands: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("command")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }

    fun toggleExpanded(id: String) {
        val current = _expandedIds.value
        _expandedIds.value = if (id in current) current - id else current + id
    }

    fun expandCommand(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "command", displayName = displayName))
        }
    }
}

private fun categoryLabel(cat: String): String = when (cat) {
    "chat" -> "Chat"
    "player" -> "Player"
    "entity" -> "Entity"
    "world" -> "World"
    "server" -> "Server"
    "operator" -> "Operator"
    "debug" -> "Debug"
    else -> cat.replaceFirstChar { it.uppercase() }
}

@Composable
private fun categoryColor(cat: String) = when (cat) {
    "chat" -> Emerald
    "player" -> PotionBlue
    "entity" -> NetherRed
    "world" -> Emerald
    "server" -> MaterialTheme.colorScheme.primary
    "operator" -> EnderPurple
    "debug" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.secondary
}

private fun permissionLabel(level: Int): String = when (level) {
    0 -> "All Players"
    2 -> "Operator (OP)"
    3 -> "Admin"
    4 -> "Server Owner"
    else -> "Level $level"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandsScreen(
    targetCommandId: String? = null,
    onItemTap: (String) -> Unit = {},
    onMobTap: (String) -> Unit = {},
    onBiomeTap: (String) -> Unit = {},
    onStructureTap: (String) -> Unit = {},
    onEnchantTap: (String) -> Unit = {},
    entityLinkIndex: EntityLinkIndex = EntityLinkIndex(emptyList()),
    vm: CommandsViewModel = viewModel(),
) {
    val query by vm.query.collectAsState()
    val category by vm.category.collectAsState()
    val commands by vm.commands.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val favoriteCommands by vm.favoriteCommands.collectAsState()

    LaunchedEffect(targetCommandId, commands) {
        if (targetCommandId != null && commands.isNotEmpty()) {
            vm.expandCommand(targetCommandId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search commands\u2026", color = MaterialTheme.colorScheme.secondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.secondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("all", "chat", "player", "entity", "world", "server", "operator", "debug").forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { vm.setCategory(cat) },
                    label = { Text(if (cat == "all") "All" else categoryLabel(cat), style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Command,
                    title = "Commands",
                    description = "All Minecraft Java 1.21.4 commands with syntax, descriptions, and permission levels.",
                    stat = "${commands.size} commands",
                )
            }
            if (favoriteCommands.isNotEmpty()) {
                item(key = "fav_header") {
                    Text("Favorites", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(favoriteCommands, key = { "fav_${it.id}" }) { fav ->
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        supportingMaxLines = 1,
                        leadingIcon = PixelIcons.Command,
                        trailing = {
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
            items(commands, key = { it.id }) { cmd ->
                val isExpanded = cmd.id in expandedIds
                Column {
                    BrowseListItem(
                        headline = cmd.name,
                        supporting = cmd.description.take(60),
                        supportingMaxLines = 1,
                        leadingIcon = PixelIcons.Command,
                        modifier = Modifier.clickable { vm.toggleExpanded(cmd.id) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(label = categoryLabel(cmd.category), color = categoryColor(cmd.category))
                                Spacer(Modifier.width(4.dp))
                                val isFav = cmd.id in favoriteIds
                                IconButton(onClick = { vm.toggleFavorite(cmd.id, cmd.name) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "Favorite",
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                    )
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        CommandDetailCard(cmd, entityLinkIndex, onItemTap, onMobTap, onBiomeTap, onStructureTap, onEnchantTap)
                    }
                }
            }
            if (commands.isEmpty()) item {
                EmptyState(
                    icon = PixelIcons.SearchOff,
                    title = "No commands found",
                    subtitle = "Try a different search or filter",
                )
            }
        }
    }
}

@Composable
private fun CommandDetailCard(cmd: CommandEntity, entityLinkIndex: EntityLinkIndex, onItemTap: (String) -> Unit, onMobTap: (String) -> Unit, onBiomeTap: (String) -> Unit, onStructureTap: (String) -> Unit, onEnchantTap: (String) -> Unit) {
    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            "SYNTAX",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Text(
                cmd.syntax,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Emerald,
            )
        }
        SpyglassDivider()
        LinkedDescription(
            description = cmd.description,
            linkIndex = entityLinkIndex,
            selfId = cmd.id,
            onItemTap = onItemTap,
            onMobTap = onMobTap,
            onBiomeTap = onBiomeTap,
            onStructureTap = onStructureTap,
            onEnchantTap = onEnchantTap,
        )
        SpyglassDivider()
        StatRow("Category", categoryLabel(cmd.category))
        StatRow("Permission", permissionLabel(cmd.permissionLevel))
    }
}
