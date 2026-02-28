package dev.spyglass.android.data

object ItemTags {

    private val tagMembers: Map<String, List<String>> = mapOf(
        "#planks" to listOf(
            "oak_planks", "spruce_planks", "birch_planks", "jungle_planks",
            "acacia_planks", "dark_oak_planks", "mangrove_planks", "cherry_planks",
            "bamboo_planks", "crimson_planks", "warped_planks", "pale_oak_planks",
        ),
        "#logs" to listOf(
            "oak_log", "spruce_log", "birch_log", "jungle_log",
            "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log",
            "pale_oak_log",
        ),
        "#wooden_slabs" to listOf(
            "oak_slab", "spruce_slab", "birch_slab", "jungle_slab",
            "acacia_slab", "dark_oak_slab", "mangrove_slab", "cherry_slab",
            "bamboo_slab", "crimson_slab", "warped_slab",
        ),
        "#wooden_stairs" to listOf(
            "oak_stairs", "spruce_stairs", "birch_stairs", "jungle_stairs",
            "acacia_stairs", "dark_oak_stairs", "mangrove_stairs", "cherry_stairs",
            "bamboo_stairs", "crimson_stairs", "warped_stairs",
        ),
        "#wooden_fences" to listOf(
            "oak_fence", "spruce_fence", "birch_fence", "jungle_fence",
            "acacia_fence", "dark_oak_fence", "mangrove_fence", "cherry_fence",
            "bamboo_fence", "crimson_fence", "warped_fence",
        ),
        "#wooden_doors" to listOf(
            "oak_door", "spruce_door", "birch_door", "jungle_door",
            "acacia_door", "dark_oak_door", "mangrove_door", "cherry_door",
            "bamboo_door", "crimson_door", "warped_door",
        ),
        "#wool" to listOf(
            "white_wool", "orange_wool", "magenta_wool", "light_blue_wool",
            "yellow_wool", "lime_wool", "pink_wool", "gray_wool",
            "light_gray_wool", "cyan_wool", "purple_wool", "blue_wool",
            "brown_wool", "green_wool", "red_wool", "black_wool",
        ),
    )

    private val itemToTag: Map<String, String> by lazy {
        val result = mutableMapOf<String, String>()
        tagMembers.forEach { (tag, members) ->
            members.forEach { item -> result[item] = tag }
        }
        result
    }

    /** Variant prefixes for each tag — if a recipe output starts with one of these,
     *  the ingredient is variant-specific, not tag-based. */
    private val tagVariants: Map<String, Set<String>> = mapOf(
        "#planks" to setOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped", "pale_oak"),
        "#logs" to setOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak"),
        "#wooden_slabs" to setOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"),
        "#wooden_stairs" to setOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"),
        "#wooden_fences" to setOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"),
        "#wooden_doors" to setOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"),
        "#wool" to setOf("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"),
    )

    fun tagForItem(itemId: String): String? = itemToTag[itemId]

    /**
     * Returns the tag for an ingredient ONLY if the recipe is truly tag-based.
     * If the recipe output is variant-specific (e.g. acacia_door → acacia_planks),
     * returns null so the specific item is shown instead of a rotating tag icon.
     */
    fun tagForIngredient(ingredientId: String, outputItem: String): String? {
        val tag = itemToTag[ingredientId] ?: return null
        val variants = tagVariants[tag] ?: return tag
        for (variant in variants) {
            if (outputItem.startsWith("${variant}_")) return null
        }
        return tag
    }

    fun membersOfTag(tag: String): List<String> = tagMembers[tag] ?: emptyList()
}
