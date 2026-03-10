package dev.spyglass.android.calculators.blockfill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun BlockFillScreen(vm: BlockFillViewModel = viewModel()) {
    val hapticConfirm = rememberHapticConfirm()
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        SectionHeader(stringResource(R.string.blockfill_header), icon = PixelIcons.Fill)

        InputCard {
            // Width
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpyglassTextField(
                    value         = s.widthInput,
                    onValueChange = vm::setWidth,
                    label         = stringResource(R.string.blockfill_width),
                    modifier      = Modifier.weight(1f),
                )
                TogglePill(listOf(stringResource(R.string.blockfill_blocks), stringResource(R.string.blockfill_chunks)), if (s.widthChunks) 1 else 0, { vm.toggleWidthChunks() }, Modifier.width(140.dp))
            }

            // Length
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpyglassTextField(
                    value         = s.lengthInput,
                    onValueChange = vm::setLength,
                    label         = stringResource(R.string.blockfill_length),
                    modifier      = Modifier.weight(1f),
                )
                TogglePill(listOf(stringResource(R.string.blockfill_blocks), stringResource(R.string.blockfill_chunks)), if (s.lengthChunks) 1 else 0, { vm.toggleLengthChunks() }, Modifier.width(140.dp))
            }

            SpyglassTextField(value = s.heightInput, onValueChange = vm::setHeight, label = stringResource(R.string.blockfill_height))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Checkbox(
                    checked = s.hollow,
                    onCheckedChange = { hapticConfirm(); vm.toggleHollow() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, uncheckedColor = MaterialTheme.colorScheme.secondary, checkmarkColor = MaterialTheme.colorScheme.onPrimary),
                )
                Text(stringResource(R.string.blockfill_hollow), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        s.result?.let { r ->
            ResultCard {
                StatRow(stringResource(R.string.blockfill_total_blocks),  "%,d".format(r.totalBlocks))
                SpyglassDivider()
                StatRow(stringResource(R.string.blockfill_stacks),       "${"%,d".format(r.stacks)} + ${r.stackRem} left")
                StatRow(stringResource(R.string.blockfill_single_chests),     "${"%,d".format(r.singleChest)} + ${r.singleRem} left")
                StatRow(stringResource(R.string.blockfill_double_chests),     "${"%,d".format(r.doubleChest)} + ${r.doubleRem} left")
                StatRow(stringResource(R.string.blockfill_shulker_boxes),     "${"%,d".format(r.shulker)} + ${r.shulkerRem} left")
            }
        }

        Text(
            stringResource(R.string.blockfill_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
