# Spyglass Connect

Spyglass Connect is a desktop companion app that reads your Minecraft Java Edition save files and streams the data to your phone over local WiFi. No cloud servers, no accounts — everything stays on your network.

## Requirements

- [Spyglass Connect](https://github.com/beryndil/Spyglass-Connect) running on your PC (Windows, macOS, or Linux)
- Both devices on the same WiFi network
- Minecraft Java Edition save files accessible on your PC

## How It Works

1. **Launch** Spyglass Connect on your PC
2. **Scan** the QR code from the Spyglass app on your phone
3. **Done** — your phone auto-reconnects whenever both devices are on the same WiFi

Under the hood, the QR code contains connection info (IP, port, public key) encoded as Base64 JSON. Devices pair via ECDH key exchange and communicate over an encrypted WebSocket (AES-256-GCM). mDNS handles automatic reconnection when you rejoin the network.

## Connection Details

| Detail | Value |
|--------|-------|
| Protocol | WebSocket (`ws://`) over LAN |
| Port | 29170 |
| Encryption | ECDH + AES-256-GCM |
| Discovery | mDNS (NsdManager on Android, JmDNS on desktop) |
| IP Restriction | Private/LAN IPs only (10.x, 172.16-31.x, 192.168.x, 127.x) |
| Protocol Version | v2 (encrypted, with version negotiation) |

## Features

### Character Viewer
See your player's full equipment — armor, held items, offhand, and all stats. Each item shows its enchantments with upgrade recommendations. Tap any item to view its full Spyglass detail page.

Active potion effects are displayed with remaining duration.

### Inventory Viewer
Browse your complete inventory in a 36-slot grid (9 columns x 4 rows), plus armor slots (head, chest, legs, feet) and offhand. Tap an item for a name tooltip, long-press to open its full item card.

Inventory updates live as you play — changes sync automatically while connected.

### Ender Chest
View your ender chest contents in a 27-slot grid (9 columns x 3 rows). Same interaction as the inventory viewer.

### Chest Finder
Search for any item across **all containers** in your world — chests, barrels, shulker boxes, hoppers, and more. Type an item name in the search bar to filter results. Each result shows:

- Container type and coordinates
- Full inventory grid of the container
- Item count

You can also browse all containers without searching to see everything in your world.

### World Map
An interactive overhead terrain map rendered from your world data:

- Pinch to zoom, drag to pan
- Crosshair shows your player position
- Structure markers for villages, temples, monuments, etc.
- Dimension switcher (Overworld, Nether, End)

Map tiles are streamed as base64 PNG images from the desktop app.

### Player Statistics
Lifetime stats pulled directly from your Minecraft save file:

- Blocks mined, placed, and broken
- Mobs killed and times died
- Distance walked, sprinted, flown, and swum
- Play time and other counters

Values are auto-formatted: centimeters convert to blocks/km, ticks convert to hours/minutes, and large numbers get comma separators. Stats are cached locally so they're viewable even when disconnected.

### Advancements Roadmap
Interactive tree view showing your advancement progress across all 5 tabs:

| Tab | Description |
|-----|-------------|
| Story | Main game progression |
| Nether | Nether-related achievements |
| End | End dimension achievements |
| Adventure | Exploration and combat |
| Husbandry | Animals, farming, and nature |

Each advancement shows its status:
- **Completed** (green) — you've earned it
- **Available** (gray) — requirements are accessible
- **Locked** (dark) — parent advancement not yet completed

Progress bars show completion percentage per tab. Bundled advancement metadata is merged with live save data for accurate tracking.

## Connection States

| State | Description |
|-------|-------------|
| Disconnected | No active connection |
| Connecting | Establishing WebSocket to desktop |
| Pairing | QR handshake in progress |
| Connected | Active connection with device name shown |
| Reconnecting | Auto-reconnect attempt (with attempt count) |
| Error | Connection failed with error message |

## Auto-Reconnect

When the connection drops unexpectedly (not user-initiated), Spyglass automatically attempts to reconnect. The desktop app is rediscovered via mDNS. When reconnected, the app re-sends any previously selected world to restore your session.

## Network Security

- Connections are restricted to private/LAN IP addresses only
- The app validates the target IP before connecting
- All WebSocket traffic is encrypted with ECDH + AES-256-GCM after pairing
- All other network traffic (data sync, Firebase, ads) uses HTTPS

## Compatibility

Both apps negotiate protocol versions during the pairing handshake. If either side is running an incompatible version, pairing is rejected with a clear message explaining which app to update.

| Scenario | Message |
|----------|---------|
| Phone app too old | "Spyglass Connect requires protocol version 2+. Update your Spyglass app." |
| Desktop app too old | "Your Spyglass app requires a newer desktop version. Update Spyglass Connect." |

## Home Screen Integration

When connected, the home screen shows a **Connect Hub** with quick-link cards for each feature:

- Character, Inventory, Ender Chest, Chest Finder, Map, Statistics, Advancements

A connection status indicator shows whether you're connected, and a "Scan QR Code" button is always accessible.

## Crash Reporting to Desktop

When the app crashes, crash logs are saved locally and sent to the desktop app on the next Connect session. This helps with debugging without requiring cloud crash reporting.
