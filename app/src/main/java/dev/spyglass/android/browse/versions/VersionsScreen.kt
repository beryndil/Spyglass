package dev.spyglass.android.browse.versions

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.spyglass.android.core.ui.SectionHeader
import dev.spyglass.android.core.ui.VersionCard
import dev.spyglass.android.core.ui.translationMapFlow
import androidx.compose.ui.res.stringResource
import dev.spyglass.android.R
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.MinecraftUpdates
import dev.spyglass.android.settings.dataStore

@Composable
fun VersionsScreen() {
    val updates = MinecraftUpdates.JAVA_UPDATES.asReversed()
    val app = LocalContext.current.applicationContext as Application
    val repo by produceState<GameDataRepository?>(null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            GameDataRepository.get(app)
        }
    }
    val txMap by remember(repo) {
        repo?.let { translationMapFlow(app.dataStore, it, "mc_update") }
            ?: kotlinx.coroutines.flow.flowOf(emptyMap())
    }.collectAsState(initial = emptyMap())

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
            val tx = txMap[update.version]
            VersionCard(
                version = update.version,
                name = tx?.get("name") ?: update.name,
                releaseDate = tx?.get("date") ?: update.releaseDate,
                accentColor = update.color,
                icon = update.icon,
                changelog = update.changelog.mapIndexed { i, fallback ->
                    tx?.get("c${i + 1}") ?: fallback
                },
            )
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(8.dp))
        }
    }
}
