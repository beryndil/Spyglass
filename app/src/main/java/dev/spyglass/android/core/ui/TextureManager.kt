package dev.spyglass.android.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Manages downloadable texture PNGs stored in `filesDir/textures/`.
 * Provides resolution from filename → [SpyglassIcon.FileBitmap] and
 * handles downloading/extracting the texture zip from GitHub.
 */
object TextureManager {

    enum class TextureState { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED }

    private val _state = MutableStateFlow(TextureState.NOT_DOWNLOADED)
    val state: StateFlow<TextureState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** Download progress 0..1. Only meaningful when state == DOWNLOADING. */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    /** LRU bitmap cache (8 MB) for scroll performance. */
    private val bitmapCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap) = bitmap.byteCount
    }

    /** Stored after [init] so [resolve] doesn't need a Context. */
    private var textureDirPath: File? = null

    /** Stored after [init] for resolving bundled drawable names. */
    private var appContext: Context? = null

    /** Cache for [Resources.getIdentifier] lookups (filename → R.drawable.xxx). */
    private val resIdCache = mutableMapOf<String, Int>()

    private fun textureDir(context: Context) = File(context.filesDir, "textures")

    /** Call once at app startup to set initial state. Extracts bundled textures on app update. */
    fun init(context: Context) {
        val dir = textureDir(context)
        textureDirPath = dir
        appContext = context.applicationContext
        val hasTextures = dir.exists() && (dir.listFiles()?.isNotEmpty() == true)
        _state.value = if (hasTextures) TextureState.DOWNLOADED else TextureState.NOT_DOWNLOADED

        // Extract bundled textures if app version changed (or first launch)
        val versionFile = File(dir, ".app_version")
        val currentVersion = dev.spyglass.android.BuildConfig.VERSION_CODE.toString()
        val lastVersion = if (versionFile.exists()) versionFile.readText().trim() else ""
        if (lastVersion != currentVersion) {
            extractMissingBundledTextures(context)
            versionFile.parentFile?.mkdirs()
            versionFile.writeText(currentVersion)
        }
    }

    /**
     * Extracts only missing PNGs from the bundled `textures.zip` into `filesDir/textures/`.
     * Skips files that already exist on disk (preserving newer synced versions).
     */
    private fun extractMissingBundledTextures(context: Context) {
        try {
            val dir = textureDir(context)
            dir.mkdirs()

            val maxEntrySize = 1_024_000L
            var extracted = 0
            ZipInputStream(context.assets.open("minecraft/textures.zip")).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.substringAfterLast('/')
                        val target = File(dir, name)
                        if (name.isNotEmpty() && !target.exists()) {
                            var entryBytes = 0L
                            val buf = ByteArray(8192)
                            target.outputStream().use { out ->
                                var n: Int
                                while (zip.read(buf).also { n = it } != -1) {
                                    entryBytes += n
                                    if (entryBytes > maxEntrySize) break
                                    out.write(buf, 0, n)
                                }
                            }
                            if (entryBytes > maxEntrySize) {
                                target.delete()
                            } else {
                                extracted++
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            if (extracted > 0) {
                bitmapCache.evictAll()
                _state.value = TextureState.DOWNLOADED
                Timber.d("TextureManager: extracted %d missing bundled textures", extracted)
            }
        } catch (e: Exception) {
            Timber.w(e, "TextureManager: failed to extract bundled textures")
        }
    }

    /**
     * Returns a [SpyglassIcon.FileBitmap] if the texture file exists on disk,
     * or null if textures aren't downloaded or [init] hasn't been called.
     */
    fun resolve(filename: String): SpyglassIcon.FileBitmap? {
        if (_state.value != TextureState.DOWNLOADED) return null
        val dir = textureDirPath ?: return null
        val file = File(dir, filename)
        return if (file.exists()) SpyglassIcon.FileBitmap(file) else null
    }

    /**
     * Returns a downloaded texture if available, otherwise the bundled drawable.
     * This is the primary lookup used by all texture objects.
     */
    fun icon(filename: String, @androidx.annotation.DrawableRes fallbackRes: Int): SpyglassIcon {
        return resolve("$filename.png") ?: SpyglassIcon.Drawable(fallbackRes)
    }

    /**
     * Resolves a texture by filename: tries downloaded override first,
     * falls back to the bundled drawable with the same name.
     * [filename] is the bare name without extension (e.g. "block_stone").
     */
    fun resolveOrBundled(filename: String): SpyglassIcon? {
        resolve("$filename.png")?.let { return it }
        val ctx = appContext ?: return null
        val resId = resIdCache.getOrPut(filename) {
            ctx.resources.getIdentifier(filename, "drawable", ctx.packageName)
        }
        return if (resId != 0) SpyglassIcon.Drawable(resId) else null
    }

    /** Decode a bitmap from file, using the LRU cache. */
    fun getCachedBitmap(file: File): Bitmap? {
        val key = file.absolutePath
        bitmapCache.get(key)?.let { return it }
        return try {
            BitmapFactory.decodeFile(key)?.also { bitmapCache.put(key, it) }
        } catch (e: Exception) {
            Timber.w(e, "Failed to decode bitmap: %s", key)
            null
        }
    }

    /**
     * Downloads textures.zip from GitHub and extracts to filesDir/textures/.
     * Reports progress via [progress] StateFlow.
     */
    suspend fun download(context: Context) = withContext(Dispatchers.IO) {
        _state.value = TextureState.DOWNLOADING
        _progress.value = 0f
        try {
            val zipBytes = dev.spyglass.android.data.sync.GitHubDataClient.fetchBinaryFile("textures.zip")
            if (zipBytes == null) {
                Timber.w("TextureManager: failed to download textures.zip")
                _state.value = TextureState.NOT_DOWNLOADED
                return@withContext
            }

            val dir = textureDir(context)
            if (dir.exists()) dir.deleteRecursively()
            dir.mkdirs()

            // Count entries first for progress reporting
            val totalEntries = ZipInputStream(zipBytes.inputStream()).use { zip ->
                var count = 0
                while (zip.nextEntry != null) { count++; zip.closeEntry() }
                count
            }

            // Extract with size guards
            val maxEntrySize = 1_024_000L // 1 MB — textures are small PNGs
            val maxTotalSize = 50L * 1024 * 1024 // 50 MB — prevent zip bomb
            var extracted = 0
            var totalBytes = 0L
            ZipInputStream(zipBytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        if (entry.size > maxEntrySize) {
                            Timber.w("TextureManager: skipping oversized entry %s (%d bytes)", entry.name, entry.size)
                            zip.closeEntry(); extracted++; entry = zip.nextEntry; continue
                        }
                        val name = entry.name.substringAfterLast('/')
                        if (name.isNotEmpty()) {
                            val buf = ByteArray(8192)
                            var entryBytes = 0L
                            File(dir, name).outputStream().use { out ->
                                var n: Int
                                while (zip.read(buf).also { n = it } != -1) {
                                    entryBytes += n
                                    if (entryBytes > maxEntrySize) {
                                        Timber.w("TextureManager: entry %s exceeded size limit, skipping", name)
                                        break
                                    }
                                    out.write(buf, 0, n)
                                }
                            }
                            if (entryBytes > maxEntrySize) {
                                File(dir, name).delete()
                            } else {
                                totalBytes += entryBytes
                            }
                            if (totalBytes > maxTotalSize) {
                                Timber.w("TextureManager: total extraction exceeded %d MB, aborting", maxTotalSize / (1024 * 1024))
                                zip.closeEntry(); break
                            }
                        }
                    }
                    zip.closeEntry()
                    extracted++
                    _progress.value = extracted.toFloat() / totalEntries.coerceAtLeast(1)
                    entry = zip.nextEntry
                }
            }

            bitmapCache.evictAll()
            _state.value = TextureState.DOWNLOADED
            Timber.d("TextureManager: extracted %d textures", extracted)
        } catch (e: Exception) {
            Timber.e(e, "TextureManager: download failed")
            _state.value = TextureState.NOT_DOWNLOADED
        }
    }

    /** Evicts all bitmaps from the LRU cache (for critical memory pressure). */
    fun evictAllBitmaps() {
        bitmapCache.evictAll()
        Timber.d("TextureManager: evicted all cached bitmaps")
    }

    /** Trims the bitmap cache to half its current size. */
    fun trimBitmapCache() {
        bitmapCache.trimToSize(bitmapCache.maxSize() / 2)
        Timber.d("TextureManager: trimmed bitmap cache to half")
    }

    /** Deletes all downloaded textures. */
    suspend fun delete(context: Context) = withContext(Dispatchers.IO) {
        textureDir(context).deleteRecursively()
        bitmapCache.evictAll()
        _state.value = TextureState.NOT_DOWNLOADED
    }

    // ── Texture map loading ─────────────────────────────────────────────────

    @Serializable
    private data class TextureMapData(
        val blocks: Map<String, String> = emptyMap(),
        val items: Map<String, String> = emptyMap(),
    )

    private val mapJson = Json { ignoreUnknownKeys = true }

    /**
     * Loads `texture_map.json` and populates [BlockTextures] and [ItemTextures].
     * Tries synced copy in `filesDir/minecraft/` first, falls back to bundled asset.
     */
    fun loadTextureMaps(context: Context) {
        val raw = readSyncedOrBundled(context, "texture_map.json")
        if (raw == null) {
            Timber.w("TextureManager: no texture_map.json found")
            return
        }
        try {
            val data = mapJson.decodeFromString<TextureMapData>(raw)
            BlockTextures.loadMap(data.blocks)
            ItemTextures.loadMap(data.items)
            Timber.d("TextureManager: loaded texture map (blocks=%d, items=%d)",
                data.blocks.size, data.items.size)
        } catch (e: Exception) {
            Timber.e(e, "TextureManager: failed to parse texture_map.json")
        }
    }

    /** Reads a JSON file from synced internal storage, falling back to bundled assets. */
    private fun readSyncedOrBundled(context: Context, fileName: String): String? {
        // Try synced copy first
        val synced = File(context.filesDir, "minecraft/$fileName")
        if (synced.exists()) {
            return try { synced.readText() } catch (_: Exception) { null }
        }
        // Fall back to bundled asset
        return try {
            context.assets.open("minecraft/$fileName").bufferedReader().readText()
        } catch (_: Exception) { null }
    }
}
