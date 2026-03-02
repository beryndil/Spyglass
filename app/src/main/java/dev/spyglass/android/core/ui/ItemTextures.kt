package dev.spyglass.android.core.ui

import dev.spyglass.android.R

/**
 * Unified texture lookup for all Minecraft items (blocks + non-block items).
 * Checks block textures first, then item textures.
 */
object ItemTextures {
    /**
     * Item ID → PNG filename (without extension).
     * Loaded from `texture_map.json` at startup; resolved via [TextureManager.resolveOrBundled].
     */
    private var textureMap: Map<String, String> = emptyMap()

    /** Replace the texture map with data loaded from JSON. */
    fun loadMap(items: Map<String, String>) {
        textureMap = items
        resolveCache.clear()
    }

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
