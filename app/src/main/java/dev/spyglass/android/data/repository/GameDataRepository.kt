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
    suspend fun blockById(id: String): BlockEntity?        = db.blockDao().byId(id)

    // Recipes
    fun searchRecipes(q: String): Flow<List<RecipeEntity>> = if (q.isBlank()) db.recipeDao().all() else db.recipeDao().search(q)
    fun recipesForItem(itemId: String): Flow<List<RecipeEntity>> = db.recipeDao().forItem(itemId)

    // Mobs
    fun searchMobs(q: String): Flow<List<MobEntity>>       = if (q.isBlank()) db.mobDao().all() else db.mobDao().search(q)
    fun mobsByCategory(cat: String): Flow<List<MobEntity>> = db.mobDao().byCategory(cat)
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

    companion object {
        @Volatile private var INSTANCE: GameDataRepository? = null
        fun get(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: GameDataRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
