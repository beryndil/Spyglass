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
        AdvancementProgressEntity::class,
        NoteEntity::class,
        WaypointEntity::class,
        CommandEntity::class,
        FavoriteEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        TodoEntity::class,
    ],
    version = 22,
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
    abstract fun advancementProgressDao(): AdvancementProgressDao
    abstract fun noteDao():         NoteDao
    abstract fun waypointDao():     WaypointDao
    abstract fun commandDao():      CommandDao
    abstract fun favoriteDao():     FavoriteDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun todoDao():         TodoDao

    companion object {
        @Volatile private var INSTANCE: SpyglassDatabase? = null

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocks ADD COLUMN minY INTEGER")
                db.execSQL("ALTER TABLE blocks ADD COLUMN maxY INTEGER")
                db.execSQL("ALTER TABLE blocks ADD COLUMN peakY INTEGER")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN xp REAL NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Items: 11 new columns
                db.execSQL("ALTER TABLE items ADD COLUMN attackDamage TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE items ADD COLUMN attackSpeed TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE items ADD COLUMN enchantability INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN hunger INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN saturation REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN foodEffect TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE items ADD COLUMN defensePoints INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN armorToughness REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN knockbackResistance REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN isRenewable INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE items ADD COLUMN enchantTarget TEXT NOT NULL DEFAULT ''")
                // Blocks: 4 new columns
                db.execSQL("ALTER TABLE blocks ADD COLUMN blastResistance REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocks ADD COLUMN lightLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocks ADD COLUMN hasGravity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE blocks ADD COLUMN isWaterloggable INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE advancements ADD COLUMN hint TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE advancements ADD COLUMN requirements TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE advancements ADD COLUMN relatedItems TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE advancements ADD COLUMN relatedMobs TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE advancements ADD COLUMN relatedStructures TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE advancements ADD COLUMN relatedBiomes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE advancements ADD COLUMN dimension TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE advancements ADD COLUMN xpReward TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS advancement_progress (
                        advancementId TEXT NOT NULL PRIMARY KEY,
                        completed INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        label TEXT NOT NULL DEFAULT '',
                        content TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS waypoints (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        dimension TEXT NOT NULL DEFAULT 'overworld',
                        category TEXT NOT NULL DEFAULT 'base',
                        color TEXT NOT NULL DEFAULT 'gold',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS commands (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        syntax TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT '',
                        permissionLevel INTEGER NOT NULL DEFAULT 2
                    )
                """.trimIndent())
            }
        }

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
                    .addMigrations(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
