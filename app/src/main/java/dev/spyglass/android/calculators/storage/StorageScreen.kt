package dev.spyglass.android.calculators.storage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(vm: StorageViewModel = viewModel()) {
    val s  by vm.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val selectedItem = vm.items[s.selectedItemIndex]

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(stringResource(R.string.storage_header), icon = PixelIcons.Storage)

        InputCard {
            SpyglassTextField(
                value         = s.input,
                onValueChange = vm::setInput,
                label         = stringResource(R.string.storage_quantity),
                placeholder   = stringResource(R.string.storage_placeholder),
                keyboardType  = androidx.compose.ui.text.input.KeyboardType.Text,
            )

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value    = selectedItem.name,
                    onValueChange = {},
                    readOnly = true,
                    label    = { Text(stringResource(R.string.storage_item_dropdown)) },
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
                StatRow(stringResource(R.string.storage_total_items),   "%,d".format(r.total))
                SpyglassDivider()
                StatRow(stringResource(R.string.storage_stacks),        "${"%,d".format(r.stacks)} + ${r.stackRem}")
                StatRow(stringResource(R.string.storage_single_chests), "${"%,d".format(r.singleChest)} + ${r.singleRem}")
                StatRow(stringResource(R.string.storage_double_chests), "${"%,d".format(r.doubleChest)} + ${r.doubleRem}")
                StatRow(stringResource(R.string.storage_shulker_boxes), "${"%,d".format(r.shulker)} + ${r.shulkerRem}")
                if (r.blocks != null && r.blockRem != null && selectedItem.ratio > 0) {
                    SpyglassDivider()
                    StatRow("${selectedItem.blockName} (${selectedItem.ratio}:1)", "${"%,d".format(r.blocks)} + ${r.blockRem} leftover")
                }
            }
        }

        Text(
            stringResource(R.string.storage_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
