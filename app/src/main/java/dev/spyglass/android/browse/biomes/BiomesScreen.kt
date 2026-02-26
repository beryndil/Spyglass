package dev.spyglass.android.browse.biomes

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.BiomeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BiomesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val biomes: StateFlow<List<BiomeEntity>> = _query.debounce(200)
        .flatMapLatest { repo.searchBiomes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
}

@Composable
fun BiomesScreen(vm: BiomesViewModel = viewModel()) {
    val query  by vm.query.collectAsState()
    val biomes by vm.biomes.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search biomes…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(biomes, key = { it.id }) { b ->
                BiomeListItem(b)
            }
            if (biomes.isEmpty()) item {
                EmptyState(
                    icon     = Icons.Default.SearchOff,
                    title    = "No biomes found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}

@Composable
private fun BiomeListItem(b: BiomeEntity) {
    val biomeColor = if (b.color.isNotEmpty()) {
        runCatching { Color(android.graphics.Color.parseColor(b.color)) }.getOrElse { Stone700 }
    } else null

    BrowseListItem(
        headline    = b.name,
        supporting  = b.id,
        leadingIcon = Icons.Default.Terrain,
        leadingIconTint = biomeColor ?: Stone300,
        trailing    = {
            Column(horizontalAlignment = Alignment.End) {
                CategoryBadge(label = b.category, color = Emerald)
                Spacer(Modifier.height(2.dp))
                Text("${b.temperature}°  ${b.precipitation}", style = MaterialTheme.typography.bodySmall, color = Stone500)
            }
        },
    )
}
