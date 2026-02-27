package dev.spyglass.android.browse.enchants

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
import dev.spyglass.android.data.db.entities.EnchantEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.ui.graphics.Color

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EnchantsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo   = GameDataRepository.get(app)
    private val _query  = MutableStateFlow("")
    private val _target = MutableStateFlow("all")
    val query:  StateFlow<String> = _query.asStateFlow()
    val target: StateFlow<String> = _target.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val enchants: StateFlow<List<EnchantEntity>> = combine(_query.debounce(200), _target) { q, t ->
        if (q.isBlank() && t == "all") repo.searchEnchants("") else
        if (t != "all") repo.enchantsForTarget(t) else repo.searchEnchants(q)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    fun setQuery(q: String)  { _query.value = q }
    fun setTarget(t: String) { _target.value = t }
    fun clearWarning() { _warningMessage.value = null }

    fun toggleExpanded(id: String) {
        val current = _expandedIds.value
        if (id in current) {
            _expandedIds.value = current - id
            return
        }
        // Check incompatibilities with already-expanded enchants
        val allEnchants = enchants.value
        val newEnchant = allEnchants.find { it.id == id }
        if (newEnchant != null) {
            val newIncompat = parseIncompatible(newEnchant.incompatibleJson)
            for (expandedId in current) {
                val expandedEnchant = allEnchants.find { it.id == expandedId } ?: continue
                val expandedIncompat = parseIncompatible(expandedEnchant.incompatibleJson)
                if (expandedId in newIncompat || id in expandedIncompat) {
                    _warningMessage.value = "${newEnchant.name} is incompatible with ${expandedEnchant.name}"
                    return
                }
            }
        }
        _expandedIds.value = current + id
    }

    fun expandEnchant(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteEnchants: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("enchant")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "enchant", displayName = displayName))
        }
    }
}

private fun formatTarget(target: String): String =
    target.split(",").joinToString(", ") {
        it.trim().replace('_', ' ').replaceFirstChar { c -> c.uppercase() }
    }

private fun formatId(id: String): String =
    id.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun parseIncompatible(json: String): List<String> {
    val trimmed = json.trim()
    if (trimmed.isBlank() || trimmed == "[]") return emptyList()
    return runCatching {
        Json.parseToJsonElement(trimmed).jsonArray.map {
            it.jsonPrimitive.content
        }
    }.getOrElse {
        // Fallback: try comma-separated
        trimmed.removeSurrounding("[", "]")
            .replace("\"", "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

private fun targetIcon(target: String): SpyglassIcon? = when (target) {
    "armor"       -> ItemTextures.get("diamond_chestplate")
    "helmet"      -> ItemTextures.get("diamond_helmet")
    "chestplate"  -> ItemTextures.get("diamond_chestplate")
    "leggings"    -> ItemTextures.get("diamond_leggings")
    "boots"       -> ItemTextures.get("diamond_boots")
    "sword"       -> ItemTextures.get("diamond_sword")
    "axe"         -> ItemTextures.get("diamond_axe")
    "pickaxe"     -> ItemTextures.get("diamond_pickaxe")
    "bow"         -> ItemTextures.get("bow")
    "crossbow"    -> ItemTextures.get("crossbow")
    "trident"     -> ItemTextures.get("trident")
    "mace"        -> ItemTextures.get("mace")
    "fishing_rod" -> ItemTextures.get("fishing_rod")
    else          -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnchantsScreen(vm: EnchantsViewModel = viewModel()) {
    val query       by vm.query.collectAsState()
    val target      by vm.target.collectAsState()
    val enchants    by vm.enchants.collectAsState()
    val expandedIds by vm.expandedIds.collectAsState()
    val warning     by vm.warningMessage.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val favoriteEnchants by vm.favoriteEnchants.collectAsState()
    val listState   = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when incompatible enchant warning fires
    LaunchedEffect(warning) {
        if (warning != null) {
            snackbarHostState.showSnackbar(warning!!, duration = SnackbarDuration.Short)
            vm.clearWarning()
        }
    }

    // Build a map for quick lookup by ID for scrolling to incompatible enchants
    val enchantIndex = remember(enchants) { enchants.mapIndexed { i, e -> e.id to i }.toMap() }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search enchantments\u2026", color = Stone500) },
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
            listOf("all", "armor", "sword", "bow", "crossbow", "trident", "mace", "fishing_rod").forEach { t ->
                val icon = targetIcon(t)
                FilterChip(selected = target == t, onClick = { vm.setTarget(t) },
                    label = { Text(t.replace('_', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (icon != null) { { SpyglassIconImage(icon, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
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
                    title = "Enchantments",
                    description = "All enchantments by target, rarity, and compatibility",
                    stat = "${enchants.size} enchantments",
                )
            }
            if (favoriteEnchants.isNotEmpty()) {
                item(key = "fav_header") {
                    Text("Favorites", style = MaterialTheme.typography.titleSmall, color = Gold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(favoriteEnchants, key = { "fav_${it.id}" }) { fav ->
                    BrowseListItem(
                        headline    = fav.displayName,
                        supporting  = "",
                        supportingMaxLines = 1,
                        leadingIcon = EnchantTextures.get(fav.id) ?: PixelIcons.Enchant,
                        trailing    = {
                            IconButton(onClick = { vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = Gold, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
            items(enchants, key = { it.id }) { e ->
                val isExpanded = e.id in expandedIds
                Column {
                    BrowseListItem(
                        headline    = buildString {
                            append(e.name)
                            if (e.isTreasure && !e.isCurse) append("  \u2022 Anvil Only")
                            if (e.isCurse) append("  \u2022 Curse")
                        },
                        supporting  = "",
                        supportingMaxLines = 1,
                        leadingIcon = EnchantTextures.get(e.id) ?: PixelIcons.Enchant,
                        modifier    = Modifier.clickable { vm.toggleExpanded(e.id) },
                        trailing    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Max ${e.maxLevel}", style = MaterialTheme.typography.bodySmall, color = Gold)
                                    Spacer(Modifier.height(2.dp))
                                    val rarityColor = when (e.rarity.lowercase()) {
                                        "common"   -> Emerald
                                        "uncommon" -> PotionBlue
                                        "rare"     -> EnderPurple
                                        "very_rare", "very rare" -> Gold
                                        else       -> Stone500
                                    }
                                    CategoryBadge(label = e.rarity.replace('_', ' '), color = rarityColor)
                                    if (e.isTreasure) {
                                        Spacer(Modifier.height(2.dp))
                                        CategoryBadge(
                                            label = if (e.isCurse) "Curse" else "Anvil",
                                            color = if (e.isCurse) Red400 else NetherRed,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                val isFav = e.id in favoriteIds
                                IconButton(onClick = { vm.toggleFavorite(e.id, e.name) }, modifier = Modifier.size(32.dp)) {
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
                        EnchantDetailCard(e, enchantIndex, listState, vm)
                    }
                }
            }
            if (enchants.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No enchantments found",
                    subtitle = "Try a different search or filter",
                )
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
    )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnchantDetailCard(
    enchant: EnchantEntity,
    enchantIndex: Map<String, Int>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    vm: EnchantsViewModel,
) {
    val scope = rememberCoroutineScope()
    val incompatible = parseIncompatible(enchant.incompatibleJson)

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        MinecraftIdRow(enchant.id)

        // Description
        if (enchant.description.isNotEmpty()) {
            Text(enchant.description, style = MaterialTheme.typography.bodyMedium, color = Stone300)
        }

        // Stats
        StatRow("Max Level", "${enchant.maxLevel}")
        StatRow("Applies to", formatTarget(enchant.target))
        StatRow("Rarity", enchant.rarity.replace('_', ' ').replaceFirstChar { it.uppercase() })
        if (enchant.isTreasure) StatRow("Treasure", "Yes (Loot/Anvil only)")
        if (enchant.isCurse) StatRow("Curse", "Yes")

        // Incompatible enchantments
        if (incompatible.isNotEmpty()) {
            SpyglassDivider()
            Text("Incompatible with", style = MaterialTheme.typography.labelSmall, color = Gold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                incompatible.forEach { incompId ->
                    AssistChip(
                        onClick = {
                            vm.expandEnchant(incompId)
                            val idx = enchantIndex[incompId]
                            if (idx != null) {
                                scope.launch {
                                    listState.animateScrollToItem(idx + 1) // +1 for intro header
                                }
                            }
                        },
                        label = { Text(formatId(incompId), style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = NetherRed,
                            containerColor = NetherRed.copy(alpha = 0.12f),
                        ),
                        border = null,
                    )
                }
            }
        }
    }
}
