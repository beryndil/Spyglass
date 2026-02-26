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

    fun toggleEnchant(e: Enchantment, level: Int = e.maxLevel) {
        val current = _state.value.pickedEnchants
        val existing = current.indexOfFirst { it.enchant.id == e.id }
        val updated = if (existing >= 0) current.toMutableList().also { it.removeAt(existing) }
                      else current + SelectedEnchant(e, level)
        _state.value = _state.value.copy(pickedEnchants = updated)
        recalc()
    }

    fun setEnchantLevel(id: String, level: Int) {
        _state.value = _state.value.copy(
            pickedEnchants = _state.value.pickedEnchants.map { if (it.enchant.id == id) it.copy(level = level) else it }
        )
        recalc()
    }

    private fun recalc() {
        val enchants = _state.value.pickedEnchants
        if (enchants.isEmpty()) {
            _state.value = _state.value.copy(steps = emptyList(), totalCost = 0, warnings = emptyList())
            return
        }

        // Check incompatibilities
        val warnings = mutableListOf<String>()
        val pickedIds = enchants.map { it.enchant.id }.toSet()
        for (se in enchants) {
            for (incompId in se.enchant.incompatible) {
                if (incompId in pickedIds) {
                    val other = enchants.first { it.enchant.id == incompId }
                    val msg = "${se.enchant.name} conflicts with ${other.enchant.name}"
                    if (warnings.none { it == msg || it == "${other.enchant.name} conflicts with ${se.enchant.name}" }) {
                        warnings.add(msg)
                    }
                }
            }
        }

        // Compute individual book costs, sort descending
        val books = enchants.map { se ->
            val cost = se.enchant.bookCost * se.level
            Pair(se, cost)
        }.sortedByDescending { it.second }

        val steps = mutableListOf<AnvilStep>()
        var total = 0

        if (books.size == 1) {
            val cost = books[0].second + books[0].second
            steps.add(AnvilStep("Apply ${books[0].first.enchant.name} to item", cost, cost >= 40))
            total = cost
        } else {
            // Combine books in pairs (highest + lowest), then apply final merged book
            val combined = combinePairs(books.map { it.first.enchant.name to it.second })
            combined.forEachIndexed { i, (desc, cost) ->
                steps.add(AnvilStep(desc, cost, cost >= 40))
                total += cost
            }
        }

        _state.value = _state.value.copy(steps = steps, totalCost = total, warnings = warnings)
    }

    private fun combinePairs(items: List<Pair<String, Int>>): List<Pair<String, Int>> {
        if (items.size == 1) return listOf("Apply ${items[0].first} to item" to items[0].second * 2)
        val result = mutableListOf<Pair<String, Int>>()
        val queue = items.toMutableList()
        while (queue.size > 1) {
            val a = queue.removeFirst()
            val b = queue.removeLast()
            val cost = a.second + b.second
            val merged = "${a.first} + ${b.first}"
            result.add("Combine $merged" to cost)
            queue.add(0, merged to cost)
        }
        // Final application to item
        val last = queue[0]
        result.add("Apply ${last.first} to item" to last.second)
        return result
    }

    fun enchantsForCurrentItem(): List<Enchantment> {
        val t = _state.value.selectedItem
        return enchantments.filter { t in it.targets }
    }
}
