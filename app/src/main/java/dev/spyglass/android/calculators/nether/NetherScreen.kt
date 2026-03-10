package dev.spyglass.android.calculators.nether

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun NetherScreen(vm: NetherViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var subTab by remember { mutableIntStateOf(0) }
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader(stringResource(R.string.nether_header), icon = PixelIcons.Nether)

        TabRow(selectedTabIndex = subTab, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            listOf(stringResource(R.string.nether_convert), stringResource(R.string.nether_obsidian), stringResource(R.string.nether_portals)).forEachIndexed { i, t ->
                Tab(selected = subTab == i, onClick = { hapticClick(); subTab = i }, text = { Text(t) })
            }
        }

        when (subTab) {
            0 -> ConvertTab(s, vm)
            1 -> ObsidianTab(s, vm)
            2 -> PortalsTab(s, vm)
        }

        Text(
            stringResource(R.string.nether_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun ConvertTab(s: NetherState, vm: NetherViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TogglePill(listOf("Overworld", "Nether"), if (s.dimension == NetherDimension.OVERWORLD) 0 else 1,
            { vm.setDimension(if (it == 0) NetherDimension.OVERWORLD else NetherDimension.NETHER) })

        InputCard {
            Text(stringResource(R.string.nether_input_coords), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpyglassTextField(s.xIn, vm::setX, "X", Modifier.weight(1f))
                SpyglassTextField(s.yIn, vm::setY, "Y", Modifier.weight(1f))
                SpyglassTextField(s.zIn, vm::setZ, "Z", Modifier.weight(1f))
            }
        }

        if (s.xOut.isNotEmpty() || s.yOut.isNotEmpty() || s.zOut.isNotEmpty()) {
            ResultCard {
                val outDim = if (s.dimension == NetherDimension.OVERWORLD) "Nether" else "Overworld"
                if (s.xOut.isNotEmpty()) StatRow("$outDim X", s.xOut)
                if (s.yOut.isNotEmpty()) StatRow(stringResource(R.string.nether_y_unchanged), s.yOut)
                if (s.zOut.isNotEmpty()) StatRow("$outDim Z", s.zOut)
                if (s.facing.isNotEmpty()) StatRow(stringResource(R.string.nether_facing), s.facing)
            }
        }

    }
}

@Composable
private fun ObsidianTab(s: NetherState, vm: NetherViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InputCard {
            Text(stringResource(R.string.nether_portal_interior), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpyglassTextField(s.obWidth, vm::setObWidth, stringResource(R.string.nether_width_min), Modifier.weight(1f))
                SpyglassTextField(s.obHeight, vm::setObHeight, stringResource(R.string.nether_height_min), Modifier.weight(1f))
            }
        }
        if (s.obNoCorners > 0) {
            ResultCard {
                StatRow(stringResource(R.string.nether_without_corners), stringResource(R.string.nether_without_corners_val, s.obNoCorners))
                StatRow(stringResource(R.string.nether_with_corners),    stringResource(R.string.nether_with_corners_val, s.obWithCorners))
            }
        }
    }
}

@Composable
private fun PortalsTab(s: NetherState, vm: NetherViewModel) {
    val hapticClick = rememberHapticClick()
    val hapticConfirm = rememberHapticConfirm()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InputCard {
            SpyglassTextField(s.newPortalName, vm::setNewPortalName, stringResource(R.string.nether_portal_name), keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
            Button(onClick = { hapticClick(); vm.savePortal() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.nether_save_portal))
            }
        }

        if (s.savedPortals.isEmpty()) {
            EmptyState(
                icon     = PixelIcons.Bookmark,
                title    = stringResource(R.string.nether_no_saved_portals),
                subtitle = stringResource(R.string.nether_no_saved_subtitle),
            )
        } else {
            s.savedPortals.forEach { p ->
                ResultCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { hapticConfirm(); vm.deletePortal(p) }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    StatRow("Overworld", "${p.owX}, ${p.owZ}")
                    StatRow("Nether",    "${p.netX}, ${p.netZ}")
                }
            }
        }
    }
}
