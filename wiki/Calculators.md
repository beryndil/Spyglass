# Calculators

Spyglass includes 19 built-in tools for building, planning, and reference. Access them from the **Calculators** tab in the bottom navigation.

## Tools Overview

| # | Calculator | Purpose |
|---|-----------|---------|
| 1 | [Todo List](#todo-list) | Task management with item linking |
| 2 | [Shopping Lists](#shopping-lists) | Crafting plan generator |
| 3 | [Enchant Optimizer](#enchant-optimizer) | Optimal anvil combining order |
| 4 | [Block Fill](#block-fill) | Volume calculator for block placement |
| 5 | [Shape Designer](#shape-designer) | Layer-by-layer 3D shape builder |
| 6 | [Maze Maker](#maze-maker) | Procedural maze generation |
| 7 | [Storage](#storage) | Container capacity calculator |
| 8 | [Smelting](#smelting) | Fuel requirements and efficiency |
| 9 | [Nether Tools](#nether-tools) | Coordinate converter, portal calculator, portal tracker |
| 10 | [Game Clock](#game-clock) | In-game day/night cycle tracker |
| 11 | [Light Spacing](#light-spacing) | Optimal torch placement guide |
| 12 | [Banner Designer](#banner-designer) | Custom banner pattern composer |
| 13 | [Food Browser](#food-browser) | Food stats with hunger/saturation |
| 14 | [Notes](#notes) | Personal note storage |
| 15 | [Waypoints](#waypoints) | Location bookmarking |
| 16 | [Redstone](#redstone) | Comparator signal calculator |
| 17 | [Librarian Guide](#librarian-guide) | Biome-locked enchantment reference |
| 18 | [Loot Tables](#loot-tables) | Structure loot reference |
| 19 | [Armor Trims](#armor-trims) | Trim template and material reference |

---

## Todo List
Personal task tracker with Minecraft item linking.

**Inputs:**
- Free-form task titles (max 200 characters)
- Item-linked tasks with quantities (searchable from blocks/items database)

**Features:**
- Toggle completion status from the list
- Quantity badges formatted as chests/stacks/items (e.g., 1,728 items = 1 chest)
- Link tasks to shopping lists
- "Clear completed" batch action
- Persistent storage in local database

---

## Shopping Lists
Create named shopping lists with automatic crafting plan generation.

**Inputs:**
- List name
- Items selected from the game database with quantities

**Features:**
- Quantity steppers (increment/decrement or direct input)
- Per-item expansion showing craft requirements
- Consolidated **Crafting Plan** showing:
  - Raw materials to gather
  - Craft steps organized by depth
  - Quantity of each craft needed
- Multiple lists supported

---

## Enchant Optimizer
Calculate the optimal order to combine enchanted books on an anvil to minimize XP cost and avoid the "Too Expensive" 40-level cap.

**Inputs:**
- Item type: 13 types (swords, pickaxes, helmets, boots, bows, crossbows, tridents, etc.)
- Enchantments: select from available enchants for the chosen item, set levels (I-V)

**Outputs:**
- Step-by-step anvil operations (book combining, then item application)
- Per-step XP cost
- "Too Expensive" warnings
- Total XP cost

**Features:**
- Incompatibility detection (e.g., Infinity and Mending)
- Prior-work penalty explanation with formulas
- Binary tree combining strategy (cheapest enchants combined first)

---

## Block Fill
Calculate total blocks needed for any rectangular volume.

**Inputs:**
- Width (blocks or chunks)
- Length (blocks or chunks)
- Height (blocks)
- Hollow toggle (shell only, removes interior)

**Outputs:**
- Total blocks needed
- Stacks (64), single chests (1,728), double chests (3,456), shulker boxes

---

## Shape Designer
Generate layer-by-layer block coordinates for 3D shapes.

**Shapes available (12):**
Circle, Sphere, Dome, Cylinder, Cone, Pyramid, Torus, Wall, Arch, Ellipsoid, Arc Wall, Spiral

**Inputs (vary by shape):**
- Radius (1-100)
- Height (1-256)
- Tube radius (torus)
- Thickness (sphere, dome, torus, arch, ellipsoid, arc wall)
- Angle span (arc wall)
- Step height (spiral staircase)
- Width (spiral, wall)
- Hollow, flipped toggles

**Outputs:**
- Layer-by-layer block map (view each Y level individually)
- Total block count
- Interactive layer slider

---

## Maze Maker
Procedurally generate mazes in three layouts.

**Maze types:**
- Rectangular (standard grid maze)
- Circular (ring-based maze)
- Multi-floor (stacked levels with staircases)

**Inputs:**
- Width/length cells (3-30)
- Rings (2-10, circular only)
- Floors (2-6, multi-floor only)
- Path width (1-3 blocks)
- Wall height (1-10 blocks)

**Outputs:**
- Layer-by-layer block map
- Total block count
- Dimensions in blocks and chunks
- Maze stats: dead ends, longest path

**Features:**
- Shuffle button to regenerate with a new seed
- Layer viewer with slider

---

## Storage
Calculate container requirements for item quantities, with optional block compression.

**Inputs:**
- Quantity (supports natural language: "14 stacks", "5k", "2 chests")
- Optional: compressible item (iron, gold, copper, diamond, lapis, redstone, emerald, etc.)

**Outputs:**
- Total item count
- Stacks, single chests, double chests, shulker boxes
- Block compression ratio (e.g., 9 iron ingots = 1 iron block)

---

## Smelting
Calculate fuel needed for smelting a given number of items.

**Input:** Quantity of items to smelt (supports "14 stacks", "5k", etc.)

**Output:** 11 fuel types ranked by efficiency:
- Units needed per fuel type
- Leftover items
- Efficiency rating (HIGH / MID / LOW) with color indicators

---

## Nether Tools
Three sub-tools in a tabbed interface:

### Coordinate Converter
- Input: X, Y, Z coordinates in either dimension
- Output: Converted coordinates (8:1 Overworld:Nether ratio, Y unchanged)
- Bonus: Parses F3 debug screen paste (coordinates + facing direction)

### Obsidian Calculator
- Input: Portal width and height
- Output: Obsidian needed (with or without corner blocks)

### Portal Tracker
- Save portal locations with custom names
- Persistent storage in local database
- Edit and delete portals

---

## Game Clock
Track the Minecraft day/night cycle.

**Sync methods:**
- F3 tick value (0-23999, most accurate)
- Day approximation (estimate current time)

**Features:**
- Current in-game time (HH:MM format)
- Day number with increment/decrement
- Upcoming events (sunrise, sunset, etc.) with countdown
- Configurable event tracking

---

## Light Spacing
Calculate optimal spacing for light sources to prevent mob spawning.

**Input:** Select from 21 light sources (beacon, torches, candles, sea pickles, lanterns, etc.)

**Output:**
- Light level emitted
- Maximum block spacing in a grid to maintain safe light levels
- Example grid visualization

---

## Banner Designer
Design custom banners with up to 6 pattern layers.

**Inputs:**
- Base color (16 dye colors)
- Pattern layers: type + color (up to 6 layers)

**Features:**
- Live banner preview (120x200 dp)
- Color swatch grid
- Layer reordering (up/down arrows)
- Pattern reference with 10 loom patterns

---

## Food Browser
Browse all food items with hunger and saturation stats.

**Filters:**
- Category: all, crop, meat, fish, crafted, other
- Sort: name, hunger, saturation, efficiency

**Display:**
- Food name and icon
- Hunger points restored
- Saturation value
- Special effects
- Efficiency rating (saturation/hunger ratio)

---

## Notes
Personal note storage with full-text search.

**Features:**
- Title, label, and multi-line content
- Label filtering
- Search across all notes
- Persistent database storage

---

## Waypoints
Save world locations organized by category.

**Properties per waypoint:**
- Name, X/Y/Z coordinates
- Category: base, farm, portal, spawner, village, monument, other
- Dimension: Overworld, Nether, End
- Color: gold, green, red, blue, purple

**Features:**
- Category and search filtering
- Color-coded markers
- Persistent database storage

---

## Redstone
Calculate comparator output signal strength from container fill levels.

**Inputs:**
- Container type (14 types: chest, furnace, hopper, brewing stand, etc.)
- Item count and stack size
- Or: target signal strength (reverse calculation)

**Outputs:**
- Signal strength (0-15)
- Items needed for target signal
- Formula: `floor(1 + fillFraction * 14)`

---

## Librarian Guide
*Experimental feature — enable in Settings > Show Experimental Features*

Reference for biome-locked librarian enchantments (Villager Trade Rebalance data pack).

**Input:** Select biome (Plains, Desert, Savanna, Snow, Taiga, Jungle, Swamp)

**Output:**
- Master-exclusive enchantment for that biome
- Common enchantment pool available

---

## Loot Tables
Browse structure loot tables to find where rare items spawn.

**Features:**
- 13+ structures with loot details
- Exclusive items (only found in this structure)
- Notable items with find chances
- Search across structure and item names
- Clickable structure names navigate to Browse detail pages

---

## Armor Trims
Reference for armor trim templates and materials.

### Templates (18)
- Name, source structure, find chance
- Duplication material
- Clickable links to structure details

### Materials (11)
- Crafting item and resulting color
