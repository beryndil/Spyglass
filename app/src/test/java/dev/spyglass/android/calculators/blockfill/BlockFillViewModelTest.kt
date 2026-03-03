package dev.spyglass.android.calculators.blockfill

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BlockFillViewModelTest {

    private lateinit var vm: BlockFillViewModel

    @Before
    fun setUp() {
        vm = BlockFillViewModel()
    }

    @Test
    fun solidFill_10x10x10() {
        vm.setWidth("10")
        vm.setLength("10")
        vm.setHeight("10")

        val r = vm.state.value.result
        assertNotNull(r)
        r!!
        assertEquals(1000L, r.totalBlocks)
        assertEquals(15L, r.stacks)
        assertEquals(40L, r.stackRem)
    }

    @Test
    fun solidFill_chestsAndShulkers() {
        vm.setWidth("10")
        vm.setLength("10")
        vm.setHeight("10")

        val r = vm.state.value.result!!
        // 1000 / 1728 = 0 single chests, rem 1000
        assertEquals(0L, r.singleChest)
        assertEquals(1000L, r.singleRem)
        // 1000 / 3456 = 0 double chests, rem 1000
        assertEquals(0L, r.doubleChest)
        assertEquals(1000L, r.doubleRem)
        // shulker same as single chest (1728 capacity)
        assertEquals(0L, r.shulker)
        assertEquals(1000L, r.shulkerRem)
    }

    @Test
    fun hollowFill_10x10x10() {
        vm.setWidth("10")
        vm.setLength("10")
        vm.setHeight("10")
        vm.toggleHollow()

        val r = vm.state.value.result
        assertNotNull(r)
        // outer=1000, inner=8*8*8=512, total=488
        assertEquals(488L, r!!.totalBlocks)
    }

    @Test
    fun hollowTooSmall_2x2x2() {
        vm.setWidth("2")
        vm.setLength("2")
        vm.setHeight("2")
        vm.toggleHollow()

        val r = vm.state.value.result
        assertNotNull(r)
        // dims < 3 so hollow is ignored, solid 2x2x2=8
        assertEquals(8L, r!!.totalBlocks)
    }

    @Test
    fun chunkMultiplier() {
        vm.setWidth("1")
        vm.toggleWidthChunks()  // 1 chunk = 16 blocks
        vm.setLength("1")
        vm.setHeight("1")

        val r = vm.state.value.result
        assertNotNull(r)
        assertEquals(16L, r!!.totalBlocks)
    }

    @Test
    fun overflowProtection() {
        vm.setWidth("999999999")
        vm.setLength("999999999")
        vm.setHeight("999999999")

        val r = vm.state.value.result
        assertNull("Overflow should produce null result", r)
    }

    @Test
    fun zeroInput_producesNull() {
        vm.setWidth("0")
        vm.setLength("10")
        vm.setHeight("10")

        assertNull(vm.state.value.result)
    }

    @Test
    fun nonNumericInput_producesNull() {
        vm.setWidth("abc")
        vm.setLength("10")
        vm.setHeight("10")

        assertNull(vm.state.value.result)
    }

    @Test
    fun singleBlock() {
        vm.setWidth("1")
        vm.setLength("1")
        vm.setHeight("1")

        val r = vm.state.value.result
        assertNotNull(r)
        assertEquals(1L, r!!.totalBlocks)
        assertEquals(0L, r.stacks)
        assertEquals(1L, r.stackRem)
    }

    @Test
    fun largeValidFill() {
        vm.setWidth("64")
        vm.setLength("1")
        vm.setHeight("1")

        val r = vm.state.value.result!!
        assertEquals(64L, r.totalBlocks)
        assertEquals(1L, r.stacks)
        assertEquals(0L, r.stackRem)
    }
}
