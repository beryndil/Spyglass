package dev.spyglass.android.calculators.blockfill

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

@Composable
fun BlockFillScreen(vm: BlockFillViewModel = viewModel()) {
    val s by vm.state.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        SectionHeader("Block Fill Calculator", icon = PixelIcons.Fill)

        InputCard {
            // Width
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpyglassTextField(
                    value         = s.widthInput,
                    onValueChange = vm::setWidth,
                    label         = "Width",
                    modifier      = Modifier.weight(1f),
                )
                TogglePill(listOf("Blocks", "Chunks"), if (s.widthChunks) 1 else 0, { vm.toggleWidthChunks() }, Modifier.width(140.dp))
            }

            // Length
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpyglassTextField(
                    value         = s.lengthInput,
                    onValueChange = vm::setLength,
                    label         = "Length",
                    modifier      = Modifier.weight(1f),
                )
                TogglePill(listOf("Blocks", "Chunks"), if (s.lengthChunks) 1 else 0, { vm.toggleLengthChunks() }, Modifier.width(140.dp))
            }

            SpyglassTextField(value = s.heightInput, onValueChange = vm::setHeight, label = "Height (blocks)")
        }

        s.result?.let { r ->
            ResultCard {
                StatRow("Total blocks",  "%,d".format(r.totalBlocks))
                SpyglassDivider()
                StatRow("Stacks (64)",       "${"%,d".format(r.stacks)} + ${r.stackRem} left")
                StatRow("Single chests",     "${"%,d".format(r.singleChest)} + ${r.singleRem} left")
                StatRow("Double chests",     "${"%,d".format(r.doubleChest)} + ${r.doubleRem} left")
                StatRow("Shulker boxes",     "${"%,d".format(r.shulker)} + ${r.shulkerRem} left")
            }
        }

        Text(
            "Enter the width, length, and height of an area to calculate how many blocks you need to fill it. Toggle between blocks and chunks for large builds. Results show storage needed in stacks, chests, and shulker boxes.",
            style = MaterialTheme.typography.bodySmall,
            color = Stone500,
        )
    }
}
