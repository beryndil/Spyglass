package dev.spyglass.android.calculators.librarian

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

private data class BiomeEnchants(
    val biome: String,
    val masterExclusive: String,
    val commonPool: List<String>,
)

private val BIOME_ENCHANTS = listOf(
    BiomeEnchants(
        biome = "Plains",
        masterExclusive = "Protection III",
        commonPool = listOf("Punch II", "Smite V", "Bane of Arthropods V"),
    ),
    BiomeEnchants(
        biome = "Desert",
        masterExclusive = "Efficiency III",
        commonPool = listOf("Fire Protection IV", "Infinity", "Thorns III"),
    ),
    BiomeEnchants(
        biome = "Savanna",
        masterExclusive = "Sharpness III",
        commonPool = listOf("Knockback II", "Curse of Binding", "Sweeping Edge III"),
    ),
    BiomeEnchants(
        biome = "Snow",
        masterExclusive = "Silk Touch",
        commonPool = listOf("Aqua Affinity", "Looting III", "Frost Walker II"),
    ),
    BiomeEnchants(
        biome = "Taiga",
        masterExclusive = "Fortune II",
        commonPool = listOf("Blast Protection IV", "Fire Aspect II", "Flame I"),
    ),
    BiomeEnchants(
        biome = "Jungle",
        masterExclusive = "Unbreaking II",
        commonPool = listOf("Feather Falling IV", "Projectile Protection IV", "Power V"),
    ),
    BiomeEnchants(
        biome = "Swamp",
        masterExclusive = "Mending",
        commonPool = listOf("Depth Strider III", "Respiration III", "Curse of Vanishing"),
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibrarianScreen() {
    var selectedBiome by remember { mutableStateOf<String?>(null) }
    val hapticClick = rememberHapticClick()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Enchant,
            title = stringResource(R.string.librarian_title),
            description = stringResource(R.string.librarian_description),
        )

        // Experimental notice
        ResultCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("⚗", style = MaterialTheme.typography.titleMedium)
                Column {
                    Text(
                        stringResource(R.string.librarian_experimental),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFC107),
                    )
                    Text(
                        stringResource(R.string.librarian_experimental_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Biome Filter ──
        SectionHeader(stringResource(R.string.librarian_select_biome))
        FlowRow(
            modifier = Modifier.padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = selectedBiome == null,
                onClick = { hapticClick(); selectedBiome = null },
                label = { Text(stringResource(R.string.librarian_all_biomes), style = MaterialTheme.typography.labelSmall) },
            )
            BIOME_ENCHANTS.forEach { be ->
                FilterChip(
                    selected = selectedBiome == be.biome,
                    onClick = { hapticClick(); selectedBiome = be.biome },
                    label = { Text(be.biome, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // ── Biome Cards ──
        val displayed = if (selectedBiome != null)
            BIOME_ENCHANTS.filter { it.biome == selectedBiome }
        else BIOME_ENCHANTS

        displayed.forEach { be ->
            SectionHeader(be.biome)
            ResultCard {
                Text(stringResource(R.string.librarian_master_exclusive), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(be.masterExclusive, style = MaterialTheme.typography.titleMedium, color = EnderPurple)
                Text(
                    stringResource(R.string.librarian_master_guaranteed),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                )
                SpyglassDivider()
                Text(stringResource(R.string.librarian_common_pool), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                be.commonPool.forEach { enchant ->
                    Text(
                        "\u2022 $enchant",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.librarian_common_available),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // ── Key Info ──
        SectionHeader(stringResource(R.string.librarian_how_it_works))
        ResultCard {
            Text(stringResource(R.string.librarian_biome_determination), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.librarian_biome_determination_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(stringResource(R.string.librarian_level_caps), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.librarian_level_caps_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(stringResource(R.string.librarian_strategy_tips), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.librarian_strategy_tips_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Unavailable ──
        SectionHeader(stringResource(R.string.librarian_not_available))
        ResultCard {
            Text(stringResource(R.string.librarian_cannot_be_traded), style = MaterialTheme.typography.labelSmall, color = Red400)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.librarian_unavailable_list),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(
                stringResource(R.string.librarian_found_from),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
