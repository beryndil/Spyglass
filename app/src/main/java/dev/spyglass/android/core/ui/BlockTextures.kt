package dev.spyglass.android.core.ui

import dev.spyglass.android.R

object BlockTextures {
    private val map = mapOf(
        "stone" to R.drawable.block_stone,
        "granite" to R.drawable.block_granite,
        "polished_granite" to R.drawable.block_polished_granite,
        "diorite" to R.drawable.block_diorite,
        "andesite" to R.drawable.block_andesite,
        "deepslate" to R.drawable.block_deepslate,
        "cobblestone" to R.drawable.block_cobblestone,
        "cobbled_deepslate" to R.drawable.block_cobbled_deepslate,
        "oak_planks" to R.drawable.block_oak_planks,
        "spruce_planks" to R.drawable.block_spruce_planks,
        "birch_planks" to R.drawable.block_birch_planks,
        "jungle_planks" to R.drawable.block_jungle_planks,
        "acacia_planks" to R.drawable.block_acacia_planks,
        "dark_oak_planks" to R.drawable.block_dark_oak_planks,
        "mangrove_planks" to R.drawable.block_mangrove_planks,
        "cherry_planks" to R.drawable.block_cherry_planks,
        "bamboo_planks" to R.drawable.block_bamboo_planks,
        "sand" to R.drawable.block_sand,
        "red_sand" to R.drawable.block_red_sand,
        "gravel" to R.drawable.block_gravel,
        "dirt" to R.drawable.block_dirt,
        "grass_block" to R.drawable.block_grass_block,
        "podzol" to R.drawable.block_podzol,
        "mycelium" to R.drawable.block_mycelium,
        "brown_mushroom_block" to R.drawable.block_brown_mushroom_block,
        "red_mushroom_block" to R.drawable.block_red_mushroom_block,
        "clay" to R.drawable.block_clay,
        "terracotta" to R.drawable.block_terracotta,
        "glazed_terracotta" to R.drawable.block_glazed_terracotta,
        "iron_ore" to R.drawable.block_iron_ore,
        "deepslate_iron_ore" to R.drawable.block_deepslate_iron_ore,
        "gold_ore" to R.drawable.block_gold_ore,
        "deepslate_gold_ore" to R.drawable.block_deepslate_gold_ore,
        "diamond_ore" to R.drawable.block_diamond_ore,
        "deepslate_diamond_ore" to R.drawable.block_deepslate_diamond_ore,
        "emerald_ore" to R.drawable.block_emerald_ore,
        "lapis_ore" to R.drawable.block_lapis_ore,
        "coal_ore" to R.drawable.block_coal_ore,
        "copper_ore" to R.drawable.block_copper_ore,
        "nether_quartz_ore" to R.drawable.block_nether_quartz_ore,
        "nether_gold_ore" to R.drawable.block_nether_gold_ore,
        "ancient_debris" to R.drawable.block_ancient_debris,
        "redstone_ore" to R.drawable.block_redstone_ore,
        "iron_block" to R.drawable.block_iron_block,
        "gold_block" to R.drawable.block_gold_block,
        "diamond_block" to R.drawable.block_diamond_block,
        "emerald_block" to R.drawable.block_emerald_block,
        "netherite_block" to R.drawable.block_netherite_block,
        "obsidian" to R.drawable.block_obsidian,
        "crying_obsidian" to R.drawable.block_crying_obsidian,
        "bedrock" to R.drawable.block_bedrock,
        "glass" to R.drawable.block_glass,
        "glass_pane" to R.drawable.block_glass_pane,
        "tinted_glass" to R.drawable.block_tinted_glass,
        "sandstone" to R.drawable.block_sandstone,
        "red_sandstone" to R.drawable.block_red_sandstone,
        "bricks" to R.drawable.block_bricks,
        "nether_bricks" to R.drawable.block_nether_bricks,
        "quartz_block" to R.drawable.block_quartz_block,
        "prismarine" to R.drawable.block_prismarine,
        "end_stone" to R.drawable.block_end_stone,
        "purpur_block" to R.drawable.block_purpur_block,
        "snow_block" to R.drawable.block_snow_block,
        "ice" to R.drawable.block_ice,
        "packed_ice" to R.drawable.block_packed_ice,
        "blue_ice" to R.drawable.block_blue_ice,
        "sponge" to R.drawable.block_sponge,
        "wet_sponge" to R.drawable.block_wet_sponge,
        "wool" to R.drawable.block_wool,
        "white_wool" to R.drawable.block_wool,
        "hay_block" to R.drawable.block_hay_block,
        "bookshelf" to R.drawable.block_bookshelf,
        "chiseled_bookshelf" to R.drawable.block_chiseled_bookshelf,
        "crafting_table" to R.drawable.block_crafting_table,
        "furnace" to R.drawable.block_furnace,
        "blast_furnace" to R.drawable.block_blast_furnace,
        "smoker" to R.drawable.block_smoker,
        "chest" to R.drawable.block_chest,
        "barrel" to R.drawable.block_barrel,
        "shulker_box" to R.drawable.block_shulker_box,
        "ender_chest" to R.drawable.block_ender_chest,
        "anvil" to R.drawable.block_anvil,
        "enchanting_table" to R.drawable.block_enchanting_table,
        "beacon" to R.drawable.block_beacon,
        "conduit" to R.drawable.block_conduit,
        "bell" to R.drawable.block_bell,
        "redstone_block" to R.drawable.block_redstone_block,
        "redstone_lamp" to R.drawable.block_redstone_lamp,
        "piston" to R.drawable.block_piston,
        "sticky_piston" to R.drawable.block_sticky_piston,
        "observer" to R.drawable.block_observer,
        "dropper" to R.drawable.block_dropper,
        "dispenser" to R.drawable.block_dispenser,
        "hopper" to R.drawable.block_hopper,
        "comparator" to R.drawable.block_comparator,
        "repeater" to R.drawable.block_repeater,
        "tnt" to R.drawable.block_tnt,
        "netherrack" to R.drawable.block_netherrack,
        "soul_sand" to R.drawable.block_soul_sand,
        "soul_soil" to R.drawable.block_soul_soil,
        "blackstone" to R.drawable.block_blackstone,
        "basalt" to R.drawable.block_basalt,
        "magma_block" to R.drawable.block_magma_block,
        "glowstone" to R.drawable.block_glowstone,
        "shroomlight" to R.drawable.block_shroomlight,
        "sculk" to R.drawable.block_sculk,
        "sculk_catalyst" to R.drawable.block_sculk_catalyst,
        "sculk_sensor" to R.drawable.block_sculk_sensor,
        "sculk_shrieker" to R.drawable.block_sculk_shrieker,
        "mud" to R.drawable.block_mud,
        "packed_mud" to R.drawable.block_packed_mud,
        "mud_bricks" to R.drawable.block_mud_bricks,
        "tuff" to R.drawable.block_tuff,
        "calcite" to R.drawable.block_calcite,
        "dripstone_block" to R.drawable.block_dripstone_block,
        "amethyst_block" to R.drawable.block_amethyst_block,
        "copper_block" to R.drawable.block_copper_block,
        "resin_block" to R.drawable.block_resin_block,
        "resin_bricks" to R.drawable.block_resin_bricks,
        "resin_brick_slab" to R.drawable.block_resin_brick_slab,
        "resin_brick_stairs" to R.drawable.block_resin_brick_stairs,
        "resin_brick_wall" to R.drawable.block_resin_brick_wall,
        "chiseled_resin_bricks" to R.drawable.block_chiseled_resin_bricks,
        "creaking_heart" to R.drawable.block_creaking_heart,
        "pale_oak_log" to R.drawable.block_pale_oak_log,
        "pale_oak_planks" to R.drawable.block_pale_oak_planks,
        "smooth_stone" to R.drawable.block_smooth_stone,
        "stone_bricks" to R.drawable.block_stone_bricks,
        "oak_log" to R.drawable.block_oak_log,
        "spruce_log" to R.drawable.block_spruce_log,
        "birch_log" to R.drawable.block_birch_log,
        "jungle_log" to R.drawable.block_jungle_log,
        "acacia_log" to R.drawable.block_acacia_log,
        "dark_oak_log" to R.drawable.block_dark_oak_log,
        "mangrove_log" to R.drawable.block_mangrove_log,
        "cherry_log" to R.drawable.block_cherry_log,
        "crimson_stem" to R.drawable.block_crimson_stem,
        "warped_stem" to R.drawable.block_warped_stem,
        "bamboo_block" to R.drawable.block_bamboo_block,
        "stripped_oak_log" to R.drawable.block_stripped_oak_log,
        "stripped_spruce_log" to R.drawable.block_stripped_spruce_log,
        "stripped_birch_log" to R.drawable.block_stripped_birch_log,
        "stripped_jungle_log" to R.drawable.block_stripped_jungle_log,
        "stripped_acacia_log" to R.drawable.block_stripped_acacia_log,
        "stripped_dark_oak_log" to R.drawable.block_stripped_dark_oak_log,
        "stripped_mangrove_log" to R.drawable.block_stripped_mangrove_log,
        "stripped_cherry_log" to R.drawable.block_stripped_cherry_log,
        "stripped_crimson_stem" to R.drawable.block_stripped_crimson_stem,
        "stripped_warped_stem" to R.drawable.block_stripped_warped_stem,
        "stripped_bamboo_block" to R.drawable.block_stripped_bamboo_block,
        "oak_leaves" to R.drawable.block_oak_leaves,
        "spruce_leaves" to R.drawable.block_spruce_leaves,
        "birch_leaves" to R.drawable.block_birch_leaves,
        "jungle_leaves" to R.drawable.block_jungle_leaves,
        "acacia_leaves" to R.drawable.block_acacia_leaves,
        "dark_oak_leaves" to R.drawable.block_dark_oak_leaves,
        "mangrove_leaves" to R.drawable.block_mangrove_leaves,
        "cherry_leaves" to R.drawable.block_cherry_leaves,
        "azalea_leaves" to R.drawable.block_azalea_leaves,
        "flowering_azalea_leaves" to R.drawable.block_flowering_azalea_leaves,
        "oak_stairs" to R.drawable.block_oak_stairs,
        "spruce_stairs" to R.drawable.block_spruce_stairs,
        "birch_stairs" to R.drawable.block_birch_stairs,
        "jungle_stairs" to R.drawable.block_jungle_stairs,
        "acacia_stairs" to R.drawable.block_acacia_stairs,
        "dark_oak_stairs" to R.drawable.block_dark_oak_stairs,
        "stone_stairs" to R.drawable.block_stone_stairs,
        "cobblestone_stairs" to R.drawable.block_cobblestone_stairs,
        "stone_brick_stairs" to R.drawable.block_stone_brick_stairs,
        "mossy_stone_brick_stairs" to R.drawable.block_mossy_stone_brick_stairs,
        "sandstone_stairs" to R.drawable.block_sandstone_stairs,
        "red_sandstone_stairs" to R.drawable.block_red_sandstone_stairs,
        "brick_stairs" to R.drawable.block_brick_stairs,
        "nether_brick_stairs" to R.drawable.block_nether_brick_stairs,
        "quartz_stairs" to R.drawable.block_quartz_stairs,
        "prismarine_stairs" to R.drawable.block_prismarine_stairs,
        "purpur_stairs" to R.drawable.block_purpur_stairs,
        "oak_slab" to R.drawable.block_oak_slab,
        "spruce_slab" to R.drawable.block_spruce_slab,
        "birch_slab" to R.drawable.block_birch_slab,
        "jungle_slab" to R.drawable.block_jungle_slab,
        "acacia_slab" to R.drawable.block_acacia_slab,
        "dark_oak_slab" to R.drawable.block_dark_oak_slab,
        "stone_slab" to R.drawable.block_stone_slab,
        "cobblestone_slab" to R.drawable.block_cobblestone_slab,
        "stone_brick_slab" to R.drawable.block_stone_brick_slab,
        "sandstone_slab" to R.drawable.block_sandstone_slab,
        "brick_slab" to R.drawable.block_brick_slab,
        "nether_brick_slab" to R.drawable.block_nether_brick_slab,
        "quartz_slab" to R.drawable.block_quartz_slab,
        "prismarine_slab" to R.drawable.block_prismarine_slab,
        "smooth_stone_slab" to R.drawable.block_smooth_stone_slab,
        "purpur_slab" to R.drawable.block_purpur_slab,
        "oak_fence" to R.drawable.block_oak_fence,
        "spruce_fence" to R.drawable.block_spruce_fence,
        "birch_fence" to R.drawable.block_birch_fence,
        "jungle_fence" to R.drawable.block_jungle_fence,
        "acacia_fence" to R.drawable.block_acacia_fence,
        "dark_oak_fence" to R.drawable.block_dark_oak_fence,
        "nether_brick_fence" to R.drawable.block_nether_brick_fence,
        "crimson_fence" to R.drawable.block_crimson_fence,
        "warped_fence" to R.drawable.block_warped_fence,
        "oak_door" to R.drawable.block_oak_door,
        "spruce_door" to R.drawable.block_spruce_door,
        "birch_door" to R.drawable.block_birch_door,
        "jungle_door" to R.drawable.block_jungle_door,
        "acacia_door" to R.drawable.block_acacia_door,
        "dark_oak_door" to R.drawable.block_dark_oak_door,
        "iron_door" to R.drawable.block_iron_door,
        "crimson_door" to R.drawable.block_crimson_door,
        "warped_door" to R.drawable.block_warped_door,
        "oak_trapdoor" to R.drawable.block_oak_trapdoor,
        "spruce_trapdoor" to R.drawable.block_spruce_trapdoor,
        "birch_trapdoor" to R.drawable.block_birch_trapdoor,
        "jungle_trapdoor" to R.drawable.block_jungle_trapdoor,
        "acacia_trapdoor" to R.drawable.block_acacia_trapdoor,
        "dark_oak_trapdoor" to R.drawable.block_dark_oak_trapdoor,
        "iron_trapdoor" to R.drawable.block_iron_trapdoor,
        "crimson_trapdoor" to R.drawable.block_crimson_trapdoor,
        "warped_trapdoor" to R.drawable.block_warped_trapdoor,
        "torch" to R.drawable.block_torch,
        "soul_torch" to R.drawable.block_soul_torch,
        "redstone_torch" to R.drawable.block_redstone_torch,
        "lantern" to R.drawable.block_lantern,
        "soul_lantern" to R.drawable.block_soul_lantern,
        "campfire" to R.drawable.block_campfire,
        "soul_campfire" to R.drawable.block_soul_campfire,
        "polished_diorite" to R.drawable.block_polished_diorite,
        "polished_andesite" to R.drawable.block_polished_andesite,
        "polished_deepslate" to R.drawable.block_polished_deepslate,
        "polished_blackstone" to R.drawable.block_polished_blackstone,
        "mossy_cobblestone" to R.drawable.block_mossy_cobblestone,
        "mossy_stone_bricks" to R.drawable.block_mossy_stone_bricks,
        "end_stone_bricks" to R.drawable.block_end_stone_bricks,
        "purpur_pillar" to R.drawable.block_purpur_pillar,
        "grindstone" to R.drawable.block_grindstone,
        "stonecutter" to R.drawable.block_stonecutter,
        "smithing_table" to R.drawable.block_smithing_table,
        "cartography_table" to R.drawable.block_cartography_table,
        "loom" to R.drawable.block_loom,
        "fletching_table" to R.drawable.block_fletching_table,
        "composter" to R.drawable.block_composter,
        "lectern" to R.drawable.block_lectern,
        "jukebox" to R.drawable.block_jukebox,
        "armor_stand" to R.drawable.block_armor_stand,
        "scaffolding" to R.drawable.block_scaffolding,
        "white_concrete" to R.drawable.block_white_concrete,
        "orange_concrete" to R.drawable.block_orange_concrete,
        "magenta_concrete" to R.drawable.block_magenta_concrete,
        "light_blue_concrete" to R.drawable.block_light_blue_concrete,
        "yellow_concrete" to R.drawable.block_yellow_concrete,
        "lime_concrete" to R.drawable.block_lime_concrete,
        "pink_concrete" to R.drawable.block_pink_concrete,
        "gray_concrete" to R.drawable.block_gray_concrete,
        "light_gray_concrete" to R.drawable.block_light_gray_concrete,
        "cyan_concrete" to R.drawable.block_cyan_concrete,
        "purple_concrete" to R.drawable.block_purple_concrete,
        "blue_concrete" to R.drawable.block_blue_concrete,
        "brown_concrete" to R.drawable.block_brown_concrete,
        "green_concrete" to R.drawable.block_green_concrete,
        "red_concrete" to R.drawable.block_red_concrete,
        "black_concrete" to R.drawable.block_black_concrete,
        "orange_wool" to R.drawable.block_orange_wool,
        "magenta_wool" to R.drawable.block_magenta_wool,
        "light_blue_wool" to R.drawable.block_light_blue_wool,
        "yellow_wool" to R.drawable.block_yellow_wool,
        "lime_wool" to R.drawable.block_lime_wool,
        "pink_wool" to R.drawable.block_pink_wool,
        "gray_wool" to R.drawable.block_gray_wool,
        "light_gray_wool" to R.drawable.block_light_gray_wool,
        "cyan_wool" to R.drawable.block_cyan_wool,
        "purple_wool" to R.drawable.block_purple_wool,
        "blue_wool" to R.drawable.block_blue_wool,
        "brown_wool" to R.drawable.block_brown_wool,
        "green_wool" to R.drawable.block_green_wool,
        "red_wool" to R.drawable.block_red_wool,
        "black_wool" to R.drawable.block_black_wool,

        // ── Additional direct mappings (no block left behind) ─────────────────

        // Anvil variants
        "chipped_anvil" to R.drawable.block_anvil,
        "damaged_anvil" to R.drawable.block_anvil,

        // Chest variants
        "trapped_chest" to R.drawable.block_chest,

        // Pumpkin variants
        "pumpkin" to R.drawable.block_carved_pumpkin_bk,
        "carved_pumpkin" to R.drawable.block_carved_pumpkin_bk,
        "jack_o_lantern" to R.drawable.block_jack_o_lantern_bk,

        // Stone/brick variants
        "polished_basalt" to R.drawable.block_polished_basalt_bk,
        "polished_blackstone_bricks" to R.drawable.block_polished_blackstone,
        "cracked_polished_blackstone_bricks" to R.drawable.block_polished_blackstone,
        "polished_tuff" to R.drawable.block_polished_tuff_bk,
        "dark_prismarine" to R.drawable.block_dark_prismarine_bk,
        "red_nether_bricks" to R.drawable.block_red_nether_bk,
        "deepslate_bricks" to R.drawable.block_deepslate_brick,
        "deepslate_tiles" to R.drawable.block_deepslate_brick,
        "cracked_deepslate_bricks" to R.drawable.block_deepslate_brick,
        "cracked_deepslate_tiles" to R.drawable.block_deepslate_brick,
        "tuff_bricks" to R.drawable.block_tuff_brick,
        "chiseled_tuff_bricks" to R.drawable.block_tuff_brick,
        "quartz_bricks" to R.drawable.block_quartz_variant,
        "quartz_pillar" to R.drawable.block_quartz_variant,
        "chiseled_copper" to R.drawable.block_copper_item,
        "reinforced_deepslate" to R.drawable.block_reinforced_deepslate_bk,
        "gilded_blackstone" to R.drawable.block_gilded_blackstone_bk,

        // Dirt variants
        "coarse_dirt" to R.drawable.block_coarse_dirt_bk,
        "dirt_path" to R.drawable.block_coarse_dirt_bk,
        "rooted_dirt" to R.drawable.block_coarse_dirt_bk,
        "farmland" to R.drawable.block_coarse_dirt_bk,

        // Sandstone variants
        "cut_sandstone" to R.drawable.block_cut_sandstone_bk,
        "cut_sandstone_slab" to R.drawable.block_cut_sandstone_bk,
        "cut_red_sandstone" to R.drawable.block_cut_red_sandstone_bk,
        "cut_red_sandstone_slab" to R.drawable.block_cut_red_sandstone_bk,

        // Raw ore blocks
        "raw_copper_block" to R.drawable.block_raw_copper_bk,
        "raw_gold_block" to R.drawable.block_raw_gold_bk,
        "raw_iron_block" to R.drawable.block_raw_iron_bk,

        // Copper functional blocks
        "copper_bars" to R.drawable.block_copper_item,
        "copper_bulb" to R.drawable.block_copper_item,
        "copper_chain" to R.drawable.block_copper_item,
        "copper_chest" to R.drawable.block_copper_item,
        "copper_door" to R.drawable.block_copper_item,
        "copper_golem_statue" to R.drawable.block_copper_item,
        "copper_grate" to R.drawable.block_copper_item,
        "copper_lantern" to R.drawable.block_copper_item,
        "copper_torch" to R.drawable.block_copper_item,
        "copper_trapdoor" to R.drawable.block_copper_item,
        "copper_wall_torch" to R.drawable.block_copper_item,

        // Nature blocks
        "bone_block" to R.drawable.block_bone_bk,
        "honeycomb_block" to R.drawable.block_honeycomb_bk,
        "dried_kelp_block" to R.drawable.block_dried_kelp_bk,
        "moss_block" to R.drawable.block_moss_bk,
        "moss_carpet" to R.drawable.block_moss_bk,
        "pale_moss_block" to R.drawable.block_pale_moss_bk,
        "pale_moss_carpet" to R.drawable.block_pale_moss_bk,
        "pale_hanging_moss" to R.drawable.block_pale_moss_bk,

        // Light/glow blocks
        "sea_lantern" to R.drawable.block_sea_lantern_bk,
        "frosted_ice" to R.drawable.block_frosted_ice_bk,
        "end_rod" to R.drawable.block_end_rod_bk,

        // Utility blocks
        "lodestone" to R.drawable.block_lodestone_bk,
        "respawn_anchor" to R.drawable.block_respawn_anchor_bk,
        "note_block" to R.drawable.block_jukebox,
        "target" to R.drawable.block_target_bk,
        "decorated_pot" to R.drawable.block_decorated_pot_bk,
        "daylight_detector" to R.drawable.block_oak_slab,
        "calibrated_sculk_sensor" to R.drawable.block_sculk_sensor,
        "crafter" to R.drawable.block_crafting_table,

        // Beehive/nest
        "beehive" to R.drawable.block_beehive_bk,
        "bee_nest" to R.drawable.block_beehive_bk,

        // Liquids and fluids
        "water" to R.drawable.block_water_still,
        "water_cauldron" to R.drawable.block_water_still,
        "lava" to R.drawable.block_lava_still,
        "lava_cauldron" to R.drawable.block_lava_still,
        "bubble_column" to R.drawable.block_bubble_column_bk,
        "powder_snow" to R.drawable.block_powder_snow_bk,
        "powder_snow_cauldron" to R.drawable.block_powder_snow_bk,

        // Fire
        "fire" to R.drawable.block_fire_bk,
        "soul_fire" to R.drawable.block_soul_fire_bk,

        // Snow
        "snow" to R.drawable.block_snow_layer,

        // Portals
        "nether_portal" to R.drawable.block_nether_portal_bk,
        "end_portal" to R.drawable.block_end_portal_bk,
        "end_portal_frame" to R.drawable.block_end_stone,

        // Amethyst
        "amethyst_cluster" to R.drawable.block_amethyst_bk,
        "budding_amethyst" to R.drawable.block_amethyst_block,
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

        // Flowers
        "blue_orchid" to R.drawable.block_flower_blue,
        "orange_tulip" to R.drawable.block_flower_orange,
        "pink_tulip" to R.drawable.block_flower_pink,
        "red_tulip" to R.drawable.block_flower_red,
        "white_tulip" to R.drawable.block_flower_white,
        "golden_dandelion" to R.drawable.block_flower_yellow,
        "open_eyeblossom" to R.drawable.block_flower_purple,
        "closed_eyeblossom" to R.drawable.block_flower_purple,
        "cactus_flower" to R.drawable.block_flower_pink,
        "pink_petals" to R.drawable.block_flower_pink,
        "wildflowers" to R.drawable.block_flower_blue,

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

        // Leaves and foliage
        "big_dripleaf" to R.drawable.block_leaf_bk,
        "big_dripleaf_stem" to R.drawable.block_vine_bk,
        "small_dripleaf" to R.drawable.block_leaf_bk,
        "leaf_litter" to R.drawable.block_coarse_dirt_bk,
        "short_dry_grass" to R.drawable.block_coarse_dirt_bk,
        "tall_dry_grass" to R.drawable.block_coarse_dirt_bk,
        "tall_seagrass" to R.drawable.block_vine_bk,

        // Dripstone
        "pointed_dripstone" to R.drawable.block_dripstone_bk,

        // Chorus
        "chorus_flower" to R.drawable.block_flower_purple,
        "chorus_plant" to R.drawable.block_flower_purple,

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
        "redstone_wall_torch" to R.drawable.block_redstone_torch,
        "wall_torch" to R.drawable.block_torch,
        "soul_wall_torch" to R.drawable.block_soul_torch,
        "heavy_weighted_pressure_plate" to R.drawable.block_iron_block,
        "light_weighted_pressure_plate" to R.drawable.block_gold_block,

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
        "light" to R.drawable.block_sea_lantern_bk,
        "structure_void" to R.drawable.block_command_bk,
        "structure_block" to R.drawable.block_command_bk,
        "jigsaw" to R.drawable.block_command_bk,
        "moving_piston" to R.drawable.block_piston,
        "test_block" to R.drawable.block_command_bk,
        "test_instance_block" to R.drawable.block_command_bk,

        // Misc blocks
        "candle_cake" to R.drawable.block_torch,
        "dried_ghast" to R.drawable.block_bone_bk,
        "muddy_mangrove_roots" to R.drawable.block_mud,
        "petrified_oak_slab" to R.drawable.block_oak_slab,
        "suspicious_gravel" to R.drawable.block_gravel,
        "suspicious_sand" to R.drawable.block_sand,
        "sea_pickle" to R.drawable.block_vine_bk,

        // Nether fungi
        "crimson_fungus" to R.drawable.block_crimson_stem,
        "warped_fungus" to R.drawable.block_warped_stem,

        // Froglights
        "ochre_froglight" to R.drawable.block_sea_lantern_bk,
        "pearlescent_froglight" to R.drawable.block_sea_lantern_bk,
        "verdant_froglight" to R.drawable.block_sea_lantern_bk,

        // Iron chain
        "iron_chain" to R.drawable.block_iron_block,

        // Polished blackstone variants
        "polished_blackstone_button" to R.drawable.block_polished_blackstone,
        "polished_blackstone_pressure_plate" to R.drawable.block_polished_blackstone,

        // Stone variants
        "stone_button" to R.drawable.block_stone,
        "stone_pressure_plate" to R.drawable.block_stone,

        // Smooth quartz
        "smooth_quartz" to R.drawable.block_quartz_variant,

        // Red sandstone slab variant
        "smooth_red_sandstone_slab" to R.drawable.block_cut_red_sandstone_bk,

        // Piston head
        "piston_head" to R.drawable.block_piston,
    )

    fun get(blockId: String): SpyglassIcon? {
        map[blockId]?.let { return SpyglassIcon.Drawable(it) }
        return resolveFallback(blockId)
    }

    /** Block-specific fallback: maps common variant patterns to a base block texture. */
    private fun resolveFallback(blockId: String): SpyglassIcon? {
        // Waxed / oxidation → copper_block
        if (blockId.startsWith("waxed_") || blockId.startsWith("exposed_") ||
            blockId.startsWith("weathered_") || blockId.startsWith("oxidized_") ||
            blockId.startsWith("cut_copper"))
            return SpyglassIcon.Drawable(map["copper_block"] ?: return null)

        // Colored block variants
        for (color in COLORS) {
            if (!blockId.startsWith("${color}_")) continue
            val suffix = blockId.removePrefix("${color}_")
            val res = when (suffix) {
                "wool" -> map["${color}_wool"] ?: map["wool"]
                "concrete", "concrete_powder" -> map["${color}_concrete"]
                "terracotta", "glazed_terracotta" -> map["terracotta"]
                "stained_glass" -> map["glass"]
                "stained_glass_pane" -> map["glass_pane"]
                "shulker_box" -> map["shulker_box"]
                "carpet", "bed", "banner", "wall_banner" -> map["${color}_wool"] ?: map["wool"]
                "candle", "candle_cake" -> map["torch"]
                else -> null
            }
            if (res != null) return SpyglassIcon.Drawable(res)
        }

        // Slab/stairs/wall → base material
        for (suffix in listOf("_slab", "_stairs", "_wall")) {
            if (!blockId.endsWith(suffix)) continue
            val base = blockId.removeSuffix(suffix)
            map[base]?.let { return SpyglassIcon.Drawable(it) }
            map["${base}s"]?.let { return SpyglassIcon.Drawable(it) }
            map["${base}_block"]?.let { return SpyglassIcon.Drawable(it) }
        }

        // Coral → prismarine
        if (blockId.contains("coral"))
            return SpyglassIcon.Drawable(map["prismarine"] ?: return null)

        return null
    }

    private val COLORS = listOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
        "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    )
}
