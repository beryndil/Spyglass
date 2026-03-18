package dev.spyglass.android.connect.map

import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.MapRenderPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * State holder for the map screen.
 * Owns the tile cache, requests tiles around the player's physical position.
 * Panning only shows cached tiles — new tiles are only fetched when the player moves.
 */
class MapState(
    private val viewModel: ConnectViewModel,
    private val scope: CoroutineScope? = null,
) {
    val tileCache = MapTileCache()

    /** Incremented on every mergeTiles() to trigger Canvas recompose. */
    private val _tileRevision = MutableStateFlow(0)
    val tileRevision: StateFlow<Int> = _tileRevision

    /** True when a tile request is in-flight. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var currentDimension: String = "overworld"
        private set

    /** Last chunk coords we requested tiles for — avoids duplicate requests. */
    private var lastRequestedChunk: Pair<Int, Int>? = null

    /** Loading timeout job — auto-clears isLoading after 15s as a safety net. */
    private var loadingTimeoutJob: Job? = null

    /** Background bitmap decode job — cancelled on clearAll or new batch. */
    private var decodeJob: Job? = null

    /** Merge an incoming tile batch into the cache. Decodes bitmaps off-thread. */
    fun mergeTiles(payload: MapRenderPayload) {
        loadingTimeoutJob?.cancel()
        decodeJob?.cancel()
        if (payload.tiles.isEmpty()) {
            _tileRevision.value++
            _isLoading.value = false
            return
        }
        // Fast: store metadata on main thread
        tileCache.storeTiles(payload)
        val dim = payload.tiles.first().dimension
        // Decode ALL tiles for this dimension (skips already-decoded ones).
        // This ensures tiles from a cancelled previous batch don't stay blank.
        val allTiles = tileCache.getRawTilesForDimension(dim)
        decodeJob = scope?.launch {
            tileCache.decodeBitmaps(allTiles, dim) {
                _tileRevision.value++
            }
            _isLoading.value = false
        }
    }

    /**
     * Request tiles around the player's current position if we haven't
     * already loaded this chunk area. Called when player data updates.
     */
    fun onPlayerMoved(posX: Double, posZ: Double, dimension: String) {
        if (!viewModel.connectionState.value.isConnected) return

        val chunkX = posX.toInt() shr 4
        val chunkZ = posZ.toInt() shr 4
        val chunkCoord = chunkX to chunkZ

        // Only request if the player moved to a new chunk or changed dimension
        if (chunkCoord == lastRequestedChunk && dimension == currentDimension) return

        currentDimension = dimension
        lastRequestedChunk = chunkCoord
        setLoadingWithTimeout()
        viewModel.requestMap(posX.toInt(), posZ.toInt(), 8, dimension)
    }

    fun switchDimension(dimension: String) {
        currentDimension = dimension
        // If we already have tiles for this dimension, just show them
        if (tileCache.tileCount(dimension) > 0) {
            _isLoading.value = false
            _tileRevision.value++
            return
        }
        if (!viewModel.connectionState.value.isConnected) return
        val player = viewModel.playerData.value
        val cx = player?.posX?.toInt() ?: 0
        val cz = player?.posZ?.toInt() ?: 0
        lastRequestedChunk = (cx shr 4) to (cz shr 4)
        setLoadingWithTimeout()
        viewModel.requestMap(cx, cz, 8, dimension)
    }

    fun requestAroundPlayer() {
        if (!viewModel.connectionState.value.isConnected) return
        val player = viewModel.playerData.value
        val cx = player?.posX?.toInt() ?: 0
        val cz = player?.posZ?.toInt() ?: 0
        currentDimension = player?.dimension ?: "overworld"
        lastRequestedChunk = (cx shr 4) to (cz shr 4)
        setLoadingWithTimeout()
        viewModel.requestMap(cx, cz, 8, currentDimension)
    }

    /** Clear tile cache (e.g. on world change). */
    fun clearAll() {
        loadingTimeoutJob?.cancel()
        decodeJob?.cancel()
        tileCache.clearAll()
        _tileRevision.value++
        _isLoading.value = false
        lastRequestedChunk = null
    }

    /** Set loading=true with a 15s timeout safety net. */
    private fun setLoadingWithTimeout() {
        _isLoading.value = true
        loadingTimeoutJob?.cancel()
        loadingTimeoutJob = scope?.launch {
            delay(15_000)
            _isLoading.value = false
        }
    }
}
