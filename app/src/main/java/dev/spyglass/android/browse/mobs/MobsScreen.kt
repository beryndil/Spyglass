package dev.spyglass.android.browse.mobs

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import dev.spyglass.android.data.db.entities.MobEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MobsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query    = MutableStateFlow("")
    private val _category = MutableStateFlow("all")
    val query:    StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String> = _category.asStateFlow()

    val mobs: StateFlow<List<MobEntity>> = combine(_query.debounce(200), _category) { q, cat ->
        if (cat == "all") repo.searchMobs(q) else repo.mobsByCategory(cat)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(c: String) { _category.value = c }
}

@Composable
fun MobsScreen(vm: MobsViewModel = viewModel()) {
    val query    by vm.query.collectAsState()
    val category by vm.category.collectAsState()
    val mobs     by vm.mobs.collectAsState()

    val categories = listOf("all", "hostile", "neutral", "passive", "boss")

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search mobs…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { c ->
                FilterChip(selected = category == c, onClick = { vm.setCategory(c) },
                    label = { Text(c.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(mobs, key = { it.id }) { m ->
                val categoryColor = when (m.category) {
                    "hostile"  -> NetherRed
                    "neutral"  -> Gold
                    "passive"  -> Emerald
                    "boss"     -> EnderPurple
                    else       -> Stone500
                }
                BrowseListItem(
                    headline    = m.name,
                    supporting  = m.id,
                    leadingIcon = Icons.Default.Pets,
                    leadingIconTint = categoryColor,
                    trailing    = {
                        Column(horizontalAlignment = Alignment.End) {
                            CategoryBadge(label = m.category, color = categoryColor)
                            Spacer(Modifier.height(2.dp))
                            Text("HP ${m.health.toInt()}", style = MaterialTheme.typography.bodySmall, color = Stone500)
                        }
                    },
                )
            }
            if (mobs.isEmpty()) item {
                EmptyState(
                    icon     = Icons.Default.SearchOff,
                    title    = "No mobs found",
                    subtitle = "Try a different search or category",
                )
            }
        }
    }
}
