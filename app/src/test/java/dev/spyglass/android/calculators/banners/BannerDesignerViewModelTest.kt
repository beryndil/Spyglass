package dev.spyglass.android.calculators.banners

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BannerDesignerViewModelTest {

    private lateinit var vm: BannerDesignerViewModel

    @Before
    fun setUp() {
        vm = BannerDesignerViewModel()
    }

    @Test
    fun initialState_whiteBaseNoLayers() {
        val s = vm.state.value
        assertEquals(DyeColor.WHITE, s.baseColor)
        assertTrue(s.layers.isEmpty())
    }

    @Test
    fun setBaseColor_updatesState() {
        vm.setBaseColor(DyeColor.RED)
        assertEquals(DyeColor.RED, vm.state.value.baseColor)
    }

    @Test
    fun addLayer_appendsToList() {
        vm.setSelectedPattern(BannerPattern.CROSS)
        vm.setSelectedLayerColor(DyeColor.BLUE)
        vm.addLayer()

        val layers = vm.state.value.layers
        assertEquals(1, layers.size)
        assertEquals(BannerPattern.CROSS, layers[0].pattern)
        assertEquals(DyeColor.BLUE, layers[0].color)
    }

    @Test
    fun addLayer_maxSixLayers() {
        repeat(6) { vm.addLayer() }
        assertEquals(6, vm.state.value.layers.size)

        vm.addLayer() // 7th should be ignored
        assertEquals(6, vm.state.value.layers.size)
    }

    @Test
    fun removeLayer_removesCorrectIndex() {
        vm.setSelectedPattern(BannerPattern.CROSS)
        vm.addLayer()
        vm.setSelectedPattern(BannerPattern.BORDER)
        vm.addLayer()
        vm.setSelectedPattern(BannerPattern.GRADIENT)
        vm.addLayer()

        vm.removeLayer(1) // remove BORDER

        val layers = vm.state.value.layers
        assertEquals(2, layers.size)
        assertEquals(BannerPattern.CROSS, layers[0].pattern)
        assertEquals(BannerPattern.GRADIENT, layers[1].pattern)
    }

    @Test
    fun moveLayerUp_swapsCorrectly() {
        vm.setSelectedPattern(BannerPattern.CROSS)
        vm.addLayer()
        vm.setSelectedPattern(BannerPattern.BORDER)
        vm.addLayer()

        vm.moveLayerUp(1) // move BORDER up

        val layers = vm.state.value.layers
        assertEquals(BannerPattern.BORDER, layers[0].pattern)
        assertEquals(BannerPattern.CROSS, layers[1].pattern)
    }

    @Test
    fun moveLayerDown_swapsCorrectly() {
        vm.setSelectedPattern(BannerPattern.CROSS)
        vm.addLayer()
        vm.setSelectedPattern(BannerPattern.BORDER)
        vm.addLayer()

        vm.moveLayerDown(0) // move CROSS down

        val layers = vm.state.value.layers
        assertEquals(BannerPattern.BORDER, layers[0].pattern)
        assertEquals(BannerPattern.CROSS, layers[1].pattern)
    }

    @Test
    fun moveLayerUp_atZero_noChange() {
        vm.setSelectedPattern(BannerPattern.CROSS)
        vm.addLayer()
        vm.setSelectedPattern(BannerPattern.BORDER)
        vm.addLayer()

        vm.moveLayerUp(0) // no-op

        assertEquals(BannerPattern.CROSS, vm.state.value.layers[0].pattern)
        assertEquals(BannerPattern.BORDER, vm.state.value.layers[1].pattern)
    }

    @Test
    fun moveLayerDown_atEnd_noChange() {
        vm.setSelectedPattern(BannerPattern.CROSS)
        vm.addLayer()
        vm.setSelectedPattern(BannerPattern.BORDER)
        vm.addLayer()

        vm.moveLayerDown(1) // no-op

        assertEquals(BannerPattern.CROSS, vm.state.value.layers[0].pattern)
        assertEquals(BannerPattern.BORDER, vm.state.value.layers[1].pattern)
    }

    @Test
    fun clearDesign_resetsToDefaults() {
        vm.setBaseColor(DyeColor.RED)
        vm.setSelectedPattern(BannerPattern.CROSS)
        vm.addLayer()
        vm.addLayer()

        vm.clearDesign()

        val s = vm.state.value
        assertEquals(DyeColor.WHITE, s.baseColor)
        assertTrue(s.layers.isEmpty())
    }

    @Test
    fun removeLayer_invalidIndex_noChange() {
        vm.addLayer()
        vm.removeLayer(-1)
        vm.removeLayer(5)

        assertEquals(1, vm.state.value.layers.size)
    }
}
