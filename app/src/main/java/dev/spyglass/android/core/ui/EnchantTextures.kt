package dev.spyglass.android.core.ui

/**
 * Texture lookup for enchantments using the item each enchantment
 * is most commonly associated with.
 */
object EnchantTextures {
    /** Enchant ID → item texture PNG filename (without extension). */
    private val textureMap = mapOf(
        // Armor — protection family
        "protection" to "item_iron_chestplate",
        "fire_protection" to "item_golden_chestplate",
        "blast_protection" to "item_diamond_chestplate",
        "projectile_protection" to "item_shield",
        "thorns" to "item_netherite_chestplate",
        "binding_curse" to "item_iron_chestplate",

        // Helmet
        "respiration" to "item_iron_helmet",
        "aqua_affinity" to "item_diamond_helmet",

        // Boots
        "feather_falling" to "item_leather_boots",
        "depth_strider" to "item_diamond_boots",
        "frost_walker" to "item_diamond_boots",
        "soul_speed" to "item_golden_boots",

        // Leggings
        "swift_sneak" to "item_leather_leggings",

        // Sword
        "sharpness" to "item_diamond_sword",
        "smite" to "item_golden_sword",
        "bane_of_arthropods" to "item_iron_sword",
        "knockback" to "item_iron_sword",
        "fire_aspect" to "item_fire_charge",
        "looting" to "item_diamond_sword",
        "sweeping_edge" to "item_netherite_sword",

        // Tool
        "efficiency" to "item_diamond_pickaxe",
        "silk_touch" to "item_diamond_pickaxe",
        "unbreaking" to "item_diamond_pickaxe",
        "fortune" to "item_diamond_pickaxe",

        // Bow
        "power" to "item_bow",
        "punch" to "item_bow",
        "flame" to "item_bow",
        "infinity" to "item_bow",

        // Crossbow
        "multishot" to "item_crossbow",
        "quick_charge" to "item_crossbow",
        "piercing" to "item_crossbow",

        // Fishing rod
        "luck_of_the_sea" to "item_fishing_rod",
        "lure" to "item_fishing_rod",

        // Spear
        "lunge" to "item_trident",

        // Trident
        "loyalty" to "item_trident",
        "impaling" to "item_trident",
        "riptide" to "item_trident",
        "channeling" to "item_trident",

        // Mace
        "wind_burst" to "item_mace",
        "density" to "item_mace",
        "breach" to "item_mace",

        // General
        "mending" to "item_enchanted_book",
        "vanishing_curse" to "item_enchanted_book",
    )

    fun get(enchantId: String): SpyglassIcon? {
        val filename = textureMap[enchantId] ?: return null
        return TextureManager.resolveOrBundled(filename)
    }
}
