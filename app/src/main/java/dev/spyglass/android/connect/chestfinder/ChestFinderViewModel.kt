package dev.spyglass.android.connect.chestfinder

import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.SearchResultsPayload
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin wrapper around ConnectViewModel for chest finder debouncing.
 * Debounces search input to avoid flooding the desktop with requests.
 */
class ChestFinderState(
    private val viewModel: ConnectViewModel,
    private val scope: CoroutineScope,
) {
    private var debounceJob: Job? = null

    val query = MutableStateFlow("")
    val results: StateFlow<SearchResultsPayload?> = viewModel.searchResults

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(300) // Debounce 300ms
            viewModel.searchItems(newQuery)
        }
    }

    fun clear() {
        query.value = ""
        viewModel.searchItems("")
    }
}
