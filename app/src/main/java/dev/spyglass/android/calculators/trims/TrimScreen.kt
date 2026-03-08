package dev.spyglass.android.calculators.trims

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.spyglass.android.core.ui.*

private data class TrimTemplate(
    val name: String,
    val location: String,
    val structure: String,
    val structureId: String,
    val chance: String,
    val duplicationMaterial: String,
    val notes: String = "",
)

private val TRIM_TEMPLATES = listOf(
    TrimTemplate("Sentry", "Overworld", "Pillager Outpost", "pillager_outpost", "~20%", "Cobblestone"),
    TrimTemplate("Dune", "Overworld", "Desert Pyramid", "desert_pyramid", "Standard", "Sandstone"),
    TrimTemplate("Coast", "Overworld", "Shipwreck", "shipwreck", "Standard", "Cobblestone"),
    TrimTemplate("Wild", "Overworld", "Jungle Pyramid", "jungle_pyramid", "Standard", "Mossy Cobblestone"),
    TrimTemplate("Vex", "Overworld", "Woodland Mansion", "woodland_mansion", "~4.8%", "Cobblestone"),
    TrimTemplate("Eye", "Overworld", "Stronghold Library", "stronghold", "Standard", "End Stone"),
    TrimTemplate("Ward", "Deep Dark", "Ancient City", "ancient_city", "Rare", "Cobbled Deepslate"),
    TrimTemplate("Silence", "Deep Dark", "Ancient City", "ancient_city", "1.2%", "Cobbled Deepslate", "Rarest trim template"),
    TrimTemplate("Snout", "Nether", "Bastion Remnant", "bastion_remnant", "~4.8%", "Blackstone"),
    TrimTemplate("Rib", "Nether", "Nether Fortress", "nether_fortress", "Standard", "Netherrack"),
    TrimTemplate("Spire", "The End", "End City", "end_city", "Standard", "Purpur Block"),
    TrimTemplate("Tide", "Ocean", "Elder Guardian Drop", "ocean_monument", "20%", "Prismarine", "Only trim from mob drop"),
    TrimTemplate("Wayfinder", "Overworld", "Trail Ruins", "trail_ruins", "~8.3%", "Terracotta", "Requires brushing"),
    TrimTemplate("Raiser", "Overworld", "Trail Ruins", "trail_ruins", "~8.3%", "Terracotta", "Requires brushing"),
    TrimTemplate("Shaper", "Overworld", "Trail Ruins", "trail_ruins", "~8.3%", "Terracotta", "Requires brushing"),
    TrimTemplate("Host", "Overworld", "Trail Ruins", "trail_ruins", "~8.3%", "Terracotta", "Requires brushing"),
    TrimTemplate("Bolt", "Overworld", "Trial Chambers Vault", "trial_chambers", "~6.3%", "Copper Block"),
    TrimTemplate("Flow", "Overworld", "Trial Chambers Ominous Vault", "trial_chambers", "22.5%", "Breeze Rod", "Ominous vault exclusive"),
)

private data class TrimMaterial(
    val name: String,
    val item: String,
    val colorDesc: String,
)

private val TRIM_MATERIALS = listOf(
    TrimMaterial("Iron", "Iron Ingot", "Silver/gray"),
    TrimMaterial("Copper", "Copper Ingot", "Reddish orange"),
    TrimMaterial("Gold", "Gold Ingot", "Golden yellow"),
    TrimMaterial("Lapis", "Lapis Lazuli", "Deep blue"),
    TrimMaterial("Emerald", "Emerald", "Green"),
    TrimMaterial("Diamond", "Diamond", "Light blue/cyan"),
    TrimMaterial("Netherite", "Netherite Ingot", "Dark gray/black"),
    TrimMaterial("Redstone", "Redstone Dust", "Red"),
    TrimMaterial("Amethyst", "Amethyst Shard", "Purple"),
    TrimMaterial("Quartz", "Nether Quartz", "White/light"),
    TrimMaterial("Resin", "Resin Brick", "Yellow/orange (1.21.4)"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrimScreen(onStructureTap: (String) -> Unit = {}) {
    val hapticClick = rememberHapticClick()
    var selectedSection by remember { mutableStateOf("templates") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Item,
            title = "Armor Trims",
            description = "All 18 armor trim templates, 11 trim materials, and where to find them.",
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("templates" to "Templates", "materials" to "Materials", "howto" to "How To").forEach { (key, label) ->
                FilterChip(
                    selected = selectedSection == key,
                    onClick = { hapticClick(); selectedSection = key },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        when (selectedSection) {
            "templates" -> TemplatesSection(onStructureTap = onStructureTap)
            "materials" -> MaterialsSection()
            "howto" -> HowToSection()
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TemplatesSection(onStructureTap: (String) -> Unit = {}) {
    val hapticClick = rememberHapticClick()
    SectionHeader("Trim Templates (18)")

    TRIM_TEMPLATES.forEach { template ->
        val locationColor = when (template.location) {
            "Nether" -> NetherRed
            "The End" -> EnderPurple
            "Deep Dark" -> PotionBlue
            "Ocean" -> PotionBlue
            else -> Emerald
        }
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(template.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                CategoryBadge(label = template.location, color = locationColor)
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text("Structure  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(
                    template.structure,
                    style = MaterialTheme.typography.bodySmall,
                    color = PotionBlue,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { hapticClick(); onStructureTap(template.structureId) },
                )
            }
            StatRow("Drop Chance", template.chance)
            StatRow("Duplicate With", "7 Diamonds + ${template.duplicationMaterial}")
            if (template.notes.isNotBlank()) {
                Text(template.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MaterialsSection() {
    SectionHeader("Trim Materials (11)")
    ResultCard {
        Text(
            "Each material changes the color of the trim pattern on the armor.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
        )
        SpyglassDivider()
        TRIM_MATERIALS.forEach { mat ->
            StatRow(mat.name, "${mat.item} \u2022 ${mat.colorDesc}")
        }
        SpyglassDivider()
        Text(
            "If the trim material matches the armor material (e.g., Diamond trim on Diamond armor), the pattern uses a darker color palette.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
        )
    }

    SectionHeader("Combinations")
    ResultCard {
        StatRow("Templates", "18")
        StatRow("Materials", "11")
        StatRow("Armor Types", "6 (leather, chainmail, iron, gold, diamond, netherite)")
        StatRow("Armor Pieces", "4 (helmet, chestplate, leggings, boots)")
        SpyglassDivider()
        StatRow("Total Unique Looks", "18 \u00d7 11 \u00d7 6 \u00d7 4 = 4,752")
    }
}

@Composable
private fun HowToSection() {
    SectionHeader("How to Trim Armor")
    ResultCard {
        Text("SMITHING TABLE RECIPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "1. Open the Smithing Table\n2. Place a Trim Template in the left slot\n3. Place the armor piece in the middle slot\n4. Place the trim material in the right slot\n5. Take the trimmed armor from the output",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpyglassDivider()
        Text("KEY NOTES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "\u2022 Templates are consumed during trimming\n\u2022 Trims are purely cosmetic (no gameplay effect)\n\u2022 You can re-trim armor (overwrites the previous pattern)\n\u2022 Enchantments are preserved when trimming\n\u2022 Works on all armor materials",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpyglassDivider()
        Text("DUPLICATING TEMPLATES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "In a crafting table:\n1 Template + 7 Diamonds + 1 Duplication Material = 2 Templates\n\nEach template has a specific duplication material (see the Templates tab). This makes rare templates renewable after finding the first one.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
