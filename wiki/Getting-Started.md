# Getting Started

## Installation

### From GitHub Releases
1. Open the [latest release](https://github.com/beryndil/Spyglass/releases/latest) on your Android device
2. Download `app-release.apk`
3. Tap the file to install (you may need to allow "Install from unknown sources" in your device settings)

### From Google Play
Spyglass is available on the Google Play Store. Search for "Spyglass" or follow the direct link from the [website](https://hardknocks.university/).

## First Launch

### Consent Dialog
On first launch, Spyglass asks for your privacy preferences:

| Option | Default | Description |
|--------|---------|-------------|
| Analytics | Off | Anonymous usage analytics via Firebase |
| Crash Reports | Off | Automatic crash reporting via Crashlytics |
| Ad Personalization | Off | Personalized ads (non-personalized shown if off) |

You can choose **Accept All**, **Decline All**, or **Customize** individual toggles. These can be changed later in Settings > Privacy & Security.

### Texture Download
After first launch, the home screen shows a **Texture Setup Card** prompting you to download pixel-art textures (~1.3 MB). These textures are used throughout the app for blocks, items, mobs, and more. Without them, the app falls back to bundled vector icons.

### Data Seeding
On first launch, the app seeds its local database from bundled JSON files. This happens automatically in the background and takes a few seconds. After seeding, background sync checks for data updates every 12 hours.

## Navigation

Spyglass uses a bottom navigation bar with four tabs:

| Tab | Description |
|-----|-------------|
| **Home** | Dashboard with Connect hub, favorites, quick links, and tip of the day |
| **Browse** | 13 categories of Minecraft game data |
| **Calculators** | 19 tools for building, enchanting, and planning |
| **Search** | Global search across all categories |

The top-right menu provides access to Settings, Changelog, Feedback, and About.

## Startup Tab

You can change which tab opens when you launch the app:

**Settings > Appearance > Startup Screen** — choose Home, Browse, Calculators, or Search.

## App Lock

For privacy, you can enable biometric lock (fingerprint or face unlock):

**Settings > Privacy & Security > App Lock**

When enabled, the app requires biometric authentication every time it launches. If your device doesn't support biometrics, the lock falls back to your device PIN/password.

## Connecting to Your PC

To use Spyglass Connect features (inventory viewer, chest finder, map, etc.):

1. Download and run [Spyglass Connect](https://github.com/beryndil/Spyglass-Connect) on your PC
2. Make sure both devices are on the same WiFi network
3. Tap "Connect to PC" from the home screen dropdown menu
4. Scan the QR code shown on your PC

> **Note:** Both apps must support the same protocol version. If you see a version mismatch error, update whichever app the message indicates.

See the [Spyglass Connect](Spyglass-Connect) page for full details.
