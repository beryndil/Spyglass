package dev.spyglass.android.data

object BiomeResourceMap {

    private val itemToBiomes: Map<String, List<String>> = mapOf(
        // Overworld logs
        "oak_log" to listOf("plains", "forest", "river", "swamp"),
        "spruce_log" to listOf("taiga", "old_growth_spruce_taiga", "old_growth_pine_taiga", "snowy_taiga", "grove"),
        "birch_log" to listOf("birch_forest", "old_growth_birch_forest", "forest"),
        "jungle_log" to listOf("jungle", "sparse_jungle", "bamboo_jungle"),
        "acacia_log" to listOf("savanna", "savanna_plateau", "windswept_savanna"),
        "dark_oak_log" to listOf("dark_forest"),
        "mangrove_log" to listOf("mangrove_swamp"),
        "cherry_log" to listOf("cherry_grove"),
        "bamboo_block" to listOf("bamboo_jungle", "jungle"),
        // Nether stems
        "crimson_stem" to listOf("crimson_forest"),
        "warped_stem" to listOf("warped_forest"),
        // Ores & minerals
        "raw_iron" to listOf("all_overworld"),
        "raw_gold" to listOf("all_overworld", "badlands", "badlands_plateau"),
        "raw_copper" to listOf("all_overworld", "dripstone_caves"),
        "diamond_ore" to listOf("all_overworld"),
        "diamond" to listOf("all_overworld"),
        "coal" to listOf("all_overworld"),
        "redstone" to listOf("all_overworld"),
        "lapis_lazuli" to listOf("all_overworld"),
        "emerald" to listOf("mountains", "windswept_hills"),
        "quartz" to listOf("nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas"),
        "netherite_scrap" to listOf("nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas"),
        "amethyst_shard" to listOf("all_overworld"),
        // Stone & earth
        "cobblestone" to listOf("all_overworld"),
        "stone" to listOf("all_overworld"),
        "sand" to listOf("desert", "beach"),
        "gravel" to listOf("all_overworld"),
        "clay_ball" to listOf("river", "swamp", "lush_caves"),
        "obsidian" to listOf("all_overworld", "the_end"),
        // Plants & farming
        "sugar_cane" to listOf("river", "swamp", "desert"),
        "wheat" to listOf("plains", "village"),
        "carrot" to listOf("plains", "village"),
        "apple" to listOf("forest", "plains"),
        "melon_slice" to listOf("jungle", "sparse_jungle"),
        "pumpkin" to listOf("plains", "taiga"),
        "cocoa_beans" to listOf("jungle"),
        // Mob drops
        "leather" to listOf("plains", "savanna"),
        "feather" to listOf("plains", "forest"),
        "string" to listOf("all_overworld"),
        "gunpowder" to listOf("all_overworld"),
        "ender_pearl" to listOf("all_overworld", "the_end", "warped_forest"),
        "blaze_rod" to listOf("nether_fortress"),
        "slimeball" to listOf("swamp", "slime_chunks"),
        "ghast_tear" to listOf("nether_wastes", "soul_sand_valley"),
        "prismarine_shard" to listOf("ocean_monument"),
        "shulker_shell" to listOf("the_end"),
        "nether_star" to listOf("summoned"),
        // Other
        "snowball" to listOf("snowy_plains", "ice_spikes", "snowy_taiga", "grove"),
        "honey_bottle" to listOf("plains", "flower_forest", "meadow"),
        "glowstone" to listOf("nether_wastes"),
        "nether_upgrade_template" to listOf("bastion_remnant"),
        "flint" to listOf("all_overworld"),
    )

    private val biomeToItems: Map<String, List<String>> by lazy {
        val result = mutableMapOf<String, MutableList<String>>()
        itemToBiomes.forEach { (item, biomes) ->
            biomes.forEach { biome ->
                result.getOrPut(biome) { mutableListOf() }.add(item)
            }
        }
        result
    }

    fun biomesForItem(itemId: String): List<String> =
        itemToBiomes[itemId] ?: emptyList()

    fun itemsForBiome(biomeId: String): List<String> =
        biomeToItems[biomeId] ?: emptyList()
}
