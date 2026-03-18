package dev.spyglass.android.connect.map

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.spyglass.android.connect.MapRenderPayload
import dev.spyglass.android.connect.MapTile
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory tile accumulator + bitmap cache for the world map.
 * Tiles are keyed by packed (chunkX, chunkZ) per dimension.
 * Bitmaps are decoded on merge (not on draw) and held in an LRU cache.
 */
class MapTileCache {

    /** Raw tile data keyed by packed coords, per dimension. */
    private val tilesByDimension = ConcurrentHashMap<String, ConcurrentHashMap<Long, MapTile>>()

    /** Pre-decoded bitmaps keyed by "chunkX:chunkZ:dim". 500-entry cap. */
    private val bitmapCache = LruCache<String, ImageBitmap>(500)

    /** Per-dimension player position from the most recent payload. */
    private val playerPosition = ConcurrentHashMap<String, Pair<Double, Double>>()

    /** Per-dimension loaded bounds (min/max chunk coords). */
    private val _loadedBounds = ConcurrentHashMap<String, TileBounds>()
    val loadedBounds: Map<String, TileBounds> get() = _loadedBounds

    data class TileBounds(
        val minChunkX: Int,
        val maxChunkX: Int,
        val minChunkZ: Int,
        val maxChunkZ: Int,
    )

    data class CachedTile(
        val tile: MapTile,
        val bitmap: ImageBitmap?,
    )

    /** Merge an incoming batch of tiles. Decodes bitmaps eagerly. */
    fun mergeTiles(payload: MapRenderPayload) {
        if (payload.tiles.isEmpty()) return

        val dim = payload.tiles.first().dimension
        val dimTiles = tilesByDimension.getOrPut(dim) { ConcurrentHashMap() }

        playerPosition[dim] = payload.playerX to payload.playerZ

        var minX = _loadedBounds[dim]?.minChunkX ?: Int.MAX_VALUE
        var maxX = _loadedBounds[dim]?.maxChunkX ?: Int.MIN_VALUE
        var minZ = _loadedBounds[dim]?.minChunkZ ?: Int.MAX_VALUE
        var maxZ = _loadedBounds[dim]?.maxChunkZ ?: Int.MIN_VALUE

        for (tile in payload.tiles) {
            val key = packKey(tile.chunkX, tile.chunkZ)
            dimTiles[key] = tile

            // Pre-decode bitmap
            val cacheKey = "${tile.chunkX}:${tile.chunkZ}:$dim"
            try {
                val bytes = Base64.decode(tile.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    bitmapCache.put(cacheKey, bitmap.asImageBitmap())
                }
            } catch (_: Exception) {
                // Skip corrupt tile
            }

            minX = minOf(minX, tile.chunkX)
            maxX = maxOf(maxX, tile.chunkX)
            minZ = minOf(minZ, tile.chunkZ)
            maxZ = maxOf(maxZ, tile.chunkZ)
        }

        _loadedBounds[dim] = TileBounds(minX, maxX, minZ, maxZ)
    }

    /** Get all cached tiles for a dimension with pre-decoded bitmaps. */
    fun getTilesForDimension(dim: String): Map<Long, CachedTile> {
        val dimTiles = tilesByDimension[dim] ?: return emptyMap()
        val result = HashMap<Long, CachedTile>(dimTiles.size)
        for ((key, tile) in dimTiles) {
            val cacheKey = "${tile.chunkX}:${tile.chunkZ}:$dim"
            result[key] = CachedTile(tile, bitmapCache.get(cacheKey))
        }
        return result
    }

    /** Get the player position for a dimension (from most recent payload). */
    fun getPlayerPosition(dim: String): Pair<Double, Double>? = playerPosition[dim]

    /** Get all raw tiles for a dimension (for disk persistence). */
    fun getRawTilesForDimension(dim: String): List<MapTile> {
        return tilesByDimension[dim]?.values?.toList() ?: emptyList()
    }

    /** Get all dimensions that have cached tiles. */
    fun getDimensions(): Set<String> = tilesByDimension.keys.toSet()

    /** Clear everything (e.g. on world change). */
    fun clearAll() {
        tilesByDimension.clear()
        bitmapCache.evictAll()
        playerPosition.clear()
        _loadedBounds.clear()
    }

    /** Number of tiles cached for a dimension. */
    fun tileCount(dim: String): Int = tilesByDimension[dim]?.size ?: 0

    companion object {
        fun packKey(chunkX: Int, chunkZ: Int): Long =
            (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)
    }
}
