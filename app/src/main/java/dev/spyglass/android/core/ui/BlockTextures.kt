package dev.spyglass.android.core.ui

import dev.spyglass.android.R

object BlockTextures {
    /**
     * Block ID → PNG filename (without extension).
     * Resolved via [TextureManager] when textures are downloaded.
     */
    private val textureMap = mapOf(
        "stone" to "block_stone",
        "granite" to "block_granite",
        "polished_granite" to "block_polished_granite",
        "diorite" to "block_diorite",
        "andesite" to "block_andesite",
        "deepslate" to "block_deepslate",
        "cobblestone" to "block_cobblestone",
        "cobbled_deepslate" to "block_cobbled_deepslate",
        "oak_planks" to "block_oak_planks",
        "spruce_planks" to "block_spruce_planks",
        "birch_planks" to "block_birch_planks",
        "jungle_planks" to "block_jungle_planks",
        "acacia_planks" to "block_acacia_planks",
        "dark_oak_planks" to "block_dark_oak_planks",
        "mangrove_planks" to "block_mangrove_planks",
        "cherry_planks" to "block_cherry_planks",
        "bamboo_planks" to "block_bamboo_planks",
        "sand" to "block_sand",
        "red_sand" to "block_red_sand",
        "gravel" to "block_gravel",
        "dirt" to "block_dirt",
        "grass_block" to "block_grass_block",
        "podzol" to "block_podzol",
        "mycelium" to "block_mycelium",
        "brown_mushroom_block" to "block_brown_mushroom_block",
        "red_mushroom_block" to "block_red_mushroom_block",
        "clay" to "block_clay",
        "terracotta" to "block_terracotta",
        "glazed_terracotta" to "block_glazed_terracotta",
        "iron_ore" to "block_iron_ore",
        "deepslate_iron_ore" to "block_deepslate_iron_ore",
        "gold_ore" to "block_gold_ore",
        "deepslate_gold_ore" to "block_deepslate_gold_ore",
        "diamond_ore" to "block_diamond_ore",
        "deepslate_diamond_ore" to "block_deepslate_diamond_ore",
        "emerald_ore" to "block_emerald_ore",
        "lapis_ore" to "block_lapis_ore",
        "coal_ore" to "block_coal_ore",
        "copper_ore" to "block_copper_ore",
        "nether_quartz_ore" to "block_nether_quartz_ore",
        "nether_gold_ore" to "block_nether_gold_ore",
        "ancient_debris" to "block_ancient_debris",
        "redstone_ore" to "block_redstone_ore",
        "iron_block" to "block_iron_block",
        "gold_block" to "block_gold_block",
        "diamond_block" to "block_diamond_block",
        "emerald_block" to "block_emerald_block",
        "netherite_block" to "block_netherite_block",
        "obsidian" to "block_obsidian",
        "crying_obsidian" to "block_crying_obsidian",
        "bedrock" to "block_bedrock",
        "glass" to "block_glass",
        "glass_pane" to "block_glass_pane",
        "tinted_glass" to "block_tinted_glass",
        "sandstone" to "block_sandstone",
        "red_sandstone" to "block_red_sandstone",
        "bricks" to "block_bricks",
        "nether_bricks" to "block_nether_bricks",
        "quartz_block" to "block_quartz_block",
        "prismarine" to "block_prismarine",
        "end_stone" to "block_end_stone",
        "purpur_block" to "block_purpur_block",
        "snow_block" to "block_snow_block",
        "ice" to "block_ice",
        "packed_ice" to "block_packed_ice",
        "blue_ice" to "block_blue_ice",
        "sponge" to "block_sponge",
        "wet_sponge" to "block_wet_sponge",
        "wool" to "block_wool",
        "white_wool" to "block_wool",
        "hay_block" to "block_hay_block",
        "bookshelf" to "block_bookshelf",
        "chiseled_bookshelf" to "block_chiseled_bookshelf",
        "crafting_table" to "block_crafting_table",
        "furnace" to "block_furnace",
        "blast_furnace" to "block_blast_furnace",
        "smoker" to "block_smoker",
        "chest" to "block_chest",
        "barrel" to "block_barrel",
        "shulker_box" to "block_shulker_box",
        "ender_chest" to "block_ender_chest",
        "anvil" to "block_anvil",
        "enchanting_table" to "block_enchanting_table",
        "beacon" to "block_beacon",
        "conduit" to "block_conduit",
        "bell" to "block_bell",
        "redstone_block" to "block_redstone_block",
        "redstone_lamp" to "block_redstone_lamp",
        "piston" to "block_piston",
        "sticky_piston" to "block_sticky_piston",
        "observer" to "block_observer",
        "dropper" to "block_dropper",
        "dispenser" to "block_dispenser",
        "hopper" to "block_hopper",
        "comparator" to "block_comparator",
        "repeater" to "block_repeater",
        "tnt" to "block_tnt",
        "netherrack" to "block_netherrack",
        "soul_sand" to "block_soul_sand",
        "soul_soil" to "block_soul_soil",
        "blackstone" to "block_blackstone",
        "basalt" to "block_basalt",
        "magma_block" to "block_magma_block",
        "glowstone" to "block_glowstone",
        "shroomlight" to "block_shroomlight",
        "sculk" to "block_sculk",
        "sculk_catalyst" to "block_sculk_catalyst",
        "sculk_sensor" to "block_sculk_sensor",
        "sculk_shrieker" to "block_sculk_shrieker",
        "mud" to "block_mud",
        "packed_mud" to "block_packed_mud",
        "mud_bricks" to "block_mud_bricks",
        "tuff" to "block_tuff",
        "calcite" to "block_calcite",
        "dripstone_block" to "block_dripstone_block",
        "amethyst_block" to "block_amethyst_block",
        "copper_block" to "block_copper_block",
        "resin_block" to "block_resin_block",
        "resin_bricks" to "block_resin_bricks",
        "chiseled_resin_bricks" to "block_chiseled_resin_bricks",
        "creaking_heart" to "block_creaking_heart",
        "pale_oak_log" to "block_pale_oak_log",
        "pale_oak_planks" to "block_pale_oak_planks",
        "smooth_stone" to "block_smooth_stone",
        "stone_bricks" to "block_stone_bricks",
        "oak_log" to "block_oak_log",
        "spruce_log" to "block_spruce_log",
        "birch_log" to "block_birch_log",
        "jungle_log" to "block_jungle_log",
        "acacia_log" to "block_acacia_log",
        "dark_oak_log" to "block_dark_oak_log",
        "mangrove_log" to "block_mangrove_log",
        "cherry_log" to "block_cherry_log",
        "crimson_stem" to "block_crimson_stem",
        "warped_stem" to "block_warped_stem",
        "bamboo_block" to "block_bamboo_block",
        "stripped_oak_log" to "block_stripped_oak_log",
        "stripped_spruce_log" to "block_stripped_spruce_log",
        "stripped_birch_log" to "block_stripped_birch_log",
        "stripped_jungle_log" to "block_stripped_jungle_log",
        "stripped_acacia_log" to "block_stripped_acacia_log",
        "stripped_dark_oak_log" to "block_stripped_dark_oak_log",
        "stripped_mangrove_log" to "block_stripped_mangrove_log",
        "stripped_cherry_log" to "block_stripped_cherry_log",
        "stripped_crimson_stem" to "block_stripped_crimson_stem",
        "stripped_warped_stem" to "block_stripped_warped_stem",
        "stripped_bamboo_block" to "block_stripped_bamboo_block",
        "oak_leaves" to "block_oak_leaves",
        "spruce_leaves" to "block_spruce_leaves",
        "birch_leaves" to "block_birch_leaves",
        "jungle_leaves" to "block_jungle_leaves",
        "acacia_leaves" to "block_acacia_leaves",
        "dark_oak_leaves" to "block_dark_oak_leaves",
        "mangrove_leaves" to "block_mangrove_leaves",
        "cherry_leaves" to "block_cherry_leaves",
        "azalea_leaves" to "block_azalea_leaves",
        "flowering_azalea_leaves" to "block_flowering_azalea_leaves",
        "torch" to "block_torch",
        "soul_torch" to "block_soul_torch",
        "redstone_torch" to "block_redstone_torch",
        "lantern" to "block_lantern",
        "soul_lantern" to "block_soul_lantern",
        "campfire" to "block_campfire",
        "soul_campfire" to "block_soul_campfire",
        "polished_diorite" to "block_polished_diorite",
        "polished_andesite" to "block_polished_andesite",
        "polished_deepslate" to "block_polished_deepslate",
        "polished_blackstone" to "block_polished_blackstone",
        "mossy_cobblestone" to "block_mossy_cobblestone",
        "mossy_stone_bricks" to "block_mossy_stone_bricks",
        "end_stone_bricks" to "block_end_stone_bricks",
        "purpur_pillar" to "block_purpur_pillar",
        "grindstone" to "block_grindstone",
        "stonecutter" to "block_stonecutter",
        "smithing_table" to "block_smithing_table",
        "cartography_table" to "block_cartography_table",
        "loom" to "block_loom",
        "fletching_table" to "block_fletching_table",
        "composter" to "block_composter",
        "lectern" to "block_lectern",
        "jukebox" to "block_jukebox",
        "armor_stand" to "block_armor_stand",
        "scaffolding" to "block_scaffolding",
        "white_concrete" to "block_white_concrete",
        "orange_concrete" to "block_orange_concrete",
        "magenta_concrete" to "block_magenta_concrete",
        "light_blue_concrete" to "block_light_blue_concrete",
        "yellow_concrete" to "block_yellow_concrete",
        "lime_concrete" to "block_lime_concrete",
        "pink_concrete" to "block_pink_concrete",
        "gray_concrete" to "block_gray_concrete",
        "light_gray_concrete" to "block_light_gray_concrete",
        "cyan_concrete" to "block_cyan_concrete",
        "purple_concrete" to "block_purple_concrete",
        "blue_concrete" to "block_blue_concrete",
        "brown_concrete" to "block_brown_concrete",
        "green_concrete" to "block_green_concrete",
        "red_concrete" to "block_red_concrete",
        "black_concrete" to "block_black_concrete",
        "orange_wool" to "block_orange_wool",
        "magenta_wool" to "block_magenta_wool",
        "light_blue_wool" to "block_light_blue_wool",
        "yellow_wool" to "block_yellow_wool",
        "lime_wool" to "block_lime_wool",
        "pink_wool" to "block_pink_wool",
        "gray_wool" to "block_gray_wool",
        "light_gray_wool" to "block_light_gray_wool",
        "cyan_wool" to "block_cyan_wool",
        "purple_wool" to "block_purple_wool",
        "blue_wool" to "block_blue_wool",
        "brown_wool" to "block_brown_wool",
        "green_wool" to "block_green_wool",
        "red_wool" to "block_red_wool",
        "black_wool" to "block_black_wool",
        "snow" to "block_snow_layer",
        "water" to "block_water_still",
        "water_cauldron" to "block_water_still",
        "lava" to "block_lava_still",
        "lava_cauldron" to "block_lava_still",

        // Anvil variants
        "chipped_anvil" to "block_anvil",
        "damaged_anvil" to "block_anvil",
        // Chest variants
        "trapped_chest" to "block_chest",
        // Stone/brick reuse
        "polished_blackstone_bricks" to "block_polished_blackstone",
        "cracked_polished_blackstone_bricks" to "block_polished_blackstone",
        "deepslate_bricks" to "block_deepslate_brick",
        "deepslate_tiles" to "block_deepslate_brick",
        "cracked_deepslate_bricks" to "block_deepslate_brick",
        "cracked_deepslate_tiles" to "block_deepslate_brick",
        "tuff_bricks" to "block_tuff_brick",
        "chiseled_tuff_bricks" to "block_tuff_brick",
        "quartz_bricks" to "block_quartz_variant",
        "quartz_pillar" to "block_quartz_variant",
        "chiseled_copper" to "block_copper_item",
        // Copper functional blocks
        "copper_bars" to "block_copper_item",
        "copper_bulb" to "block_copper_item",
        "copper_chain" to "block_copper_item",
        "copper_chest" to "block_copper_item",
        "copper_golem_statue" to "block_copper_item",
        "copper_grate" to "block_copper_item",
        "copper_lantern" to "block_copper_item",
        "copper_torch" to "block_copper_item",
        "copper_wall_torch" to "block_copper_item",
        // Redstone components (reuse existing)
        "redstone_wall_torch" to "block_redstone_torch",
        "wall_torch" to "block_torch",
        "soul_wall_torch" to "block_soul_torch",
        "note_block" to "block_jukebox",
        "calibrated_sculk_sensor" to "block_sculk_sensor",
        "crafter" to "block_crafting_table",
        "moving_piston" to "block_piston",
        "daylight_detector" to "block_oak_planks",
        "muddy_mangrove_roots" to "block_mud",
        "suspicious_gravel" to "block_gravel",
        "suspicious_sand" to "block_sand",
        "crimson_fungus" to "block_crimson_stem",
        "warped_fungus" to "block_warped_stem",
        "iron_chain" to "block_iron_block",
        "smooth_quartz" to "block_quartz_variant",
        "piston_head" to "block_piston",
        "end_portal_frame" to "block_end_stone",
        "budding_amethyst" to "block_amethyst_block",
        "nether_wart_block" to "block_netherrack",
        "candle_cake" to "block_torch",
    )

    /**
     * Block ID → bundled vector drawable resource.
     * These are XML vectors that stay in the APK (not downloaded PNGs).
     */
    private val vectorMap = mapOf(
        // Pumpkin variants
        "pumpkin" to R.drawable.block_carved_pumpkin_bk,
        "carved_pumpkin" to R.drawable.block_carved_pumpkin_bk,
        "jack_o_lantern" to R.drawable.block_jack_o_lantern_bk,
        // Stone/brick variants
        "polished_basalt" to R.drawable.block_polished_basalt_bk,
        "polished_tuff" to R.drawable.block_polished_tuff_bk,
        "dark_prismarine" to R.drawable.block_dark_prismarine_bk,
        "red_nether_bricks" to R.drawable.block_red_nether_bk,
        "reinforced_deepslate" to R.drawable.block_reinforced_deepslate_bk,
        "gilded_blackstone" to R.drawable.block_gilded_blackstone_bk,
        // Dirt variants
        "coarse_dirt" to R.drawable.block_coarse_dirt_bk,
        "dirt_path" to R.drawable.block_coarse_dirt_bk,
        "rooted_dirt" to R.drawable.block_coarse_dirt_bk,
        "farmland" to R.drawable.block_coarse_dirt_bk,
        // Sandstone variants
        "cut_sandstone" to R.drawable.block_cut_sandstone_bk,
        "cut_red_sandstone" to R.drawable.block_cut_red_sandstone_bk,
        // Raw ore blocks
        "raw_copper_block" to R.drawable.block_raw_copper_bk,
        "raw_gold_block" to R.drawable.block_raw_gold_bk,
        "raw_iron_block" to R.drawable.block_raw_iron_bk,
        // Nature blocks
        "bone_block" to R.drawable.block_bone_bk,
        "honeycomb_block" to R.drawable.block_honeycomb_bk,
        "dried_kelp_block" to R.drawable.block_dried_kelp_bk,
        "moss_block" to R.drawable.block_moss_bk,
        "moss_carpet" to R.drawable.block_moss_bk,
        "pale_moss_block" to R.drawable.block_pale_moss_bk,
        "pale_moss_carpet" to R.drawable.block_pale_moss_bk,
        "pale_hanging_moss" to R.drawable.block_pale_moss_bk,
        "dried_ghast" to R.drawable.block_bone_bk,
        // Light/glow blocks
        "sea_lantern" to R.drawable.block_sea_lantern_bk,
        "frosted_ice" to R.drawable.block_frosted_ice_bk,
        "end_rod" to R.drawable.block_end_rod_bk,
        "ochre_froglight" to R.drawable.block_sea_lantern_bk,
        "pearlescent_froglight" to R.drawable.block_sea_lantern_bk,
        "verdant_froglight" to R.drawable.block_sea_lantern_bk,
        "light" to R.drawable.block_sea_lantern_bk,
        // Utility blocks
        "lodestone" to R.drawable.block_lodestone_bk,
        "respawn_anchor" to R.drawable.block_respawn_anchor_bk,
        "target" to R.drawable.block_target_bk,
        "decorated_pot" to R.drawable.block_decorated_pot_bk,
        // Beehive/nest
        "beehive" to R.drawable.block_beehive_bk,
        "bee_nest" to R.drawable.block_beehive_bk,
        // Fluids
        "bubble_column" to R.drawable.block_bubble_column_bk,
        "powder_snow" to R.drawable.block_powder_snow_bk,
        "powder_snow_cauldron" to R.drawable.block_powder_snow_bk,
        // Fire
        "fire" to R.drawable.block_fire_bk,
        "soul_fire" to R.drawable.block_soul_fire_bk,
        // Portals
        "nether_portal" to R.drawable.block_nether_portal_bk,
        "end_portal" to R.drawable.block_end_portal_bk,
        // Amethyst
        "amethyst_cluster" to R.drawable.block_amethyst_bk,
        "large_amethyst_bud" to R.drawable.block_amethyst_bk,
        "medium_amethyst_bud" to R.drawable.block_amethyst_bk,
        "small_amethyst_bud" to R.drawable.block_amethyst_bk,
        // Cobweb
        "cobweb" to R.drawable.block_cobweb_bk,
        // Plants & bushes
        "lily_pad" to R.drawable.block_lily_pad_bk,
        "azalea" to R.drawable.block_azalea_bk,
        "flowering_azalea" to R.drawable.block_azalea_bk,
        "bush" to R.drawable.block_bush_bk,
        "firefly_bush" to R.drawable.block_bush_bk,
        "dead_bush" to R.drawable.block_bush_bk,
        "sweet_berry_bush" to R.drawable.block_bush_bk,
        // Flowers — color matches the dye each flower crafts into
        "dandelion" to R.drawable.block_flower_yellow,          // → yellow dye
        "allium" to R.drawable.block_flower_magenta,            // → magenta dye
        "azure_bluet" to R.drawable.block_flower_light_gray,    // → light gray dye
        "blue_orchid" to R.drawable.block_flower_light_blue,    // → light blue dye
        "cornflower" to R.drawable.block_flower_blue,           // → blue dye
        "lily_of_the_valley" to R.drawable.block_flower_white,  // → white dye
        "orange_tulip" to R.drawable.block_flower_orange,       // → orange dye
        "oxeye_daisy" to R.drawable.block_flower_light_gray,    // → light gray dye
        "pink_tulip" to R.drawable.block_flower_pink,           // → pink dye
        "red_tulip" to R.drawable.block_flower_red,             // → red dye
        "white_tulip" to R.drawable.block_flower_light_gray,    // → light gray dye
        "wither_rose" to R.drawable.block_flower_black,         // → black dye
        "torchflower" to R.drawable.block_flower_orange,        // → orange dye
        "pitcher_plant" to R.drawable.block_flower_cyan,        // → cyan dye
        "golden_dandelion" to R.drawable.block_flower_yellow,   // → yellow dye
        "open_eyeblossom" to R.drawable.block_flower_orange,    // → orange dye
        "closed_eyeblossom" to R.drawable.block_flower_gray,    // → gray dye
        // Tall flowers (×2 dye output)
        "sunflower" to R.drawable.block_flower_yellow,          // → yellow dye
        "lilac" to R.drawable.block_flower_magenta,             // → magenta dye
        "rose_bush" to R.drawable.block_flower_red,             // → red dye
        "peony" to R.drawable.block_flower_pink,                // → pink dye
        // Decorative (no dye recipe)
        "spore_blossom" to R.drawable.block_flower_pink,
        "cactus_flower" to R.drawable.block_flower_yellow,
        "pink_petals" to R.drawable.block_flower_pink,
        "wildflowers" to R.drawable.block_flower_blue,
        "chorus_flower" to R.drawable.block_flower_purple,
        "chorus_plant" to R.drawable.block_flower_purple,
        // Vines and hanging plants
        "vine" to R.drawable.block_vine_bk,
        "glow_lichen" to R.drawable.block_vine_bk,
        "hanging_roots" to R.drawable.block_vine_bk,
        "cave_vines" to R.drawable.block_vine_bk,
        "cave_vines_plant" to R.drawable.block_vine_bk,
        "twisting_vines_plant" to R.drawable.block_vine_bk,
        "weeping_vines_plant" to R.drawable.block_vine_bk,
        "kelp_plant" to R.drawable.block_vine_bk,
        "sculk_vein" to R.drawable.block_sculk_vein_bk,
        "sea_pickle" to R.drawable.block_vine_bk,
        "big_dripleaf_stem" to R.drawable.block_vine_bk,
        "tall_seagrass" to R.drawable.block_vine_bk,
        // Leaves and foliage
        "big_dripleaf" to R.drawable.block_leaf_bk,
        "small_dripleaf" to R.drawable.block_leaf_bk,
        "leaf_litter" to R.drawable.block_coarse_dirt_bk,
        "short_dry_grass" to R.drawable.block_coarse_dirt_bk,
        "tall_dry_grass" to R.drawable.block_coarse_dirt_bk,
        // Dripstone
        "pointed_dripstone" to R.drawable.block_dripstone_bk,
        // Crops
        "beetroots" to R.drawable.block_crop_bk,
        "carrots" to R.drawable.block_crop_bk,
        "potatoes" to R.drawable.block_crop_bk,
        "melon_stem" to R.drawable.block_crop_bk,
        "pumpkin_stem" to R.drawable.block_crop_bk,
        "attached_melon_stem" to R.drawable.block_crop_bk,
        "attached_pumpkin_stem" to R.drawable.block_crop_bk,
        "cocoa" to R.drawable.block_crop_bk,
        "pitcher_crop" to R.drawable.block_crop_bk,
        "torchflower_crop" to R.drawable.block_crop_bk,
        // Rails
        "activator_rail" to R.drawable.block_rail_bk,
        "detector_rail" to R.drawable.block_rail_bk,
        // Redstone components
        "lever" to R.drawable.block_lever_bk,
        "tripwire" to R.drawable.block_redstone_wire_bk,
        "redstone_wire" to R.drawable.block_redstone_wire_bk,
        // Spawner / cage blocks
        "spawner" to R.drawable.block_spawner_bk,
        "trial_spawner" to R.drawable.block_spawner_bk,
        "vault" to R.drawable.block_spawner_bk,
        // Command blocks
        "command_block" to R.drawable.block_command_bk,
        "chain_command_block" to R.drawable.block_command_bk,
        "repeating_command_block" to R.drawable.block_command_bk,
        // Technical / invisible blocks
        "air" to R.drawable.block_powder_snow_bk,
        "cave_air" to R.drawable.block_powder_snow_bk,
        "void_air" to R.drawable.block_end_portal_bk,
        "barrier" to R.drawable.block_command_bk,
        "structure_void" to R.drawable.block_command_bk,
        "structure_block" to R.drawable.block_command_bk,
        "jigsaw" to R.drawable.block_command_bk,
        "test_block" to R.drawable.block_command_bk,
        "test_instance_block" to R.drawable.block_command_bk,
    )

    fun get(blockId: String): SpyglassIcon? {
        // 1. Try downloadable texture
        textureMap[blockId]?.let { filename ->
            TextureManager.resolveOrBundled(filename)?.let { return it }
        }
        // 2. Try bundled vector
        vectorMap[blockId]?.let { return SpyglassIcon.Drawable(it) }
        // 3. Try fallback resolution
        return resolveFallback(blockId)
    }

    /** Used by other texture objects to check if a texture name exists. */
    internal fun resolveTextureName(blockId: String): String? = textureMap[blockId]

    /** Block-specific fallback: maps common variant patterns to a base block texture. */
    private fun resolveFallback(blockId: String): SpyglassIcon? {
        // Waxed / oxidation → copper_block
        if (blockId.startsWith("waxed_") || blockId.startsWith("exposed_") ||
            blockId.startsWith("weathered_") || blockId.startsWith("oxidized_") ||
            blockId.startsWith("cut_copper")) {
            return resolveById("copper_block")
        }

        // Colored block variants
        for (color in COLORS) {
            if (!blockId.startsWith("${color}_")) continue
            val suffix = blockId.removePrefix("${color}_")
            val result = when (suffix) {
                "wool" -> resolveById("${color}_wool") ?: resolveById("wool")
                "concrete", "concrete_powder" -> resolveById("${color}_concrete")
                "terracotta", "glazed_terracotta" -> resolveById("terracotta")
                "stained_glass" -> resolveById("glass")
                "stained_glass_pane" -> resolveById("glass_pane")
                "shulker_box" -> resolveById("shulker_box")
                "carpet", "bed", "banner", "wall_banner" -> resolveById("${color}_wool") ?: resolveById("wool")
                "candle", "candle_cake" -> resolveById("torch")
                else -> null
            }
            if (result != null) return result
        }

        // Coral → prismarine
        if (blockId.contains("coral")) return resolveById("prismarine")

        return null
    }

    /** Resolve a block ID from both maps. */
    private fun resolveById(id: String): SpyglassIcon? {
        textureMap[id]?.let { filename ->
            TextureManager.resolveOrBundled(filename)?.let { return it }
        }
        vectorMap[id]?.let { return SpyglassIcon.Drawable(it) }
        return null
    }

    private val COLORS = listOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
        "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    )
}
