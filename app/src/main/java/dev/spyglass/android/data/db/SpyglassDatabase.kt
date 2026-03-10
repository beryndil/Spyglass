package dev.spyglass.android.data.db

import android.content.Context
import android.os.Trace
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
        CommandEntity::class,
        VersionTagEntity::class,
        TranslationEntity::class,
    ],
    version = 30,
    exportSchema = true,
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
    abstract fun commandDao():      CommandDao
    abstract fun versionTagDao():   VersionTagDao
    abstract fun translationDao():  TranslationDao

    companion object {
        @Volatile private var INSTANCE: SpyglassDatabase? = null

        // Migration 29→30: add translations table for i18n overlay
        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS translations (
                        locale TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        field TEXT NOT NULL,
                        value TEXT NOT NULL,
                        PRIMARY KEY (locale, entityType, entityId, field)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_translations_locale_entityType_entityId ON translations(locale, entityType, entityId)")
            }
        }

        // Migration 28→29: add tutorial and difficulty columns to advancements
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE advancements ADD COLUMN tutorial TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE advancements ADD COLUMN difficulty TEXT NOT NULL DEFAULT ''")
            }
        }

        // Migration 27→28: add indexes on frequently queried columns
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_blocks_name ON blocks(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_blocks_category ON blocks(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_outputItem ON recipes(outputItem)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_type ON recipes(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mobs_name ON mobs(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mobs_category ON mobs(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_biomes_name ON biomes(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_enchants_name ON enchants(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_potions_name ON potions(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trades_profession ON trades(profession)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_structures_name ON structures(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_advancements_name ON advancements(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_advancements_category ON advancements(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_name ON items(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_category ON items(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_commands_name ON commands(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_commands_category ON commands(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_version_tags_entityType ON version_tags(entityType)")
            }
        }

        // Migration 26→27: drop user data tables (moved to spyglass_user.db)
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS notes")
                db.execSQL("DROP TABLE IF EXISTS waypoints")
                db.execSQL("DROP TABLE IF EXISTS favorites")
                db.execSQL("DROP TABLE IF EXISTS shopping_list_items")
                db.execSQL("DROP TABLE IF EXISTS shopping_lists")
                db.execSQL("DROP TABLE IF EXISTS todos")
                db.execSQL("DROP TABLE IF EXISTS advancement_progress")
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE version_tags ADD COLUMN mechanicsChangedInJava TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE version_tags ADD COLUMN mechanicsChangedInBedrock TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE version_tags ADD COLUMN mechanicsChangeNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS version_tags (
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        addedInJava TEXT NOT NULL DEFAULT '',
                        removedInJava TEXT NOT NULL DEFAULT '',
                        addedInBedrock TEXT NOT NULL DEFAULT '',
                        removedInBedrock TEXT NOT NULL DEFAULT '',
                        javaOnly INTEGER NOT NULL DEFAULT 0,
                        bedrockOnly INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (entityType, entityId)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE biomes ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE biomes ADD COLUMN buildingPalette TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE mobs ADD COLUMN attackDamage TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trades ADD COLUMN maxUses INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE mobs ADD COLUMN spawnConditions TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blocks ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blocks ADD COLUMN isObtainable INTEGER NOT NULL DEFAULT 1")
            }
        }

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
                // advancement_progress stays in old DB for migration chain;
                // it will be dropped in 26→27 after data is moved to UserDatabase.
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
                INSTANCE ?: run {
                    Trace.beginSection("SpyglassDatabase.build")
                    try {
                        Room.databaseBuilder(
                            context.applicationContext,
                            SpyglassDatabase::class.java,
                            "spyglass.db",
                        )
                            .addMigrations(
                                MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                                MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
                                MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23,
                                MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26,
                                MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29,
                                MIGRATION_29_30,
                            )
                            // Game data can always be rebuilt from bundled assets or sync.
                            // This ensures the app never crashes due to a missing migration.
                            .fallbackToDestructiveMigration()
                            .build()
                    } finally {
                        Trace.endSection()
                    }
                }.also { INSTANCE = it }
            }
    }
}
