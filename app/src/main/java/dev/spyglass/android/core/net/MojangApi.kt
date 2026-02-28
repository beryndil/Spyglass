package dev.spyglass.android.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object MojangApi {

    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,16}$")
    private val UUID_HEX_REGEX = Regex("^[a-f0-9]{32}$")
    private val UUID_DASHED_REGEX = Regex("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$")

    /**
     * Fetches the Mojang UUID for a Minecraft Java username.
     * Returns the dashed UUID string (e.g. "069a79f4-44e9-4726-a5be-fca90e38aaf5") or null on failure.
     */
    suspend fun fetchUuid(username: String): String? = withContext(Dispatchers.IO) {
        if (!USERNAME_REGEX.matches(username)) return@withContext null
        try {
            val url = URL("https://api.mojang.com/users/profiles/minecraft/$username")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                if (conn.responseCode != 200) return@withContext null
                val body = conn.inputStream.bufferedReader().use { it.readText().take(1024) }
                val raw = JSONObject(body).getString("id") // 32 hex chars, no dashes
                if (!UUID_HEX_REGEX.matches(raw)) return@withContext null
                // Insert dashes: 8-4-4-4-12
                "${raw.substring(0, 8)}-${raw.substring(8, 12)}-${raw.substring(12, 16)}-${raw.substring(16, 20)}-${raw.substring(20)}"
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Posed full-body skin render (starlightskins). */
    fun skinUrl(uuid: String): String? {
        if (!UUID_DASHED_REGEX.matches(uuid)) return null
        return "https://starlightskins.lunareclipse.studio/render/reading/$uuid/full"
    }

    /** Cheering pose render (starlightskins). */
    fun cheerUrl(uuid: String): String? {
        if (!UUID_DASHED_REGEX.matches(uuid)) return null
        return "https://starlightskins.lunareclipse.studio/render/crossed/$uuid/full"
    }

    /** Face avatar URL (mc-heads.net). */
    fun avatarUrl(uuid: String): String? {
        if (!UUID_DASHED_REGEX.matches(uuid)) return null
        return "https://mc-heads.net/avatar/$uuid/128"
    }
}
