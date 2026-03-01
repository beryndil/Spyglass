package dev.spyglass.android.data.seed

import android.content.Context
import android.os.Trace
import dev.spyglass.android.data.db.SpyglassDatabase
import dev.spyglass.android.data.db.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads JSON files from assets/minecraft/ and inserts into Room.
 * Uses a version number to trigger full re-seeding when data changes.
 * Favorites are preserved across re-seeds (separate table, not cleared).
 */
object DataSeeder {

    private const val CURRENT_DATA_VERSION = 16
    private const val PREFS_NAME = "spyglass_seed"
    private const val KEY_DATA_VERSION = "data_version"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        Trace.beginSection("DataSeeder.seedIfNeeded")
        try {
            val db = SpyglassDatabase.get(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val storedVersion = prefs.getInt(KEY_DATA_VERSION, 0)

            if (storedVersion < CURRENT_DATA_VERSION) {
                Trace.beginSection("DataSeeder.clearTables")
                try {
                    // Clear game data tables (NOT favorites)
                    db.blockDao().deleteAll()
                    db.mobDao().deleteAll()
                    db.biomeDao().deleteAll()
                    db.enchantDao().deleteAll()
                    db.potionDao().deleteAll()
                    db.tradeDao().deleteAll()
                    db.recipeDao().deleteAll()
                    db.structureDao().deleteAll()
                    db.itemDao().deleteAll()
                    db.advancementDao().deleteAll()
                    db.commandDao().deleteAll()
                } finally {
                    Trace.endSection()
                }
            }

            // Seed any empty tables
            if (db.blockDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.blocks")
                try { seedBlocks(context, db) } finally { Trace.endSection() }
            }
            if (db.mobDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.mobs")
                try { seedMobs(context, db) } finally { Trace.endSection() }
            }
            if (db.biomeDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.biomes")
                try { seedBiomes(context, db) } finally { Trace.endSection() }
            }
            if (db.enchantDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.enchants")
                try { seedEnchants(context, db) } finally { Trace.endSection() }
            }
            if (db.potionDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.potions")
                try { seedPotions(context, db) } finally { Trace.endSection() }
            }
            if (db.tradeDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.trades")
                try { seedTrades(context, db) } finally { Trace.endSection() }
            }
            if (db.recipeDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.recipes")
                try { seedRecipes(context, db) } finally { Trace.endSection() }
            }
            if (db.structureDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.structures")
                try { seedStructures(context, db) } finally { Trace.endSection() }
            }
            if (db.itemDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.items")
                try { seedItems(context, db) } finally { Trace.endSection() }
            }
            if (db.advancementDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.advancements")
                try { seedAdvancements(context, db) } finally { Trace.endSection() }
            }
            if (db.commandDao().count() == 0) {
                Trace.beginSection("DataSeeder.seed.commands")
                try { seedCommands(context, db) } finally { Trace.endSection() }
            }

            if (storedVersion < CURRENT_DATA_VERSION) {
                prefs.edit().putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
            }
        } finally {
            Trace.endSection()
        }
    }

    private fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().readText()

    // ── JSON DTOs ─────────────────────────────────────────────────────────────

    @Serializable data class BlockJson(
        val id: String, val name: String, val stackSize: Int = 64,
        val hardness: Float = 1.5f, val toolRequired: String = "",
        val toolLevel: String = "", val isFlammable: Boolean = false,
        val isTransparent: Boolean = false, val drops: String = "",
        val category: String = "",
        val blastResistance: Float = 0f, val lightLevel: Int = 0,
        val hasGravity: Boolean = false, val isWaterloggable: Boolean = false,
        val minY: Int? = null, val maxY: Int? = null, val peakY: Int? = null,
    )

    @Serializable data class MobJson(
        val id: String, val name: String,
        val health: kotlinx.serialization.json.JsonElement = kotlinx.serialization.json.JsonPrimitive(20),
        val category: String = "hostile", val hostility: String = "",
        val spawnBiomes: String = "",
        val drops: String = "",
        val xp: kotlinx.serialization.json.JsonElement = kotlinx.serialization.json.JsonPrimitive(0),
        val isFireImmune: Boolean = false, val description: String = "",
        val breeding: String = "",
    )

    @Serializable data class BiomeJson(
        val id: String, val name: String, val temperature: Float = 0.5f,
        val precipitation: String = "rain", val category: String = "",
        val color: String = "", val structures: String = "",
        val mobs: String = "[]", val features: String = "",
    )

    @Serializable data class EnchantJson(
        val id: String, val name: String, val maxLevel: Int = 1,
        val target: String = "all", val incompatible: List<String> = emptyList(),
        val description: String = "", val rarity: String = "common",
        val isTreasure: Boolean = false, val isCurse: Boolean = false,
    )

    @Serializable data class PotionJson(
        val id: String, val name: String, val effect: String = "",
        val duration: Int = 0, val amplifier: Int = 0,
        val category: String = "positive", val ingredientPath: String = "",
        val color: String = "",
    )

    @Serializable data class TradeJson(
        val id: String = "", val profession: String, val level: Int,
        val buyItem1: String, val buyItem1Count: Int,
        val buyItem2: String = "", val buyItem2Count: Int = 0,
        val sellItem: String, val sellCount: Int = 1,
        val quantity: Int = 0,
    )

    @Serializable data class RecipeJson(
        val id: String, val outputItem: String, val outputCount: Int = 1,
        val type: String = "crafting_shaped",
        val ingredientsJson: String = "[]",
        val xp: Float = 0f,
    )

    @Serializable data class StructureJson(
        val id: String, val name: String, val dimension: String = "overworld",
        val difficulty: String = "", val description: String = "",
        val biomes: String = "", val mobs: String = "", val loot: String = "",
        val uniqueBlocks: String = "", val findMethod: String = "",
    )

    @Serializable data class ItemJson(
        val id: String, val name: String, val stackSize: Int = 64,
        val category: String = "", val durability: Int = 0,
        val description: String = "", val obtainedFrom: String = "",
        val droppedBy: String = "", val minedFrom: String = "",
        val attackDamage: String = "", val attackSpeed: String = "",
        val enchantability: Int = 0, val hunger: Int = 0,
        val saturation: Float = 0f, val foodEffect: String = "",
        val defensePoints: Int = 0, val armorToughness: Float = 0f,
        val knockbackResistance: Float = 0f, val isRenewable: Boolean = true,
        val enchantTarget: String = "",
    )

    @Serializable data class AdvancementJson(
        val id: String, val name: String, val description: String = "",
        val category: String = "", val type: String = "task",
        val parent: String = "",
        val hint: String = "", val requirements: String = "",
        val relatedItems: String = "", val relatedMobs: String = "",
        val relatedStructures: String = "", val relatedBiomes: String = "",
        val dimension: String = "", val xpReward: String = "",
    )

    @Serializable data class CommandJson(
        val id: String, val name: String, val syntax: String = "",
        val description: String = "", val category: String = "",
        val permissionLevel: Int = 2,
    )

    // ── Seed helpers ──────────────────────────────────────────────────────────

    private suspend fun seedBlocks(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/blocks.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<BlockJson>>(raw)
        db.blockDao().insertAll(items.map {
            BlockEntity(it.id, it.name, it.stackSize, it.hardness, it.toolRequired,
                it.toolLevel, it.isFlammable, it.isTransparent, it.drops, it.category,
                it.blastResistance, it.lightLevel, it.hasGravity, it.isWaterloggable,
                it.minY, it.maxY, it.peakY)
        })
    }

    private suspend fun seedMobs(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/mobs.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<MobJson>>(raw)
        db.mobDao().insertAll(items.map {
            val healthStr = it.health.toString().trim('"')
            val xpStr = it.xp.toString().trim('"')
            val biomesList = if (it.spawnBiomes.isBlank()) "[]"
                else "[${it.spawnBiomes.split(",").joinToString(",") { s -> "\"${s.trim()}\""}}]"
            MobEntity(it.id, it.name, healthStr, it.hostility.ifEmpty { it.category },
                biomesList, it.drops, xpStr, it.isFireImmune, it.description, it.breeding)
        })
    }

    private suspend fun seedBiomes(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/biomes.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<BiomeJson>>(raw)
        db.biomeDao().insertAll(items.map {
            BiomeEntity(it.id, it.name, it.temperature, it.precipitation, it.category,
                it.color, it.structures, it.mobs, it.features)
        })
    }

    private suspend fun seedEnchants(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/enchants.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<EnchantJson>>(raw)
        db.enchantDao().insertAll(items.map {
            EnchantEntity(it.id, it.name, it.maxLevel, it.target,
                "[${it.incompatible.joinToString(",") { s -> "\"$s\""}}]",
                it.description, it.rarity, it.isTreasure, it.isCurse)
        })
    }

    private suspend fun seedPotions(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/potions.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<PotionJson>>(raw)
        db.potionDao().insertAll(items.map {
            PotionEntity(it.id, it.name, it.effect, it.duration, it.amplifier,
                it.category, it.ingredientPath, it.color)
        })
    }

    private val levelNames = mapOf(1 to "Novice", 2 to "Apprentice", 3 to "Journeyman", 4 to "Expert", 5 to "Master")

    private suspend fun seedTrades(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/trades.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<TradeJson>>(raw)
        db.tradeDao().insertAll(items.map {
            TradeEntity(0, it.profession, it.level, levelNames[it.level] ?: "Novice",
                it.buyItem1, it.buyItem1Count, it.buyItem2, it.buyItem2Count,
                it.sellItem, it.sellCount)
        })
    }

    private suspend fun seedRecipes(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/recipes.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<RecipeJson>>(raw)
        db.recipeDao().insertAll(items.map {
            RecipeEntity(it.id, it.outputItem, it.outputCount, it.type,
                it.ingredientsJson, "", it.xp)
        })
    }

    private suspend fun seedStructures(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/structures.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<StructureJson>>(raw)
        db.structureDao().insertAll(items.map {
            StructureEntity(it.id, it.name, it.dimension, it.difficulty,
                it.description, it.biomes, it.mobs, it.loot, it.uniqueBlocks, it.findMethod)
        })
    }

    private suspend fun seedItems(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/items.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<ItemJson>>(raw)
        db.itemDao().insertAll(items.map {
            ItemEntity(it.id, it.name, it.stackSize, it.category, it.durability,
                it.description, it.obtainedFrom, it.droppedBy, it.minedFrom,
                it.attackDamage, it.attackSpeed, it.enchantability, it.hunger,
                it.saturation, it.foodEffect, it.defensePoints, it.armorToughness,
                it.knockbackResistance, it.isRenewable, it.enchantTarget)
        })
    }

    private suspend fun seedAdvancements(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/advancements.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<AdvancementJson>>(raw)
        db.advancementDao().insertAll(items.map {
            AdvancementEntity(it.id, it.name, it.description, it.category, it.type, it.parent,
                it.hint, it.requirements, it.relatedItems, it.relatedMobs,
                it.relatedStructures, it.relatedBiomes, it.dimension, it.xpReward)
        })
    }

    private suspend fun seedCommands(context: Context, db: SpyglassDatabase) {
        val raw = runCatching { readAsset(context, "minecraft/commands.json") }.getOrNull() ?: return
        val items = json.decodeFromString<List<CommandJson>>(raw)
        db.commandDao().insertAll(items.map {
            CommandEntity(it.id, it.name, it.syntax, it.description, it.category, it.permissionLevel)
        })
    }
}
