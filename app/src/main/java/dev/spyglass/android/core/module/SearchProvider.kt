package dev.spyglass.android.core.module

/**
 * A search result returned by a module's [SearchProvider].
 */
data class ModuleSearchResult(
    val type: String,
    val id: String,
    val name: String,
    val detail: String = "",
    val browseTab: Int = 0,
)

/**
 * Provides search results from a module for the global search screen.
 */
interface SearchProvider {
    /** Search for entities matching the query. Called on IO dispatcher. */
    suspend fun search(query: String): List<ModuleSearchResult>
}
