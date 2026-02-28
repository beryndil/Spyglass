package dev.spyglass.android.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.spyglass.android.BuildConfig
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun AboutScreen(onBack: () -> Unit = {}, onLicense: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Stone300,
            )
        }

        // ── App Info ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpyglassIconImage(
                icon = SpyglassIcon.Drawable(R.drawable.ic_launcher_foreground),
                contentDescription = "Spyglass",
                modifier = Modifier.size(64.dp),
                tint = Color.Unspecified,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Spyglass",
                style = MaterialTheme.typography.headlineMedium,
                color = Gold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone300,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A Minecraft companion tool.",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone300,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "by Beryndil",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone500,
            )
            Spacer(Modifier.height(8.dp))
            SpyglassIconImage(
                icon = SpyglassIcon.Drawable(R.drawable.about_beryndil),
                contentDescription = "Beryndil",
                modifier = Modifier.height(140.dp),
                tint = Color.Unspecified,
            )
            Spacer(Modifier.height(12.dp))
            // TODO: Replace with actual Buy Me a Coffee / Ko-fi URL
            val coffeeUrl = "https://buymeacoffee.com/TODO"
            Column(
                modifier = Modifier
                    .background(Gold, RoundedCornerShape(8.dp))
                    .clickable { uriHandler.openUri(coffeeUrl) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Enjoy SpyGlass?",
                    style = MaterialTheme.typography.bodySmall,
                    color = SurfaceMid,
                )
                Text(
                    text = "Buy Me a Cup of Coffee",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Background,
                )
            }
        }

        // ── Disclaimer ───────────────────────────────────────────────────────
        Text(
            text = "Not affiliated with Mojang Studios or Microsoft.",
            style = MaterialTheme.typography.bodySmall,
            color = Stone500,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // ── Game Data ────────────────────────────────────────────────────────
        SectionHeader("Game Data")
        ResultCard {
            StatRow("Minecraft Version", "Java 1.21.4")
        }

        // ── Credits & Licenses ───────────────────────────────────────────────
        SectionHeader("Credits & Licenses")
        ResultCard {
            // Spyglass license
            Text("Spyglass", style = MaterialTheme.typography.bodyLarge, color = Stone100)
            Text("by Beryndil", style = MaterialTheme.typography.bodyMedium, color = Stone500)
            Text(
                text = "CC BY-NC-SA 4.0 \u2014 View License",
                style = MaterialTheme.typography.bodyMedium,
                color = Gold,
                modifier = Modifier.clickable { onLicense() },
            )

            SpyglassDivider()

            // Pixel Perfection
            Text("Pixel Perfection", style = MaterialTheme.typography.bodyLarge, color = Stone100)
            Text("by XSSheep", style = MaterialTheme.typography.bodyMedium, color = Stone500)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CC-BY-SA 4.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://creativecommons.org/licenses/by-sa/4.0/")
                    },
                )
                Text(
                    text = "Project Page",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://www.curseforge.com/minecraft/texture-packs/pixel-perfection")
                    },
                )
            }

            SpyglassDivider()

            // Entity-Icons
            Text("Entity-Icons", style = MaterialTheme.typography.bodyLarge, color = Stone100)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CC0 1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://creativecommons.org/publicdomain/zero/1.0/")
                    },
                )
                Text(
                    text = "GitHub",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold,
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://github.com/Simplexity-Development/Entity-Icons")
                    },
                )
            }
        }

        // ── App License ──────────────────────────────────────────────────────
        Text(
            text = "\u00A9 2026 Beryndil. Licensed under CC BY-NC-SA 4.0.",
            style = MaterialTheme.typography.bodySmall,
            color = Stone500,
            modifier = Modifier.fillMaxWidth(),
        )

        // ── Privacy ──────────────────────────────────────────────────────────
        SectionHeader("Privacy")
        ResultCard {
            Text(
                text = "All game data is stored locally on your device. If you provide your Minecraft username, it is sent to Mojang (api.mojang.com) to look up your player UUID. Your UUID is then used to load your skin from third-party rendering services (starlightskins.lunareclipse.studio, mc-heads.net). No other personal data is collected or shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone300,
            )
        }

        // ── Feedback ─────────────────────────────────────────────────────────
        SectionHeader("Feedback")
        ResultCard {
            val issuesUrl = "https://github.com/user/spyglass-android/issues"
            Text(
                text = "Report a Bug",
                style = MaterialTheme.typography.bodyLarge,
                color = Gold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(issuesUrl) }
                    .padding(vertical = 4.dp),
            )
            SpyglassDivider()
            Text(
                text = "Request a Feature",
                style = MaterialTheme.typography.bodyLarge,
                color = Gold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(issuesUrl) }
                    .padding(vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}
