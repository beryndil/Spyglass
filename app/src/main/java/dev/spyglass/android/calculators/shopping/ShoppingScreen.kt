package dev.spyglass.android.calculators.shopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*
import dev.spyglass.android.data.db.entities.RecipeEntity
import dev.spyglass.android.data.db.entities.ShoppingListItemEntity

// ── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun ShoppingScreen(vm: ShoppingViewModel = viewModel()) {
    val selectedListId by vm.selectedListId.collectAsStateWithLifecycle()

    if (selectedListId == null) {
        ListPicker(vm)
    } else {
        ListDetail(vm)
    }
}

// ── List picker (no list selected) ───────────────────────────────────────────

@Composable
private fun ListPicker(vm: ShoppingViewModel) {
    val lists by vm.allLists.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<Long?>(null) }
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TabIntroHeader(
                icon = PixelIcons.Storage,
                title = "Shopping Lists",
                description = "Plan your gathering and crafting with named lists",
            )
        }

        item {
            Button(
                onClick = { hapticClick(); showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New List")
            }
        }

        if (lists.isEmpty()) {
            item {
                EmptyState(
                    icon = PixelIcons.Storage,
                    title = "No lists yet",
                    subtitle = "Create a list to start tracking items",
                )
            }
        }

        items(lists, key = { it.id }) { list ->
            BrowseListItem(
                headline = list.name,
                supporting = "Created ${formatDate(list.createdAt)}",
                leadingIcon = PixelIcons.Storage,
                modifier = Modifier.clickable { hapticClick(); vm.selectList(list.id) },
                trailing = {
                    IconButton(
                        onClick = { hapticConfirm(); listToDelete = list.id },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }
                },
            )
        }
    }

    // New list dialog
    if (showDialog) {
        NewListDialog(
            onConfirm = { name ->
                vm.createList(name)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }

    // Delete confirmation dialog
    listToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text("Delete list?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will permanently remove this list and all its items.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { hapticConfirm(); vm.deleteList(id); listToDelete = null }) {
                    Text(stringResource(R.string.delete), color = NetherRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { hapticClick(); listToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

// ── List detail (list selected) ──────────────────────────────────────────────

@Composable
private fun ListDetail(vm: ShoppingViewModel) {
    val hapticClick = rememberHapticClick()
    val listId by vm.selectedListId.collectAsStateWithLifecycle()
    val lists by vm.allLists.collectAsStateWithLifecycle()
    val items by vm.selectedListItems.collectAsStateWithLifecycle()
    val allRecipes by vm.allRecipes.collectAsStateWithLifecycle()
    val craftingPlan by vm.craftingPlan.collectAsStateWithLifecycle()
    val expandedItemId by vm.expandedItemId.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()

    val currentList = lists.find { it.id == listId }
    var quantityInput by remember { mutableStateOf("1") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Header with back button ──
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { hapticClick(); vm.selectList(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    currentList?.name ?: "List",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { hapticClick(); showRenameDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename list", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "${items.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // ── Add item section ──
        item(key = "add_section") {
            InputCard {
                Text(stringResource(R.string.add), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                        placeholder = { Text("Search\u2026", color = MaterialTheme.colorScheme.secondary) },
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
                        label = { Text("Qty") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = MaterialTheme.colorScheme.secondary, cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.width(72.dp),
                    )
                    if (selectedId != null) {
                        IconButton(
                            onClick = {
                                hapticClick()
                                val qty = quantityInput.toIntOrNull() ?: 1
                                if (qty in 1..10_000_000) {
                                    vm.addItem(selectedId!!, selectedName, qty)
                                    selectedId = null
                                    selectedName = ""
                                    quantityInput = "1"
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
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
                                        hapticClick()
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

        // ── Checklist ──
        if (items.isNotEmpty()) {
            item(key = "list_header") {
                SectionHeader("Checklist", icon = PixelIcons.Storage)
            }
        }

        items(items, key = { it.id }) { item ->
            val isExpanded = expandedItemId == item.id
            Column {
                ShoppingItemRow(
                    item = item,
                    onCheckedChange = { vm.toggleChecked(item) },
                    onTap = { vm.toggleBreakdown(item.id) },
                    onDelete = { vm.deleteItem(item.id) },
                    onQuantityChange = { qty -> vm.updateQuantity(item.id, qty) },
                )
                val reduceMotion = LocalReduceAnimations.current
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = if (reduceMotion) expandVertically(snap()) else expandVertically(),
                    exit = if (reduceMotion) shrinkVertically(snap()) else shrinkVertically(),
                ) {
                    ItemBreakdown(item, allRecipes, vm)
                }
            }
        }

        // ── Crafting plan ──
        if (craftingPlan.isNotEmpty()) {
            item(key = "plan_header") {
                SectionHeader("Crafting Plan", icon = PixelIcons.Crafting)
            }
            item(key = "plan_card") {
                val grouped = craftingPlan.groupBy { it.depth }
                ResultCard {
                    grouped.entries.sortedBy { it.key }.forEachIndexed { index, (depth, steps) ->
                        if (index > 0) SpyglassDivider()
                        if (depth == 0) {
                            Text("Gather", style = MaterialTheme.typography.labelSmall, color = Emerald)
                            steps.forEach { step ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PlanStepIcon(step, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "${step.quantity}\u00d7 ${planStepName(step)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Emerald,
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Step $depth \u2014 ${stepGroupLabel(steps)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            steps.forEach { step ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PlanStepIcon(step, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "${step.quantity}\u00d7 ${planStepName(step)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Text(
                                        "${step.craftsNeeded} ${actionVerb(step.recipe?.type)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    // Rename dialog
    if (showRenameDialog && currentList != null) {
        RenameDialog(
            currentName = currentList.name,
            onConfirm = { name ->
                vm.renameList(currentList.id, name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }
}

// ── Shopping item row ────────────────────────────────────────────────────────

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItemEntity,
    onCheckedChange: () -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Int) -> Unit,
) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    var editingQty by remember { mutableStateOf(false) }
    var qtyInput by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalSurfaceCard.current, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { hapticClick(); onTap() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { hapticConfirm(); onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.secondary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        val tex = ItemTextures.get(item.itemId)
        if (tex != null) {
            SpyglassIconImage(tex, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.itemName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(item.itemId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
        }
        // Quantity stepper
        if (editingQty) {
            BasicTextField(
                value = qtyInput,
                onValueChange = { qtyInput = it.filter { c -> c.isDigit() } },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .width(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            )
            TextButton(onClick = {
                hapticClick()
                val qty = qtyInput.toIntOrNull()
                if (qty != null && qty in 1..10_000_000) onQuantityChange(qty)
                editingQty = false
            }) {
                Text("OK", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 2.dp),
            ) {
                IconButton(
                    onClick = { hapticClick(); if (item.quantity > 1) onQuantityChange(item.quantity - 1) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Text("\u2212", color = if (item.quantity > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { hapticClick(); editingQty = true }
                        .padding(horizontal = 6.dp),
                )
                IconButton(
                    onClick = { hapticClick(); onQuantityChange(item.quantity + 1) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Text("+", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        IconButton(onClick = { hapticConfirm(); onDelete() }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Per-item chain breakdown ─────────────────────────────────────────────────

@Composable
private fun ItemBreakdown(
    item: ShoppingListItemEntity,
    allRecipes: Map<String, RecipeEntity>,
    vm: ShoppingViewModel,
) {
    val chain = remember(item.itemId, item.quantity, allRecipes) {
        vm.resolveChain(item.itemId, item.quantity, allRecipes)
    }

    if (chain.isEmpty()) return

    val craftedSteps = chain.filter { it.recipe != null }
    val rawSteps = chain.filter { it.recipe == null }

    ResultCard(modifier = Modifier.padding(start = 32.dp, top = 4.dp)) {
        if (craftedSteps.isNotEmpty()) {
            Text("Craft", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            craftedSteps.forEach { step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BreakdownStepIcon(step, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${breakdownStepName(step)} \u00d7 ${step.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        "${step.craftsNeeded} crafts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        if (rawSteps.isNotEmpty()) {
            if (craftedSteps.isNotEmpty()) SpyglassDivider()
            Text("Gather", style = MaterialTheme.typography.labelSmall, color = Emerald)
            rawSteps.forEach { step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BreakdownStepIcon(step, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${breakdownStepName(step)} \u00d7 ${step.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Emerald,
                    )
                }
            }
        }
    }
}

// ── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun NewListDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val hapticClick = rememberHapticClick()
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Shopping List", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 100) name = it },
                placeholder = { Text("List name", color = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { hapticClick(); if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create", color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = { hapticClick(); onDismiss() }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun RenameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val hapticClick = rememberHapticClick()
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename List", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 100) name = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { hapticClick(); if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Rename", color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = { hapticClick(); onDismiss() }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatItemName(id: String): String =
    id.substringAfterLast(':').split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

@Composable
private fun PlanStepIcon(step: CraftingPlanStep, modifier: Modifier = Modifier) {
    if (step.tagId != null) {
        RotatingTagIcon(step.tagId, modifier = modifier)
    } else {
        val tex = ItemTextures.get(step.itemId)
        if (tex != null) {
            SpyglassIconImage(tex, contentDescription = null, modifier = modifier)
        }
    }
}

private fun planStepName(step: CraftingPlanStep): String =
    if (step.tagId != null) formatTagName(step.tagId) else formatItemName(step.itemId)

@Composable
private fun BreakdownStepIcon(step: dev.spyglass.android.core.ui.ChainStep, modifier: Modifier = Modifier) {
    if (step.tagId != null) {
        RotatingTagIcon(step.tagId, modifier = modifier)
    } else {
        val tex = ItemTextures.get(step.itemId)
        if (tex != null) {
            SpyglassIconImage(tex, contentDescription = null, modifier = modifier)
        }
    }
}

private fun breakdownStepName(step: dev.spyglass.android.core.ui.ChainStep): String =
    if (step.tagId != null) formatTagName(step.tagId) else formatItemName(step.itemId)

private fun actionVerb(recipeType: String?): String = when {
    recipeType == null -> ""
    recipeType.contains("smelting") -> "smelts"
    recipeType.contains("stonecutting") -> "cuts"
    recipeType.contains("smithing") -> "smiths"
    else -> "crafts"
}

private fun stepGroupLabel(steps: List<dev.spyglass.android.core.ui.CraftingPlanStep>): String {
    val types = steps.mapNotNull { it.recipe?.type }.toSet()
    return when {
        types.all { it.contains("smelting") } -> "Smelt"
        types.all { it.contains("stonecutting") } -> "Stonecut"
        types.all { it.contains("smithing") } -> "Smith"
        types.size == 1 -> "Craft"
        else -> "Process"
    }
}

private fun formatDate(millis: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val month = cal.get(java.util.Calendar.MONTH) + 1
    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val year = cal.get(java.util.Calendar.YEAR)
    return "$month/$day/$year"
}
