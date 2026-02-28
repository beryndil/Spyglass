package dev.spyglass.android.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey val id: String,          // e.g. "minecraft:stone"
    val name: String,
    val stackSize: Int      = 64,
    val hardness: Float     = 1.5f,
    val toolRequired: String = "",       // "pickaxe", "axe", "shovel", ""
    val toolLevel: String   = "",        // "stone", "iron", "diamond", ""
    val isFlammable: Boolean = false,
    val isTransparent: Boolean = false,
    val drops: String       = "",        // comma-separated item IDs
    val category: String    = "",        // "building", "natural", "redstone", etc.
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val outputItem: String,
    val outputCount: Int = 1,
    val type: String = "crafting_shaped", // shaped, shapeless, smelting, stonecutting, etc.
    val ingredientsJson: String = "[]",  // JSON array of ingredient item IDs (with nulls for gaps)
    val shapedGrid: String = "",         // "3x3" or "2x2" grid pattern as 9 or 4 comma-sep items
)

@Entity(tableName = "mobs")
data class MobEntity(
    @PrimaryKey val id: String,
    val name: String,
    val health: String,             // "20" or "1-16" for size-based mobs
    val category: String,           // "hostile", "neutral", "passive", "boss"
    val spawnBiomesJson: String = "[]",
    val dropsJson: String = "[]",   // JSON: [{id, name, minCount, maxCount, chance}]
    val xpDrop: String = "0",       // "5" or "1-4" for size-based mobs
    val isFireImmune: Boolean = false,
    val description: String = "",
    val breeding: String = "",      // e.g. "wheat" or "carrots,potatoes,beetroot" or ""
)

@Entity(tableName = "biomes")
data class BiomeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val temperature: Float = 0.5f,
    val precipitation: String = "rain", // "none", "rain", "snow"
    val category: String = "",          // "forest", "ocean", "desert", etc.
    val color: String = "",             // hex e.g. "#56821e"
    val structures: String = "",        // comma-sep structure IDs
    val mobsJson: String = "[]",
    val features: String = "",
)

@Entity(tableName = "enchants")
data class EnchantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val maxLevel: Int,
    val target: String,             // "armor", "sword", "tool", "bow", "all", etc.
    val incompatibleJson: String = "[]",
    val description: String = "",
    val rarity: String = "common",  // "common", "uncommon", "rare", "very_rare"
    val isTreasure: Boolean = false,
    val isCurse: Boolean = false,
)

@Entity(tableName = "potions")
data class PotionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val effect: String,
    val durationSeconds: Int = 0,
    val amplifier: Int = 0,         // 0 = level I
    val category: String = "",      // "positive", "negative", "special"
    val ingredientPath: String = "", // brewing chain: "nether_wart → base → ..."
    val color: String = "",
)

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val profession: String,
    val level: Int,                 // 1=Novice .. 5=Master
    val levelName: String,
    val buyItem1: String,
    val buyItem1Count: Int,
    val buyItem2: String = "",      // second buy slot (often emeralds)
    val buyItem2Count: Int = 0,
    val sellItem: String,
    val sellItemCount: Int,
)

@Entity(tableName = "structures")
data class StructureEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dimension: String,
    val difficulty: String = "",
    val description: String = "",
    val biomes: String = "",        // comma-separated biome IDs
    val mobs: String = "",          // comma-separated mob IDs
    val loot: String = "",          // comma-separated item IDs
    val uniqueBlocks: String = "",  // comma-separated block IDs
    val findMethod: String = "",
)

@Entity(tableName = "advancements")
data class AdvancementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val category: String = "",      // "minecraft", "nether", "end", "adventure", "husbandry"
    val type: String = "task",      // "task", "goal", "challenge"
    val parent: String = "",
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stackSize: Int = 64,
    val category: String = "",      // tools, weapons, armor, food, materials, mob_drops, brewing, misc
    val durability: Int = 0,
    val description: String = "",
    val obtainedFrom: String = "",  // comma-sep: crafting, mob_drop, mining, trading, fishing, structure_loot, farming, smelting, bartering, found
    val droppedBy: String = "",     // comma-sep mob IDs
    val minedFrom: String = "",     // comma-sep block IDs
)
