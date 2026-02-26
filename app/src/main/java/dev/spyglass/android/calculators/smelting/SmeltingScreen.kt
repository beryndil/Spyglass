package dev.spyglass.android.calculators.smelting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.core.ui.*

@Composable
fun SmeltingScreen(vm: SmeltingViewModel = viewModel()) {
    val s by vm.state.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("Smelting / Fuel Calculator", icon = PixelIcons.Smelt)

        InputCard {
            SpyglassTextField(
                value         = s.input,
                onValueChange = vm::setInput,
                label         = "Items to smelt",
                placeholder   = "e.g. 14 stacks, 5k, 2 chests",
                keyboardType  = androidx.compose.ui.text.input.KeyboardType.Text,
            )

            if (s.parseError) {
                Text("Could not parse quantity", color = Red400, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (s.results.isNotEmpty()) {
            ResultCard {
                s.results.forEachIndexed { i, r ->
                    if (i > 0) SpyglassDivider()
                    FuelRow(r)
                }
            }
        }
    }
}

@Composable
private fun FuelRow(r: FuelResult) {
    Row(
        modifier            = Modifier.fillMaxWidth(),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Efficiency dot
        val dotColor = when (r.efficiency) {
            Efficiency.HIGH -> Green400
            Efficiency.MID  -> Gold
            Efficiency.LOW  -> Stone500
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(r.fuel.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
            val stacks = if (r.fuel.stackSize > 1) " (${r.units / r.fuel.stackSize} stacks + ${r.units % r.fuel.stackSize})" else ""
            Text("%,d%s".format(r.units, stacks), style = MaterialTheme.typography.bodyLarge, color = Stone100)
            if (r.unused > 0) {
                Text("%.1f unused".format(r.unused), style = MaterialTheme.typography.bodySmall, color = Stone500)
            }
        }
    }
}
