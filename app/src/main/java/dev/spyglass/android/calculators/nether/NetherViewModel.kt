package dev.spyglass.android.calculators.nether

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedPortal(val name: String, val owX: Int, val owZ: Int, val netX: Int, val netZ: Int)

enum class NetherDimension { OVERWORLD, NETHER }

data class NetherState(
    val dimension: NetherDimension = NetherDimension.OVERWORLD,
    val xIn: String = "", val yIn: String = "", val zIn: String = "",
    val xOut: String = "", val yOut: String = "", val zOut: String = "",
    val facing: String = "",
    // Obsidian calc
    val obWidth: String = "2", val obHeight: String = "3",
    val obNoCorners: Int = 0, val obWithCorners: Int = 0,
    // Portals
    val savedPortals: List<SavedPortal> = emptyList(),
    val newPortalName: String = "",
)

class NetherViewModel : ViewModel() {
    private val _state = MutableStateFlow(NetherState())
    val state: StateFlow<NetherState> = _state.asStateFlow()

    fun setDimension(d: NetherDimension) { _state.value = _state.value.copy(dimension = d); convert() }
    fun setX(v: String) { _state.value = _state.value.copy(xIn = v); convert() }
    fun setY(v: String) { _state.value = _state.value.copy(yIn = v); convert() }
    fun setZ(v: String) { _state.value = _state.value.copy(zIn = v); convert() }

    fun pasteF3(text: String) {
        // Parse "XYZ: 100.5 / 64 / -200.3  Facing: NORTH"
        val xyzRegex  = Regex("""XYZ:\s*([-\d.]+)\s*/\s*([-\d.]+)\s*/\s*([-\d.]+)""", RegexOption.IGNORE_CASE)
        val facingReg = Regex("""Facing:\s*(\w+)""", RegexOption.IGNORE_CASE)
        xyzRegex.find(text)?.let { m ->
            _state.value = _state.value.copy(
                xIn = m.groupValues[1].toDoubleOrNull()?.toInt()?.toString() ?: _state.value.xIn,
                yIn = m.groupValues[2].toDoubleOrNull()?.toInt()?.toString() ?: _state.value.yIn,
                zIn = m.groupValues[3].toDoubleOrNull()?.toInt()?.toString() ?: _state.value.zIn,
                facing = facingReg.find(text)?.groupValues?.get(1)?.uppercase() ?: _state.value.facing,
            )
            convert()
        }
    }

    private fun convert() {
        val s = _state.value
        val x = s.xIn.toIntOrNull()
        val y = s.yIn.toIntOrNull()
        val z = s.zIn.toIntOrNull()
        val xOut = x?.let { if (s.dimension == NetherDimension.OVERWORLD) it / 8 else it * 8 }
        val zOut = z?.let { if (s.dimension == NetherDimension.OVERWORLD) it / 8 else it * 8 }
        _state.value = s.copy(
            xOut = xOut?.toString() ?: "",
            yOut = y?.toString() ?: "",
            zOut = zOut?.toString() ?: "",
        )
    }

    fun setObWidth(v: String)  { _state.value = _state.value.copy(obWidth = v);  calcObsidian() }
    fun setObHeight(v: String) { _state.value = _state.value.copy(obHeight = v); calcObsidian() }

    private fun calcObsidian() {
        val s = _state.value
        val w = s.obWidth.toIntOrNull()  ?: return
        val h = s.obHeight.toIntOrNull() ?: return
        if (w < 2 || h < 3) return
        _state.value = s.copy(obNoCorners = 2 * w + 2 * h, obWithCorners = 2 * w + 2 * h + 4)
    }

    fun setNewPortalName(v: String) { _state.value = _state.value.copy(newPortalName = v) }

    fun savePortal() {
        val s = _state.value
        val name = s.newPortalName.ifBlank { return }
        val owX = if (s.dimension == NetherDimension.OVERWORLD) s.xIn.toIntOrNull() ?: return else s.xOut.toIntOrNull() ?: return
        val owZ = if (s.dimension == NetherDimension.OVERWORLD) s.zIn.toIntOrNull() ?: return else s.zOut.toIntOrNull() ?: return
        val netX = if (s.dimension == NetherDimension.NETHER) s.xIn.toIntOrNull() ?: return else s.xOut.toIntOrNull() ?: return
        val netZ = if (s.dimension == NetherDimension.NETHER) s.zIn.toIntOrNull() ?: return else s.zOut.toIntOrNull() ?: return
        _state.value = s.copy(
            savedPortals = s.savedPortals + SavedPortal(name, owX, owZ, netX, netZ),
            newPortalName = "",
        )
    }

    fun deletePortal(p: SavedPortal) {
        _state.value = _state.value.copy(savedPortals = _state.value.savedPortals - p)
    }
}
