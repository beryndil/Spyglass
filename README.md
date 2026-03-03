# Spyglass

**Your Minecraft companion for crafting, building, and exploring.**

Spyglass is a fully offline Android reference app for Minecraft Java Edition 1.21.4. Browse a complete database of blocks, items, recipes, mobs, biomes, enchantments, potions, structures, and villager trades — plus six specialized calculators for building and enchanting. Everything runs locally on your device with zero data collection.

## Disclaimer

**Spyglass is not affiliated with, endorsed by, or associated with Mojang Studios or Microsoft.** Minecraft is a trademark of Mojang Studios. All game data is used for informational purposes only.

---

## Download

**[Download the latest APK from Releases](https://github.com/Dev-VulX/Spyglass/releases/latest)**

1. Open the link on your Android device
2. Download `app-debug.apk`
3. Tap the file to install (you may need to allow "Install from unknown sources")

**Requirements:** Android 8.0+ (Oreo) | ~30 MB storage | No internet needed

---

## Features

### Browse

Search and filter across 9 categories with pixel-art textures and deep cross-linking — tap any item, mob, biome, or structure to jump to its detail page.

| Category | Count | Highlights |
|----------|-------|------------|
| Blocks | 1,167 | Hardness, required tool, drops, flammability |
| Items | 502 | Durability, how to obtain, crafting recipes, ingredient chain calculator |
| Recipes | 1,240 | Shaped, shapeless, smelting, smithing — with visual crafting grids |
| Mobs | 81 | Health, XP, drops, spawn biomes and structures |
| Trades | 231 | All 13 villager professions + wandering trader, levels 1-5 |
| Biomes | 65 | Temperature, precipitation, resources, structures, mobs |
| Structures | 25 | How to find, loot tables, unique blocks, mobs |
| Enchantments | 43 | Max level, rarity, incompatibilities, treasure/curse flags |
| Potions | 46 | Effects, duration, step-by-step brewing paths |

### Calculators

| Calculator | What it does |
|------------|-------------|
| **Block Fill** | Blocks needed for any rectangular volume, with stack/chest/shulker breakdown |
| **Smelting** | Fuel requirements with efficiency ratings for every fuel type |
| **Storage** | Convert quantities to stacks, chests, shulker boxes, and compressed blocks |
| **Nether Tools** | Coordinate converter (1:8 ratio), obsidian calculator, portal tracker |
| **Shapes** | Layer-by-layer visualization for spheres, cylinders, cones, and tori |
| **Enchanting** | Optimal anvil combining order to minimize XP cost |

### Global Search

Real-time search across all 9 categories simultaneously. Tap any result to jump directly to its detail page.

---

## Screenshots

*Coming soon.*

---

## Build from Source

```bash
git clone https://github.com/Dev-VulX/Spyglass.git
cd Spyglass
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

**Build requirements:** JDK 17, Android SDK (API 35)

### Run Tests

```bash
./gradlew testDebugUnitTest
```

45 JVM unit tests covering calculators (block fill, shapes), data sync manifest, and search tab mapping.

---

## Credits

**Spyglass** is created by **Beryndil**.

### Texture Packs

| Resource | Author | License |
|----------|--------|---------|
| [Pixel Perfection](https://www.curseforge.com/minecraft/texture-packs/pixel-perfection) | XSSheep | [CC-BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| [Entity-Icons](https://github.com/Simplexity-Development/Entity-Icons) | Simplexity Development | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) |

---

## Privacy

Spyglass does not collect, store, or transmit any personal data. No analytics, no tracking, no ads, no accounts, no internet required.

---

## Contributing

- [Report a Bug](https://github.com/Dev-VulX/Spyglass/issues)
- [Request a Feature](https://github.com/Dev-VulX/Spyglass/issues)

Pull requests welcome — fork, branch, and submit a PR.

---

*Built with Kotlin, Jetpack Compose, and a deep appreciation for Minecraft.*
