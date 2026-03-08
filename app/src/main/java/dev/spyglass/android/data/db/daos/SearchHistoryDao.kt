package dev.spyglass.android.data.db.daos

import androidx.room.*
import dev.spyglass.android.data.db.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE category = :category ORDER BY searchedAt DESC LIMIT 20")
    fun recentByCategory(category: String): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE category = :category AND query LIKE :prefix || '%' ORDER BY searchedAt DESC LIMIT 10")
    fun matchingByCategory(category: String, prefix: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM search_history WHERE category = :category")
    suspend fun clearCategory(category: String)

    @Query("DELETE FROM search_history WHERE category = :category AND id NOT IN (SELECT id FROM search_history WHERE category = :category ORDER BY searchedAt DESC LIMIT 20)")
    suspend fun pruneCategory(category: String)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}
