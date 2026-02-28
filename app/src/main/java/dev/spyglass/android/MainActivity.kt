package dev.spyglass.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import dev.spyglass.android.core.ui.DEFAULT_THEME
import dev.spyglass.android.core.ui.SpyglassTheme
import dev.spyglass.android.data.seed.DataSeeder
import dev.spyglass.android.navigation.AppNavGraph
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Seed game data from bundled JSON assets (no-op after first install)
        lifecycleScope.launch { DataSeeder.seedIfNeeded(this@MainActivity) }

        setContent {
            val theme by dataStore.data
                .map { it[PreferenceKeys.BACKGROUND_THEME] ?: DEFAULT_THEME }
                .collectAsState(initial = DEFAULT_THEME)

            SpyglassTheme(theme = theme) {
                AppNavGraph()
            }
        }
    }
}
