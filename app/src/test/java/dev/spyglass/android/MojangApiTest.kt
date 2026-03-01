package dev.spyglass.android

import dev.spyglass.android.core.net.MojangApi
import org.junit.Assert.*
import org.junit.Test

class MojangApiTest {

    // ── USERNAME_REGEX ──────────────────────────────────────────────────────

    @Test
    fun `valid usernames pass regex`() {
        assertTrue(MojangApi.USERNAME_REGEX.matches("Steve"))
        assertTrue(MojangApi.USERNAME_REGEX.matches("Alex"))
        assertTrue(MojangApi.USERNAME_REGEX.matches("Player_123"))
        assertTrue(MojangApi.USERNAME_REGEX.matches("abc"))              // 3 chars min
        assertTrue(MojangApi.USERNAME_REGEX.matches("a_b_c_d_e_f_g_h")) // 16 chars max
        assertTrue(MojangApi.USERNAME_REGEX.matches("__underscore__"))
    }

    @Test
    fun `too short usernames fail regex`() {
        assertFalse(MojangApi.USERNAME_REGEX.matches("ab"))
        assertFalse(MojangApi.USERNAME_REGEX.matches("a"))
        assertFalse(MojangApi.USERNAME_REGEX.matches(""))
    }

    @Test
    fun `too long usernames fail regex`() {
        assertFalse(MojangApi.USERNAME_REGEX.matches("a".repeat(17)))
    }

    @Test
    fun `usernames with invalid characters fail regex`() {
        assertFalse(MojangApi.USERNAME_REGEX.matches("user name"))   // space
        assertFalse(MojangApi.USERNAME_REGEX.matches("user-name"))   // hyphen
        assertFalse(MojangApi.USERNAME_REGEX.matches("user.name"))   // dot
        assertFalse(MojangApi.USERNAME_REGEX.matches("user@name"))   // at sign
        assertFalse(MojangApi.USERNAME_REGEX.matches("über_name"))   // unicode
    }

    // ── skinUrl ─────────────────────────────────────────────────────────────

    @Test
    fun `skinUrl returns URL for valid dashed UUID`() {
        val uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5"
        val url = MojangApi.skinUrl(uuid)
        assertNotNull(url)
        assertTrue(url!!.contains(uuid))
        assertTrue(url.startsWith("https://starlightskins.lunareclipse.studio/"))
    }

    @Test
    fun `skinUrl returns null for undashed UUID`() {
        assertNull(MojangApi.skinUrl("069a79f444e94726a5befca90e38aaf5"))
    }

    @Test
    fun `skinUrl returns null for empty string`() {
        assertNull(MojangApi.skinUrl(""))
    }

    @Test
    fun `skinUrl returns null for invalid format`() {
        assertNull(MojangApi.skinUrl("not-a-uuid-at-all"))
        assertNull(MojangApi.skinUrl("069a79f4-44e9-4726-a5be"))
    }

    // ── cheerUrl ────────────────────────────────────────────────────────────

    @Test
    fun `cheerUrl returns URL for valid dashed UUID`() {
        val uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5"
        val url = MojangApi.cheerUrl(uuid)
        assertNotNull(url)
        assertTrue(url!!.contains("crossed"))
        assertTrue(url.contains(uuid))
    }

    @Test
    fun `cheerUrl returns null for invalid UUID`() {
        assertNull(MojangApi.cheerUrl("invalid"))
    }

    // ── avatarUrl ───────────────────────────────────────────────────────────

    @Test
    fun `avatarUrl returns URL for valid dashed UUID`() {
        val uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5"
        val url = MojangApi.avatarUrl(uuid)
        assertNotNull(url)
        assertTrue(url!!.contains("mc-heads.net"))
        assertTrue(url.contains(uuid))
    }

    @Test
    fun `avatarUrl returns null for undashed UUID`() {
        assertNull(MojangApi.avatarUrl("069a79f444e94726a5befca90e38aaf5"))
    }

    @Test
    fun `avatarUrl returns null for empty string`() {
        assertNull(MojangApi.avatarUrl(""))
    }
}
