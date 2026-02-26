package dev.spyglass.android.browse.potions

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.PotionEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PotionsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val potions: StateFlow<List<PotionEntity>> = _query.debounce(200)
        .flatMapLatest { repo.searchPotions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
}

@Composable
fun PotionsScreen(vm: PotionsViewModel = viewModel()) {
    val query   by vm.query.collectAsState()
    val potions by vm.potions.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search potions…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(potions, key = { it.id }) { p ->
                val catColor = when (p.category) {
                    "negative" -> NetherRed
                    "positive" -> Emerald
                    else       -> Stone500
                }
                BrowseListItem(
                    headline    = p.name,
                    supporting  = p.effect.ifBlank { p.ingredientPath },
                    leadingIcon = Icons.Default.Science,
                    leadingIconTint = PotionBlue,
                    trailing    = {
                        Column(horizontalAlignment = Alignment.End) {
                            if (p.durationSeconds > 0)
                                Text("${p.durationSeconds}s", style = MaterialTheme.typography.bodySmall, color = Gold)
                            Spacer(Modifier.height(2.dp))
                            CategoryBadge(label = p.category, color = catColor)
                        }
                    },
                )
            }
            if (potions.isEmpty()) item {
                EmptyState(
                    icon     = Icons.Default.SearchOff,
                    title    = "No potions found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}
