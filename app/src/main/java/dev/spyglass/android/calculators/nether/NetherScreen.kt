package dev.spyglass.android.calculators.nether

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun NetherScreen(vm: NetherViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    var subTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Nether Tools", icon = PixelIcons.Nether)

        TabRow(selectedTabIndex = subTab, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            listOf("Convert", "Obsidian", "Portals").forEachIndexed { i, t ->
                Tab(selected = subTab == i, onClick = { subTab = i }, text = { Text(t) })
            }
        }

        when (subTab) {
            0 -> ConvertTab(s, vm)
            1 -> ObsidianTab(s, vm)
            2 -> PortalsTab(s, vm)
        }

        Text(
            "Convert coordinates between the Overworld and Nether (8:1 ratio), calculate how much obsidian you need for a custom-sized portal, or save portal locations to keep track of your network.",
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
            Text("Input coordinates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
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
                if (s.yOut.isNotEmpty()) StatRow("Y (unchanged)", s.yOut)
                if (s.zOut.isNotEmpty()) StatRow("$outDim Z", s.zOut)
                if (s.facing.isNotEmpty()) StatRow("Facing", s.facing)
            }
        }

    }
}

@Composable
private fun ObsidianTab(s: NetherState, vm: NetherViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InputCard {
            Text("Portal interior dimensions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpyglassTextField(s.obWidth, vm::setObWidth, "Width (min 2)", Modifier.weight(1f))
                SpyglassTextField(s.obHeight, vm::setObHeight, "Height (min 3)", Modifier.weight(1f))
            }
        }
        if (s.obNoCorners > 0) {
            ResultCard {
                StatRow("Without corners", "${s.obNoCorners} obsidian")
                StatRow("With corners",    "${s.obWithCorners} obsidian")
            }
        }
    }
}

@Composable
private fun PortalsTab(s: NetherState, vm: NetherViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InputCard {
            SpyglassTextField(s.newPortalName, vm::setNewPortalName, "Portal name", keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
            Button(onClick = vm::savePortal, modifier = Modifier.fillMaxWidth()) {
                Text("Save current coordinates as portal")
            }
        }

        if (s.savedPortals.isEmpty()) {
            EmptyState(
                icon     = PixelIcons.Bookmark,
                title    = "No saved portals",
                subtitle = "Save coordinates above to track your portals",
            )
        } else {
            s.savedPortals.forEach { p ->
                ResultCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { vm.deletePortal(p) }) {
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
