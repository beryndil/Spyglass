package dev.spyglass.android.data.repository

import android.content.Context
import android.os.Trace
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
    fun recipesByItemCategory(category: String): Flow<List<RecipeEntity>> = db.recipeDao().byItemCategory(category)

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

    // Advancements
    fun searchAdvancements(q: String): Flow<List<AdvancementEntity>> = if (q.isBlank()) db.advancementDao().all() else db.advancementDao().search(q)
    fun advancementsByCategory(cat: String): Flow<List<AdvancementEntity>> = db.advancementDao().byCategory(cat)
    suspend fun advancementById(id: String): AdvancementEntity? = db.advancementDao().byId(id)

    // Advancement Progress
    fun advancementCompletedIds(): Flow<List<String>> = db.advancementProgressDao().completedIds()
    fun advancementCompletedCount(): Flow<Int> = db.advancementProgressDao().completedCount()
    suspend fun toggleAdvancementCompleted(id: String, completed: Boolean) {
        val existing = db.advancementProgressDao().byId(id)
        if (existing != null) {
            db.advancementProgressDao().setCompleted(id, completed, if (completed) System.currentTimeMillis() else null)
        } else {
            db.advancementProgressDao().upsert(AdvancementProgressEntity(id, completed, if (completed) System.currentTimeMillis() else null))
        }
    }
    suspend fun resetAllAdvancementProgress() { db.advancementProgressDao().deleteAll() }

    // Items
    fun searchItems(q: String): Flow<List<ItemEntity>>       = if (q.isBlank()) db.itemDao().all() else db.itemDao().search(q)
    fun itemsByCategory(cat: String): Flow<List<ItemEntity>> = db.itemDao().byCategory(cat)
    suspend fun itemById(id: String): ItemEntity?            = db.itemDao().byId(id)
    fun itemCountFlow(): Flow<Int> = db.itemDao().countFlow()

    // Notes
    fun allNotes(): Flow<List<NoteEntity>>                   = db.noteDao().all()
    fun searchNotes(q: String): Flow<List<NoteEntity>>       = if (q.isBlank()) db.noteDao().all() else db.noteDao().search(q)
    fun notesByLabel(label: String): Flow<List<NoteEntity>>  = db.noteDao().byLabel(label)
    fun allNoteLabels(): Flow<List<String>>                  = db.noteDao().allLabels()
    suspend fun noteById(id: Long): NoteEntity?              = db.noteDao().byId(id)
    suspend fun createNote(note: NoteEntity): Long           = db.noteDao().insert(note)
    suspend fun updateNote(id: Long, title: String, label: String, content: String) {
        db.noteDao().update(id, title, label, content, System.currentTimeMillis())
    }
    suspend fun deleteNote(id: Long)                         { db.noteDao().delete(id) }

    // Waypoints
    fun allWaypoints(): Flow<List<WaypointEntity>>                     = db.waypointDao().all()
    fun searchWaypoints(q: String): Flow<List<WaypointEntity>>         = if (q.isBlank()) db.waypointDao().all() else db.waypointDao().search(q)
    fun waypointsByCategory(cat: String): Flow<List<WaypointEntity>>   = db.waypointDao().byCategory(cat)
    fun waypointsByDimension(dim: String): Flow<List<WaypointEntity>>  = db.waypointDao().byDimension(dim)
    suspend fun waypointById(id: Long): WaypointEntity?                = db.waypointDao().byId(id)
    suspend fun createWaypoint(waypoint: WaypointEntity): Long         = db.waypointDao().insert(waypoint)
    suspend fun updateWaypoint(id: Long, name: String, x: Int, y: Int, z: Int, dimension: String, category: String, color: String, notes: String) {
        db.waypointDao().update(id, name, x, y, z, dimension, category, color, notes)
    }
    suspend fun deleteWaypoint(id: Long)                               { db.waypointDao().delete(id) }

    // Commands
    fun searchCommands(q: String): Flow<List<CommandEntity>> = if (q.isBlank()) db.commandDao().all() else db.commandDao().search(q)

    // One-shot search methods (LIMIT 5, for global search performance)
    suspend fun searchBlocksOnce(q: String): List<BlockEntity>         = db.blockDao().searchOnce(q)
    suspend fun searchRecipesOnce(q: String): List<RecipeEntity>       = db.recipeDao().searchOnce(q)
    suspend fun searchMobsOnce(q: String): List<MobEntity>            = db.mobDao().searchOnce(q)
    suspend fun searchBiomesOnce(q: String): List<BiomeEntity>         = db.biomeDao().searchOnce(q)
    suspend fun searchEnchantsOnce(q: String): List<EnchantEntity>     = db.enchantDao().searchOnce(q)
    suspend fun searchPotionsOnce(q: String): List<PotionEntity>       = db.potionDao().searchOnce(q)
    suspend fun searchTradesOnce(q: String): List<TradeEntity>         = db.tradeDao().searchOnce(q)
    suspend fun searchStructuresOnce(q: String): List<StructureEntity> = db.structureDao().searchOnce(q)
    suspend fun searchItemsOnce(q: String): List<ItemEntity>           = db.itemDao().searchOnce(q)
    suspend fun searchAdvancementsOnce(q: String): List<AdvancementEntity> = db.advancementDao().searchOnce(q)
    suspend fun searchCommandsOnce(q: String): List<CommandEntity>     = db.commandDao().searchOnce(q)
    fun commandsByCategory(cat: String): Flow<List<CommandEntity>> = db.commandDao().byCategory(cat)
    suspend fun commandById(id: String): CommandEntity?      = db.commandDao().byId(id)

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

    // Delete all user-generated data (todos, notes, waypoints, shopping lists, favorites, advancement progress)
    suspend fun deleteAllUserData() {
        db.todoDao().deleteAll()
        db.noteDao().deleteAll()
        db.waypointDao().deleteAll()
        db.shoppingListDao().deleteAllItems()
        db.shoppingListDao().deleteAllLists()
        db.favoriteDao().deleteAll()
        db.advancementProgressDao().deleteAll()
    }

    companion object {
        @Volatile private var INSTANCE: GameDataRepository? = null
        fun get(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                Trace.beginSection("GameDataRepository.init")
                try {
                    GameDataRepository(context.applicationContext)
                } finally {
                    Trace.endSection()
                }
            }.also { INSTANCE = it }
        }
    }
}
