# Spyglass

**Your Minecraft companion for crafting, building, and exploring.**

Spyglass is a fully offline Android reference app for Minecraft Java Edition 1.21.4. It bundles a complete database of blocks, items, recipes, mobs, biomes, enchantments, potions, structures, and villager trades alongside six specialized calculators for building and enchanting. Everything runs locally on your device with zero data collection.

> Not affiliated with Mojang Studios or Microsoft.

---

## Table of Contents

- [Features at a Glance](#features-at-a-glance)
- [Screenshots](#screenshots)
- [Getting Started](#getting-started)
  - [Requirements](#requirements)
  - [Install from Source](#install-from-source)
- [App Manual](#app-manual)
  - [Home Screen](#home-screen)
  - [Browse](#browse)
    - [Blocks](#blocks)
    - [Items](#items)
    - [Recipes](#recipes)
    - [Mobs](#mobs)
    - [Trades](#trades)
    - [Biomes](#biomes)
    - [Structures](#structures)
    - [Enchantments](#enchantments)
    - [Potions](#potions)
  - [Calculators](#calculators)
    - [Block Fill Calculator](#block-fill-calculator)
    - [Smelting Calculator](#smelting-calculator)
    - [Storage Calculator](#storage-calculator)
    - [Nether Tools](#nether-tools)
    - [Shapes Calculator](#shapes-calculator)
    - [Enchanting Optimizer](#enchanting-optimizer)
  - [Global Search](#global-search)
  - [Settings](#settings)
  - [About](#about)
- [Cross-Tab Navigation](#cross-tab-navigation)
- [Data Reference](#data-reference)
  - [Database Contents](#database-contents)
  - [Recipe Types](#recipe-types)
  - [Data Sources](#data-sources)
- [Architecture](#architecture)
  - [Tech Stack](#tech-stack)
  - [Project Structure](#project-structure)
  - [Data Flow](#data-flow)
  - [Database Schema](#database-schema)
  - [UI Component Library](#ui-component-library)
  - [Texture System](#texture-system)
  - [Theme and Colors](#theme-and-colors)
- [Building and Development](#building-and-development)
  - [Build Variants](#build-variants)
  - [Adding New Data](#adding-new-data)
  - [Dependencies](#dependencies)
- [Credits and Licenses](#credits-and-licenses)
- [Privacy Policy](#privacy-policy)
- [Contributing](#contributing)

---

## Features at a Glance

| Feature | Description |
|---------|-------------|
| **Browse** | Search and filter 9 categories of Minecraft data with pixel-art textures |
| **Recipes** | Visual crafting grids, smelting recipes, and full ingredient chain calculator |
| **Cross-links** | Tap any item, mob, biome, or structure to jump directly to its detail page |
| **Block Fill** | Calculate blocks needed for any rectangular volume |
| **Smelting** | Compare fuel efficiency across all fuel types |
| **Storage** | Convert item counts to stacks, chests, shulker boxes, and compressed blocks |
| **Nether Tools** | Coordinate converter, obsidian calculator, and portal tracker |
| **Shapes** | Design spheres, cylinders, cones, and tori with per-layer block visualization |
| **Enchanting** | Optimize anvil combining order to minimize XP cost |
| **Global Search** | Real-time search across all 9 data categories simultaneously |
| **Offline** | All data bundled locally. No internet required, no data collected |

---

## Screenshots

*Coming soon.*

---

## Getting Started

### Requirements

- Android 8.0 (Oreo) or higher (API 26+)
- ~30 MB storage for app + data
- No internet connection required

### Install from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/user/spyglass-android.git
   cd spyglass-android
   ```

2. Open in Android Studio (Hedgehog or newer recommended).

3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```
   Or press **Run** in Android Studio with a connected device or emulator.

4. On first launch, Spyglass automatically seeds its local database from bundled JSON data files. This takes a few seconds and only happens once.

---

## App Manual

### Home Screen

The home screen is the first thing you see when opening Spyglass. It provides:

- **Welcome header** with app branding and Minecraft version info (Java 1.21.4)
- **Did You Know?** A rotating daily Minecraft tip (30 tips cycling by calendar day). Tips cover game mechanics, hidden features, building tricks, and combat strategies.
- **Quick Access - Browse** A 2-column grid of 9 quick links to all Browse categories (Blocks, Items, Recipes, Mobs, Trades, Biomes, Structures, Enchantments, Potions). Tap any link to jump directly to that tab.
- **Quick Access - Calculators** A 2-column grid of 6 quick links to all Calculator tools (Block Fill, Smelting, Storage, Nether Portal, Shapes, Enchanting).
- **What's New** Recent feature highlights and additions.
- **Search Everything** A prominent button at the bottom that opens the global search.

---

### Browse

The Browse section contains 9 tabbed categories accessible via a scrollable tab row. Each tab features:

- A **search bar** for filtering by name (debounced for responsiveness)
- **Category filter chips** for narrowing results
- **Expandable list items** that reveal detailed information when tapped
- **Cross-tab navigation** via clickable chips that jump to related items in other tabs

#### Blocks

Browse all Minecraft blocks with their mining properties.

**Filter categories:** All, Building, Natural, Decoration, Utility

**Each block entry shows:**
- Block name with pixel-art texture
- Category badge

**Expanded details include:**
- **Hardness** - Mining resistance value
- **Tool Required** - Which tool and minimum tier needed (e.g., "Pickaxe (Iron)")
- **Stack Size** - Shown only if different from 64
- **Flammable / Transparent** flags
- **Drops** - What items the block yields when mined (clickable)
- **Job Block** - If this block is a villager workstation, shows the linked profession
- **Found in Structures** - Structures where this block naturally generates (clickable)
- **Recipe / Uses** - Crafting recipe pager showing how to make this block and what it's used in

#### Items

Browse all Minecraft items with detailed sourcing information.

**Filter categories:** All, Tools, Weapons, Armor, Food, Materials, Mob Drops, Brewing, Misc

**Each item entry shows:**
- Item name with texture icon
- Category badge (color-coded)
- Durability value (for tools, weapons, armor)

**Expanded details include:**
- **Description** - What the item does and how it's used
- **Stack Size** and **Durability**
- **Category** classification
- **How to Obtain** - Color-coded badges showing all acquisition methods:
  - Crafting, Mob Drop, Mining, Trading, Fishing, Structure Loot, Farming, Smelting, Bartering, Found
- **Crafting Recipe** - Visual grid with ingredients (if craftable)
- **Dropped By** - Mobs that drop this item (clickable)
- **Mined From** - Blocks that yield this item (clickable)
- **Found in Biomes** - Biomes where this item can be sourced (clickable)
- **Found in Structures** - Structures with this item in loot tables (clickable)
- **Used In** - Up to 10 recipes that use this item as an ingredient (clickable)

**Chain Calculator:** When viewing a crafted item's recipe, the chain calculator automatically traces the full ingredient tree. It calculates total raw materials needed across all crafting steps and shows which biomes to find uncraftable ingredients in.

#### Recipes

Browse all crafting, smelting, and smithing recipes with visual grid previews.

**Filter types:** All, Shaped, Shapeless, Smelting, Smithing

**Each recipe shows:**
- Output item name and count (e.g., "4x Stone Bricks")
- Recipe type badge
- Compact ingredient preview (grid icons for shaped, ingredient list for shapeless, arrow for smelting)

**Expanded details include:**
- **Full 3x3 crafting grid** with item textures in each slot
- **Smelting recipes** shown as input arrow output
- **Shapeless recipes** shown as ingredient icon row
- **Recipes For** - Other recipes that produce this item
- **Recipes Using** - Recipes that use this item as an ingredient

The recipe database contains **487 recipes** including all wall, stair, slab, building block, smelting, and smithing variants.

#### Mobs

Browse all Minecraft mobs with combat stats and drop information.

**Filter categories:** All, Hostile, Neutral, Passive, Boss

**Each mob shows:**
- Mob name with entity icon
- Category badge (Red = Hostile, Gold = Neutral, Green = Passive, Purple = Boss)
- Health (HP) value

**Expanded details include:**
- **Description** - Behavior patterns and combat tips
- **Health** - Hit points (can be a range for variable-health mobs like Slimes)
- **XP Drop** - Experience orbs dropped on kill
- **Fire Immune** flag (for Nether mobs, etc.)
- **Drops** - Items dropped on death with clickable links
- **Found In** - Spawn biomes and structures (clickable chips)
  - Biome chips navigate to the Biomes tab
  - Structure chips navigate to the Structures tab
  - Special locations (e.g., "slime_chunks", "bred", "summoned") shown as info badges

#### Trades

Browse all villager trades organized by profession and level.

**Filter professions:** All, Armorer, Butcher, Cartographer, Cleric, Farmer, Fisherman, Fletcher, Leatherworker, Librarian, Mason, Shepherd, Toolsmith, Weaponsmith

**When a profession is selected**, a **Job Block Card** appears at the top showing:
- The profession's workstation block (e.g., Blast Furnace for Armorer)
- The visual crafting recipe for that job block
- Ingredient list

**Each trade shows:**
- Buy items (with quantities) -> Sell item (with quantity)
- Profession and level name (Novice through Master)
- Level badge (1-5)
- Item textures and clickable item names

**Trade levels:**
| Level | Name | Unlock |
|-------|------|--------|
| 1 | Novice | Default |
| 2 | Apprentice | After trading |
| 3 | Journeyman | After more trading |
| 4 | Expert | After more trading |
| 5 | Master | Final tier |

#### Biomes

Browse all Minecraft biomes with climate data and content listings.

**Filter categories:** All, Forest, Ocean, Desert, Mountain, Cave, Nether, End

**Each biome shows:**
- Biome name with representative texture
- Category badge
- Temperature and precipitation type

**Expanded details (with color-tinted background matching the biome):**
- **Temperature** in degrees
- **Precipitation** - None, Rain, or Snow
- **Category**
- **Features** - Terrain characteristics and vegetation
- **Resources** - Items that can be gathered here (clickable)
- **Structures** - Generated structures found in this biome (clickable)
- **Mobs** - Creatures that spawn here (clickable)

The background color of each expanded biome card is derived from the biome's hex color data, with text color automatically adjusting for readability.

#### Structures

Browse all generated structures across all three dimensions.

**Filter dimensions:** All, Overworld, Nether, End

**Each structure shows:**
- Structure name with representative texture
- Dimension badge
- Difficulty badge (Easy = Green, Medium = Gold, Hard = Red)

**Expanded details include:**
- **Description** - What the structure looks like and contains
- **How to Find** - Step-by-step instructions for locating it
- **Biomes** - Where it generates (clickable)
- **Mobs** - Hostile and passive mobs found inside (clickable)
- **Loot** - Valuable items found in chests (clickable)
- **Unique Blocks** - Blocks exclusive to or characteristic of this structure (clickable)

#### Enchantments

Browse all enchantments with compatibility and conflict information.

**Filter targets:** All, Armor, Sword, Bow, Crossbow, Trident, Mace, Fishing Rod (each with a diamond gear icon)

**Each enchantment shows:**
- Enchantment name
- Max level (Roman numerals)
- Rarity badge (Green = Common, Blue = Uncommon, Purple = Rare, Gold = Very Rare)
- Treasure and Curse flags

**Expanded details include:**
- **Description** - What the enchantment does mechanically
- **Max Level**
- **Applies To** - Item types this enchantment works on
- **Rarity**
- **Treasure Only** - Whether it can only be found in loot or via anvil (not enchanting table)
- **Curse** flag
- **Incompatible With** - Enchantments that conflict (clickable, with auto-scroll)

**Incompatibility detection:** If you expand an enchantment that conflicts with one already expanded, a warning snackbar appears and the expansion is prevented. This mirrors the in-game behavior where mutually exclusive enchantments cannot coexist.

#### Potions

Browse all potions with brewing paths and effect information.

**Filter categories:** All, Positive, Negative, Special

**Each potion shows:**
- Potion name with ingredient icon
- Duration (MM:SS format)
- Category badge

**Expanded details include:**
- **Effect** - What the potion does
- **Duration** - How long it lasts
- **Level** - Potion amplifier
- **Category**
- **Brewing Path** - Step-by-step ingredient chain shown as clickable chips
  - Example: Nether Wart -> Sugar -> Redstone
  - Clickable ingredients link to their item detail page
  - Intermediate potions (e.g., "Awkward Potion") shown as info badges

---

### Calculators

Six specialized calculator tools are accessible via the Calculators tab, each with its own sub-tab.

#### Block Fill Calculator

Calculate the total number of blocks needed to fill a rectangular volume.

**Inputs:**
- **Width** - Number field with Blocks/Chunks toggle (1 chunk = 16 blocks)
- **Length** - Number field with Blocks/Chunks toggle
- **Height** - Number field (blocks only)

**Results:**
| Output | Description |
|--------|-------------|
| Total blocks | Width x Length x Height |
| Stacks | Total / 64 + remainder |
| Single chests | Total / (27 x 64) + remainder |
| Double chests | Total / (54 x 64) + remainder |
| Shulker boxes | Total / (27 x 64) + remainder |

Each result shows the count plus any leftover (e.g., "42 + 53 left").

#### Smelting Calculator

Calculate fuel requirements for any smelting job.

**Input:**
- **Items to smelt** - Supports natural language quantities:
  - `64` or `64 items` - Exact count
  - `14 stacks` - 14 x 64 = 896
  - `5k` - 5,000
  - `2 chests` - 2 x 27 x 64 = 3,456
  - `1 double chest` - 54 x 64 = 3,456
  - `3 shulker boxes` - 3 x 27 x 64 = 5,184

**Results (for each fuel type):**
- Fuel name (Coal, Charcoal, Wood Planks, Sticks, Bamboo, etc.)
- Units of fuel needed
- Stack breakdown
- Efficiency indicator:
  - Green dot = 100% efficient (no wasted burns)
  - Gold dot = 75-99% efficient
  - Gray dot = Below 75% efficient
- Unused burn percentage

#### Storage Calculator

Calculate container requirements and block compression.

**Inputs:**
- **Quantity** - Same natural language parser as Smelting calculator
- **Item type** - Dropdown to select an item for block compression (e.g., Iron Ingots, Diamonds, Gold Ingots)

**Results:**
| Output | Description |
|--------|-------------|
| Total items | Parsed quantity |
| Stacks of 64 | Items / 64 |
| Single chests | Items / (27 x 64) |
| Double chests | Items / (54 x 64) |
| Shulker boxes | Items / (27 x 64) |
| Compressed blocks | Items / compression ratio (e.g., 9 ingots per block) |

The compression feature calculates how many storage blocks (Iron Blocks, Diamond Blocks, etc.) can be formed from the input quantity.

#### Nether Tools

Three sub-tools for Nether gameplay.

**Convert Tab:**
- Toggle between Overworld -> Nether and Nether -> Overworld
- Enter X, Y, Z coordinates
- Get converted coordinates with the 1:8 ratio (X and Z divided/multiplied by 8, Y unchanged)
- Includes facing direction hint

**Obsidian Tab:**
- Enter portal width (minimum 2) and height (minimum 3)
- Calculates obsidian needed:
  - **Without corners** - Standard frame
  - **With corners** - Filled corner frame

**Portals Tab:**
- Save portal locations by name with coordinates
- View saved portals showing both Overworld and Nether coordinates
- Delete portals individually
- Persistent storage across app sessions

#### Shapes Calculator

Design pixel-perfect 3D structures with layer-by-layer visualization.

**Inputs:**
- **Shape type** - Sphere, Cylinder, Cone, or Torus
- **Radius** - 1 to 100 blocks
- **Tube radius** - (Torus only) Inner tube radius

**Output:**
- **Total block count** for the entire shape
- **Layer slider** - Scrub through each Y layer
- **2D visualization** - Canvas grid showing the current layer from above
  - Gold squares = blocks to place
  - Dark squares = empty space
  - Coordinate labels for X and Z ranges
- **Layer info** - Block count for current layer, X/Z min-max ranges

This tool is essential for building organic shapes in Minecraft. Design your sphere or dome here, then follow it layer-by-layer in the game.

#### Enchanting Optimizer

Calculate the cheapest possible XP cost for combining multiple enchantments using anvils.

**How Minecraft Anvil Costs Work:**
Every time an item or book is processed through an anvil, it gains a "prior work penalty." This penalty doubles each time: 0, 1, 3, 7, 15, 31 levels. If any single anvil operation costs 40+ levels, it fails as "Too Expensive." The order you combine enchantments matters enormously.

**Step 1 - Select your item:**
- **Weapons:** Sword, Bow, Crossbow, Trident, Mace
- **Tools:** Pickaxe, Axe, Shovel, Hoe, Fishing Rod
- **Armor:** Helmet, Chestplate, Leggings, Boots

Each item type shows a diamond gear texture icon for visual identification.

**Step 2 - Pick enchantments:**
Only enchantments valid for the selected item are shown. For each enchantment:
- Tap to toggle on/off
- Select level (I through V) using Roman numeral buttons
- **Incompatible enchantments** are grayed out and cannot be selected
- If you try to select an incompatible enchant, a warning snackbar appears (e.g., "Mending is incompatible with Infinity")

**Step 3 - View optimal order:**
The optimizer uses a balanced binary tree algorithm:
1. Sort enchanted books by cost (cheapest first)
2. Combine adjacent pairs each round (cheapest books accumulate more penalty since their base cost is small)
3. Finally apply the merged book to the item

**Results show:**
- Step-by-step instructions (e.g., "Step 1: Combine Unbreaking + Mending → 4 lvl")
- Cost per step in levels
- Red "Too Expensive!" warning if any step exceeds 39 levels
- **Total XP cost** across all operations

---

### Global Search

The Search tab provides real-time full-text search across all 9 data categories.

- **Minimum query:** 2 characters
- **Debounce:** 300ms (typing pauses before searching)
- **Results:** Up to 5 per category (45 max total)
- **Categories searched:** Blocks, Items, Recipes, Mobs, Biomes, Enchantments, Potions, Trades, Structures

Each result shows:
- Category badge (color-coded by type)
- Item name with actual pixel-art texture
- Detail line (category, recipe type, profession, etc.)

Tap any result to navigate directly to that item in the appropriate Browse tab.

---

### Settings

*Settings page coming soon.* Currently shows a placeholder.

---

### About

The About screen (accessible from the top-right menu) contains:

- **App version** (CalVer format: YYYY.MMDD)
- **Author:** Beryndil
- **Support:** "Buy Me a Cup of Coffee" link
- **Game data version:** Minecraft Java 1.21.4
- **Credits and licenses** for texture packs
- **Privacy statement**
- **Bug report and feature request** links

---

## Cross-Tab Navigation

One of Spyglass's most powerful features is deep cross-tab linking. Almost every piece of data is interconnected:

| From | Tapping... | Goes To |
|------|------------|---------|
| Any tab | An item name | Items or Blocks tab (auto-detected) |
| Items | "Dropped By" mob chip | Mobs tab, scrolled to that mob |
| Items | "Mined From" block chip | Blocks tab, scrolled to that block |
| Items | "Found in Biome" chip | Biomes tab, scrolled to that biome |
| Items | "Found in Structure" chip | Structures tab, scrolled to that structure |
| Mobs | Spawn biome chip | Biomes tab |
| Mobs | Spawn structure chip | Structures tab |
| Mobs | Drop item chip | Items tab |
| Biomes | Structure chip | Structures tab |
| Biomes | Resource chip | Items tab |
| Biomes | Mob chip | Mobs tab |
| Structures | Loot item chip | Items tab |
| Structures | Biome chip | Biomes tab |
| Structures | Mob chip | Mobs tab |
| Enchantments | Incompatible enchant chip | Same tab, auto-scrolls |
| Potions | Brewing ingredient chip | Items tab |
| Trades | Buy/Sell item name | Items tab |
| Recipes | Ingredient in grid | Items tab |
| Search | Any result | Appropriate Browse tab |
| Home | Quick link | Browse or Calculators tab |

---

## Data Reference

### Database Contents

| Category | Count | Key Fields |
|----------|-------|------------|
| Blocks | 100+ | Hardness, tool, drops, category, stack size |
| Items | 260+ | Durability, category, obtain method, sources |
| Recipes | 487 | Shaped/shapeless/smelting/smithing grids |
| Mobs | 74 | Health, XP, drops, spawn biomes, hostility |
| Biomes | 60 | Temperature, precipitation, structures, mobs |
| Enchantments | 43 | Max level, targets, incompatibilities, rarity |
| Potions | 47 | Effect, duration, amplifier, brewing path |
| Structures | 25 | Dimension, difficulty, loot, biomes, mobs |
| Trades | 40+ | Profession, level, buy/sell items and quantities |

### Recipe Types

| Type | Description | Example |
|------|-------------|---------|
| `crafting_shaped` | Grid-based with specific positions | Iron Pickaxe (3x3 grid) |
| `crafting_shapeless` | Any arrangement of ingredients | Book (3 paper + 1 leather) |
| `smelting` | Furnace/blast furnace/smoker | Raw Iron -> Iron Ingot |
| `smithing` | Smithing table upgrade | Diamond Sword + Netherite -> Netherite Sword |
| `found` | No recipe; found in world | Saddle, Elytra, Trident |

### Data Sources

All game data targets **Minecraft Java Edition 1.21.4** and is stored as JSON files bundled in the app assets:

| File | Description |
|------|-------------|
| `blocks.json` | Block properties, mining requirements, drops |
| `items.json` | Item stats, descriptions, sourcing information |
| `recipes.json` | All crafting/smelting/smithing recipes with grids |
| `mobs.json` | Mob health, drops, spawn locations, descriptions |
| `biomes.json` | Climate data, structures, mobs, features |
| `enchants.json` | Enchant levels, targets, incompatibilities |
| `potions.json` | Effect data, brewing ingredient paths |
| `structures.json` | Location info, loot, unique blocks, mobs |
| `trades.json` | Villager profession trades by level |

---

## Architecture

### Tech Stack

| Technology | Purpose |
|------------|---------|
| **Kotlin** 2.0 | Language |
| **Jetpack Compose** (BOM 2024.12) | UI framework |
| **Material3** | Design system |
| **Room** 2.6 | Local SQLite database |
| **Navigation Compose** 2.8 | Screen routing |
| **Kotlin Coroutines** 1.9 | Async operations |
| **Kotlin Serialization** 1.7 | JSON parsing |
| **DataStore Preferences** 1.1 | Key-value persistence |
| **Lifecycle ViewModel** 2.8 | UI state management |
| **KSP** 2.0 | Annotation processing for Room |

**Build:** Gradle 8.9, AGP 8.7, JDK 17, Target SDK 35 (Android 15), Min SDK 26 (Android 8.0)

**Versioning:** CalVer (Calendar Versioning) - `versionCode = YYYYMMDD`, `versionName = "YYYY.MMDD"`

### Project Structure

```
app/src/main/java/dev/spyglass/android/
├── MainActivity.kt                 # Entry point, seeds database, applies theme
├── home/
│   └── HomeScreen.kt              # Landing page with tips, quick links, what's new
├── navigation/
│   └── AppNavGraph.kt             # All routes, top bar, bottom nav, cross-tab linking
├── browse/
│   ├── BrowseScreen.kt            # 9-tab container with cross-tab detection
│   ├── blocks/BlocksScreen.kt     # Block browser
│   ├── items/ItemsScreen.kt       # Item browser
│   ├── crafting/CraftingScreen.kt # Recipe browser
│   ├── mobs/MobsScreen.kt        # Mob browser
│   ├── trades/TradesScreen.kt     # Trade browser
│   ├── biomes/BiomesScreen.kt     # Biome browser
│   ├── structures/StructuresScreen.kt  # Structure browser
│   ├── enchants/EnchantsScreen.kt # Enchantment browser
│   ├── potions/PotionsScreen.kt   # Potion browser
│   └── search/SearchScreen.kt     # Global search
├── calculators/
│   ├── CalculatorsScreen.kt       # 6-tab calculator container
│   ├── blockfill/                 # Block Fill calculator
│   ├── smelting/                  # Smelting fuel calculator
│   ├── storage/                   # Storage space calculator
│   ├── nether/                    # Nether tools (converter, obsidian, portals)
│   ├── shapes/                    # Shape designer with visualization
│   └── anvil/                     # Enchanting XP optimizer
├── core/
│   ├── ui/
│   │   ├── SpyglassTheme.kt      # Colors, typography, Material3 theme
│   │   ├── SpyglassIcon.kt       # Vector/Drawable icon abstraction
│   │   ├── PixelIcons.kt         # Pre-defined pixel art icon set
│   │   ├── CommonComponents.kt   # Reusable composables (cards, headers, etc.)
│   │   ├── ItemDetailPager.kt    # Recipe viewer + chain calculator
│   │   ├── BlockTextures.kt      # 272 block texture lookups
│   │   ├── ItemTextures.kt       # 300+ item texture lookups
│   │   ├── MobTextures.kt        # 84 mob entity icon lookups
│   │   ├── BiomeTextures.kt      # Biome → block texture mappings
│   │   ├── EnchantTextures.kt    # Enchant → item icon mappings
│   │   ├── PotionTextures.kt     # Potion → ingredient icon mappings
│   │   └── StructureTextures.kt  # Structure → block texture mappings
│   └── parser/
│       └── ItemQuantityParser.kt  # Natural language quantity parsing
├── data/
│   ├── db/
│   │   ├── SpyglassDatabase.kt   # Room database (v9, 9 entities)
│   │   ├── daos/Daos.kt          # All DAO interfaces
│   │   └── entities/Entities.kt  # All entity data classes
│   ├── repository/
│   │   └── GameDataRepository.kt  # Single data access layer
│   ├── seed/
│   │   └── DataSeeder.kt         # First-launch JSON → Room seeder
│   ├── BiomeResourceMap.kt       # 67 item-to-biome mappings
│   └── ItemTags.kt               # Item group tags (#planks, #logs, etc.)
├── settings/
│   └── SettingsScreen.kt         # Settings (placeholder)
└── about/
    └── AboutScreen.kt            # App info, credits, privacy, feedback
```

### Data Flow

```
[First Launch]
    assets/minecraft/*.json
         │
         ▼
    DataSeeder (Kotlin Serialization)
         │
         ▼
    Room Database (SQLite)
         │
         ▼
    GameDataRepository (Singleton)
         │  Returns Flow<List<Entity>>
         ▼
    Screen ViewModels (StateFlow)
         │
         ▼
    Compose UI (Reactive)
```

- All 9 JSON files are loaded into Room on first install
- Subsequent launches skip seeding entirely
- All queries return Kotlin `Flow` for reactive UI updates
- ViewModels expose state via `StateFlow` for Compose consumption
- Cross-entity references use string IDs (no foreign keys in Room)

### Database Schema

**9 entities, 9 DAOs, 1 repository.**

| Entity | Table | Primary Key | Key Relationships |
|--------|-------|-------------|-------------------|
| `BlockEntity` | blocks | `id` | drops → items, category filter |
| `ItemEntity` | items | `id` | droppedBy → mobs, minedFrom → blocks |
| `RecipeEntity` | recipes | `id` | outputItem → items, ingredients → items |
| `MobEntity` | mobs | `id` | spawnBiomes → biomes, drops → items |
| `BiomeEntity` | biomes | `id` | structures → structures, mobs → mobs |
| `EnchantEntity` | enchants | `id` | incompatible → enchants, target filter |
| `PotionEntity` | potions | `id` | ingredientPath → items |
| `TradeEntity` | trades | `rowId` (auto) | buyItem/sellItem → items |
| `StructureEntity` | structures | `id` | biomes → biomes, loot → items, mobs → mobs |

### UI Component Library

Reusable composables in `CommonComponents.kt`:

| Component | Purpose |
|-----------|---------|
| `SectionHeader` | Gold uppercase title with optional icon and divider |
| `StatRow` | Label-value pair row |
| `SpyglassTextField` | Styled number/text input with gold focus border |
| `ResultCard` | Dark card container for displaying results |
| `InputCard` | Slightly lighter card for calculator inputs |
| `TogglePill` | Two-option segmented toggle (e.g., Blocks/Chunks) |
| `SpyglassDivider` | Thin 0.5dp horizontal divider |
| `BrowseListItem` | List row with icon, headline, and supporting text |
| `EmptyState` | Centered empty-state placeholder with icon and message |
| `TabIntroHeader` | Centered icon + title + description for tab pages |
| `CategoryBadge` | Small colored pill for category/type labels |
| `TextureCraftingGrid` | Visual 3x3 crafting grid with item textures |
| `SpyglassTabRow` | Scrollable tab row with icon + text |

### Texture System

Spyglass maps Minecraft IDs to bundled PNG textures via lookup objects:

| Lookup Object | Count | Source |
|---------------|-------|--------|
| `BlockTextures` | 272 blocks | Pixel Perfection (CC-BY-SA 4.0) |
| `ItemTextures` | 300+ items | Pixel Perfection + delegates to BlockTextures |
| `MobTextures` | 84 entities | Entity-Icons (CC0 1.0) |
| `BiomeTextures` | ~50 biomes | Maps to representative block textures |
| `EnchantTextures` | ~40 enchants | Maps to associated item textures |
| `PotionTextures` | ~47 potions | Maps to brewing ingredient textures |
| `StructureTextures` | 25 structures | Maps to characteristic block textures |

`ItemTextures.get(id)` is the primary lookup: it checks `BlockTextures` first, then falls back to the item-specific map.

### Theme and Colors

Dark Minecraft-inspired palette:

| Token | Hex | Usage |
|-------|-----|-------|
| `Gold` | `#C9A84C` | Primary accent, selections, interactive elements |
| `GoldDim` | `#9E7A2A` | Muted gold, containers |
| `Background` | `#0E0C0A` | Main dark background |
| `SurfaceDark` | `#1C1A17` | Card backgrounds |
| `SurfaceCard` | `#211F1B` | Input card backgrounds |
| `SurfaceMid` | `#2A2720` | Variant surfaces |
| `Stone700` | `#3A3730` | Borders, dividers |
| `Stone500` | `#6B6860` | Secondary text, disabled states |
| `Stone300` | `#9E9B94` | Tertiary text |
| `Stone100` | `#D4D1CA` | Primary text |
| `NetherRed` | `#D32F2F` | Hostile mobs, Nether content |
| `EnderPurple` | `#AB47BC` | End content, boss mobs |
| `PotionBlue` | `#42A5F5` | Potion effects |
| `Emerald` | `#66BB6A` | Positive states, passive mobs, found items |
| `Red400` | `#EF5350` | Errors, "Too Expensive" warnings |

---

## Building and Development

### Build Variants

| Variant | Minification | Description |
|---------|-------------|-------------|
| `debug` | Off | Development build with debug info |
| `release` | On (R8/ProGuard) | Production build, minified and obfuscated |

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

### Adding New Data

To add new items, recipes, or other game data:

1. Edit the appropriate JSON file in `app/src/main/assets/minecraft/`
2. Follow the existing format (see any entry as a template)
3. If adding a new item with a texture, place the PNG in `app/src/main/res/drawable-hdpi/` with naming convention `block_*.png` or `item_*.png`
4. Register the texture in the appropriate lookup file (`BlockTextures.kt`, `ItemTextures.kt`, etc.)
5. Increment the database version in `SpyglassDatabase.kt` if the schema changes (this triggers a database rebuild on next launch)

**Recipe format example:**
```json
{
  "id": "diamond_pickaxe",
  "outputItem": "diamond_pickaxe",
  "outputCount": 1,
  "type": "crafting_shaped",
  "ingredientsJson": "[[\"diamond\",\"diamond\",\"diamond\"],[\"\",\"stick\",\"\"],[\"\",\"stick\",\"\"]]"
}
```

### Dependencies

All dependencies are managed via Gradle version catalog (`gradle/libs.versions.toml`):

| Library | Version |
|---------|---------|
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Room | 2.6.1 |
| Navigation Compose | 2.8.5 |
| Lifecycle | 2.8.7 |
| DataStore | 1.1.1 |
| Kotlinx Serialization | 1.7.3 |
| Coroutines | 1.9.0 |
| KSP | 2.0.21-1.0.28 |
| AGP | 8.7.0 |
| Gradle | 8.9 |

---

## Credits and Licenses

**Spyglass** is created by **Beryndil**.

### Texture Packs

| Resource | Author | License | Link |
|----------|--------|---------|------|
| Pixel Perfection | XSSheep | [CC-BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | [CurseForge](https://www.curseforge.com/minecraft/texture-packs/pixel-perfection) |
| Entity-Icons | Simplexity Development | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | [GitHub](https://github.com/Simplexity-Development/Entity-Icons) |

### Game Data

All game data references **Minecraft Java Edition 1.21.4** by Mojang Studios / Microsoft. Spyglass is an independent fan project and is not affiliated with, endorsed by, or connected to Mojang Studios or Microsoft.

### App License

Copyright 2026 Beryndil. All Rights Reserved except where noted above.

---

## Privacy Policy

**Spyglass does not collect, store, or transmit any personal data.**

- All game data is stored locally on your device in a SQLite database
- No analytics, tracking, or telemetry
- No internet connection required or used
- No accounts or sign-in
- No ads

---

## Contributing

Found a bug or have a feature request?

- [Report a Bug](https://github.com/user/spyglass-android/issues)
- [Request a Feature](https://github.com/user/spyglass-android/issues)

To contribute code:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Make your changes
4. Test with `./gradlew assembleDebug`
5. Submit a pull request

---

*Built with Kotlin, Jetpack Compose, and a deep appreciation for Minecraft.*
