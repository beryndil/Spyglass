package dev.spyglass.android.data.repository

import android.content.Context
import android.os.Trace
import androidx.room.withTransaction
import dev.spyglass.android.data.db.SpyglassDatabase
import dev.spyglass.android.data.db.UserDatabase
import dev.spyglass.android.data.db.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all game data queries.
 * ViewModels depend only on this class, never on DAOs directly.
 *
 * Game data lives in [SpyglassDatabase] (spyglass.db) — rebuildable from assets.
 * User data lives in [UserDatabase] (spyglass_user.db) — never destroyed by game updates.
 */
class GameDataRepository(context: Context) {
    // UserDatabase must init first to migrate data from old spyglass.db before
    // SpyglassDatabase migration 26→27 drops user tables from the game DB.
    private val userDb = UserDatabase.get(context)
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
    suspend fun enchantsForTargetOnce(target: String): List<EnchantEntity> = db.enchantDao().forTargetOnce(target)

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

    // Advancement Progress (user data)
    fun advancementCompletedIds(): Flow<List<String>> = userDb.advancementProgressDao().completedIds()
    fun advancementCompletedCount(): Flow<Int> = userDb.advancementProgressDao().completedCount()
    suspend fun toggleAdvancementCompleted(id: String, completed: Boolean) {
        userDb.withTransaction {
            val existing = userDb.advancementProgressDao().byId(id)
            if (existing != null) {
                userDb.advancementProgressDao().setCompleted(id, completed, if (completed) System.currentTimeMillis() else null)
            } else {
                userDb.advancementProgressDao().upsert(AdvancementProgressEntity(id, completed, if (completed) System.currentTimeMillis() else null))
            }
        }
    }
    suspend fun resetAllAdvancementProgress() { userDb.advancementProgressDao().deleteAll() }

    // Items
    fun searchItems(q: String): Flow<List<ItemEntity>>       = if (q.isBlank()) db.itemDao().all() else db.itemDao().search(q)
    fun itemsByCategory(cat: String): Flow<List<ItemEntity>> = db.itemDao().byCategory(cat)
    suspend fun itemById(id: String): ItemEntity?            = db.itemDao().byId(id)
    fun itemCountFlow(): Flow<Int> = db.itemDao().countFlow()

    // Notes (user data)
    fun allNotes(): Flow<List<NoteEntity>>                   = userDb.noteDao().all()
    fun searchNotes(q: String): Flow<List<NoteEntity>>       = if (q.isBlank()) userDb.noteDao().all() else userDb.noteDao().search(q)
    fun notesByLabel(label: String): Flow<List<NoteEntity>>  = userDb.noteDao().byLabel(label)
    fun allNoteLabels(): Flow<List<String>>                  = userDb.noteDao().allLabels()
    suspend fun noteById(id: Long): NoteEntity?              = userDb.noteDao().byId(id)
    suspend fun createNote(note: NoteEntity): Long           = userDb.noteDao().insert(note)
    suspend fun updateNote(id: Long, title: String, label: String, content: String) {
        userDb.noteDao().update(id, title, label, content, System.currentTimeMillis())
    }
    suspend fun deleteNote(id: Long)                         { userDb.noteDao().delete(id) }

    // Waypoints (user data)
    fun allWaypoints(): Flow<List<WaypointEntity>>                     = userDb.waypointDao().all()
    fun searchWaypoints(q: String): Flow<List<WaypointEntity>>         = if (q.isBlank()) userDb.waypointDao().all() else userDb.waypointDao().search(q)
    fun waypointsByCategory(cat: String): Flow<List<WaypointEntity>>   = userDb.waypointDao().byCategory(cat)
    fun waypointsByDimension(dim: String): Flow<List<WaypointEntity>>  = userDb.waypointDao().byDimension(dim)
    suspend fun waypointById(id: Long): WaypointEntity?                = userDb.waypointDao().byId(id)
    suspend fun createWaypoint(waypoint: WaypointEntity): Long         = userDb.waypointDao().insert(waypoint)
    suspend fun updateWaypoint(id: Long, name: String, x: Int, y: Int, z: Int, dimension: String, category: String, color: String, notes: String) {
        userDb.waypointDao().update(id, name, x, y, z, dimension, category, color, notes)
    }
    suspend fun deleteWaypoint(id: Long)                               { userDb.waypointDao().delete(id) }

    // Version Tags
    fun versionTagsByType(type: String): Flow<List<VersionTagEntity>> = db.versionTagDao().byType(type)
    fun allVersionTags(): Flow<List<VersionTagEntity>> = db.versionTagDao().all()

    // Translations (i18n overlay)
    fun translationsForType(locale: String, entityType: String): Flow<List<TranslationEntity>> =
        db.translationDao().forType(locale, entityType)
    fun getTranslation(locale: String, entityType: String, entityId: String, field: String): Flow<String?> =
        db.translationDao().get(locale, entityType, entityId, field)

    // Translation Reports (user data)
    fun allTranslationReports(): Flow<List<TranslationReportEntity>> = userDb.translationReportDao().all()
    suspend fun createTranslationReport(report: TranslationReportEntity): Long = userDb.translationReportDao().insert(report)
    suspend fun markReportSubmitted(id: Long) { userDb.translationReportDao().markSubmitted(id) }
    suspend fun deleteTranslationReport(id: Long) { userDb.translationReportDao().delete(id) }

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

    // Favorites (user data)
    fun allFavorites(): Flow<List<FavoriteEntity>>            = userDb.favoriteDao().all()
    fun favoritesByType(type: String): Flow<List<FavoriteEntity>> = userDb.favoriteDao().byType(type)
    fun allFavoriteIds(): Flow<List<String>>                  = userDb.favoriteDao().allIds()
    fun isFavorite(id: String): Flow<Boolean>                 = userDb.favoriteDao().isFavorite(id)
    suspend fun insertFavorite(fav: FavoriteEntity)           { userDb.favoriteDao().insert(fav) }
    suspend fun deleteFavorite(id: String)                    { userDb.favoriteDao().delete(id) }
    suspend fun deleteAllFavorites()                          { userDb.favoriteDao().deleteAll() }

    // Shopping lists (user data)
    fun allShoppingLists(): Flow<List<ShoppingListEntity>>    = userDb.shoppingListDao().allLists()
    suspend fun createShoppingList(name: String): Long        = userDb.shoppingListDao().insertList(ShoppingListEntity(name = name))
    suspend fun renameShoppingList(listId: Long, name: String) { userDb.shoppingListDao().renameList(listId, name) }
    suspend fun deleteShoppingList(listId: Long)              { userDb.shoppingListDao().deleteList(listId) }
    fun shoppingListItems(listId: Long): Flow<List<ShoppingListItemEntity>> = userDb.shoppingListDao().itemsForList(listId)
    suspend fun addToShoppingList(listId: Long, itemId: String, itemName: String, quantity: Int) {
        userDb.withTransaction {
            val existing = userDb.shoppingListDao().findItem(listId, itemId)
            if (existing != null) {
                userDb.shoppingListDao().updateItemQuantity(existing.id, existing.quantity + quantity)
            } else {
                userDb.shoppingListDao().insertItem(
                    ShoppingListItemEntity(listId = listId, itemId = itemId, itemName = itemName, quantity = quantity)
                )
            }
        }
    }
    suspend fun updateShoppingItemQuantity(id: Long, quantity: Int) { userDb.shoppingListDao().updateItemQuantity(id, quantity) }
    suspend fun setShoppingItemChecked(id: Long, checked: Boolean)  { userDb.shoppingListDao().setItemChecked(id, checked) }
    suspend fun deleteShoppingItem(id: Long)                        { userDb.shoppingListDao().deleteItem(id) }

    // Todos (user data)
    fun allTodos(): Flow<List<TodoEntity>>                          = userDb.todoDao().all()
    fun incompleteTodos(): Flow<List<TodoEntity>>                   = userDb.todoDao().incomplete()
    fun incompleteTodosPreview(limit: Int = 3): Flow<List<TodoEntity>> = userDb.todoDao().incompletePreview(limit)
    fun incompleteTodoCount(): Flow<Int>                            = userDb.todoDao().incompleteCount()
    suspend fun createTodo(todo: TodoEntity): Long                  = userDb.todoDao().insert(todo)
    suspend fun toggleTodoCompleted(id: Long, completed: Boolean) {
        userDb.todoDao().setCompleted(id, completed, if (completed) System.currentTimeMillis() else null)
    }
    suspend fun updateTodoTitle(id: Long, title: String)            { userDb.todoDao().updateTitle(id, title) }
    suspend fun linkTodo(id: Long, linkedType: String?, linkedId: Long?) { userDb.todoDao().setLink(id, linkedType, linkedId) }
    suspend fun deleteTodo(id: Long)                                { userDb.todoDao().delete(id) }
    suspend fun deleteCompletedTodos()                              { userDb.todoDao().deleteCompleted() }
    fun todosForLink(type: String, id: Long): Flow<List<TodoEntity>> = userDb.todoDao().findByLink(type, id)

    // Search history (user data)
    fun searchHistory(category: String): Flow<List<SearchHistoryEntity>> = userDb.searchHistoryDao().recentByCategory(category)
    fun matchingHistory(category: String, prefix: String): Flow<List<SearchHistoryEntity>> = userDb.searchHistoryDao().matchingByCategory(category, prefix)
    suspend fun saveSearchHistory(category: String, query: String) {
        userDb.searchHistoryDao().upsert(SearchHistoryEntity(category = category, query = query))
        userDb.searchHistoryDao().pruneCategory(category)
    }
    suspend fun deleteSearchHistory(id: Long) { userDb.searchHistoryDao().delete(id) }
    suspend fun clearSearchHistory(category: String) { userDb.searchHistoryDao().clearCategory(category) }

    // Delete all user-generated data (todos, notes, waypoints, shopping lists, favorites, advancement progress)
    suspend fun deleteAllUserData() {
        userDb.withTransaction {
            userDb.todoDao().deleteAll()
            userDb.noteDao().deleteAll()
            userDb.waypointDao().deleteAll()
            userDb.shoppingListDao().deleteAllItems()
            userDb.shoppingListDao().deleteAllLists()
            userDb.favoriteDao().deleteAll()
            userDb.advancementProgressDao().deleteAll()
            userDb.searchHistoryDao().deleteAll()
        }
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
