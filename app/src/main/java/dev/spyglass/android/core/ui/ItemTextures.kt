package dev.spyglass.android.core.ui

import dev.spyglass.android.R

/**
 * Unified texture lookup for all Minecraft items (blocks + non-block items).
 * Checks block textures first, then item textures.
 */
object ItemTextures {
    private val items = mapOf(
        "acacia_boat" to R.drawable.item_acacia_boat,
        "acacia_button" to R.drawable.item_acacia_button,
        "acacia_fence_gate" to R.drawable.item_acacia_fence_gate,
        "acacia_pressure_plate" to R.drawable.item_acacia_pressure_plate,
        "acacia_sign" to R.drawable.item_acacia_sign,
        "amethyst_shard" to R.drawable.item_amethyst_shard,
        "apple" to R.drawable.item_apple,
        "armadillo_scute" to R.drawable.item_armadillo_scute,
        "arrow" to R.drawable.item_arrow,
        "baked_potato" to R.drawable.item_baked_potato,
        "bamboo" to R.drawable.item_bamboo,
        "beetroot" to R.drawable.item_beetroot,
        "beetroot_seeds" to R.drawable.item_beetroot_seeds,
        "beetroot_soup" to R.drawable.item_mushroom_stew,
        "bamboo_boat" to R.drawable.item_bamboo_boat,
        "bamboo_raft" to R.drawable.item_bamboo_raft,
        "bamboo_button" to R.drawable.item_bamboo_button,
        "bamboo_door" to R.drawable.item_bamboo_door,
        "bamboo_fence" to R.drawable.item_bamboo_fence,
        "bamboo_fence_gate" to R.drawable.item_bamboo_fence_gate,
        "bamboo_pressure_plate" to R.drawable.item_bamboo_pressure_plate,
        "bamboo_sign" to R.drawable.item_bamboo_sign,
        "bamboo_slab" to R.drawable.item_bamboo_slab,
        "bamboo_stairs" to R.drawable.item_bamboo_stairs,
        "bamboo_trapdoor" to R.drawable.item_bamboo_trapdoor,
        "birch_boat" to R.drawable.item_birch_boat,
        "birch_button" to R.drawable.item_birch_button,
        "birch_fence_gate" to R.drawable.item_birch_fence_gate,
        "birch_pressure_plate" to R.drawable.item_birch_pressure_plate,
        "birch_sign" to R.drawable.item_birch_sign,
        "black_dye" to R.drawable.item_black_dye,
        "blaze_powder" to R.drawable.item_blaze_powder,
        "blaze_rod" to R.drawable.item_blaze_rod,
        "blue_dye" to R.drawable.item_blue_dye,
        "bone" to R.drawable.item_bone,
        "bone_meal" to R.drawable.item_bone_meal,
        "breeze_rod" to R.drawable.item_breeze_rod,
        "brush" to R.drawable.item_brush,
        "book" to R.drawable.item_book,
        "book_and_quill" to R.drawable.item_book_and_quill,
        "bottle_enchanting" to R.drawable.item_bottle_enchanting,
        "bowl" to R.drawable.item_bowl,
        "bow" to R.drawable.item_bow,
        "bread" to R.drawable.item_bread,
        "brewing_stand" to R.drawable.item_brewing_stand,
        "brick" to R.drawable.item_brick,
        "brown_dye" to R.drawable.item_brown_dye,
        "bucket" to R.drawable.item_bucket,
        "cactus" to R.drawable.item_cactus,
        "cake" to R.drawable.item_cake,
        "candle" to R.drawable.block_torch,
        "carrot" to R.drawable.item_carrot,
        "carrot_on_a_stick" to R.drawable.item_carrot_on_a_stick,
        "cauldron" to R.drawable.item_cauldron,
        "chainmail_boots" to R.drawable.item_chainmail_boots,
        "chainmail_chestplate" to R.drawable.item_chainmail_chestplate,
        "chainmail_helmet" to R.drawable.item_chainmail_helmet,
        "chainmail_leggings" to R.drawable.item_chainmail_leggings,
        "chorus_fruit" to R.drawable.item_chorus_fruit,
        "chain" to R.drawable.item_iron_bars,
        "charcoal" to R.drawable.item_charcoal,
        "cherry_boat" to R.drawable.item_cherry_boat,
        "cherry_button" to R.drawable.item_cherry_button,
        "cherry_door" to R.drawable.item_cherry_door,
        "cherry_fence" to R.drawable.item_cherry_fence,
        "cherry_fence_gate" to R.drawable.item_cherry_fence_gate,
        "cherry_pressure_plate" to R.drawable.item_cherry_pressure_plate,
        "cherry_sign" to R.drawable.item_cherry_sign,
        "cherry_slab" to R.drawable.item_cherry_slab,
        "cherry_stairs" to R.drawable.item_cherry_stairs,
        "cherry_trapdoor" to R.drawable.item_cherry_trapdoor,
        "clay_ball" to R.drawable.item_clay_ball,
        "clock" to R.drawable.item_clock,
        "coal" to R.drawable.item_coal,
        "coal_block" to R.drawable.item_coal_block,
        "cocoa_beans" to R.drawable.item_cocoa_beans,
        "compass" to R.drawable.item_compass,
        "cooked_beef" to R.drawable.item_cooked_beef,
        "cooked_chicken" to R.drawable.item_cooked_chicken,
        "cooked_cod" to R.drawable.item_cooked_cod,
        "cooked_mutton" to R.drawable.item_cooked_mutton,
        "cooked_porkchop" to R.drawable.item_cooked_porkchop,
        "cooked_rabbit" to R.drawable.item_cooked_rabbit,
        "cooked_salmon" to R.drawable.item_cooked_salmon,
        "cookie" to R.drawable.item_cookie,
        "copper_axe" to R.drawable.item_iron_axe,
        "copper_boots" to R.drawable.item_iron_boots,
        "copper_chestplate" to R.drawable.item_iron_chestplate,
        "copper_helmet" to R.drawable.item_iron_helmet,
        "copper_hoe" to R.drawable.item_iron_hoe,
        "copper_horse_armor" to R.drawable.item_iron_horse_armor,
        "copper_ingot" to R.drawable.item_copper_ingot,
        "copper_leggings" to R.drawable.item_iron_leggings,
        "copper_nugget" to R.drawable.item_copper_ingot,
        "copper_pickaxe" to R.drawable.item_iron_pickaxe,
        "copper_shovel" to R.drawable.item_iron_shovel,
        "copper_sword" to R.drawable.item_iron_sword,
        "crimson_button" to R.drawable.item_crimson_button,
        "crimson_fence_gate" to R.drawable.item_crimson_fence_gate,
        "crimson_planks" to R.drawable.item_crimson_planks,
        "crimson_pressure_plate" to R.drawable.item_crimson_pressure_plate,
        "crimson_sign" to R.drawable.item_crimson_sign,
        "crimson_slab" to R.drawable.item_crimson_slab,
        "crimson_stairs" to R.drawable.item_crimson_stairs,
        "crossbow" to R.drawable.item_crossbow,
        "cyan_dye" to R.drawable.item_cyan_dye,
        "dandelion" to R.drawable.item_dandelion,
        "dead_bush" to R.drawable.item_stick,
        "dark_oak_boat" to R.drawable.item_dark_oak_boat,
        "dark_oak_button" to R.drawable.item_dark_oak_button,
        "dark_oak_fence_gate" to R.drawable.item_dark_oak_fence_gate,
        "dark_oak_pressure_plate" to R.drawable.item_dark_oak_pressure_plate,
        "dark_oak_sign" to R.drawable.item_dark_oak_sign,
        "diamond" to R.drawable.item_diamond,
        "disc_fragment_5" to R.drawable.item_disc_fragment_5,
        "diamond_axe" to R.drawable.item_diamond_axe,
        "diamond_horse_armor" to R.drawable.item_diamond_horse_armor,
        "diamond_boots" to R.drawable.item_diamond_boots,
        "diamond_chestplate" to R.drawable.item_diamond_chestplate,
        "diamond_helmet" to R.drawable.item_diamond_helmet,
        "diamond_hoe" to R.drawable.item_diamond_hoe,
        "diamond_ingot" to R.drawable.item_diamond_ingot,
        "diamond_leggings" to R.drawable.item_diamond_leggings,
        "diamond_pickaxe" to R.drawable.item_diamond_pickaxe,
        "diamond_shovel" to R.drawable.item_diamond_shovel,
        "diamond_sword" to R.drawable.item_diamond_sword,
        "dragon_breath" to R.drawable.item_dragon_breath,
        "dragon_egg" to R.drawable.item_dragon_egg,
        "dried_kelp" to R.drawable.item_dried_kelp,
        "echo_shard" to R.drawable.item_echo_shard,
        "egg" to R.drawable.item_egg,
        "elytra" to R.drawable.item_elytra,
        "emerald" to R.drawable.item_emerald,
        "empty_map" to R.drawable.item_map,
        "ender_eye" to R.drawable.item_ender_eye,
        "enchanted_book" to R.drawable.item_enchanted_book,
        "end_gateway" to R.drawable.block_end_stone,
        "enchanted_golden_apple" to R.drawable.item_enchanted_golden_apple,
        "end_crystal" to R.drawable.item_end_crystal,
        "ender_pearl" to R.drawable.item_ender_pearl,
        "experience_bottle" to R.drawable.item_experience_bottle,
        "feather" to R.drawable.item_feather,
        "fern" to R.drawable.block_grass_block,
        "fermented_spider_eye" to R.drawable.item_fermented_spider_eye,
        "fire_charge" to R.drawable.item_fire_charge,
        "firework_rocket" to R.drawable.item_firework_rocket,
        "firework_star" to R.drawable.item_gunpowder,
        "fishing_rod" to R.drawable.item_fishing_rod,
        "flint" to R.drawable.item_flint,
        "flower_pot" to R.drawable.item_brick,
        "frogspawn" to R.drawable.item_egg,
        "flint_and_steel" to R.drawable.item_flint_and_steel,
        "ghast_tear" to R.drawable.item_ghast_tear,
        "glass_bottle" to R.drawable.item_glass_bottle,
        "glistering_melon" to R.drawable.item_glistering_melon_slice,
        "glistering_melon_slice" to R.drawable.item_glistering_melon_slice,
        "glow_berries" to R.drawable.item_glow_berries,
        "glow_item_frame" to R.drawable.item_item_frame,
        "glow_ink_sac" to R.drawable.item_glow_ink_sac,
        "glowstone_dust" to R.drawable.item_glowstone_dust,
        "goat_horn" to R.drawable.item_goat_horn,
        "gold_ingot" to R.drawable.item_gold_ingot,
        "gold_nugget" to R.drawable.item_gold_nugget,
        "golden_apple" to R.drawable.item_golden_apple,
        "golden_axe" to R.drawable.item_golden_axe,
        "golden_boots" to R.drawable.item_golden_boots,
        "golden_carrot" to R.drawable.item_golden_carrot,
        "golden_chestplate" to R.drawable.item_golden_chestplate,
        "golden_helmet" to R.drawable.item_golden_helmet,
        "golden_horse_armor" to R.drawable.item_golden_horse_armor,
        "golden_hoe" to R.drawable.item_golden_hoe,
        "golden_leggings" to R.drawable.item_golden_leggings,
        "golden_pickaxe" to R.drawable.item_golden_pickaxe,
        "golden_shovel" to R.drawable.item_golden_shovel,
        "golden_sword" to R.drawable.item_golden_sword,
        "green_dye" to R.drawable.item_green_dye,
        "gray_dye" to R.drawable.item_gray_dye,
        "gunpowder" to R.drawable.item_gunpowder,
        "heart_of_the_sea" to R.drawable.item_heart_of_the_sea,
        "heavy_core" to R.drawable.item_heavy_core,
        "honey_block" to R.drawable.item_honey_block,
        "honey_bottle" to R.drawable.item_honey_bottle,
        "honeycomb" to R.drawable.item_honey_bottle,
        "ink_sac" to R.drawable.item_ink_sac,
        "iron_axe" to R.drawable.item_iron_axe,
        "iron_horse_armor" to R.drawable.item_iron_horse_armor,
        "iron_bars" to R.drawable.item_iron_bars,
        "iron_boots" to R.drawable.item_iron_boots,
        "iron_chestplate" to R.drawable.item_iron_chestplate,
        "iron_helmet" to R.drawable.item_iron_helmet,
        "iron_hoe" to R.drawable.item_iron_hoe,
        "iron_ingot" to R.drawable.item_iron_ingot,
        "iron_leggings" to R.drawable.item_iron_leggings,
        "iron_nugget" to R.drawable.item_iron_nugget,
        "iron_pickaxe" to R.drawable.item_iron_pickaxe,
        "iron_shovel" to R.drawable.item_iron_shovel,
        "iron_sword" to R.drawable.item_iron_sword,
        "item_frame" to R.drawable.item_item_frame,
        "jungle_boat" to R.drawable.item_jungle_boat,
        "jungle_button" to R.drawable.item_jungle_button,
        "jungle_fence_gate" to R.drawable.item_jungle_fence_gate,
        "jungle_pressure_plate" to R.drawable.item_jungle_pressure_plate,
        "jungle_sign" to R.drawable.item_jungle_sign,
        "kelp" to R.drawable.item_kelp,
        "ladder" to R.drawable.item_ladder,
        "lapis_block" to R.drawable.item_lapis_lazuli,
        "large_fern" to R.drawable.block_grass_block,
        "lava_bucket" to R.drawable.item_lava_bucket,
        "lapis_lazuli" to R.drawable.item_lapis_lazuli,
        "lead" to R.drawable.item_lead,
        "leather" to R.drawable.item_leather,
        "leather_boots" to R.drawable.item_leather_boots,
        "leather_chestplate" to R.drawable.item_leather_chestplate,
        "leather_helmet" to R.drawable.item_leather_helmet,
        "leather_horse_armor" to R.drawable.item_leather_horse_armor,
        "leather_leggings" to R.drawable.item_leather_leggings,
        "leather_pants" to R.drawable.item_leather_leggings,
        "light_blue_dye" to R.drawable.item_light_blue_dye,
        "light_gray_dye" to R.drawable.item_light_gray_dye,
        "lightning_rod" to R.drawable.item_lightning_rod,
        "lime_dye" to R.drawable.item_lime_dye,
        "mace" to R.drawable.item_mace,
        "magenta_dye" to R.drawable.item_magenta_dye,
        "magma_cream" to R.drawable.item_magma_cream,
        "mangrove_boat" to R.drawable.item_mangrove_boat,
        "mangrove_button" to R.drawable.item_mangrove_button,
        "mangrove_door" to R.drawable.item_mangrove_door,
        "mangrove_fence" to R.drawable.item_mangrove_fence,
        "mangrove_fence_gate" to R.drawable.item_mangrove_fence_gate,
        "mangrove_pressure_plate" to R.drawable.item_mangrove_pressure_plate,
        "mangrove_sign" to R.drawable.item_mangrove_sign,
        "mangrove_slab" to R.drawable.item_mangrove_slab,
        "mangrove_stairs" to R.drawable.item_mangrove_stairs,
        "mangrove_trapdoor" to R.drawable.item_mangrove_trapdoor,
        "map" to R.drawable.item_map,
        "melon" to R.drawable.item_melon_slice,
        "melon_seeds" to R.drawable.item_melon_seeds,
        "melon_slice" to R.drawable.item_melon_slice,
        "milk_bucket" to R.drawable.item_milk_bucket,
        "minecart" to R.drawable.item_minecart,
        "brown_mushroom" to R.drawable.item_brown_mushroom,
        "red_mushroom" to R.drawable.item_red_mushroom,
        "mushroom_stem" to R.drawable.block_birch_log,
        "mushroom_stew" to R.drawable.item_mushroom_stew,
        "music_disc_13" to R.drawable.item_music_disc_13,
        "name_tag" to R.drawable.item_name_tag,
        "knowledge_book" to R.drawable.item_book,
        "nautilus_shell" to R.drawable.item_nautilus_shell,
        "nether_brick" to R.drawable.item_nether_brick,
        "nether_sprouts" to R.drawable.block_warped_stem,
        "nether_wart_block" to R.drawable.block_netherrack,
        "nether_star" to R.drawable.item_nether_star,
        "netherite_axe" to R.drawable.item_netherite_axe,
        "netherite_boots" to R.drawable.item_netherite_boots,
        "netherite_chestplate" to R.drawable.item_netherite_chestplate,
        "netherite_helmet" to R.drawable.item_netherite_helmet,
        "netherite_hoe" to R.drawable.item_netherite_hoe,
        "netherite_horse_armor" to R.drawable.item_diamond_horse_armor,
        "netherite_ingot" to R.drawable.item_netherite_ingot,
        "netherite_leggings" to R.drawable.item_netherite_leggings,
        "netherite_pickaxe" to R.drawable.item_netherite_pickaxe,
        "netherite_scrap" to R.drawable.item_netherite_scrap,
        "netherite_shovel" to R.drawable.item_netherite_shovel,
        "netherite_sword" to R.drawable.item_netherite_sword,
        "netherite_upgrade_template" to R.drawable.item_netherite_upgrade_template,
        "nether_wart" to R.drawable.item_nether_wart,
        "orange_dye" to R.drawable.item_orange_dye,
        "oak_boat" to R.drawable.item_oak_boat,
        "ominous_trial_key" to R.drawable.item_ominous_trial_key,
        "oak_button" to R.drawable.item_oak_button,
        "oak_fence_gate" to R.drawable.item_oak_fence_gate,
        "oak_pressure_plate" to R.drawable.item_oak_pressure_plate,
        "oak_sign" to R.drawable.item_oak_sign,
        "ocean_explorer_map" to R.drawable.item_map,
        "ocean_map" to R.drawable.item_map,
        "ominous_bottle" to R.drawable.item_experience_bottle,
        "painting" to R.drawable.item_painting,
        "pitcher_pod" to R.drawable.item_wheat_seeds,
        "poisonous_potato" to R.drawable.item_potato,
        "popped_chorus_fruit" to R.drawable.item_chorus_fruit,
        "paper" to R.drawable.item_paper,
        "phantom_membrane" to R.drawable.item_phantom_membrane,
        "pink_dye" to R.drawable.item_pink_dye,
        "poppy" to R.drawable.item_poppy,
        "potato" to R.drawable.item_potato,
        "powered_rail" to R.drawable.item_powered_rail,
        "prismarine_bricks" to R.drawable.item_prismarine_bricks,
        "prismarine_crystals" to R.drawable.item_prismarine_crystals,
        "prismarine_shard" to R.drawable.item_prismarine_shard,
        "pumpkin" to R.drawable.item_pumpkin,
        "pumpkin_pie" to R.drawable.item_pumpkin_pie,
        "pumpkin_seeds" to R.drawable.item_pumpkin_seeds,
        "pufferfish" to R.drawable.item_pufferfish,
        "purple_dye" to R.drawable.item_purple_dye,
        "quartz" to R.drawable.item_quartz,
        "rail" to R.drawable.item_rail,
        "rabbit_foot" to R.drawable.item_rabbit_foot,
        "rabbit_hide" to R.drawable.item_rabbit_hide,
        "rabbit_stew" to R.drawable.item_rabbit_stew,
        "raw_beef" to R.drawable.item_raw_beef,
        "raw_chicken" to R.drawable.item_raw_chicken,
        "raw_cod" to R.drawable.item_raw_cod,
        "raw_copper" to R.drawable.item_raw_copper,
        "raw_gold" to R.drawable.item_raw_gold,
        "raw_iron" to R.drawable.item_raw_iron,
        "raw_mutton" to R.drawable.item_raw_mutton,
        "raw_porkchop" to R.drawable.item_raw_porkchop,
        "raw_rabbit" to R.drawable.item_raw_rabbit,
        "raw_salmon" to R.drawable.item_raw_salmon,
        "red_dye" to R.drawable.item_red_dye,
        "redstone" to R.drawable.item_redstone,
        "recovery_compass" to R.drawable.item_compass,
        "redstone_dust" to R.drawable.item_redstone,
        "resin_brick" to R.drawable.item_resin_brick,
        "resin_clump" to R.drawable.item_resin_clump,
        "rotten_flesh" to R.drawable.item_rotten_flesh,
        "saddle" to R.drawable.item_saddle,
        "scute" to R.drawable.item_scute,
        "seagrass" to R.drawable.block_prismarine,
        "shears" to R.drawable.item_shears,
        "short_grass" to R.drawable.block_grass_block,
        "shield" to R.drawable.item_shield,
        "shulker_shell" to R.drawable.item_shulker_shell,
        "slime_ball" to R.drawable.item_slime_ball,
        "slime_block" to R.drawable.item_slime_block,
        "slimeball" to R.drawable.item_slimeball,
        "smooth_stone" to R.drawable.item_smooth_stone,
        "sniffer_egg" to R.drawable.item_sniffer_egg,
        "snowball" to R.drawable.item_snowball,
        "spectral_arrow" to R.drawable.item_spectral_arrow,
        "spruce_boat" to R.drawable.item_spruce_boat,
        "spruce_button" to R.drawable.item_spruce_button,
        "spruce_fence_gate" to R.drawable.item_spruce_fence_gate,
        "spruce_pressure_plate" to R.drawable.item_spruce_pressure_plate,
        "spruce_sign" to R.drawable.item_spruce_sign,
        "spider_eye" to R.drawable.item_spider_eye,
        "spyglass" to R.drawable.ic_launcher_foreground,
        "stick" to R.drawable.item_stick,
        "stone_axe" to R.drawable.item_stone_axe,
        "stone_hoe" to R.drawable.item_stone_hoe,
        "stone_pickaxe" to R.drawable.item_stone_pickaxe,
        "stone_shovel" to R.drawable.item_stone_shovel,
        "stone_sword" to R.drawable.item_stone_sword,
        "string" to R.drawable.item_string,
        "suspicious_stew" to R.drawable.item_suspicious_stew,
        "sweet_berries" to R.drawable.item_sweet_berries,
        "sweet_berry_bush" to R.drawable.item_sweet_berries,
        "tall_grass" to R.drawable.block_grass_block,
        "turtle_egg" to R.drawable.item_egg,
        "twisting_vines" to R.drawable.block_warped_stem,
        "tipped_arrow" to R.drawable.item_arrow,
        "tipped_arrow_poison" to R.drawable.item_tipped_arrow_poison,
        "tipped_arrow_slowness" to R.drawable.item_tipped_arrow_slowness,
        "torchflower_seeds" to R.drawable.item_wheat_seeds,
        "totem_of_undying" to R.drawable.item_totem_of_undying,
        "trial_key" to R.drawable.item_trial_key,
        "trident" to R.drawable.item_trident,
        "tripwire_hook" to R.drawable.item_tripwire_hook,
        "tropical_fish" to R.drawable.item_tropical_fish,
        "turtle_helmet" to R.drawable.item_turtle_helmet,
        "turtle_scute" to R.drawable.item_turtle_scute,
        "sugar" to R.drawable.item_sugar,
        "sugar_cane" to R.drawable.item_sugar_cane,
        "warped_fungus_on_a_stick" to R.drawable.item_carrot_on_a_stick,
        "warped_wart_block" to R.drawable.block_warped_stem,
        "warped_button" to R.drawable.item_warped_button,
        "warped_fence_gate" to R.drawable.item_warped_fence_gate,
        "warped_planks" to R.drawable.item_warped_planks,
        "warped_pressure_plate" to R.drawable.item_warped_pressure_plate,
        "warped_sign" to R.drawable.item_warped_sign,
        "warped_slab" to R.drawable.item_warped_slab,
        "warped_stairs" to R.drawable.item_warped_stairs,
        "water_bucket" to R.drawable.item_water_bucket,
        "weeping_vines" to R.drawable.block_crimson_stem,
        "wheat" to R.drawable.item_wheat,
        "wheat_seeds" to R.drawable.item_wheat_seeds,
        "white_banner" to R.drawable.item_white_wool,
        "white_bed" to R.drawable.item_white_bed,
        "white_concrete_powder" to R.drawable.item_white_concrete_powder,
        "white_dye" to R.drawable.item_white_dye,
        "white_wool" to R.drawable.item_white_wool,
        "wind_charge" to R.drawable.item_wind_charge,
        "wither_skeleton_skull" to R.drawable.item_wither_skeleton_skull,
        "wolf_armor" to R.drawable.item_wolf_armor,
        "woodland_explorer_map" to R.drawable.item_map,
        "woodland_map" to R.drawable.item_map,
        "writable_book" to R.drawable.item_book_and_quill,
        "written_book" to R.drawable.item_book,
        "wooden_axe" to R.drawable.item_wooden_axe,
        "wooden_hoe" to R.drawable.item_wooden_hoe,
        "wooden_pickaxe" to R.drawable.item_wooden_pickaxe,
        "wooden_shovel" to R.drawable.item_wooden_shovel,
        "wooden_sword" to R.drawable.item_wooden_sword,
        "yellow_dye" to R.drawable.item_yellow_dye,
    )

    // ── Smart variant resolution constants ───────────────────────────────────

    private val COLORS = listOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
        "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    )

    private val WOOD_TYPES = listOf(
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
        "mangrove", "cherry", "bamboo", "pale_oak",
    )

    private val NETHER_WOOD = listOf("crimson", "warped")

    private val resolveCache = mutableMapOf<String, SpyglassIcon?>()

    /** Look up a texture for any item ID. Checks block textures, item textures, then smart fallback. */
    fun get(itemId: String): SpyglassIcon? {
        BlockTextures.get(itemId)?.let { return it }
        items[itemId]?.let { return SpyglassIcon.Drawable(it) }
        return resolveCache.getOrPut(itemId) { resolveVariant(itemId) }
    }

    /** Check both maps for a derived ID. */
    private fun lookup(id: String): SpyglassIcon? {
        BlockTextures.get(id)?.let { return it }
        items[id]?.let { return SpyglassIcon.Drawable(it) }
        return null
    }

    // ── Smart variant resolution ─────────────────────────────────────────────

    private fun resolveVariant(itemId: String): SpyglassIcon? {
        // Copper oxidation / waxed / cut variants
        if (itemId.startsWith("waxed_") || itemId.startsWith("exposed_") ||
            itemId.startsWith("weathered_") || itemId.startsWith("oxidized_") ||
            itemId.startsWith("cut_copper"))
            return lookup("copper_block")

        // Infested → normal block
        if (itemId.startsWith("infested_"))
            return lookup(itemId.removePrefix("infested_")) ?: lookup("stone")

        // Known color variants (carpet, bed, stained glass, etc.)
        resolveColorVariant(itemId)?.let { return it }

        // Wood type variants (stairs, slab, sign, hanging_sign, etc.)
        resolveWoodVariant(itemId)?.let { return it }

        // General material suffix (stairs, slab, wall, fence, etc.)
        resolveSuffix(itemId)?.let { return it }

        // Prefix variants (cracked, chiseled, smooth)
        resolvePrefixVariant(itemId)?.let { return it }

        // Deepslate ores
        if (itemId.startsWith("deepslate_") && itemId.endsWith("_ore"))
            return lookup("deepslate")

        // Coral → prismarine
        if (itemId.contains("coral")) return lookup("prismarine")

        // Skulls/heads
        if (itemId.endsWith("_head") || itemId.endsWith("_skull") ||
            itemId.endsWith("_wall_head") || itemId.endsWith("_wall_skull"))
            return lookup("soul_sand")

        // Pottery sherds → clay ball
        if (itemId.endsWith("_pottery_sherd")) return lookup("clay_ball")

        // Music discs → disc_13
        if (itemId.startsWith("music_disc_")) return lookup("music_disc_13")

        // Smithing templates → netherite upgrade template
        if (itemId.endsWith("_smithing_template")) return lookup("netherite_upgrade_template")

        // Banner patterns → paper
        if (itemId.endsWith("_banner_pattern")) return lookup("paper")

        // Mob buckets → bucket
        if (itemId.endsWith("_bucket") && itemId != "bucket") return lookup("bucket")

        // Minecart variants → minecart
        if (itemId.endsWith("_minecart")) return lookup("minecart")

        // Hanging signs → sign of same wood
        if (itemId.endsWith("_hanging_sign")) {
            val wood = itemId.removeSuffix("_hanging_sign")
            return lookup("${wood}_sign") ?: lookup("oak_sign")
        }

        // Boat/raft variants
        if (itemId.endsWith("_boat") || itemId.endsWith("_raft"))
            return lookup("oak_boat")

        // Potion items
        if (itemId == "potion" || itemId == "splash_potion" || itemId == "lingering_potion")
            return SpyglassIcon.Drawable(R.drawable.item_potion)

        // Spawn eggs → egg
        if (itemId.endsWith("_spawn_egg")) return lookup("egg")

        // Bundles → leather
        if (itemId == "bundle" || itemId.endsWith("_bundle")) return lookup("leather")

        // Spears → trident (closest weapon shape)
        if (itemId.endsWith("_spear")) return lookup("trident")

        // Nautilus armor → matching tier chestplate
        if (itemId.endsWith("_nautilus_armor")) {
            val tier = itemId.removeSuffix("_nautilus_armor")
            return lookup("${tier}_chestplate") ?: lookup("iron_chestplate")
        }

        // Colored eggs → egg
        if (itemId.endsWith("_egg") && itemId != "dragon_egg") return lookup("egg")

        // Flowers and small plants → poppy/dandelion fallback
        if (itemId in setOf(
                "allium", "azure_bluet", "oxeye_daisy", "cornflower",
                "lily_of_the_valley", "wither_rose", "torchflower", "pitcher_plant",
                "spore_blossom", "lilac", "rose_bush", "peony", "sunflower",
                "blue_orchid", "orange_tulip", "pink_tulip", "red_tulip", "white_tulip",
                "golden_dandelion", "open_eyeblossom", "closed_eyeblossom",
                "cactus_flower", "pink_petals", "wildflowers",
            ))
            return lookup("poppy") ?: lookup("dandelion")

        return null
    }

    private fun resolveColorVariant(itemId: String): SpyglassIcon? {
        for (color in COLORS) {
            if (!itemId.startsWith("${color}_")) continue
            return when (itemId.removePrefix("${color}_")) {
                "carpet", "bed", "banner" -> lookup("${color}_wool") ?: lookup("wool")
                "stained_glass" -> lookup("glass")
                "stained_glass_pane" -> lookup("glass_pane")
                "concrete_powder" -> lookup("${color}_concrete")
                "glazed_terracotta" -> lookup("terracotta")
                "terracotta" -> lookup("terracotta")
                "shulker_box" -> lookup("shulker_box")
                "candle" -> lookup("torch")
                "tulip", "orchid" -> lookup("poppy")
                "petals" -> lookup("poppy")
                "harness" -> lookup("saddle")
                else -> null
            }
        }
        return null
    }

    private fun resolveWoodVariant(itemId: String): SpyglassIcon? {
        for (wood in WOOD_TYPES + NETHER_WOOD) {
            val isNether = wood in NETHER_WOOD
            val isMatch = itemId.startsWith("${wood}_") || itemId == wood
            val isStripped = itemId.startsWith("stripped_${wood}_")
            if (!isMatch && !isStripped) continue

            val planks = lookup("${wood}_planks")
            val log = if (isNether) lookup("${wood}_stem") else lookup("${wood}_log")
            val strippedLog = if (isNether) lookup("stripped_${wood}_stem") else lookup("stripped_${wood}_log")

            val result = when {
                isStripped -> strippedLog ?: log
                itemId.endsWith("_wood") || itemId.endsWith("_hyphae") -> log
                itemId.endsWith("_mosaic") -> planks
                itemId.endsWith("_stairs") -> lookup("${wood}_stairs") ?: planks
                itemId.endsWith("_slab") -> lookup("${wood}_slab") ?: planks
                itemId.endsWith("_fence_gate") -> lookup("${wood}_fence_gate") ?: lookup("${wood}_fence") ?: planks
                itemId.endsWith("_fence") -> lookup("${wood}_fence") ?: planks
                itemId.endsWith("_door") -> lookup("${wood}_door") ?: planks
                itemId.endsWith("_trapdoor") -> lookup("${wood}_trapdoor") ?: planks
                itemId.endsWith("_sign") || itemId.endsWith("_hanging_sign") -> lookup("${wood}_sign") ?: planks
                itemId.endsWith("_button") -> lookup("${wood}_button") ?: planks
                itemId.endsWith("_pressure_plate") -> lookup("${wood}_pressure_plate") ?: planks
                itemId.endsWith("_shelf") -> lookup("bookshelf") ?: planks
                itemId.endsWith("_leaves") -> lookup("${wood}_leaves") ?: lookup("oak_leaves")
                itemId.endsWith("_sapling") || itemId.endsWith("_propagule") -> lookup("${wood}_leaves") ?: lookup("oak_leaves")
                itemId.endsWith("_roots") -> log ?: lookup("netherrack")
                itemId.endsWith("_nylium") -> lookup("netherrack")
                itemId.endsWith("_log") || itemId.endsWith("_stem") -> log
                itemId.endsWith("_planks") -> planks
                else -> null
            }
            if (result != null) return result
        }
        return null
    }

    private fun resolveSuffix(itemId: String): SpyglassIcon? {
        for (suffix in listOf("_stairs", "_slab", "_wall", "_fence", "_fence_gate", "_button", "_pressure_plate")) {
            if (!itemId.endsWith(suffix)) continue
            val base = itemId.removeSuffix(suffix)

            lookup(base)?.let { return it }
            lookup("${base}s")?.let { return it }
            lookup("${base}_block")?.let { return it }

            // Try parent material (strip last segment)
            val lastUnderscore = base.lastIndexOf('_')
            if (lastUnderscore > 0) {
                val parent = base.substring(0, lastUnderscore)
                lookup(parent)?.let { return it }
                lookup("${parent}s")?.let { return it }
            }

            // Try stripping known prefixes
            for (prefix in listOf("dark_", "smooth_", "polished_", "mossy_", "chiseled_", "cracked_", "red_")) {
                if (base.startsWith(prefix)) {
                    val stripped = base.removePrefix(prefix)
                    lookup(stripped)?.let { return it }
                    lookup("${stripped}s")?.let { return it }
                }
            }

            return null
        }
        return null
    }

    private fun resolvePrefixVariant(itemId: String): SpyglassIcon? {
        for (prefix in listOf("cracked_", "chiseled_", "smooth_")) {
            if (!itemId.startsWith(prefix)) continue
            val base = itemId.removePrefix(prefix)

            lookup(base)?.let { return it }
            lookup("${base}s")?.let { return it }

            val lastUnderscore = base.lastIndexOf('_')
            if (lastUnderscore > 0) {
                val parent = base.substring(0, lastUnderscore)
                lookup(parent)?.let { return it }
                lookup("${parent}s")?.let { return it }
            }

            return null
        }
        return null
    }
}
