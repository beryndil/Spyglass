package dev.spyglass.android.settings

object MinecraftVersions {

    val JAVA_VERSIONS = listOf(
        "1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9",
        "1.10", "1.11", "1.12", "1.13", "1.14", "1.15", "1.16", "1.17", "1.18",
        "1.19", "1.20", "1.21",
    )

    val BEDROCK_VERSIONS = listOf(
        "1.0", "1.1", "1.2", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9",
        "1.10", "1.11", "1.12", "1.13", "1.14", "1.16", "1.17", "1.18",
        "1.19", "1.20", "1.21",
    )

    /** Compares two Minecraft version strings (e.g. "1.13" vs "1.9"). */
    fun compare(a: String, b: String): Int {
        val aParts = a.split('.').map { it.toIntOrNull() ?: 0 }
        val bParts = b.split('.').map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(aParts.size, bParts.size)
        for (i in 0 until maxLen) {
            val av = aParts.getOrElse(i) { 0 }
            val bv = bParts.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }
}
