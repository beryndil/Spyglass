package dev.spyglass.android.core.ui

import dev.spyglass.android.R

/**
 * Unified texture lookup for all Minecraft items (blocks + non-block items).
 * Checks block textures first, then item textures.
 */
object ItemTextures {
    /**
     * Item ID → PNG filename (without extension).
     * Resolved via [TextureManager] when textures are downloaded.
     */
    private val textureMap = mapOf(
        "acacia_boat" to "item_acacia_boat",
        "amethyst_shard" to "item_amethyst_shard",
        "apple" to "item_apple",
        "armadillo_scute" to "item_armadillo_scute",
        "arrow" to "item_arrow",
        "baked_potato" to "item_baked_potato",
        "bamboo" to "item_bamboo",
        "beetroot" to "item_beetroot",
        "beetroot_seeds" to "item_beetroot_seeds",
        "beetroot_soup" to "item_mushroom_stew",
        "bamboo_boat" to "item_bamboo_boat",
        "bamboo_raft" to "item_bamboo_raft",
        "birch_boat" to "item_birch_boat",
        "black_dye" to "item_black_dye",
        "blaze_powder" to "item_blaze_powder",
        "blaze_rod" to "item_blaze_rod",
        "blue_dye" to "item_blue_dye",
        "bone" to "item_bone",
        "bone_meal" to "item_bone_meal",
        "breeze_rod" to "item_breeze_rod",
        "brush" to "item_brush",
        "book" to "item_book",
        "book_and_quill" to "item_book_and_quill",
        "bottle_enchanting" to "item_bottle_enchanting",
        "bowl" to "item_bowl",
        "bow" to "item_bow",
        "bread" to "item_bread",
        "brewing_stand" to "item_brewing_stand",
        "brick" to "item_brick",
        "brown_dye" to "item_brown_dye",
        "bucket" to "item_bucket",
        "cactus" to "item_cactus",
        "cake" to "item_cake",
        "carrot" to "item_carrot",
        "carrot_on_a_stick" to "item_carrot_on_a_stick",
        "cauldron" to "item_cauldron",
        "chainmail_boots" to "item_chainmail_boots",
        "chainmail_chestplate" to "item_chainmail_chestplate",
        "chainmail_helmet" to "item_chainmail_helmet",
        "chainmail_leggings" to "item_chainmail_leggings",
        "chorus_fruit" to "item_chorus_fruit",
        "chain" to "item_iron_bars",
        "charcoal" to "item_charcoal",
        "cherry_boat" to "item_cherry_boat",
        "clay_ball" to "item_clay_ball",
        "clock" to "item_clock",
        "coal" to "item_coal",
        "coal_block" to "item_coal_block",
        "cocoa_beans" to "item_cocoa_beans",
        "compass" to "item_compass",
        "cooked_beef" to "item_cooked_beef",
        "cooked_chicken" to "item_cooked_chicken",
        "cooked_cod" to "item_cooked_cod",
        "cooked_mutton" to "item_cooked_mutton",
        "cooked_porkchop" to "item_cooked_porkchop",
        "cooked_rabbit" to "item_cooked_rabbit",
        "cooked_salmon" to "item_cooked_salmon",
        "cookie" to "item_cookie",
        "copper_axe" to "item_iron_axe",
        "copper_boots" to "item_iron_boots",
        "copper_chestplate" to "item_iron_chestplate",
        "copper_helmet" to "item_iron_helmet",
        "copper_hoe" to "item_iron_hoe",
        "copper_horse_armor" to "item_iron_horse_armor",
        "copper_ingot" to "item_copper_ingot",
        "copper_leggings" to "item_iron_leggings",
        "copper_nugget" to "item_copper_ingot",
        "copper_pickaxe" to "item_iron_pickaxe",
        "copper_shovel" to "item_iron_shovel",
        "copper_sword" to "item_iron_sword",
        "crimson_planks" to "item_crimson_planks",
        "crossbow" to "item_crossbow",
        "cyan_dye" to "item_cyan_dye",
        "dandelion" to "item_dandelion",
        "dead_bush" to "item_stick",
        "dark_oak_boat" to "item_dark_oak_boat",
        "diamond" to "item_diamond",
        "disc_fragment_5" to "item_disc_fragment_5",
        "diamond_axe" to "item_diamond_axe",
        "diamond_horse_armor" to "item_diamond_horse_armor",
        "diamond_boots" to "item_diamond_boots",
        "diamond_chestplate" to "item_diamond_chestplate",
        "diamond_helmet" to "item_diamond_helmet",
        "diamond_hoe" to "item_diamond_hoe",
        "diamond_ingot" to "item_diamond_ingot",
        "diamond_leggings" to "item_diamond_leggings",
        "diamond_pickaxe" to "item_diamond_pickaxe",
        "diamond_shovel" to "item_diamond_shovel",
        "diamond_sword" to "item_diamond_sword",
        "dragon_breath" to "item_dragon_breath",
        "dragon_egg" to "item_dragon_egg",
        "dried_kelp" to "item_dried_kelp",
        "echo_shard" to "item_echo_shard",
        "egg" to "item_egg",
        "elytra" to "item_elytra",
        "emerald" to "item_emerald",
        "empty_map" to "item_map",
        "ender_eye" to "item_ender_eye",
        "enchanted_book" to "item_enchanted_book",
        "enchanted_golden_apple" to "item_enchanted_golden_apple",
        "end_crystal" to "item_end_crystal",
        "ender_pearl" to "item_ender_pearl",
        "experience_bottle" to "item_experience_bottle",
        "feather" to "item_feather",
        "fermented_spider_eye" to "item_fermented_spider_eye",
        "fire_charge" to "item_fire_charge",
        "firework_rocket" to "item_firework_rocket",
        "firework_star" to "item_gunpowder",
        "fishing_rod" to "item_fishing_rod",
        "flint" to "item_flint",
        "flower_pot" to "item_brick",
        "frogspawn" to "item_egg",
        "flint_and_steel" to "item_flint_and_steel",
        "ghast_tear" to "item_ghast_tear",
        "glass_bottle" to "item_glass_bottle",
        "glistering_melon" to "item_glistering_melon_slice",
        "glistering_melon_slice" to "item_glistering_melon_slice",
        "glow_berries" to "item_glow_berries",
        "glow_item_frame" to "item_item_frame",
        "glow_ink_sac" to "item_glow_ink_sac",
        "glowstone_dust" to "item_glowstone_dust",
        "goat_horn" to "item_goat_horn",
        "gold_ingot" to "item_gold_ingot",
        "gold_nugget" to "item_gold_nugget",
        "golden_apple" to "item_golden_apple",
        "golden_axe" to "item_golden_axe",
        "golden_boots" to "item_golden_boots",
        "golden_carrot" to "item_golden_carrot",
        "golden_chestplate" to "item_golden_chestplate",
        "golden_helmet" to "item_golden_helmet",
        "golden_horse_armor" to "item_golden_horse_armor",
        "golden_hoe" to "item_golden_hoe",
        "golden_leggings" to "item_golden_leggings",
        "golden_pickaxe" to "item_golden_pickaxe",
        "golden_shovel" to "item_golden_shovel",
        "golden_sword" to "item_golden_sword",
        "green_dye" to "item_green_dye",
        "gray_dye" to "item_gray_dye",
        "gunpowder" to "item_gunpowder",
        "heart_of_the_sea" to "item_heart_of_the_sea",
        "heavy_core" to "item_heavy_core",
        "honey_block" to "item_honey_block",
        "honey_bottle" to "item_honey_bottle",
        "honeycomb" to "item_honey_bottle",
        "ink_sac" to "item_ink_sac",
        "iron_axe" to "item_iron_axe",
        "iron_horse_armor" to "item_iron_horse_armor",
        "iron_bars" to "item_iron_bars",
        "iron_boots" to "item_iron_boots",
        "iron_chestplate" to "item_iron_chestplate",
        "iron_helmet" to "item_iron_helmet",
        "iron_hoe" to "item_iron_hoe",
        "iron_ingot" to "item_iron_ingot",
        "iron_leggings" to "item_iron_leggings",
        "iron_nugget" to "item_iron_nugget",
        "iron_pickaxe" to "item_iron_pickaxe",
        "iron_shovel" to "item_iron_shovel",
        "iron_sword" to "item_iron_sword",
        "item_frame" to "item_item_frame",
        "jungle_boat" to "item_jungle_boat",
        "kelp" to "item_kelp",
        "ladder" to "item_ladder",
        "lapis_block" to "item_lapis_lazuli",
        "lava_bucket" to "item_lava_bucket",
        "lapis_lazuli" to "item_lapis_lazuli",
        "lead" to "item_lead",
        "leather" to "item_leather",
        "leather_boots" to "item_leather_boots",
        "leather_chestplate" to "item_leather_chestplate",
        "leather_helmet" to "item_leather_helmet",
        "leather_horse_armor" to "item_leather_horse_armor",
        "leather_leggings" to "item_leather_leggings",
        "leather_pants" to "item_leather_leggings",
        "light_blue_dye" to "item_light_blue_dye",
        "light_gray_dye" to "item_light_gray_dye",
        "lightning_rod" to "item_lightning_rod",
        "lime_dye" to "item_lime_dye",
        "mace" to "item_mace",
        "magenta_dye" to "item_magenta_dye",
        "magma_cream" to "item_magma_cream",
        "mangrove_boat" to "item_mangrove_boat",
        "map" to "item_map",
        "melon" to "item_melon_slice",
        "melon_seeds" to "item_melon_seeds",
        "melon_slice" to "item_melon_slice",
        "milk_bucket" to "item_milk_bucket",
        "minecart" to "item_minecart",
        "brown_mushroom" to "item_brown_mushroom",
        "red_mushroom" to "item_red_mushroom",
        "mushroom_stew" to "item_mushroom_stew",
        "music_disc_13" to "item_music_disc_13",
        "name_tag" to "item_name_tag",
        "knowledge_book" to "item_book",
        "nautilus_shell" to "item_nautilus_shell",
        "nether_brick" to "item_nether_brick",
        "nether_star" to "item_nether_star",
        "netherite_axe" to "item_netherite_axe",
        "netherite_boots" to "item_netherite_boots",
        "netherite_chestplate" to "item_netherite_chestplate",
        "netherite_helmet" to "item_netherite_helmet",
        "netherite_hoe" to "item_netherite_hoe",
        "netherite_horse_armor" to "item_diamond_horse_armor",
        "netherite_ingot" to "item_netherite_ingot",
        "netherite_leggings" to "item_netherite_leggings",
        "netherite_pickaxe" to "item_netherite_pickaxe",
        "netherite_scrap" to "item_netherite_scrap",
        "netherite_shovel" to "item_netherite_shovel",
        "netherite_sword" to "item_netherite_sword",
        "netherite_upgrade_template" to "item_netherite_upgrade_template",
        "nether_wart" to "item_nether_wart",
        "orange_dye" to "item_orange_dye",
        "oak_boat" to "item_oak_boat",
        "ominous_trial_key" to "item_ominous_trial_key",
        "ocean_explorer_map" to "item_map",
        "ocean_map" to "item_map",
        "ominous_bottle" to "item_experience_bottle",
        "painting" to "item_painting",
        "pitcher_pod" to "item_wheat_seeds",
        "poisonous_potato" to "item_potato",
        "popped_chorus_fruit" to "item_chorus_fruit",
        "paper" to "item_paper",
        "phantom_membrane" to "item_phantom_membrane",
        "pink_dye" to "item_pink_dye",
        "poppy" to "item_poppy",
        "potato" to "item_potato",
        "powered_rail" to "item_powered_rail",
        "prismarine_bricks" to "item_prismarine_bricks",
        "prismarine_crystals" to "item_prismarine_crystals",
        "prismarine_shard" to "item_prismarine_shard",
        "pumpkin" to "item_pumpkin",
        "pumpkin_pie" to "item_pumpkin_pie",
        "pumpkin_seeds" to "item_pumpkin_seeds",
        "pufferfish" to "item_pufferfish",
        "purple_dye" to "item_purple_dye",
        "quartz" to "item_quartz",
        "rail" to "item_rail",
        "rabbit_foot" to "item_rabbit_foot",
        "rabbit_hide" to "item_rabbit_hide",
        "rabbit_stew" to "item_rabbit_stew",
        "raw_beef" to "item_raw_beef",
        "raw_chicken" to "item_raw_chicken",
        "raw_cod" to "item_raw_cod",
        "raw_copper" to "item_raw_copper",
        "raw_gold" to "item_raw_gold",
        "raw_iron" to "item_raw_iron",
        "raw_mutton" to "item_raw_mutton",
        "raw_porkchop" to "item_raw_porkchop",
        "raw_rabbit" to "item_raw_rabbit",
        "raw_salmon" to "item_raw_salmon",
        "red_dye" to "item_red_dye",
        "redstone" to "item_redstone",
        "recovery_compass" to "item_compass",
        "redstone_dust" to "item_redstone",
        "resin_brick" to "item_resin_brick",
        "resin_clump" to "item_resin_clump",
        "rotten_flesh" to "item_rotten_flesh",
        "saddle" to "item_saddle",
        "scute" to "item_scute",
        "shears" to "item_shears",
        "shield" to "item_shield",
        "shulker_shell" to "item_shulker_shell",
        "slime_ball" to "item_slime_ball",
        "slime_block" to "item_slime_block",
        "slimeball" to "item_slimeball",
        "smooth_stone" to "item_smooth_stone",
        "sniffer_egg" to "item_sniffer_egg",
        "snowball" to "item_snowball",
        "spectral_arrow" to "item_spectral_arrow",
        "spruce_boat" to "item_spruce_boat",
        "spider_eye" to "item_spider_eye",
        "stick" to "item_stick",
        "stone_axe" to "item_stone_axe",
        "stone_hoe" to "item_stone_hoe",
        "stone_pickaxe" to "item_stone_pickaxe",
        "stone_shovel" to "item_stone_shovel",
        "stone_sword" to "item_stone_sword",
        "string" to "item_string",
        "suspicious_stew" to "item_suspicious_stew",
        "sweet_berries" to "item_sweet_berries",
        "sweet_berry_bush" to "item_sweet_berries",
        "tipped_arrow" to "item_arrow",
        "tipped_arrow_poison" to "item_tipped_arrow_poison",
        "tipped_arrow_slowness" to "item_tipped_arrow_slowness",
        "torchflower_seeds" to "item_wheat_seeds",
        "totem_of_undying" to "item_totem_of_undying",
        "trial_key" to "item_trial_key",
        "trident" to "item_trident",
        "tripwire_hook" to "item_tripwire_hook",
        "tropical_fish" to "item_tropical_fish",
        "turtle_helmet" to "item_turtle_helmet",
        "turtle_scute" to "item_turtle_scute",
        "sugar" to "item_sugar",
        "sugar_cane" to "item_sugar_cane",
        "warped_fungus_on_a_stick" to "item_carrot_on_a_stick",
        "warped_planks" to "item_warped_planks",
        "water_bucket" to "item_water_bucket",
        "wheat" to "item_wheat",
        "wheat_seeds" to "item_wheat_seeds",
        "white_banner" to "item_white_wool",
        "white_bed" to "item_white_bed",
        "white_concrete_powder" to "item_white_concrete_powder",
        "white_dye" to "item_white_dye",
        "white_wool" to "item_white_wool",
        "wind_charge" to "item_wind_charge",
        "wither_skeleton_skull" to "item_wither_skeleton_skull",
        "wolf_armor" to "item_wolf_armor",
        "woodland_explorer_map" to "item_map",
        "woodland_map" to "item_map",
        "writable_book" to "item_book_and_quill",
        "written_book" to "item_book",
        "wooden_axe" to "item_wooden_axe",
        "wooden_hoe" to "item_wooden_hoe",
        "wooden_pickaxe" to "item_wooden_pickaxe",
        "wooden_shovel" to "item_wooden_shovel",
        "wooden_sword" to "item_wooden_sword",
        "yellow_dye" to "item_yellow_dye",
        "potion" to "item_potion",
        "splash_potion" to "item_potion",
        "lingering_potion" to "item_potion",
    )

    /**
     * Items that reference block textures (not their own PNGs).
     * These resolve through [BlockTextures].
     */
    private val blockRefs = setOf(
        "candle", "fern", "large_fern", "seagrass", "short_grass", "tall_grass",
        "nether_sprouts", "nether_wart_block", "warped_wart_block",
        "weeping_vines", "twisting_vines", "mushroom_stem", "end_gateway",
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
        // Block ref items that always go through BlockTextures
        if (itemId in blockRefs) {
            BlockTextures.get(itemId)?.let { return it }
        }
        // Shape overlay: material texture clipped to shape mask + frame
        resolveShapeOverlay(itemId)?.let { return it }
        // Check block textures first
        BlockTextures.get(itemId)?.let { return it }
        // Check item texture map
        textureMap[itemId]?.let { filename ->
            TextureManager.resolveOrBundled(filename)?.let { return it }
        }
        // Special: spyglass uses the launcher icon (always bundled)
        if (itemId == "spyglass") return SpyglassIcon.Drawable(R.drawable.ic_launcher_foreground)
        // Smart fallback
        return resolveCache.getOrPut(itemId) { resolveVariant(itemId) }
    }

    /** Check both maps for a derived ID. */
    private fun lookup(id: String): SpyglassIcon? {
        BlockTextures.get(id)?.let { return it }
        textureMap[id]?.let { filename ->
            TextureManager.resolveOrBundled(filename)?.let { return it }
        }
        return null
    }

    // ── Shape overlay resolution ────────────────────────────────────────────

    /** Suffix → shape name, checked longest-first so `_wall_hanging_sign` matches before `_sign`. */
    private val SHAPE_SUFFIXES = listOf(
        "_wall_hanging_sign" to "hanging_sign",
        "_pressure_plate" to "pressure_plate",
        "_wall_sign" to "sign",
        "_hanging_sign" to "hanging_sign",
        "_fence_gate" to "fence_gate",
        "_trapdoor" to "trapdoor",
        "_button" to "button",
        "_stairs" to "stairs",
        "_shelf" to "shelf",
        "_fence" to "fence",
        "_door" to "door",
        "_slab" to "slab",
        "_sign" to "sign",
        "_wall" to "wall",
    )

    private fun resolveShapeOverlay(itemId: String): SpyglassIcon? {
        for ((suffix, shape) in SHAPE_SUFFIXES) {
            if (!itemId.endsWith(suffix)) continue
            val base = itemId.removeSuffix(suffix)
            val materialId = resolveMaterial(base) ?: return null
            val texture = lookup(materialId) ?: return null
            val mask = TextureManager.resolveOrBundled("block_${shape}_mask") ?: return null
            val frame = TextureManager.resolveOrBundled("block_${shape}_frame") ?: return null
            return SpyglassIcon.Overlay(texture, mask, frame)
        }
        return null
    }

    /** Map a shape's base name to the block ID of its material texture. */
    private fun resolveMaterial(base: String): String? {
        // Special cases
        when (base) {
            "iron" -> return "iron_block"
            "copper" -> return "copper_block"
            "heavy_weighted" -> return "iron_block"
            "light_weighted" -> return "gold_block"
            "petrified_oak" -> return "oak_planks"
            "bamboo_mosaic" -> return "bamboo_planks"
            "stone" -> return "stone"
            "polished_blackstone" -> return "polished_blackstone"
        }
        // Waxed / oxidized copper
        if (base.startsWith("waxed_") || base.startsWith("exposed_") ||
            base.startsWith("weathered_") || base.startsWith("oxidized_")) return "copper_block"

        // Wood: ${base}_planks
        if (lookup("${base}_planks") != null) return "${base}_planks"
        // Bricks: ${base}s  (brick→bricks, nether_brick→nether_bricks, stone_brick→stone_bricks)
        if (lookup("${base}s") != null) return "${base}s"
        // Block form: ${base}_block  (quartz→quartz_block, purpur→purpur_block)
        if (lookup("${base}_block") != null) return "${base}_block"
        // Direct: base itself (stone, cobblestone, sandstone, prismarine, etc.)
        if (lookup(base) != null) return base

        // Strip last segment (mossy_stone_brick → mossy_stone → mossy_stone_bricks)
        val lastUnderscore = base.lastIndexOf('_')
        if (lastUnderscore > 0) {
            val parent = base.substring(0, lastUnderscore)
            if (lookup("${parent}s") != null) return "${parent}s"
            if (lookup(parent) != null) return parent
            if (lookup("${parent}_block") != null) return "${parent}_block"
        }

        // Strip known prefixes
        for (prefix in listOf("smooth_", "polished_", "mossy_", "red_", "cut_", "dark_", "chiseled_", "cracked_")) {
            if (base.startsWith(prefix)) {
                val stripped = base.removePrefix(prefix)
                if (lookup("${stripped}_planks") != null) return "${stripped}_planks"
                if (lookup("${stripped}s") != null) return "${stripped}s"
                if (lookup("${stripped}_block") != null) return "${stripped}_block"
                if (lookup(stripped) != null) return stripped
            }
        }

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
