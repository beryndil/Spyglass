package dev.spyglass.android.core.ui

import dev.spyglass.android.R

/**
 * Texture lookup for potions using their key brewing ingredient.
 * Variants (_long, _ii, _iv) share the base potion's ingredient.
 */
object PotionTextures {
    private val map = mapOf(
        // Base potions
        "water" to R.drawable.item_glass_bottle,
        "mundane" to R.drawable.item_potion,
        "thick" to R.drawable.item_glowstone_dust,
        "awkward" to R.drawable.item_nether_wart,

        // Night Vision family → Golden Carrot
        "night_vision" to R.drawable.item_golden_carrot,
        "night_vision_long" to R.drawable.item_golden_carrot,

        // Invisibility → Fermented Spider Eye
        "invisibility" to R.drawable.item_fermented_spider_eye,
        "invisibility_long" to R.drawable.item_fermented_spider_eye,

        // Leaping → Rabbit Foot
        "leaping" to R.drawable.item_rabbit_foot,
        "leaping_long" to R.drawable.item_rabbit_foot,
        "leaping_ii" to R.drawable.item_rabbit_foot,

        // Fire Resistance → Magma Cream
        "fire_resistance" to R.drawable.item_magma_cream,
        "fire_resistance_long" to R.drawable.item_magma_cream,

        // Swiftness → Sugar
        "swiftness" to R.drawable.item_sugar,
        "swiftness_long" to R.drawable.item_sugar,
        "swiftness_ii" to R.drawable.item_sugar,

        // Slowness → Fermented Spider Eye (from Swiftness)
        "slowness" to R.drawable.item_fermented_spider_eye,
        "slowness_long" to R.drawable.item_fermented_spider_eye,
        "slowness_iv" to R.drawable.item_fermented_spider_eye,

        // Water Breathing → Pufferfish
        "water_breathing" to R.drawable.item_pufferfish,
        "water_breathing_long" to R.drawable.item_pufferfish,

        // Healing → Glistering Melon Slice
        "healing" to R.drawable.item_glistering_melon_slice,
        "healing_ii" to R.drawable.item_glistering_melon_slice,

        // Harming → Fermented Spider Eye (from Healing)
        "harming" to R.drawable.item_fermented_spider_eye,
        "harming_ii" to R.drawable.item_fermented_spider_eye,

        // Poison → Spider Eye
        "poison" to R.drawable.item_spider_eye,
        "poison_long" to R.drawable.item_spider_eye,
        "poison_ii" to R.drawable.item_spider_eye,

        // Regeneration → Ghast Tear
        "regeneration" to R.drawable.item_ghast_tear,
        "regeneration_long" to R.drawable.item_ghast_tear,
        "regeneration_ii" to R.drawable.item_ghast_tear,

        // Strength → Blaze Powder
        "strength" to R.drawable.item_blaze_powder,
        "strength_long" to R.drawable.item_blaze_powder,
        "strength_ii" to R.drawable.item_blaze_powder,

        // Weakness → Fermented Spider Eye
        "weakness" to R.drawable.item_fermented_spider_eye,
        "weakness_long" to R.drawable.item_fermented_spider_eye,

        // Luck → Creative only
        "luck" to R.drawable.item_potion,

        // Turtle Master → Turtle Shell
        "turtle_master" to R.drawable.item_turtle_helmet,
        "turtle_master_long" to R.drawable.item_turtle_helmet,
        "turtle_master_ii" to R.drawable.item_turtle_helmet,

        // Slow Falling → Phantom Membrane
        "slow_falling" to R.drawable.item_phantom_membrane,
        "slow_falling_long" to R.drawable.item_phantom_membrane,

        // Trial chamber potions
        "oozing" to R.drawable.item_slime_ball,
        "weaving" to R.drawable.item_string,
        "infested" to R.drawable.item_potion,
        "wind_charged" to R.drawable.item_breeze_rod,
    )

    fun get(potionId: String): SpyglassIcon? {
        val resId = map[potionId] ?: return null
        return SpyglassIcon.Drawable(resId)
    }
}
