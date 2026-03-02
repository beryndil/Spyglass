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
}
