package dev.spyglass.android.calculators.storage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(vm: StorageViewModel = viewModel()) {
    val s  by vm.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = vm.items[s.selectedItemIndex]

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Storage Calculator", icon = PixelIcons.Storage)

        InputCard {
            SpyglassTextField(
                value         = s.input,
                onValueChange = vm::setInput,
                label         = "Quantity",
                placeholder   = "e.g. 14 stacks, 5k",
                keyboardType  = androidx.compose.ui.text.input.KeyboardType.Text,
            )

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value    = selectedItem.name,
                    onValueChange = {},
                    readOnly = true,
                    label    = { Text("Item (for block compression)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor    = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor  = MaterialTheme.colorScheme.secondary,
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    vm.items.forEachIndexed { i, item ->
                        DropdownMenuItem(text = { Text(item.name) }, onClick = { vm.setItem(i); expanded = false })
                    }
                }
            }
        }

        s.result?.let { r ->
            ResultCard {
                StatRow("Total items",   "%,d".format(r.total))
                SpyglassDivider()
                StatRow("Stacks",        "${"%,d".format(r.stacks)} + ${r.stackRem}")
                StatRow("Single chests", "${"%,d".format(r.singleChest)} + ${r.singleRem}")
                StatRow("Double chests", "${"%,d".format(r.doubleChest)} + ${r.doubleRem}")
                StatRow("Shulker boxes", "${"%,d".format(r.shulker)} + ${r.shulkerRem}")
                if (r.blocks != null && r.blockRem != null && selectedItem.ratio > 0) {
                    SpyglassDivider()
                    StatRow("${selectedItem.blockName} (${selectedItem.ratio}:1)", "${"%,d".format(r.blocks)} + ${r.blockRem} leftover")
                }
            }
        }

        Text(
            "Figure out how many chests and shulker boxes you need for a given quantity of items. Pick an item from the dropdown to also see block compression (e.g. how many iron blocks you can make from your iron ingots).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
