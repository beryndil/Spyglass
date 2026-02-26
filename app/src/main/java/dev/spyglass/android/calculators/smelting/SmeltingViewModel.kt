package dev.spyglass.android.calculators.smelting

import androidx.lifecycle.ViewModel
import dev.spyglass.android.core.parser.ItemQuantityParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.ceil

data class Fuel(
    val name: String,
    val smeltsPerUnit: Double,   // items smelted per one unit
    val stackSize: Int,          // how many fit in a stack (1 for non-stackable)
)

enum class Efficiency { HIGH, MID, LOW }

data class FuelResult(val fuel: Fuel, val units: Long, val unused: Double, val efficiency: Efficiency)

data class SmeltingState(
    val input: String = "",
    val results: List<FuelResult> = emptyList(),
    val parseError: Boolean = false,
)

class SmeltingViewModel : ViewModel() {

    val fuels: List<Fuel> = listOf(
        Fuel("Lava Bucket",   100.0,  1),
        Fuel("Coal Block",     80.0, 64),
        Fuel("Dried Kelp Block", 20.0, 64),
        Fuel("Blaze Rod",      12.0, 64),
        Fuel("Coal",            8.0, 64),
        Fuel("Charcoal",        8.0, 64),
        Fuel("Wood Log",        1.5, 64),
        Fuel("Wood Plank",      1.5, 64),
        Fuel("Wood Slab",       0.75, 64),
        Fuel("Stick",           0.5, 64),
        Fuel("Bamboo",          0.25, 64),
    )

    private val _state = MutableStateFlow(SmeltingState())
    val state: StateFlow<SmeltingState> = _state.asStateFlow()

    fun setInput(v: String) {
        _state.value = _state.value.copy(input = v)
        recalc(v)
    }

    private fun recalc(raw: String) {
        val count = ItemQuantityParser.parse(raw)
        if (count == null || count <= 0L) {
            _state.value = _state.value.copy(results = emptyList(), parseError = raw.isNotBlank())
            return
        }
        val results = fuels.map { fuel ->
            val unitsExact = count / fuel.smeltsPerUnit
            val units      = ceil(unitsExact).toLong()
            val unused     = units * fuel.smeltsPerUnit - count
            val efficiency = when {
                fuel.smeltsPerUnit >= 20.0 -> Efficiency.HIGH
                fuel.smeltsPerUnit >= 8.0  -> Efficiency.MID
                else                       -> Efficiency.LOW
            }
            FuelResult(fuel, units, unused, efficiency)
        }
        _state.value = _state.value.copy(results = results, parseError = false)
    }
}
