package dev.spyglass.android.data.db.daos

import androidx.room.*
import dev.spyglass.android.data.db.entities.ShoppingListEntity
import dev.spyglass.android.data.db.entities.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    // ── Lists ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC")
    fun allLists(): Flow<List<ShoppingListEntity>>

    @Insert
    suspend fun insertList(list: ShoppingListEntity): Long

    @Query("UPDATE shopping_lists SET name = :name WHERE id = :listId")
    suspend fun renameList(listId: Long, name: String)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM shopping_list_items")
    suspend fun deleteAllItems()

    // ── Items ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId ORDER BY id ASC")
    fun itemsForList(listId: Long): Flow<List<ShoppingListItemEntity>>

    @Insert
    suspend fun insertItem(item: ShoppingListItemEntity)

    @Query("UPDATE shopping_list_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateItemQuantity(id: Long, quantity: Int)

    @Query("UPDATE shopping_list_items SET checked = :checked WHERE id = :id")
    suspend fun setItemChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId")
    suspend fun clearList(listId: Long)

    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId AND itemId = :itemId LIMIT 1")
    suspend fun findItem(listId: Long, itemId: String): ShoppingListItemEntity?
}
