package dev.spyglass.android.connect.gear

import dev.spyglass.android.connect.ItemStack
import dev.spyglass.android.connect.PlayerData
import dev.spyglass.android.data.db.entities.EnchantEntity
import dev.spyglass.android.data.db.entities.ItemEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

enum class SlotType { HEAD, CHEST, LEGS, FEET, MAIN_HAND, OFF_HAND }

data class GearAnalysis(val slots: List<SlotAnalysis>)

data class SlotAnalysis(
    val slotType: SlotType,
    val item: ItemStack?,
    val itemEntity: ItemEntity?,
    val currentEnchants: List<EnchantInfo>,
    val missingEnchants: List<EnchantRecommendation>,
    val upgradeableEnchants: List<EnchantInfo>,
    val tierUpgrade: ItemEntity?,
)

data class EnchantInfo(
    val id: String,
    val name: String,
    val level: Int,
    val maxLevel: Int,
)

/** A group of enchant recommendations. Size 1 = standalone; 2+ = mutually exclusive (pick one). */
data class EnchantRecommendation(
    val enchants: List<EnchantEntity>,
)

object GearAnalyzer {

    private val json = Json { ignoreUnknownKeys = true }

    // Material tier hierarchies
    // Chainmail skipped — too difficult to obtain (mob drops only, no crafting recipe)
    private val ARMOR_TIERS = listOf("leather", "copper", "iron", "diamond", "netherite")
    private val TOOL_TIERS = listOf("wooden", "stone", "iron", "diamond", "netherite")

    private val ARMOR_SLOT_SUFFIXES = mapOf(
        SlotType.HEAD to "helmet",
        SlotType.CHEST to "chestplate",
        SlotType.LEGS to "leggings",
        SlotType.FEET to "boots",
    )

    private val TOOL_SUFFIXES = listOf("sword", "pickaxe", "axe", "shovel", "hoe")

    private val DEFAULT_SUGGESTIONS = mapOf(
        SlotType.HEAD to "leather_helmet",
        SlotType.CHEST to "leather_chestplate",
        SlotType.LEGS to "leather_leggings",
        SlotType.FEET to "leather_boots",
        SlotType.MAIN_HAND to "wooden_sword",
        SlotType.OFF_HAND to "shield",
    )

    suspend fun analyze(playerData: PlayerData, repo: GameDataRepository): GearAnalysis {
        val slots = SlotType.entries.map { slotType ->
            val item = resolveSlotItem(slotType, playerData)
            analyzeSlot(slotType, item, repo)
        }
        return GearAnalysis(slots)
    }

    private fun resolveSlotItem(slotType: SlotType, playerData: PlayerData): ItemStack? {
        return when (slotType) {
            SlotType.HEAD -> playerData.armor.firstOrNull { it.slot == 103 }
                ?: playerData.inventory.firstOrNull { it.slot == 103 }
            SlotType.CHEST -> playerData.armor.firstOrNull { it.slot == 102 }
                ?: playerData.inventory.firstOrNull { it.slot == 102 }
            SlotType.LEGS -> playerData.armor.firstOrNull { it.slot == 101 }
                ?: playerData.inventory.firstOrNull { it.slot == 101 }
            SlotType.FEET -> playerData.armor.firstOrNull { it.slot == 100 }
                ?: playerData.inventory.firstOrNull { it.slot == 100 }
            SlotType.OFF_HAND -> playerData.offhand
            SlotType.MAIN_HAND -> playerData.inventory.firstOrNull { it.slot == playerData.selectedSlot }
        }
    }

    private suspend fun analyzeSlot(
        slotType: SlotType,
        item: ItemStack?,
        repo: GameDataRepository,
    ): SlotAnalysis {
        if (item == null) {
            // Empty slot — suggest first-tier gear + all applicable enchants
            val suggestedId = DEFAULT_SUGGESTIONS[slotType]
            val suggestedItem = suggestedId?.let { repo.itemById(it) }
            val recommendations = if (suggestedItem != null) {
                val applicable = collectApplicableEnchants(suggestedItem, repo)
                buildRecommendations(applicable)
            } else emptyList()
            return SlotAnalysis(
                slotType = slotType,
                item = null,
                itemEntity = null,
                currentEnchants = emptyList(),
                missingEnchants = recommendations,
                upgradeableEnchants = emptyList(),
                tierUpgrade = suggestedItem,
            )
        }

        val itemEntity = repo.itemById(item.id)
        val applicableEnchants = collectApplicableEnchants(itemEntity, repo)
        val presentIds = item.enchantments.map { it.id }.toSet()

        // Build enchant entity lookup for present enchants
        val enchantEntityMap = mutableMapOf<String, EnchantEntity>()
        for (enchant in applicableEnchants) {
            enchantEntityMap[enchant.id] = enchant
        }

        // Parse incompatible sets for present enchants
        val incompatibleIds = mutableSetOf<String>()
        for (present in item.enchantments) {
            val entity = enchantEntityMap[present.id]
            if (entity != null) {
                incompatibleIds.addAll(parseIncompatible(entity.incompatibleJson))
            }
        }

        val currentEnchants = mutableListOf<EnchantInfo>()
        val upgradeableEnchants = mutableListOf<EnchantInfo>()

        // Classify present enchants
        for (enchData in item.enchantments) {
            val entity = enchantEntityMap[enchData.id]
            val maxLevel = entity?.maxLevel ?: enchData.level
            val name = entity?.name ?: enchData.id.replace("_", " ")
                .replaceFirstChar { it.uppercase() }
            val info = EnchantInfo(enchData.id, name, enchData.level, maxLevel)

            if (enchData.level >= maxLevel) {
                currentEnchants.add(info)
            } else {
                upgradeableEnchants.add(info)
            }
        }

        // Build grouped recommendations for missing enchants
        val missingEnchants = buildRecommendations(applicableEnchants, presentIds, incompatibleIds)

        val tierUpgrade = findTierUpgrade(item.id, slotType, repo)

        return SlotAnalysis(
            slotType = slotType,
            item = item,
            itemEntity = itemEntity,
            currentEnchants = currentEnchants,
            missingEnchants = missingEnchants,
            upgradeableEnchants = upgradeableEnchants,
            tierUpgrade = tierUpgrade,
        )
    }

    private suspend fun collectApplicableEnchants(
        itemEntity: ItemEntity?,
        repo: GameDataRepository,
    ): List<EnchantEntity> {
        val targets = itemEntity?.enchantTarget?.split(",")?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: return emptyList()

        val allEnchants = mutableMapOf<String, EnchantEntity>()
        for (target in targets) {
            val enchants = repo.enchantsForTargetOnce(target)
            for (e in enchants) {
                allEnchants[e.id] = e
            }
        }
        return allEnchants.values.toList()
    }

    private fun parseIncompatible(incompatibleJson: String): List<String> {
        return try {
            json.parseToJsonElement(incompatibleJson).jsonArray
                .map { it.jsonPrimitive.content }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val RARITY_ORDER = mapOf(
        "common" to 0,
        "uncommon" to 1,
        "rare" to 2,
        "very_rare" to 3,
    )

    /**
     * Build grouped enchant recommendations from applicable enchants.
     * Groups mutually exclusive enchants together (e.g., Protection / Fire Prot / Blast Prot).
     * Sorted by importance: common rarity first, non-treasure before treasure.
     */
    private fun buildRecommendations(
        applicable: List<EnchantEntity>,
        presentIds: Set<String> = emptySet(),
        incompatibleWithPresent: Set<String> = emptySet(),
    ): List<EnchantRecommendation> {
        val candidates = applicable.filter {
            !it.isCurse && it.id !in presentIds && it.id !in incompatibleWithPresent
        }

        val used = mutableSetOf<String>()
        val groups = mutableListOf<EnchantRecommendation>()

        // Sort: common (most essential) first, non-treasure before treasure, then by name
        val sorted = candidates.sortedWith(compareBy(
            { RARITY_ORDER[it.rarity] ?: 99 },
            { if (it.isTreasure) 1 else 0 },
            { it.name },
        ))

        for (enchant in sorted) {
            if (enchant.id in used) continue
            used.add(enchant.id)

            // Find mutually exclusive alternatives
            val incompatible = parseIncompatible(enchant.incompatibleJson)
            val alts = incompatible
                .mapNotNull { altId -> candidates.find { it.id == altId } }
                .filter { it.id !in used }
                .sortedBy { it.name }

            alts.forEach { used.add(it.id) }

            groups.add(EnchantRecommendation(
                enchants = listOf(enchant) + alts,
            ))
        }

        return groups
    }

    private suspend fun findTierUpgrade(
        itemId: String,
        slotType: SlotType,
        repo: GameDataRepository,
    ): ItemEntity? {
        // Determine if armor or tool
        val armorSuffix = ARMOR_SLOT_SUFFIXES[slotType]
        if (armorSuffix != null) {
            return findNextTier(itemId, armorSuffix, ARMOR_TIERS, repo)
        }

        // For main_hand/off_hand, try tool suffixes
        for (suffix in TOOL_SUFFIXES) {
            if (itemId.endsWith("_$suffix")) {
                return findNextTier(itemId, suffix, TOOL_TIERS, repo)
            }
        }
        return null
    }

    // Chainmail maps to copper tier for upgrade purposes (both upgrade to iron)
    private val TIER_ALIASES = mapOf("chainmail" to "copper")

    private suspend fun findNextTier(
        itemId: String,
        suffix: String,
        tiers: List<String>,
        repo: GameDataRepository,
    ): ItemEntity? {
        var currentTier = tiers.indexOfFirst { itemId == "${it}_$suffix" }
        // Check aliases (e.g. chainmail → copper tier)
        if (currentTier < 0) {
            val prefix = itemId.removeSuffix("_$suffix")
            val alias = TIER_ALIASES[prefix]
            if (alias != null) currentTier = tiers.indexOf(alias)
        }
        if (currentTier < 0 || currentTier >= tiers.lastIndex) return null

        val nextId = "${tiers[currentTier + 1]}_$suffix"
        return repo.itemById(nextId)
    }
}
