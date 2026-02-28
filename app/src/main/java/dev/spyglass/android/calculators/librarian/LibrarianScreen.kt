package dev.spyglass.android.calculators.librarian

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Enchant,
            title = "Librarian Guide",
            description = "Librarian enchantments are biome-locked in 1.21+. Find the right biome for the enchantment you need.",
        )

        // ── Biome Filter ──
        SectionHeader("Select Biome")
        FlowRow(
            modifier = Modifier.padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = selectedBiome == null,
                onClick = { selectedBiome = null },
                label = { Text("All Biomes", style = MaterialTheme.typography.labelSmall) },
            )
            BIOME_ENCHANTS.forEach { be ->
                FilterChip(
                    selected = selectedBiome == be.biome,
                    onClick = { selectedBiome = be.biome },
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
                Text("MASTER LEVEL EXCLUSIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(be.masterExclusive, style = MaterialTheme.typography.titleMedium, color = EnderPurple)
                Text(
                    "Guaranteed enchanted book trade at Master level.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                )
                SpyglassDivider()
                Text("COMMON POOL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                be.commonPool.forEach { enchant ->
                    Text(
                        "\u2022 $enchant",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Available at Novice through Expert levels.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // ── Key Info ──
        SectionHeader("How It Works")
        ResultCard {
            Text("BIOME DETERMINATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "A librarian's available enchantments are determined by the biome they first claimed a lectern in. Moving them to a different biome does NOT change their enchantment pool.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text("ENCHANTMENT LEVEL CAPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Some enchantments are capped below their maximum level from librarians:\n\u2022 Protection III (max IV requires anvil combining)\n\u2022 Fortune II (max III requires anvil combining)\n\u2022 Unbreaking II (max III requires anvil combining)",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text("STRATEGY TIPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "\u2022 For Mending, breed villagers in a Swamp biome\n\u2022 For Silk Touch, use a Snowy biome\n\u2022 Combine capped enchantments via anvil for max level\n\u2022 Break and replace the lectern to reroll trades",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Unavailable ──
        SectionHeader("Not Available from Librarians")
        ResultCard {
            Text("THESE ENCHANTMENTS CANNOT BE TRADED", style = MaterialTheme.typography.labelSmall, color = Red400)
            Spacer(Modifier.height(4.dp))
            Text(
                "\u2022 Trident: Channeling, Loyalty, Riptide, Impaling\n\u2022 Crossbow: Multishot, Piercing, Quick Charge\n\u2022 Fishing Rod: Luck of the Sea, Lure\n\u2022 Treasure Only: Soul Speed, Swift Sneak, Wind Burst\n\u2022 1.21+ Only: Breach, Density (Mace enchantments)",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpyglassDivider()
            Text(
                "These must be found from loot chests, fishing, mob drops, or enchanting tables.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
