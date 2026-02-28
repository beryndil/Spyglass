package dev.spyglass.android.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.spyglass.android.data.db.entities.ShoppingListEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToListSection(
    itemId: String,
    itemName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember { GameDataRepository.get(context) }
    val scope = rememberCoroutineScope()
    val lists by repo.allShoppingLists().collectAsState(initial = emptyList())

    var quantityInput by remember { mutableStateOf("1") }
    var selectedList by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    // Auto-select first list when available
    LaunchedEffect(lists) {
        if (selectedList == null && lists.isNotEmpty()) selectedList = lists.first()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Add to Shopping List", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

        if (lists.isEmpty()) {
            Text(
                "No shopping lists yet \u2014 create one in the Shopping tab",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            return@Column
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // List dropdown
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = selectedList?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                selectedList = list
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }

            // Quantity input
            OutlinedTextField(
                value = quantityInput,
                onValueChange = { quantityInput = it.filter { c -> c.isDigit() } },
                label = { Text("Qty") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = MaterialTheme.colorScheme.secondary, cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.width(72.dp),
            )

            // Add button
            IconButton(
                onClick = {
                    val list = selectedList ?: return@IconButton
                    val qty = quantityInput.toIntOrNull() ?: return@IconButton
                    if (qty !in 1..10_000_000) return@IconButton
                    scope.launch {
                        repo.addToShoppingList(list.id, itemId, itemName, qty)
                        feedback = "Added!"
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add to list", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // Success feedback
        if (feedback != null) {
            Text(feedback!!, style = MaterialTheme.typography.bodySmall, color = Emerald)
            LaunchedEffect(feedback) {
                kotlinx.coroutines.delay(2000)
                feedback = null
            }
        }
    }
}
