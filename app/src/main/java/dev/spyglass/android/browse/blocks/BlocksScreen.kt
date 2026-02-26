package dev.spyglass.android.browse.blocks

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.BlockEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BlocksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val blocks: StateFlow<List<BlockEntity>> = _query
        .debounce(200)
        .flatMapLatest { repo.searchBlocks(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
}

@Composable
fun BlocksScreen(vm: BlocksViewModel = viewModel()) {
    val query  by vm.query.collectAsState()
    val blocks by vm.blocks.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value         = query,
            onValueChange = vm::setQuery,
            placeholder   = { Text("Search blocks…", color = Stone500) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold, unfocusedBorderColor = Stone700,
                cursorColor = Gold,
            ),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(blocks, key = { it.id }) { b ->
                BrowseListItem(
                    headline    = b.name,
                    supporting  = b.id,
                    leadingIcon = Icons.Default.ViewInAr,
                    trailing    = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            if (b.toolRequired.isNotBlank())
                                Text(b.toolRequired, style = MaterialTheme.typography.bodySmall, color = Stone300)
                            Text("hardness ${b.hardness}", style = MaterialTheme.typography.bodySmall, color = Stone500)
                        }
                    },
                )
            }
            if (blocks.isEmpty()) item {
                EmptyState(
                    icon     = Icons.Default.SearchOff,
                    title    = "No blocks found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}
