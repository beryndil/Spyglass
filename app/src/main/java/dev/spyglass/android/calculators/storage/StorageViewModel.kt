package dev.spyglass.android.calculators.storage

import androidx.lifecycle.ViewModel
import dev.spyglass.android.core.parser.ItemQuantityParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CompressibleItem(val name: String, val blockName: String, val ratio: Int)

data class StorageState(
    val input: String = "",
    val selectedItemIndex: Int = 0,
    val result: StorageResult? = null,
)

data class StorageResult(
    val total: Long,
    val stacks: Long, val stackRem: Long,
    val singleChest: Long, val singleRem: Long,
    val doubleChest: Long, val doubleRem: Long,
    val shulker: Long, val shulkerRem: Long,
    val blocks: Long?, val blockRem: Long?,
)

class StorageViewModel : ViewModel() {

    val items: List<CompressibleItem> = listOf(
        CompressibleItem("(none)", "", 0),
        CompressibleItem("Iron Ingot",       "Iron Block",        9),
        CompressibleItem("Raw Iron",         "Raw Iron Block",    9),
        CompressibleItem("Gold Ingot",       "Gold Block",        9),
        CompressibleItem("Raw Gold",         "Raw Gold Block",    9),
        CompressibleItem("Copper Ingot",     "Copper Block",      9),
        CompressibleItem("Diamond",          "Diamond Block",     9),
        CompressibleItem("Emerald",          "Emerald Block",     9),
        CompressibleItem("Netherite Ingot",  "Netherite Block",   9),
        CompressibleItem("Coal",             "Coal Block",        9),
        CompressibleItem("Redstone Dust",    "Redstone Block",    9),
        CompressibleItem("Lapis Lazuli",     "Lapis Block",       9),
        CompressibleItem("Wheat",            "Hay Bale",          9),
        CompressibleItem("Bone",             "Bone Block",        9),
        CompressibleItem("Slimeball",        "Slime Block",       9),
        CompressibleItem("Melon Slice",      "Melon",             9),
        CompressibleItem("Amethyst Shard",   "Amethyst Block",    4),
        CompressibleItem("Nether Quartz",    "Quartz Block",      4),
        CompressibleItem("Honeycomb",        "Honeycomb Block",   4),
        CompressibleItem("Snowball",         "Snow Block",        4),
    )

    private val _state = MutableStateFlow(StorageState())
    val state: StateFlow<StorageState> = _state.asStateFlow()

    fun setInput(v: String)    { _state.value = _state.value.copy(input = v); recalc() }
    fun setItem(idx: Int)      { _state.value = _state.value.copy(selectedItemIndex = idx); recalc() }

    private fun recalc() {
        val s = _state.value
        val total = ItemQuantityParser.parse(s.input) ?: return let { _state.value = s.copy(result = null) }
        val item  = items[s.selectedItemIndex]
        val (blocks, blockRem) = if (item.ratio > 0) Pair(total / item.ratio, total % item.ratio) else Pair(null, null)

        _state.value = s.copy(result = StorageResult(
            total       = total,
            stacks      = total / 64,   stackRem    = total % 64,
            singleChest = total / 1728, singleRem   = total % 1728,
            doubleChest = total / 3456, doubleRem   = total % 3456,
            shulker     = total / 1728, shulkerRem  = total % 1728,
            blocks      = blocks,       blockRem    = blockRem,
        ))
    }
}
