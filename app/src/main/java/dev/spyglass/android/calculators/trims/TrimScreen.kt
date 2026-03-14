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
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

private data class TrimTemplate(
    val name: String,
    val locationKey: String,
    val structure: String,
    val structureId: String,
    val chance: String,
    val duplicationMaterial: String,
    val notes: String = "",
)

@Composable
private fun trimLocationLabel(key: String): String = when (key) {
    "overworld" -> stringResource(R.string.trim_location_overworld)
    "nether" -> stringResource(R.string.trim_location_nether)
    "the_end" -> stringResource(R.string.trim_location_the_end)
    "deep_dark" -> stringResource(R.string.trim_location_deep_dark)
    "ocean" -> stringResource(R.string.trim_location_ocean)
    else -> key
}

@Composable
private fun trimNoteLabel(note: String): String = when (note) {
    "rarest" -> stringResource(R.string.trim_note_rarest)
    "mob_drop" -> stringResource(R.string.trim_note_mob_drop)
    "brushing" -> stringResource(R.string.trim_note_brushing)
    "ominous" -> stringResource(R.string.trim_note_ominous)
    else -> note
}

private val TRIM_TEMPLATES = listOf(
    TrimTemplate("Sentry", "overworld", "Pillager Outpost", "pillager_outpost", "~20%", "Cobblestone"),
    TrimTemplate("Dune", "overworld", "Desert Pyramid", "desert_pyramid", "Standard", "Sandstone"),
    TrimTemplate("Coast", "overworld", "Shipwreck", "shipwreck", "Standard", "Cobblestone"),
    TrimTemplate("Wild", "overworld", "Jungle Pyramid", "jungle_pyramid", "Standard", "Mossy Cobblestone"),
    TrimTemplate("Vex", "overworld", "Woodland Mansion", "woodland_mansion", "~4.8%", "Cobblestone"),
    TrimTemplate("Eye", "overworld", "Stronghold Library", "stronghold", "Standard", "End Stone"),
    TrimTemplate("Ward", "deep_dark", "Ancient City", "ancient_city", "Rare", "Cobbled Deepslate"),
    TrimTemplate("Silence", "deep_dark", "Ancient City", "ancient_city", "1.2%", "Cobbled Deepslate", "rarest"),
    TrimTemplate("Snout", "nether", "Bastion Remnant", "bastion_remnant", "~4.8%", "Blackstone"),
    TrimTemplate("Rib", "nether", "Nether Fortress", "nether_fortress", "Standard", "Netherrack"),
    TrimTemplate("Spire", "the_end", "End City", "end_city", "Standard", "Purpur Block"),
    TrimTemplate("Tide", "ocean", "Elder Guardian Drop", "ocean_monument", "20%", "Prismarine", "mob_drop"),
    TrimTemplate("Wayfinder", "overworld", "Trail Ruins", "trail_ruins", "~8.3%", "Terracotta", "brushing"),
    TrimTemplate("Raiser", "overworld", "Trail Ruins", "trail_ruins", "~8.3%", "Terracotta", "brushing"),
    TrimTemplate("Shaper", "overworld", "Trail Ruins", "trail_ruins", "~8.3%", "Terracotta", "brushing"),
    TrimTemplate("Host", "overworld", "Trail Ruins", "trail_ruins", "~8.3%", "Terracotta", "brushing"),
    TrimTemplate("Bolt", "overworld", "Trial Chambers Vault", "trial_chambers", "~6.3%", "Copper Block"),
    TrimTemplate("Flow", "overworld", "Trial Chambers Ominous Vault", "trial_chambers", "22.5%", "Breeze Rod", "ominous"),
)

private data class TrimMaterial(
    val name: String,
    val item: String,
    val colorKey: String,
)

@Composable
private fun trimColorLabel(key: String): String = when (key) {
    "silver_gray" -> stringResource(R.string.trim_color_silver_gray)
    "reddish_orange" -> stringResource(R.string.trim_color_reddish_orange)
    "golden_yellow" -> stringResource(R.string.trim_color_golden_yellow)
    "deep_blue" -> stringResource(R.string.trim_color_deep_blue)
    "green" -> stringResource(R.string.trim_color_green)
    "light_blue" -> stringResource(R.string.trim_color_light_blue)
    "dark_gray" -> stringResource(R.string.trim_color_dark_gray)
    "red" -> stringResource(R.string.trim_color_red)
    "purple" -> stringResource(R.string.trim_color_purple)
    "white" -> stringResource(R.string.trim_color_white)
    "yellow_orange" -> stringResource(R.string.trim_color_yellow_orange)
    else -> key
}

private val TRIM_MATERIALS = listOf(
    TrimMaterial("Iron", "Iron Ingot", "silver_gray"),
    TrimMaterial("Copper", "Copper Ingot", "reddish_orange"),
    TrimMaterial("Gold", "Gold Ingot", "golden_yellow"),
    TrimMaterial("Lapis", "Lapis Lazuli", "deep_blue"),
    TrimMaterial("Emerald", "Emerald", "green"),
    TrimMaterial("Diamond", "Diamond", "light_blue"),
    TrimMaterial("Netherite", "Netherite Ingot", "dark_gray"),
    TrimMaterial("Redstone", "Redstone Dust", "red"),
    TrimMaterial("Amethyst", "Amethyst Shard", "purple"),
    TrimMaterial("Quartz", "Nether Quartz", "white"),
    TrimMaterial("Resin", "Resin Brick", "yellow_orange"),
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
            title = stringResource(R.string.trim_title),
            description = stringResource(R.string.trim_description),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("templates" to stringResource(R.string.trim_templates), "materials" to stringResource(R.string.trim_materials), "howto" to stringResource(R.string.trim_how_to)).forEach { (key, label) ->
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

// ── Templates ────────────────────────────────────────────────────────────────

@Composable
private fun TemplatesSection(onStructureTap: (String) -> Unit = {}) {
    val hapticClick = rememberHapticClick()
    SectionHeader(stringResource(R.string.trim_templates_header))

    TRIM_TEMPLATES.forEach { template ->
        val locationColor = when (template.locationKey) {
            "nether"               -> NetherRed
            "the_end"              -> EnderPurple
            "deep_dark", "ocean"   -> PotionBlue
            else                   -> Emerald
        }
        ResultCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(template.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                CategoryBadge(label = trimLocationLabel(template.locationKey), color = locationColor)
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text(stringResource(R.string.trim_structure_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(
                    template.structure,
                    style = MaterialTheme.typography.bodySmall,
                    color = PotionBlue,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { hapticClick(); onStructureTap(template.structureId) },
                )
            }
            StatRow(stringResource(R.string.trim_drop_chance), template.chance)
            StatRow(stringResource(R.string.trim_duplicate_with), stringResource(R.string.trim_duplicate_val, template.duplicationMaterial))
            if (template.notes.isNotBlank()) {
                Text(trimNoteLabel(template.notes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Materials ────────────────────────────────────────────────────────────────

@Composable
private fun MaterialsSection() {
    SectionHeader(stringResource(R.string.trim_materials_header))
    ResultCard {
        Text(
            stringResource(R.string.trim_materials_desc),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
        )
        SpyglassDivider()
        TRIM_MATERIALS.forEach { mat ->
            StatRow(mat.name, "${mat.item} \u2022 ${trimColorLabel(mat.colorKey)}")
        }
        SpyglassDivider()
        Text(
            stringResource(R.string.trim_materials_darker),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
        )
    }

    SectionHeader(stringResource(R.string.trim_combinations))
    ResultCard {
        StatRow(stringResource(R.string.trim_templates), "18")
        StatRow(stringResource(R.string.trim_materials), "11")
        StatRow(stringResource(R.string.trim_armor_types), stringResource(R.string.trim_armor_types_val))
        StatRow(stringResource(R.string.trim_armor_pieces), stringResource(R.string.trim_armor_pieces_val))
        SpyglassDivider()
        StatRow(stringResource(R.string.trim_total_looks), stringResource(R.string.trim_total_looks_val))
    }
}

// ── How To ───────────────────────────────────────────────────────────────────

@Composable
private fun HowToSection() {
    SectionHeader(stringResource(R.string.trim_how_to_header))
    ResultCard {
        Text(stringResource(R.string.trim_smithing_table), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.trim_smithing_steps),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpyglassDivider()
        Text(stringResource(R.string.trim_key_notes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.trim_key_notes_text),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpyglassDivider()
        Text(stringResource(R.string.trim_duplicating), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.trim_duplicating_text),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
