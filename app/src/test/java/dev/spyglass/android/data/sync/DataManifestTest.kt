package dev.spyglass.android.data.sync

import org.junit.Assert.*
import org.junit.Test

class DataManifestTest {

    @Test
    fun jsonRoundTrip() {
        val original = DataManifest(
            version = "FireHorse.0303.0500",
            blocks = "FireHorse.0303.0500", mobs = "FireHorse.0302.1319", biomes = "FireHorse.0301.0800",
            enchants = "FireHorse.0301.0800", potions = "FireHorse.0302.1319", trades = "FireHorse.0302.1430",
            recipes = "FireHorse.0303.0500", structures = "FireHorse.0301.0800", items = "FireHorse.0303.0500",
            advancements = "FireHorse.0301.0800", commands = "FireHorse.0301.0800",
            textures = "FireHorse.0302.2227", textureMap = "FireHorse.0303.1800", news = "FireHorse.0302.1319",
            checksums = mapOf("blocks.json" to "abc123"),
        )
        val json = DataManifest.toJson(original)
        val restored = DataManifest.fromJson(json)
        assertEquals(original, restored)
    }

    @Test
    fun unknownKeysIgnored() {
        val json = """{"blocks":"FireHorse.0302.1319","unknown_field":"hello","extra":999}"""
        val m = DataManifest.fromJson(json)
        assertEquals("FireHorse.0302.1319", m.blocks)
    }

    @Test
    fun changedTables_detectsDifferences() {
        val a = DataManifest(blocks = "FireHorse.0301.0800", mobs = "FireHorse.0301.0800")
        val b = DataManifest(blocks = "FireHorse.0302.1319", mobs = "FireHorse.0303.0500")
        val changed = a.changedTables(b)
        assertTrue("blocks" in changed)
        assertTrue("mobs" in changed)
        assertEquals(2, changed.size)
    }

    @Test
    fun changedTables_identical() {
        val m = DataManifest(blocks = "FireHorse.0303.0500", mobs = "FireHorse.0303.0500")
        assertTrue(m.changedTables(m).isEmpty())
    }

    @Test
    fun withVersion_updatesCorrectField() {
        val m = DataManifest()
        val updated = m.withVersion("blocks", "FireHorse.0303.0500")
        assertEquals("FireHorse.0303.0500", updated.blocks)
        assertEquals("", updated.mobs)
    }

    @Test
    fun withVersion_unknownTable_returnsUnchanged() {
        val m = DataManifest(blocks = "FireHorse.0301.0800")
        val same = m.withVersion("nonexistent", "FireHorse.0303.0500")
        assertEquals(m, same)
    }

    @Test
    fun versionOf_knownTables() {
        val m = DataManifest(
            blocks = "v1", mobs = "v2", biomes = "v3",
            enchants = "v4", potions = "v5", trades = "v6",
            recipes = "v7", structures = "v8", items = "v9",
            advancements = "v10", commands = "v11",
        )
        assertEquals("v1", m.versionOf("blocks"))
        assertEquals("v2", m.versionOf("mobs"))
        assertEquals("v3", m.versionOf("biomes"))
        assertEquals("v4", m.versionOf("enchants"))
        assertEquals("v5", m.versionOf("potions"))
        assertEquals("v6", m.versionOf("trades"))
        assertEquals("v7", m.versionOf("recipes"))
        assertEquals("v8", m.versionOf("structures"))
        assertEquals("v9", m.versionOf("items"))
        assertEquals("v10", m.versionOf("advancements"))
        assertEquals("v11", m.versionOf("commands"))
    }

    @Test
    fun versionOf_unknownTable_returnsEmpty() {
        assertEquals("", DataManifest().versionOf("unknown"))
    }

    @Test
    fun effectiveVersion_returnsMax() {
        val m = DataManifest(
            blocks = "FireHorse.0301.0800",
            recipes = "FireHorse.0303.0500",
            commands = "FireHorse.0302.1319",
        )
        assertEquals("FireHorse.0303.0500", m.effectiveVersion)
    }

    @Test
    fun effectiveVersion_allEmpty() {
        assertEquals("", DataManifest().effectiveVersion)
    }

    @Test
    fun hasTextureUpdate() {
        val remote = DataManifest(textures = "FireHorse.0303.0500")
        val local = DataManifest(textures = "FireHorse.0302.1319")
        assertTrue(remote.hasTextureUpdate(local))
        assertFalse(local.hasTextureUpdate(remote))
        assertFalse(remote.hasTextureUpdate(remote))
    }

    @Test
    fun hasTextureMapUpdate() {
        val remote = DataManifest(textureMap = "FireHorse.0303.0500")
        val local = DataManifest(textureMap = "FireHorse.0302.1319")
        assertTrue(remote.hasTextureMapUpdate(local))
        assertFalse(local.hasTextureMapUpdate(remote))
    }

    @Test
    fun hasNewsUpdate() {
        val remote = DataManifest(news = "FireHorse.0303.0500")
        val local = DataManifest(news = "FireHorse.0302.1319")
        assertTrue(remote.hasNewsUpdate(local))
        assertFalse(local.hasNewsUpdate(remote))
    }

    @Test
    fun checksumsDefault_emptyMap() {
        val json = """{"blocks":"FireHorse.0301.0800"}"""
        val m = DataManifest.fromJson(json)
        assertTrue(m.checksums.isEmpty())
    }

    @Test
    fun minimalJson_allDefaults() {
        val m = DataManifest.fromJson("{}")
        assertEquals("", m.version)
        assertEquals("", m.blocks)
        assertEquals("", m.mobs)
        assertEquals("", m.textures)
        assertEquals("", m.textureMap)
        assertEquals("", m.news)
        assertTrue(m.checksums.isEmpty())
    }

    @Test
    fun compareVersions_ordering() {
        assertTrue(DataManifest.compareVersions("FireHorse.0303.0500", "FireHorse.0302.1319") > 0)
        assertTrue(DataManifest.compareVersions("FireHorse.0302.1319", "FireHorse.0303.0500") < 0)
        assertEquals(0, DataManifest.compareVersions("FireHorse.0303.0500", "FireHorse.0303.0500"))
    }

    @Test
    fun compareVersions_differentYears() {
        assertTrue(DataManifest.compareVersions("FireGoat.0301.0000", "FireHorse.0301.0000") > 0)
    }

    @Test
    fun compareVersions_emptyStrings() {
        assertTrue(DataManifest.compareVersions("FireHorse.0303.0500", "") > 0)
        assertTrue(DataManifest.compareVersions("", "FireHorse.0303.0500") < 0)
        assertEquals(0, DataManifest.compareVersions("", ""))
    }
}
