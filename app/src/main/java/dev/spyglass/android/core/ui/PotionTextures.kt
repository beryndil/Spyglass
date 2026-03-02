package dev.spyglass.android.core.ui

import androidx.compose.ui.graphics.Color

/**
 * Texture lookup for potions — returns a tinted potion bottle icon.
 * Colors sourced from Minecraft Wiki effect color values (Java 1.21.4).
 * Variants (_long, _ii, _iv) share the base potion's color.
 */
object PotionTextures {

    // ── Potion effect colors ────────────────────────────────────────────────
    private val WATER         = Color(0xFF385DC6)
    private val NIGHT_VISION  = Color(0xFFC2FF66)
    private val INVISIBILITY  = Color(0xFFF6F6F6)
    private val LEAPING       = Color(0xFFFDFF84)
    private val FIRE_RES      = Color(0xFFFF9900)
    private val SWIFTNESS     = Color(0xFF33EBFF)
    private val SLOWNESS      = Color(0xFF8BAFE0)
    private val WATER_BREATH  = Color(0xFF98DAC0)
    private val HEALING       = Color(0xFFF82423)
    private val HARMING       = Color(0xFFA9656A)
    private val POISON        = Color(0xFF87A363)
    private val REGENERATION  = Color(0xFFCD5CAB)
    private val STRENGTH      = Color(0xFFFFC700)
    private val WEAKNESS      = Color(0xFF484D48)
    private val LUCK          = Color(0xFF59C106)
    private val TURTLE_MASTER = Color(0xFF8D82E6)
    private val SLOW_FALLING  = Color(0xFFF3CFB9)
    private val WIND_CHARGED  = Color(0xFFBDC9FF)
    private val OOZING        = Color(0xFF99FFA3)
    private val WEAVING       = Color(0xFF78695A)
    private val INFESTED      = Color(0xFF8C9B8C)

    private val map = mapOf(
        // Base potions (default water-blue tint)
        "water"                to WATER,
        "mundane"              to WATER,
        "thick"                to WATER,
        "awkward"              to WATER,

        // Night Vision
        "night_vision"         to NIGHT_VISION,
        "night_vision_long"    to NIGHT_VISION,

        // Invisibility
        "invisibility"         to INVISIBILITY,
        "invisibility_long"    to INVISIBILITY,

        // Leaping (Jump Boost)
        "leaping"              to LEAPING,
        "leaping_long"         to LEAPING,
        "leaping_ii"           to LEAPING,

        // Fire Resistance
        "fire_resistance"      to FIRE_RES,
        "fire_resistance_long" to FIRE_RES,

        // Swiftness (Speed)
        "swiftness"            to SWIFTNESS,
        "swiftness_long"       to SWIFTNESS,
        "swiftness_ii"         to SWIFTNESS,

        // Slowness
        "slowness"             to SLOWNESS,
        "slowness_long"        to SLOWNESS,
        "slowness_iv"          to SLOWNESS,

        // Water Breathing
        "water_breathing"      to WATER_BREATH,
        "water_breathing_long" to WATER_BREATH,

        // Healing (Instant Health)
        "healing"              to HEALING,
        "healing_ii"           to HEALING,

        // Harming (Instant Damage)
        "harming"              to HARMING,
        "harming_ii"           to HARMING,

        // Poison
        "poison"               to POISON,
        "poison_long"          to POISON,
        "poison_ii"            to POISON,

        // Regeneration
        "regeneration"         to REGENERATION,
        "regeneration_long"    to REGENERATION,
        "regeneration_ii"      to REGENERATION,

        // Strength
        "strength"             to STRENGTH,
        "strength_long"        to STRENGTH,
        "strength_ii"          to STRENGTH,

        // Weakness
        "weakness"             to WEAKNESS,
        "weakness_long"        to WEAKNESS,

        // Luck (Creative only)
        "luck"                 to LUCK,

        // Turtle Master (Slowness + Resistance blend)
        "turtle_master"        to TURTLE_MASTER,
        "turtle_master_long"   to TURTLE_MASTER,
        "turtle_master_ii"     to TURTLE_MASTER,

        // Slow Falling
        "slow_falling"         to SLOW_FALLING,
        "slow_falling_long"    to SLOW_FALLING,

        // Trial chamber potions (1.21+)
        "wind_charged"         to WIND_CHARGED,
        "oozing"               to OOZING,
        "weaving"              to WEAVING,
        "infested"             to INFESTED,
    )

    fun get(potionId: String): SpyglassIcon? {
        val color = map[potionId] ?: return null
        return SpyglassIcon.Potion(color)
    }
}
