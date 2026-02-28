package dev.spyglass.android.core.ui

/**
 * Index that maps entity display names to their type + id,
 * enabling description text to be linkified with tappable entity references.
 */

enum class EntityType { ITEM, BLOCK, MOB, BIOME, STRUCTURE, ENCHANT }

data class EntityLink(val type: EntityType, val id: String, val name: String)

data class LinkMatch(val start: Int, val end: Int, val link: EntityLink)

class EntityLinkIndex(entries: List<EntityLink>) {

    // Candidates sorted longest-first so "Suspicious Sand" matches before "Sand"
    private data class Candidate(val regex: Regex, val link: EntityLink)

    private val candidates: List<Candidate>

    init {
        // Generate plural variants and sort longest-first
        val expanded = mutableListOf<Pair<String, EntityLink>>()
        for (entry in entries) {
            expanded.add(entry.name to entry)
            // Simple English plurals
            val name = entry.name
            if (name.endsWith("y") && !name.endsWith("ey") && !name.endsWith("ay") && !name.endsWith("oy")) {
                expanded.add(name.dropLast(1) + "ies" to entry)
            } else if (name.endsWith("s") || name.endsWith("x") || name.endsWith("z") ||
                       name.endsWith("sh") || name.endsWith("ch")) {
                expanded.add(name + "es" to entry)
            } else {
                expanded.add(name + "s" to entry)
            }
        }
        // Sort longest-first, then alphabetical for stability
        val sorted = expanded.sortedWith(compareByDescending<Pair<String, EntityLink>> { it.first.length }.thenBy { it.first })
        // Pre-compile word-boundary regex for each candidate
        candidates = sorted.map { (text, link) ->
            val pattern = "\\b${Regex.escape(text)}\\b"
            Candidate(Regex(pattern, RegexOption.IGNORE_CASE), link)
        }
    }

    /**
     * Find all non-overlapping entity name matches in [text].
     * [selfId] is excluded to prevent self-links (e.g. Arrow description linking to itself).
     */
    fun findMatches(text: String, selfId: String? = null): List<LinkMatch> {
        if (text.isEmpty()) return emptyList()
        // Occupancy array: true = position already claimed by an earlier (longer) match
        val occupied = BooleanArray(text.length)
        val results = mutableListOf<LinkMatch>()

        for (candidate in candidates) {
            if (candidate.link.id == selfId) continue
            for (match in candidate.regex.findAll(text)) {
                val start = match.range.first
                val end = match.range.last + 1 // exclusive
                // Check no overlap with previously accepted matches
                var overlaps = false
                for (i in start until end) {
                    if (occupied[i]) { overlaps = true; break }
                }
                if (overlaps) continue
                // Claim positions
                for (i in start until end) occupied[i] = true
                results.add(LinkMatch(start, end, candidate.link))
            }
        }
        // Return sorted by position for building AnnotatedString
        return results.sortedBy { it.start }
    }
}
