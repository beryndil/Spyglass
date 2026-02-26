package dev.spyglass.android.calculators.storage

import androidx.compose.foundation.layout.*
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        focusedBorderColor   = Gold,
                        unfocusedBorderColor = Stone700,
                        focusedLabelColor    = Gold,
                        unfocusedLabelColor  = Stone500,
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
    }
}
