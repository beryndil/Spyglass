package dev.spyglass.android.core.ui

import dev.spyglass.android.R

/**
 * Texture lookup for enchantments using the item each enchantment
 * is most commonly associated with.
 */
object EnchantTextures {
    private val map = mapOf(
        // Armor — protection family
        "protection" to R.drawable.item_iron_chestplate,
        "fire_protection" to R.drawable.item_golden_chestplate,
        "blast_protection" to R.drawable.item_diamond_chestplate,
        "projectile_protection" to R.drawable.item_shield,
        "thorns" to R.drawable.item_netherite_chestplate,
        "binding_curse" to R.drawable.item_iron_chestplate,

        // Helmet
        "respiration" to R.drawable.item_iron_helmet,
        "aqua_affinity" to R.drawable.item_diamond_helmet,

        // Boots
        "feather_falling" to R.drawable.item_leather_boots,
        "depth_strider" to R.drawable.item_diamond_boots,
        "frost_walker" to R.drawable.item_diamond_boots,
        "soul_speed" to R.drawable.item_golden_boots,

        // Leggings
        "swift_sneak" to R.drawable.item_leather_leggings,

        // Sword
        "sharpness" to R.drawable.item_diamond_sword,
        "smite" to R.drawable.item_golden_sword,
        "bane_of_arthropods" to R.drawable.item_iron_sword,
        "knockback" to R.drawable.item_iron_sword,
        "fire_aspect" to R.drawable.item_fire_charge,
        "looting" to R.drawable.item_diamond_sword,
        "sweeping_edge" to R.drawable.item_netherite_sword,

        // Tool
        "efficiency" to R.drawable.item_diamond_pickaxe,
        "silk_touch" to R.drawable.item_diamond_pickaxe,
        "unbreaking" to R.drawable.item_diamond_pickaxe,
        "fortune" to R.drawable.item_diamond_pickaxe,

        // Bow
        "power" to R.drawable.item_bow,
        "punch" to R.drawable.item_bow,
        "flame" to R.drawable.item_bow,
        "infinity" to R.drawable.item_bow,

        // Crossbow
        "multishot" to R.drawable.item_crossbow,
        "quick_charge" to R.drawable.item_crossbow,
        "piercing" to R.drawable.item_crossbow,

        // Fishing rod
        "luck_of_the_sea" to R.drawable.item_fishing_rod,
        "lure" to R.drawable.item_fishing_rod,

        // Spear
        "lunge" to R.drawable.item_trident,

        // Trident
        "loyalty" to R.drawable.item_trident,
        "impaling" to R.drawable.item_trident,
        "riptide" to R.drawable.item_trident,
        "channeling" to R.drawable.item_trident,

        // Mace
        "wind_burst" to R.drawable.item_mace,
        "density" to R.drawable.item_mace,
        "breach" to R.drawable.item_mace,

        // General
        "mending" to R.drawable.item_enchanted_book,
        "vanishing_curse" to R.drawable.item_enchanted_book,
    )

    fun get(enchantId: String): SpyglassIcon? {
        val resId = map[enchantId] ?: return null
        return SpyglassIcon.Drawable(resId)
    }
}
