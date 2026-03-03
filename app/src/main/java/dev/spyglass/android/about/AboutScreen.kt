package dev.spyglass.android.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import dev.spyglass.android.data.sync.DataManifest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.spyglass.android.BuildConfig
import dev.spyglass.android.R
import dev.spyglass.android.core.ui.*

@Composable
fun AboutScreen(onBack: () -> Unit = {}, onLicense: () -> Unit = {}, onDisclaimer: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val manifest by produceState<DataManifest?>(null) {
        value = try {
            val prefs = context.getSharedPreferences("spyglass_sync", android.content.Context.MODE_PRIVATE)
            val raw = prefs.getString("local_manifest", null)
            if (raw != null) {
                DataManifest.fromJson(raw)
            } else {
                val bundled = context.assets.open("minecraft/manifest.json").bufferedReader().readText()
                DataManifest.fromJson(bundled)
            }
        } catch (_: Exception) { null }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "back") {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── App Info ─────────────────────────────────────────────────────────
        item(key = "app_info") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpyglassIconImage(
                    icon = SpyglassIcon.Drawable(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.about_app_title),
                    modifier = Modifier.size(64.dp),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_app_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_companion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.about_author),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                SpyglassIconImage(
                    icon = SpyglassIcon.Drawable(R.drawable.about_beryndil),
                    contentDescription = "Beryndil",
                    modifier = Modifier.height(140.dp),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.height(12.dp))
                val coffeeUrl = "https://buymeacoffee.com/beryndil"
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .clickable { uriHandler.openUri(coffeeUrl) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.about_enjoy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.about_coffee),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        // ── Disclaimer ───────────────────────────────────────────────────────
        item(key = "disclaimer") {
            Text(
                text = stringResource(R.string.about_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDisclaimer() },
            )
        }

        // ── Game Data ────────────────────────────────────────────────────────
        item(key = "game_data") {
            SectionHeader(stringResource(R.string.about_game_data))
            ResultCard {
                StatRow(stringResource(R.string.home_minecraft_version), stringResource(R.string.about_minecraft_version))
                StatRow(stringResource(R.string.about_data_version), formatDataVersion(manifest?.effectiveVersion ?: 0L))
                StatRow("Images Version", formatDataVersion(manifest?.textures ?: 0L))
            }
        }

        // ── Credits & Licenses ───────────────────────────────────────────────
        item(key = "credits") {
            SectionHeader(stringResource(R.string.about_credits))
            ResultCard {
                // Spyglass license
                Text(stringResource(R.string.about_app_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.about_author), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text(
                    text = stringResource(R.string.about_license_cc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onLicense() },
                )

                SpyglassDivider()

                // Pixel Perfection
                Text(stringResource(R.string.about_pixel_perfection), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.about_by_xssheep), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.about_cc_by_sa),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://creativecommons.org/licenses/by-sa/4.0/")
                        },
                    )
                    Text(
                        text = stringResource(R.string.about_project_page),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.curseforge.com/minecraft/texture-packs/pixel-perfection")
                        },
                    )
                }

                SpyglassDivider()

                // Entity-Icons
                Text(stringResource(R.string.about_entity_icons), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.about_cc0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://creativecommons.org/publicdomain/zero/1.0/")
                        },
                    )
                    Text(
                        text = stringResource(R.string.about_github),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/Simplexity-Development/Entity-Icons")
                        },
                    )
                }
            }
        }

        // ── App License ──────────────────────────────────────────────────────
        item(key = "license") {
            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── Privacy ──────────────────────────────────────────────────────────
        item(key = "privacy") {
            SectionHeader(stringResource(R.string.about_privacy))
            ResultCard {
                Text(
                    text = stringResource(R.string.about_privacy_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Feedback ─────────────────────────────────────────────────────────
        item(key = "feedback") {
            SectionHeader(stringResource(R.string.feedback))
            ResultCard {
                val issuesUrl = "https://github.com/beryndil/spyglass-android/issues"
                Text(
                    text = stringResource(R.string.about_report_bug),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(issuesUrl) }
                        .padding(vertical = 4.dp),
                )
                SpyglassDivider()
                Text(
                    text = stringResource(R.string.about_request_feature),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(issuesUrl) }
                        .padding(vertical = 4.dp),
                )
            }
        }

        item(key = "bottom_spacer") { Spacer(Modifier.height(8.dp)) }
    }
}

/** Formats a YYMMDDHHMM Long (e.g. 2603021319) as "2026.0302.1319". */
private fun formatDataVersion(v: Long): String {
    if (v < 1_000_000_000) return v.toString() // legacy int format
    val yy = v / 100_000_000
    val mm = (v / 1_000_000) % 100
    val dd = (v / 10_000) % 100
    val hh = (v / 100) % 100
    val min = v % 100
    return "20%02d.%02d%02d.%02d%02d".format(yy, mm, dd, hh, min)
}
