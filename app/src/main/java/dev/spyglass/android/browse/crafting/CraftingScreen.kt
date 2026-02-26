package dev.spyglass.android.browse.crafting

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CraftingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameDataRepository.get(app)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val recipes: StateFlow<List<RecipeEntity>> = _query.debounce(200)
        .flatMapLatest { repo.searchRecipes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
}

@Composable
fun CraftingScreen(vm: CraftingViewModel = viewModel()) {
    val query   by vm.query.collectAsState()
    val recipes by vm.recipes.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery,
            placeholder = { Text("Search recipes…", color = Stone500) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Stone500) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Stone700, cursorColor = Gold),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(recipes, key = { it.id }) { r ->
                BrowseListItem(
                    headline    = "${r.outputCount}× ${r.outputItem.substringAfterLast(':').replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                    supporting  = r.type.replace('_', ' '),
                    leadingIcon = Icons.Default.Construction,
                    trailing    = {
                        RecipeGridPreview(r)
                    },
                )
            }
            if (recipes.isEmpty()) item {
                EmptyState(
                    icon     = Icons.Default.SearchOff,
                    title    = "No recipes found",
                    subtitle = "Try a different search term",
                )
            }
        }
    }
}

@Composable
private fun RecipeGridPreview(r: RecipeEntity) {
    if (r.type.contains("shaped") && r.ingredientsJson.isNotBlank()) {
        val cells = runCatching {
            Json.parseToJsonElement(r.ingredientsJson).jsonArray.map {
                if (it is JsonNull) null else it.jsonPrimitive.contentOrNull?.substringAfterLast(':')?.take(2)?.uppercase()
            }
        }.getOrElse { emptyList() }

        if (cells.size == 9) {
            Column(modifier = Modifier.size(72.dp)) {
                for (row in 0..2) {
                    Row {
                        for (col in 0..2) {
                            val cell = cells.getOrNull(row * 3 + col)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(24.dp)
                                    .background(if (cell != null) SurfaceMid else Background, RoundedCornerShape(2.dp))
                                    .border(0.5.dp, Stone700, RoundedCornerShape(2.dp)),
                            ) {
                                if (cell != null) Text(cell, fontSize = 7.sp, color = Stone300, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}
