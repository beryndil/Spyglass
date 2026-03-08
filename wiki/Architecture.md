# Architecture

Technical overview of the Spyglass Android app for developers and contributors.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0.21 |
| UI Framework | Jetpack Compose (Material 3) |
| Database | Room 2.6.1 |
| Preferences | DataStore |
| Networking | OkHttp 4.12.0 |
| Serialization | kotlinx.serialization 1.7.3 |
| Background Work | WorkManager 2.10.0 |
| Navigation | Navigation Compose 2.8.5 |
| Build System | Gradle 8.13.2 with KSP |

## SDK Targets

| Setting | Value |
|---------|-------|
| Compile SDK | 35 (Android 15) |
| Min SDK | 26 (Android 8.0 Oreo) |
| Target SDK | 35 (Android 15) |
| Java | 17 |

## Module System

The app uses a module architecture where features register themselves as independent modules:

| Module | Package | Responsibility |
|--------|---------|---------------|
| **CoreModule** | `core/module/` | Appearance, data sync, privacy, about settings |
| **BrowseModule** | `core/module/` | Game version, browse settings, favorites |
| **ToolsModule** | `core/module/` | Calculator settings, game clock |
| **ConnectModule** | `core/module/` | Desktop companion connection |

Modules register via `ModuleRegistry` in `SpyglassApp.onCreate()`. Each module provides:
- `settingsSections()` — Settings UI sections with sort weight
- Feature toggles (can be enabled/disabled)

Settings are rendered by `ShellSettingsScreen`, which collects sections from all enabled modules: `ModuleRegistry.enabledModules(context).flatMap { it.settingsSections() }.sortedBy { it.weight }`.

## Package Structure

```
dev.spyglass.android/
├── about/                  About screen
├── browse/                 13 browse categories + detail screens
│   ├── blocks/            Block detail + list
│   ├── items/             Item detail + list
│   ├── recipes/           Recipe detail + list
│   ├── mobs/              Mob detail + list
│   ├── trades/            Trade detail + list
│   ├── biomes/            Biome detail + list
│   ├── structures/        Structure detail + list
│   ├── enchants/          Enchantment detail + list
│   ├── potions/           Potion detail + list
│   ├── advancements/      Advancement detail + list
│   ├── commands/          Command detail + list
│   ├── versions/          Version history display
│   └── search/            Global search
├── calculators/            19 calculator tools
│   ├── todo/              Task manager
│   ├── shopping/          Shopping lists + crafting plans
│   ├── anvil/             Enchant optimizer
│   ├── blockfill/         Block fill calculator
│   ├── shapes/            3D shape designer
│   ├── maze/              Maze generator
│   ├── storage/           Storage calculator
│   ├── smelting/          Fuel calculator
│   ├── nether/            Coordinate converter + portals
│   ├── clock/             Game clock
│   ├── light/             Light spacing
│   ├── banners/           Banner designer
│   ├── food/              Food browser
│   ├── notes/             Note storage
│   ├── waypoints/         Location bookmarks
│   ├── redstone/          Comparator calculator
│   ├── librarian/         Librarian guide
│   ├── loot/              Loot table reference
│   └── trims/             Armor trim reference
├── changelog/              Version history screen
├── connect/                Desktop companion
│   ├── client/            WebSocket client, encryption
│   ├── character/         Character viewer
│   ├── inventory/         Inventory + ender chest
│   ├── chestfinder/       Chest finder
│   ├── map/               Overhead map
│   ├── statistics/        Player statistics
│   └── advancements/      Advancements roadmap
├── core/
│   ├── module/            Module system (Core, Browse, Tools, Connect)
│   ├── ui/                Theme, textures, ads, consent, haptics
│   └── net/               Network utilities
├── data/
│   ├── db/                Room database + entities (12 tables, 18 indexes)
│   ├── repository/        GameDataRepository (data access layer)
│   ├── sync/              Data sync (manifest, CDN client, worker)
│   └── seed/              Initial data seeding from bundled JSON
├── disclaimer/             Legal disclaimer
├── feedback/               Feedback screen
├── home/                   Home screen + tips
├── license/                License screen
├── navigation/             NavGraph + route definitions
├── settings/               Settings screen + ViewModel + preferences
├── MainActivity.kt         Entry point, biometric lock, consent
└── SpyglassApp.kt          Application init, module registration
```

## Database

Room database with 12 entity tables and 18 performance indexes.

| Entity | Table | Indexes |
|--------|-------|---------|
| Block | blocks | name, category |
| Item | items | name, category |
| Recipe | recipes | outputItem, type |
| Mob | mobs | name, category |
| Trade | trades | profession |
| Biome | biomes | name |
| Structure | structures | name |
| Enchantment | enchants | name |
| Potion | potions | name |
| Advancement | advancements | name, category |
| Command | commands | name, category |
| VersionTag | version_tags | entityType |

Additional tables for user data: todos, shopping lists, notes, waypoints, portals.

**Current DB version:** 28 (migration 27→28 added indexes)

## Data Flow

```
Bundled JSON (assets/) → DataSeeder → Room DB → Repository → ViewModel → Compose UI
                              ↑
CDN/GitHub → DataSyncManager → Downloaded JSON → DataSeeder.reseedTable()
```

## Networking

| Client | Purpose | Base URL |
|--------|---------|----------|
| GitHubDataClient | Data sync | `data.hardknocks.university` (CDN) + GitHub fallback |
| SpyglassClient | Desktop WebSocket | `ws://{lan-ip}:29170/ws` |
| Update checker | App version check | `api.github.com/repos/beryndil/Spyglass/releases/latest` |

## Key Dependencies

### Core
- `androidx.compose.bom:2024.12.01` — Compose UI framework
- `androidx.room:2.6.1` — Local database
- `com.squareup.okhttp3:4.12.0` — HTTP and WebSocket client
- `kotlinx-serialization-json:1.7.3` — JSON parsing
- `kotlinx-coroutines-android:1.9.0` — Async operations

### Spyglass Connect
- `zxing-android-embedded:4.3.0` — QR code scanning
- `androidx.camera:1.4.1` — Camera for QR scanner
- `androidx.biometric:1.2.0-alpha05` — App lock

### Monetization
- `play-services-ads:23.6.0` — AdMob banner ads
- Mediation: Meta, Unity, AppLovin, IronSource

### Analytics (opt-in)
- `firebase-bom:33.7.0` — Crashlytics, Analytics, Performance

### Debug
- `timber:5.0.1` — Structured logging
- `leakcanary:2.14` — Memory leak detection (debug only)

## Testing

45 JVM unit tests across 4 test suites:

| Suite | Tests | Focus |
|-------|-------|-------|
| BlockFillViewModelTest | 10 | Block fill calculations |
| DataManifestTest | 18 | Manifest parsing, zodiac version comparison |
| BrowseTabMappingTest | 12 | Search result → browse tab routing |
| ShapesViewModelTest | 8 | Shape generation (async) |

Run tests: `./gradlew testDebugUnitTest`

## Versioning

Chinese Zodiac versioning system:
- **versionCode**: `YYMMDDHHmm` (integer, e.g., `2603080806`)
- **versionName**: `FireHorse.0308.0806-a` (display, zodiac + MMDD.HHmm + alpha suffix)
- Zodiac names: FireHorse (2026), FireGoat (2027), EarthMonkey (2028), etc.
- Computed at build time from `Calendar.getInstance()` in America/Chicago timezone

## Build & CI

- Build: `./gradlew assembleRelease`
- CI: GitHub Actions — builds APK and creates Release on push to `main`
- Local branch: `master`, remote default: `main`
- Push command: `git push origin master master:main`

## Performance Optimizations

- Baseline profiles for faster cold start
- 18 database indexes for search/filter queries
- All disk I/O on background threads (Dispatchers.IO)
- LRU bitmap cache (100 entries) for textures
- Debounced search (300ms)
- Async data seeding and sync
- Paging for large lists (Paging 3)
