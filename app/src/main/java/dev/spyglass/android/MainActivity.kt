package dev.spyglass.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import dev.spyglass.android.core.ui.ConsentDialog
import dev.spyglass.android.core.ui.DEFAULT_THEME
import dev.spyglass.android.core.ui.SpyglassTheme
import dev.spyglass.android.navigation.AppNavGraph
import dev.spyglass.android.settings.PreferenceKeys
import dev.spyglass.android.settings.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val theme by dataStore.data
                .map { it[PreferenceKeys.BACKGROUND_THEME] ?: DEFAULT_THEME }
                .collectAsState(initial = DEFAULT_THEME)

            val consentShown by dataStore.data
                .map { it[PreferenceKeys.CONSENT_SHOWN] ?: false }
                .collectAsState(initial = true) // default true to avoid flash

            val scope = rememberCoroutineScope()

            val configuration = LocalConfiguration.current
            val isWideScreen = configuration.screenWidthDp.dp >= 600.dp

            SpyglassTheme(theme = theme, isWideScreen = isWideScreen) {
                if (!consentShown) {
                    ConsentDialog { analyticsConsent, crashConsent ->
                        scope.launch {
                            dataStore.edit {
                                it[PreferenceKeys.ANALYTICS_CONSENT] = analyticsConsent
                                it[PreferenceKeys.CRASH_CONSENT] = crashConsent
                                it[PreferenceKeys.CONSENT_SHOWN] = true
                            }
                        }
                    }
                }
                AppNavGraph()
            }
        }
    }
}
