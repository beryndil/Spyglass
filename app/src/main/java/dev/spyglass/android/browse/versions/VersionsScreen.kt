package dev.spyglass.android.browse.versions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.VersionCard
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.settings.MinecraftUpdates

@Composable
fun VersionsScreen() {
    val updates = MinecraftUpdates.JAVA_UPDATES.asReversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            SectionHeader(stringResource(R.string.versions_header))
            Text(
                stringResource(R.string.versions_subtitle, updates.size, updates.first().version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        items(updates, key = { it.version }) { update ->
            VersionCard(
                version = update.version,
                name = update.name,
                releaseDate = update.releaseDate,
                accentColor = update.color,
                icon = update.icon,
                changelog = update.changelog,
            )
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(8.dp))
        }
    }
}
