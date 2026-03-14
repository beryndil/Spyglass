package dev.spyglass.android.calculators.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.connect.PlayerAdvancementsPayload
import dev.spyglass.android.core.VersionFilterState
import dev.spyglass.android.core.applyVersionFilter
import dev.spyglass.android.core.toTagMap
import dev.spyglass.android.core.versionFilterFrom
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.AdvancementEntity
import dev.spyglass.android.data.db.entities.FavoriteEntity
import dev.spyglass.android.data.db.entities.VersionTagEntity
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TreeNode(
    val advancement: AdvancementEntity,
    val depth: Int,
    val children: List<TreeNode>,
    val totalDescendantCount: Int,
)

enum class AdvState { COMPLETED, AVAILABLE, LOCKED }

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedIds: StateFlow<Set<String>> = _expandedIds.asStateFlow()

    private val _treeExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    val treeExpandedIds: StateFlow<Set<String>> = _treeExpandedIds.asStateFlow()

    private val _sortKey = MutableStateFlow("name")
    val sortKey: StateFlow<String> = _sortKey.asStateFlow()
    fun setSortKey(k: String) { _sortKey.value = k }

    val versionFilter: StateFlow<VersionFilterState> = versionFilterFrom(app.dataStore)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VersionFilterState())
    val versionTags: StateFlow<Map<String, VersionTagEntity>> = repo.allVersionTags()
        .map { it.toTagMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val translations: StateFlow<Map<String, Map<String, String>>> =
        translationMapFlow(app.dataStore, repo, "advancement")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val advancements: StateFlow<List<AdvancementEntity>> = combine(_query.debounce(200), _category, _sortKey) { q, cat, sort ->
        val flow = if (q.isBlank() && cat == "all") repo.searchAdvancements("")
        else if (cat != "all") repo.advancementsByCategory(cat)
        else repo.searchAdvancements(q)
        flow.map { list ->
            when (sort) {
                "type" -> list.sortedWith(compareByDescending {
                    when (it.type.lowercase()) { "challenge" -> 3; "goal" -> 2; else -> 1 }
                })
                else -> list
            }
        }
    }.flatMapLatest { it }
     .applyVersionFilter(versionFilter, versionTags, "advancement") { it.id }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Auto-expand root nodes when advancements first load
        viewModelScope.launch {
            advancements.first { it.isNotEmpty() }.let { advs ->
                val rootIds = advs.filter { it.parent.isEmpty() }.map { it.id }.toSet()
                _treeExpandedIds.value = _treeExpandedIds.value + rootIds
            }
        }
    }

    val completedIds: StateFlow<Set<String>> = repo.advancementCompletedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val completedCount: StateFlow<Int> = repo.advancementCompletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoriteIds: StateFlow<Set<String>> = repo.allFavoriteIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteAdvancements: StateFlow<List<FavoriteEntity>> = repo.favoritesByType("advancement")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Connect integration ─────────────────────────────────────────────────

    private val _connectPayload = MutableStateFlow<PlayerAdvancementsPayload?>(null)

    val connectCompletedIds: StateFlow<Set<String>> = _connectPayload
        .map { payload ->
            payload?.advancements
                ?.filter { it.done }
                ?.map { it.id.removePrefix("minecraft:") }
                ?.toSet()
                ?: emptySet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val isConnectActive: StateFlow<Boolean> = _connectPayload
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val connectWorldName: StateFlow<String?> = _connectPayload
        .map { it?.worldName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val effectiveCompletedIds: StateFlow<Set<String>> = combine(isConnectActive, connectCompletedIds, completedIds) { active, connect, manual ->
        if (active) connect else manual
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setConnectAdvancements(payload: PlayerAdvancementsPayload?) {
        _connectPayload.value = payload
        if (payload != null) {
            val ids = payload.advancements.filter { it.done }.map { it.id.removePrefix("minecraft:") }.toSet()
            bulkSyncFromConnect(ids)
        }
    }

    private fun bulkSyncFromConnect(completedIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val manualIds = this@TrackerViewModel.completedIds.value
            // Mark connect-completed as completed
            for (id in completedIds) {
                repo.toggleAdvancementCompleted(id, true)
            }
            // Unmark any manual completions not in connect data
            for (id in manualIds) {
                if (id !in completedIds) {
                    repo.toggleAdvancementCompleted(id, false)
                }
            }
        }
    }

    // ── State filters ───────────────────────────────────────────────────────

    private val _showCompleted = MutableStateFlow(true)
    val showCompleted: StateFlow<Boolean> = _showCompleted.asStateFlow()
    fun setShowCompleted(v: Boolean) { _showCompleted.value = v }

    private val _showAvailable = MutableStateFlow(true)
    val showAvailable: StateFlow<Boolean> = _showAvailable.asStateFlow()
    fun setShowAvailable(v: Boolean) { _showAvailable.value = v }

    private val _showLocked = MutableStateFlow(true)
    val showLocked: StateFlow<Boolean> = _showLocked.asStateFlow()
    fun setShowLocked(v: Boolean) { _showLocked.value = v }

    // Build tree structure from flat list
    val flatTreeItems: StateFlow<List<Pair<AdvancementEntity, Int>>> = combine(advancements, _treeExpandedIds, _query) { advs, expanded, q ->
        if (q.isNotBlank()) {
            // In search mode, show flat list
            advs.map { it to 0 }
        } else {
            val tree = buildTree(advs)
            flattenTree(tree, expanded)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tree items with state filter applied
    val filteredFlatTreeItems: StateFlow<List<Pair<AdvancementEntity, Int>>> =
        combine(flatTreeItems, effectiveCompletedIds, _showCompleted, _showAvailable, _showLocked) { items, completed, showC, showA, showL ->
            if (showC && showA && showL) items
            else {
                // Build parent lookup from all advancements for state computation
                val parentMap = items.associate { (adv, _) -> adv.id to adv.parent }
                items.filter { (adv, _) ->
                    val state = computeAdvState(adv.id, parentMap, completed)
                    when (state) {
                        AdvState.COMPLETED -> showC
                        AdvState.AVAILABLE -> showA
                        AdvState.LOCKED -> showL
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Per-category completed/total counts
    val categoryCounts: StateFlow<Map<String, Pair<Int, Int>>> = combine(advancements, effectiveCompletedIds) { advs, completed ->
        val byCategory = advs.groupBy { it.category }
        byCategory.mapValues { (_, catAdvs) ->
            val completedInCat = catAdvs.count { it.id in completed }
            completedInCat to catAdvs.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) {
        _category.value = c
        // Auto-expand root nodes when switching categories
        if (c != "all") {
            viewModelScope.launch {
                advancements.first { it.isNotEmpty() }.let { advs ->
                    val rootIds = advs.filter { it.parent.isEmpty() }.map { it.id }.toSet()
                    _treeExpandedIds.value = _treeExpandedIds.value + rootIds
                }
            }
        }
    }

    fun toggleExpanded(id: String) {
        val current = _expandedIds.value
        _expandedIds.value = if (id in current) current - id else current + id
    }

    fun expandAdvancement(id: String) {
        _expandedIds.value = _expandedIds.value + id
    }

    fun toggleTreeExpanded(id: String) {
        val current = _treeExpandedIds.value
        _treeExpandedIds.value = if (id in current) current - id else current + id
    }

    fun expandTreeNode(id: String) {
        _treeExpandedIds.value = _treeExpandedIds.value + id
    }

    fun toggleCompleted(id: String) {
        viewModelScope.launch {
            val isCompleted = id in completedIds.value
            repo.toggleAdvancementCompleted(id, !isCompleted)
        }
    }

    fun toggleFavorite(id: String, displayName: String) {
        viewModelScope.launch {
            if (id in favoriteIds.value) repo.deleteFavorite(id)
            else repo.insertFavorite(FavoriteEntity(id = id, type = "advancement", displayName = displayName))
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch { repo.resetAllAdvancementProgress() }
    }

    // Navigate to a parent advancement: expand its tree path and detail card
    fun navigateToAdvancement(id: String, allAdvancements: List<AdvancementEntity>) {
        val ancestorIds = mutableListOf<String>()
        var current = id
        val byId = allAdvancements.associateBy { it.id }
        while (true) {
            val adv = byId[current] ?: break
            if (adv.parent.isNotEmpty()) {
                ancestorIds.add(adv.parent)
                current = adv.parent
            } else break
        }
        _treeExpandedIds.value = _treeExpandedIds.value + ancestorIds.toSet()
        _expandedIds.value = _expandedIds.value + id
    }

    fun advState(id: String): AdvState {
        val completed = effectiveCompletedIds.value
        val allAdvs = advancements.value
        val parentMap = allAdvs.associate { it.id to it.parent }
        return computeAdvState(id, parentMap, completed)
    }

    private fun computeAdvState(id: String, parentMap: Map<String, String>, completed: Set<String>): AdvState {
        if (id in completed) return AdvState.COMPLETED
        val parent = parentMap[id] ?: ""
        return if (parent.isEmpty() || parent in completed) AdvState.AVAILABLE else AdvState.LOCKED
    }

    private fun buildTree(advancements: List<AdvancementEntity>): List<TreeNode> {
        val byParent = advancements.groupBy { it.parent }
        fun build(parentId: String, depth: Int): List<TreeNode> {
            val children = byParent[parentId] ?: return emptyList()
            return children.sortedBy { it.name }.map { adv ->
                val childNodes = build(adv.id, depth + 1)
                val descendantCount = childNodes.sumOf { it.totalDescendantCount + 1 }
                TreeNode(adv, depth, childNodes, descendantCount)
            }
        }
        // Roots have empty parent
        return build("", 0)
    }

    private fun flattenTree(nodes: List<TreeNode>, expandedIds: Set<String>): List<Pair<AdvancementEntity, Int>> {
        val result = mutableListOf<Pair<AdvancementEntity, Int>>()
        fun flatten(node: TreeNode) {
            result.add(node.advancement to node.depth)
            if (node.advancement.id in expandedIds) {
                node.children.forEach { flatten(it) }
            }
        }
        nodes.forEach { flatten(it) }
        return result
    }
}
