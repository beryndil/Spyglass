package dev.spyglass.android.core.ui

import dev.spyglass.android.R

/**
 * Texture lookup for biomes using representative block textures.
 * Each biome is mapped to the block most associated with its terrain.
 */
object BiomeTextures {
    private val map = mapOf(
        // Plains
        "plains" to R.drawable.block_grass_block,
        "sunflower_plains" to R.drawable.block_hay_block,
        "savanna" to R.drawable.block_acacia_log,
        "savanna_plateau" to R.drawable.block_acacia_planks,
        "windswept_savanna" to R.drawable.block_acacia_leaves,
        "meadow" to R.drawable.block_flowering_azalea_leaves,

        // Forest
        "forest" to R.drawable.block_oak_log,
        "flower_forest" to R.drawable.block_oak_leaves,
        "birch_forest" to R.drawable.block_birch_log,
        "old_growth_birch_forest" to R.drawable.block_birch_leaves,
        "dark_forest" to R.drawable.block_dark_oak_log,
        "jungle" to R.drawable.block_jungle_log,
        "sparse_jungle" to R.drawable.block_jungle_leaves,
        "bamboo_jungle" to R.drawable.block_bamboo_block,
        "cherry_grove" to R.drawable.block_cherry_leaves,
        "grove" to R.drawable.block_spruce_leaves,
        "pale_garden" to R.drawable.block_pale_oak_planks,

        // Desert & Badlands
        "desert" to R.drawable.block_sand,
        "badlands" to R.drawable.block_terracotta,
        "wooded_badlands" to R.drawable.block_red_sand,
        "eroded_badlands" to R.drawable.block_red_sandstone,

        // Swamp
        "swamp" to R.drawable.block_mangrove_leaves,
        "mangrove_swamp" to R.drawable.block_mangrove_log,

        // Taiga
        "taiga" to R.drawable.block_spruce_log,
        "snowy_taiga" to R.drawable.block_spruce_planks,
        "old_growth_pine_taiga" to R.drawable.block_podzol,
        "old_growth_spruce_taiga" to R.drawable.block_spruce_stairs,

        // Snowy
        "snowy_plains" to R.drawable.block_snow_block,
        "ice_spikes" to R.drawable.block_packed_ice,

        // Mountain
        "snowy_slopes" to R.drawable.block_snow_block,
        "frozen_peaks" to R.drawable.block_ice,
        "jagged_peaks" to R.drawable.block_stone,
        "stony_peaks" to R.drawable.block_cobblestone,

        // Ocean
        "ocean" to R.drawable.block_prismarine,
        "deep_ocean" to R.drawable.block_prismarine_stairs,
        "lukewarm_ocean" to R.drawable.block_prismarine_slab,
        "warm_ocean" to R.drawable.block_sponge,
        "cold_ocean" to R.drawable.block_blue_ice,
        "frozen_ocean" to R.drawable.block_packed_ice,

        // River
        "river" to R.drawable.block_clay,
        "frozen_river" to R.drawable.block_ice,

        // Beach
        "beach" to R.drawable.block_sand,
        "snowy_beach" to R.drawable.block_sandstone,
        "stony_shore" to R.drawable.block_gravel,

        // Mushroom
        "mushroom_fields" to R.drawable.block_mycelium,

        // Nether
        "nether_wastes" to R.drawable.block_netherrack,
        "soul_sand_valley" to R.drawable.block_soul_sand,
        "crimson_forest" to R.drawable.block_crimson_stem,
        "warped_forest" to R.drawable.block_warped_stem,
        "basalt_deltas" to R.drawable.block_basalt,

        // End
        "the_end" to R.drawable.block_end_stone,
        "end_highlands" to R.drawable.block_end_stone_bricks,
        "end_midlands" to R.drawable.block_purpur_block,
        "end_barrens" to R.drawable.block_end_stone,
        "small_end_islands" to R.drawable.block_end_stone,
        "void" to R.drawable.block_obsidian,

        // Underground
        "lush_caves" to R.drawable.block_azalea_leaves,
        "deep_dark" to R.drawable.block_sculk,
        "dripstone_caves" to R.drawable.block_dripstone_block,

        // Windswept
        "windswept_hills" to R.drawable.block_stone,
        "windswept_forest" to R.drawable.block_oak_leaves,
        "windswept_gravelly_hills" to R.drawable.block_gravel,

        // Deep ocean variants
        "deep_lukewarm_ocean" to R.drawable.block_prismarine,
        "deep_cold_ocean" to R.drawable.block_blue_ice,
        "deep_frozen_ocean" to R.drawable.block_packed_ice,
    )

    fun get(biomeId: String): SpyglassIcon? {
        val resId = map[biomeId] ?: return null
        return SpyglassIcon.Drawable(resId)
    }
}
