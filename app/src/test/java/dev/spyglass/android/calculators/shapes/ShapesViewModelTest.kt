package dev.spyglass.android.calculators.shapes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShapesViewModelTest {

    private lateinit var vm: ShapesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = ShapesViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Await until state matches [predicate] (recalc runs on Dispatchers.Default). */
    private fun awaitState(predicate: (ShapesState) -> Boolean): ShapesState = runBlocking {
        withTimeout(2000) { vm.state.first(predicate) }
    }

    @Test
    fun circle_radius5_hasLayerAtZero() {
        vm.setShape(ShapeType.CIRCLE)
        vm.setRadius("5")

        val s = awaitState { it.layers.size == 1 && it.layers.containsKey(0) }
        assertTrue(s.totalBlocks > 0)
    }

    @Test
    fun sphere_radius3_symmetricLayers() {
        vm.setShape(ShapeType.SPHERE)
        vm.setRadius("3")

        val s = awaitState { it.layers.keys.any { k -> k < 0 } }
        assertTrue("Should have positive Y layers", s.layers.keys.any { it > 0 })
        for (y in s.layers.keys.filter { it > 0 }) {
            val posCount = s.layers[y]?.size ?: 0
            val negCount = s.layers[-y]?.size ?: 0
            assertEquals("Layer $y and ${-y} should be symmetric", posCount, negCount)
        }
    }

    @Test
    fun dome_radius3_onlyNonNegativeLayers() {
        vm.setShape(ShapeType.DOME)
        vm.setRadius("3")

        val s = awaitState { it.layers.isNotEmpty() && it.layers.keys.all { k -> k >= 0 } }
        assertTrue("Should have y=0 layer", s.layers.containsKey(0))
    }

    @Test
    fun cylinder_radius3_height5_exactly5Layers() {
        vm.setShape(ShapeType.CYLINDER)
        vm.setRadius("3")
        vm.setHeight("5")

        val s = awaitState { it.layers.size == 5 }
        for (y in 0 until 5) {
            assertTrue("Missing layer y=$y", s.layers.containsKey(y))
        }
    }

    @Test
    fun invalidRadius_zero_noLayers() {
        vm.setShape(ShapeType.CIRCLE)
        vm.setRadius("0")

        val s = awaitState { it.layers.isEmpty() }
        assertTrue(s.layers.isEmpty())
        assertEquals(0, s.totalBlocks)
    }

    @Test
    fun invalidRadius_empty_noLayers() {
        vm.setShape(ShapeType.CIRCLE)
        vm.setRadius("")

        val s = awaitState { it.layers.isEmpty() }
        assertTrue(s.layers.isEmpty())
        assertEquals(0, s.totalBlocks)
    }

    @Test
    fun dome_hasFewerBlocksThanSphere() {
        vm.setShape(ShapeType.SPHERE)
        vm.setRadius("5")
        val sphereState = awaitState { it.layers.keys.any { k -> k < 0 } }
        val sphereBlocks = sphereState.totalBlocks

        vm.setShape(ShapeType.DOME)
        val domeState = awaitState { it.layers.isNotEmpty() && it.layers.keys.all { k -> k >= 0 } }
        val domeBlocks = domeState.totalBlocks

        assertTrue("Dome ($domeBlocks) should have fewer blocks than sphere ($sphereBlocks)",
            domeBlocks < sphereBlocks)
    }

    @Test
    fun circle_totalBlocksEqualsLayerSize() {
        vm.setShape(ShapeType.CIRCLE)
        vm.setRadius("4")

        val s = awaitState { it.layers.size == 1 && it.layers.containsKey(0) }
        assertEquals(s.layers[0]!!.size, s.totalBlocks)
    }
}
