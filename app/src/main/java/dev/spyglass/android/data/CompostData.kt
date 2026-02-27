package dev.spyglass.android.data

/**
 * Compostable items and their compost chance percentages.
 * Data from Minecraft Java Edition 1.21.
 */
object CompostData {

    private val compostChances: Map<String, Int> = buildMap {
        // 30% chance
        val thirtyPercent = listOf(
            // Leaves
            "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves",
            "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "cherry_leaves",
            "azalea_leaves", "flowering_azalea_leaves", "pale_oak_leaves",
            // Saplings
            "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling",
            "acacia_sapling", "dark_oak_sapling", "mangrove_propagule", "cherry_sapling",
            "pale_oak_sapling",
            // Seeds
            "wheat_seeds", "melon_seeds", "pumpkin_seeds", "beetroot_seeds",
            "torchflower_seeds", "pitcher_pod",
            // Grass & ferns
            "short_grass", "grass", "tall_grass", "fern", "large_fern",
            // Aquatic
            "seagrass", "kelp", "dried_kelp",
            // Vines & roots
            "vine", "hanging_roots", "small_dripleaf", "mangrove_roots",
            "nether_sprouts", "twisting_vines", "weeping_vines",
            // Berries
            "sweet_berries", "glow_berries",
            // Other
            "moss_carpet", "pink_petals",
        )
        thirtyPercent.forEach { put(it, 30) }

        // 50% chance
        val fiftyPercent = listOf(
            "cactus", "dried_kelp_block", "sugar_cane", "melon_slice",
            "glow_lichen", "crimson_roots", "warped_roots",
            "crimson_fungus", "warped_fungus", "nether_wart",
        )
        fiftyPercent.forEach { put(it, 50) }

        // 65% chance
        val sixtyFivePercent = listOf(
            "apple", "beetroot", "carrot", "cocoa_beans", "potato", "poisonous_potato", "wheat",
            "sea_pickle", "lily_pad", "pumpkin", "melon",
            "red_mushroom", "brown_mushroom", "mushroom_stem",
            "red_mushroom_block", "brown_mushroom_block",
            "azalea", "big_dripleaf", "moss_block", "spore_blossom", "shroomlight",
            "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet",
            "red_tulip", "orange_tulip", "white_tulip", "pink_tulip",
            "oxeye_daisy", "cornflower", "lily_of_the_valley",
            "sunflower", "lilac", "rose_bush", "peony",
            "wither_rose", "torchflower", "pitcher_plant",
        )
        sixtyFivePercent.forEach { put(it, 65) }

        // 85% chance
        val eightyFivePercent = listOf(
            "bread", "cookie", "baked_potato", "hay_block",
            "flowering_azalea", "nether_wart_block", "warped_wart_block",
        )
        eightyFivePercent.forEach { put(it, 85) }

        // 100% chance
        val hundredPercent = listOf(
            "cake", "pumpkin_pie",
        )
        hundredPercent.forEach { put(it, 100) }
    }

    /** Returns the compost chance (30, 50, 65, 85, or 100) or null if not compostable. */
    fun chanceFor(itemId: String): Int? = compostChances[itemId]

    /** Returns all compostable items grouped by chance tier, sorted highest first. */
    val byChance: Map<Int, List<String>> by lazy {
        compostChances.entries
            .groupBy({ it.value }, { it.key })
            .toSortedMap(compareByDescending { it })
    }
}
