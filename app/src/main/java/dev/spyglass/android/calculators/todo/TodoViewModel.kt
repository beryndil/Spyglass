package dev.spyglass.android.calculators.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spyglass.android.R
import dev.spyglass.android.data.db.entities.ShoppingListEntity
import dev.spyglass.android.data.db.entities.TodoEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)

    val allTodos: StateFlow<List<TodoEntity>> = repo.allTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShoppingLists: StateFlow<List<ShoppingListEntity>> = repo.allShoppingLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Pair<String, String>>> = _searchQuery
        .debounce(200)
        .flatMapLatest { q ->
            if (q.length < 2) flowOf(emptyList())
            else combine(repo.searchBlocks(q), repo.searchItems(q)) { blocks, items ->
                (blocks.map { it.id to it.name } + items.map { it.id to it.name })
                    .distinctBy { it.first }
                    .sortedWith(compareBy(
                        { !it.second.equals(q, ignoreCase = true) },
                        { !it.second.startsWith(q, ignoreCase = true) },
                        { it.second.length },
                    ))
                    .take(20)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun createFreeformTodo(title: String, linkedType: String? = null, linkedId: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.createTodo(TodoEntity(title = title.trim(), linkedType = linkedType, linkedId = linkedId))
        }
    }

    fun createItemTodo(itemId: String, itemName: String, quantity: Int, linkedType: String? = null, linkedId: Long? = null) {
        if (quantity <= 0) return
        viewModelScope.launch {
            repo.createTodo(
                TodoEntity(
                    title = getApplication<Application>().getString(R.string.todo_gather_prefix, "$quantity $itemName"),
                    itemId = itemId,
                    itemName = itemName,
                    quantity = quantity,
                    linkedType = linkedType,
                    linkedId = linkedId,
                )
            )
            _searchQuery.value = ""
        }
    }

    fun toggleCompleted(todo: TodoEntity) {
        viewModelScope.launch {
            repo.toggleTodoCompleted(todo.id, !todo.completed)
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch { repo.deleteTodo(id) }
    }

    fun deleteCompleted() {
        viewModelScope.launch { repo.deleteCompletedTodos() }
    }

    fun editTitle(id: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repo.updateTodoTitle(id, title.trim()) }
    }

    fun linkToTool(todoId: Long, linkedType: String?, linkedId: Long?) {
        viewModelScope.launch { repo.linkTodo(todoId, linkedType, linkedId) }
    }
}
