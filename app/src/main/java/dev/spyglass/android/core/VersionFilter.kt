package dev.spyglass.android.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.spyglass.android.data.db.entities.VersionTagEntity
import dev.spyglass.android.settings.MinecraftVersions
import dev.spyglass.android.settings.PreferenceKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class VersionFilterState(
    val edition: String = "java",    // "java" or "bedrock"
    val version: String = "",        // e.g. "1.21" — empty = latest / show all
    val mode: String = "show_all",   // "show_all", "highlight", "hide"
)

enum class VersionAvailability {
    AVAILABLE,
    NOT_YET_ADDED,
    REMOVED,
    WRONG_EDITION,
}

data class MechanicsInfo(
    val changed: Boolean,
    val version: String,
    val notes: String,
)

fun checkAvailability(
    tag: VersionTagEntity?,
    filter: VersionFilterState,
): VersionAvailability {
    if (filter.mode == "show_all" || filter.version.isBlank()) return VersionAvailability.AVAILABLE
    if (tag == null) return VersionAvailability.AVAILABLE // no tag = exists since 1.0 in all editions

    val isJava = filter.edition == "java"

    // Check edition exclusivity
    if (isJava && tag.bedrockOnly) return VersionAvailability.WRONG_EDITION
    if (!isJava && tag.javaOnly) return VersionAvailability.WRONG_EDITION

    // Check version range
    val addedIn = if (isJava) tag.addedInJava else tag.addedInBedrock
    val removedIn = if (isJava) tag.removedInJava else tag.removedInBedrock

    if (addedIn.isNotBlank() && MinecraftVersions.compare(filter.version, addedIn) < 0) {
        return VersionAvailability.NOT_YET_ADDED
    }
    if (removedIn.isNotBlank() && MinecraftVersions.compare(filter.version, removedIn) >= 0) {
        return VersionAvailability.REMOVED
    }

    return VersionAvailability.AVAILABLE
}

/**
 * Checks whether this entity's mechanics were changed between its introduction
 * and the selected filter version. Returns non-null if relevant.
 */
fun checkMechanicsChanged(
    tag: VersionTagEntity?,
    filter: VersionFilterState,
): MechanicsInfo? {
    if (tag == null || filter.mode == "show_all" || filter.version.isBlank()) return null
    val isJava = filter.edition == "java"
    val mechVersion = if (isJava) tag.mechanicsChangedInJava else tag.mechanicsChangedInBedrock
    if (mechVersion.isBlank() || tag.mechanicsChangeNotes.isBlank()) return null

    val addedIn = if (isJava) tag.addedInJava else tag.addedInBedrock
    // Only relevant if mechanics changed after introduction and within selected version range
    if (addedIn.isNotBlank() && MinecraftVersions.compare(mechVersion, addedIn) <= 0) return null
    if (MinecraftVersions.compare(filter.version, mechVersion) >= 0) {
        return MechanicsInfo(changed = true, version = mechVersion, notes = tag.mechanicsChangeNotes)
    }
    return null
}

/** Creates a VersionFilterState flow from a DataStore. */
fun versionFilterFrom(store: DataStore<Preferences>): Flow<VersionFilterState> =
    store.data.map { prefs ->
        VersionFilterState(
            edition = prefs[PreferenceKeys.MINECRAFT_EDITION] ?: "java",
            version = prefs[PreferenceKeys.MINECRAFT_VERSION] ?: "",
            mode = prefs[PreferenceKeys.VERSION_FILTER_MODE] ?: "show_all",
        )
    }

/** Applies version filtering to a list flow — filters out unavailable entities in "hide" mode. */
fun <T> Flow<List<T>>.applyVersionFilter(
    versionFilter: Flow<VersionFilterState>,
    versionTags: Flow<Map<String, VersionTagEntity>>,
    typeKey: String,
    idExtractor: (T) -> String,
): Flow<List<T>> = combine(this, versionFilter, versionTags) { list, filter, tags ->
    if (filter.mode == "hide") {
        list.filter { checkAvailability(tags["$typeKey:${idExtractor(it)}"], filter) == VersionAvailability.AVAILABLE }
    } else list
}

/** Builds a lookup map keyed by "entityType:entityId" for O(1) access. */
fun List<VersionTagEntity>.toTagMap(): Map<String, VersionTagEntity> =
    associateBy { "${it.entityType}:${it.entityId}" }
