package dev.spyglass.android.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tracks per-table version numbers plus textures and news versions.
 *
 * Version format: `ZodiacName.MMDD.HHmm` — e.g. `FireHorse.0302.1430` = March 2, 2026 at 14:30.
 * Zodiac names map to years: FireHorse=2026, FireGoat=2027, etc.
 */
@Serializable
data class DataManifest(
    val version: String = "",
    val blocks: String = "",
    val mobs: String = "",
    val biomes: String = "",
    val enchants: String = "",
    val potions: String = "",
    val trades: String = "",
    val recipes: String = "",
    val structures: String = "",
    val items: String = "",
    val advancements: String = "",
    val commands: String = "",
    @SerialName("version_tags") val versionTags: String = "",
    val textures: String = "",
    @SerialName("texture_map") val textureMap: String = "",
    val news: String = "",
    val tips: String = "",
    @SerialName("latest_app") val latestApp: String = "",
    /** Optional SHA-256 checksums per file. Empty map when server doesn't provide them. */
    val checksums: Map<String, String> = emptyMap(),
) {
    /** The effective version is the highest per-table version. */
    val effectiveVersion: String
        get() = listOf(blocks, mobs, biomes, enchants, potions, trades, recipes, structures, items, advancements, commands, versionTags)
            .maxWithOrNull { a, b -> compareVersions(a, b) } ?: ""

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
        if (versionTags != other.versionTags) add("version_tags")
    }

    /** Returns a copy with the version for [table] updated to [version]. */
    fun withVersion(table: String, version: String): DataManifest = when (table) {
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
        "version_tags" -> copy(versionTags = version)
        else -> this
    }

    /** Gets the version string for a specific [table]. */
    fun versionOf(table: String): String = when (table) {
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
        "version_tags" -> versionTags
        else -> ""
    }

    /** True when remote textures version is higher than local. */
    fun hasTextureUpdate(local: DataManifest): Boolean = compareVersions(textures, local.textures) > 0

    /** True when remote texture_map version is higher than local. */
    fun hasTextureMapUpdate(local: DataManifest): Boolean = compareVersions(textureMap, local.textureMap) > 0

    /** True when remote news version is higher than local. */
    fun hasNewsUpdate(local: DataManifest): Boolean = compareVersions(news, local.news) > 0

    /** True when remote tips version is higher than local. */
    fun hasTipsUpdate(local: DataManifest): Boolean = compareVersions(tips, local.tips) > 0

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private val ZODIAC_YEARS = mapOf(
            "WoodDragon" to 2024, "WoodSnake" to 2025,
            "FireHorse" to 2026, "FireGoat" to 2027,
            "EarthMonkey" to 2028, "EarthRooster" to 2029,
            "MetalDog" to 2030, "MetalPig" to 2031,
            "WaterRat" to 2032, "WaterOx" to 2033,
            "WoodTiger" to 2034, "WoodRabbit" to 2035,
        )

        /** Compares two zodiac version strings. Returns negative, zero, or positive. */
        fun compareVersions(a: String, b: String): Int {
            if (a == b) return 0
            if (a.isEmpty()) return -1
            if (b.isEmpty()) return 1
            return versionToCode(a).compareTo(versionToCode(b))
        }

        private fun versionToCode(v: String): Long {
            val parts = v.split(".", limit = 3)
            if (parts.size != 3) return 0
            val year = ZODIAC_YEARS[parts[0]] ?: return 0
            val mo = parts[1].substring(0, 2).toIntOrNull() ?: return 0
            val dd = parts[1].substring(2, 4).toIntOrNull() ?: return 0
            val hh = parts[2].substring(0, 2).toIntOrNull() ?: return 0
            val mi = parts[2].substring(2, 4).toIntOrNull() ?: return 0
            return (year - 2000).toLong() * 10_000_000 + mo * 1_000_000 + dd * 10_000 + hh * 100 + mi
        }

        fun fromJson(raw: String): DataManifest = json.decodeFromString(raw)

        fun toJson(manifest: DataManifest): String =
            Json.encodeToString(serializer(), manifest)
    }
}
