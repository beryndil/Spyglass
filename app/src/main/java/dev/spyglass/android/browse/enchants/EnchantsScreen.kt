package dev.spyglass.android.browse.enchants

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
import dev.spyglass.android.data.db.entities.EnchantEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class EnchantsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo   = GameDataRepository.get(app)
    private val _query  = MutableStateFlow("")
    private val _target = MutableStateFlow("all")
    val query:  StateFlow<String> = _query.asStateFlow()
    val target: StateFlow<String> = _target.asStateFlow()

    val enchants: StateFlow<List<EnchantEntity>> = combine(_query.debounce(200), _target) { q, t ->
        if (q.isBlank() && t == "all") repo.searchEnchants("") else
        if (t != "all") repo.enchantsForTarget(t) else repo.searchEnchants(q)
    }.flatMapLatest { it }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String)  { _query.value = q }
    fun setTarget(t: String) { _target.value = t }
}

private fun formatTarget(target: String): String =
    target.split(",").joinToString(", ") {
        it.trim().replace('_', ' ').replaceFirstChar { c -> c.uppercase() }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnchantsScreen(vm: EnchantsViewModel = viewModel()) {
    val query   by vm.query.collectAsState()
    val target  by vm.target.collectAsState()
    val enchants by vm.enchants.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search enchantments…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("all", "armor", "sword", "bow", "crossbow", "trident", "mace", "fishing_rod").forEach { t ->
                FilterChip(selected = target == t, onClick = { vm.setTarget(t) },
                    label = { Text(t.replace('_', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                TabIntroHeader(
                    icon = PixelIcons.Enchant,
                    title = "Enchantments",
                    description = "All enchantments by target, rarity, and compatibility",
                    stat = "${enchants.size} enchantments",
                )
            }
            items(enchants, key = { it.id }) { e ->
                BrowseListItem(
                    headline    = buildString {
                        append(e.name)
                        if (e.isTreasure && !e.isCurse) append("  \u2022 Anvil Only")
                        if (e.isCurse) append("  \u2022 Curse")
                    },
                    supporting  = buildString {
                        append(e.description.ifBlank { e.id })
                        append("\nApplies to: ${formatTarget(e.target)}")
                    },
                    supportingMaxLines = 4,
                    leadingIcon = EnchantTextures.get(e.id) ?: PixelIcons.Enchant,
                    trailing    = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Max ${e.maxLevel}", style = MaterialTheme.typography.bodySmall, color = Gold)
                            Spacer(Modifier.height(2.dp))
                            val rarityColor = when (e.rarity.lowercase()) {
                                "common"   -> Emerald
                                "uncommon" -> PotionBlue
                                "rare"     -> EnderPurple
                                "very_rare", "very rare" -> Gold
                                else       -> Stone500
                            }
                            CategoryBadge(label = e.rarity.replace('_', ' '), color = rarityColor)
                            if (e.isTreasure) {
                                Spacer(Modifier.height(2.dp))
                                CategoryBadge(
                                    label = if (e.isCurse) "Curse" else "Anvil",
                                    color = if (e.isCurse) Red400 else NetherRed,
                                )
                            }
                        }
                    },
                )
            }
            if (enchants.isEmpty()) item {
                EmptyState(
                    icon     = PixelIcons.SearchOff,
                    title    = "No enchantments found",
                    subtitle = "Try a different search or filter",
                )
            }
        }
    }
}
