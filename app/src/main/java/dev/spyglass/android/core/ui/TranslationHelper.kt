package dev.spyglass.android.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.spyglass.android.data.repository.GameDataRepository
import dev.spyglass.android.settings.PreferenceKeys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Locales that have game-data translations. */
private val SUPPORTED_GAME_LOCALES = setOf("es", "pt", "fr", "de", "ja")

/**
 * Looks up a translated string for a game data entity field.
 * Returns [fallback] if the current locale is English or no translation is available.
 */
@Composable
fun translatedText(
    repo: GameDataRepository,
    entityType: String,
    entityId: String,
    field: String,
    fallback: String,
): String {
    val locale = LocalAppLocale.current
    if (locale == "en") return fallback
    val translated by repo.getTranslation(locale, entityType, entityId, field)
        .collectAsState(initial = null)
    return translated ?: fallback
}

/**
 * Resolves the user's language preference from DataStore into an actual locale code.
 * "system" resolves to the device default if supported, otherwise "en".
 */
fun resolveLocaleFlow(dataStore: DataStore<Preferences>): Flow<String> =
    dataStore.data.map { prefs ->
        val pref = prefs[PreferenceKeys.APP_LANGUAGE] ?: "system"
        if (pref == "system") {
            val lang = java.util.Locale.getDefault().language
            if (lang in SUPPORTED_GAME_LOCALES) lang else "en"
        } else pref
    }

/**
 * Reactive translation map for a given entity type.
 * Returns `Map<entityId, Map<field, translatedValue>>`.
 * Empty when locale is English (no lookups needed).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun translationMapFlow(
    dataStore: DataStore<Preferences>,
    repo: GameDataRepository,
    entityType: String,
): Flow<Map<String, Map<String, String>>> =
    kotlinx.coroutines.flow.combine(
        resolveLocaleFlow(dataStore),
        dataStore.data.map { it[PreferenceKeys.TRANSLATE_GAME_DATA] ?: true },
    ) { locale, enabled -> locale to enabled }
        .flatMapLatest { (locale, enabled) ->
            if (locale == "en" || !enabled) flowOf(emptyMap())
            else repo.translationsForType(locale, entityType).map { list ->
                list.groupBy { it.entityId }
                    .mapValues { (_, entries) -> entries.associate { it.field to it.value } }
            }
        }

/** Supported languages with display names (always shown in native script). */
val SupportedLanguages = listOf(
    "system" to "System Default",
    "en" to "English",
    "es" to "Espa\u00f1ol",
    "pt" to "Portugu\u00eas",
    "fr" to "Fran\u00e7ais",
    "de" to "Deutsch",
    "ja" to "\u65e5\u672c\u8a9e",
)
