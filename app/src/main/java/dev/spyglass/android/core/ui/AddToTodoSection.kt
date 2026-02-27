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
import dev.spyglass.android.data.db.entities.TodoEntity
import dev.spyglass.android.data.repository.GameDataRepository
import kotlinx.coroutines.launch

@Composable
fun AddToTodoSection(
    itemId: String,
    itemName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember { GameDataRepository.get(context) }
    val scope = rememberCoroutineScope()

    var quantityInput by remember { mutableStateOf("64") }
    var feedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Add to Todo List", style = MaterialTheme.typography.labelSmall, color = Gold)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Quantity input
            OutlinedTextField(
                value = quantityInput,
                onValueChange = { quantityInput = it.filter { c -> c.isDigit() } },
                label = { Text("Qty") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold, unfocusedBorderColor = Stone700,
                    focusedLabelColor = Gold, unfocusedLabelColor = Stone500, cursorColor = Gold,
                ),
                modifier = Modifier.width(80.dp),
            )

            // Preview of what will be added
            val qty = quantityInput.toIntOrNull() ?: 0
            val preview = if (qty > 0) "Gather $qty $itemName" else ""
            if (preview.isNotEmpty()) {
                Text(
                    formatForDisplay(qty, itemName),
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone300,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Add button
            IconButton(
                onClick = {
                    val q = quantityInput.toIntOrNull() ?: return@IconButton
                    if (q <= 0) return@IconButton
                    scope.launch {
                        repo.createTodo(
                            TodoEntity(
                                title = "Gather $q $itemName",
                                itemId = itemId,
                                itemName = itemName,
                                quantity = q,
                            )
                        )
                        feedback = "Added!"
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Gold),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add to todo", tint = Background, modifier = Modifier.size(20.dp))
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

private fun formatForDisplay(quantity: Int, itemName: String): String {
    val chests = quantity / 1728
    val afterChests = quantity % 1728
    val stacks = afterChests / 64
    val items = afterChests % 64

    val parts = mutableListOf<String>()
    if (chests > 0) parts += "$chests chest${if (chests > 1) "s" else ""}"
    if (stacks > 0) parts += "$stacks stack${if (stacks > 1) "s" else ""}"
    if (items > 0)  parts += "$items"
    return if (parts.isEmpty()) "Gather 0 $itemName"
    else "Gather ${parts.joinToString(", ")} $itemName"
}
