package dev.spyglass.android.calculators.smelting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
fun SmeltingScreen(vm: SmeltingViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(stringResource(R.string.smelting_header), icon = PixelIcons.Smelt)

        InputCard {
            SpyglassTextField(
                value         = s.input,
                onValueChange = vm::setInput,
                label         = stringResource(R.string.smelting_label),
                placeholder   = stringResource(R.string.smelting_placeholder),
                keyboardType  = androidx.compose.ui.text.input.KeyboardType.Text,
            )

            if (s.parseError) {
                Text(stringResource(R.string.smelting_parse_error), color = Red400, style = MaterialTheme.typography.bodySmall)
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

        Text(
            stringResource(R.string.smelting_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
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
            Efficiency.MID  -> MaterialTheme.colorScheme.primary
            Efficiency.LOW  -> MaterialTheme.colorScheme.secondary
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(r.fuel.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
            val qty = if (r.fuel.stackSize <= 1 || r.units < r.fuel.stackSize) {
                "%,d".format(r.units)
            } else {
                val stacks = r.units / r.fuel.stackSize
                val remainder = r.units % r.fuel.stackSize
                if (remainder == 0L) "$stacks stack${if (stacks > 1) "s" else ""}"
                else "$stacks stack${if (stacks > 1) "s" else ""} + $remainder"
            }
            Text(qty, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            val wasted = r.unused.toLong()
            if (wasted > 0) {
                Text(stringResource(R.string.smelting_wasted, wasted), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
