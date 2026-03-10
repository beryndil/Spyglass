package dev.spyglass.android.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.spyglass.android.data.repository.GameDataRepository

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
