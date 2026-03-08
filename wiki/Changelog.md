# Changelog

## v1.0-alpha.7 (March 2026)
- Modular architecture — Connect, Browse, and Tools are now independent modules that can be toggled on/off in Settings
- Cold start optimization — MobileAds, Firebase, and DataStore reads moved off main thread for faster launch
- Rebranded to Spyglass Connect throughout the app
- All links updated to hardknocks.university
- Developer page linked from About screen

## v1.0-alpha.6 (March 2026)
- **Spyglass Connect: Player Statistics** — view lifetime stats pulled from your Minecraft save (blocks mined, mobs killed, distance walked, play time, and more)
- **Spyglass Connect: Advancements Roadmap** — interactive tree showing completed, available, and locked advancements across all 5 tabs
- Statistics auto-format distance (cm to blocks/km), time (ticks to hours/minutes), and large numbers with commas
- Advancements merge bundled metadata with live save data — filter by tab, see progress bar, and track what's next
- Offline caching for both Statistics and Advancements — viewable even when disconnected from PC

## v1.0-alpha.5 (March 2026)
- News feed on the home screen — synced from Spyglass-Data, supports Markdown and images
- Updatable textures — bundled icons can be updated via sync without rebuilding the APK
- Versioning now includes hour and minute (YYMMDDHHMM format)
- Images version displayed on the About screen
- Texture lookup uses downloaded overrides with bundled drawable fallback

## v1.0-alpha.4 (March 2026)
- Remote data sync — app checks GitHub for data updates without needing an app update
- Data version displayed in About screen for easy verification
- Removed player username/skin feature for a cleaner home screen

## v1.0-alpha.3 (February 2026)
- Major data accuracy pass across all 11 categories
- 1,167 blocks with corrected properties and missing entries added
- 550+ entries added or corrected across mobs, biomes, items, trades, and more
- 5 new shapes: Wall, Arch, Ellipsoid, Arc Wall, Spiral
- Smelting XP values for all 78 smelting recipes
- Block glance text, ore Y-levels, and sort/filter UI for browse tabs
- Linkified entity descriptions with cross-tab navigation
- Search moved to top of home page
- Reference tab added to Browse
- Game clock: editable day counter and color fixes
- Baseline profiles and trace instrumentation for faster startup
- ANR fix: database init moved off main thread
- Startup performance: HomeScreen converted to LazyColumn

## v1.0-alpha.2 (February 2026)
- 15 background color themes — dark, mid-tone, and light options
- Theme picker in Settings with tappable color swatches
- Improved contrast for gold accent and muted text on light themes
- Expanded item data with armor stats, food stats, and attack info

## v1.0-alpha.1 (February 2026)
- 930+ blocks and 360 items with full detail cards
- 9 Browse tabs: Blocks, Items, Recipes, Mobs, Trades, Biomes, Structures, Enchantments, Potions
- 7 Tools: Block Fill, Smelting, Storage, Nether Portal, Shapes, Enchanting, Quick Reference
- Enchant optimizer — cheapest XP order for combining books on the anvil
- Cross-tab links — tap any item, mob, biome, or structure to jump to its detail page
- Global search across all data types
- Favorites system — star any item and pin it to the top of Browse tabs
- Settings — default tabs, tip of the day toggle, favorites on home page
- Changelog and Feedback screens

---

## Production Readiness Updates (March 8, 2026)

These changes were applied across the codebase for production scale:

- **Cloudflare CDN** — data sync now served from `data.hardknocks.university` via Cloudflare Pages with 300+ edge servers, unlimited bandwidth, automatic fallback to GitHub
- **18 database indexes** across 12 entities for faster search and filtering (migration 27→28)
- **ANR prevention** — TipsLoader rewritten with async suspend + caching, all disk I/O off main thread
- **Network security** — Spyglass Connect validates LAN-only IP addresses before connecting
- **Settings reorganized** — streamlined from 12 sections to 7 logical groups across modules
- **Test stability** — fixed flaky ShapesViewModel tests with proper async assertions
