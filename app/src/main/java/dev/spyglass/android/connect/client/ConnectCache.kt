package dev.spyglass.android.connect.client

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.spyglass.android.connect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

/**
 * File-based cache for Connect data so it's viewable offline.
 * Directory layout: filesDir/connect/meta.json, filesDir/connect/{worldFolder}/...
 */
object ConnectCache {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class CacheMeta(
        val selectedWorld: String? = null,
        val worlds: List<WorldInfo> = emptyList(),
    )

    // ── Directories ──────────────────────────────────────────────────────────

    private fun connectDir(context: Context): File =
        File(context.filesDir, "connect")

    private fun worldDir(context: Context, worldFolder: String): File =
        File(connectDir(context), worldFolder)

    // ── Meta ─────────────────────────────────────────────────────────────────

    suspend fun saveMeta(context: Context, meta: CacheMeta) = withContext(Dispatchers.IO) {
        try {
            val dir = connectDir(context)
            dir.mkdirs()
            File(dir, "meta.json").writeText(json.encodeToString(meta))
        } catch (e: Exception) {
            Timber.w(e, "Failed to save cache meta")
        }
    }

    suspend fun loadMeta(context: Context): CacheMeta? = withContext(Dispatchers.IO) {
        try {
            val file = File(connectDir(context), "meta.json")
            if (!file.exists()) return@withContext null
            json.decodeFromString<CacheMeta>(file.readText())
        } catch (e: Exception) {
            Timber.w(e, "Failed to load cache meta")
            null
        }
    }

    // ── Player Data ──────────────────────────────────────────────────────────

    suspend fun savePlayerData(context: Context, worldFolder: String, data: PlayerData) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "player_data.json").writeText(json.encodeToString(data))
                saveLastUpdated(context, worldFolder)
            } catch (e: Exception) {
                Timber.w(e, "Failed to save player data cache")
            }
        }

    suspend fun loadPlayerData(context: Context, worldFolder: String): PlayerData? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "player_data.json")
                if (!file.exists()) return@withContext null
                json.decodeFromString<PlayerData>(file.readText())
            } catch (e: Exception) {
                Timber.w(e, "Failed to load player data cache")
                null
            }
        }

    // ── Structures ───────────────────────────────────────────────────────────

    suspend fun saveStructures(context: Context, worldFolder: String, structures: List<StructureLocation>) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "structures.json").writeText(json.encodeToString(structures))
            } catch (e: Exception) {
                Timber.w(e, "Failed to save structures cache")
            }
        }

    suspend fun loadStructures(context: Context, worldFolder: String): List<StructureLocation>? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "structures.json")
                if (!file.exists()) return@withContext null
                json.decodeFromString<List<StructureLocation>>(file.readText())
            } catch (e: Exception) {
                Timber.w(e, "Failed to load structures cache")
                null
            }
        }

    // ── Map Data ─────────────────────────────────────────────────────────────

    suspend fun saveMapData(context: Context, worldFolder: String, data: MapRenderPayload) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "map_data.json").writeText(json.encodeToString(data))
            } catch (e: Exception) {
                Timber.w(e, "Failed to save map data cache")
            }
        }

    suspend fun loadMapData(context: Context, worldFolder: String): MapRenderPayload? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "map_data.json")
                if (!file.exists()) return@withContext null
                json.decodeFromString<MapRenderPayload>(file.readText())
            } catch (e: Exception) {
                Timber.w(e, "Failed to load map data cache")
                null
            }
        }

    // ── Stats ──────────────────────────────────────────────────────────────────

    suspend fun saveStats(context: Context, worldFolder: String, data: PlayerStatsPayload) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "stats.json").writeText(json.encodeToString(data))
                saveLastUpdated(context, worldFolder)
            } catch (e: Exception) {
                Timber.w(e, "Failed to save stats cache")
            }
        }

    suspend fun loadStats(context: Context, worldFolder: String): PlayerStatsPayload? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "stats.json")
                if (!file.exists()) return@withContext null
                json.decodeFromString<PlayerStatsPayload>(file.readText())
            } catch (e: Exception) {
                Timber.w(e, "Failed to load stats cache")
                null
            }
        }

    // ── Advancements ──────────────────────────────────────────────────────────

    suspend fun saveAdvancements(context: Context, worldFolder: String, data: PlayerAdvancementsPayload) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "advancements.json").writeText(json.encodeToString(data))
                saveLastUpdated(context, worldFolder)
            } catch (e: Exception) {
                Timber.w(e, "Failed to save advancements cache")
            }
        }

    suspend fun loadAdvancements(context: Context, worldFolder: String): PlayerAdvancementsPayload? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "advancements.json")
                if (!file.exists()) return@withContext null
                json.decodeFromString<PlayerAdvancementsPayload>(file.readText())
            } catch (e: Exception) {
                Timber.w(e, "Failed to load advancements cache")
                null
            }
        }

    // ── Skin Bitmaps ─────────────────────────────────────────────────────────

    suspend fun saveSkinAvatar(context: Context, worldFolder: String, bitmap: Bitmap) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "skin_avatar.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to save skin avatar cache")
            }
        }

    suspend fun loadSkinAvatar(context: Context, worldFolder: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "skin_avatar.png")
                if (!file.exists()) return@withContext null
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                Timber.w(e, "Failed to load skin avatar cache")
                null
            }
        }

    suspend fun saveSkinBody(context: Context, worldFolder: String, bitmap: Bitmap) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "skin_body.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to save skin body cache")
            }
        }

    suspend fun loadSkinBody(context: Context, worldFolder: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "skin_body.png")
                if (!file.exists()) return@withContext null
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                Timber.w(e, "Failed to load skin body cache")
                null
            }
        }

    // ── Timestamp ────────────────────────────────────────────────────────────

    private suspend fun saveLastUpdated(context: Context, worldFolder: String) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "last_updated").writeText(System.currentTimeMillis().toString())
            } catch (e: Exception) {
                Timber.w(e, "Failed to save last_updated")
            }
        }

    suspend fun loadLastUpdated(context: Context, worldFolder: String): Long? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "last_updated")
                if (!file.exists()) return@withContext null
                file.readText().trim().toLongOrNull()
            } catch (e: Exception) {
                Timber.w(e, "Failed to load last_updated")
                null
            }
        }

    // ── Pets ─────────────────────────────────────────────────────────────────

    suspend fun savePets(context: Context, worldFolder: String, pets: List<PetData>) =
        withContext(Dispatchers.IO) {
            try {
                val dir = worldDir(context, worldFolder)
                dir.mkdirs()
                File(dir, "pets.json").writeText(json.encodeToString(pets))
                saveLastUpdated(context, worldFolder)
            } catch (e: Exception) {
                Timber.w(e, "Failed to save pets cache")
            }
        }

    suspend fun loadPets(context: Context, worldFolder: String): List<PetData>? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(worldDir(context, worldFolder), "pets.json")
                if (!file.exists()) return@withContext null
                json.decodeFromString<List<PetData>>(file.readText())
            } catch (e: Exception) {
                Timber.w(e, "Failed to load pets cache")
                null
            }
        }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    suspend fun deleteWorld(context: Context, worldFolder: String) = withContext(Dispatchers.IO) {
        try {
            worldDir(context, worldFolder).deleteRecursively()
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete world cache")
        }
    }

    suspend fun deleteAll(context: Context) = withContext(Dispatchers.IO) {
        try {
            connectDir(context).deleteRecursively()
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete all cache")
        }
    }
}
