package dev.spyglass.android.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["category", "query"], unique = true)],
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)
