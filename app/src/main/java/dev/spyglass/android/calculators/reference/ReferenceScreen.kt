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
            title = "Quick Reference",
            description = "Cheat sheets for ores, XP, brewing, redstone, times, and dye colors",
        )

        // ── 1. Ore Levels by Y ──────────────────────────────────────────
        SectionHeader("Ore Levels by Y")
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
                    Text("Y ${ore.yMin} to ${ore.yMax}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    CategoryBadge(label = "Peak ${ore.peakY}", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── 2. XP Table ─────────────────────────────────────────────────
        SectionHeader("XP Table")
        ResultCard {
            Text("Levels 0-16", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("XP per level", "2L + 7")
            StatRow("Total to 16", "315 XP")
            SpyglassDivider()
            Text("Levels 17-31", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("XP per level", "5L - 38")
            StatRow("Total to 30", "1,395 XP")
            SpyglassDivider()
            Text("Levels 32+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("XP per level", "9L - 158")
            SpyglassDivider()
            Text("Enchanting Table Slots", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Slot 1 (cheapest)", "1 Lapis + 1 level")
            StatRow("Slot 2 (mid)", "2 Lapis + 2 levels")
            StatRow("Slot 3 (best)", "3 Lapis + 3 levels")
            SpyglassDivider()
            Text("Anvil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Too Expensive cap", "40 levels")
            StatRow("Rename cost", "1 level (first time)")
            StatRow("Cost doubles", "Each prior work penalty")
        }

        // ── 3. Brewing Chart ────────────────────────────────────────────
        SectionHeader("Brewing Chart")
        ResultCard {
            Text("Base Chain", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Water Bottle", "Starting point")
            StatRow("+ Nether Wart", "Awkward Potion")
            SpyglassDivider()
            Text("Effect Ingredients", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
            Text("Modifiers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Redstone", "Extends duration")
            StatRow("Glowstone", "Amplifies effect")
            StatRow("Fermented Spider Eye", "Corrupts (inverts)")
            StatRow("Gunpowder", "Splash version")
            StatRow("Dragon Breath", "Lingering version")
        }

        // ── 4. Redstone Basics ──────────────────────────────────────────
        SectionHeader("Redstone Basics")
        ResultCard {
            Text("Signal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Max range", "15 blocks")
            StatRow("Strength loss", "1 per block traveled")
            StatRow("Redstone tick", "0.1 seconds (2 game ticks)")
            SpyglassDivider()
            Text("Repeater", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Delay settings", "1-4 ticks (0.1-0.4s)")
            StatRow("Resets signal", "Back to 15")
            StatRow("Lock mode", "Side input freezes state")
            SpyglassDivider()
            Text("Comparator", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Compare mode", "Output = back if back >= side")
            StatRow("Subtract mode", "Output = back - side")
            StatRow("Container read", "Signal = fill level (0-15)")
            SpyglassDivider()
            Text("Key Components", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Piston", "Pushes up to 12 blocks")
            StatRow("Sticky Piston", "Pushes and pulls")
            StatRow("Observer", "Detects block updates")
            StatRow("Hopper", "5 items/sec transfer rate")
            StatRow("Dropper", "Ejects 1 item per pulse")
            StatRow("Daylight Sensor", "Outputs based on sky light")
        }

        // ── 5. Times & Durations ──────────────────────────────────────
        SectionHeader("Times & Durations")
        ResultCard {
            Text("Day/Night Cycle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Full cycle", "20 minutes (24,000 ticks)")
            StatRow("Daytime", "10 min (tick 0-12,000)")
            StatRow("Sunset", "1.5 min (tick 12,000-13,800)")
            StatRow("Night", "7 min (tick 13,800-22,200)")
            StatRow("Sunrise", "1.5 min (tick 22,200-24,000)")
            StatRow("Sleep available", "Tick 12,542-23,460")
            SpyglassDivider()
            Text("Breeding & Growth", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Breeding cooldown", "5 minutes")
            StatRow("Baby growth time", "20 minutes")
            StatRow("Speed up growth", "Feed baby its breeding food")
            StatRow("Villager restock", "2x per in-game day")
            SpyglassDivider()
            Text("Crop Growth (avg)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Wheat / Carrots / Potatoes", "~25 minutes (8 stages)")
            StatRow("Beetroot", "~25 minutes (4 stages)")
            StatRow("Nether Wart", "~10 minutes (4 stages)")
            StatRow("Sugarcane / Cactus", "~18 min per block")
            StatRow("Bamboo", "~4 min per block")
            StatRow("Melon / Pumpkin stem", "~25 min, then fruit spawns")
            SpyglassDivider()
            Text("Other Timers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            StatRow("Smelting per item", "10 seconds (200 ticks)")
            StatRow("Item despawn", "5 minutes")
            StatRow("Mob despawn", ">128 blocks = instant")
            StatRow("Drowning", "15 sec air, then 1 hp/sec")
            StatRow("Phantoms spawn after", "3+ days without sleep")
            StatRow("Fire spread check", "Every 2 seconds")
            StatRow("Raid timeout", "40 minutes")
        }

        // ── 6. Color Codes ──────────────────────────────────────────────
        SectionHeader("Dye Colors")
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
