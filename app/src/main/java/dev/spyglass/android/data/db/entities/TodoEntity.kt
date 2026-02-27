package dev.spyglass.android.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val itemId: String? = null,
    val itemName: String? = null,
    val quantity: Int? = null,
    val linkedType: String? = null,
    val linkedId: Long? = null,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)
