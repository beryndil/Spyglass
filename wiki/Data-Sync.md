# Data Sync

Spyglass keeps its game data current through an automatic sync system. All data is bundled with the app for offline use, and background sync downloads only what's changed.

## How It Works

```
App Launch → Check manifest → Compare versions → Download changed files → Re-seed database
```

1. The app fetches `manifest.json` from the CDN
2. Each file in the manifest has a version string (e.g., `FireHorse.0308.1430`)
3. The app compares remote versions against its local manifest
4. Only files with newer versions are downloaded
5. Downloaded files are parsed and inserted into the local Room database

## CDN Architecture

| Component | URL | Purpose |
|-----------|-----|---------|
| **Primary CDN** | `data.hardknocks.university` | Cloudflare Pages — edge-cached across 300+ global servers |
| **Fallback** | `raw.githubusercontent.com/beryndil/Spyglass-Data/main/` | GitHub raw — used if CDN is unreachable |

The app tries the CDN first. If it fails (network error, HTTP error), it automatically falls back to GitHub. A log message `"CDN miss, fell back to GitHub"` is recorded when fallback is used.

## Sync Schedule

| Setting | Options | Default |
|---------|---------|---------|
| Sync Frequency | 1h, 6h, 12h, 24h | 12h |
| Offline Mode | On/Off | Off |

Background sync runs via Android WorkManager with a `NetworkType.CONNECTED` constraint — it only runs when the device has an active network connection.

You can also trigger a manual sync: **Settings > Data & Sync > Sync Now**.

## Data Files Synced

| File | Content | Entries |
|------|---------|---------|
| blocks.json | All Java Edition blocks | 1,167 |
| items.json | Items with properties | 502 |
| recipes.json | Crafting, smelting, and processing | 1,291 |
| mobs.json | Mob stats and drops | 81 |
| trades.json | Villager trades | 231 |
| biomes.json | Biome data | 65 |
| structures.json | Generated structures | 25 |
| enchants.json | Enchantments | 43 |
| potions.json | All potion variants | 130 |
| advancements.json | Advancement trees | 125 |
| commands.json | Java Edition commands | 85 |
| version_tags.json | Edition/version metadata | 1,279 |
| textures.zip | Pixel-art textures | 696 PNGs (~1.3 MB) |

## Manifest Format

The manifest uses a per-file versioning scheme:

```json
{
  "blocks": "FireHorse.0308.1430",
  "items": "FireHorse.0305.0900",
  "textures": "FireHorse.0301.1200",
  ...
}
```

Version strings follow the Chinese Zodiac format: `ZodiacName.MMDD.HHmm`. The zodiac name maps to the year (FireHorse = 2026), and the timestamp uses America/Chicago timezone.

## Texture System

Textures are handled separately from JSON data:

1. **First launch**: App prompts to download `textures.zip` from the CDN
2. **Extraction**: 696 PNG files extracted to internal storage (`filesDir/textures/`)
3. **Caching**: LRU bitmap cache (100 entries) for fast rendering
4. **Updates**: When the `textures` version in the manifest is newer, Settings shows an "Update" button
5. **Deletion**: Textures can be deleted from Settings to free ~1.3 MB of storage
6. **Fallback**: Without downloaded textures, the app uses bundled vector icons

## Offline Mode

When **Offline Mode** is enabled in Settings, the app:
- Skips all automatic background sync
- Uses only bundled data and previously synced data
- All features remain functional with local data

## Bundled Data

Every data file is also bundled in the APK under `assets/minecraft/`. This ensures:
- The app works immediately on first launch without internet
- Fresh installs have complete data
- If sync fails, the app always has a working fallback

## Update Checking

Separate from data sync, the app checks for **app updates** by fetching the latest GitHub release tag. If a newer version exists, an "Update Available" card appears on the Home screen. Tapping it opens the Play Store or GitHub Releases page.
