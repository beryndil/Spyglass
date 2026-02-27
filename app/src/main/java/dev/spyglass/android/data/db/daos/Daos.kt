package dev.spyglass.android.data.db.daos

import androidx.room.*
import dev.spyglass.android.data.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {
    @Query("SELECT * FROM blocks WHERE name LIKE '%' || :q || '%' OR id LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks ORDER BY name")
    fun all(): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks WHERE id = :id")
    suspend fun byId(id: String): BlockEntity?

    @Query("SELECT * FROM blocks WHERE category = :cat ORDER BY name")
    fun byCategory(cat: String): Flow<List<BlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BlockEntity>)

    @Query("SELECT COUNT(*) FROM blocks")
    suspend fun count(): Int
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes WHERE outputItem LIKE '%' || :q || '%' ORDER BY outputItem")
    fun search(q: String): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE outputItem = :itemId")
    fun forItem(itemId: String): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes ORDER BY outputItem")
    fun all(): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecipeEntity>)

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Query("SELECT * FROM recipes WHERE ingredientsJson LIKE '%\"' || :ingredient || '\"%' ORDER BY outputItem")
    fun recipesUsingIngredient(ingredient: String): Flow<List<RecipeEntity>>
}

@Dao
interface MobDao {
    @Query("SELECT * FROM mobs WHERE name LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<MobEntity>>

    @Query("SELECT * FROM mobs ORDER BY name")
    fun all(): Flow<List<MobEntity>>

    @Query("SELECT * FROM mobs WHERE category = :cat ORDER BY name")
    fun byCategory(cat: String): Flow<List<MobEntity>>

    @Query("SELECT * FROM mobs WHERE id = :id")
    suspend fun byId(id: String): MobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MobEntity>)

    @Query("SELECT COUNT(*) FROM mobs")
    suspend fun count(): Int
}

@Dao
interface BiomeDao {
    @Query("SELECT * FROM biomes WHERE name LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<BiomeEntity>>

    @Query("SELECT * FROM biomes ORDER BY name")
    fun all(): Flow<List<BiomeEntity>>

    @Query("SELECT * FROM biomes WHERE id = :id")
    suspend fun byId(id: String): BiomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BiomeEntity>)

    @Query("SELECT COUNT(*) FROM biomes")
    suspend fun count(): Int
}

@Dao
interface EnchantDao {
    @Query("SELECT * FROM enchants WHERE name LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<EnchantEntity>>

    @Query("SELECT * FROM enchants ORDER BY name")
    fun all(): Flow<List<EnchantEntity>>

    @Query("SELECT * FROM enchants WHERE ',' || target || ',' LIKE '%,' || :target || ',%' OR target = 'all' ORDER BY name")
    fun forTarget(target: String): Flow<List<EnchantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EnchantEntity>)

    @Query("SELECT COUNT(*) FROM enchants")
    suspend fun count(): Int
}

@Dao
interface PotionDao {
    @Query("SELECT * FROM potions WHERE name LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<PotionEntity>>

    @Query("SELECT * FROM potions ORDER BY name")
    fun all(): Flow<List<PotionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PotionEntity>)

    @Query("SELECT COUNT(*) FROM potions")
    suspend fun count(): Int
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades WHERE profession LIKE '%' || :q || '%' OR sellItem LIKE '%' || :q || '%' ORDER BY profession, level")
    fun search(q: String): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades ORDER BY profession, level")
    fun all(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE profession = :prof ORDER BY level")
    fun byProfession(prof: String): Flow<List<TradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TradeEntity>)

    @Query("SELECT COUNT(*) FROM trades")
    suspend fun count(): Int
}

@Dao
interface StructureDao {
    @Query("SELECT * FROM structures WHERE name LIKE '%' || :q || '%' OR id LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<StructureEntity>>

    @Query("SELECT * FROM structures ORDER BY name")
    fun all(): Flow<List<StructureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StructureEntity>)

    @Query("SELECT COUNT(*) FROM structures")
    suspend fun count(): Int
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE name LIKE '%' || :q || '%' OR id LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY name")
    fun all(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun byId(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE category = :cat ORDER BY name")
    fun byCategory(cat: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int
}
