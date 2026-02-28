package dev.spyglass.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        AdvancementEntity::class,
        FavoriteEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        TodoEntity::class,
    ],
    version = 17,
    exportSchema = false,
)
abstract class SpyglassDatabase : RoomDatabase() {
    abstract fun blockDao():        BlockDao
    abstract fun recipeDao():       RecipeDao
    abstract fun mobDao():          MobDao
    abstract fun biomeDao():        BiomeDao
    abstract fun enchantDao():      EnchantDao
    abstract fun potionDao():       PotionDao
    abstract fun tradeDao():        TradeDao
    abstract fun structureDao():    StructureDao
    abstract fun itemDao():          ItemDao
    abstract fun advancementDao():  AdvancementDao
    abstract fun favoriteDao():     FavoriteDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun todoDao():         TodoDao

    companion object {
        @Volatile private var INSTANCE: SpyglassDatabase? = null

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS advancements (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT '',
                        type TEXT NOT NULL DEFAULT 'task',
                        parent TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        itemId TEXT,
                        itemName TEXT,
                        quantity INTEGER,
                        linkedType TEXT,
                        linkedId INTEGER,
                        completed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        itemId TEXT NOT NULL,
                        itemName TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        checked INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_listId ON shopping_list_items(listId)")
            }
        }

        fun get(context: Context): SpyglassDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpyglassDatabase::class.java,
                    "spyglass.db",
                )
                    .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
