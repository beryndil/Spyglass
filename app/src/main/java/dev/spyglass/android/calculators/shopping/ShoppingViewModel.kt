package dev.spyglass.android.calculators.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.core.ui.ChainStep
import dev.spyglass.android.core.ui.CraftingPlanStep
import dev.spyglass.android.core.ui.calculateChain
import dev.spyglass.android.core.ui.consolidateCraftingPlan
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.db.entities.ShoppingListEntity
import dev.spyglass.android.data.db.entities.ShoppingListItemEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)

    val allLists: StateFlow<List<ShoppingListEntity>> = repo.allShoppingLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

    val selectedListItems: StateFlow<List<ShoppingListItemEntity>> = _selectedListId
        .flatMapLatest { id ->
            if (id != null) repo.shoppingListItems(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecipes: StateFlow<Map<String, RecipeEntity>> = repo.searchRecipes("")
        .map { list -> list.associateBy { it.outputItem } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val craftingPlan: StateFlow<List<CraftingPlanStep>> = combine(selectedListItems, allRecipes) { items, recipes ->
        if (items.isEmpty()) emptyList()
        else consolidateCraftingPlan(items.map { it.itemId to it.quantity }, recipes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedItemId = MutableStateFlow<Long?>(null)
    val expandedItemId: StateFlow<Long?> = _expandedItemId.asStateFlow()

    // ── Search state for add-item ────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Pair<String, String>>> = _searchQuery
        .debounce(200)
        .flatMapLatest { q ->
            if (q.length < 2) flowOf(emptyList())
            else combine(repo.searchBlocks(q), repo.searchItems(q)) { blocks, items ->
                val qLower = q.lowercase()
                (blocks.map { it.id to it.name } + items.map { it.id to it.name })
                    .distinctBy { it.first }
                    .sortedWith(compareBy(
                        { !it.second.equals(q, ignoreCase = true) },            // exact match first
                        { !it.second.startsWith(q, ignoreCase = true) },         // prefix match next
                        { !it.second.lowercase().startsWith(qLower) },           // case-insensitive prefix
                        { it.second.length },                                     // shorter names first
                    ))
                    .take(20)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Actions ──────────────────────────────────────────────────────────────

    fun selectList(id: Long?) { _selectedListId.value = id }

    fun createList(name: String) {
        viewModelScope.launch {
            val id = repo.createShoppingList(name)
            _selectedListId.value = id
        }
    }

    fun renameList(id: Long, name: String) {
        viewModelScope.launch { repo.renameShoppingList(id, name) }
    }

    fun deleteList(id: Long) {
        viewModelScope.launch {
            repo.deleteShoppingList(id)
            if (_selectedListId.value == id) _selectedListId.value = null
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun addItem(itemId: String, itemName: String, quantity: Int) {
        val listId = _selectedListId.value ?: return
        if (quantity <= 0) return
        viewModelScope.launch {
            repo.addToShoppingList(listId, itemId, itemName, quantity)
            _searchQuery.value = ""
        }
    }

    fun updateQuantity(id: Long, quantity: Int) {
        if (quantity <= 0) return
        viewModelScope.launch { repo.updateShoppingItemQuantity(id, quantity) }
    }

    fun toggleChecked(item: ShoppingListItemEntity) {
        viewModelScope.launch { repo.setShoppingItemChecked(item.id, !item.checked) }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch { repo.deleteShoppingItem(id) }
    }

    fun toggleBreakdown(itemId: Long) {
        _expandedItemId.value = if (_expandedItemId.value == itemId) null else itemId
    }

    fun resolveChain(itemId: String, quantity: Int, recipes: Map<String, RecipeEntity>): List<ChainStep> =
        calculateChain(itemId, quantity.toLong(), recipes)
}
