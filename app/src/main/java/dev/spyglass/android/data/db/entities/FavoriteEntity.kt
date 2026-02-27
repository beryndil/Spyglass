package dev.spyglass.android.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,       // e.g. "minecraft:stone" or "zombie"
    val type: String,                 // "block", "item", "recipe", "mob", "biome", "structure", "enchant", "potion", "trade"
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis(),
)
