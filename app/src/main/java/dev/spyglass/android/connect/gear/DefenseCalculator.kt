package dev.spyglass.android.connect.gear

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class DefenseStats(
    val totalArmor: Int,
    val armorToughness: Float,
    val knockbackResistance: Float,
    val genericReduction: Int,
    val fireReduction: Int,
    val blastReduction: Int,
    val projectileReduction: Int,
    val fallReduction: Int,
    val maxSurvivableFall: Int,
)

object DefenseCalculator {

    private val ARMOR_SLOTS = setOf(SlotType.HEAD, SlotType.CHEST, SlotType.LEGS, SlotType.FEET)

    fun calculate(gearAnalysis: GearAnalysis, currentHealth: Float): DefenseStats {
        val armorSlots = gearAnalysis.slots.filter { it.slotType in ARMOR_SLOTS }

        var totalArmor = 0
        var totalToughness = 0f
        var totalKbResist = 0f

        // EPF totals per damage type
        var epfGeneric = 0
        var epfFire = 0
        var epfBlast = 0
        var epfProjectile = 0
        var epfFall = 0

        for (slot in armorSlots) {
            val entity = slot.itemEntity
            if (entity != null) {
                totalArmor += entity.defensePoints
                totalToughness += entity.armorToughness
                totalKbResist += entity.knockbackResistance
            }

            val enchants = slot.item?.enchantments ?: emptyList()
            for (e in enchants) {
                when (e.id) {
                    "protection" -> {
                        val epf = 1 * e.level
                        epfGeneric += epf
                        epfFire += epf
                        epfBlast += epf
                        epfProjectile += epf
                        epfFall += epf
                    }
                    "fire_protection" -> epfFire += 2 * e.level
                    "blast_protection" -> epfBlast += 2 * e.level
                    "projectile_protection" -> epfProjectile += 2 * e.level
                    "feather_falling" -> epfFall += 3 * e.level
                }
            }
        }

        val refDamage = 10f
        val genericReduction = reductionPercent(refDamage, totalArmor, totalToughness, epfGeneric)
        val fireReduction = reductionPercent(refDamage, totalArmor, totalToughness, epfFire)
        val blastReduction = reductionPercent(refDamage, totalArmor, totalToughness, epfBlast)
        val projectileReduction = reductionPercent(refDamage, totalArmor, totalToughness, epfProjectile)
        val fallReduction = reductionPercent(refDamage, totalArmor, totalToughness, epfFall)

        val maxFall = calculateMaxFall(currentHealth, totalArmor, totalToughness, epfFall)

        return DefenseStats(
            totalArmor = totalArmor,
            armorToughness = totalToughness,
            knockbackResistance = totalKbResist,
            genericReduction = genericReduction,
            fireReduction = fireReduction,
            blastReduction = blastReduction,
            projectileReduction = projectileReduction,
            fallReduction = fallReduction,
            maxSurvivableFall = maxFall,
        )
    }

    private fun reductionPercent(damage: Float, armor: Int, toughness: Float, epf: Int): Int {
        val afterArmor = afterArmor(damage, armor, toughness)
        val afterEnchants = afterArmor * (1f - min(20, epf) / 25f)
        return ((1f - afterEnchants / damage) * 100f).roundToInt()
    }

    private fun afterArmor(damage: Float, armor: Int, toughness: Float): Float {
        val effective = max(
            armor / 5f,
            armor - 4f * damage / (2f + toughness / 4f)
        ).coerceIn(0f, 20f)
        return damage * (1f - effective / 25f)
    }

    private fun calculateMaxFall(health: Float, armor: Int, toughness: Float, epfFall: Int): Int {
        for (blocks in 4..500) {
            val rawDamage = (blocks - 3).toFloat()
            val afterArm = afterArmor(rawDamage, armor, toughness)
            val finalDamage = afterArm * (1f - min(20, epfFall) / 25f)
            if (finalDamage >= health) return blocks - 1
        }
        return 500
    }
}
