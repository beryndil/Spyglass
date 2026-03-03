package dev.spyglass.android.browse.search

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseTabMappingTest {

    @Test fun block()       = assertEquals(0, browseTabForType("Block"))
    @Test fun item()        = assertEquals(1, browseTabForType("Item"))
    @Test fun recipe()      = assertEquals(2, browseTabForType("Recipe"))
    @Test fun mob()         = assertEquals(3, browseTabForType("Mob"))
    @Test fun trade()       = assertEquals(4, browseTabForType("Trade"))
    @Test fun biome()       = assertEquals(5, browseTabForType("Biome"))
    @Test fun structure()   = assertEquals(6, browseTabForType("Structure"))
    @Test fun enchantment() = assertEquals(7, browseTabForType("Enchantment"))
    @Test fun potion()      = assertEquals(8, browseTabForType("Potion"))
    @Test fun advancement() = assertEquals(9, browseTabForType("Advancement"))
    @Test fun command()     = assertEquals(10, browseTabForType("Command"))

    @Test
    fun unknownType_defaultsToZero() {
        assertEquals(0, browseTabForType("Nonsense"))
        assertEquals(0, browseTabForType(""))
    }
}
