# FAQ

## General

### What Minecraft version does Spyglass support?
Spyglass targets **Minecraft Java Edition 1.21.4**. While it includes Bedrock edition info for reference, the primary data is Java Edition.

### Does Spyglass work offline?
Yes. All game data is bundled with the app. You can browse blocks, items, recipes, mobs, and use all 19 calculators without internet. Only Spyglass Connect (desktop pairing) and data sync require a network connection.

### How much storage does Spyglass use?
The app itself is ~30 MB. Downloading textures adds ~1.3 MB. The local database with all game data adds a few more MB. Total: roughly 35-40 MB.

### Is Spyglass affiliated with Mojang or Microsoft?
No. Spyglass is not affiliated with, endorsed by, or associated with Mojang Studios or Microsoft. Minecraft is a trademark of Mojang Studios. All game data is used for informational purposes only.

---

## Spyglass Connect

### What is Spyglass Connect?
A desktop companion app that reads your Minecraft save files and streams the data to your phone over local WiFi. View your inventory, search chests, find structures, and explore an overhead map.

### Do I need a special account?
No accounts required. Pairing is done via QR code on your local network.

### Is my data sent to any servers?
No. Everything stays on your local network. The desktop app reads your save files directly, and data is transmitted over a local WebSocket connection. No cloud servers are involved.

### Why can't I connect to a public IP?
For security, Spyglass Connect only allows connections to private/LAN IP addresses (10.x, 172.16-31.x, 192.168.x, 127.x). This prevents accidental connections to public servers.

### The connection keeps dropping. What can I do?
- Make sure both devices are on the same WiFi network
- Check that your router allows local device communication (some guest networks block this)
- Spyglass auto-reconnects when the connection drops — wait a moment for it to reconnect
- If the desktop app was restarted, you may need to re-select your world

### Does it work with Bedrock Edition?
No. Spyglass Connect reads Java Edition save files only. Bedrock uses a different save format.

---

## Data & Sync

### How does data stay up to date?
The app automatically checks for updates every 12 hours (configurable in Settings). It downloads only changed files from the [Spyglass-Data CDN](https://data.hardknocks.university). You can also tap "Sync Now" in Settings.

### What if the sync fails?
The app falls back to its bundled data. If the CDN is unreachable, it tries GitHub directly. If both fail, you still have the data that was bundled with your version of the app.

### Can I force a manual sync?
Yes. Go to **Settings > Data & Sync > Sync Now**.

### How do I know what data version I have?
Go to **Settings > About**. The data version is displayed there (e.g., `FireHorse.0308.1430`).

---

## Textures

### Why do I need to download textures separately?
To keep the APK download size small (~30 MB vs ~31.3 MB), pixel-art textures are downloaded separately on first launch. The app works without them using vector icon fallbacks.

### Can I delete downloaded textures?
Yes. Go to **Settings > Data & Sync > Textures > Delete**. The app will revert to bundled vector icons.

### How do I update textures?
When new textures are available, an "Update" option appears in **Settings > Data & Sync > Textures**.

---

## Settings & Customization

### How do I change the theme?
**Settings > Appearance > Theme** — tap any color swatch. There are 11 dark themes and 6 light themes, plus Material You dynamic color on Android 12+.

### How do I change the text size?
**Settings > Appearance > Font Scale** — choose Small, Default, Large, or Extra Large.

### How do I lock the app with biometrics?
**Settings > Privacy & Security > App Lock** — toggle on. The app will require fingerprint or face authentication on launch.

### How do I disable ads?
Ads support the ongoing development of Spyglass. There is currently no option to disable them, but you can control ad personalization in **Settings > Privacy & Security > Ad Personalization**.

---

## Calculators

### What calculators are available?
19 tools: Todo List, Shopping Lists, Enchant Optimizer, Block Fill, Shape Designer, Maze Maker, Storage, Smelting, Nether Tools, Game Clock, Light Spacing, Banner Designer, Food Browser, Notes, Waypoints, Redstone, Librarian Guide, Loot Tables, and Armor Trims. See the full [Calculators](Calculators) page.

### How do I enable the Librarian Guide?
It's an experimental feature. Enable it in **Settings > Game Version > Show Experimental Features**.

### Are my todos and notes saved?
Yes. Todos, shopping lists, notes, waypoints, and portals are stored in the local database and persist across app restarts.

---

## Updates

### How do I know when a new version is available?
The app checks for updates and shows an "Update Available" card on the Home screen when a newer version exists. Tap it to open the download page.

### Do I lose my data when updating?
No. Your settings, favorites, todos, notes, waypoints, and other user data are preserved across updates. Game data may be re-seeded if the data version changed.

---

## Privacy

### What data does Spyglass collect?
By default, nothing. Analytics, crash reports, and ad personalization are all **opt-in**. You choose what to share on first launch, and can change it anytime in Settings.

### Where is my data stored?
All game data, settings, and user data (todos, notes, waypoints) are stored locally on your device. Nothing is uploaded to any server unless you explicitly opt in to analytics or crash reporting.

### Can I read the full privacy policy?
Yes: [Privacy Policy](https://hardknocks.university/privacy-policy/)
