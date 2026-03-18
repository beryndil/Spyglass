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
 * Owns the tile cache, tracks viewport, requests new tiles at edges.
 */
class MapState(
    private val viewModel: ConnectViewModel,
    private val scope: CoroutineScope,
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

    private var viewportDebounceJob: Job? = null
    private var pendingRequestCenter: Pair<Int, Int>? = null

    /** Merge an incoming tile batch into the cache. */
    fun mergeTiles(payload: MapRenderPayload) {
        tileCache.mergeTiles(payload)
        _tileRevision.value++
        _isLoading.value = false
        pendingRequestCenter = null
    }

    fun requestTiles(centerX: Int, centerZ: Int, radius: Int = 8) {
        _isLoading.value = true
        viewModel.requestMap(centerX, centerZ, radius, currentDimension)
    }

    fun switchDimension(dimension: String) {
        currentDimension = dimension
        // If we already have tiles for this dimension, don't request again
        if (tileCache.tileCount(dimension) > 0) {
            _tileRevision.value++ // trigger recompose with cached data
            return
        }
        val player = viewModel.playerData.value
        val cx = player?.posX?.toInt() ?: 0
        val cz = player?.posZ?.toInt() ?: 0
        requestTiles(cx, cz)
    }

    fun requestAroundPlayer() {
        val player = viewModel.playerData.value
        val cx = player?.posX?.toInt() ?: 0
        val cz = player?.posZ?.toInt() ?: 0
        currentDimension = player?.dimension ?: "overworld"
        requestTiles(cx, cz)
    }

    /**
     * Called after each gesture/pan. If the viewport is within 2 chunks of the
     * loaded boundary, debounce 300ms then request more tiles.
     */
    fun onViewportChanged(viewCenterChunkX: Int, viewCenterChunkZ: Int, visibleRadiusChunks: Int) {
        val bounds = tileCache.loadedBounds[currentDimension] ?: return
        if (_isLoading.value) return // already loading

        val margin = 2
        val needsMore = viewCenterChunkX - visibleRadiusChunks <= bounds.minChunkX + margin ||
            viewCenterChunkX + visibleRadiusChunks >= bounds.maxChunkX - margin ||
            viewCenterChunkZ - visibleRadiusChunks <= bounds.minChunkZ + margin ||
            viewCenterChunkZ + visibleRadiusChunks >= bounds.maxChunkZ - margin

        if (!needsMore) return

        val requestCenter = viewCenterChunkX to viewCenterChunkZ
        if (requestCenter == pendingRequestCenter) return

        viewportDebounceJob?.cancel()
        viewportDebounceJob = scope.launch {
            delay(300)
            pendingRequestCenter = requestCenter
            requestTiles(viewCenterChunkX * 16, viewCenterChunkZ * 16)
        }
    }

    /** Clear tile cache (e.g. on world change). */
    fun clearAll() {
        tileCache.clearAll()
        _tileRevision.value++
        _isLoading.value = false
        pendingRequestCenter = null
    }
}
