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
)

enum class ItemType { SWORD, HELMET, CHESTPLATE, LEGGINGS, BOOTS, ALL }

data class SelectedEnchant(val enchant: Enchantment, val level: Int)

data class AnvilStep(val desc: String, val cost: Int, val tooExpensive: Boolean)

data class AnvilState(
    val selectedItem: ItemType = ItemType.SWORD,
    val pickedEnchants: List<SelectedEnchant> = emptyList(),
    val steps: List<AnvilStep> = emptyList(),
    val totalCost: Int = 0,
)

class AnvilViewModel : ViewModel() {

    val enchantments: List<Enchantment> = buildList {
        // Universal
        add(Enchantment("protection",         "Protection",         4, 1, 1, setOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)))
        add(Enchantment("fire_protection",    "Fire Protection",    4, 1, 1, setOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)))
        add(Enchantment("blast_protection",   "Blast Protection",   4, 2, 2, setOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)))
        add(Enchantment("projectile_protection","Projectile Prot.", 4, 1, 1, setOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)))
        add(Enchantment("thorns",             "Thorns",             3, 4, 4, setOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)))
        add(Enchantment("unbreaking",         "Unbreaking",         3, 1, 1, ItemType.entries.toSet()))
        add(Enchantment("mending",            "Mending",            1, 2, 2, ItemType.entries.toSet()))
        add(Enchantment("curse_of_vanishing", "Curse of Vanishing", 1, 1, 1, ItemType.entries.toSet()))
        add(Enchantment("curse_of_binding",   "Curse of Binding",   1, 4, 4, setOf(ItemType.HELMET, ItemType.CHESTPLATE, ItemType.LEGGINGS, ItemType.BOOTS)))
        // Helmet
        add(Enchantment("respiration",        "Respiration",        3, 2, 2, setOf(ItemType.HELMET)))
        add(Enchantment("aqua_affinity",      "Aqua Affinity",      1, 2, 2, setOf(ItemType.HELMET)))
        // Leggings
        add(Enchantment("swift_sneak",        "Swift Sneak",        3, 4, 4, setOf(ItemType.LEGGINGS)))
        // Boots
        add(Enchantment("feather_falling",    "Feather Falling",    4, 1, 1, setOf(ItemType.BOOTS)))
        add(Enchantment("depth_strider",      "Depth Strider",      3, 2, 2, setOf(ItemType.BOOTS)))
        add(Enchantment("frost_walker",       "Frost Walker",       2, 2, 2, setOf(ItemType.BOOTS)))
        add(Enchantment("soul_speed",         "Soul Speed",         3, 5, 5, setOf(ItemType.BOOTS)))
        // Sword
        add(Enchantment("sharpness",          "Sharpness",          5, 1, 1, setOf(ItemType.SWORD)))
        add(Enchantment("smite",              "Smite",              5, 2, 2, setOf(ItemType.SWORD)))
        add(Enchantment("bane_of_arthropods", "Bane of Arthropods", 5, 2, 2, setOf(ItemType.SWORD)))
        add(Enchantment("knockback",          "Knockback",          2, 1, 1, setOf(ItemType.SWORD)))
        add(Enchantment("fire_aspect",        "Fire Aspect",        2, 2, 2, setOf(ItemType.SWORD)))
        add(Enchantment("looting",            "Looting",            3, 2, 2, setOf(ItemType.SWORD)))
        add(Enchantment("sweeping_edge",      "Sweeping Edge",      3, 2, 2, setOf(ItemType.SWORD)))
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
        if (enchants.isEmpty()) { _state.value = _state.value.copy(steps = emptyList(), totalCost = 0); return }

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

        _state.value = _state.value.copy(steps = steps, totalCost = total)
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
        return enchantments.filter { t in it.targets || ItemType.ALL in it.targets }
    }
}
