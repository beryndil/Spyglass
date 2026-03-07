package dev.spyglass.android.data.db

import android.content.ContentValues
import android.content.Context
import android.os.Trace
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.spyglass.android.data.db.daos.*
import dev.spyglass.android.data.db.entities.*
import timber.log.Timber

/**
 * Separate database for user-created data (notes, waypoints, favorites, etc.).
 * Isolating user data guarantees it is never affected by game data migrations,
 * re-seeding, or schema changes.
 */
@Database(
    entities = [
        NoteEntity::class,
        WaypointEntity::class,
        FavoriteEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        TodoEntity::class,
        AdvancementProgressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun waypointDao(): WaypointDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun todoDao(): TodoDao
    abstract fun advancementProgressDao(): AdvancementProgressDao

    companion object {
        @Volatile private var INSTANCE: UserDatabase? = null

        fun get(context: Context): UserDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    Trace.beginSection("UserDatabase.build")
                    try {
                        buildDatabase(context.applicationContext)
                    } finally {
                        Trace.endSection()
                    }
                }.also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): UserDatabase {
            return Room.databaseBuilder(
                context,
                UserDatabase::class.java,
                "spyglass_user.db",
            )
                .addCallback(MigrateFromOldDbCallback(context))
                .build()
        }

        /**
         * Called when spyglass_user.db is first created. Copies any existing
         * user data from the old spyglass.db so nothing is lost.
         */
        private class MigrateFromOldDbCallback(
            private val context: Context,
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                migrateUserData(context, db)
            }
        }

        private fun migrateUserData(context: Context, userDb: SupportSQLiteDatabase) {
            val oldDbFile = context.getDatabasePath("spyglass.db")
            if (!oldDbFile.exists()) return

            try {
                val oldDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                    oldDbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
                )

                migrateTable(oldDb, userDb, "notes",
                    "id", "title", "label", "content", "createdAt", "updatedAt")
                migrateTable(oldDb, userDb, "waypoints",
                    "id", "name", "x", "y", "z", "dimension", "category", "color", "notes", "createdAt")
                migrateTable(oldDb, userDb, "favorites",
                    "id", "type", "displayName", "addedAt")
                migrateTable(oldDb, userDb, "shopping_lists",
                    "id", "name", "createdAt")
                migrateTable(oldDb, userDb, "shopping_list_items",
                    "id", "listId", "itemId", "itemName", "quantity", "checked")
                migrateTable(oldDb, userDb, "todos",
                    "id", "title", "itemId", "itemName", "quantity", "linkedType", "linkedId",
                    "completed", "createdAt", "completedAt")
                migrateTable(oldDb, userDb, "advancement_progress",
                    "advancementId", "completed", "completedAt")

                oldDb.close()
                Timber.d("User data migrated from spyglass.db to spyglass_user.db")
            } catch (e: Exception) {
                Timber.e(e, "Failed to migrate user data from old database")
            }
        }

        private fun migrateTable(
            oldDb: android.database.sqlite.SQLiteDatabase,
            userDb: SupportSQLiteDatabase,
            tableName: String,
            vararg columns: String,
        ) {
            try {
                val check = oldDb.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    arrayOf(tableName),
                )
                val exists = check.moveToFirst()
                check.close()
                if (!exists) return

                val colList = columns.joinToString()
                val cursor = oldDb.rawQuery("SELECT $colList FROM $tableName", null)
                var count = 0

                while (cursor.moveToNext()) {
                    val values = ContentValues()
                    columns.forEachIndexed { i, col ->
                        when (cursor.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> values.putNull(col)
                            android.database.Cursor.FIELD_TYPE_INTEGER -> values.put(col, cursor.getLong(i))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> values.put(col, cursor.getDouble(i))
                            android.database.Cursor.FIELD_TYPE_STRING -> values.put(col, cursor.getString(i))
                            android.database.Cursor.FIELD_TYPE_BLOB -> values.put(col, cursor.getBlob(i))
                        }
                    }
                    userDb.insert(tableName, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values)
                    count++
                }
                cursor.close()
                if (count > 0) Timber.d("Migrated %d rows from %s", count, tableName)
            } catch (e: Exception) {
                Timber.w(e, "Failed to migrate table %s", tableName)
            }
        }
    }
}
