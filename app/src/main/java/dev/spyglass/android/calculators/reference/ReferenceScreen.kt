package dev.spyglass.android.calculators.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

// ── Ore data ────────────────────────────────────────────────────────────────

private data class OreInfo(
    val name: String,
    val textureId: String,
    val yMin: Int,
    val yMax: Int,
    val peakY: Int,
)

private val ORES = listOf(
    OreInfo("Coal",      "coal_ore",      0,   320,  96),
    OreInfo("Iron",      "iron_ore",     -64,  320,  16),
    OreInfo("Copper",    "copper_ore",   -16,  112,  48),
    OreInfo("Gold",      "gold_ore",     -64,   32, -16),
    OreInfo("Lapis",     "lapis_ore",    -64,   64,   0),
    OreInfo("Redstone",  "redstone_ore", -64,   16, -59),
    OreInfo("Diamond",   "diamond_ore",  -64,   16, -59),
    OreInfo("Emerald",   "emerald_ore",  -16,  320, 236),
    OreInfo("Nether Gold","nether_gold_ore", 10, 117, 15),
    OreInfo("Ancient Debris","ancient_debris", 8, 119, 15),
)

// ── Color code data ─────────────────────────────────────────────────────────

private data class DyeColor(
    val name: String,
    val hex: String,
    val color: Color,
    val source: String,
)

private val DYE_COLORS = listOf(
    DyeColor("White",      "#F9FFFE", Color(0xFFF9FFFE), "Bone Meal / Lily of the Valley"),
    DyeColor("Orange",     "#F9801D", Color(0xFFF9801D), "Orange Tulip / Red + Yellow"),
    DyeColor("Magenta",    "#C74EBD", Color(0xFFC74EBD), "Allium / Purple + Pink"),
    DyeColor("Light Blue", "#3AB3DA", Color(0xFF3AB3DA), "Blue Orchid / Blue + White"),
    DyeColor("Yellow",     "#FED83D", Color(0xFFFED83D), "Dandelion / Sunflower"),
    DyeColor("Lime",       "#80C71F", Color(0xFF80C71F), "Sea Pickle / Green + White"),
    DyeColor("Pink",       "#F38BAA", Color(0xFFF38BAA), "Peony / Pink Tulip / Red + White"),
    DyeColor("Gray",       "#474F52", Color(0xFF474F52), "Black + White"),
    DyeColor("Light Gray", "#9D9D97", Color(0xFF9D9D97), "Azure Bluet / Oxeye Daisy"),
    DyeColor("Cyan",       "#169C9C", Color(0xFF169C9C), "Blue + Green"),
    DyeColor("Purple",     "#8932B8", Color(0xFF8932B8), "Blue + Red"),
    DyeColor("Blue",       "#3C44AA", Color(0xFF3C44AA), "Lapis Lazuli / Cornflower"),
    DyeColor("Brown",      "#835432", Color(0xFF835432), "Cocoa Beans"),
    DyeColor("Green",      "#5E7C16", Color(0xFF5E7C16), "Smelting Cactus"),
    DyeColor("Red",        "#B02E26", Color(0xFFB02E26), "Poppy / Rose Bush / Beetroot"),
    DyeColor("Black",      "#1D1D21", Color(0xFF1D1D21), "Ink Sac / Wither Rose"),
)

// ── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun ReferenceScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabIntroHeader(
            icon = PixelIcons.Bookmark,
            title = stringResource(R.string.reference_title),
            description = stringResource(R.string.reference_description),
        )

        // ── 1. Ore Levels by Y ──────────────────────────────────────────
        SectionHeader(stringResource(R.string.reference_ore_levels))
        ResultCard {
            ORES.forEachIndexed { i, ore ->
                if (i > 0) SpyglassDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val icon = ItemTextures.get(ore.textureId)
                    if (icon != null) {
                        SpyglassIconImage(icon, contentDescription = ore.name, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(ore.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.reference_ore_y_range, ore.yMin, ore.yMax), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    CategoryBadge(label = stringResource(R.string.reference_ore_peak, ore.peakY), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── 2. XP Table ─────────────────────────────────────────────────
        SectionHeader(stringResource(R.string.reference_xp_table))
        ResultCard {
            Text(stringResource(R.string.reference_levels_0_16), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_xp_per_level), stringResource(R.string.reference_xp_formula_low))
            StatRow(stringResource(R.string.reference_total_to_16), stringResource(R.string.reference_xp_315))
            SpyglassDivider()
            Text(stringResource(R.string.reference_levels_17_31), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_xp_per_level), stringResource(R.string.reference_xp_formula_mid))
            StatRow(stringResource(R.string.reference_total_to_30), stringResource(R.string.reference_xp_1395))
            SpyglassDivider()
            Text(stringResource(R.string.reference_levels_32_plus), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_xp_per_level), stringResource(R.string.reference_xp_formula_high))
            SpyglassDivider()
            Text(stringResource(R.string.reference_enchanting_table_slots), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_slot_1), stringResource(R.string.reference_slot_1_cost))
            StatRow(stringResource(R.string.reference_slot_2), stringResource(R.string.reference_slot_2_cost))
            StatRow(stringResource(R.string.reference_slot_3), stringResource(R.string.reference_slot_3_cost))
            SpyglassDivider()
            Text(stringResource(R.string.reference_anvil), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_too_expensive_cap), stringResource(R.string.reference_too_expensive_val))
            StatRow(stringResource(R.string.reference_rename_cost), stringResource(R.string.reference_rename_cost_val))
            StatRow(stringResource(R.string.reference_cost_doubles), stringResource(R.string.reference_cost_doubles_val))
        }

        // ── 3. Brewing Chart ────────────────────────────────────────────
        SectionHeader(stringResource(R.string.reference_brewing_chart))
        ResultCard {
            Text(stringResource(R.string.reference_base_chain), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_water_bottle), stringResource(R.string.reference_starting_point))
            StatRow(stringResource(R.string.reference_plus_nether_wart), stringResource(R.string.reference_awkward_potion))
            SpyglassDivider()
            Text(stringResource(R.string.reference_effect_ingredients), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Sugar", "Speed")
            StatRow("Rabbit Foot", "Leaping")
            StatRow("Glistering Melon", "Healing")
            StatRow("Spider Eye", "Poison")
            StatRow("Ghast Tear", "Regeneration")
            StatRow("Blaze Powder", "Strength")
            StatRow("Magma Cream", "Fire Resistance")
            StatRow("Golden Carrot", "Night Vision")
            StatRow("Pufferfish", "Water Breathing")
            StatRow("Phantom Membrane", "Slow Falling")
            StatRow("Turtle Helmet", "Turtle Master")
            StatRow("Breeze Rod", "Wind Charged")
            SpyglassDivider()
            Text(stringResource(R.string.reference_modifiers), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Redstone", stringResource(R.string.reference_extends_duration))
            StatRow("Glowstone", stringResource(R.string.reference_amplifies_effect))
            StatRow("Fermented Spider Eye", stringResource(R.string.reference_corrupts))
            StatRow("Gunpowder", stringResource(R.string.reference_splash_version))
            StatRow("Dragon Breath", stringResource(R.string.reference_lingering_version))
        }

        // ── 4. Redstone Basics ──────────────────────────────────────────
        SectionHeader(stringResource(R.string.reference_redstone_basics))
        ResultCard {
            Text(stringResource(R.string.reference_signal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_max_range), stringResource(R.string.reference_max_range_val))
            StatRow(stringResource(R.string.reference_strength_loss), stringResource(R.string.reference_strength_loss_val))
            StatRow(stringResource(R.string.reference_redstone_tick), stringResource(R.string.reference_redstone_tick_val))
            SpyglassDivider()
            Text(stringResource(R.string.reference_repeater), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_delay_settings), stringResource(R.string.reference_delay_val))
            StatRow(stringResource(R.string.reference_resets_signal), stringResource(R.string.reference_resets_val))
            StatRow(stringResource(R.string.reference_lock_mode), stringResource(R.string.reference_lock_mode_val))
            SpyglassDivider()
            Text(stringResource(R.string.reference_comparator), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_compare_mode), stringResource(R.string.reference_compare_mode_val))
            StatRow(stringResource(R.string.reference_subtract_mode), stringResource(R.string.reference_subtract_mode_val))
            StatRow(stringResource(R.string.reference_container_read), stringResource(R.string.reference_container_read_val))
            SpyglassDivider()
            Text(stringResource(R.string.reference_key_components), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_piston), stringResource(R.string.reference_piston_val))
            StatRow(stringResource(R.string.reference_sticky_piston), stringResource(R.string.reference_sticky_piston_val))
            StatRow(stringResource(R.string.reference_observer), stringResource(R.string.reference_observer_val))
            StatRow(stringResource(R.string.reference_hopper), stringResource(R.string.reference_hopper_val))
            StatRow(stringResource(R.string.reference_dropper), stringResource(R.string.reference_dropper_val))
            StatRow(stringResource(R.string.reference_daylight_sensor), stringResource(R.string.reference_daylight_sensor_val))
        }

        // ── 5. Times & Durations ──────────────────────────────────────
        SectionHeader(stringResource(R.string.reference_times_durations))
        ResultCard {
            Text(stringResource(R.string.reference_day_night_cycle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_full_cycle), stringResource(R.string.reference_full_cycle_val))
            StatRow(stringResource(R.string.reference_daytime), stringResource(R.string.reference_daytime_val))
            StatRow(stringResource(R.string.reference_sunset), stringResource(R.string.reference_sunset_val))
            StatRow(stringResource(R.string.reference_night), stringResource(R.string.reference_night_val))
            StatRow(stringResource(R.string.reference_sunrise), stringResource(R.string.reference_sunrise_val))
            StatRow(stringResource(R.string.reference_sleep_available), stringResource(R.string.reference_sleep_available_val))
            SpyglassDivider()
            Text(stringResource(R.string.reference_breeding_growth), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_breeding_cooldown), stringResource(R.string.reference_breeding_cooldown_val))
            StatRow(stringResource(R.string.reference_baby_growth), stringResource(R.string.reference_baby_growth_val))
            StatRow(stringResource(R.string.reference_speed_up_growth), stringResource(R.string.reference_speed_up_growth_val))
            StatRow(stringResource(R.string.reference_villager_restock), stringResource(R.string.reference_villager_restock_val))
            SpyglassDivider()
            Text(stringResource(R.string.reference_crop_growth), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_wheat), stringResource(R.string.reference_wheat_val))
            StatRow(stringResource(R.string.reference_beetroot), stringResource(R.string.reference_beetroot_val))
            StatRow(stringResource(R.string.reference_nether_wart), stringResource(R.string.reference_nether_wart_val))
            StatRow(stringResource(R.string.reference_sugarcane), stringResource(R.string.reference_sugarcane_val))
            StatRow(stringResource(R.string.reference_bamboo), stringResource(R.string.reference_bamboo_val))
            StatRow(stringResource(R.string.reference_melon_pumpkin), stringResource(R.string.reference_melon_pumpkin_val))
            SpyglassDivider()
            Text(stringResource(R.string.reference_other_timers), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow(stringResource(R.string.reference_smelting_per_item), stringResource(R.string.reference_smelting_per_item_val))
            StatRow(stringResource(R.string.reference_item_despawn), stringResource(R.string.reference_item_despawn_val))
            StatRow(stringResource(R.string.reference_mob_despawn), stringResource(R.string.reference_mob_despawn_val))
            StatRow(stringResource(R.string.reference_drowning), stringResource(R.string.reference_drowning_val))
            StatRow(stringResource(R.string.reference_phantoms), stringResource(R.string.reference_phantoms_val))
            StatRow(stringResource(R.string.reference_fire_spread), stringResource(R.string.reference_fire_spread_val))
            StatRow(stringResource(R.string.reference_raid_timeout), stringResource(R.string.reference_raid_timeout_val))
        }

        // ── 6. Color Codes ──────────────────────────────────────────────
        SectionHeader(stringResource(R.string.reference_dye_colors))
        ResultCard {
            DYE_COLORS.forEachIndexed { i, dye ->
                if (i > 0) SpyglassDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(dye.color, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dye.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(dye.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(dye.hex, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
