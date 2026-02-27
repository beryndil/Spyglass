package dev.spyglass.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.spyglass.android.data.db.daos.*
import dev.spyglass.android.data.db.entities.*

@Database(
    entities = [
        BlockEntity::class,
        RecipeEntity::class,
        MobEntity::class,
        BiomeEntity::class,
        EnchantEntity::class,
        PotionEntity::class,
        TradeEntity::class,
        StructureEntity::class,
        ItemEntity::class,
    ],
    version = 11,
    exportSchema = false,
)
abstract class SpyglassDatabase : RoomDatabase() {
    abstract fun blockDao():     BlockDao
    abstract fun recipeDao():    RecipeDao
    abstract fun mobDao():       MobDao
    abstract fun biomeDao():     BiomeDao
    abstract fun enchantDao():   EnchantDao
    abstract fun potionDao():    PotionDao
    abstract fun tradeDao():     TradeDao
    abstract fun structureDao(): StructureDao
    abstract fun itemDao():      ItemDao

    companion object {
        @Volatile private var INSTANCE: SpyglassDatabase? = null

        fun get(context: Context): SpyglassDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpyglassDatabase::class.java,
                    "spyglass.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
