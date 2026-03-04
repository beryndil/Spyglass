package dev.spyglass.android.calculators.banners

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BannerDesignerState(
    val baseColor: DyeColor = DyeColor.WHITE,
    val layers: List<BannerLayer> = emptyList(),
    val selectedPattern: BannerPattern = BannerPattern.STRIPE_TOP,
    val selectedLayerColor: DyeColor = DyeColor.BLACK,
    val selectedCategory: String = "basic",
)

class BannerDesignerViewModel : ViewModel() {
    private val _state = MutableStateFlow(BannerDesignerState())
    val state: StateFlow<BannerDesignerState> = _state.asStateFlow()

    fun setBaseColor(color: DyeColor) { _state.value = _state.value.copy(baseColor = color) }
    fun setSelectedPattern(pattern: BannerPattern) { _state.value = _state.value.copy(selectedPattern = pattern) }
    fun setSelectedLayerColor(color: DyeColor) { _state.value = _state.value.copy(selectedLayerColor = color) }
    fun setSelectedCategory(category: String) { _state.value = _state.value.copy(selectedCategory = category) }

    fun addLayer() {
        val s = _state.value
        if (s.layers.size >= 6) return
        _state.value = s.copy(layers = s.layers + BannerLayer(s.selectedPattern, s.selectedLayerColor))
    }

    fun removeLayer(index: Int) {
        val s = _state.value
        if (index !in s.layers.indices) return
        _state.value = s.copy(layers = s.layers.toMutableList().apply { removeAt(index) })
    }

    fun moveLayerUp(index: Int) {
        val s = _state.value
        if (index <= 0 || index !in s.layers.indices) return
        val list = s.layers.toMutableList()
        val tmp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = tmp
        _state.value = s.copy(layers = list)
    }

    fun moveLayerDown(index: Int) {
        val s = _state.value
        if (index < 0 || index >= s.layers.size - 1) return
        val list = s.layers.toMutableList()
        val tmp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = tmp
        _state.value = s.copy(layers = list)
    }

    fun clearDesign() {
        _state.value = BannerDesignerState()
    }
}
