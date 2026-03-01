package dev.spyglass.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.spyglass.android.data.db.SpyglassDatabase
import dev.spyglass.android.data.db.entities.TodoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpyglassDatabaseTest {

    private lateinit var db: SpyglassDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SpyglassDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun database_creates_successfully() {
        assertNotNull(db)
        assertNotNull(db.blockDao())
        assertNotNull(db.recipeDao())
        assertNotNull(db.mobDao())
        assertNotNull(db.biomeDao())
        assertNotNull(db.enchantDao())
        assertNotNull(db.potionDao())
        assertNotNull(db.tradeDao())
        assertNotNull(db.structureDao())
        assertNotNull(db.itemDao())
        assertNotNull(db.advancementDao())
        assertNotNull(db.advancementProgressDao())
        assertNotNull(db.noteDao())
        assertNotNull(db.waypointDao())
        assertNotNull(db.commandDao())
        assertNotNull(db.favoriteDao())
        assertNotNull(db.shoppingListDao())
        assertNotNull(db.todoDao())
    }

    @Test
    fun todoDao_insert_and_query() = runBlocking {
        val dao = db.todoDao()

        val todo = TodoEntity(
            id = 0,
            title = "Mine diamonds",
            itemId = "minecraft:diamond",
            itemName = "Diamond",
            quantity = 64,
            linkedType = null,
            linkedId = null,
            completed = false,
            createdAt = System.currentTimeMillis(),
            completedAt = null,
        )

        dao.insert(todo)
        val all = dao.allTodos().first()
        assertEquals(1, all.size)
        assertEquals("Mine diamonds", all[0].title)
        assertFalse(all[0].completed)
    }

    @Test
    fun todoDao_delete_all() = runBlocking {
        val dao = db.todoDao()

        dao.insert(TodoEntity(0, "Task 1", null, null, null, null, null, false, System.currentTimeMillis(), null))
        dao.insert(TodoEntity(0, "Task 2", null, null, null, null, null, false, System.currentTimeMillis(), null))

        val before = dao.allTodos().first()
        assertEquals(2, before.size)

        dao.deleteAll()

        val after = dao.allTodos().first()
        assertTrue(after.isEmpty())
    }

    @Test
    fun favoriteDao_insert_and_query() = runBlocking {
        val dao = db.favoriteDao()
        val fav = dev.spyglass.android.data.db.entities.FavoriteEntity(
            id = "minecraft:diamond_sword",
            type = "item",
            displayName = "Diamond Sword",
        )
        dao.insert(fav)
        val all = dao.allFavorites().first()
        assertEquals(1, all.size)
        assertEquals("Diamond Sword", all[0].displayName)
    }
}
