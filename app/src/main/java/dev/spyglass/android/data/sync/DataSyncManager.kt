package dev.spyglass.android.data.sync

import android.content.Context
import dev.spyglass.android.core.ui.TextureManager
import dev.spyglass.android.data.seed.DataSeeder
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

/**
 * Orchestrates data sync: fetches the remote manifest from GitHub,
 * compares with the local manifest, downloads changed JSON files,
 * and re-seeds the affected database tables.
 */
object DataSyncManager {

    private const val PREFS_NAME = "spyglass_sync"
    private const val KEY_MANIFEST = "local_manifest"

    /** Maps table name → JSON file name in the data directory. */
    private val TABLE_FILES = mapOf(
        "blocks" to "blocks.json",
        "mobs" to "mobs.json",
        "biomes" to "biomes.json",
        "enchants" to "enchants.json",
        "potions" to "potions.json",
        "trades" to "trades.json",
        "recipes" to "recipes.json",
        "structures" to "structures.json",
        "items" to "items.json",
        "advancements" to "advancements.json",
        "commands" to "commands.json",
        "version_tags" to "version_tags.json",
        "translations_es" to "translations_es.json",
        "translations_pt" to "translations_pt.json",
        "translations_fr" to "translations_fr.json",
        "translations_de" to "translations_de.json",
        "translations_ja" to "translations_ja.json",
    )

    /** Supported translation locales. */
    val TRANSLATION_LOCALES = listOf("es", "pt", "fr", "de", "ja")

    /**
     * Runs a full sync cycle:
     * 1. Fetch remote manifest
     * 2. Compare with local manifest
     * 3. Download changed files
     * 4. Re-seed affected tables
     * 5. Handle texture updates
     * 6. Persist updated manifest
     */
    suspend fun sync(context: Context) {
        Timber.d("DataSync: starting sync")

        // 1. Fetch remote manifest
        val remoteManifest = GitHubDataClient.fetchManifest()
        if (remoteManifest == null) {
            Timber.d("DataSync: no remote manifest, skipping")
            return
        }

        // 2. Load local manifest
        var localManifest = loadLocalManifest(context)

        Timber.d("DataSync: local v%s, remote v%s", localManifest.version, remoteManifest.version)

        // 3. Determine which tables have changed (compare per-table versions)
        val changed = remoteManifest.changedTables(localManifest)

        // 4. Download and reseed each changed table
        if (changed.isNotEmpty()) {
            Timber.d("DataSync: %d table(s) changed: %s", changed.size, changed)

            for (table in changed) {
                val fileName = TABLE_FILES[table] ?: continue
                var jsonContent: String? = null
                for (attempt in 1..3) {
                    jsonContent = GitHubDataClient.fetchDataFile(fileName)
                    if (jsonContent != null) break
                }
                if (jsonContent == null) {
                    Timber.w("DataSync: failed to download %s after retries, skipping", fileName)
                    continue
                }

                // Verify SHA-256 checksum if provided by the manifest
                val expectedHash = remoteManifest.checksums[fileName]
                if (!expectedHash.isNullOrBlank() && !verifySha256(jsonContent, expectedHash)) {
                    Timber.w("DataSync: checksum mismatch for %s, skipping", fileName)
                    continue
                }

                // Save to internal storage so DataSeeder can read it
                saveToInternalStorage(context, fileName, jsonContent)

                // Re-seed the table
                DataSeeder.reseedTable(context, table)

                // Update local manifest version for this table
                localManifest = localManifest.withVersion(table, remoteManifest.versionOf(table))
                saveLocalManifest(context, localManifest)

                Timber.d("DataSync: updated %s to version %s", table, remoteManifest.versionOf(table))
            }
        }

        // 5. Sync texture_map.json if version changed
        if (remoteManifest.hasTextureMapUpdate(localManifest)) {
            val textureMapJson = GitHubDataClient.fetchDataFile("texture_map.json")
            if (textureMapJson != null) {
                saveToInternalStorage(context, "texture_map.json", textureMapJson)
                TextureManager.loadTextureMaps(context)
                localManifest = localManifest.copy(textureMap = remoteManifest.textureMap)
                Timber.d("DataSync: updated texture_map to version %s", remoteManifest.textureMap)
            }
        }

        // 6. Flag texture update if textures are already downloaded and remote is newer
        if (remoteManifest.hasTextureUpdate(localManifest) &&
            TextureManager.state.value == TextureManager.TextureState.DOWNLOADED) {
            Timber.d("DataSync: texture update available (local=%s, remote=%s)",
                localManifest.textures, remoteManifest.textures)
            // Auto-update textures in background
            TextureManager.download(context)
            localManifest = localManifest.copy(textures = remoteManifest.textures)
        }

        // 7. Sync tips.json if version changed
        if (remoteManifest.hasTipsUpdate(localManifest)) {
            val tipsJson = GitHubDataClient.fetchDataFile("tips.json")
            if (tipsJson != null) {
                saveToInternalStorage(context, "tips.json", tipsJson)
                localManifest = localManifest.copy(tips = remoteManifest.tips)
                Timber.d("DataSync: updated tips to version %s", remoteManifest.tips)
            }
        }

        // Update top-level version
        localManifest = localManifest.copy(version = remoteManifest.version)
        saveLocalManifest(context, localManifest)

        Timber.d("DataSync: sync complete, now at v%s", remoteManifest.version)
    }

    /** Saves JSON content to internal storage at minecraft/{fileName}. */
    private fun saveToInternalStorage(context: Context, fileName: String, content: String) {
        val dir = File(context.filesDir, "minecraft")
        if (!dir.exists()) dir.mkdirs()
        File(dir, fileName).writeText(content)
    }

    fun loadLocalManifest(context: Context): DataManifest {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MANIFEST, null) ?: return loadBundledManifest(context)
        return try {
            DataManifest.fromJson(raw)
        } catch (e: Exception) {
            Timber.w(e, "DataSync: invalid local manifest, using bundled")
            loadBundledManifest(context)
        }
    }

    private fun loadBundledManifest(context: Context): DataManifest {
        return try {
            val raw = context.assets.open("minecraft/manifest.json").bufferedReader().readText()
            DataManifest.fromJson(raw)
        } catch (e: Exception) {
            Timber.w(e, "DataSync: failed to read bundled manifest")
            DataManifest()
        }
    }

    /** Returns true if the SHA-256 of [content] matches [expectedHex]. */
    private fun verifySha256(content: String, expectedHex: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
            val hex = hash.joinToString("") { "%02x".format(it) }
            hex.equals(expectedHex, ignoreCase = true)
        } catch (e: Exception) {
            Timber.w(e, "DataSync: SHA-256 verification error")
            false // fail closed — reject if hashing itself fails; next sync retries
        }
    }

    private fun saveLocalManifest(context: Context, manifest: DataManifest) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MANIFEST, DataManifest.toJson(manifest))
            .apply()
    }
}
