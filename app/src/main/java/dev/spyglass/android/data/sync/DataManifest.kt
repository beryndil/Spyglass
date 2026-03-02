package dev.spyglass.android.data.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DataManifest(
    val version: Int = 0,
    val blocks: Int = 0,
    val mobs: Int = 0,
    val biomes: Int = 0,
    val enchants: Int = 0,
    val potions: Int = 0,
    val trades: Int = 0,
    val recipes: Int = 0,
    val structures: Int = 0,
    val items: Int = 0,
    val advancements: Int = 0,
    val commands: Int = 0,
) {
    /** Returns the list of table names whose version differs between [this] and [other]. */
    fun changedTables(other: DataManifest): List<String> = buildList {
        if (blocks != other.blocks) add("blocks")
        if (mobs != other.mobs) add("mobs")
        if (biomes != other.biomes) add("biomes")
        if (enchants != other.enchants) add("enchants")
        if (potions != other.potions) add("potions")
        if (trades != other.trades) add("trades")
        if (recipes != other.recipes) add("recipes")
        if (structures != other.structures) add("structures")
        if (items != other.items) add("items")
        if (advancements != other.advancements) add("advancements")
        if (commands != other.commands) add("commands")
    }

    /** Returns a copy with the version for [table] updated to [version]. */
    fun withVersion(table: String, version: Int): DataManifest = when (table) {
        "blocks" -> copy(blocks = version)
        "mobs" -> copy(mobs = version)
        "biomes" -> copy(biomes = version)
        "enchants" -> copy(enchants = version)
        "potions" -> copy(potions = version)
        "trades" -> copy(trades = version)
        "recipes" -> copy(recipes = version)
        "structures" -> copy(structures = version)
        "items" -> copy(items = version)
        "advancements" -> copy(advancements = version)
        "commands" -> copy(commands = version)
        else -> this
    }

    /** Gets the version number for a specific [table]. */
    fun versionOf(table: String): Int = when (table) {
        "blocks" -> blocks
        "mobs" -> mobs
        "biomes" -> biomes
        "enchants" -> enchants
        "potions" -> potions
        "trades" -> trades
        "recipes" -> recipes
        "structures" -> structures
        "items" -> items
        "advancements" -> advancements
        "commands" -> commands
        else -> 0
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(raw: String): DataManifest = json.decodeFromString(raw)

        fun toJson(manifest: DataManifest): String =
            Json.encodeToString(serializer(), manifest)
    }
}
