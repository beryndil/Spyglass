package dev.spyglass.android.data.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tracks per-table version numbers plus textures and news versions.
 *
 * Version format: `YYMMDDHHMM` (Long) — e.g. `2603021430` = March 2, 2026 at 14:30.
 * Old integer versions (e.g. `18`) remain valid; they simply compare as older.
 */
@Serializable
data class DataManifest(
    val version: Long = 0,
    val blocks: Long = 0,
    val mobs: Long = 0,
    val biomes: Long = 0,
    val enchants: Long = 0,
    val potions: Long = 0,
    val trades: Long = 0,
    val recipes: Long = 0,
    val structures: Long = 0,
    val items: Long = 0,
    val advancements: Long = 0,
    val commands: Long = 0,
    val textures: Long = 0,
    val news: Long = 0,
) {
    /** The effective version is the highest per-table version. */
    val effectiveVersion: Long
        get() = maxOf(blocks, mobs, biomes, enchants, potions, trades, recipes, structures, items, advancements, commands)

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
    fun withVersion(table: String, version: Long): DataManifest = when (table) {
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
    fun versionOf(table: String): Long = when (table) {
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

    /** True when remote textures version is higher than local. */
    fun hasTextureUpdate(local: DataManifest): Boolean = textures > local.textures

    /** True when remote news version is higher than local. */
    fun hasNewsUpdate(local: DataManifest): Boolean = news > local.news

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(raw: String): DataManifest = json.decodeFromString(raw)

        fun toJson(manifest: DataManifest): String =
            Json.encodeToString(serializer(), manifest)
    }
}
