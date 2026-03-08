package dev.spyglass.android.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object BlockTextures {
    /**
     * Block ID → texture filename (without extension).
     * Loaded from `texture_map.json` at startup; resolved via [TextureManager.resolveOrBundled].
     * Handles both downloaded PNGs and bundled vector drawables.
     * Backed by [mutableStateOf] so Compose recomposes when the map is reloaded.
     */
    private var textureMap: Map<String, String> by mutableStateOf(emptyMap())

    /** Replace the texture map with data loaded from JSON. */
    fun loadMap(blocks: Map<String, String>) {
        textureMap = blocks
    }

    fun get(blockId: String): SpyglassIcon? {
        resolveFlowerOverlay(blockId)?.let { return it }
        textureMap[blockId]?.let { filename ->
            TextureManager.resolveOrBundled(filename)?.let { return it }
        }
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
                "stained_glass", "stained_glass_pane" ->
                    TextureManager.resolveOrBundled("block_${color}_stained_glass_bk") ?: resolveById("glass")
                "shulker_box" ->
                    TextureManager.resolveOrBundled("block_${color}_shulker_box_bk") ?: resolveById("shulker_box")
                "carpet", "bed", "banner", "wall_banner" -> resolveById("${color}_wool") ?: resolveById("wool")
                "candle", "candle_cake" ->
                    TextureManager.resolveOrBundled("block_${color}_candle_bk") ?: TextureManager.resolveOrBundled("block_candle_bk")
                else -> null
            }
            if (result != null) return result
        }

        // Coral → type-specific fallbacks
        if (blockId.contains("coral")) {
            val coralType = CORAL_TYPES.firstOrNull { blockId.contains(it) }
            if (coralType != null) {
                val isDead = blockId.startsWith("dead_")
                val key = if (isDead) "block_dead_${coralType}_coral_bk" else "block_${coralType}_coral_bk"
                TextureManager.resolveOrBundled(key)?.let { return it }
            }
            return TextureManager.resolveOrBundled("block_coral_block_bk") ?: resolveById("prismarine")
        }

        // Potted plants → use the plant's texture
        if (blockId.startsWith("potted_")) {
            val plant = blockId.removePrefix("potted_")
            return get(plant)
        }

        // Ender chest → dedicated icon
        if (blockId == "ender_chest") return PixelIcons.EnderChest

        // Chest variants → chest icon
        if (blockId == "chest" || blockId == "trapped_chest" ||
            blockId == "barrel" || blockId.endsWith("shulker_box")) {
            return PixelIcons.Storage
        }

        return null
    }

    /** Resolve a block ID through the unified texture map. */
    private fun resolveById(id: String): SpyglassIcon? {
        textureMap[id]?.let { filename ->
            TextureManager.resolveOrBundled(filename)?.let { return it }
        }
        return null
    }

    private val COLORS = listOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
        "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    )

    private val CORAL_TYPES = listOf("tube", "brain", "bubble", "fire", "horn")

    /** Maps flower block IDs to the dye color they produce when crafted. */
    private val FLOWER_DYE_MAP = mapOf(
        "dandelion" to "yellow",
        "poppy" to "red",
        "blue_orchid" to "light_blue",
        "allium" to "magenta",
        "azure_bluet" to "light_gray",
        "red_tulip" to "red",
        "orange_tulip" to "orange",
        "pink_tulip" to "pink",
        "white_tulip" to "light_gray",
        "oxeye_daisy" to "light_gray",
        "cornflower" to "blue",
        "lily_of_the_valley" to "white",
        "wither_rose" to "black",
        "sunflower" to "yellow",
        "lilac" to "magenta",
        "rose_bush" to "red",
        "peony" to "pink",
        "torchflower" to "orange",
        "pitcher_plant" to "cyan",
        "cactus_flower" to "yellow",
        "open_eyeblossom" to "orange",
        "closed_eyeblossom" to "gray",
        "golden_dandelion" to "yellow",
        "pink_petals" to "pink",
        "spore_blossom" to "pink",
        "wildflowers" to "blue",
    )

    /** Returns an [SpyglassIcon.Overlay] with the flower's dye color clipped to the bud shape. */
    private fun resolveFlowerOverlay(blockId: String): SpyglassIcon? {
        val dyeColor = FLOWER_DYE_MAP[blockId] ?: return null
        val texture = TextureManager.resolveOrBundled("block_dye_$dyeColor") ?: return null
        val mask = TextureManager.resolveOrBundled("block_flower_bud_mask") ?: return null
        val frame = TextureManager.resolveOrBundled("block_flower_stem_frame") ?: return null
        return SpyglassIcon.Overlay(texture, mask, frame)
    }
}
