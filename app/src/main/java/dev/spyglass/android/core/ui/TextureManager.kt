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

    /** LRU bitmap cache (~100 entries) for scroll performance. */
    private val bitmapCache = LruCache<String, Bitmap>(100)

    /** Stored after [init] so [resolve] doesn't need a Context. */
    private var textureDirPath: File? = null

    /** Stored after [init] for resolving bundled drawable names. */
    private var appContext: Context? = null

    /** Cache for [Resources.getIdentifier] lookups (filename → R.drawable.xxx). */
    private val resIdCache = mutableMapOf<String, Int>()

    private fun textureDir(context: Context) = File(context.filesDir, "textures")

    /** Call once at app startup to set initial state. */
    fun init(context: Context) {
        val dir = textureDir(context)
        textureDirPath = dir
        appContext = context.applicationContext
        _state.value = if (dir.exists() && (dir.listFiles()?.isNotEmpty() == true))
            TextureState.DOWNLOADED else TextureState.NOT_DOWNLOADED
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

            // Extract
            var extracted = 0
            ZipInputStream(zipBytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.substringAfterLast('/')
                        if (name.isNotEmpty()) {
                            File(dir, name).outputStream().use { out -> zip.copyTo(out) }
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
