package dev.spyglass.android.connect.waypoints

import kotlinx.serialization.Serializable

@Serializable
data class ConnectWaypoint(
    val id: String,
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val dimension: String = "overworld",
    val category: String = "other",
    val color: String = "gold",
    val notes: String = "",
    val source: String = "custom",
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val ID_WORLD_SPAWN = "world_spawn"
        const val ID_RESPAWN = "respawn"
        const val ID_DEATH = "death"

        const val SOURCE_AUTO = "auto"
        const val SOURCE_CUSTOM = "custom"

        val CATEGORIES = listOf("spawn", "death", "base", "farm", "portal", "spawner", "village", "monument", "other")
        val COLORS = listOf("gold", "green", "red", "blue", "purple")
        val DIMENSIONS = listOf("overworld", "the_nether", "the_end")
    }
}
