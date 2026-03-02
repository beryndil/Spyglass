package dev.spyglass.android.core.ui

/**
 * Texture lookup for structures using representative block textures.
 * Each structure is mapped to the block most associated with its construction.
 */
object StructureTextures {
    /** Structure ID → block texture PNG filename (without extension). */
    private val textureMap = mapOf(
        "village" to "block_oak_planks",
        "pillager_outpost" to "block_dark_oak_planks",
        "woodland_mansion" to "block_dark_oak_log",
        "ocean_monument" to "block_prismarine",
        "stronghold" to "block_mossy_stone_bricks",
        "mineshaft" to "block_oak_planks",
        "dungeon" to "block_mossy_cobblestone",
        "desert_pyramid" to "block_sandstone",
        "jungle_temple" to "block_cobblestone",
        "swamp_hut" to "block_spruce_planks",
        "igloo" to "block_snow_block",
        "ocean_ruins" to "block_prismarine_stairs",
        "shipwreck" to "block_spruce_log",
        "buried_treasure" to "block_sand",
        "ruined_portal" to "block_obsidian",
        "ancient_city" to "block_sculk",
        "trail_ruins" to "block_terracotta",
        "trial_chambers" to "block_tuff",
        "nether_fortress" to "block_nether_bricks",
        "bastion_remnant" to "block_blackstone",
        "end_city" to "block_purpur_block",
        "end_gateway" to "block_obsidian",
        "fossil" to "block_stone",
        "nether_fossil" to "block_soul_sand",
        "desert_well" to "block_sandstone",
    )

    fun get(structureId: String): SpyglassIcon? {
        val filename = textureMap[structureId] ?: return null
        return TextureManager.resolveOrBundled(filename)
    }
}
