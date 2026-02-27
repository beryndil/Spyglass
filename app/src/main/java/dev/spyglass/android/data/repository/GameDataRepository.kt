package dev.spyglass.android.data.repository

import android.content.Context
import dev.spyglass.android.data.db.SpyglassDatabase
import dev.spyglass.android.data.db.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all game data queries.
 * ViewModels depend only on this class, never on DAOs directly.
 */
class GameDataRepository(context: Context) {
    private val db = SpyglassDatabase.get(context)

    // Blocks
    fun searchBlocks(q: String): Flow<List<BlockEntity>>  = if (q.isBlank()) db.blockDao().all() else db.blockDao().search(q)
    fun blocksByCategory(cat: String): Flow<List<BlockEntity>> = db.blockDao().byCategory(cat)
    suspend fun blockById(id: String): BlockEntity?        = db.blockDao().byId(id)
    fun blockCountFlow(): Flow<Int> = db.blockDao().countFlow()

    // Recipes
    fun searchRecipes(q: String): Flow<List<RecipeEntity>> = if (q.isBlank()) db.recipeDao().all() else db.recipeDao().search(q)
    fun recipesForItem(itemId: String): Flow<List<RecipeEntity>> = db.recipeDao().forItem(itemId)
    fun recipesUsingIngredient(ingredient: String): Flow<List<RecipeEntity>> = db.recipeDao().recipesUsingIngredient(ingredient)

    // Mobs
    fun searchMobs(q: String): Flow<List<MobEntity>>       = if (q.isBlank()) db.mobDao().all() else db.mobDao().search(q)
    fun mobsByCategory(cat: String): Flow<List<MobEntity>> = db.mobDao().byCategory(cat)
    fun breedableMobs(): Flow<List<MobEntity>>             = db.mobDao().breedable()
    suspend fun mobById(id: String): MobEntity?            = db.mobDao().byId(id)

    // Biomes
    fun searchBiomes(q: String): Flow<List<BiomeEntity>>   = if (q.isBlank()) db.biomeDao().all() else db.biomeDao().search(q)
    suspend fun biomeById(id: String): BiomeEntity?        = db.biomeDao().byId(id)

    // Enchants
    fun searchEnchants(q: String): Flow<List<EnchantEntity>>     = if (q.isBlank()) db.enchantDao().all() else db.enchantDao().search(q)
    fun enchantsForTarget(target: String): Flow<List<EnchantEntity>> = db.enchantDao().forTarget(target)

    // Potions
    fun searchPotions(q: String): Flow<List<PotionEntity>>  = if (q.isBlank()) db.potionDao().all() else db.potionDao().search(q)

    // Trades
    fun searchTrades(q: String): Flow<List<TradeEntity>>    = if (q.isBlank()) db.tradeDao().all() else db.tradeDao().search(q)
    fun tradesByProfession(prof: String): Flow<List<TradeEntity>> = db.tradeDao().byProfession(prof)

    // Structures
    fun searchStructures(q: String): Flow<List<StructureEntity>> = if (q.isBlank()) db.structureDao().all() else db.structureDao().search(q)

    // Items
    fun searchItems(q: String): Flow<List<ItemEntity>>       = if (q.isBlank()) db.itemDao().all() else db.itemDao().search(q)
    fun itemsByCategory(cat: String): Flow<List<ItemEntity>> = db.itemDao().byCategory(cat)
    suspend fun itemById(id: String): ItemEntity?            = db.itemDao().byId(id)
    fun itemCountFlow(): Flow<Int> = db.itemDao().countFlow()

    // Favorites
    fun allFavorites(): Flow<List<FavoriteEntity>>            = db.favoriteDao().all()
    fun favoritesByType(type: String): Flow<List<FavoriteEntity>> = db.favoriteDao().byType(type)
    fun allFavoriteIds(): Flow<List<String>>                  = db.favoriteDao().allIds()
    fun isFavorite(id: String): Flow<Boolean>                 = db.favoriteDao().isFavorite(id)
    suspend fun insertFavorite(fav: FavoriteEntity)           { db.favoriteDao().insert(fav) }
    suspend fun deleteFavorite(id: String)                    { db.favoriteDao().delete(id) }
    suspend fun deleteAllFavorites()                          { db.favoriteDao().deleteAll() }

    // Shopping lists
    fun allShoppingLists(): Flow<List<ShoppingListEntity>>    = db.shoppingListDao().allLists()
    suspend fun createShoppingList(name: String): Long        = db.shoppingListDao().insertList(ShoppingListEntity(name = name))
    suspend fun renameShoppingList(listId: Long, name: String) { db.shoppingListDao().renameList(listId, name) }
    suspend fun deleteShoppingList(listId: Long)              { db.shoppingListDao().deleteList(listId) }
    fun shoppingListItems(listId: Long): Flow<List<ShoppingListItemEntity>> = db.shoppingListDao().itemsForList(listId)
    suspend fun addToShoppingList(listId: Long, itemId: String, itemName: String, quantity: Int) {
        val existing = db.shoppingListDao().findItem(listId, itemId)
        if (existing != null) {
            db.shoppingListDao().updateItemQuantity(existing.id, existing.quantity + quantity)
        } else {
            db.shoppingListDao().insertItem(
                ShoppingListItemEntity(listId = listId, itemId = itemId, itemName = itemName, quantity = quantity)
            )
        }
    }
    suspend fun updateShoppingItemQuantity(id: Long, quantity: Int) { db.shoppingListDao().updateItemQuantity(id, quantity) }
    suspend fun setShoppingItemChecked(id: Long, checked: Boolean)  { db.shoppingListDao().setItemChecked(id, checked) }
    suspend fun deleteShoppingItem(id: Long)                        { db.shoppingListDao().deleteItem(id) }

    // Todos
    fun allTodos(): Flow<List<TodoEntity>>                          = db.todoDao().all()
    fun incompleteTodos(): Flow<List<TodoEntity>>                   = db.todoDao().incomplete()
    fun incompleteTodosPreview(limit: Int = 3): Flow<List<TodoEntity>> = db.todoDao().incompletePreview(limit)
    fun incompleteTodoCount(): Flow<Int>                            = db.todoDao().incompleteCount()
    suspend fun createTodo(todo: TodoEntity): Long                  = db.todoDao().insert(todo)
    suspend fun toggleTodoCompleted(id: Long, completed: Boolean) {
        db.todoDao().setCompleted(id, completed, if (completed) System.currentTimeMillis() else null)
    }
    suspend fun updateTodoTitle(id: Long, title: String)            { db.todoDao().updateTitle(id, title) }
    suspend fun linkTodo(id: Long, linkedType: String?, linkedId: Long?) { db.todoDao().setLink(id, linkedType, linkedId) }
    suspend fun deleteTodo(id: Long)                                { db.todoDao().delete(id) }
    suspend fun deleteCompletedTodos()                              { db.todoDao().deleteCompleted() }
    fun todosForLink(type: String, id: Long): Flow<List<TodoEntity>> = db.todoDao().findByLink(type, id)

    companion object {
        @Volatile private var INSTANCE: GameDataRepository? = null
        fun get(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: GameDataRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
