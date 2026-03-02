package dev.spyglass.android.core.ui

import dev.spyglass.android.R

/**
 * Texture lookup for structures using representative block textures.
 * Each structure is mapped to the block most associated with its construction.
 */
object StructureTextures {
    private val map = mapOf(
        "village" to R.drawable.block_oak_planks,
        "pillager_outpost" to R.drawable.block_dark_oak_planks,
        "woodland_mansion" to R.drawable.block_dark_oak_log,
        "ocean_monument" to R.drawable.block_prismarine,
        "stronghold" to R.drawable.block_mossy_stone_bricks,
        "mineshaft" to R.drawable.block_oak_planks,
        "dungeon" to R.drawable.block_mossy_cobblestone,
        "desert_pyramid" to R.drawable.block_sandstone,
        "jungle_temple" to R.drawable.block_cobblestone,
        "swamp_hut" to R.drawable.block_spruce_planks,
        "igloo" to R.drawable.block_snow_block,
        "ocean_ruins" to R.drawable.block_prismarine_stairs,
        "shipwreck" to R.drawable.block_spruce_log,
        "buried_treasure" to R.drawable.block_sand,
        "ruined_portal" to R.drawable.block_obsidian,
        "ancient_city" to R.drawable.block_sculk,
        "trail_ruins" to R.drawable.block_terracotta,
        "trial_chambers" to R.drawable.block_tuff,
        "nether_fortress" to R.drawable.block_nether_bricks,
        "bastion_remnant" to R.drawable.block_blackstone,
        "end_city" to R.drawable.block_purpur_block,
        "end_gateway" to R.drawable.block_obsidian,
        "fossil" to R.drawable.block_stone,
        "nether_fossil" to R.drawable.block_soul_sand,
        "desert_well" to R.drawable.block_sandstone,
    )

    fun get(structureId: String): SpyglassIcon? {
        val resId = map[structureId] ?: return null
        return SpyglassIcon.Drawable(resId)
    }
}
