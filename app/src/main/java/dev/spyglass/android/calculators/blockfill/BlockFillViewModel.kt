package dev.spyglass.android.calculators.blockfill

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockFillState(
    val widthInput:  String = "",
    val lengthInput: String = "",
    val heightInput: String = "",
    val widthChunks:  Boolean = false,
    val lengthChunks: Boolean = false,
    val result: BlockFillResult? = null,
)

data class BlockFillResult(
    val totalBlocks: Long,
    val stacks:      Long, val stackRem:  Long,
    val singleChest: Long, val singleRem: Long,
    val doubleChest: Long, val doubleRem: Long,
    val shulker:     Long, val shulkerRem:Long,
)

class BlockFillViewModel : ViewModel() {
    private val _state = MutableStateFlow(BlockFillState())
    val state: StateFlow<BlockFillState> = _state.asStateFlow()

    fun setWidth(v: String)  { _state.value = _state.value.copy(widthInput  = v); recalc() }
    fun setLength(v: String) { _state.value = _state.value.copy(lengthInput = v); recalc() }
    fun setHeight(v: String) { _state.value = _state.value.copy(heightInput = v); recalc() }
    fun toggleWidthChunks()  { _state.value = _state.value.copy(widthChunks  = !_state.value.widthChunks);  recalc() }
    fun toggleLengthChunks() { _state.value = _state.value.copy(lengthChunks = !_state.value.lengthChunks); recalc() }

    private fun recalc() {
        val s = _state.value
        val w = (s.widthInput.toLongOrNull()  ?: return let { _state.value = s.copy(result = null) }) *
                (if (s.widthChunks)  16L else 1L)
        val l = (s.lengthInput.toLongOrNull() ?: return let { _state.value = s.copy(result = null) }) *
                (if (s.lengthChunks) 16L else 1L)
        val h = s.heightInput.toLongOrNull()  ?: return let { _state.value = s.copy(result = null) }
        if (w <= 0 || l <= 0 || h <= 0) { _state.value = s.copy(result = null); return }

        val total = w * l * h
        _state.value = s.copy(result = BlockFillResult(
            totalBlocks = total,
            stacks      = total / 64,       stackRem   = total % 64,
            singleChest = total / 1728,     singleRem  = total % 1728,
            doubleChest = total / 3456,     doubleRem  = total % 3456,
            shulker     = total / 1728,     shulkerRem = total % 1728,
        ))
    }
}
