package dev.spyglass.android.core.ui

/**
 * Texture lookup for biomes using representative block textures.
 * Each biome is mapped to the block most associated with its terrain.
 */
object BiomeTextures {
    /** Biome ID → block texture PNG filename (without extension). */
    private val textureMap = mapOf(
        // Plains
        "plains" to "block_grass_block",
        "sunflower_plains" to "block_hay_block",
        "savanna" to "block_acacia_log",
        "savanna_plateau" to "block_acacia_planks",
        "savanna_plateau" to "block_acacia_planks",
        "windswept_savanna" to "block_acacia_leaves",
        "meadow" to "block_flowering_azalea_leaves",

        // Forest
        "forest" to "block_oak_log",
        "flower_forest" to "block_oak_leaves",
        "birch_forest" to "block_birch_log",
        "old_growth_birch_forest" to "block_birch_leaves",
        "dark_forest" to "block_dark_oak_log",
        "jungle" to "block_jungle_log",
        "sparse_jungle" to "block_jungle_leaves",
        "bamboo_jungle" to "block_bamboo_block",
        "cherry_grove" to "block_cherry_leaves",
        "grove" to "block_spruce_leaves",
        "pale_garden" to "block_pale_oak_planks",

        // Desert & Badlands
        "desert" to "block_sand",
        "badlands" to "block_terracotta",
        "wooded_badlands" to "block_red_sand",
        "eroded_badlands" to "block_red_sandstone",

        // Swamp
        "swamp" to "block_mangrove_leaves",
        "mangrove_swamp" to "block_mangrove_log",

        // Taiga
        "taiga" to "block_spruce_log",
        "snowy_taiga" to "block_spruce_planks",
        "old_growth_pine_taiga" to "block_podzol",
        "old_growth_spruce_taiga" to "block_spruce_stairs",

        // Snowy
        "snowy_plains" to "block_snow_block",
        "ice_spikes" to "block_packed_ice",

        // Mountain
        "snowy_slopes" to "block_snow_block",
        "frozen_peaks" to "block_ice",
        "jagged_peaks" to "block_stone",
        "stony_peaks" to "block_cobblestone",

        // Ocean
        "ocean" to "block_prismarine",
        "deep_ocean" to "block_prismarine_stairs",
        "lukewarm_ocean" to "block_prismarine_slab",
        "warm_ocean" to "block_sponge",
        "cold_ocean" to "block_blue_ice",
        "frozen_ocean" to "block_packed_ice",

        // River
        "river" to "block_clay",
        "frozen_river" to "block_ice",

        // Beach
        "beach" to "block_sand",
        "snowy_beach" to "block_sandstone",
        "stony_shore" to "block_gravel",

        // Mushroom
        "mushroom_fields" to "block_mycelium",

        // Nether
        "nether_wastes" to "block_netherrack",
        "soul_sand_valley" to "block_soul_sand",
        "crimson_forest" to "block_crimson_stem",
        "warped_forest" to "block_warped_stem",
        "basalt_deltas" to "block_basalt",

        // End
        "the_end" to "block_end_stone",
        "end_highlands" to "block_end_stone_bricks",
        "end_midlands" to "block_purpur_block",
        "end_barrens" to "block_end_stone",
        "small_end_islands" to "block_end_stone",
        "void" to "block_obsidian",

        // Underground
        "lush_caves" to "block_azalea_leaves",
        "deep_dark" to "block_sculk",
        "dripstone_caves" to "block_dripstone_block",

        // Windswept
        "windswept_hills" to "block_stone",
        "windswept_forest" to "block_oak_leaves",
        "windswept_gravelly_hills" to "block_gravel",

        // Deep ocean variants
        "deep_lukewarm_ocean" to "block_prismarine",
        "deep_cold_ocean" to "block_blue_ice",
        "deep_frozen_ocean" to "block_packed_ice",
    )

    fun get(biomeId: String): SpyglassIcon? {
        val filename = textureMap[biomeId] ?: return null
        return TextureManager.resolveOrBundled(filename)
    }
}
