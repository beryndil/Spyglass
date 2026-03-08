# Browse

The Browse tab provides searchable, filterable access to 13 categories of Minecraft Java Edition game data. Every entry has a full detail page with cross-links to related items, mobs, biomes, and structures.

## Categories

### Blocks (1,167 entries)
Complete catalog of every Java Edition block.

| Property | Description |
|----------|-------------|
| Name & ID | Display name and `minecraft:` ID |
| Category | Decorative, redstone, building, natural, etc. |
| Hardness | Mining time factor |
| Blast Resistance | Explosion protection |
| Tool & Tier | Required tool type and minimum tier |
| Light Level | Light emitted (0-15) |
| Flammability | Whether it burns |
| Drops | What the block drops when broken |
| Description | Full description with cross-links |
| Obtainable | Whether it can be obtained in Survival |
| Y-Levels | Ore generation range (for ores) |

**Filters:** Category, tool type, obtainability, version availability

### Items (502 entries)
Every item in the game with crafting and usage info.

| Property | Description |
|----------|-------------|
| Name & ID | Display name and ID |
| Category | Tool, weapon, armor, food, material, etc. |
| Stack Size | Maximum stack (1, 16, or 64) |
| Durability | Uses before breaking (tools/armor) |
| Renewable | Whether it can be farmed infinitely (86 flagged non-renewable) |
| How to Obtain | Crafting, trading, mob drops, etc. |
| Recipes | Linked crafting recipes |

### Recipes (1,291 entries)
All crafting and processing recipes.

| Type | Count | Description |
|------|-------|-------------|
| Shaped Crafting | — | 3x3 grid with specific placement |
| Shapeless Crafting | — | Any arrangement in crafting grid |
| Smelting | — | Furnace recipes |
| Blasting | — | Blast furnace recipes |
| Smoking | — | Smoker recipes |
| Campfire | — | Campfire cooking |
| Stonecutting | — | Stonecutter variants |
| Loom | 51 | Banner pattern recipes |

**Features:** Visual crafting grids, input/output items, XP values for smelting recipes. Filter chips for each recipe type.

### Mobs (81 entries)
Every mob in the game with combat and spawning data.

| Property | Description |
|----------|-------------|
| Health | Hit points |
| Attack Damage | Melee/ranged damage |
| XP Drops | Experience on kill |
| Spawn Biomes | Where it naturally spawns |
| Spawn Conditions | Light level, block requirements |
| Drops | Structured loot tables with min/max/chance |
| Description | Behavior and cross-links |

### Trades (231 entries)
All villager and wandering trader trades, organized by profession.

| Property | Description |
|----------|-------------|
| Profession | 13 villager professions + wandering trader |
| Level | Novice through Master (1-5) |
| Input Items | Cost in emeralds and/or items |
| Output Item | What you receive |
| Max Uses | Trades before restock |

### Biomes (65 entries)
Every biome with climate data and building suggestions.

| Property | Description |
|----------|-------------|
| Temperature | Climate classification |
| Precipitation | Rain, snow, or none |
| Structures | Generated structures found here |
| Mobs | Naturally spawning mobs |
| Building Palette | Suggested blocks for building in this biome |
| Description | Full biome description |

### Structures (25 entries)
All generated structures across all three dimensions.

| Property | Description |
|----------|-------------|
| Dimension | Overworld, Nether, or End |
| How to Find | Location tips and methods |
| Loot Tables | Chest contents and rare items |
| Unique Blocks | Blocks only found here |
| Associated Mobs | Mobs that spawn in/around the structure |

### Enchantments (43 entries)
All enchantments including 1.21+ additions.

| Property | Description |
|----------|-------------|
| Max Level | I through V |
| Rarity | Common, uncommon, rare, very rare |
| Target Items | What can be enchanted |
| Incompatibilities | Conflicting enchantments |
| Treasure | Only from loot/trading (not enchanting table) |
| Curse | Negative enchantment |

### Potions (130 entries)
All potion variants with brewing paths.

| Variant | Count |
|---------|-------|
| Base Potions | 46 |
| Splash Potions | 42 |
| Lingering Potions | 42 |

Each potion shows: effects, duration, color, and step-by-step brewing path.

### Commands (85 entries)
Full Java Edition command reference.

| Property | Description |
|----------|-------------|
| Syntax | Command format with arguments |
| Usage | What the command does |
| Examples | Common usage patterns |

### Advancements (125 entries)
All advancements across 5 tabs.

| Tab | Description |
|-----|-------------|
| Story | Main progression |
| Nether | Nether achievements |
| End | End dimension |
| Adventure | Exploration and combat |
| Husbandry | Animals and farming |

Each advancement shows: requirements, rewards, and parent chain.

### Version Tags (1,279 entries)
Edition and version metadata for game content.

| Property | Description |
|----------|-------------|
| Added in Java | Java Edition version when added |
| Added in Bedrock | Bedrock Edition version when added |
| Java Only | Content exclusive to Java Edition (228 entries) |
| Mechanics Changed | Version-specific behavior differences |

## Cross-Tab Navigation

Descriptions throughout the app are linkified — tap any referenced item, mob, biome, or structure name to jump directly to its detail page, even across categories. An internal `EntityLinkIndex` routes taps to the correct browse tab.

## Filtering & Sorting

Each browse tab supports:
- **Search** — filter by name within the category
- **Category chips** — filter by sub-category
- **Version filtering** — show all, highlight unavailable, or hide unavailable content based on your selected Minecraft version
- **Favorites** — star any entry to pin it to the top

## Version & Edition Badges

Entries display edition badges showing:
- Java Edition version when the content was added
- Bedrock Edition availability
- "Java Only" flag for exclusive content
- Mechanics change warnings when behavior differs between versions
