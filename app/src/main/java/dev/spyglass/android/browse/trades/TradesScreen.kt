package dev.spyglass.android.browse.trades

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.TradeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TradesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query      = MutableStateFlow("")
    private val _profession = MutableStateFlow("all")
    val query:      StateFlow<String> = _query.asStateFlow()
    val profession: StateFlow<String> = _profession.asStateFlow()

    val trades: StateFlow<List<TradeEntity>> = combine(_query.debounce(200), _profession) { q, prof ->
        if (prof == "all") repo.searchTrades(q) else repo.tradesByProfession(prof)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setProfession(p: String) { _profession.value = p }

    val professions = listOf("all", "armorer", "butcher", "cartographer", "cleric",
        "farmer", "fisherman", "fletcher", "leatherworker", "librarian",
        "mason", "shepherd", "toolsmith", "weaponsmith")
}

@Composable
fun TradesScreen(vm: TradesViewModel = viewModel()) {
    val query      by vm.query.collectAsState()
    val profession by vm.profession.collectAsState()
    val trades     by vm.trades.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search trades…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            items(vm.professions) { p ->
                FilterChip(selected = profession == p, onClick = { vm.setProfession(p) },
                    label = { Text(p.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Trade,
                    title = "Trades",
                    description = "Villager trades by profession and level",
                    stat = "${trades.size} trades",
                )
            }
            items(trades, key = { it.rowId }) { t ->
                BrowseListItem(
                    headline    = "${t.buyItem1Count}× ${t.buyItem1.substringAfterLast(':')}${if (t.buyItem2.isNotBlank()) " + ${t.buyItem2Count}× ${t.buyItem2.substringAfterLast(':')}" else ""} → ${t.sellItemCount}× ${t.sellItem.substringAfterLast(':')}",
                    supporting  = "${t.profession.replaceFirstChar { it.uppercase() }} · ${t.levelName}",
                    leadingIcon = PixelIcons.Trade,
                    leadingIconTint = Emerald,
                    trailing    = {
                        CategoryBadge(label = "Lvl ${t.level}", color = Gold)
                    },
                )
            }
            if (trades.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No trades found",
                    subtitle = "Try a different search or profession",
                )
            }
        }
    }
}
