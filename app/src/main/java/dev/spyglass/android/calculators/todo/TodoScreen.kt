package dev.spyglass.android.calculators.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.ShoppingListEntity
import dev.spyglass.android.data.db.entities.TodoEntity

@Composable
fun TodoScreen(vm: TodoViewModel = viewModel()) {
    val todos by vm.allTodos.collectAsStateWithLifecycle()
    val shoppingLists by vm.allShoppingLists.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()

    var mode by remember { mutableIntStateOf(0) } // 0 = free-form, 1 = item-linked
    var freeformInput by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("64") }
    var linkDialogTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var editDialogTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var createLinkedList by remember { mutableStateOf<ShoppingListEntity?>(null) }

    val completedCount = todos.count { it.completed }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            TabIntroHeader(
                icon = PixelIcons.Todo,
                title = stringResource(R.string.todo_title),
                description = stringResource(R.string.todo_description),
            )
        }

        // ── Add section ──
        item(key = "add_section") {
            InputCard {
                TogglePill(
                    options = listOf(stringResource(R.string.todo_free_form), stringResource(R.string.todo_item_linked)),
                    selected = mode,
                    onSelect = { mode = it },
                )

                // ── Optional shopping list link ──
                ShoppingListLinkPicker(
                    shoppingLists = shoppingLists,
                    selected = createLinkedList,
                    onSelect = { createLinkedList = it },
                )

                if (mode == 0) {
                    // Free-form input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = freeformInput,
                            onValueChange = { if (it.length <= 200) freeformInput = it },
                            placeholder = { Text(stringResource(R.string.todo_freeform_placeholder), color = MaterialTheme.colorScheme.secondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                vm.createFreeformTodo(
                                    freeformInput,
                                    linkedType = createLinkedList?.let { "shopping_list" },
                                    linkedId = createLinkedList?.id,
                                )
                                freeformInput = ""
                                createLinkedList = null
                            },
                            enabled = freeformInput.isNotBlank(),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (freeformInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    // Item-linked input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                vm.setSearchQuery(it)
                                if (selectedId != null) {
                                    selectedId = null
                                    selectedName = ""
                                }
                            },
                            placeholder = { Text(stringResource(R.string.todo_search_items_placeholder), color = MaterialTheme.colorScheme.secondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (selectedId != null) Emerald else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (selectedId != null) Emerald else MaterialTheme.colorScheme.outline,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.todo_qty)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = MaterialTheme.colorScheme.secondary, cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.width(80.dp),
                        )
                        if (selectedId != null) {
                            IconButton(
                                onClick = {
                                    val qty = quantityInput.toIntOrNull() ?: 1
                                    if (qty in 1..10_000_000) {
                                        vm.createItemTodo(
                                            selectedId!!, selectedName, qty,
                                            linkedType = createLinkedList?.let { "shopping_list" },
                                            linkedId = createLinkedList?.id,
                                        )
                                        selectedId = null
                                        selectedName = ""
                                        quantityInput = "64"
                                        createLinkedList = null
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Search results dropdown
                    if (selectedId == null && searchResults.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                        ) {
                            searchResults.forEach { (id, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedId = id
                                            selectedName = name
                                            vm.setSearchQuery(name)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val tex = ItemTextures.get(id)
                                    if (tex != null) {
                                        SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Task list ──
        if (todos.isNotEmpty()) {
            item(key = "list_header") {
                SectionHeader(stringResource(R.string.todo_tasks_header), icon = PixelIcons.Todo)
            }
        } else {
            item(key = "empty") {
                EmptyState(
                    icon = PixelIcons.Todo,
                    title = stringResource(R.string.todo_no_tasks_yet),
                    subtitle = stringResource(R.string.todo_add_first_task),
                )
            }
        }

        items(todos, key = { it.id }) { todo ->
            TodoRow(
                todo = todo,
                onToggle = { vm.toggleCompleted(todo) },
                onEdit = { editDialogTodo = todo },
                onDelete = { vm.deleteTodo(todo.id) },
                onLinkTap = { linkDialogTodo = todo },
            )
        }

        // ── Clear completed ──
        if (completedCount > 0) {
            item(key = "clear_completed") {
                TextButton(onClick = vm::deleteCompleted) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Red400, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.todo_clear_completed, completedCount), color = Red400)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    // ── Edit dialog ──
    if (editDialogTodo != null) {
        EditTodoDialog(
            todo = editDialogTodo!!,
            onSave = { newTitle -> vm.editTitle(editDialogTodo!!.id, newTitle); editDialogTodo = null },
            onDismiss = { editDialogTodo = null },
        )
    }

    // ── Link dialog ──
    if (linkDialogTodo != null) {
        LinkDialog(
            todo = linkDialogTodo!!,
            shoppingLists = shoppingLists,
            onLink = { type, id -> vm.linkToTool(linkDialogTodo!!.id, type, id) },
            onDismiss = { linkDialogTodo = null },
        )
    }
}

// ── Todo row ────────────────────────────────────────────────────────────────

@Composable
private fun TodoRow(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLinkTap: () -> Unit,
) {
    val hapticConfirm = rememberHapticConfirm()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalSurfaceCard.current, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = todo.completed,
            onCheckedChange = { hapticConfirm(); onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.secondary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))

        // Item icon if item-linked
        if (todo.itemId != null) {
            val tex = ItemTextures.get(todo.itemId)
            if (tex != null) {
                SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = !todo.completed) { onEdit() },
        ) {
            Text(
                todo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (todo.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Quantity badge
                if (todo.quantity != null) {
                    CategoryBadge(
                        label = formatQuantity(todo.quantity),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                // Linked tool badge
                if (todo.linkedType != null) {
                    CategoryBadge(
                        label = linkedLabel(todo.linkedType),
                        color = Emerald,
                    )
                }
            }
        }

        // Edit button
        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        }
        // Link button
        IconButton(onClick = onLinkTap, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Link, contentDescription = stringResource(R.string.todo_link_cd), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        }
        // Delete button
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Edit dialog ─────────────────────────────────────────────────────────────

@Composable
private fun EditTodoDialog(
    todo: TodoEntity,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(todo.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.todo_edit_task), color = MaterialTheme.colorScheme.onSurface) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 200) text = it },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank() && text != todo.title,
            ) {
                Text(stringResource(R.string.save), color = if (text.isNotBlank() && text != todo.title) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

// ── Link dialog ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkDialog(
    todo: TodoEntity,
    shoppingLists: List<ShoppingListEntity>,
    onLink: (String?, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedList by remember { mutableStateOf<ShoppingListEntity?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.todo_link_to_tool), color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.todo_link_desc, todo.title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (shoppingLists.isEmpty()) {
                    Text(stringResource(R.string.todo_no_shopping_lists), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedList?.name ?: stringResource(R.string.todo_select_list),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            shoppingLists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.name, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        selectedList = list
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedList != null) {
                        onLink("shopping_list", selectedList!!.id)
                    }
                    onDismiss()
                },
                enabled = selectedList != null,
            ) {
                Text(stringResource(R.string.todo_link_btn), color = if (selectedList != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            if (todo.linkedType != null) {
                TextButton(onClick = { onLink(null, null); onDismiss() }) {
                    Text(stringResource(R.string.todo_unlink), color = Red400)
                }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

// ── Shopping list link picker (inline in add section) ────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingListLinkPicker(
    shoppingLists: List<ShoppingListEntity>,
    selected: ShoppingListEntity?,
    onSelect: (ShoppingListEntity?) -> Unit,
) {
    if (shoppingLists.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.todo_link_shopping_optional),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            shoppingLists.forEach { list ->
                FilterChip(
                    selected = selected?.id == list.id,
                    onClick = { onSelect(if (selected?.id == list.id) null else list) },
                    label = { Text(list.name, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (selected?.id == list.id) {
                        { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald.copy(alpha = 0.2f),
                        selectedLabelColor = Emerald,
                        selectedLeadingIconColor = Emerald,
                    ),
                )
            }
            if (selected != null) {
                IconButton(onClick = { onSelect(null) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.LinkOff, contentDescription = stringResource(R.string.todo_clear_link_cd), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatQuantity(quantity: Int): String {
    val chests = quantity / 1728
    val afterChests = quantity % 1728
    val stacks = afterChests / 64
    val items = afterChests % 64

    val parts = mutableListOf<String>()
    if (chests > 0) parts += "$chests chest${if (chests > 1) "s" else ""}"
    if (stacks > 0) parts += "$stacks stack${if (stacks > 1) "s" else ""}"
    if (items > 0)  parts += "$items"
    return if (parts.isEmpty()) "\u00d70" else parts.joinToString(", ")
}

private fun linkedLabel(type: String): String = when (type) {
    "shopping_list" -> "Materials"
    "inventory"     -> "Inventory"
    "build"         -> "Build"
    else            -> type.replaceFirstChar { it.uppercase() }
}
