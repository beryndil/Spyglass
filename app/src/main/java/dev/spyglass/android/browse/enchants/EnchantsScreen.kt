package dev.spyglass.android.browse.enchants

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.VersionAvailability
import dev.spyglass.android.core.VersionFilterState
import dev.spyglass.android.core.applyVersionFilter
import dev.spyglass.android.core.checkAvailability
import dev.spyglass.android.core.toTagMap
import dev.spyglass.android.core.versionFilterFrom
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.core.ui.SpyglassSearchBar
import dev.spyglass.android.core.ui.rememberHapticConfirm
import dev.spyglass.android.core.ui.rememberHapticClick
import dev.spyglass.android.data.db.entities.EnchantEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.VersionTagEntity
import dev.spyglass.android.data.repository.GameDataRepository
import androidx.compose.ui.platform.LocalContext
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EnchantsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo   = GameDataRepository.get(app)
    private val _query  = MutableStateFlow("")
    private val _target = MutableStateFlow("all")
    private val _sortKey = MutableStateFlow("name")
    val query:  StateFlow<String> = _query.asStateFlow()
    val target: StateFlow<String> = _target.asStateFlow()
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    val versionFilter: StateFlow<VersionFilterState> = versionFilterFrom(app.dataStore)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionFilterState())
    val versionTags: StateFlow<Map<String, VersionTagEntity>> = repo.allVersionTags()
        .map { it.toTagMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val enchants: StateFlow<List<EnchantEntity>> = combine(_query.debounce(200), _target, _sortKey) { q, t, sort ->
        (if (q.isBlank() && t == "all") repo.searchEnchants("") else
        if (t != "all") repo.enchantsForTarget(t) else repo.searchEnchants(q)).map { list ->
            when (sort) {
                "max_level" -> list.sortedByDescending { it.maxLevel }
                "rarity" -> list.sortedWith(compareByDescending {
                    when (it.rarity.lowercase()) { "very_rare" -> 4; "rare" -> 3; "uncommon" -> 2; else -> 1 }
                })
                else -> list
            }
        }
    }.flatMapLatest { it }
     .applyVersionFilter(versionFilter, versionTags, "enchant") { it.id }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    fun setQuery(q: String)  { _query.value = q }
    fun setTarget(t: String) { _target.value = t }
    fun setSortKey(k: String) { _sortKey.value = k }
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
fun EnchantsScreen(
    targetEnchantId: String? = null,
    onCalcTab: (Int) -> Unit = {},
    onItemTap: (String) -> Unit = {},
    onMobTap: (String) -> Unit = {},
    onBiomeTap: (String) -> Unit = {},
    onStructureTap: (String) -> Unit = {},
    onEnchantTap: (String) -> Unit = {},
    entityLinkIndex: EntityLinkIndex = EntityLinkIndex(emptyList()),
    vm: EnchantsViewModel = viewModel(),
) {
    val query       by vm.query.collectAsStateWithLifecycle()
    val sortKey     by vm.sortKey.collectAsStateWithLifecycle()
    val target      by vm.target.collectAsStateWithLifecycle()
    val enchants    by vm.enchants.collectAsStateWithLifecycle()
    val expandedIds by vm.expandedIds.collectAsStateWithLifecycle()
    val warning     by vm.warningMessage.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val favoriteEnchants by vm.favoriteEnchants.collectAsStateWithLifecycle()
    val vFilter     by vm.versionFilter.collectAsStateWithLifecycle()
    val vTags       by vm.versionTags.collectAsStateWithLifecycle()
    val listState   = rememberLazyListState()
    val hapticConfirm = rememberHapticConfirm()
    val hapticClick = rememberHapticClick()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val showExperimental by remember {
        context.dataStore.data.map { it[PreferenceKeys.SHOW_EXPERIMENTAL] ?: false }
    }.collectAsStateWithLifecycle(initialValue = true)

    // Show snackbar when incompatible enchant warning fires
    LaunchedEffect(warning) {
        if (warning != null) {
            snackbarHostState.showSnackbar(warning!!, duration = SnackbarDuration.Short)
            vm.clearWarning()
        }
    }

    // Build a map for quick lookup by ID for scrolling to incompatible enchants
    val enchantIndex = remember(enchants) { enchants.mapIndexed { i, e -> e.id to i }.toMap() }

    // Auto-expand and scroll to target enchant from cross-reference
    LaunchedEffect(targetEnchantId) {
        if (targetEnchantId != null) {
            vm.setQuery("")
            vm.setTarget("all")
            snapshotFlow { enchants }
                .first { it.isNotEmpty() }
            val idx = enchantIndex[targetEnchantId]
            if (idx != null) {
                listState.scrollToItem(idx + 1) // +1 for intro header
                vm.expandEnchant(targetEnchantId)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    val sortOptions = listOf(
        SortOption(stringResource(R.string.enchants_sort_name), "name"),
        SortOption(stringResource(R.string.enchants_sort_max_level), "max_level"),
        SortOption(stringResource(R.string.enchants_sort_rarity), "rarity"),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpyglassSearchBar(
                query = query, onQueryChange = vm::setQuery,
                category = "enchants", placeholder = stringResource(R.string.enchants_search_placeholder),
                modifier = Modifier.weight(1f),
            )
            SortButton(options = sortOptions, selectedKey = sortKey, onSelect = vm::setSortKey)
        }
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("all", "armor", "sword", "bow", "crossbow", "trident", "mace", "fishing_rod").forEach { t ->
                val icon = targetIcon(t)
                FilterChip(selected = target == t, onClick = { hapticClick(); vm.setTarget(t) },
                    label = { Text(if (t == "all") stringResource(R.string.all) else t.replace('_', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
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
                    title = stringResource(R.string.enchants_title),
                    description = stringResource(R.string.enchants_description),
                    stat = stringResource(R.string.enchants_stat, enchants.size),
                )
                if (showExperimental) {
                    Text(
                        stringResource(R.string.enchants_open_librarian_guide),
                        style = MaterialTheme.typography.labelSmall,
                        color = PotionBlue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { onCalcTab(14) }
                            .padding(top = 4.dp),
                    )
                }
            }
            if (favoriteEnchants.isNotEmpty()) {
                item(key = "fav_header") {
                    Text(stringResource(R.string.favorites), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(favoriteEnchants, key = { "fav_${it.id}" }) { fav ->
                    BrowseListItem(
                        headline    = fav.displayName,
                        supporting  = "",
                        supportingMaxLines = 1,
                        leadingIcon = EnchantTextures.get(fav.id) ?: PixelIcons.Enchant,
                        trailing    = {
                            IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(fav.id, fav.displayName) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        },
                    )
                }
            }
            items(enchants, key = { it.id }) { e ->
                val tag = vTags["enchant:${e.id}"]
                val availability = checkAvailability(tag, vFilter)
                val vAlpha = when (availability) {
                    VersionAvailability.NOT_YET_ADDED -> 0.5f
                    VersionAvailability.REMOVED, VersionAvailability.WRONG_EDITION -> 0.4f
                    else -> 1f
                }
                val addedIn = tag?.let { if (vFilter.edition == "java") it.addedInJava else it.addedInBedrock } ?: ""
                val isExpanded = e.id in expandedIds
                Column(modifier = Modifier.alpha(vAlpha)) {
                    BrowseListItem(
                        headline    = buildString {
                            append(e.name)
                            if (e.isTreasure && !e.isCurse) append("  \u2022 ${stringResource(R.string.enchants_anvil_only)}")
                            if (e.isCurse) append("  \u2022 ${stringResource(R.string.enchants_curse)}")
                        },
                        supporting  = "",
                        supportingMaxLines = 1,
                        leadingIcon = EnchantTextures.get(e.id) ?: PixelIcons.Enchant,
                        modifier    = Modifier.clickable { vm.toggleExpanded(e.id) },
                        trailing    = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    if (addedIn.isNotBlank() && availability != VersionAvailability.AVAILABLE) {
                                        VersionBadge(addedIn)
                                        Spacer(Modifier.height(2.dp))
                                    }
                                    Text(stringResource(R.string.enchants_max_level, e.maxLevel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(2.dp))
                                    val rarityColor = when (e.rarity.lowercase()) {
                                        "common"   -> Emerald
                                        "uncommon" -> PotionBlue
                                        "rare"     -> EnderPurple
                                        "very_rare", "very rare" -> MaterialTheme.colorScheme.primary
                                        else       -> MaterialTheme.colorScheme.secondary
                                    }
                                    CategoryBadge(label = e.rarity.replace('_', ' '), color = rarityColor)
                                    if (e.isTreasure) {
                                        Spacer(Modifier.height(2.dp))
                                        CategoryBadge(
                                            label = if (e.isCurse) stringResource(R.string.enchants_curse) else stringResource(R.string.enchants_anvil),
                                            color = if (e.isCurse) Red400 else NetherRed,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                val isFav = e.id in favoriteIds
                                IconButton(onClick = { hapticConfirm(); vm.toggleFavorite(e.id, e.name) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = stringResource(R.string.favorite),
                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                    )
                    val reduceMotion = LocalReduceAnimations.current
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = if (reduceMotion) expandVertically(snap()) else expandVertically(),
                        exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(),
                    ) {
                        EnchantDetailCard(e, enchantIndex, listState, vm, entityLinkIndex, onItemTap, onMobTap, onBiomeTap, onStructureTap, onEnchantTap, tag, vFilter)
                    }
                }
            }
            if (enchants.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = stringResource(R.string.enchants_no_results_title),
                    subtitle = stringResource(R.string.enchants_no_results_subtitle),
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
    entityLinkIndex: EntityLinkIndex,
    onItemTap: (String) -> Unit,
    onMobTap: (String) -> Unit,
    onBiomeTap: (String) -> Unit,
    onStructureTap: (String) -> Unit,
    onEnchantTap: (String) -> Unit,
    tag: VersionTagEntity? = null,
    vFilter: VersionFilterState = VersionFilterState(),
) {
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceAnimations.current
    val incompatible = parseIncompatible(enchant.incompatibleJson)

    ResultCard(modifier = Modifier.padding(top = 4.dp)) {
        MinecraftIdRow(enchant.id)

        if (tag != null) {
            VersionEditionSection(tag, vFilter)
        }

        // Description
        if (enchant.description.isNotEmpty()) {
            LinkedDescription(
                description = enchant.description,
                linkIndex = entityLinkIndex,
                selfId = enchant.id,
                onItemTap = onItemTap,
                onMobTap = onMobTap,
                onBiomeTap = onBiomeTap,
                onStructureTap = onStructureTap,
                onEnchantTap = onEnchantTap,
            )
        }

        // Stats
        StatRow(stringResource(R.string.enchants_max_level_label), "${enchant.maxLevel}")
        StatRow(stringResource(R.string.enchants_applies_to), formatTarget(enchant.target))
        StatRow(stringResource(R.string.enchants_rarity), enchant.rarity.replace('_', ' ').replaceFirstChar { it.uppercase() })
        if (enchant.isTreasure) StatRow(stringResource(R.string.enchants_treasure), stringResource(R.string.enchants_treasure_value))
        if (enchant.isCurse) StatRow(stringResource(R.string.enchants_curse), stringResource(R.string.yes))

        // Incompatible enchantments
        if (incompatible.isNotEmpty()) {
            SpyglassDivider()
            Text(stringResource(R.string.enchants_incompatible_with), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                                    if (reduceMotion) listState.scrollToItem(idx + 1)
                                    else listState.animateScrollToItem(idx + 1) // +1 for intro header
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

        SpyglassDivider()
        ReportProblemRow(entityType = "Enchantment", entityName = enchant.name, entityId = enchant.id)
    }
}
