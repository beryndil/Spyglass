package dev.spyglass.android.calculators.anvil

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ── Data ─────────────────────────────────────────────────────────────────────

data class Enchantment(
    val id: String,
    val name: String,
    val maxLevel: Int,
    val bookCost: Int,          // multiplier on the book side
    val itemCost: Int,          // multiplier on the item side (often same)
    val targets: Set<ItemType>,
    val incompatible: Set<String> = emptySet(),
)

enum class ItemType { SWORD, PICKAXE, AXE, SHOVEL, HOE, BOW, CROSSBOW, TRIDENT, FISHING_ROD, MACE, HELMET, CHESTPLATE, LEGGINGS, BOOTS }

data class SelectedEnchant(val enchant: Enchantment, val level: Int)

data class AnvilStep(val desc: String, val cost: Int, val tooExpensive: Boolean)

data class AnvilState(
    val selectedItem: ItemType = ItemType.SWORD,
    val pickedEnchants: List<SelectedEnchant> = emptyList(),
    val steps: List<AnvilStep> = emptyList(),
    val totalCost: Int = 0,
    val warnings: List<String> = emptyList(),
)

private val ARMOR = setOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)
private val TOOLS = setOf(ItemType.PICKAXE, ItemType.AXE, ItemType.SHOVEL, ItemType.HOE)
private val MELEE = setOf(ItemType.SWORD, ItemType.AXE)
private val ALL = ItemType.entries.toSet()

class AnvilViewModel : ViewModel() {

    val enchantments: List<Enchantment> = buildList {
        // Universal
        add(Enchantment("unbreaking",         "Unbreaking",         3, 1, 1, ALL))
        add(Enchantment("mending",            "Mending",            1, 2, 2, ALL, setOf("infinity")))
        add(Enchantment("curse_of_vanishing", "Curse of Vanishing", 1, 1, 1, ALL))

        // Armor (all pieces)
        add(Enchantment("protection",           "Protection",           4, 1, 1, ARMOR, setOf("fire_protection", "blast_protection", "projectile_protection")))
        add(Enchantment("fire_protection",      "Fire Protection",      4, 1, 1, ARMOR, setOf("protection", "blast_protection", "projectile_protection")))
        add(Enchantment("blast_protection",     "Blast Protection",     4, 2, 2, ARMOR, setOf("protection", "fire_protection", "projectile_protection")))
        add(Enchantment("projectile_protection","Projectile Prot.",     4, 1, 1, ARMOR, setOf("protection", "fire_protection", "blast_protection")))
        add(Enchantment("thorns",               "Thorns",               3, 4, 4, ARMOR))
        add(Enchantment("curse_of_binding",     "Curse of Binding",     1, 4, 4, ARMOR))

        // Helmet
        add(Enchantment("respiration",   "Respiration",   3, 2, 2, setOf(ItemType.HELMET)))
        add(Enchantment("aqua_affinity", "Aqua Affinity", 1, 2, 2, setOf(ItemType.HELMET)))

        // Leggings
        add(Enchantment("swift_sneak", "Swift Sneak", 3, 4, 4, setOf(ItemType.LEGGINGS)))

        // Boots
        add(Enchantment("feather_falling", "Feather Falling", 4, 1, 1, setOf(ItemType.BOOTS)))
        add(Enchantment("depth_strider",   "Depth Strider",   3, 2, 2, setOf(ItemType.BOOTS), setOf("frost_walker")))
        add(Enchantment("frost_walker",    "Frost Walker",    2, 2, 2, setOf(ItemType.BOOTS), setOf("depth_strider")))
        add(Enchantment("soul_speed",      "Soul Speed",      3, 5, 5, setOf(ItemType.BOOTS)))

        // Sword + Axe (melee)
        add(Enchantment("sharpness",          "Sharpness",          5, 1, 1, MELEE, setOf("smite", "bane_of_arthropods")))
        add(Enchantment("smite",              "Smite",              5, 2, 2, MELEE, setOf("sharpness", "bane_of_arthropods")))
        add(Enchantment("bane_of_arthropods", "Bane of Arthropods", 5, 2, 2, MELEE, setOf("sharpness", "smite")))

        // Sword only
        add(Enchantment("knockback",     "Knockback",     2, 1, 1, setOf(ItemType.SWORD)))
        add(Enchantment("fire_aspect",   "Fire Aspect",   2, 2, 2, setOf(ItemType.SWORD)))
        add(Enchantment("looting",       "Looting",       3, 2, 2, setOf(ItemType.SWORD)))
        add(Enchantment("sweeping_edge", "Sweeping Edge", 3, 2, 2, setOf(ItemType.SWORD)))

        // Tools (pickaxe, shovel, axe, hoe)
        add(Enchantment("efficiency", "Efficiency", 5, 1, 1, TOOLS + setOf(ItemType.AXE)))
        add(Enchantment("silk_touch", "Silk Touch",  1, 4, 4, TOOLS, setOf("fortune")))
        add(Enchantment("fortune",    "Fortune",     3, 2, 2, TOOLS, setOf("silk_touch")))

        // Bow
        add(Enchantment("power",    "Power",    5, 1, 1, setOf(ItemType.BOW)))
        add(Enchantment("punch",    "Punch",    2, 2, 2, setOf(ItemType.BOW)))
        add(Enchantment("flame",    "Flame",    1, 2, 2, setOf(ItemType.BOW)))
        add(Enchantment("infinity", "Infinity", 1, 4, 4, setOf(ItemType.BOW), setOf("mending")))

        // Crossbow
        add(Enchantment("multishot",    "Multishot",    1, 2, 2, setOf(ItemType.CROSSBOW), setOf("piercing")))
        add(Enchantment("quick_charge", "Quick Charge", 3, 1, 1, setOf(ItemType.CROSSBOW)))
        add(Enchantment("piercing",     "Piercing",     4, 1, 1, setOf(ItemType.CROSSBOW), setOf("multishot")))

        // Trident
        add(Enchantment("loyalty",    "Loyalty",    3, 1, 1, setOf(ItemType.TRIDENT), setOf("riptide")))
        add(Enchantment("impaling",   "Impaling",   5, 2, 2, setOf(ItemType.TRIDENT)))
        add(Enchantment("riptide",    "Riptide",    3, 2, 2, setOf(ItemType.TRIDENT), setOf("loyalty", "channeling")))
        add(Enchantment("channeling", "Channeling", 1, 4, 4, setOf(ItemType.TRIDENT), setOf("riptide")))

        // Fishing rod
        add(Enchantment("luck_of_the_sea", "Luck of the Sea", 3, 2, 2, setOf(ItemType.FISHING_ROD)))
        add(Enchantment("lure",            "Lure",            3, 2, 2, setOf(ItemType.FISHING_ROD)))

        // Mace
        add(Enchantment("wind_burst", "Wind Burst", 3, 4, 4, setOf(ItemType.MACE)))
        add(Enchantment("density",    "Density",    5, 1, 1, setOf(ItemType.MACE), setOf("breach")))
        add(Enchantment("breach",     "Breach",     4, 2, 2, setOf(ItemType.MACE), setOf("density")))
    }

    private val _state = MutableStateFlow(AnvilState())
    val state: StateFlow<AnvilState> = _state.asStateFlow()

    fun setItem(t: ItemType) {
        _state.value = AnvilState(selectedItem = t)
    }

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()
    fun clearWarning() { _warningMessage.value = null }

    fun toggleEnchant(e: Enchantment, level: Int = e.maxLevel) {
        val current = _state.value.pickedEnchants
        val existing = current.indexOfFirst { it.enchant.id == e.id }
        if (existing >= 0) {
            // Deselecting — always allowed
            val updated = current.toMutableList().also { it.removeAt(existing) }
            _state.value = _state.value.copy(pickedEnchants = updated)
            recalc()
            return
        }
        // Check incompatibility before selecting
        val pickedIds = current.map { it.enchant.id }.toSet()
        for (incompId in e.incompatible) {
            if (incompId in pickedIds) {
                val conflict = current.first { it.enchant.id == incompId }
                _warningMessage.value = "${e.name} is incompatible with ${conflict.enchant.name}"
                return
            }
        }
        _state.value = _state.value.copy(pickedEnchants = current + SelectedEnchant(e, level))
        recalc()
    }

    fun isIncompatible(e: Enchantment): Boolean {
        val pickedIds = _state.value.pickedEnchants.map { it.enchant.id }.toSet()
        return e.incompatible.any { it in pickedIds }
    }

    fun setEnchantLevel(id: String, level: Int) {
        _state.value = _state.value.copy(
            pickedEnchants = _state.value.pickedEnchants.map { if (it.enchant.id == id) it.copy(level = level) else it }
        )
        recalc()
    }

    /**
     * Optimal enchanting order using binary tree combining with prior work penalties.
     *
     * Minecraft anvil mechanics:
     * - Each item/book tracks "anvil uses" (starts at 0 for fresh items)
     * - Prior work penalty = 2^uses - 1 (0, 1, 3, 7, 15, 31...)
     * - Anvil cost per operation = enchant costs from sacrifice + target penalty + sacrifice penalty
     * - Output uses = max(target_uses, sacrifice_uses) + 1
     * - "Too Expensive" if a single operation costs >= 40 levels
     *
     * Optimal strategy: sort books by cost ascending, combine in balanced binary tree
     * so cheapest books accumulate more prior work (their base cost is small).
     */
    private fun recalc() {
        val enchants = _state.value.pickedEnchants
        if (enchants.isEmpty()) {
            _state.value = _state.value.copy(steps = emptyList(), totalCost = 0, warnings = emptyList())
            return
        }

        // Each node in the combining tree
        data class BookNode(
            val label: String,
            val enchantCost: Int,   // sum of enchant level costs on this combined book
            val anvilUses: Int,     // number of times combined
        ) {
            val priorWork get() = (1 shl anvilUses) - 1   // 2^uses - 1
        }

        // Sort books by cost ascending — cheapest combined first
        val sortedBooks = enchants
            .map { se -> BookNode(se.enchant.name, se.enchant.bookCost * se.level, 0) }
            .sortedBy { it.enchantCost }

        val steps = mutableListOf<AnvilStep>()
        var total = 0

        if (sortedBooks.size == 1) {
            // Single enchant: apply book directly to item
            val book = sortedBooks[0]
            // Item has 0 prior work; book has 0 prior work
            val cost = book.enchantCost + 0 + 0
            steps.add(AnvilStep("Apply ${book.label} to item", cost, cost >= 40))
            total = cost
        } else {
            // Build balanced binary tree: combine adjacent pairs each round
            var queue = sortedBooks.toMutableList()

            // Phase 1: Combine books into one merged book
            while (queue.size > 1) {
                val nextQueue = mutableListOf<BookNode>()
                var i = 0
                while (i < queue.size) {
                    if (i + 1 < queue.size) {
                        val target = queue[i]
                        val sacrifice = queue[i + 1]
                        val cost = sacrifice.enchantCost + target.priorWork + sacrifice.priorWork
                        val merged = BookNode(
                            label = "${target.label} + ${sacrifice.label}",
                            enchantCost = target.enchantCost + sacrifice.enchantCost,
                            anvilUses = maxOf(target.anvilUses, sacrifice.anvilUses) + 1,
                        )
                        steps.add(AnvilStep("Combine ${target.label} + ${sacrifice.label}", cost, cost >= 40))
                        total += cost
                        nextQueue.add(merged)
                        i += 2
                    } else {
                        // Odd one out — carry forward
                        nextQueue.add(queue[i])
                        i++
                    }
                }
                queue = nextQueue
            }

            // Phase 2: Apply final merged book to the item
            val finalBook = queue[0]
            // Item starts with 0 prior work, 0 anvil uses
            val applyCost = finalBook.enchantCost + 0 + finalBook.priorWork
            steps.add(AnvilStep("Apply ${finalBook.label} to item", applyCost, applyCost >= 40))
            total += applyCost
        }

        _state.value = _state.value.copy(steps = steps, totalCost = total, warnings = emptyList())
    }

    fun enchantsForCurrentItem(): List<Enchantment> {
        val t = _state.value.selectedItem
        return enchantments.filter { t in it.targets }
    }
}
