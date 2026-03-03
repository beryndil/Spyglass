package dev.spyglass.android.data.sync

import org.junit.Assert.*
import org.junit.Test

class DataManifestTest {

    @Test
    fun jsonRoundTrip() {
        val original = DataManifest(
            version = 1,
            blocks = 100, mobs = 200, biomes = 300,
            enchants = 400, potions = 500, trades = 600,
            recipes = 700, structures = 800, items = 900,
            advancements = 1000, commands = 1100,
            textures = 1200, textureMap = 1300, news = 1400,
            checksums = mapOf("blocks.json" to "abc123"),
        )
        val json = DataManifest.toJson(original)
        val restored = DataManifest.fromJson(json)
        assertEquals(original, restored)
    }

    @Test
    fun unknownKeysIgnored() {
        val json = """{"blocks":42,"unknown_field":"hello","extra":999}"""
        val m = DataManifest.fromJson(json)
        assertEquals(42L, m.blocks)
        // should not crash
    }

    @Test
    fun changedTables_detectsDifferences() {
        val a = DataManifest(blocks = 1, mobs = 1)
        val b = DataManifest(blocks = 2, mobs = 3)
        val changed = a.changedTables(b)
        assertTrue("blocks" in changed)
        assertTrue("mobs" in changed)
        assertEquals(2, changed.size)
    }

    @Test
    fun changedTables_identical() {
        val m = DataManifest(blocks = 5, mobs = 5)
        assertTrue(m.changedTables(m).isEmpty())
    }

    @Test
    fun withVersion_updatesCorrectField() {
        val m = DataManifest()
        val updated = m.withVersion("blocks", 42)
        assertEquals(42L, updated.blocks)
        assertEquals(0L, updated.mobs)
    }

    @Test
    fun withVersion_unknownTable_returnsUnchanged() {
        val m = DataManifest(blocks = 10)
        val same = m.withVersion("nonexistent", 999)
        assertEquals(m, same)
    }

    @Test
    fun versionOf_knownTables() {
        val m = DataManifest(
            blocks = 10, mobs = 20, biomes = 30,
            enchants = 40, potions = 50, trades = 60,
            recipes = 70, structures = 80, items = 90,
            advancements = 100, commands = 110,
        )
        assertEquals(10L, m.versionOf("blocks"))
        assertEquals(20L, m.versionOf("mobs"))
        assertEquals(30L, m.versionOf("biomes"))
        assertEquals(40L, m.versionOf("enchants"))
        assertEquals(50L, m.versionOf("potions"))
        assertEquals(60L, m.versionOf("trades"))
        assertEquals(70L, m.versionOf("recipes"))
        assertEquals(80L, m.versionOf("structures"))
        assertEquals(90L, m.versionOf("items"))
        assertEquals(100L, m.versionOf("advancements"))
        assertEquals(110L, m.versionOf("commands"))
    }

    @Test
    fun versionOf_unknownTable_returnsZero() {
        assertEquals(0L, DataManifest().versionOf("unknown"))
    }

    @Test
    fun effectiveVersion_returnsMax() {
        val m = DataManifest(blocks = 5, recipes = 100, commands = 50)
        assertEquals(100L, m.effectiveVersion)
    }

    @Test
    fun effectiveVersion_allZero() {
        assertEquals(0L, DataManifest().effectiveVersion)
    }

    @Test
    fun hasTextureUpdate() {
        val remote = DataManifest(textures = 10)
        val local = DataManifest(textures = 5)
        assertTrue(remote.hasTextureUpdate(local))
        assertFalse(local.hasTextureUpdate(remote))
        assertFalse(remote.hasTextureUpdate(remote))
    }

    @Test
    fun hasTextureMapUpdate() {
        val remote = DataManifest(textureMap = 10)
        val local = DataManifest(textureMap = 5)
        assertTrue(remote.hasTextureMapUpdate(local))
        assertFalse(local.hasTextureMapUpdate(remote))
    }

    @Test
    fun hasNewsUpdate() {
        val remote = DataManifest(news = 10)
        val local = DataManifest(news = 5)
        assertTrue(remote.hasNewsUpdate(local))
        assertFalse(local.hasNewsUpdate(remote))
    }

    @Test
    fun checksumsDefault_emptyMap() {
        val json = """{"blocks":1}"""
        val m = DataManifest.fromJson(json)
        assertTrue(m.checksums.isEmpty())
    }

    @Test
    fun minimalJson_allDefaults() {
        val m = DataManifest.fromJson("{}")
        assertEquals(0L, m.version)
        assertEquals(0L, m.blocks)
        assertEquals(0L, m.mobs)
        assertEquals(0L, m.textures)
        assertEquals(0L, m.textureMap)
        assertEquals(0L, m.news)
        assertTrue(m.checksums.isEmpty())
    }
}
