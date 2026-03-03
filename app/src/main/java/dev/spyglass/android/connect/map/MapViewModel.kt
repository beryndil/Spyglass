package dev.spyglass.android.connect.map

import dev.spyglass.android.connect.ConnectViewModel
import dev.spyglass.android.connect.MapRenderPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * State holder for the map screen.
 * Requests map tiles as viewport changes, tracks current dimension.
 */
class MapState(
    private val viewModel: ConnectViewModel,
) {
    val mapData: StateFlow<MapRenderPayload?> = viewModel.mapTiles

    var currentDimension: String = "overworld"
        private set

    fun requestTiles(centerX: Int, centerZ: Int, radius: Int = 8) {
        viewModel.requestMap(centerX, centerZ, radius, currentDimension)
    }

    fun switchDimension(dimension: String) {
        currentDimension = dimension
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
}
