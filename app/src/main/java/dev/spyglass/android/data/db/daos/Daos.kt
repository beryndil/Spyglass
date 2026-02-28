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

    @Query("SELECT COUNT(*) FROM blocks")
    fun countFlow(): Flow<Int>

    @Query("DELETE FROM blocks")
    suspend fun deleteAll()
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

    @Query("SELECT r.* FROM recipes r INNER JOIN items i ON r.outputItem = i.id WHERE i.category = :category ORDER BY r.outputItem")
    fun byItemCategory(category: String): Flow<List<RecipeEntity>>

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()
}

@Dao
interface MobDao {
    @Query("SELECT * FROM mobs WHERE name LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<MobEntity>>

    @Query("SELECT * FROM mobs ORDER BY name")
    fun all(): Flow<List<MobEntity>>

    @Query("SELECT * FROM mobs WHERE category = :cat ORDER BY name")
    fun byCategory(cat: String): Flow<List<MobEntity>>

    @Query("SELECT * FROM mobs WHERE length(breeding) > 0 ORDER BY name")
    fun breedable(): Flow<List<MobEntity>>

    @Query("SELECT * FROM mobs WHERE id = :id")
    suspend fun byId(id: String): MobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MobEntity>)

    @Query("SELECT COUNT(*) FROM mobs")
    suspend fun count(): Int

    @Query("DELETE FROM mobs")
    suspend fun deleteAll()
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

    @Query("DELETE FROM biomes")
    suspend fun deleteAll()
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

    @Query("DELETE FROM enchants")
    suspend fun deleteAll()
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

    @Query("DELETE FROM potions")
    suspend fun deleteAll()
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

    @Query("DELETE FROM trades")
    suspend fun deleteAll()
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

    @Query("DELETE FROM structures")
    suspend fun deleteAll()
}

@Dao
interface AdvancementDao {
    @Query("SELECT * FROM advancements WHERE name LIKE '%' || :q || '%' OR id LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<AdvancementEntity>>

    @Query("SELECT * FROM advancements ORDER BY name")
    fun all(): Flow<List<AdvancementEntity>>

    @Query("SELECT * FROM advancements WHERE id = :id")
    suspend fun byId(id: String): AdvancementEntity?

    @Query("SELECT * FROM advancements WHERE category = :cat ORDER BY name")
    fun byCategory(cat: String): Flow<List<AdvancementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AdvancementEntity>)

    @Query("SELECT COUNT(*) FROM advancements")
    suspend fun count(): Int

    @Query("DELETE FROM advancements")
    suspend fun deleteAll()
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

    @Query("SELECT COUNT(*) FROM items")
    fun countFlow(): Flow<Int>

    @Query("DELETE FROM items")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun all(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%' OR label LIKE '%' || :q || '%' ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE label = :label ORDER BY updatedAt DESC")
    fun byLabel(label: String): Flow<List<NoteEntity>>

    @Query("SELECT DISTINCT label FROM notes WHERE label != '' ORDER BY label")
    fun allLabels(): Flow<List<String>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: Long): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("UPDATE notes SET title = :title, label = :label, content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, title: String, label: String, content: String, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints ORDER BY createdAt DESC")
    fun all(): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE name LIKE '%' || :q || '%' OR notes LIKE '%' || :q || '%' ORDER BY createdAt DESC")
    fun search(q: String): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE category = :cat ORDER BY createdAt DESC")
    fun byCategory(cat: String): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE dimension = :dim ORDER BY createdAt DESC")
    fun byDimension(dim: String): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun byId(id: Long): WaypointEntity?

    @Insert
    suspend fun insert(waypoint: WaypointEntity): Long

    @Query("UPDATE waypoints SET name = :name, x = :x, y = :y, z = :z, dimension = :dimension, category = :category, color = :color, notes = :notes WHERE id = :id")
    suspend fun update(id: Long, name: String, x: Int, y: Int, z: Int, dimension: String, category: String, color: String, notes: String)

    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AdvancementProgressDao {
    @Query("SELECT advancementId FROM advancement_progress WHERE completed = 1")
    fun completedIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM advancement_progress WHERE completed = 1")
    fun completedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: AdvancementProgressEntity)

    @Query("UPDATE advancement_progress SET completed = :completed, completedAt = :completedAt WHERE advancementId = :id")
    suspend fun setCompleted(id: String, completed: Boolean, completedAt: Long?)

    @Query("SELECT * FROM advancement_progress WHERE advancementId = :id")
    suspend fun byId(id: String): AdvancementProgressEntity?

    @Query("DELETE FROM advancement_progress")
    suspend fun deleteAll()
}

@Dao
interface CommandDao {
    @Query("SELECT * FROM commands WHERE name LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%' OR id LIKE '%' || :q || '%' ORDER BY name")
    fun search(q: String): Flow<List<CommandEntity>>

    @Query("SELECT * FROM commands ORDER BY name")
    fun all(): Flow<List<CommandEntity>>

    @Query("SELECT * FROM commands WHERE id = :id")
    suspend fun byId(id: String): CommandEntity?

    @Query("SELECT * FROM commands WHERE category = :cat ORDER BY name")
    fun byCategory(cat: String): Flow<List<CommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CommandEntity>)

    @Query("SELECT COUNT(*) FROM commands")
    suspend fun count(): Int

    @Query("DELETE FROM commands")
    suspend fun deleteAll()
}
