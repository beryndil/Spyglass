package dev.spyglass.android.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocks",
    indices = [Index(value = ["name"]), Index(value = ["category"])],
)
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
    val blastResistance: Float = 0f,
    val lightLevel: Int = 0,
    val hasGravity: Boolean = false,
    val isWaterloggable: Boolean = false,
    val minY: Int? = null,
    val maxY: Int? = null,
    val peakY: Int? = null,
    val description: String = "",
    val isObtainable: Boolean = true,
)

@Entity(
    tableName = "recipes",
    indices = [Index(value = ["outputItem"]), Index(value = ["type"])],
)
data class RecipeEntity(
    @PrimaryKey val id: String,
    val outputItem: String,
    val outputCount: Int = 1,
    val type: String = "crafting_shaped", // shaped, shapeless, smelting, stonecutting, etc.
    val ingredientsJson: String = "[]",  // JSON array of ingredient item IDs (with nulls for gaps)
    val shapedGrid: String = "",         // "3x3" or "2x2" grid pattern as 9 or 4 comma-sep items
    val xp: Float = 0f,                  // XP reward per smelt (smelting recipes only)
)

@Entity(
    tableName = "mobs",
    indices = [Index(value = ["name"]), Index(value = ["category"])],
)
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
    val spawnConditions: String = "", // e.g. "Light level 0-7, solid block"
    val attackDamage: String = "",   // "3" or "2-6" for variable damage
)

@Entity(
    tableName = "biomes",
    indices = [Index(value = ["name"])],
)
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
    val description: String = "",
    val buildingPalette: String = "",   // comma-sep block IDs
)

@Entity(
    tableName = "enchants",
    indices = [Index(value = ["name"])],
)
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

@Entity(
    tableName = "potions",
    indices = [Index(value = ["name"])],
)
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

@Entity(
    tableName = "trades",
    indices = [Index(value = ["profession"])],
)
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
    val maxUses: Int = 0,           // times available per restock
)

@Entity(
    tableName = "structures",
    indices = [Index(value = ["name"])],
)
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

@Entity(
    tableName = "advancements",
    indices = [Index(value = ["name"]), Index(value = ["category"])],
)
data class AdvancementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val category: String = "",      // "minecraft", "nether", "end", "adventure", "husbandry"
    val type: String = "task",      // "task", "goal", "challenge"
    val parent: String = "",
    val hint: String = "",              // Practical how-to guidance
    val requirements: String = "",      // Exact trigger conditions
    val relatedItems: String = "",      // Comma-sep item IDs
    val relatedMobs: String = "",       // Comma-sep mob IDs
    val relatedStructures: String = "", // Comma-sep structure IDs
    val relatedBiomes: String = "",     // Comma-sep biome IDs
    val dimension: String = "",         // "overworld", "nether", "end"
    val xpReward: String = "",          // XP reward info
)

@Entity(tableName = "advancement_progress")
data class AdvancementProgressEntity(
    @PrimaryKey val advancementId: String,
    val completed: Boolean = false,
    val completedAt: Long? = null,
)

@Entity(
    tableName = "items",
    indices = [Index(value = ["name"]), Index(value = ["category"])],
)
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
    val attackDamage: String = "",       // "7" for diamond sword
    val attackSpeed: String = "",        // "1.6" for swords
    val enchantability: Int = 0,         // enchanting quality
    val hunger: Int = 0,                 // half-shanks (food only)
    val saturation: Float = 0f,          // saturation points (food only)
    val foodEffect: String = "",         // "Poison (0:05)"
    val defensePoints: Int = 0,          // armor points
    val armorToughness: Float = 0f,      // 2.0 for diamond
    val knockbackResistance: Float = 0f, // 0.1 for netherite
    val isRenewable: Boolean = true,     // most items are renewable
    val enchantTarget: String = "",      // "sword", "armor,boots" — matches enchant target field
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val label: String = "",         // user-defined label/tag
    val content: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val dimension: String = "overworld", // overworld, nether, end
    val category: String = "base",       // base, farm, portal, spawner, village, monument, other
    val color: String = "gold",          // gold, green, red, blue, purple
    val notes: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "commands",
    indices = [Index(value = ["name"]), Index(value = ["category"])],
)
data class CommandEntity(
    @PrimaryKey val id: String,
    val name: String,
    val syntax: String = "",
    val description: String = "",
    val category: String = "",      // chat, player, entity, world, server, operator, debug
    val permissionLevel: Int = 2,
)

@Entity(
    tableName = "version_tags",
    primaryKeys = ["entityType", "entityId"],
    indices = [Index(value = ["entityType"])],
)
data class VersionTagEntity(
    val entityType: String,          // "block", "item", "mob", etc.
    val entityId: String,            // matches PK of target table
    val addedInJava: String = "",    // "1.13" — empty = since 1.0
    val removedInJava: String = "",  // empty = still present
    val addedInBedrock: String = "",
    val removedInBedrock: String = "",
    val javaOnly: Boolean = false,
    val bedrockOnly: Boolean = false,
    val mechanicsChangedInJava: String = "",
    val mechanicsChangedInBedrock: String = "",
    val mechanicsChangeNotes: String = "",
)
