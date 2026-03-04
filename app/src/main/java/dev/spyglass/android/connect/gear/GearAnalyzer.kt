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
    val missingEnchants: List<EnchantEntity>,
    val upgradeableEnchants: List<EnchantInfo>,
    val tierUpgrade: ItemEntity?,
)

data class EnchantInfo(
    val id: String,
    val name: String,
    val level: Int,
    val maxLevel: Int,
)

object GearAnalyzer {

    private val json = Json { ignoreUnknownKeys = true }

    // Material tier hierarchies
    private val ARMOR_TIERS = listOf("leather", "chainmail", "iron", "diamond", "netherite")
    private val TOOL_TIERS = listOf("wooden", "stone", "iron", "diamond", "netherite")

    private val ARMOR_SLOT_SUFFIXES = mapOf(
        SlotType.HEAD to "helmet",
        SlotType.CHEST to "chestplate",
        SlotType.LEGS to "leggings",
        SlotType.FEET to "boots",
    )

    private val TOOL_SUFFIXES = listOf("sword", "pickaxe", "axe", "shovel", "hoe")

    private val DEFAULT_SUGGESTIONS = mapOf(
        SlotType.HEAD to "diamond_helmet",
        SlotType.CHEST to "diamond_chestplate",
        SlotType.LEGS to "diamond_leggings",
        SlotType.FEET to "diamond_boots",
        SlotType.MAIN_HAND to "diamond_sword",
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
            // Empty slot — suggest default
            val suggestedId = DEFAULT_SUGGESTIONS[slotType]
            val suggestedItem = suggestedId?.let { repo.itemById(it) }
            return SlotAnalysis(
                slotType = slotType,
                item = null,
                itemEntity = null,
                currentEnchants = emptyList(),
                missingEnchants = emptyList(),
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
        val missingEnchants = mutableListOf<EnchantEntity>()

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

        // Find missing (applicable but absent, excluding curses and incompatible)
        for (enchant in applicableEnchants) {
            if (enchant.id in presentIds) continue
            if (enchant.isCurse) continue
            if (enchant.id in incompatibleIds) continue
            missingEnchants.add(enchant)
        }

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

    private suspend fun findNextTier(
        itemId: String,
        suffix: String,
        tiers: List<String>,
        repo: GameDataRepository,
    ): ItemEntity? {
        val currentTier = tiers.indexOfFirst { itemId == "${it}_$suffix" }
        if (currentTier < 0 || currentTier >= tiers.lastIndex) return null

        // Try the next tier up
        for (i in (currentTier + 1)..tiers.lastIndex) {
            val nextId = "${tiers[i]}_$suffix"
            val nextItem = repo.itemById(nextId)
            if (nextItem != null) return nextItem
        }
        return null
    }
}
