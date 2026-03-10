package dev.spyglass.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.spyglass.android.data.db.entities.SearchHistoryEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.launch

@Composable
fun SpyglassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    category: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
    },
) {
    val context = LocalContext.current
    val repo = remember { GameDataRepository.get(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isFocused by remember { mutableStateOf(false) }

    val historyEntries by remember(category, query, isFocused) {
        if (!isFocused) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else if (query.isBlank()) {
            repo.searchHistory(category)
        } else {
            repo.matchingHistory(category, query)
        }
    }.collectAsState(emptyList())

    val showDropdown = isFocused && historyEntries.isNotEmpty()

    Box(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.secondary) },
            leadingIcon = leadingIcon,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.length >= 2) {
                    scope.launch { repo.saveSearchHistory(category, query) }
                }
                focusManager.clearFocus()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Tab && event.type == KeyEventType.KeyDown && historyEntries.isNotEmpty()) {
                        onQueryChange(historyEntries.first().query)
                        true
                    } else false
                },
        )

        if (showDropdown) {
            val cardColor = LocalSurfaceCard.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp)
                    .zIndex(1f)
                    .background(cardColor, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            ) {
                historyEntries.forEach { entry ->
                    HistoryRow(
                        entry = entry,
                        onClick = {
                            onQueryChange(entry.query)
                            isFocused = false
                            focusManager.clearFocus()
                        },
                        onDelete = { scope.launch { repo.deleteSearchHistory(entry.id) } },
                    )
                }
                TextButton(
                    onClick = { scope.launch { repo.clearSearchHistory(category) } },
                    modifier = Modifier.align(Alignment.End).padding(end = 4.dp),
                ) {
                    Text(stringResource(R.string.core_clear_history), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: SearchHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            entry.query,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.core_remove),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
