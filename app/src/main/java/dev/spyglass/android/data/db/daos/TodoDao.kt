package dev.spyglass.android.data.db.daos

import androidx.room.*
import dev.spyglass.android.data.db.entities.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY completed ASC, createdAt DESC")
    fun all(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE completed = 0 ORDER BY createdAt DESC")
    fun incomplete(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE completed = 0 ORDER BY createdAt DESC LIMIT :limit")
    fun incompletePreview(limit: Int): Flow<List<TodoEntity>>

    @Query("SELECT COUNT(*) FROM todos WHERE completed = 0")
    fun incompleteCount(): Flow<Int>

    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Query("UPDATE todos SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE todos SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE todos SET linkedType = :linkedType, linkedId = :linkedId WHERE id = :id")
    suspend fun setLink(id: Long, linkedType: String?, linkedId: Long?)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM todos WHERE completed = 1")
    suspend fun deleteCompleted()

    @Query("SELECT * FROM todos WHERE linkedType = :linkedType AND linkedId = :linkedId ORDER BY completed ASC, createdAt DESC")
    fun findByLink(linkedType: String, linkedId: Long): Flow<List<TodoEntity>>
}
