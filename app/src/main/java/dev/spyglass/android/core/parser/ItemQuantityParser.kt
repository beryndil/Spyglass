package dev.spyglass.android.core.parser

/**
 * Flexible item-quantity parser.
 *
 * Accepts:
 *   "64"          → 64
 *   "5k"          → 5000
 *   "2.5k"        → 2500
 *   "1m"          → 1_000_000
 *   "14 stacks"   → 896
 *   "2 chests"    → 3456      (double chest = 3456 items)
 *   "1 chest"     → 1728      (single chest)
 *   "3 boxes"     → 5184      (shulker box = 1728)
 *   "3 shulkers"  → 5184
 *   Combinations: "2 stacks 32" → 160
 */
object ItemQuantityParser {
    private const val STACK        = 64L
    private const val SINGLE_CHEST = 1728L   // 27 slots × 64
    private const val DOUBLE_CHEST = 3456L   // 54 slots × 64
    private const val SHULKER      = 1728L   // 27 slots × 64

    /** Returns null if the string is blank or unparseable. */
    fun parse(raw: String): Long? {
        val s = raw.trim().lowercase().replace(",", "")
        if (s.isBlank()) return null

        var total = 0L
        var remaining = s

        // Extract named units
        val unitPattern = Regex("""(\d+(?:\.\d+)?)\s*(double\s+chests?|chests?|stacks?|shulkers?|boxes?|k|m)""")
        val matches = unitPattern.findAll(remaining)
        for (m in matches) {
            val num = m.groupValues[1].toDoubleOrNull() ?: continue
            total += when {
                m.groupValues[2].startsWith("double") -> (num * DOUBLE_CHEST).toLong()
                m.groupValues[2].startsWith("chest")  -> (num * SINGLE_CHEST).toLong()
                m.groupValues[2].startsWith("stack")  -> (num * STACK).toLong()
                m.groupValues[2].startsWith("shulker") || m.groupValues[2].startsWith("box") ->
                    (num * SHULKER).toLong()
                m.groupValues[2] == "k" -> (num * 1_000).toLong()
                m.groupValues[2] == "m" -> (num * 1_000_000).toLong()
                else -> num.toLong()
            }
            remaining = remaining.replace(m.value, " ")
        }

        // Any leftover bare number
        val bare = remaining.trim().toDoubleOrNull()
        if (bare != null) total += bare.toLong()

        return if (total > 0L) total else null
    }

    fun format(n: Long): String = when {
        n >= 1_000_000 -> "%.2fm".format(n / 1_000_000.0).trimEnd('0').trimEnd('.')
        n >= 1_000     -> "%.1fk".format(n / 1_000.0).trimEnd('0').trimEnd('.')
        else           -> n.toString()
    }

    /** Split a total into (quotient, remainder) for a given unit size. */
    fun breakdown(total: Long, unitSize: Long): Pair<Long, Long> =
        Pair(total / unitSize, total % unitSize)
}
