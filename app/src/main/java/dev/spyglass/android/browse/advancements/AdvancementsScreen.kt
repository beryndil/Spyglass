package dev.spyglass.android.browse.advancements

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.AdvancementEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AdvancementsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val advancements: StateFlow<List<AdvancementEntity>> = combine(_query.debounce(200), _category) { q, cat ->
        if (q.isBlank() && cat == "all") repo.searchAdvancements("")
        else if (cat != "all") repo.advancementsByCategory(cat)
        else repo.searchAdvancements(q)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteAdvancements: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("advancement")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }

    fun toggleExpanded(id: String) {
        val current = _expandedIds.value
        _expandedIds.value = if (id in current) current - id else current + id
    }

    fun expandAdvancement(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "advancement", displayName = displayName))
        }
    }
}

private fun typeLabel(type: String): String = when (type) {
    "task" -> "Task"
    "goal" -> "Goal"
    "challenge" -> "Challenge"
    else -> type.replaceFirstChar { it.uppercase() }
}

private fun categoryLabel(cat: String): String = when (cat) {
    "minecraft" -> "Minecraft"
    "nether" -> "Nether"
    "end" -> "The End"
    "adventure" -> "Adventure"
    "husbandry" -> "Husbandry"
    else -> cat.replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancementsScreen(
    targetAdvancementId: String? = null,
    vm: AdvancementsViewModel = viewModel(),
) {
    val query by vm.query.collectAsState()
    val category by vm.category.collectAsState()
    val advancements by vm.advancements.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val favoriteAdvancements by vm.favoriteAdvancements.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(targetAdvancementId, advancements) {
        if (targetAdvancementId != null && advancements.isNotEmpty()) {
            vm.expandAdvancement(targetAdvancementId)
            val idx = advancements.indexOfFirst { it.id == targetAdvancementId }
            if (idx >= 0) listState.animateScrollToItem(idx + 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search advancements\u2026", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("all", "minecraft", "nether", "end", "adventure", "husbandry").forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { vm.setCategory(cat) },
                    label = { Text(if (cat == "all") "All" else categoryLabel(cat), style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Enchant,
                    title = "Advancements",
                    description = "All advancements across Minecraft, Nether, The End, Adventure, and Husbandry",
                    stat = "${advancements.size} advancements",
                )
            }
            if (favoriteAdvancements.isNotEmpty()) {
                item(key = "fav_header") {
                    Text("Favorites", style = MaterialTheme.typography.titleSmall, color = Gold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(favoriteAdvancements, key = { "fav_${it.id}" }) { fav ->
                    BrowseListItem(
                        headline = fav.displayName,
                        supporting = "",
                        supportingMaxLines = 1,
                        leadingIcon = PixelIcons.Enchant,
                        trailing = {
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = Gold, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
            items(advancements, key = { it.id }) { adv ->
                val isExpanded = adv.id in expandedIds
                Column {
                    BrowseListItem(
                        headline = adv.name,
                        supporting = "",
                        supportingMaxLines = 1,
                        leadingIcon = PixelIcons.Enchant,
                        modifier = Modifier.clickable { vm.toggleExpanded(adv.id) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val typeColor = when (adv.type) {
                                        "challenge" -> Gold
                                        "goal" -> PotionBlue
                                        else -> Emerald
                                    }
                                    CategoryBadge(label = typeLabel(adv.type), color = typeColor)
                                    Spacer(Modifier.height(2.dp))
                                    CategoryBadge(label = categoryLabel(adv.category), color = Stone500)
                                }
                                Spacer(Modifier.width(4.dp))
                                val isFav = adv.id in favoriteIds
                                IconButton(onClick = { vm.toggleFavorite(adv.id, adv.name) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "Favorite",
                                        tint = if (isFav) Gold else Stone700,
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
                        AdvancementDetailCard(adv)
                    }
                }
            }
            if (advancements.isEmpty()) item {
                EmptyState(
                    icon = PixelIcons.SearchOff,
                    title = "No advancements found",
                    subtitle = "Try a different search or filter",
                )
            }
        }
    }
}

@Composable
private fun AdvancementDetailCard(adv: AdvancementEntity) {
    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        MinecraftIdRow(adv.id)
        if (adv.description.isNotEmpty()) {
            Text(adv.description, style = MaterialTheme.typography.bodyMedium, color = Stone300)
        }
        StatRow("Category", categoryLabel(adv.category))
        StatRow("Type", typeLabel(adv.type))
        if (adv.parent.isNotEmpty()) {
            StatRow("Requires", adv.parent.substringAfterLast('/').replace('_', ' ').replaceFirstChar { it.uppercase() })
        }
    }
}
